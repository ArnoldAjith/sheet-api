package com.task.sheet.DTO;

public class RSSItem {
    private String title;
    private String link;
    private String description;
    private java.util.Date pubDate;
    private String author;

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getLink() { return link; }
    public void setLink(String link) { this.link = link; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public java.util.Date getPubDate() { return pubDate; }
    public void setPubDate(java.util.Date pubDate) { this.pubDate = pubDate; }
    public String getAuthor() { return author; }
    public void setAuthor(String author) { this.author = author; }
}

