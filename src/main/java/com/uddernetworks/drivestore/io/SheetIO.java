package com.uddernetworks.drivestore.io;

import com.google.api.client.http.ByteArrayContent;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.model.File;
import com.google.api.services.sheets.v4.Sheets;
import com.uddernetworks.drivestore.Mime;
import com.uddernetworks.drivestore.SheetManager;
import com.uddernetworks.drivestore.encoding.DecodingOutputStream;
import com.uddernetworks.drivestore.encoding.EncodingOutputStream;
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
import java.util.stream.Collectors;

import static com.uddernetworks.drivestore.Utility.round;
import static com.uddernetworks.drivestore.encoding.ByteUtil.humanReadableByteCountSI;

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

        LOGGER.info("Downloaded and unencoded {}", humanReadableByteCountSI(ou.toByteArray().length));

        return Optional.of(ou);
    }

    private void downloadSheet(File file, OutputStream out) {
        try {
            var props = file.getProperties();
            LOGGER.info("Downloading sheet #{} - {}", props.get("index"), humanReadableByteCountSI(Long.parseLong(props.get("size"))));
            var shit = new ByteArrayOutputStream();
            drive.files().export(file.getId(), "text/tab-separated-values").executeMediaAndDownloadTo(shit);
            var ba = shit.toByteArray();
            out.write(ba);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public File uploadData(String title, byte[] data) throws IOException {
        var encoded = EncodingOutputStream.encode(data, MAX_SHEET_SIZE);
        var byteArrayList = encoded.getChunks();

        LOGGER.info("Encoded from {} - {} ({}% overhead)", humanReadableByteCountSI(data.length), humanReadableByteCountSI(encoded.getLength()), round((encoded.getLength() - data.length) / (double) data.length * 100D, 2));

        LOGGER.info("This upload will use {} sheets", byteArrayList.size());

        var parent = sheetManager.createFolder(title, sheetManager.getDocstore(), Map.of(
                "directParent", "true",
                "size", String.valueOf(encoded.getLength()),
                "sheets", String.valueOf(sheets)
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
                    .setChunkSize(5 * 0x100000) // 5MB (Default 10)
//                .setProgressListener(new ProgressListener(""))
            ;
            return request.execute();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        } finally {
            LOGGER.info("Done processing #{}", chunk.getIndex());
        }
    }


}
