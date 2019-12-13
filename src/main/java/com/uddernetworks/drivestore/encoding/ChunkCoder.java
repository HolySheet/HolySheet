package com.uddernetworks.drivestore.encoding;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.List;

import static com.uddernetworks.drivestore.encoding.ByteUtil.longToBytes;

public class ChunkCoder {

    private static final Logger LOGGER = LoggerFactory.getLogger(ChunkCoder.class);

//    public static void main(String[] args) {
//        var text = "12345678";
//
//        var chunkOS = new DocOutputStream();
//        try {
//            chunkOS.write(text.getBytes());
//            chunkOS.close();
//        } catch (IOException e) {
//            LOGGER.error("An error occurred while encoding", e);
//        }
//
//        LOGGER.info("{} > {} chunks", text, chunkOS.getChunks().size());
//
//        LOGGER.info("Decoding...");
//
//        var bytes = decodeChunks(chunkOS.getChunks());
//        LOGGER.info(new String(bytes));
//    }

    public static byte[] decodeChunks(List<DataChunk> chunks) {
        var output = new ByteArrayOutputStream();
        chunks.forEach(chunk -> {
            try {
                output.write(longToBytes(chunk.deconstructChunk()));
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        });

        return output.toByteArray();
    }

}
