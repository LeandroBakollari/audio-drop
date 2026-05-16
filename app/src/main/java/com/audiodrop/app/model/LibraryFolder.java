package com.audiodrop.app.model;

public class LibraryFolder {
    private final String id;
    private String name;
    private String description;
    private String iconSeed;
    private final boolean systemFolder;
    private boolean pinnedOnHome;
    private String parentFolderId;

    public LibraryFolder(
            String id,
            String name,
            String description,
            String iconSeed,
            boolean systemFolder,
            boolean pinnedOnHome,
            String parentFolderId
    ) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.iconSeed = iconSeed;
        this.systemFolder = systemFolder;
        this.pinnedOnHome = pinnedOnHome;
        this.parentFolderId = parentFolderId;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
        this.iconSeed = name.length() >= 2 ? name.substring(0, 2).toUpperCase() : name.toUpperCase();
    }

    public String getDescription() {
        return description;
    }

    public String getIconSeed() {
        return iconSeed;
    }

    public boolean isSystemFolder() {
        return systemFolder;
    }

    public boolean isPinnedOnHome() {
        return pinnedOnHome;
    }

    public void setPinnedOnHome(boolean pinnedOnHome) {
        this.pinnedOnHome = pinnedOnHome;
    }

    public String getParentFolderId() {
        return parentFolderId;
    }

    public void setParentFolderId(String parentFolderId) {
        this.parentFolderId = parentFolderId;
    }
}
