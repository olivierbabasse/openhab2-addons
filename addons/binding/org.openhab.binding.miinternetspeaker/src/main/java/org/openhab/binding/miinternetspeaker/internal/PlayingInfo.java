package org.openhab.binding.miinternetspeaker.internal;

/**
 * Created by Ondrej Pecta on 04.05.2017.
 */
public class PlayingInfo {
    private String title;
    private String artist;

    public PlayingInfo(String title, String artist) {
        this.title = title;
        this.artist = rewriteChars(artist);
    }

    public String getTitle() {
        return title;
    }

    public String getArtist() {
        return artist;
    }

    private String rewriteChars(String artist) {
        return artist.replace("&amp;", "&");
    }

    @Override
    public String toString() {
        return "PlayingInfo{" +
                "title='" + title + '\'' +
                ", artist='" + artist + '\'' +
                '}';
    }
}
