package org.jbehave.core.model;

import static java.util.stream.Collectors.joining;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;

import org.jbehave.core.model.ExamplesTable.TableProperties;
import org.jbehave.core.model.TableTransformers.TableTransformer;

/**
 * <p>
 * Facade responsible for transforming table string representations. It allows
 * the registration of several {@link TableTransformer} instances by name.
 * </p>
 * <p>
 * Some Transformers are provided out-of-the-box:
 * <ul>
 * <li>{@link TableTransformers.FromLandscape FromLandscape}: registered under
 * name {@link TableTransformers#FROM_LANDSCAPE}</li>
 * <li>{@link TableTransformers.Formatting Formatting}: registered under name
 * {@link TableTransformers#FORMATTING}</li>
 * <li>{@link TableTransformers.Replacing Replacing}: registered under name
 * {@link TableTransformers#REPLACING}</li>
 * </ul>
 * </p>
 */
public class TableTransformers {

    public static final String FROM_LANDSCAPE = "FROM_LANDSCAPE";
    public static final String FORMATTING = "FORMATTING";
    public static final String REPLACING = "REPLACING";

    private final Map<String, TableTransformer> transformers = new HashMap<>();

    public TableTransformers() {
        useTransformer(FROM_LANDSCAPE, new FromLandscape());
        useTransformer(FORMATTING, new Formatting());
        useTransformer(REPLACING, new Replacing());
    }

    public String transform(String transformerName, String tableAsString, TableParsers tableParsers,
            TableProperties properties) {
        TableTransformer transformer = transformers.get(transformerName);
        if (transformer != null) {
            return transformer.transform(tableAsString, tableParsers, properties);
        }
        return tableAsString;
    }

    public void useTransformer(String name, TableTransformer transformer) {
        transformers.put(name, transformer);
    }

    public interface TableTransformer {
        String transform(String tableAsString, TableParsers tableParsers, TableProperties properties);
    }

    public static class FromLandscape implements TableTransformer {

        @Override
        public String transform(String tableAsString, TableParsers tableParsers, TableProperties properties) {
            Map<String, List<String>> data = new LinkedHashMap<>();
            for (String rowAsString : tableAsString.split(properties.getRowSeparator())) {
                if (ignoreRow(rowAsString, properties.getIgnorableSeparator())) {
                    continue;
                }
                List<String> values = tableParsers.parseRow(rowAsString, false, properties);
                String header = values.get(0);
                List<String> rowValues = new ArrayList<>(values);
                rowValues.remove(0);
                data.put(header, rowValues);
            }

            if (data.values().stream().mapToInt(List::size).distinct().count() != 1) {
                String errorMessage = data.entrySet()
                        .stream()
                        .map(e -> {
                            int numberOfCells = e.getValue().size();
                            StringBuilder rowDescription = new StringBuilder(e.getKey())
                                    .append(" -> ")
                                    .append(numberOfCells)
                                    .append(" cell");
                            if (numberOfCells > 1) {
                                rowDescription.append('s');
                            }
                            return rowDescription.toString();
                        })
                        .collect(joining(", ", "The table rows have unequal numbers of cells: ", ""));
                throw new IllegalArgumentException(errorMessage);
            }

            StringBuilder builder = new StringBuilder();
            builder.append(properties.getHeaderSeparator());
            for (String header : data.keySet()) {
                builder.append(header).append(properties.getHeaderSeparator());
            }
            builder.append(properties.getRowSeparator());
            int numberOfCells = data.values().iterator().next().size();
            for (int c = 0; c < numberOfCells; c++) {
                builder.append(properties.getValueSeparator());
                for (List<String> row : data.values()) {
                    builder.append(row.get(c)).append(properties.getValueSeparator());
                }
                builder.append(properties.getRowSeparator());
            }
            return builder.toString();
        }

        private boolean ignoreRow(String rowAsString, String ignorableSeparator) {
            return rowAsString.startsWith(ignorableSeparator)
                    || rowAsString.length() == 0;
        }

    }

    public static class Formatting implements TableTransformer {

        @Override
        public String transform(String tableAsString, TableParsers tableParsers, TableProperties properties) {
            List<List<String>> data = new ArrayList<>();
            for (String rowAsString : tableAsString.split(properties.getRowSeparator())) {
                if (ignoreRow(rowAsString, properties.getIgnorableSeparator())) {
                    continue;
                }
                data.add(tableParsers.parseRow(rowAsString, rowAsString.contains(properties.getHeaderSeparator()),
                        properties));
            }

            StringBuilder builder = new StringBuilder();
            Map<Integer, Integer> maxWidths = maxWidth(data);
            for (int r = 0; r < data.size(); r++) {
                String formattedRow = formatRow(data.get(r), maxWidths,
                        r == 0 ? properties.getHeaderSeparator() : properties.getValueSeparator());
                builder.append(formattedRow).append(properties.getRowSeparator());
            }
            return builder.toString();
        }

        private boolean ignoreRow(String rowAsString, String ignorableSeparator) {
            return rowAsString.startsWith(ignorableSeparator)
                    || rowAsString.length() == 0;
        }

        private Map<Integer, Integer> maxWidth(List<List<String>> data) {
            Map<Integer, Integer> maxWidths = new HashMap<>();
            for (List<String> row : data) {
                for (int c = 0; c < row.size(); c++) {
                    String cell = row.get(c).trim();
                    Integer width = maxWidths.get(c);
                    int length = cell.length();
                    if (width == null || length > width) {
                        width = length;
                        maxWidths.put(c, width);
                    }
                }
            }

            return maxWidths;
        }

        private String formatRow(List<String> row,
                Map<Integer, Integer> maxWidths, String separator) {
            StringBuilder builder = new StringBuilder();
            builder.append(separator);
            for (int c = 0; c < row.size(); c++) {
                builder.append(formatValue(row.get(c).trim(), maxWidths.get(c)))
                        .append(separator);
            }
            return builder.toString();
        }

        private String formatValue(String value, int width) {
            if (value.length() < width) {
                return value + padding(width - value.length());
            }
            return value;
        }

        private String padding(int size) {
            StringBuilder builder = new StringBuilder();
            for (int i = 0; i < size; i++) {
                builder.append(' ');
            }
            return builder.toString();
        }

    }

    public static class Replacing implements TableTransformer {

        @Override
        public String transform(String tableAsString, TableParsers tableParsers, TableProperties properties) {
            String replacing = properties.getProperties().getProperty("replacing");
            String replacement = properties.getProperties().getProperty("replacement");
            if (replacing == null || replacement == null) {
                return tableAsString;
            }
            return tableAsString.replace(replacing, replacement);
        }
    }
}
