package com.uddernetworks.drivestore.encoding;

import org.junit.jupiter.api.Test;

import java.util.concurrent.ThreadLocalRandom;

import static org.junit.jupiter.api.Assertions.*;

public class DocCoderTest {

    @Test
    public void encodeDecodeChunk() {
        for (int i = 0; i < 100; i++) {
            // Generate a random 0-64 bit number, representing an arbitrary long of data from a stream.
            long data = Math.abs(ThreadLocalRandom.current().nextLong());
            var chunk = DataChunk.constructChunk(data);

            var textRun = DocCoder.encodeChunk(chunk);
            var decoded = DocCoder.decodeChunk(textRun);

            assertEquals(chunk, decoded.orElse(null), "The encoded and decoded chunks do not match.\nChunk: " + data + "\nChunk information: " + chunk + "\nDecoded: " + decoded);
        }
    }

}