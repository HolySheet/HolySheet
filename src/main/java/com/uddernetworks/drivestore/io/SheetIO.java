package com.uddernetworks.drivestore.io;

import com.google.api.client.http.ByteArrayContent;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.model.File;
import com.google.api.services.sheets.v4.Sheets;
import com.uddernetworks.drivestore.Mime;
import com.uddernetworks.drivestore.SheetManager;
import com.uddernetworks.drivestore.encoding.DecodingOutputStream;
import com.uddernetworks.drivestore.encoding.EncodingOutputStream;
import org.apache.commons.compress.archivers.sevenz.SevenZArchiveEntry;
import org.apache.commons.compress.archivers.sevenz.SevenZFile;
import org.apache.commons.compress.archivers.sevenz.SevenZOutputFile;
import org.apache.commons.compress.utils.SeekableInMemoryByteChannel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Optional;
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

        var files = sheetManager.getAllFile(parent.getId());

        LOGGER.info("Found {} children", files.size());

        var encodingOut = new DecodingOutputStream<ByteArrayOutputStream>();

        files.stream().sorted(Comparator.comparingInt(file -> {
            var fp = file.getProperties();
            if (fp == null) return -1;
            return Integer.parseInt(fp.get("index"));
        })).forEach(file -> downloadSheet(file, encodingOut));
        encodingOut.flush();
        LOGGER.info("Downloaded {} sheets", files.size());

        var ou = encodingOut.getOut();

        SeekableInMemoryByteChannel inMemoryByteChannel = new SeekableInMemoryByteChannel(ou.toByteArray());
        SevenZFile sevenZFile = new SevenZFile(inMemoryByteChannel);
        SevenZArchiveEntry entry = sevenZFile.getNextEntry();

        var uncompressed = new ByteArrayOutputStream();
        for (int i; (i =sevenZFile.read()) != -1;) {
            uncompressed.write(i);
        }

        LOGGER.info("Downloaded and unencoded {}", humanReadableByteCountSI(uncompressed.toByteArray().length));

        return Optional.of(uncompressed);
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

    public File uploadData(String title, byte[] data) throws IOException {

//        var sevenZOutput = new SevenZOutputFile();
//        var sevenZFile = new SevenZFile(new SeekableInMemoryByteChannel(data));

        var channel = new SeekableInMemoryByteChannel();
        SevenZOutputFile sevenZOutput = new SevenZOutputFile(channel);
        var entry = new SevenZArchiveEntry();
        entry.setDirectory(false);
        entry.setName("7z");
        entry.setLastModifiedDate(new Date());

        sevenZOutput.putArchiveEntry(entry);
        sevenZOutput.write(data);
        sevenZOutput.closeArchiveEntry();

        var sevenZEncoded = channel.array();

        var encoded = EncodingOutputStream.encode(sevenZEncoded, MAX_SHEET_SIZE);
        var byteArrayList = encoded.getChunks();

        LOGGER.info("Encoded from {} - {} ({}% overhead)", humanReadableByteCountSI(sevenZEncoded.length), humanReadableByteCountSI(encoded.getLength()), round((encoded.getLength() - sevenZEncoded.length) / (double) sevenZEncoded.length * 100D, 2));

        LOGGER.info("This upload will use {} sheets", byteArrayList.size());

        var parent = sheetManager.createFolder(title, sheetManager.getDocstore(), Map.of(
                "directParent", "true",
                "size", String.valueOf(encoded.getLength()),
                "sheets", String.valueOf(byteArrayList.size())
        ));

        LOGGER.info("Created parent docstore/{} ({})", parent.getName(), parent.getId());

        var chunks = new ArrayList<FileChunk>();

        for (int i = 0; i < byteArrayList.size(); i++) {
            chunks.add(new FileChunk(parent, byteArrayList.get(i), i));
        }

        LOGGER.info("Processing {} chunks", chunks.size());

        processChunks(chunks, parent);

        return parent;
    }

    private Map<FileChunk, File> processChunks(List<FileChunk> chunks, File parent) {
        return chunks.stream().collect(Collectors.toMap(c -> c, c -> processChunk(c, parent)));
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


}
