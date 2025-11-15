package com.task.sheet.controller;

import com.task.sheet.service.DriveExportService;
import jakarta.servlet.http.HttpServletRequest;
import org.apache.http.impl.bootstrap.HttpServer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.web.bind.annotation.*;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

import static java.util.Map.entry;

@RestController
@RequestMapping("/export")
public class ExportController {

    private final DriveExportService service;

    @Value("${google.export.path}")
    private String exportPath;

    public ExportController(DriveExportService service) {
        this.service = service;
    }

    @Scheduled(fixedDelay = 30 * 60 * 1000, initialDelay = 0)
    public void testing() throws Exception {
        Map<String, String> response = Map.ofEntries(entry("1yonm6OXmVy9jsM02xXm6LuFPs029KFcKUb8yh50HHM8", exportPath + "zeroActive.xlsx"), entry("1xencCawyYXgJF0BWhkvVh1Ls4mUWNW-NjpWfjbVL5Cc", exportPath + "WOWConversion.xlsx"), entry("1uKsauKif9MmrTxFH9iLntkv2n8HwrYAGXunspDZTCjM", exportPath + "LocationSettings.xlsx"), entry("1J8h1-xCCv80H7HPQm3aP6YbbODoBNW7eq1Q9budKG18", exportPath + "LanguageSettings.xlsx"), entry("1qautdux00JtUQCa14X93a4gLPcFz4oloTGA04tGdur8", exportPath + "LabelChecks.xlsx"), entry("1WK-qYOcsr-RqtgnGaectJEFSbKhgq1jkB66ZqNnlj9M", exportPath + "ImpressionCheck.xlsx"), // (typo?)
                entry("1GKrOAhDvJfiODWMtcxoHQBaFD7AuzUio8L6qg52hrbQ", exportPath + "ImageExtensionCheck.xlsx"), entry("1Y15_0kcuny-grPtS__3jyyRuMboGB9xM6FW7Wc-_wVM", exportPath + "ExpiredSitelinks.xlsx"), entry("1jc3ntAcs8n3IecNrAeao2shP7NkGyd31dmaWgtX65Dw", exportPath + "DisapprovedAds.xlsx"), entry("17B4kZu9N0gypCHys_TZoYnjkuUN1iX1E5cei_ItUDCQ", exportPath + "ConversionCheck.xlsx"), entry("1LblCpYo9XzzXllqCC_2fuRPPD2rMq0El2Ec3LjJqY3g", exportPath + "CampaignSettings.xlsx"), entry("1wBxdVG8RRRYrdbABeCdkKY0p78162EIR74dCS24BF7o", exportPath + "AdGroupSettings.xlsx"), entry("1ANGlFnZ6saQpwaueyA7hmdr618eWJcyfGPJhrWd4efg", exportPath + "NegativeKeywords.xlsx"));
        for (String spreadSheetId : response.keySet()) {
            service.exportSheetXlsx(Path.of(response.get(spreadSheetId)), spreadSheetId);
        }
    }


    @GetMapping("/fetchSheet")
    public ResponseEntity<?> fetchSheet(HttpServletRequest request) throws Exception {
        String spreadSheet = request.getParameter("spreadSheet");
        String sheetName = request.getParameter("sheetName");
        return ResponseEntity.ok(service.importSheetXlsx(spreadSheet, sheetName));
    }


    @GetMapping("/dsrSummary")
    public ResponseEntity<?> dsrSummary(HttpServletRequest request) throws Exception {
        return ResponseEntity.ok(service.adcopySummary());
//        return ResponseEntity.ok(service.getSummarData());
    }

}
