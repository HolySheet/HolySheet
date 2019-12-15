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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import static com.uddernetworks.drivestore.Utility.round;
import static com.uddernetworks.drivestore.Utility.sleep;
import static com.uddernetworks.drivestore.encoding.ByteUtil.humanReadableByteCountSI;
import static com.uddernetworks.drivestore.encoding.EncodingOutputStream.CELL_WIDTH;

public class SheetIO {

    private static final Logger LOGGER = LoggerFactory.getLogger(SheetIO.class);

    private static final int STAGGER_MS = 750;

    private static final int MB = 0x100000;
    //    private static final int MAX_SHEET_SIZE = 25 * MB;
    private static final int MAX_SHEET_SIZE = 15;
    private static final int MAX_ROWS = (int) Math.floor(MAX_SHEET_SIZE / (double) (CELL_WIDTH + 1)); // +1 for newline
//    private static final int MAX_ROWS = 2; // Make dynamic later
    private static final int SNAPPED_SIZE = MAX_ROWS * (CELL_WIDTH + 1); // +1 for newline

    private final SheetManager sheetManager;
    private final Drive drive;
    private final Sheets sheets;
    private final File docstore;

    public SheetIO(SheetManager sheetManager) {
        this.sheetManager = sheetManager;
        this.drive = sheetManager.getDrive();
        this.sheets = sheetManager.getSheets();
        this.docstore = sheetManager.getDocstore();

//        System.out.println("MAX_SHEET_SIZE = " + MAX_SHEET_SIZE);
        System.out.println("MAX_ROWS = " + MAX_ROWS);
        System.out.println("SNAPPED_SIZE = " + SNAPPED_SIZE);
    }

    public Optional<ByteArrayOutputStream> downloadData(String id) throws IOException {
        var parent = drive.files().get(id).setFields("id, properties").execute();
        if (parent == null) {
            LOGGER.error("Couldn't find id {}", id);
            return Optional.empty();
        }
        System.out.println(parent.getProperties());

//        var props = parent.getProperties();
//        if (!props.get("directParent").equals("true")) {
//            LOGGER.error("Not a direct parent!");
//            return Optional.empty();
//        }

        System.out.println("Before");
        var files = sheetManager.getAllFile(parent.getId());
        System.out.println("After");

        LOGGER.info("Found {} children", files.size());

        var encodingOut = new DecodingOutputStream<ByteArrayOutputStream>();

        files.stream().sorted(Comparator.comparingInt(file -> {
            var fp = file.getProperties();
            if (fp == null) return -1;
            return Integer.parseInt(fp.get("index"));
        })).forEach(file -> downloadSheet(file, encodingOut));
        LOGGER.info("Downloaded {} sheets", files.size());

        var ou = encodingOut.getOut();
        System.out.println("Total length (124??): " + ou.toByteArray().length);
        return Optional.of(ou);
    }

    private void downloadSheet(File file, OutputStream out) {
        try {
            var shit = new ByteArrayOutputStream();
            drive.files().export(file.getId(), "text/tab-separated-values").executeMediaAndDownloadTo(shit);
            var ba = shit.toByteArray();
            System.out.println("ba = " + Arrays.toString(ba));
            out.write(ba);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public File uploadData(String title, byte[] data) throws IOException {
        var encoded = EncodingOutputStream.encode(data, MAX_ROWS);
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
        return chunks.parallelStream().collect(Collectors.toMap(c -> c, c -> processChunk(c, parent)));
    }

    private File processChunk(FileChunk chunk, File parent) {
        try {
            // Stagger uploads STAGGER_MS ms
            sleep(chunk.getIndex() * STAGGER_MS);

            LOGGER.info("Processing chunk #{}", chunk.getIndex());
            var content = new ByteArrayContent("text/tab-separated-values", chunk.getBytes());
            var request = drive.files().create(new File()
                    .setMimeType(Mime.SHEET.getMime())
                    .setName("chunk-" + chunk.getIndex())
                    .setProperties(chunk.getProperties())
                    .setParents(Collections.singletonList(parent.getId())), content)
                    .setFields("id");

            request.getMediaHttpUploader()
                    .setChunkSize(5 * MB) // 5MB (Default 10)
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
