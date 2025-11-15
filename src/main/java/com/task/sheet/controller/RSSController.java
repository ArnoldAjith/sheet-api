package com.task.sheet.controller;

import com.task.sheet.DTO.RSSFeedResponse;
import com.task.sheet.DTO.RSSItem;
import com.task.sheet.utils.RSSReader;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import com.rometools.rome.feed.synd.*;
import com.rometools.rome.io.SyndFeedInput;
import com.rometools.rome.io.XmlReader;
import java.net.URL;
import java.util.Collections;
import java.util.List;
import java.text.SimpleDateFormat;
import java.util.Map;

@Controller
@RequestMapping("/rss")
public class RSSController {

    private final RSSReader rssReader;

    public RSSController(RSSReader rssReader) {
        this.rssReader = rssReader;
    }

    @GetMapping(value = "/display", produces = MediaType.TEXT_HTML_VALUE)
    public ResponseEntity<String> displayRSSFeed(
            @RequestParam(defaultValue = "https://www.edmunds.com/feeds/rss/car-news.xml") String feedUrl) {

        try {
            String htmlContent = convertRSSToHTML(feedUrl);
            return ResponseEntity.ok(htmlContent);
        } catch (Exception e) {
            String errorHtml = createErrorPage(e.getMessage(), feedUrl);
            return ResponseEntity.status(500).body(errorHtml);
        }
    }

    @GetMapping("/api")
    @ResponseBody
    public ResponseEntity<RSSFeedResponse> getRSSFeedData(
            @RequestParam(defaultValue = "https://www.edmunds.com/feeds/rss/car-news.xml") String feedUrl) {

        try {
            RSSFeedResponse feedResponse = parseRSSFeed(feedUrl);
            return ResponseEntity.ok(feedResponse);
        } catch (Exception e) {
            return ResponseEntity.status(500).body(null);
        }
    }

    private String convertRSSToHTML(String feedUrl) throws Exception {
        SyndFeedInput input = new SyndFeedInput();
        SyndFeed feed = input.build(new XmlReader(new URL(feedUrl)));

        StringBuilder html = new StringBuilder();

        // HTML Template
        html.append("""
            <!DOCTYPE html>
            <html lang="en">
            <head>
                <meta charset="UTF-8">
                <meta name="viewport" content="width=device-width, initial-scale=1.0">
                <title>RSS Feed Display</title>
                <style>
                    * { box-sizing: border-box; }
                    body { 
                        font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Arial, sans-serif;
                        margin: 0; padding: 20px; background: #f8f9fa; line-height: 1.6;
                    }
                    .container { max-width: 900px; margin: 0 auto; background: white; border-radius: 8px; box-shadow: 0 2px 10px rgba(0,0,0,0.1); }
                    .header { background: linear-gradient(135deg, #667eea 0%, #764ba2 100%); color: white; padding: 30px; border-radius: 8px 8px 0 0; }
                    .feed-title { margin: 0; font-size: 28px; font-weight: 300; }
                    .feed-description { margin: 10px 0 0; opacity: 0.9; font-size: 16px; }
                    .feed-link { color: #e3f2fd; text-decoration: none; }
                    .feed-link:hover { text-decoration: underline; }
                    .items-container { padding: 0; }
                    .item { border-bottom: 1px solid #eee; padding: 25px 30px; transition: background-color 0.2s; }
                    .item:hover { background-color: #f8f9fa; }
                    .item:last-child { border-bottom: none; }
                    .item-title { font-size: 20px; font-weight: 600; margin: 0 0 12px; }
                    .item-title a { color: #2c3e50; text-decoration: none; }
                    .item-title a:hover { color: #3498db; }
                    .item-description { color: #666; margin: 12px 0; font-size: 15px; line-height: 1.6; }
                    .item-meta { display: flex; align-items: center; margin-top: 15px; font-size: 14px; color: #999; }
                    .item-date { margin-right: 20px; }
                    .item-author { margin-right: 20px; }
                    .item-categories { display: flex; gap: 8px; }
                    .category-tag { background: #e3f2fd; color: #1976d2; padding: 4px 8px; border-radius: 4px; font-size: 12px; }
                    .read-more { color: #3498db; text-decoration: none; font-weight: 500; }
                    .read-more:hover { text-decoration: underline; }
                    .feed-info { background: #f8f9fa; padding: 15px 30px; font-size: 14px; color: #666; border-radius: 0 0 8px 8px; }
                </style>
            </head>
            <body>
                <div class="container">
            """);

        // Feed header
        html.append("<div class='header'>");
        String feedTitle = feed.getTitle() != null ? feed.getTitle() : "RSS Feed";
        String feedLink = feed.getLink();

        if (feedLink != null && !feedLink.isEmpty()) {
            html.append("<h1 class='feed-title'><a href='").append(escapeHtml(feedLink))
                    .append("' target='_blank' class='feed-link'>").append(escapeHtml(feedTitle)).append("</a></h1>");
        } else {
            html.append("<h1 class='feed-title'>").append(escapeHtml(feedTitle)).append("</h1>");
        }

        if (feed.getDescription() != null) {
            html.append("<p class='feed-description'>").append(escapeHtml(feed.getDescription())).append("</p>");
        }
        html.append("</div>");

        // Feed items
        html.append("<div class='items-container'>");
        List<SyndEntry> entries = feed.getEntries();

        for (SyndEntry entry : entries) {
            html.append("<article class='item'>");

            // Title
            String title = entry.getTitle() != null ? entry.getTitle() : "No title";
            String link = entry.getLink();

            html.append("<h2 class='item-title'>");
            if (link != null && !link.isEmpty()) {
                html.append("<a href='").append(escapeHtml(link)).append("' target='_blank'>")
                        .append(escapeHtml(title)).append("</a>");
            } else {
                html.append(escapeHtml(title));
            }
            html.append("</h2>");

            // Description
            String description = "";
            if (entry.getDescription() != null) {
                description = entry.getDescription().getValue();
            }

            if (description != null && !description.isEmpty()) {
                String cleanDescription = description.replaceAll("<[^>]+>", "");
                if (cleanDescription.length() > 400) {
                    cleanDescription = cleanDescription.substring(0, 400) + "...";
                }
                html.append("<div class='item-description'>").append(escapeHtml(cleanDescription));
                if (link != null && !link.isEmpty()) {
                    html.append(" <a href='").append(escapeHtml(link)).append("' target='_blank' class='read-more'>Read more</a>");
                }
                html.append("</div>");
            }

            // Metadata
            html.append("<div class='item-meta'>");

            if (entry.getPublishedDate() != null) {
                SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy 'at' HH:mm");
                html.append("<span class='item-date'>📅 ").append(sdf.format(entry.getPublishedDate())).append("</span>");
            }

            if (entry.getAuthor() != null && !entry.getAuthor().isEmpty()) {
                html.append("<span class='item-author'>✍️ ").append(escapeHtml(entry.getAuthor())).append("</span>");
            }

            if (entry.getCategories() != null && !entry.getCategories().isEmpty()) {
                html.append("<div class='item-categories'>");
                for (SyndCategory category : entry.getCategories()) {
                    html.append("<span class='category-tag'>").append(escapeHtml(category.getName())).append("</span>");
                }
                html.append("</div>");
            }

            html.append("</div>");
            html.append("</article>");
        }

        html.append("</div>");

        // Feed info
        html.append("<div class='feed-info'>");
        html.append("Feed URL: <a href='").append(escapeHtml(feedUrl)).append("' target='_blank'>")
                .append(escapeHtml(feedUrl)).append("</a> | ");
        html.append("Items: ").append(entries.size());
        if (feed.getPublishedDate() != null) {
            SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy");
            html.append(" | Last updated: ").append(sdf.format(feed.getPublishedDate()));
        }
        html.append("</div>");

        html.append("</div></body></html>");

        return html.toString();
    }

    private RSSFeedResponse parseRSSFeed(String feedUrl) throws Exception {
        SyndFeedInput input = new SyndFeedInput();
        SyndFeed feed = input.build(new XmlReader(new URL(feedUrl)));

        RSSFeedResponse response = new RSSFeedResponse();
        response.setTitle(feed.getTitle());
        response.setDescription(feed.getDescription());
        response.setLink(feed.getLink());
        response.setItems(feed.getEntries().stream()
                .map(this::convertToRSSItem)
                .collect(java.util.stream.Collectors.toList()));

        return response;
    }

    private RSSItem convertToRSSItem(SyndEntry entry) {
        RSSItem item = new RSSItem();
        item.setTitle(entry.getTitle());
        item.setLink(entry.getLink());
        item.setDescription(entry.getDescription() != null ? entry.getDescription().getValue() : "");
        item.setPubDate(entry.getPublishedDate());
        item.setAuthor(entry.getAuthor());
        return item;
    }

    private String createErrorPage(String error, String feedUrl) {
        return String.format("""
            <!DOCTYPE html>
            <html>
            <head><title>RSS Feed Error</title>
            <style>
                body { font-family: Arial, sans-serif; margin: 40px; }
                .error { background: #fee; border: 1px solid #fcc; padding: 20px; border-radius: 5px; }
                .error h1 { color: #c33; margin-top: 0; }
            </style>
            </head>
            <body>
                <div class="error">
                    <h1>🚫 RSS Feed Error</h1>
                    <p><strong>Error:</strong> %s</p>
                    <p><strong>Feed URL:</strong> <a href="%s" target="_blank">%s</a></p>
                    <p>Please check if the RSS feed URL is valid and accessible.</p>
                </div>
            </body>
            </html>
            """, escapeHtml(error), escapeHtml(feedUrl), escapeHtml(feedUrl));
    }

    private String escapeHtml(String text) {
        if (text == null) return "";
        return text.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#x27;");
    }

    @GetMapping("/rss-feed")
    public ResponseEntity<List<Map<String, Object>>> getRSSFeed() {
        return ResponseEntity.ok(rssReader.getDescription());
    }

    @GetMapping("/rss-feed-ppc")
    public ResponseEntity<List<Map<String, Object>>> getRSSFeedForPPC() {
        return ResponseEntity.ok(rssReader.getFeedForPPC());
    }

    @GetMapping("/rss-feed-paid-search")
    public ResponseEntity<List<Map<String, Object>>> getRssForPaidSearch() {
        return ResponseEntity.ok(rssReader.getFeedForPaidSearch());
    }
}

