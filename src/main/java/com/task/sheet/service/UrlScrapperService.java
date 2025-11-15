package com.task.sheet.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestTemplate;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class UrlScrapperService {

    @Autowired
    private RestTemplate restTemplate;

    @Value("${api.url}")
    private String domainApi;

    private String sessionID = null;

    public ResponseEntity<String> getData(String fileType) {
//        String loginUrl = "https://deetaanalyticsllc.com:8054/auth/login";
        String loginUrl = domainApi + "/auth/login";
        Map<String, String> loginBody = new HashMap<>();
        loginBody.put("email", "analyst@digitalanalystteam.com");
        loginBody.put("password", "Duc@1234");

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<Map<String, String>> loginRequest = new HttpEntity<>(loginBody, headers);

        ResponseEntity<String> loginResponse =
                restTemplate.postForEntity(loginUrl, loginRequest, String.class);

        List<String> cookies = loginResponse.getHeaders().get("Set-Cookie");
        String sessionCookie = cookies != null && !cookies.isEmpty() ? cookies.get(0) : null;


        String apiUrl = domainApi + "/api/checks";

        HttpHeaders apiHeaders = new HttpHeaders();
        apiHeaders.add("Cookie", sessionCookie);
        apiHeaders.setAccept(List.of(MediaType.APPLICATION_JSON));

        HttpEntity<Void> apiRequest = new HttpEntity<>(apiHeaders);

        ResponseEntity<String> apiResponse =
                restTemplate.exchange(apiUrl, HttpMethod.GET, apiRequest, String.class);
        return apiResponse;
    }

    public ResponseEntity<String> fetchAccounts(String endpoint, String sessionId) {
        String url = domainApi + endpoint + sessionId;

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        String requestBody = "{}";

        HttpEntity<String> requestEntity = new HttpEntity<>(requestBody, headers);

        ResponseEntity<String> response = restTemplate.exchange(
                url,
                HttpMethod.POST,
                requestEntity,
                String.class
        );

        return response;
//        return null;
    }

    public ResponseEntity<String> makeList(String endpoint, String sessionId) {
        String url = domainApi + endpoint + sessionId;
        HttpHeaders headers = new HttpHeaders();
        headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));
        headers.add(HttpHeaders.COOKIE, "session=" + sessionId);

        HttpEntity<Void> requestEntity = new HttpEntity<>(headers);

        int maxRetries = 3;
        int attempt = 0;

        while (attempt < maxRetries) {
            attempt++;

            try {
                ResponseEntity<String> response = restTemplate.exchange(
                        url,
                        HttpMethod.GET,
                        requestEntity,
                        String.class
                );

                String body = response.getBody();

                if (body != null && !body.trim().isEmpty() && !body.trim().equals("{}") && !body.trim().equals("[]")) {
                    return response;
                } else {
                    Thread.sleep(2000);
                }

            } catch (HttpClientErrorException e) {
                break;
            } catch (HttpServerErrorException e) {
                break;
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body("Failed to get non-empty response after " + maxRetries + " attempts");
    }


    public String createNewUploadSession() {
        final String LOGIN_URL = domainApi + "/auth/login";
        final String SESSION_URL = domainApi + "/api/session";

        HttpHeaders loginHeaders = new HttpHeaders();
        loginHeaders.setContentType(MediaType.APPLICATION_JSON);

        String loginBody = """
                {
                  "email": "analyst@digitalanalystteam.com",
                  "password": "Duc@1234"
                }
                """;

        HttpEntity<String> loginRequest = new HttpEntity<>(loginBody, loginHeaders);

        ResponseEntity<String> loginResponse = restTemplate.postForEntity(LOGIN_URL, loginRequest, String.class);

        if (loginResponse.getStatusCode().is2xxSuccessful()) {
            List<String> cookies = loginResponse.getHeaders().get("Set-Cookie");
            if (cookies != null && !cookies.isEmpty()) {
                String sessionCookie = cookies.get(0);

                HttpHeaders sessionHeaders = new HttpHeaders();
                sessionHeaders.set("Cookie", sessionCookie);
                sessionHeaders.setContentType(MediaType.APPLICATION_JSON);

                HttpEntity<String> sessionRequest = new HttpEntity<>("{}", sessionHeaders);
                ResponseEntity<String> sessionResponse = restTemplate.postForEntity(SESSION_URL, sessionRequest, String.class);
                String responseBody = sessionResponse.getBody();

                ObjectMapper mapper = new ObjectMapper();
                JsonNode jsonNode = null;
                try {
                    jsonNode = mapper.readTree(responseBody);
                } catch (JsonProcessingException e) {
                    throw new RuntimeException(e);
                }

                String sessionId = null;
                if (jsonNode.has("session_id")) {
                    sessionId = jsonNode.get("session_id").asText();
                } else if (jsonNode.has("sessionId")) {
                    sessionId = jsonNode.get("sessionId").asText();
                } else if (jsonNode.has("id")) {
                    sessionId = jsonNode.get("id").asText();
                } else {
                    throw new RuntimeException("❌ No session ID found in response: " + responseBody);
                }

                sessionID = sessionId;
                fetchAccounts("/api/fetch-accounts/", sessionID);
            }
        }
        return sessionID;
    }
}
