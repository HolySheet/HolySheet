package com.uddernetworks.drivestore.encoding;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import java.util.concurrent.ThreadLocalRandom;

public class DataChunkTest {

    @Test
    public void conversion() {
        for (int i = 0; i < 100; i++) {
            // Generate a random 0-64 bit number, representing an arbitrary long of data from a stream.
            long data = Math.abs(ThreadLocalRandom.current().nextLong());

            var chunk = DataChunk.constructChunk(data);
            var deconstructed = chunk.deconstructChunk();

            assertEquals(data, deconstructed, "The constructed and deconstructed chunks do not match.\nData: " + data + "\nDeconstructed: " + deconstructed + "\nChunk information: " + chunk);
        }
    }

}
