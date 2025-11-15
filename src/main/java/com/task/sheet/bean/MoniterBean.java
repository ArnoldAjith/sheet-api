package com.task.sheet.bean;


import java.util.Date;

public class MoniterBean {
    private String username;
    private String mailID;
    private int monitoring;
    private Date startDate;
    private Date endDate;

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getMailID() {
        return mailID;
    }

    public void setMailID(String mailID) {
        this.mailID = mailID;
    }

    public int getMonitoring() {
        return monitoring;
    }

    public void setMonitoring(int monitoring) {
        this.monitoring = monitoring;
    }

    public Date getStartDate() {
        return startDate;
    }

    public void setStartDate(Date startDate) {
        this.startDate = startDate;
    }

    public Date getEndDate() {
        return endDate;
    }

    public void setEndDate(Date endDate) {
        this.endDate = endDate;
    }
}
