package com.example.performanceTesting.shop.csv;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

public final class CsvSupport {

    private static final List<DateTimeFormatter> TIMESTAMP_FORMATTERS = List.of(
        DateTimeFormatter.ISO_OFFSET_DATE_TIME,
        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSSSSS XXX", Locale.ROOT),
        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss XXX", Locale.ROOT),
        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSSSSSXX", Locale.ROOT),
        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ssXX", Locale.ROOT)
    );

    private CsvSupport() {
    }

    public static List<String[]> parseCsvRows(String body) {
        if (body == null || body.isBlank()) {
            return List.of();
        }

        String[] rawLines = body.replace("\r", "").split("\n");
        List<String[]> rows = new ArrayList<>();
        for (String rawLine : rawLines) {
            if (rawLine == null || rawLine.isBlank()) {
                continue;
            }
            rows.add(rawLine.split(",", -1));
        }
        return rows;
    }

    public static List<String[]> stripOptionalHeader(List<String[]> rows, String[] expectedHeader) {
        if (rows.isEmpty()) {
            return rows;
        }

        String[] first = rows.get(0);
        if (first.length != expectedHeader.length) {
            return rows;
        }

        for (int i = 0; i < expectedHeader.length; i++) {
            if (!expectedHeader[i].equalsIgnoreCase(first[i].trim())) {
                return rows;
            }
        }

        return rows.subList(1, rows.size());
    }

    public static String toCsv(String[] header, List<List<Object>> rows) {
        StringBuilder sb = new StringBuilder();
        sb.append(String.join(",", header)).append('\n');

        for (List<Object> row : rows) {
            for (int i = 0; i < row.size(); i++) {
                if (i > 0) {
                    sb.append(',');
                }
                sb.append(escapeCsv(toOutputValue(row.get(i))));
            }
            sb.append('\n');
        }

        return sb.toString();
    }

    public static String nullable(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        if (trimmed.isEmpty() || "NULL".equalsIgnoreCase(trimmed)) {
            return null;
        }
        return trimmed;
    }

    public static Integer asInt(String value) {
        String v = nullable(value);
        return v == null ? null : Integer.parseInt(v);
    }

    public static Long asLong(String value) {
        String v = nullable(value);
        return v == null ? null : Long.parseLong(v);
    }

    public static Boolean asBoolean(String value) {
        String v = nullable(value);
        return v == null ? null : Boolean.parseBoolean(v);
    }

    public static OffsetDateTime asOffsetDateTime(String value) {
        String raw = nullable(value);
        if (raw == null) {
            return null;
        }

        for (DateTimeFormatter formatter : TIMESTAMP_FORMATTERS) {
            try {
                return OffsetDateTime.parse(raw, formatter);
            } catch (DateTimeParseException ignored) {
                // try next formatter
            }
        }

        throw new IllegalArgumentException("Unsupported timestamp format: " + raw);
    }

    private static String toOutputValue(Object value) {
        if (value == null) {
            return "";
        }
        if (value instanceof OffsetDateTime odt) {
            return odt.withOffsetSameInstant(ZoneOffset.UTC).format(DateTimeFormatter.ISO_OFFSET_DATE_TIME);
        }
        return Objects.toString(value, "");
    }

    private static String escapeCsv(String value) {
        if (value == null) {
            return "";
        }
        boolean quote = value.contains(",") || value.contains("\"") || value.contains("\n");
        if (!quote) {
            return value;
        }
        return '"' + value.replace("\"", "\"\"") + '"';
    }
}

