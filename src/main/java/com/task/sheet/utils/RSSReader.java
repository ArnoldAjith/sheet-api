package com.task.sheet.utils;

import com.rometools.rome.feed.synd.SyndEntry;
import com.rometools.rome.feed.synd.SyndFeed;
import com.rometools.rome.io.ParsingFeedException;
import com.rometools.rome.io.SyndFeedInput;
import com.rometools.rome.io.XmlReader;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.http.*;
import org.springframework.web.client.RestTemplate;
import org.w3c.dom.*;

import javax.xml.parsers.*;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.*;


@Component
public class RSSReader {

    @Autowired
    private RestTemplate restTemplate;

    public List<Map<String, Object>> getDescription() {
        String rssUrl = "https://automotive.einnews.com/rss/3k3IchAxgy-sX3fq";
        List<Map<String, Object>> returnList = new ArrayList<>();

        HttpURLConnection conn = null;
        try {
            URL url = new URL(rssUrl);
            conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64)");
            conn.setConnectTimeout(10_000);
            conn.setReadTimeout(15_000);

            int status = conn.getResponseCode();
            if (status != HttpURLConnection.HTTP_OK) {
                System.err.println("⚠️ RSS fetch failed: HTTP " + status);
                return Collections.emptyList();
            }

            // Read first few bytes to detect if it’s valid XML
            try (InputStream input = conn.getInputStream()) {
                byte[] preview = input.readNBytes(512);
                String head = new String(preview, StandardCharsets.UTF_8).trim();
                if (!head.startsWith("<")) {
                    System.err.println("❌ Not XML content (starts with): " + head.substring(0, Math.min(50, head.length())));
                    return Collections.singletonList(Map.of("Error", "Invalid feed content, not XML"));
                }

                // Combine preview + rest of stream
                ByteArrayOutputStream buffer = new ByteArrayOutputStream();
                buffer.write(preview);
                input.transferTo(buffer);

                try (InputStream xmlStream = new ByteArrayInputStream(buffer.toByteArray())) {
                    SyndFeedInput feedInput = new SyndFeedInput();
                    SyndFeed feed = feedInput.build(new XmlReader(xmlStream));

                    for (SyndEntry entry : feed.getEntries()) {
                        Map<String, Object> item = new HashMap<>();
                        item.put("Title", entry.getTitle());
                        item.put("Link", entry.getLink());
                        item.put("Published Date", entry.getPublishedDate());
                        item.put("Description", entry.getDescription() != null ? entry.getDescription().getValue() : "No description");
                        returnList.add(item);
                    }
                }
            }

        } catch (ParsingFeedException e) {
            System.err.println("❌ Invalid XML in feed: " + e.getMessage());
            return Collections.singletonList(Map.of("Error", "Feed returned invalid XML"));
        } catch (IOException e) {
            System.err.println("❌ Network error fetching RSS: " + e.getMessage());
            return Collections.singletonList(Map.of("Error", "Failed to fetch feed"));
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (conn != null) conn.disconnect();
        }

        return returnList;

    }

    public List<Map<String, Object>> getFeedForPPC() {
        String rssUrl = "https://www.einnews.com/rss/aE7mNzijuONKE33B";
        List<Map<String, Object>> returnList = new ArrayList<>();

        HttpURLConnection conn = null;
        try {
            URL url = new URL(rssUrl);
            conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64)");
            conn.setConnectTimeout(10_000);
            conn.setReadTimeout(15_000);

            int status = conn.getResponseCode();
            if (status != HttpURLConnection.HTTP_OK) {
                System.err.println("⚠️ RSS fetch failed: HTTP " + status);
                return Collections.emptyList();
            }

            // Read first few bytes to detect if it’s valid XML
            try (InputStream input = conn.getInputStream()) {
                byte[] preview = input.readNBytes(512);
                String head = new String(preview, StandardCharsets.UTF_8).trim();
                if (!head.startsWith("<")) {
                    System.err.println("❌ Not XML content (starts with): " + head.substring(0, Math.min(50, head.length())));
                    return Collections.singletonList(Map.of("Error", "Invalid feed content, not XML"));
                }

                // Combine preview + rest of stream
                ByteArrayOutputStream buffer = new ByteArrayOutputStream();
                buffer.write(preview);
                input.transferTo(buffer);

                try (InputStream xmlStream = new ByteArrayInputStream(buffer.toByteArray())) {
                    SyndFeedInput feedInput = new SyndFeedInput();
                    SyndFeed feed = feedInput.build(new XmlReader(xmlStream));

                    for (SyndEntry entry : feed.getEntries()) {
                        Map<String, Object> item = new HashMap<>();
                        item.put("Title", entry.getTitle());
                        item.put("Link", entry.getLink());
                        item.put("Published Date", entry.getPublishedDate());
                        item.put("Description", entry.getDescription() != null ? entry.getDescription().getValue() : "No description");
                        returnList.add(item);
                    }
                }
            }

        } catch (ParsingFeedException e) {
            System.err.println("❌ Invalid XML in feed: " + e.getMessage());
            return Collections.singletonList(Map.of("Error", "Feed returned invalid XML"));
        } catch (IOException e) {
            System.err.println("❌ Network error fetching RSS: " + e.getMessage());
            return Collections.singletonList(Map.of("Error", "Failed to fetch feed"));
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (conn != null) conn.disconnect();
        }

        return returnList;

    }


    public List<Map<String, Object>> getFeedForPaidSearch() {
        String rssUrl = "https://marketing.einnews.com/rss/PQ4rG88Szio8fy6p";
        List<Map<String, Object>> returnList = new ArrayList<>();

        HttpURLConnection conn = null;
        try {
            URL url = new URL(rssUrl);
            conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64)");
            conn.setConnectTimeout(10_000);
            conn.setReadTimeout(15_000);

            int status = conn.getResponseCode();
            if (status != HttpURLConnection.HTTP_OK) {
                System.err.println("⚠️ RSS fetch failed: HTTP " + status);
                return Collections.emptyList();
            }

            // Read first few bytes to detect if it’s valid XML
            try (InputStream input = conn.getInputStream()) {
                byte[] preview = input.readNBytes(512);
                String head = new String(preview, StandardCharsets.UTF_8).trim();
                if (!head.startsWith("<")) {
                    System.err.println("❌ Not XML content (starts with): " + head.substring(0, Math.min(50, head.length())));
                    return Collections.singletonList(Map.of("Error", "Invalid feed content, not XML"));
                }

                // Combine preview + rest of stream
                ByteArrayOutputStream buffer = new ByteArrayOutputStream();
                buffer.write(preview);
                input.transferTo(buffer);

                try (InputStream xmlStream = new ByteArrayInputStream(buffer.toByteArray())) {
                    SyndFeedInput feedInput = new SyndFeedInput();
                    SyndFeed feed = feedInput.build(new XmlReader(xmlStream));

                    for (SyndEntry entry : feed.getEntries()) {
                        Map<String, Object> item = new HashMap<>();
                        item.put("Title", entry.getTitle());
                        item.put("Link", entry.getLink());
                        item.put("Published Date", entry.getPublishedDate());
                        item.put("Description", entry.getDescription() != null ? entry.getDescription().getValue() : "No description");
                        returnList.add(item);
                    }
                }
            }

        } catch (ParsingFeedException e) {
            System.err.println("❌ Invalid XML in feed: " + e.getMessage());
            return Collections.singletonList(Map.of("Error", "Feed returned invalid XML"));
        } catch (IOException e) {
            System.err.println("❌ Network error fetching RSS: " + e.getMessage());
            return Collections.singletonList(Map.of("Error", "Failed to fetch feed"));
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (conn != null) conn.disconnect();
        }

        return returnList;

    }
}

