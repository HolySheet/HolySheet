package com.uddernetworks.drivestore.encoding;

import com.google.api.services.docs.v1.model.Color;
import com.google.api.services.docs.v1.model.Dimension;
import com.google.api.services.docs.v1.model.OptionalColor;
import com.google.api.services.docs.v1.model.RgbColor;
import com.google.api.services.docs.v1.model.TextStyle;
import com.uddernetworks.drivestore.docs.RequestBuilder;

import java.util.Collections;
import java.util.List;

import static com.uddernetworks.drivestore.encoding.ByteUtil.getLongBitRange;

public class DocEncoder {

    public static final char[] TABLE = new char[]{'A', 'B', 'C', 'D', 'E', 'F', 'G', 'H', 'I', 'J', 'K', 'L', 'M', 'N', 'O', 'P', 'Q', 'R', 'S', 'T', 'U', 'V', 'W', 'X', 'Y', 'Z', 'a', 'b', 'c', 'd', 'e', 'f', 'g', 'h', 'i', 'j', 'k', 'l', 'm', 'n', 'o', 'p', 'q', 'r', 's', 't', 'u', 'v', 'w', 'x', 'y', 'z', '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', '+', '/'};
    public static final int[] REV_TABLE = new int[]{-1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, 62, -1, -1, -1, 63, 52, 53, 54, 55, 56, 57, 58, 59, 60, 61, -1, -1, -1, -1, -1, -1, -1, 0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21, 22, 23, 24, 25, -1, -1, -1, -1, -1, -1, 26, 27, 28, 29, 30, 31, 32, 33, 34, 35, 36, 37, 38, 39, 40, 41, 42, 43, 44, 45, 46, 47, 48, 49, 50, 51, -1, -1, -1, -1, -1};

    public static void encodeChunk(RequestBuilder requestBuilder, DataChunk chunk) {
        encodeChunks(requestBuilder, Collections.singletonList(chunk));
    }

    public static void encodeChunks(RequestBuilder requestBuilder, List<DataChunk> chunks) {
        chunks.forEach(chunk ->
                requestBuilder.addStyledText(String.valueOf(TABLE[chunk.getCharacter()]), new TextStyle()
                        .setBold(chunk.getBold() == 1)
                        .setUnderline(chunk.getUnderline() == 1)
                        .setItalic(chunk.getItalics() == 1)
                        .setFontSize(new Dimension().setUnit("PT").setMagnitude((double) chunk.getSize()))
                        .setForegroundColor(optionalColorFromLong(chunk.getColor()))
                        .setBackgroundColor(optionalColorFromLong(chunk.getHighlight()))
                ));
    }

    private static OptionalColor optionalColorFromLong(long number) {
        return new OptionalColor().setColor(colorFromLong(number));
    }

    private static Color colorFromLong(long number) {
        float r = getLongBitRange(number, 0, 8) / 255F;
        float g = getLongBitRange(number, 8, 8) / 255F;
        float b = getLongBitRange(number, 16, 8) / 255F;
        return new Color().setRgbColor(new RgbColor().setRed(r).setGreen(g).setBlue(b));
    }

}
