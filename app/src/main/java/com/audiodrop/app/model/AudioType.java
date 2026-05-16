package com.audiodrop.app.model;

public enum AudioType {
    MUSIC("Music"),
    AUDIOBOOK("Audiobook");

    private final String label;

    AudioType(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
