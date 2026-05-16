package com.audiodrop.app.model;

public class Playlist {
    private final String id;
    private final String name;

    public Playlist(String id, String name) {
        this.id = id;
        this.name = name;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }
}
