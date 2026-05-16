package com.audiodrop.app.model;

public class Chapter {
    private final String title;
    private final int startSeconds;

    public Chapter(String title, int startSeconds) {
        this.title = title;
        this.startSeconds = Math.max(0, startSeconds);
    }

    public String getTitle() {
        return title;
    }

    public int getStartSeconds() {
        return startSeconds;
    }
}
