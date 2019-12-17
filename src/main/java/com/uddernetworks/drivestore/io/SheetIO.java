package com.uddernetworks.drivestore.io;

import com.google.api.client.http.ByteArrayContent;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.model.File;
import com.google.api.services.sheets.v4.Sheets;
import com.uddernetworks.drivestore.Mime;
import com.uddernetworks.drivestore.SheetManager;
import com.uddernetworks.drivestore.encoding.DecodingOutputStream;
import com.uddernetworks.drivestore.encoding.EncodingOutputStream;
import com.uddernetworks.drivestore.utility.CompressionUtils;
import com.uddernetworks.drivestore.utility.Utility;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Scanner;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import static com.uddernetworks.drivestore.utility.Utility.round;
import static com.uddernetworks.drivestore.utility.Utility.humanReadableByteCountSI;

public class SheetIO {

    private static final Logger LOGGER = LoggerFactory.getLogger(SheetIO.class);

    private static final int STAGGER_MS = 5000;

    private static final int MB = 1000000;
    private static final int MAX_SHEET_SIZE = 10 * MB;

    private final SheetManager sheetManager;
    private final Drive drive;
    private final Sheets sheets;
    private final File docstore;

    public SheetIO(SheetManager sheetManager) {
        this.sheetManager = sheetManager;
        this.drive = sheetManager.getDrive();
        this.sheets = sheetManager.getSheets();
        this.docstore = sheetManager.getDocstore();
    }

    public Optional<ByteArrayOutputStream> downloadData(String id) throws IOException {
        var parent = drive.files().get(id).setFields("id, properties").execute();
        if (parent == null) {
            LOGGER.error("Couldn't find id {}", id);
            return Optional.empty();
        }

        var props = parent.getProperties();
        if (!props.get("directParent").equals("true")) {
            LOGGER.error("Not a direct parent!");
            return Optional.empty();
        }

        boolean compressed = props.get("compressed").equals("true");

        LOGGER.info("File is {}", compressed ? "compressed" : "uncompressed");

        var files = sheetManager.getAllSheets(parent.getId());

        LOGGER.info("Found {} children", files.size());

        var encodingOut = new DecodingOutputStream<ByteArrayOutputStream>();

        files.stream().sorted(Comparator.comparingInt(file -> {
            var fp = file.getProperties();
            if (fp == null) return -1;
            return Integer.parseInt(fp.get("index"));
        })).forEach(file -> downloadSheet(file, encodingOut));
        encodingOut.flush();
        LOGGER.info("Downloaded {} sheets", files.size());

        var finalStream = encodingOut.getOut();

        if (compressed) {
            LOGGER.info("Uncompressing data...");
            finalStream = CompressionUtils.uncompressToOutputStream(encodingOut.getOut().toByteArray());
        }

        LOGGER.info("Downloaded and unencoded {}", humanReadableByteCountSI(finalStream.size()));

        return Optional.of(finalStream);
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

    public File uploadData(String title, boolean compress, byte[] data) throws IOException {
        if (compress) {
            data = CompressionUtils.compress(data);
        }

        var encoded = EncodingOutputStream.encode(data, MAX_SHEET_SIZE);
        var byteArrayList = encoded.getChunks();

        LOGGER.info("Encoded from {} - {} ({}% overhead)", humanReadableByteCountSI(data.length), humanReadableByteCountSI(encoded.getLength()), round((encoded.getLength() - data.length) / (double) data.length * 100D, 2));

        LOGGER.info("This upload will use {} sheets", byteArrayList.size());

        var parent = sheetManager.createFolder(title, sheetManager.getDocstore(), Map.of(
                "directParent", "true",
                "size", String.valueOf(encoded.getLength()),
                "sheets", String.valueOf(byteArrayList.size()),
                "compressed", String.valueOf(compress)
        ));

        LOGGER.info("Created parent docstore/{} ({})", parent.getName(), parent.getId());

        var chunks = new ArrayList<FileChunk>();

        for (int i = 0; i < byteArrayList.size(); i++) {
            chunks.add(new FileChunk(parent, byteArrayList.get(i), i));
        }

        LOGGER.info("Processing {} chunks", chunks.size());

        processChunks(chunks, parent, title, encoded.getLength());

        return parent;
    }

    private Map<FileChunk, File> processChunks(List<FileChunk> chunks, File parent, String title, long size) {
        var index = new AtomicInteger();
        var map = chunks.stream().collect(Collectors.toMap(c -> c, c -> {
            printProgress(title, c.getBytes().length, size, index.getAndIncrement(), chunks.size());
            return processChunk(c, parent);
        }));
        printProgress(title, size, size, chunks.size(), chunks.size());
        return map;
    }

    private void printProgress(String title, long currSize, long totalSize, int index, int total) {
        System.out.println(Utility.progressBar("Uploading " + title, humanReadableByteCountSI(currSize) + "/" + humanReadableByteCountSI(totalSize), 40,  index/ (double) total));
    }

    private File processChunk(FileChunk chunk, File parent) {
        try {
            // Stagger uploads STAGGER_MS ms
//            sleep(chunk.getIndex() * STAGGER_MS);

            LOGGER.info("Processing chunk #{} ({})", chunk.getIndex(), humanReadableByteCountSI(chunk.getBytes().length));
            var content = new ByteArrayContent("text/tab-separated-values", chunk.getBytes());
            var request = drive.files().create(new File()
                    .setMimeType(Mime.SHEET.getMime())
                    .setName("chunk-" + chunk.getIndex())
                    .setProperties(chunk.getProperties())
                    .setParents(Collections.singletonList(parent.getId())), content)
                    .setFields("id");

            request.getMediaHttpUploader()
                    .setChunkSize(5 * 0x100000); // 5MB (Default 10)
            return request.execute();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        } finally {
            LOGGER.info("Done processing #{}", chunk.getIndex());
        }
    }

    public void deleteData(String id) {
        try {
            var file = drive.files().get(id).setFields("id, name, properties").execute();

            if (file == null) {
                LOGGER.error("No file could be found with the given ID \"{}\"", id);
                return;
            }

            var properties = file.getProperties();
            if (!"true".equals(properties.get("directParent"))) {
                LOGGER.error("The given file was not detected as a direct parent of generated sheet data. For your safety, DocStore will not delete anything not directly created by it, therefore this action has been cancelled.");
                return;
            }

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

            LOGGER.info("Removing {}...", file.getName());

            drive.files().delete(id).execute();

            LOGGER.info("Removed successfully");
        } catch (IOException e) {
            LOGGER.error("An error occurred while deleting the file " + id, e);
        }
    }
}
