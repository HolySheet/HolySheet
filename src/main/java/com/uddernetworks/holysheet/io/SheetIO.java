package com.uddernetworks.holysheet.io;

import com.google.api.client.http.ByteArrayContent;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.model.File;
import com.google.api.services.sheets.v4.Sheets;
import com.uddernetworks.grpc.HolysheetService.UploadRequest.Compression;
import com.uddernetworks.grpc.HolysheetService.UploadRequest.Upload;
import com.uddernetworks.holysheet.Mime;
import com.uddernetworks.holysheet.SheetManager;
import com.uddernetworks.holysheet.encoding.DecodingOutputStream;
import com.uddernetworks.holysheet.encoding.EncodingOutputStream;
import com.uddernetworks.holysheet.utility.Utility;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Scanner;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import static com.uddernetworks.holysheet.SheetManager.PATH_REGEX;
import static com.uddernetworks.holysheet.utility.Utility.DRIVE_FIELDS;
import static com.uddernetworks.holysheet.utility.Utility.humanReadableByteCountSI;

public class SheetIO {

    private static final Logger LOGGER = LoggerFactory.getLogger(SheetIO.class);

    private static final int STAGGER_MS = 5000;

//    private static final int MB = 1000000;
//    private static final int MAX_SHEET_SIZE = 10 * MB;

    private final SheetManager sheetManager;
    private final Drive drive;
    private final Sheets sheets;

    public SheetIO(SheetManager sheetManager, Drive drive, Sheets sheets) {
        this.sheetManager = sheetManager;
        this.drive = drive;
        this.sheets = sheets;
    }

    public CompletableFuture<File> downloadData(java.io.File file, String id) {
        return downloadData(file, id, $ -> {});
    }

    public CompletableFuture<File> downloadData(java.io.File destination, String id, Consumer<Double> statusUpdate) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                var parent = drive.files().get(id).setFields(DRIVE_FIELDS).execute();
                if (parent == null) {
                    throw new RuntimeException("Couldn't find id " + id);
                }

                var props = parent.getProperties();
                if (!props.get("directParent").equals("true")) {
                    throw new RuntimeException("Not a direct parent!");
                }

                // Defaults to NONE(0), will never be null
                var compression = parseLegacyCompression(props.get("compressed"));

                LOGGER.info("File compression: {}", compression.name());

                var files = sheetManager.getAllSheets(parent.getId());

                LOGGER.info("Found {} children", files.size());

                var encodingOut = new DecodingOutputStream<>(new FileOutputStream(destination));
                var downloadedIndex = new double[]{0};

                files.stream().sorted(Comparator.comparingInt(file -> {
                    var fp = file.getProperties();
                    if (fp == null) return -1;
                    return Integer.parseInt(fp.get("index"));
                })).forEach(file -> {
                    downloadSheet(file, encodingOut);
                    statusUpdate.accept(downloadedIndex[0]++ / (double) files.size());
                });

                encodingOut.close();

                LOGGER.info("Downloaded {} sheets", files.size());

                if (compression == Compression.ZIP) {
                    LOGGER.error("Ignoring compression! This is only due to being in a development environment");
//                    LOGGER.info("Uncompressing data...");
//                    finalStream = CompressionUtils.uncompressToOutputStream(encodingOut.getOut().toByteArray());
                }

                LOGGER.info("Downloaded and unencoded {}", humanReadableByteCountSI(destination.length()));

                return parent;
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        });
    }

    private Compression parseLegacyCompression(String compression) {
        if (compression.equals("true")) {
            return Compression.ZIP;
        } else if (compression.equals("false")) {
            return Compression.NONE;
        }

        return Compression.forNumber(Utility.tryParse(compression, 0));
    }

    private void downloadSheet(File file, OutputStream out) {
        try {
            var properties = file.getProperties();
            LOGGER.info("Downloading sheet #{} - {}", properties.get("index"), humanReadableByteCountSI(Long.parseLong(properties.get("size"))));
            var byteOutput = new ByteArrayOutputStream();
            drive.files().export(file.getId(), "text/tab-separated-values").executeMediaAndDownloadTo(byteOutput);
            out.write(byteOutput.toByteArray());
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public CompletableFuture<File> uploadDataStream(String title, String path, long fileSize, long maxSheetSize, Compression compress, Upload uploadType, EncodingOutputStream outputStream) throws IOException {
        path = cleanPath(path);

        var parent = sheetManager.createFolder(title, sheetManager.getSheetStore(), Map.of(
                "directParent", "true",
                "starred", "false",
                "processing", "true",
                "size", "0",
                "sheets", "0",
                "path", path,
                "compressed", String.valueOf(compress.getNumber())
        ));

        LOGGER.info("Created parent sheetStore/{} ({})", parent.getName(), parent.getId());

        return processRawStream(outputStream, fileSize, (int) maxSheetSize, parent, uploadType).thenApply($ -> parent);
    }

    private CompletableFuture<Void> processRawStream(EncodingOutputStream encodingOut, long totalSize, int maxLength, File parent, Upload uploadType) {

        // ~22% overhead
        int estimatedChunks = (int) Math.ceil((totalSize * 1.22) / (double) maxLength);

        LOGGER.info("File size: {} estimated chunks: {}", humanReadableByteCountSI(totalSize), estimatedChunks);

        long start = System.currentTimeMillis();

        encodingOut.setChunkConsumer((index, bytes) -> {
            LOGGER.info("Uploading {}/~{}", index + 1, estimatedChunks);

            int iterations = 0;
            int delay = 1000;
            while (true) {
                try {
                    processChunk(new FileChunk(parent, bytes, index), parent, uploadType);
                    return;
                } catch (Exception e) {
                    LOGGER.error("An exception occurred during the processing of file " + index, e);

                    delay = Math.max(30000, delay * 2); // Increase the delay 5x from previous, max of 30 seconds

                    if (iterations++ >= 5) { // Separate from timing, as that cna change
                        LOGGER.info("It has been 5 failed iterations, terminating upload. The file will remain with the 'processing' property set to true, it may be manually deleted later. IN the future, a more robust system may be implemented of trying file uploads later.");
                        System.exit(0);
                    }

                    LOGGER.info("Waiting {}ms", delay);
                    Utility.sleep(delay);
                }
            }
        });

        var completer = new CompletableFuture<Void>();

        encodingOut.setOnClose(() -> {
            int sheets = encodingOut.getChunkIndex();
            long size = encodingOut.getLength();

            LOGGER.info("Completed. Readable data: {} sheet estimated: {} sheet exact: {}", humanReadableByteCountSI(size), estimatedChunks, sheets);

            double durationSeconds = (System.currentTimeMillis() - start) / 1000D;
            long bps = (long) ((double) size / durationSeconds);
            LOGGER.info("Finished upload in {} ms at a rate of {}/s", System.currentTimeMillis() - start, humanReadableByteCountSI(bps));

            try {
                sheetManager.addProperties(parent, Map.of(
                        "processing", "false",
                        "size", String.valueOf(size),
                        "sheets", String.valueOf(sheets)
                ));
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }

            completer.complete(null);
        });

        return completer;
    }

    public File uploadDataFile(String title, String path, long fileSize, long maxSheetSize, Compression compress, Upload uploadType, InputStream data) throws IOException {
        return uploadDataFile(title, path, fileSize, maxSheetSize, compress, uploadType, data, null);
    }

    public File uploadDataFile(String title, String path, long fileSize, long maxSheetSize, Compression compress, Upload uploadType, InputStream data, Consumer<Double> statusUpdate) throws IOException {
        path = cleanPath(path);
        if (statusUpdate == null) {
            statusUpdate = $ -> {};
        }

        var parent = sheetManager.createFolder(title, sheetManager.getSheetStore(), Map.of(
                "directParent", "true",
                "starred", "false",
                "processing", "true",
                "size", "0",
                "sheets", "0",
                "path", path,
                "compressed", String.valueOf(compress.getNumber())
        ));

        LOGGER.info("Created parent sheetStore/{} ({})", parent.getName(), parent.getId());

        processRawFile(data, fileSize, (int) maxSheetSize, parent, uploadType, statusUpdate);

        return parent;
    }

    private void processRawFile(InputStream input, long totalSize, int maxLength, File parent, Upload uploadType, Consumer<Double> statusUpdate) throws IOException {

        // ~22% overhead
        int estimatedChunks = (int) Math.ceil((totalSize * 1.22) / (double) maxLength);

        LOGGER.info("File size: {} estimated chunks: {}", humanReadableByteCountSI(totalSize), estimatedChunks);

        long start = System.currentTimeMillis();
        boolean[] sentMax = {false};

        statusUpdate.accept(0D);

        var encodingOut = EncodingOutputStream.encode(input, maxLength, (index, bytes) -> {
            LOGGER.info("Uploading {}/~{}", index + 1, estimatedChunks);

            int iterations = 0;
            int delay = 1000;
            while (true) {
                try {
                    processChunk(new FileChunk(parent, bytes, index), parent, uploadType);
                    var percent = Math.max((index + 1) / ((double) estimatedChunks + 1), 1D);
                    if (!sentMax[0]) {
                        sentMax[0] = percent == 1D;
                    }
                    statusUpdate.accept(percent);
                    return;
                } catch (Exception e) {
                    LOGGER.error("An exception occurred during the processing of file " + index, e);

                    delay = Math.max(30000, delay * 2); // Increase the delay 5x from previous, max of 30 seconds

                    if (iterations++ >= 5) { // Separate from timing, as that cna change
                        LOGGER.info("It has been 5 failed iterations, terminating upload. The file will remain with the 'processing' property set to true, it may be manually deleted later. IN the future, a more robust system may be implemented of trying file uploads later.");
                        System.exit(0);
                    }

                    LOGGER.info("Waiting {}ms", delay);
                    Utility.sleep(delay);
                }
            }
        });

        if (!sentMax[0]) {
            statusUpdate.accept(1D);
        }

        int sheets = encodingOut.getChunkIndex();
        long size = encodingOut.getLength();

        LOGGER.info("Completed. Readable data: {} sheet estimated: {} sheet exact: {}", humanReadableByteCountSI(size), estimatedChunks, sheets);

        double durationSeconds = (System.currentTimeMillis() - start) / 1000D;
        long bps = (long) ((double) size / durationSeconds);
        LOGGER.info("Finished upload in {} ms at a rate of {}/s", System.currentTimeMillis() - start, humanReadableByteCountSI(bps));

        sheetManager.addProperties(parent, Map.of(
                "processing", "false",
                "size", String.valueOf(size),
                "sheets", String.valueOf(sheets)
        ));
    }

    private File processChunk(FileChunk chunk, File parent, Upload uploadType) {
        try {
            LOGGER.info("Uploading chunk-{}", chunk.getIndex() + 1);

            var content = new ByteArrayContent("text/tab-separated-values", chunk.getBytes());
            var request = drive.files().create(new File()
                    .setMimeType(Mime.SHEET.getMime())
                    .setName("chunk-" + chunk.getIndex())
                    .setProperties(chunk.getProperties())
                    .setParents(Collections.singletonList(parent.getId())), content)
                    .setFields("id");

            request.getMediaHttpUploader()
                    .setDirectUploadEnabled(uploadType == Upload.DIRECT)
                    .setChunkSize(20 * 0x100000); // 20MB (Default 10)

            return request.execute();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public void setStarred(String id, boolean starred) throws IOException {
        sheetManager.addProperties(id, Map.of("starred", starred ? "true" : "false"));
    }

    public void setPath(String id, String path) throws IOException {
        path = cleanPath(path);
        sheetManager.addProperties(id, Map.of("path", path));
    }

    public String cleanPath(String path) {
        if (path.isBlank() || !PATH_REGEX.matcher(path).matches()) {
            path = "/";
        }

        if (!path.startsWith("/")) {
            path = "/" + path;
        }

        if (!path.endsWith("/")) {
            path += "/";
        }

        return path;
    }

    public void createFolder(String path) throws IOException {
        var folders = new ArrayList<>(getFolders());

        if (folders.contains(path)) {
            return;
        }

        folders.add(path);

        sheetManager.addProperties(sheetManager.getSheetStore(), Map.of("folders", String.join(",", folders)));
    }

    public List<String> getFolders() {
        var properties = sheetManager.getSheetStore().getProperties();
        var folderString = properties == null ? "" : properties.getOrDefault("folders", "");
        return new ArrayList<>(Arrays.stream(folderString.split(",")).collect(Collectors.toUnmodifiableList()));
    }

    public void deleteData(String id, boolean permanent) throws IOException {
        deleteData(id, true, permanent);
    }

    public void deleteData(String id, boolean confirm, boolean permanent) throws IOException {
        var file = drive.files().get(id).setFields("id, name, properties, trashed").execute();

        if (file == null) {
            throw new RuntimeException("No file could be found with the given ID \"" + id + "\"");
        }

        var properties = file.getProperties();
        if (!"true".equals(properties.get("directParent"))) {
            throw new RuntimeException("The given file was not detected as a direct parent of generated sheet data. For your safety, HolySheet will not delete anything not directly created by it, therefore this action has been cancelled.");
        }

        if (confirm) {
            LOGGER.info("Are you sure you want to delete \"{}\"? It consists of {} sheets totalling {}{}. This action skips the trash and is irreversible. (y/n)",
                    file.getName(),
                    properties.get("sheets"),
                    humanReadableByteCountSI(Long.parseLong(properties.get("size"))),
                    "true".equals(properties.get("compressed")) ? " compressed" : "");

            var scanner = new Scanner(System.in);
            if (!scanner.hasNextLine()) {
                LOGGER.info("Cancelling removal.");
                return;
            }

            var line = scanner.nextLine();
            if (!"y".equalsIgnoreCase(line) && "yes".equalsIgnoreCase(line)) {
                LOGGER.info("Cancelling removal.");
                return;
            }
        }

        LOGGER.info("Removing {}...", file.getName());

        if (permanent) {
            drive.files().delete(id).execute();
        } else {
            if (file.getTrashed()) {
                drive.files().delete(id).execute();
            } else {
                var temp = new File();
                temp.setTrashed(true);
                drive.files().update(id, temp).execute();
            }
        }

        LOGGER.info("Removed successfully");
    }

    public void restoreData(String id) throws IOException {
        var file = drive.files().get(id).setFields("id, name, trashed").execute();

        if (file == null) {
            throw new RuntimeException("No file could be found with the given ID \"" + id + "\"");
        }

        LOGGER.info("Removing {}...", file.getName());

        if (!file.getTrashed()) {
            LOGGER.info("Tried to restore a non-trashed file!");
            return;
        }

        var temp = new File();
        temp.setTrashed(false);
        drive.files().update(id, temp).execute();

        LOGGER.info("Restored successfully");
    }

    public void cloneFile(String fileId, int maxSheetSize, Compression compress) {
        downloadFile(fileId).ifPresent(fileData -> {
            var file = fileData.getFile();
            var in = fileData.getIn();

            var name = file.getName();

            LOGGER.info("Saving {}...", name);

            try {
                uploadDataFile(name, "/", fileData.getSize(), maxSheetSize, compress, Upload.MULTIPART, in);
            } catch (IOException e) {
                LOGGER.error("An error occurred while uploading the " + fileId, e);
            }
        });
    }

    /**
     * Downloads a file for CLONING ONLY.
     *
     * @return File bytes
     */
    public Optional<FileData> downloadFile(String fileId) {
        try {
            var file = drive.files().get(fileId).execute();

            if (file == null) {
                LOGGER.error("No file could be found with the given ID \"{}\"", fileId);
                return Optional.empty();
            }

            var downloaded = java.io.File.createTempFile("downloaded-" + hashCode(), "");
            drive.files().get(fileId).executeMediaAndDownloadTo(new FileOutputStream(downloaded));
            return Optional.of(new FileData(file, downloaded.length(), new FileInputStream(downloaded)));
        } catch (IOException e) {
            LOGGER.error("An error occurred while downloading the file " + fileId, e);
            return Optional.empty();
        }
    }

    public static class FileData {
        private final File file;
        private final long size;
        private final InputStream in;

        public FileData(File file, long size, InputStream in) {
            this.file = file;
            this.size = size;
            this.in = in;
        }

        public File getFile() {
            return file;
        }

        public long getSize() {
            return size;
        }

        public InputStream getIn() {
            return in;
        }
    }
}
