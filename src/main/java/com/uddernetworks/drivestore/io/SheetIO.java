package com.uddernetworks.drivestore.io;

import com.google.api.client.http.ByteArrayContent;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.model.File;
import com.google.api.services.sheets.v4.Sheets;
import com.uddernetworks.drivestore.DocStore;
import com.uddernetworks.drivestore.Mime;
import com.uddernetworks.drivestore.SheetManager;
import com.uddernetworks.drivestore.docs.ProgressListener;
import com.uddernetworks.drivestore.encoding.DecodingOutputStream;
import com.uddernetworks.drivestore.encoding.EncodingOutputStream;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static com.uddernetworks.drivestore.Utility.round;
import static com.uddernetworks.drivestore.Utility.sleep;
import static com.uddernetworks.drivestore.encoding.ByteUtil.humanReadableByteCountSI;

public class SheetIO {

    private static final Logger LOGGER = LoggerFactory.getLogger(SheetIO.class);

    private static final int STAGGER_MS = 750;

    private static final int MB = 0x100000;
//    private static final int MAX_SHEET_SIZE = 25 * MB;
    private static final int MAX_SHEET_SIZE = 10;

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

    public File uploadSheet(String title, byte[] data) throws IOException {
        var encoded = EncodingOutputStream.encode(data);

        LOGGER.info("Encoded from {} - {} ({}% overhead)", humanReadableByteCountSI(data.length), humanReadableByteCountSI(encoded.length), round((encoded.length - data.length) / (double) data.length * 100D, 2));

        var sheets = Math.ceil((double) encoded.length / MAX_SHEET_SIZE);

        LOGGER.info("This upload will use {} sheets", sheets);

        var parent = sheetManager.createFolder(title, null, Map.of(
                "directParent", "true",
                "size", String.valueOf(encoded.length),
                "sheets", String.valueOf(sheets)
        ));

        LOGGER.info("Created parent docstore/{} ({})", parent.getName(), parent.getId());

        var chunks = new ArrayList<FileChunk>();

        var lastEnded = 0;
        for (int i = 0;; i++) {
            var start = lastEnded;
            int length = Math.min(encoded.length - start, MAX_SHEET_SIZE);
            if (length == 0) {
                break;
            }

            var subBytes = new byte[length];
            System.arraycopy(encoded, start, subBytes, 0, length);
            lastEnded = start + length;

            chunks.add(new FileChunk(parent, subBytes, i));
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

    public ByteArrayOutputStream download(String id) throws IOException {
        return download(id, new ByteArrayOutputStream());
    }

    public <T extends OutputStream> T download(String id, T out) throws IOException {
        var encodingOut = new DecodingOutputStream<>(out);
        var export = drive.files().export(id, "text/tab-separated-values");
        IOUtils.copy(export.executeMediaAsInputStream(), encodingOut);
        return encodingOut.getOut();
    }


}
