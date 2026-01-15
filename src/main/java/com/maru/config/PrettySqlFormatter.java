package com.maru.config;

import com.p6spy.engine.spy.appender.MessageFormattingStrategy;
import com.github.vertical_blank.sqlformatter.SqlFormatter;
import com.github.vertical_blank.sqlformatter.core.FormatConfig;
import com.github.vertical_blank.sqlformatter.languages.Dialect;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Custom P6Spy message formatter for pretty SQL output
 */
public class PrettySqlFormatter implements MessageFormattingStrategy {

    private static final DateTimeFormatter TIME_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private static final FormatConfig FORMAT_CONFIG = FormatConfig.builder()
            .indent("  ")
            .uppercase(false)
            .maxColumnLength(80)
            .build();

    @Override
    public String formatMessage(int connectionId, String now, long elapsed,
                                 String category, String prepared, String sql, String url) {
        if (sql == null || sql.trim().isEmpty()) {
            return "";
        }

        String formattedSql = SqlFormatter.of(Dialect.MySql)
                .format(sql, FORMAT_CONFIG);

        // Move commas to the beginning of lines
        formattedSql = moveCommasToLineStart(formattedSql);

        StringBuilder sb = new StringBuilder();
        sb.append("\n");
        sb.append("=".repeat(80)).append("\n");
        sb.append("  [").append(LocalDateTime.now().format(TIME_FORMATTER)).append("] ");
        sb.append("Execution Time: ").append(elapsed).append("ms\n");
        sb.append("-".repeat(80)).append("\n");
        sb.append(formattedSql).append("\n");
        sb.append("=".repeat(80));

        return sb.toString();
    }

    /**
     * Move trailing commas to the beginning of the next line
     * Example: "column1," on line 1 and "column2" on line 2
     *       -> "column1" on line 1 and ", column2" on line 2
     */
    private String moveCommasToLineStart(String sql) {
        String[] lines = sql.split("\n");
        StringBuilder result = new StringBuilder();

        boolean previousLineHadComma = false;

        for (int i = 0; i < lines.length; i++) {
            String line = lines[i];
            String trimmedLine = line.trim();

            if (trimmedLine.isEmpty()) {
                result.append(line).append("\n");
                previousLineHadComma = false;
                continue;
            }

            // Get indentation
            int indentLength = line.length() - line.stripLeading().length();
            String indent = indentLength > 0 ? line.substring(0, indentLength) : "";

            // Check if current line ends with comma
            boolean currentEndsWithComma = trimmedLine.endsWith(",");
            String content = currentEndsWithComma
                    ? trimmedLine.substring(0, trimmedLine.length() - 1)
                    : trimmedLine;

            // Build the line
            if (previousLineHadComma) {
                result.append(indent).append(", ").append(content).append("\n");
            } else {
                result.append(indent).append(content).append("\n");
            }

            previousLineHadComma = currentEndsWithComma;
        }

        return result.toString().trim();
    }
}
