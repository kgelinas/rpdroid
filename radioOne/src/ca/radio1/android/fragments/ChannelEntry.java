package ca.radio1.android.fragments;

/**
 * Data object that represents a channel
 */
public class ChannelEntry {
    private int id;
    private String name;
    private boolean favorite;

    public ChannelEntry(int channelId, String channelName) {
        id = channelId;
        name = channelName;
    }

    public int getChannelId() {
        return id;
    }

    public String getChannelName() {
        return name;
    }

    public boolean isFavorite() {
        return favorite;
    }

    public void setFavorite(boolean favorite) {
        this.favorite = favorite;
    }
}
