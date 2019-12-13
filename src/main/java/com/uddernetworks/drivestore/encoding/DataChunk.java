package com.uddernetworks.drivestore.encoding;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.uddernetworks.drivestore.encoding.ByteUtil.getBit;
import static com.uddernetworks.drivestore.encoding.ByteUtil.getBitRange;
import static com.uddernetworks.drivestore.encoding.ByteUtil.getLongBitRange;
import static java.lang.Long.toBinaryString;

/**
 * A chunk of 64 bits of data with the ability to be recreated to/from Google Docs' format, as an abstracted layer.
 */
public class DataChunk {

    private static final Logger LOGGER = LoggerFactory.getLogger(DataChunk.class);

    private byte bold       = 0b0; // 1
    private byte underline  = 0b0; // 1
    private byte italics    = 0b0; // 1
    private short character = 0b00000; // 5
    private short size      = 0b00000000; // 8
    private long color      = 0b000000000000000000000000; // 24
    private long highlight  = 0b000000000000000000000000; // 24

    public DataChunk() {
    }

    public DataChunk(byte bold, byte underline, byte italics, short character, short size, long color, long highlight) {
        this.bold = bold;
        this.underline = underline;
        this.italics = italics;
        this.character = character;
        this.size = size;
        this.color = color;
        this.highlight = highlight;
    }

    public static DataChunk constructChunk(long data) {
        var chunk = new DataChunk();
        chunk.bold = getBit(data, 0);
        chunk.underline = getBit(data, 1);
        chunk.italics = getBit(data, 2);
        chunk.character = getBitRange(data, 3, 5);
        chunk.size = (short) getLongBitRange(data, 8, 8);
        chunk.color = (int) getLongBitRange(data, 16, 24);
        chunk.highlight = (int) getLongBitRange(data, 40, 24);
        return chunk;
    }

    public long deconstructChunk() {
        long output = 0L;
        output |= bold;
        output |= underline << 0b1;
        output |= italics << 0b10;
        output |= (character & 0b11111) << 0b11;
        output |= (size & 0b11111111L) << 0b1000L;
        output |= (color & 0b111111111111111111111111L) << 0b10000;
        output |= (highlight & 0b111111111111111111111111L) << 0b101000;
        return output;
    }

    public byte getBold() {
        return bold;
    }

    public byte getUnderline() {
        return underline;
    }

    public byte getItalics() {
        return italics;
    }

    public short getCharacter() {
        return character;
    }

    public short getSize() {
        return size;
    }

    public long getColor() {
        return color;
    }

    public long getHighlight() {
        return highlight;
    }

    @Override
    public String toString() {
        return "DataChunk{" +
                "bold=" + toBinaryString(bold) +
                ", underline=" + toBinaryString(underline) +
                ", italics=" + toBinaryString(italics) +
                ", character=" + toBinaryString(character) +
                ", size=" + toBinaryString(size) +
                ", color=" + toBinaryString(color) +
                ", highlight=" + toBinaryString(highlight) +
                '}';
    }
}
