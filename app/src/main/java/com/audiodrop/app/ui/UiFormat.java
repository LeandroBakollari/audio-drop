package com.audiodrop.app.ui;

public final class UiFormat {
    private UiFormat() {
    }

    public static String duration(int totalSeconds) {
        int safeSeconds = Math.max(0, totalSeconds);
        int hours = safeSeconds / 3600;
        int minutes = (safeSeconds % 3600) / 60;
        int seconds = safeSeconds % 60;

        if (hours > 0) {
            return String.format("%d:%02d:%02d", hours, minutes, seconds);
        }
        return String.format("%02d:%02d", minutes, seconds);
    }

    public static String countLabel(int count, String singular, String plural) {
        return count + " " + (count == 1 ? singular : plural);
    }
}
