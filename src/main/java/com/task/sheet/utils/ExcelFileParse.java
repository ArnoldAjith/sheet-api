package com.task.sheet.utils;

import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.services.sheets.v4.Sheets;
import com.google.api.services.sheets.v4.SheetsScopes;
import com.google.api.services.sheets.v4.model.ValueRange;
import com.google.auth.http.HttpCredentialsAdapter;
import com.google.auth.oauth2.ServiceAccountCredentials;
import org.apache.poi.ss.formula.eval.NotImplementedFunctionException;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.usermodel.Row.MissingCellPolicy;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

import java.io.*;
import java.security.GeneralSecurityException;
import java.time.LocalDateTime;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

@Component
public class ExcelFileParse {

    @Value("${google.credentials.json-path}")
    private String jsonPath;

    public Map<String, Object> readFile(String filePath, String sheetNameReq, int headerRowIdx)
            throws IOException {
        System.out.println(filePath);
        System.out.println(sheetNameReq);
        Map<String, Object> out = new HashMap<>();

        try (FileInputStream fis = new FileInputStream(new File(filePath));
             XSSFWorkbook wb = new XSSFWorkbook(fis)) {

            final DataFormatter formatter = new DataFormatter(Locale.getDefault());
            final FormulaEvaluator evaluator = wb.getCreationHelper().createFormulaEvaluator();

            boolean matchedNamedSheet = false;
            for (int i = 0; i < wb.getNumberOfSheets(); i++) {
                XSSFSheet sheet = wb.getSheetAt(i);
                String sheetName = sheet.getSheetName();

                if (sheetName.equalsIgnoreCase(sheetNameReq)) {
                    matchedNamedSheet = true;
                    sheetName = sheetName.replaceAll(" ", "_");
                        out.put(sheetName, readSheet(sheet, headerRowIdx, formatter, evaluator));

                    break;
                }
            }

            // Fallback: if no named sheet matched and filename suggests "read all sheets"
            if (!matchedNamedSheet && shouldReadAllSheets(filePath)) {
                for (int i = 0; i < wb.getNumberOfSheets(); i++) {
                    XSSFSheet sheet = wb.getSheetAt(i);
                    String sheetName = sheet.getSheetName();
                    sheetName = sheetName.replaceAll(" ", "_");
                    out.put(sheetName, readSheet(sheet, headerRowIdx, formatter, evaluator));
                }
            }
        }

        if (out.keySet().contains("Final")) {
            out.remove("Final");
            out.remove("FinalSheet");
        }else if(out.keySet().contains("PMax")){
            out.remove("Video2");
            out.remove("Search2");
            out.remove("Search3");
            out.remove("Display2");
            out.remove("VLA");
            out.remove("VLA2");
            out.remove("PMax");
            out.remove("PMax2");
            out.remove("Video");
            out.remove("Search");
            out.remove("Display");
        }
        return out;
    }

    private boolean shouldReadAllSheets(String filePath) {
        String f = filePath.toLowerCase();
        return f.contains("locationsettings")
                || f.contains("labelchecks")
                || f.contains("impressioncheck")
                || f.contains("imageextensioncheck");
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> readSheet(
            Sheet sheet,
            int headerRowIdx,
            DataFormatter formatter,
            FormulaEvaluator evaluator
    ) {
        List<Map<String, Object>> rows = new ArrayList<>();

        Row headerRow = sheet.getRow(headerRowIdx);
        if (headerRow == null) return rows;

        List<String> headers = extractHeaders(headerRow, formatter);
        if (headers.isEmpty()) return rows;

        int lastRow = sheet.getLastRowNum();
        for (int r = headerRowIdx + 1; r <= lastRow; r++) {
            Row row = sheet.getRow(r);
            if (isRowBlank(row)) continue;

            Map<String, Object> rowMap = new LinkedHashMap<>();
            for (int c = 0; c < headers.size(); c++) {
                String header = headers.get(c);
                Cell cell = (row == null) ? null : row.getCell(c, MissingCellPolicy.RETURN_BLANK_AS_NULL);

                // Build a rich value: raw + display + format string
                Map<String, Object> cellOut = buildCellOutput(cell, formatter, evaluator);
                rowMap.put(header, cellOut);
            }
            rows.add(rowMap);
        }

        return rows;
    }

    private boolean isRowBlank(Row row, int fromColInclusive, int toColExclusive) {
        if (row == null) return true;
        for (int c = Math.max(fromColInclusive, 0); c < toColExclusive; c++) {
            Cell cell = row.getCell(c, MissingCellPolicy.RETURN_BLANK_AS_NULL);
            if (!isCellBlank(cell)) return false;
        }
        return true;
    }


    private Map<String, Object> buildCellOutput(Cell cell, DataFormatter formatter, FormulaEvaluator evaluator) {
        Map<String, Object> m = new LinkedHashMap<>(3);

        // 1) Excel-style displayed text (what users see in Excel)
        String display = formatAsDisplayed(cell, formatter, evaluator);

        // 2) Raw, strongly-typed value (preserves your existing behavior)
        Object raw = parseCellRaw(cell, formatter, evaluator);

        // 3) The Excel data format string for transparency/debugging
        String fmt = null;
        if (cell != null) {
            CellStyle style = cell.getCellStyle();
            if (style != null) {
                fmt = style.getDataFormatString();
            }
        }

        m.put("raw", raw);
        m.put("display", (display == null || display.isEmpty()) ? null : display);
        m.put("format", fmt);
        return m;
    }

    private String formatAsDisplayed(Cell cell, DataFormatter formatter, FormulaEvaluator evaluator) {
        if (cell == null) return null;

        // Use evaluator for normal formulas; for UDFs rely on cached result by passing null evaluator
        if (cell.getCellType() == CellType.FORMULA) {
            String formula = cell.getCellFormula();
            boolean looksLikeUdf = formula.contains("__xludf.") || formula.startsWith("_xlfn.");
            try {
                if (looksLikeUdf) {
                    return trimToNull(formatter.formatCellValue(cell)); // uses cached result
                } else {
                    return trimToNull(formatter.formatCellValue(cell, evaluator));
                }
            } catch (RuntimeException e) {
                // Any evaluation issue: fall back to cached text
                return trimToNull(formatter.formatCellValue(cell));
            }
        }

        // Non-formula cells:
        try {
            return trimToNull(formatter.formatCellValue(cell));
        } catch (RuntimeException e) {
            return null;
        }
    }

    private String trimToNull(String s) {
        if (s == null) return null;
        s = s.trim();
        return s.isEmpty() ? null : s;
    }

    private List<String> extractHeaders(Row headerRow, DataFormatter formatter) {
        int lastCell = headerRow.getLastCellNum(); // may be -1 if no cells
        if (lastCell < 0) return Collections.emptyList();

        List<String> headers = new ArrayList<>(lastCell);
        Map<String, Integer> seen = new HashMap<>();

        for (int c = 0; c < lastCell; c++) {
            Cell cell = headerRow.getCell(c, MissingCellPolicy.RETURN_BLANK_AS_NULL);
            String raw = (cell == null) ? "" : formatter.formatCellValue(cell).trim();
            String header = raw.isEmpty() ? ("Column_" + (c + 1)) : raw;

            // Ensure uniqueness (duplicate headers → "Header", "Header (2)", ...)
            int count = seen.getOrDefault(header, 0) + 1;
            seen.put(header, count);
            headers.add(count == 1 ? header : header + " (" + count + ")");
        }
        // Trim trailing empty headers
        int lastNonEmpty = headers.size() - 1;
        while (lastNonEmpty >= 0 && headers.get(lastNonEmpty).startsWith("Column_")) {
            lastNonEmpty--;
        }
        return headers.subList(0, Math.max(lastNonEmpty + 1, 0));
    }

    private boolean isRowBlank(Row row) {
        if (row == null) return true;
        short lastCell = row.getLastCellNum();
        if (lastCell < 0) return true;
        for (int c = row.getFirstCellNum(); c < lastCell; c++) {
            if (c < 0) continue;
            Cell cell = row.getCell(c, MissingCellPolicy.RETURN_BLANK_AS_NULL);
            if (!isCellBlank(cell)) return false;
        }
        return true;
    }

    private boolean isCellBlank(Cell cell) {
        if (cell == null) return true;
        CellType type = cell.getCellType();
        if (type == CellType.BLANK) return true;
        if (type == CellType.STRING) return cell.getStringCellValue().trim().isEmpty();
        return false;
    }

    /**
     * Raw parsing (no display formatting). Preserves your previous return types:
     * - LocalDateTime if Excel date/time
     * - Long if numeric and integral, else Double
     * - Boolean
     * - String (trimmed) if text
     * - null for blank/error
     * - Formula cells are evaluated, with graceful fallbacks to cached results
     */
    private Object parseCellRaw(Cell cell, DataFormatter formatter, FormulaEvaluator evaluator) {
        if (cell == null) return null;

        CellType type = cell.getCellType();
        if (type == CellType.FORMULA) {
            String formula = cell.getCellFormula();
            boolean looksLikeUdf = formula.contains("__xludf.") || formula.startsWith("_xlfn.");

            if (looksLikeUdf) {
                // Do NOT evaluate; use cached result
                return parseCachedFormulaValue(cell, formatter);
            }

            try {
                // Try to evaluate normal formulas
                type = evaluator.evaluateFormulaCell(cell);
            } catch (NotImplementedFunctionException e) {
                // Fallback to cached result if evaluator can't handle it
                return parseCachedFormulaValue(cell, formatter);
            } catch (RuntimeException e) {
                // Any other evaluation failure -> cached result fallback
                return parseCachedFormulaValue(cell, formatter);
            }
        }

        switch (type) {
            case NUMERIC:
                if (DateUtil.isCellDateFormatted(cell)) {
                    // Prefer LocalDateTime (handles date & time)
                    try {
                        return cell.getLocalDateTimeCellValue();
                    } catch (Exception ignored) {
                        // Fallback for older POI/edge cases: convert from java.util.Date
                        return toLocalDateTime(cell.getDateCellValue());
                    }
                } else {
                    double d = cell.getNumericCellValue();
                    // If integer-like, store as Long
                    if (isIntegral(d)) {
                        long asLong = (long) Math.floor(d);
                        return asLong;
                    }
                    return d; // keep as Double
                }
            case STRING: {
                String s = cell.getStringCellValue();
                s = (s == null) ? null : s.trim();
                return (s == null || s.isEmpty()) ? null : s;
            }
            case BOOLEAN:
                return cell.getBooleanCellValue();
            case BLANK:
            case ERROR:
            default:
                return null;
        }
    }

    private boolean isIntegral(double value) {
        // Exactly integral within double precision (Excel stores ints as doubles)
        return Math.floor(value) == value
                && value >= Long.MIN_VALUE
                && value <= Long.MAX_VALUE;
    }

    private LocalDateTime toLocalDateTime(Date date) {
        return (date == null) ? null : LocalDateTime.ofInstant(date.toInstant(), java.time.ZoneId.systemDefault());
    }

    private Object parseCachedFormulaValue(Cell cell, DataFormatter formatter) {
        CellType cached = cell.getCachedFormulaResultType();

        switch (cached) {
            case NUMERIC:
                if (DateUtil.isCellDateFormatted(cell)) {
                    try {
                        return cell.getLocalDateTimeCellValue();
                    } catch (Exception ignored) {
                        return toLocalDateTime(cell.getDateCellValue());
                    }
                } else {
                    double d = cell.getNumericCellValue();
                    return isIntegral(d) ? (long) Math.floor(d) : d;
                }
            case STRING: {
                String s = cell.getStringCellValue();
                s = (s == null) ? null : s.trim();
                return (s == null || s.isEmpty()) ? null : s;
            }
            case BOOLEAN:
                return cell.getBooleanCellValue();
            case ERROR:
            case BLANK:
            default:
                // As a last resort, give formatted text (won't re-evaluate)
                String text = formatter.formatCellValue(cell);
                text = (text == null) ? null : text.trim();
                return (text == null || text.isEmpty()) ? null : text;
        }
    }

    public ResponseEntity<?> insertionSheet() {
        try (FileInputStream serviceAccountStream = new FileInputStream(jsonPath)) {

            var credentials = ServiceAccountCredentials.fromStream(serviceAccountStream)
                    .createScoped(Collections.singleton(SheetsScopes.SPREADSHEETS));

            Sheets sheetsService = new Sheets.Builder(
                    GoogleNetHttpTransport.newTrustedTransport(),
                    JacksonFactory.getDefaultInstance(),
                    new HttpCredentialsAdapter(credentials)
            )
                    .setApplicationName("Sitelink Extension Input")
                    .build();

            List<List<Object>> values = Arrays.asList(
                    Arrays.asList("Name", "Department", "Hours Worked"),
                    Arrays.asList("Ajith", "Backend", "8"),
                    Arrays.asList("Rahul", "Frontend", "7")
            );

            ValueRange body = new ValueRange().setValues(values);

            sheetsService.spreadsheets().values()
                    .update("1OrilM1OTBXFPcaW3APhTx91WNcgP8s5nt32zseMbqGY", "Input!A1", body)
                    .setValueInputOption("RAW")
                    .execute();

            return ResponseEntity.ok("✅ Data inserted successfully into Google Sheet!");

        } catch (FileNotFoundException e) {
//            LOGGER.log(Level.SEVERE, "Service account JSON file not found: " + jsonPath, e);
            return ResponseEntity.internalServerError().body("❌ File not found: " + jsonPath);
        } catch (GeneralSecurityException | IOException e) {
//            LOGGER.log(Level.SEVERE, "Error while writing to Google Sheet", e);
            return ResponseEntity.internalServerError().body("❌ Failed to insert data: " + e.getMessage());
        }
    }
}
