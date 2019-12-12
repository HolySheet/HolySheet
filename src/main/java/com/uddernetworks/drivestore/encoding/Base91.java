package com.uddernetworks.drivestore.encoding;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public class Base91 {

    private static final Logger LOGGER = LoggerFactory.getLogger(Base91.class);

    public static void main(String[] args) {
        var text = "1234567812345678";

        var chunkOS = new DocOutputStream();
        try {
            chunkOS.write(text.getBytes());
            chunkOS.close();
        } catch (IOException e) {
            LOGGER.error("An error occurred while encoding", e);
        }

        LOGGER.info("{} > {} chunks", text, chunkOS.getChunks().size());
    }

}
