package com.uddernetworks.holysheet.console;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

public class ConsoleTableBuilder {

    private static final Logger LOGGER = LoggerFactory.getLogger(ConsoleTableBuilder.class);

    private int horizontalSpacing = 1;
    private Map<String, Integer> columns = new LinkedHashMap<>();

    public ConsoleTableBuilder addColumn(String title, int width) {
        if (title.length() > width) {
            LOGGER.error("Title width can not be more than the set width");
            return this;
        }

        columns.put(title, width);
        return this;
    }

    public String generateTable(List<List<String>> rows) {
        return generateTable(rows, null);
    }

    public String generateTable(List<List<String>> rows, List<String> total) {
        var builder = new StringBuilder();
        var temp = new StringBuilder();
        columns.forEach((title, width) -> {
            builder.append(paddedRight(title, width)).append(" ".repeat(horizontalSpacing));
            temp.append("-".repeat(width)).append(" ".repeat(horizontalSpacing));
        });
        builder.append("\n").append(temp).append("\n");

        if (total != null && rows.size() > 0 && total.size() == rows.get(0).size()) {
            rows = new ArrayList<>(rows);
            rows.addAll(Arrays.asList(null, total));
        }

        var columnWidths = columns.values().toArray(Integer[]::new);

        rows.forEach(row -> {
            if (row == null) {
                builder.append('\n');
                return;
            }

            if (row.size() != columns.size()) {
                LOGGER.error("Columns do not match! ({} and {})", row.size(), columns.size());
                return;
            }

            var i = new AtomicInteger();
            row.stream().map(str -> str == null ? "" : str).map(column ->
                    limit(column, columnWidths[i.getAndIncrement()]) + " ".repeat(horizontalSpacing))
                    .forEach(builder::append);
            builder.append("\n");
        });

        return builder.toString();
    }

    public int getHorizontalSpacing() {
        return horizontalSpacing;
    }

    public ConsoleTableBuilder setHorizontalSpacing(int horizontalSpacing) {
        this.horizontalSpacing = horizontalSpacing;
        return this;
    }

    private String limit(String string, int amount) {
        if (string.length() <= amount) {
            return paddedRight(string, amount);
        }

        return string.substring(0, amount - 3) + "...";
    }

    public static String paddedLeft(String string, int length) {
        return String.format("%1$"+length+ "s", string);
    }

    public static String paddedRight(String string, int length) {
        return String.format("%1$-"+length+ "s", string);
    }

}
