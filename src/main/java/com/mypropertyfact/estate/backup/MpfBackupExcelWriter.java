package com.mypropertyfact.estate.backup;

import org.apache.poi.ss.SpreadsheetVersion;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.IOException;
import java.io.OutputStream;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public final class MpfBackupExcelWriter {

    /** Excel .xlsx max characters per cell (POI / SpreadsheetVersion.EXCEL2007). */
    private static final int MAX_CELL_TEXT_LENGTH = SpreadsheetVersion.EXCEL2007.getMaxTextLength();

    private static final String TRUNCATION_SUFFIX = "\n…[truncated — Excel max 32,767 chars per cell]";

    private MpfBackupExcelWriter() {}

    public static void writeWorkbook(Path outputFile, String sheetName, List<String> headers, List<List<Object>> rows)
            throws IOException {
        Files.createDirectories(outputFile.getParent());
        try (Workbook workbook = new XSSFWorkbook();
                OutputStream out = Files.newOutputStream(outputFile)) {
            Sheet sheet = workbook.createSheet(sheetName);
            Row headerRow = sheet.createRow(0);
            for (int c = 0; c < headers.size(); c++) {
                Cell cell = headerRow.createCell(c);
                cell.setCellValue(headers.get(c));
            }
            int r = 1;
            for (List<Object> rowData : rows) {
                Row row = sheet.createRow(r++);
                for (int c = 0; c < rowData.size(); c++) {
                    setCellValue(row.createCell(c), rowData.get(c));
                }
            }
            for (int c = 0; c < headers.size(); c++) {
                sheet.autoSizeColumn(c);
            }
            workbook.write(out);
        }
    }

    public static <T> void writeDtoWorkbook(Path outputFile, String sheetName, List<T> items, Class<T> type)
            throws IOException {
        List<String> headers = new ArrayList<>();
        List<Field> fields = new ArrayList<>();
        for (Field f : type.getDeclaredFields()) {
            if (java.lang.reflect.Modifier.isStatic(f.getModifiers())) {
                continue;
            }
            f.setAccessible(true);
            headers.add(f.getName());
            fields.add(f);
        }
        List<List<Object>> rows = new ArrayList<>();
        for (T item : items) {
            List<Object> row = new ArrayList<>();
            for (Field f : fields) {
                try {
                    row.add(f.get(item));
                } catch (IllegalAccessException e) {
                    row.add(null);
                }
            }
            rows.add(row);
        }
        writeWorkbook(outputFile, sheetName, headers, rows);
    }

    private static void setCellValue(Cell cell, Object value) {
        if (value == null) {
            cell.setBlank();
        } else if (value instanceof Number n) {
            cell.setCellValue(n.doubleValue());
        } else if (value instanceof Boolean b) {
            cell.setCellValue(b);
        } else {
            cell.setCellValue(toExcelCellText(value));
        }
    }

    /** Keeps cell text within Excel's per-cell limit so export does not fail. */
    static String toExcelCellText(Object value) {
        String text = String.valueOf(value);
        if (text.length() <= MAX_CELL_TEXT_LENGTH) {
            return text;
        }
        int keep = MAX_CELL_TEXT_LENGTH - TRUNCATION_SUFFIX.length();
        if (keep < 0) {
            return text.substring(0, MAX_CELL_TEXT_LENGTH);
        }
        return text.substring(0, keep) + TRUNCATION_SUFFIX;
    }

    public static boolean exceedsExcelCellLimit(String text) {
        return text != null && text.length() > MAX_CELL_TEXT_LENGTH;
    }
}
