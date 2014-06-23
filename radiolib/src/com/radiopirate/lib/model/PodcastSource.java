package com.radiopirate.lib.model;


public enum PodcastSource {
    RP, Marto, Unknown;

    public static PodcastSource int2e(int i) {
        for (PodcastSource current : values()) {
            if (current.ordinal() == i) {
                return current;
            }
        }
        return Unknown;
    }
}