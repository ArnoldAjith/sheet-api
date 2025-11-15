package com.task.sheet.controller;

import com.task.sheet.service.UrlScrapperService;
import com.task.sheet.utils.ExcelFileParse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/url")
public class UrlController {

    @Autowired
    private UrlScrapperService urlScrapperService;

    @Autowired
    private ExcelFileParse excelFileParse;

    @GetMapping("/session")
    public ResponseEntity<?> getSessionID(){
        return ResponseEntity.ok(urlScrapperService.createNewUploadSession());
    }

    @GetMapping("/fetch-account")
    public ResponseEntity<?> fetchAccount(@RequestParam String sessionID){
        System.out.println(sessionID);
        return urlScrapperService.fetchAccounts("/api/fetch-accounts/", sessionID);
    }

//    @GetMapping("/scrapper")
//    public ResponseEntity<String> getUrlData(@RequestParam String fileType){
//        urlScrapperService.createNewUploadSession();
//        urlScrapperService.fetchAccounts("/api/fetch-accounts/");
//        return urlScrapperService.makeList("/api/accounts/");
//    }

    @PostMapping("/makelist")
    public ResponseEntity<?> getMakeList(@RequestParam String sessionID){
        return urlScrapperService.makeList("/api/accounts/", sessionID);
    }

    @GetMapping("/spreadSheetTest")
    public ResponseEntity<?> testInsertion(){
        return excelFileParse.insertionSheet();
    }

}
