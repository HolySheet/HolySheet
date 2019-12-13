package com.uddernetworks.drivestore.encoding;

import com.google.api.services.docs.v1.model.Color;
import com.google.api.services.docs.v1.model.Dimension;
import com.google.api.services.docs.v1.model.OptionalColor;
import com.google.api.services.docs.v1.model.RgbColor;
import com.google.api.services.docs.v1.model.TextRun;
import com.google.api.services.docs.v1.model.TextStyle;
import com.uddernetworks.drivestore.docs.RequestBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static com.uddernetworks.drivestore.encoding.ByteUtil.getLongBitRange;
import static java.lang.Long.toBinaryString;

public class DocCoder {

    private static final Logger LOGGER = LoggerFactory.getLogger(DocCoder.class);

    public static final char[] TABLE = new char[]{'A', 'B', 'C', 'D', 'E', 'F', 'G', 'H', 'I', 'J', 'K', 'L', 'M', 'N', 'O', 'P', 'Q', 'R', 'S', 'T', 'U', 'V', 'W', 'X', 'Y', 'Z', 'a', 'b', 'c', 'd', 'e', 'f', 'g', 'h', 'i', 'j', 'k', 'l', 'm', 'n', 'o', 'p', 'q', 'r', 's', 't', 'u', 'v', 'w', 'x', 'y', 'z', '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', '+', '/'};
    public static final int[] REV_TABLE = new int[]{-1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, 62, -1, -1, -1, 63, 52, 53, 54, 55, 56, 57, 58, 59, 60, 61, -1, -1, -1, -1, -1, -1, -1, 0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21, 22, 23, 24, 25, -1, -1, -1, -1, -1, -1, 26, 27, 28, 29, 30, 31, 32, 33, 34, 35, 36, 37, 38, 39, 40, 41, 42, 43, 44, 45, 46, 47, 48, 49, 50, 51, -1, -1, -1, -1, -1};

    public static void encodeChunk(RequestBuilder requestBuilder, DataChunk chunk) {
        encodeChunks(requestBuilder, Collections.singletonList(chunk));
    }

    public static void encodeChunks(RequestBuilder requestBuilder, List<DataChunk> chunks) {
        chunks.forEach(chunk -> requestBuilder.addStyledText(encodeChunk(chunk)));
    }

    public static TextRun encodeChunk(DataChunk chunk) {
        return new TextRun()
                .setContent(String.valueOf(TABLE[chunk.getCharacter()]))
                .setTextStyle(new TextStyle()
                        .setBold(chunk.getBold() == 1)
                        .setUnderline(chunk.getUnderline() == 1)
                        .setItalic(chunk.getItalics() == 1)
                        .setFontSize(new Dimension().setUnit("PT").setMagnitude((double) chunk.getSize()))
                        .setForegroundColor(optionalColorFromLong(chunk.getColor()))
                        .setBackgroundColor(optionalColorFromLong(chunk.getHighlight())));
    }

    public static Optional<DataChunk> decodeChunk(TextRun textRun) {
        var style = textRun.getTextStyle();
        if (style == null) {
            return Optional.of(new DataChunk((byte) 0, (byte) 0, (byte) 0, (short) REV_TABLE[textRun.getContent().charAt(0)], (short) 0, 0, 0));
        }

        return Optional.of(new DataChunk(
                boolToByte(style.getBold()),
                boolToByte(style.getUnderline()),
                boolToByte(style.getItalic()),
                (short) REV_TABLE[textRun.getContent().charAt(0)],
                getMagnitude(style.getFontSize()),
                longFromColor(style.getForegroundColor()),
                longFromColor(style.getBackgroundColor())
        ));
    }

    private static byte boolToByte(Boolean bool) {
        if (bool == null) return 0;
        return (byte) (bool ? 1 : 0);
    }

    private static short getMagnitude(Dimension dimension) {
        if (dimension == null) return 0;
        return dimension.getMagnitude().shortValue();
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

    private static long longFromColor(OptionalColor color) {
        if (color == null) return 0;
        return longFromColor(color.getColor());
    }

    private static long longFromColor(Color color) {
        if (color == null) return 0;
        var rgb = color.getRgbColor();
        long r = (long) (getSafe(rgb.getRed()) * 255D);
        long g = (long) (getSafe(rgb.getGreen()) * 255D);
        long b = (long) (getSafe(rgb.getBlue()) * 255D);
        r |= g << 8;
        r |= b << 16;
        return r;
    }

    private static float getSafe(Float f) {
        return f == null ? 0 : f;
    }

}
