package com.task.sheet.DTO;

import java.util.List;

public class RSSFeedResponse {
    private String title;
    private String description;
    private String link;
    private List<RSSItem> items;

    // Getters and setters
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getLink() { return link; }
    public void setLink(String link) { this.link = link; }
    public List<RSSItem> getItems() { return items; }
    public void setItems(List<RSSItem> items) { this.items = items; }
}

