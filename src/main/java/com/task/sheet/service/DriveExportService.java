package com.task.sheet.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.api.services.drive.Drive;
import com.task.sheet.utils.ExcelFileParse;
import org.apache.tomcat.util.net.jsse.JSSEUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URI;
import java.nio.file.*;
import java.text.NumberFormat;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class DriveExportService {

    @Autowired
    private Drive drive;

    @Autowired
    private ExcelFileParse excelFileParse;

    @Value("${google.export.path}")
    private String exportPath;

    @Value("${google.export.summary}")
    private String summaryFile;


    public Path exportSheetXlsx(Path outPath, String spreadsheetId) throws Exception {

        Files.createDirectories(outPath.getParent());
        try (OutputStream out = Files.newOutputStream(outPath)) {
            drive.files()
                    .export(
                            spreadsheetId,
                            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
                    )
                    .executeMediaAndDownloadTo(out);
        }

        return outPath;
    }


    public Map<String, Object> importSheetXlsx(String fileName, String sheetName) throws Exception {
        Map<String, Object> returnMap = new HashMap<>();
        if (!fileName.equalsIgnoreCase("dsrtracker")) {
            Path matchFile = Files.list(Path.of(exportPath))
                    .filter(p -> p.getFileName().toString().toLowerCase().contains(fileName.toLowerCase()))
                    .findFirst()
                    .orElseThrow();
            int skipRow = 0;
            if (sheetName.toLowerCase().contains("zeroactiveadgroup") || sheetName.toLowerCase().contains("zeroactiveads")
                    || sheetName.toLowerCase().contains("wowconversion_gte_5") || sheetName.toLowerCase().contains("zeroactivekeywords")
                    || sheetName.toLowerCase().contains("topconvertingkeywords") || sheetName.toLowerCase().contains("language")
                    || sheetName.toLowerCase().contains("expiredsitelinks") || sheetName.toLowerCase().contains("disapproved ads")
                    || sheetName.contains("output")) {
                skipRow = 1;
            } else if (fileName.equalsIgnoreCase("LocationSettings") || fileName.equalsIgnoreCase("LabelChecks")
                    || fileName.equalsIgnoreCase("ImpressionCheck") || fileName.equalsIgnoreCase("ImageExtensionCheck")) {
                skipRow = 1;
            } else if (sheetName.toLowerCase().contains("check for campaigns")) {
                skipRow = 1;
                sheetName = sheetName + " ";
            } else if (sheetName.contains("OUTPUT")) {
                skipRow = 2;
            } else if (fileName.toLowerCase().contains("negativekeywords")) {
                skipRow = 0;
                sheetName = "output";
            }

            returnMap = excelFileParse.readFile(matchFile.toString(), sheetName, skipRow);
            returnMap.put("summaryData", getSummaryData(fileName, sheetName, returnMap));
        } else {
            Path matchFile = Files.list(Path.of("/opt/automation/archive/"))
                    .filter(p -> p.getFileName().toString().toLowerCase().contains("dsr_output_service_report".toLowerCase()))
                    .findFirst()
                    .orElseThrow();
            returnMap = excelFileParse.readFile(matchFile.toString(), sheetName, 0);

        }

        return returnMap;
    }

    private Object getSummaryData(String fileName, String sheetName, Map<String, Object> sheetData) {
        Map<String, Object> returnMap = new HashMap<>();
        for (String key : sheetData.keySet()) {
            if (key.equalsIgnoreCase("Alert_when_Active_Campaign_budg")) {
                List<Map<String, String>> sheetList = (List<Map<String, String>>) sheetData.get(key);
                if (sheetList.size() > 0) {
                    returnMap.put("summary", campaignBudgetGreaterThanForThousand(sheetList));
                } else {
                    returnMap.put("summary", "No campaigns with budgets exceeding $4000");
                }
            } else if (key.equalsIgnoreCase("ExpiredSitelinks")) {
                List<Map<String, Object>> sheetList = (List<Map<String, Object>>) sheetData.get(key);
                if (sheetList.size() > 0) {
                    returnMap.put("no_account", expiredSitelinkAccount(sheetList));
                    returnMap.put("expired_sitelink", sheetList.size());

                } else {
                    returnMap.put("no_account", 0);
                    returnMap.put("expired_sitelink", 0);
                }
            } else if (key.equalsIgnoreCase("Final1")) {
                List<Map<String, Object>> sheetList = (List<Map<String, Object>>) sheetData.get(key);
                if (sheetList.size() > 0) {
                    returnMap.put("summary", zeroSpendForThreeDays(sheetList));
                } else {
                    returnMap.put("summary", "The system has identified 0 campaigns across 0 accounts with stagnant impression delivery.");
                }
            } else if (key.equalsIgnoreCase("Language")) {
                List<Map<String, Object>> sheetList = (List<Map<String, Object>>) sheetData.get(key);
                if (sheetList.size() > 0) {
                    returnMap.put("summary", "There is " + sheetList.size() + " campaigns across " + sheetList.size() + " accounts targeting either all languages or non-English languages.");
                } else {
                    returnMap.put("summary", "There is 0 campaigns across 0 accounts targeting either all languages or non-English languages.");
                }
            } else if (key.toLowerCase().contains("target")) {
                List<Map<String, Object>> sheetList = (List<Map<String, Object>>) sheetData.get(key);
                if (sheetList.size() > 0) {
                    returnMap.put("summary", "There is " + sheetList.size() + " campaigns across " + sheetList.size() + " accounts targeting either all countries or regions outside the USA and Canada");
                    break;
                } else {
                    returnMap.put("summary", "There is 0 campaigns across 0 accounts targeting either all countries or regions outside the USA and Canada");
                }

            } else if (key.equalsIgnoreCase("Zeroactiveads")) {
                List<Map<String, Object>> sheetList = (List<Map<String, Object>>) sheetData.get(key);
                if (sheetList.size() > 0) {
                    returnMap.put("summary", zeroActiveAdsSummary(sheetList, sheetName));
                }
            } else if (key.equalsIgnoreCase("Zeroactivekeywords")) {
                List<Map<String, Object>> sheetList = (List<Map<String, Object>>) sheetData.get(key);
                if (sheetList.size() > 0) {
                    returnMap.put("summary", zeroActiveAdsSummary(sheetList, key));
                }
            } else if (key.equalsIgnoreCase("ZeroactiveAdGroup")) {
                List<Map<String, Object>> sheetList = (List<Map<String, Object>>) sheetData.get(key);
                if (sheetList.size() > 0) {
                    returnMap.put("summary", zeroActiveAdsSummary(sheetList, key));
                }
            } else if (key.equals("Output")) {
                List<Map<String, Object>> sheetList = (List<Map<String, Object>>) sheetData.get(key);
                if (sheetList.size() > 0) {
                    returnMap.put("summary", sheetList.size() + " accounts analyze for conversion performance differences");
                    returnMap.put("data", conversionPerformanceSummary(sheetList));
//                    returnMap.put("summary",zeroActiveAdsSummary(sheetList, key));
                }
            }
        }
        return returnMap;
    }

    private static final List<String> NON_PERF_KEYS = List.of(
            "< -100%", "-80% to -100%", "-50% to -80%", "-20% to -50%", "0% to -20%"
    );
    private static final List<String> PERF_KEYS = List.of(
            "0% to 20%", "20% to 50%", "50% to 80%", "80% to 100%", "> 100%"
    );

    private Object conversionPerformanceSummary(List<Map<String, Object>> sheetList) {
        Map<String, Long> nonPerf = new LinkedHashMap<>();
        Map<String, Long> perf = new LinkedHashMap<>();
        NON_PERF_KEYS.forEach(k -> nonPerf.put(k, 0L));
        PERF_KEYS.forEach(k -> perf.put(k, 0L));
        for (Map<String, Object> row : sheetList) {
            Object col = row.getOrDefault("Total % Difference", Map.of());
            double v = extractRawDouble(col);

            if (Double.isNaN(v)) continue;

            if (v < 0) {
                String key = nonPerfKey(v);
                nonPerf.computeIfPresent(key, (k, c) -> c + 1);
            } else if (v > 0) {
                String key = perfKey(v);
                perf.computeIfPresent(key, (k, c) -> c + 1);
            } else {
                nonPerf.computeIfPresent("0% to -20%", (k, c) -> c + 1);
            }
        }
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("nonPerforming", nonPerf);
        out.put("performing", perf);

        Long totalNonPerf = 0l;
        Long totalPerf = 0l;
        Long criticalCondition = 0l;
        Long successCondition = 0l;

        for (String k : nonPerf.keySet()) {
            if (k.equalsIgnoreCase("-80% to -100%") || k.equalsIgnoreCase("< -100%")) {
                criticalCondition += nonPerf.get(k);
            }
            totalNonPerf += nonPerf.get(k);
        }
        for (String k : perf.keySet()) {
            if (k.equalsIgnoreCase("80% to 100%") || k.equalsIgnoreCase("> 100%")) {
                successCondition += perf.get(k);
            }
            totalPerf += perf.get(k);
        }
//        out.put("totalNonPerf", totalNonPerf);
//        out.put("totalPerf", totalPerf);
        out.put("performance_distribution", totalPerf + " accounts showing positive conversion growth vs " + totalNonPerf + "accounts with declining performance");
        out.put("critical_attention_needed", criticalCondition + " accounts with severe performance decline (< -80%) require immediate intervention");
        out.put("success_stories", successCondition + " accounts demonstrating exceptional conversion improvement (> 80%) can serve as benchmarks for optimization strategies");

        return out;
    }

    private static String nonPerfKey(double v) {
        if (v <= -1.0) return "< -100%";
        if (v > -1.0 && v <= -0.80) return "-80% to -100%";
        if (v > -0.80 && v <= -0.50) return "-50% to -80%";
        if (v > -0.50 && v <= -0.20) return "-20% to -50%";
        // (-0.20, 0] including 0 handled here
        return "0% to -20%";
    }

    private static String perfKey(double v) {
        if (v > 1.0) return "> 100%";
        if (v > 0.80) return "80% to 100%";
        if (v > 0.50) return "50% to 80%";
        if (v > 0.20) return "20% to 50%";
        return "0% to 20%";
    }

    @SuppressWarnings("unchecked")
    private static double extractRawDouble(Object totalPctDiffCell) {
        try {
            if (totalPctDiffCell instanceof Map<?, ?> m) {
                Object raw = ((Map<String, Object>) m).get("raw");
                if (raw instanceof Number n) return n.doubleValue();
                if (raw instanceof String s && !s.isBlank()) return Double.parseDouble(s);
            } else if (totalPctDiffCell instanceof Number n) {
                return n.doubleValue();
            } else if (totalPctDiffCell instanceof String s && !s.isBlank()) {
                return Double.parseDouble(s);
            }
        } catch (Exception ignored) {
        }
        return Double.NaN;
    }

    private Object zeroActiveAdsSummary(List<Map<String, Object>> sheetList, String sheetName) {
        Map<String, Object> returnMap = new HashMap<>();
        if (sheetName.equalsIgnoreCase("ZeroactiveAdGroup")) {
//            returnMap.put("rawData", zeroActiveAdgroups(sheetList));
            returnMap.put("rawData", zeroAcitveAdsAndKeyword(sheetList));
        } else {
            returnMap.put("rawData", zeroAcitveAdsAndKeyword(sheetList));
        }
        return returnMap;
    }


    private Map<String, Object> zeroAcitveAdsAndKeyword(List<Map<String, Object>> sheetList) {
        Map<String, Object> returnMap = new HashMap<>();
        Set<String> uniqueAccounts = new HashSet<>();
        Set<String> uniqueAdGroups = new HashSet<>();

        Map<String, AccountStats> accountStatsMap = new HashMap<>();

        for (Map<String, Object> row : sheetList) {
            Map<String, Object> accountIdMap = (Map<String, Object>) row.getOrDefault("Customer ID", row.get("Account ID"));
            Map<String, Object> accountNameMap = (Map<String, Object>) row.getOrDefault("Campaign Name", Collections.emptyMap());
            Map<String, Object> adGroupIdMap = (Map<String, Object>) row.get("AdGroup ID");

            String accountId = accountIdMap != null ? Objects.toString(accountIdMap.get("raw"), null) : null;
            String accountName = accountNameMap != null ? Objects.toString(accountNameMap.get("raw"), "") : "";
            String adGroupId = adGroupIdMap != null ? Objects.toString(adGroupIdMap.get("raw"), null) : null;

            if (accountId != null) {
                uniqueAccounts.add(accountId);

                accountStatsMap
                        .computeIfAbsent(accountId, id -> new AccountStats(accountId, accountName))
                        .increment();
            }

            if (adGroupId != null) {
                uniqueAdGroups.add(adGroupId);
            }
        }

        returnMap.put("unique_accounnts", uniqueAccounts.size());
        returnMap.put("unique_adgroups", uniqueAdGroups.size());

        List<AccountStats> top10 = accountStatsMap.values().stream()
                .sorted(Comparator.comparingInt(AccountStats::getAdGroupCount).reversed())
                .limit(10)
                .collect(Collectors.toList());

        returnMap.put("tableData", top10);
        return returnMap;
    }

    private static final Set<String> VALID_ERRORS = Set.of(
            "Model", "Wrong Make", "Missing Make", "Dealer", "New", "Repetition", "Account Name"
    );

    @SuppressWarnings("unchecked")
    private static String getRawString(Object fieldObj) {
        if (fieldObj instanceof Map<?, ?> m) {
            Object raw = ((Map<String, Object>) m).get("raw");
            return raw != null ? String.valueOf(raw) : null;
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    public static List<Map<String, Object>> buildErrorSummary(Map<String, Object> returnMap) {
        Object rowsObj = returnMap.get("Adcopy_check_result");
        if (!(rowsObj instanceof List<?> rows)) {
            return List.of();
        }

        Map<String, Integer> counts = new HashMap<>();

        for (Object rowObj : rows) {
            if (!(rowObj instanceof Map<?, ?> row)) continue;

            // Each field itself is a map with a "raw" key
            String errorRaw = getRawString(row.get("Error"));
            if (!"Yes".equalsIgnoreCase(errorRaw)) continue;

            String errorTypeRaw = getRawString(row.get("Error Type"));
            if (errorTypeRaw == null || errorTypeRaw.equals("--")) continue;

            // Split on commas, trim, drop blanks, keep only valid types
            String[] parts = errorTypeRaw.split(",");
            for (String p : parts) {
                String t = p.trim();
                if (t.isEmpty()) continue;
                if (!VALID_ERRORS.contains(t)) continue;
                counts.merge(t, 1, Integer::sum);
            }
        }

        // Convert to List<Map<String,Object>> as requested
        return counts.entrySet().stream()
                .filter(e -> e.getValue() != 0)
                .sorted(Map.Entry.comparingByKey()) // optional: stable order by name
                .map(e -> {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("error", e.getKey());
                    m.put("errorCount", e.getValue());
                    if(e.getKey().equals("Account Name")) {
                        m.put("summary", "");
                    }else if(e.getKey().equals("Model")) {
                        m.put("summary", "There are "+e.getValue()+" accounts which have wrong model name in their headings or descriptions");
                    }else if(e.getKey().equals("Dealer")){
                        m.put("summary", "There are "+e.getValue()+" accounts which have wrong dealer-name issues in their content");
                    }else if(e.getKey().equals("Wrong Make")){
                        m.put("summary", "There are "+e.getValue()+" accounts which have incorrect make information");
                    }else if(e.getKey().equals("Missing Make")){
                        m.put("summary", "There are "+e.getValue()+" accounts which are missing make information");
                    }else if(e.getKey().equals("New")){
                        m.put("summary", "There are "+e.getValue()+" accounts which have incorrect year information in their content");
                    }else if(e.getKey().equals("Repetition")){
                        m.put("summary", "There are "+e.getValue()+" accounts which have repetitive content issues");
                    }
                    return m;
                })
                .toList();
    }


    public List<Map<String, Object>> adcopySummary() {
        String sheetName = "Adcopy_check_result";
        Map<String, Object> returnMap = new HashMap<>();
        List<Map<String, Object>> returnList = new ArrayList<>();
        try {
            returnMap = excelFileParse.readFile(summaryFile, sheetName, 0);
            returnList = buildErrorSummary(returnMap);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return returnList;
    }

    private class AccountStats {
        private final String accountId;
        private final String accountName;
        private int adGroupCount;

        AccountStats(String accountId, String accountName) {
            this.accountId = accountId;
            this.accountName = accountName;
            this.adGroupCount = 0;
        }

        void increment() {
            this.adGroupCount++;
        }

        public String getAccountId() {
            return accountId;
        }

        public String getAccountName() {
            return accountName;
        }

        public int getAdGroupCount() {
            return adGroupCount;
        }
    }

    @SuppressWarnings("unchecked")
    private Object zeroSpendForThreeDays(List<Map<String, Object>> sheetList) {
        Set<String> uniqueAccounts = sheetList.stream()
                .map(m -> (Map<String, Object>) m.get("Account Name"))
                .filter(Objects::nonNull)
                .map(v -> v.get("raw"))
                .filter(Objects::nonNull)
                .map(Object::toString)
                .collect(Collectors.toSet());

        Set<String> uniqueCampaigns = sheetList.stream()
                .map(m -> (Map<String, Object>) m.get("Campaign ID"))
                .filter(Objects::nonNull)
                .map(v -> v.get("raw"))
                .filter(Objects::nonNull)
                .map(Object::toString)
                .collect(Collectors.toSet());

        return String.format(
                "The system has identified %d campaigns across %d accounts with stagnant impression delivery.",
                uniqueCampaigns.size(),
                uniqueAccounts.size()
        );
//        return null;
    }

    private Object expiredSitelinkAccount(List<Map<String, Object>> sheetList) {
        Set<String> uniqueAccounts = sheetList.stream()
                .map(m -> {
                    Object account = m.get("AccountName");
                    if (account instanceof Map) {
                        return ((Map<?, ?>) account).get("raw").toString();
                    }
                    return null;
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        Map<String, Object> tableMap = new HashMap<>();

        // Count rows by AccountName
        List<Map<String, Object>> sortedList = sheetList.stream()
                .collect(Collectors.groupingBy(
                        m -> getRaw(m, "Account Name"), // group only by AccountName
                        Collectors.counting()          // count how many rows per account
                ))
                .entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue(Comparator.reverseOrder()))
                .limit(3)
                .map(e -> {
                    Map<String, Object> resultRow = new LinkedHashMap<>();
                    resultRow.put("AccountName", e.getKey());
                    resultRow.put("row_count", e.getValue()); // total rows for that account
                    return resultRow;
                })
                .collect(Collectors.toList());

        tableMap.put("top_3_sites", sortedList);


        tableMap.put("top_3_sites", sortedList);
        tableMap.put("unique_accounts", uniqueAccounts.size());
        System.out.println(tableMap);
        return tableMap;
    }

    private Object campaignBudgetGreaterThanForThousand(List<Map<String, String>> sheetList) {
        Set<String> uniqueAccounts = sheetList.stream()
                .map(m -> m.get("account_name"))
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        Set<String> uniqueCampaigns = sheetList.stream()
                .map(m -> m.get("campaign_id"))
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        return String.format(
                "The system has identified %d campaigns across %d accounts with budgets exceeding $4000",
                usNumberformat(uniqueCampaigns.size()),
                usNumberformat(uniqueAccounts.size())
        );
    }

    private Object usNumberformat(int size) {
        NumberFormat formatter = NumberFormat.getInstance(Locale.US);
        String formatted = formatter.format(size);
        return formatted;
    }

    @SuppressWarnings("unchecked")
    private static String getRaw(Map<String, Object> row, String key) {
        Object v = row.get(key);
        if (v instanceof Map) {
            Object r = ((Map<String, Object>) v).get("raw");
            return r == null ? "" : r.toString();
        }
        return v == null ? "" : v.toString();
    }

    @SuppressWarnings("unchecked")
    private static Long getRawLong(Map<String, Object> row, String key) {
        String s = getRaw(row, key).replaceAll("[^0-9]", "");
        try {
            return s.isEmpty() ? 0L : Long.parseLong(s);
        } catch (NumberFormatException e) {
            return 0L;
        }
    }


    public Object getSummarData() {
//        String src = "/opt/automation/input/dsr output service report.xlsx";
//        try {
//            Path copied = duplicateRemovingSpaces(src);
//            System.out.println("File duplicated to: " + copied.toString());
//        } catch (IOException e) {
//            System.err.println("Failed to duplicate file: " + e.getMessage());
//            e.printStackTrace();
//        }
        RestTemplate restTemplate = new RestTemplate();
        URI uri = UriComponentsBuilder
                .fromHttpUrl("http://localhost:5000/summaries?dsrtracker")
                .query("dsrtracker")               // produces ?keywordcheck (no value)
                .build(true)
                .toUri();

        HttpHeaders headers = new HttpHeaders();
        headers.setAccept(List.of(MediaType.APPLICATION_JSON));
        HttpEntity<Void> req = new HttpEntity<>(headers);

        ResponseEntity<Map<String, String>> resp =
                restTemplate.exchange(
                        uri, HttpMethod.GET, req,
                        new ParameterizedTypeReference<Map<String, String>>() {
                        }
                );

        Map<String, String> body = resp.getBody();
//        String htmlSummary = body != null ? body.get("adcopycheck.py") : null;
        Object data = new HashMap<>();
        if (body != null) {
            data = readJsonFile(summaryFile);
        }
        return data;
    }

    private Object readJsonFile(String summaryFile) {
        ObjectMapper mapper = new ObjectMapper();

        try {
            String json = Files.readString(new File(summaryFile).toPath()).trim();

            if (json.startsWith("[")) {
                return mapper.readValue(json, new TypeReference<List<Map<String, Object>>>() {
                });
            } else {
                return mapper.readValue(json, new TypeReference<Map<String, Object>>() {
                });
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to parse JSON file: " + summaryFile, e);
        }
    }

    public static Path duplicateRemovingSpaces(String sourcePathStr) throws IOException {
        Path source = Paths.get(sourcePathStr);
        if (!Files.exists(source) || !Files.isRegularFile(source)) {
            throw new NoSuchFileException("Source file not found or is not a regular file: " + sourcePathStr);
        }

        Path dir = source.getParent();
        if (dir == null) {
            dir = Paths.get(".");
        }

        String fileName = source.getFileName().toString();
        int dot = fileName.lastIndexOf('.');
        String base = (dot > -1) ? fileName.substring(0, dot) : fileName;
        String ext = (dot > -1) ? fileName.substring(dot) : "";

        // sanitize: replace one or more whitespace with underscore
        String baseSanitized = base.replaceAll("\\s+", "_");

        // first candidate
        Path target = dir.resolve(baseSanitized + ext);

        // if exists, try adding suffixes: _copy, _copy2, ...
        if (Files.exists(target)) {
            int counter = 1;
            while (true) {
                String suffix = (counter == 1) ? "_copy" : "_copy" + counter;
                Path candidate = dir.resolve(baseSanitized + suffix + ext);
                if (!Files.exists(candidate)) {
                    target = candidate;
                    break;
                }
                counter++;
            }
        }

        // copy file (will throw IOException on failure)
        Files.copy(source, target, StandardCopyOption.COPY_ATTRIBUTES);

        return target;
    }

}
