package util;

import java.io.IOException;
import java.util.ArrayList;

/// Utility class to extract userID and playlistID from the playlist URL
public class PlaylistIDManager {
    public static final String[] DEFAULT_URL = {
            "https://open.spotify.com/playlist/4fvinlRCC1ts2U74tUwwJB",
            "http://music.163.com/#/playlist?id=498231585",
            "https://www.youtube.com/playlist?list=PLgnqnloKbnvK6BsJhzfrY8fD_cEYFS6uS",
            "https://y.qq.com/n/yqq/playlist/3717970005.html",
            "http://www.kugou.com/yy/special/single/102589.html"};

    int sourceId;
    String userID = "";
    String playlistID = "";

    public PlaylistIDManager() {
        // TODO Auto-generated constructor stub
        getSourceId();
    }

    private void getSourceId() {
        try {
            sourceId = Integer.parseInt(PropertiesManager.getProperty("sourceId"));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public String[] getPlaylistIDAndUserIDFromURL(String URL) throws InvalidPlaylistURLException {
        if (URL == null || URL.equals("")) {
            throw new InvalidPlaylistURLException();
        }
        String key;

        switch (sourceId) {
            case 0:
                key = "open.spotify.com/playlist/";
                judgeBySlash(URL, key);
                break;
            case 1: // 163 music
                key = "music.163.com/#/playlist?id=";
                judgeByEqual(URL, key);
                break;
            case 2: // YouTube
                key = "www.youtube.com/playlist?list=";
                judgeByEqual(URL, key);
                break;
            case 3: // QQM
                // there are three kinds of url in qq music to indicate a playlist
                //https://y.qq.com/n/yqq/playlist/*id*.html
                //https://y.qq.com/n/yqq/playsquare/*id*
                //y.qq.com/w/taoge.html?id=*id*
                if (URL.contains("y.qq.com/w/taoge.html?id=")) {
                    judgeByEqual(URL, "y.qq.com/w/taoge.html?id=");
                } else {
                    URL = URL.split("#")[0];
                    judgeBySlash(URL, "y.qq.com/n/yqq/");
                }
                break;
            case 4:// KUGOU
                key = "www.kugou.com/yy/special/single/";
                judgeBySlash(URL, key);
                break;
        }

        return new String[]{playlistID, userID};
    }

    private String cleanUpURL(String url) {
        int end = url.indexOf("?");
        if (end == -1) return  url;
        else return url.substring(0, end);
    }

    private void judgeBySlash(String URL, String key) throws InvalidPlaylistURLException {
        URL = cleanUpURL(URL);
        URL = getTrunk(URL);

        String[] parts = URL.split("/");
        // example URL:
        // https://open.spotify.com/user/jellyberg/playlist/5P7onC083Jj3A78VSyk4ns
        // note that userID always follows /user/ and playlistID always
        // follows /playlist/
        // example YTB
        // URL:www.youtube.com/playlist?list=PLgnqnloKbnvK6BsJhzfrY8fD_cEYFS6uS
        try {
            for (int i = 0; i < parts.length; i++) {
                String part = parts[i];
                if (part.equals("user")) {
                    userID = parts[i + 1];
                }
                if (part.equals("playlist") || part.equals("playsquare") || part.equals("single")) {
                    playlistID = parts[i + 1].replace(".html", "");// .split("?")[0];
                }
            }
        } catch (ArrayIndexOutOfBoundsException e) {
            throw new InvalidPlaylistURLException();
        }

        if (playlistID.equals("") || !URL.startsWith(key)) {
            throw new InvalidPlaylistURLException();
        }
    }

    private String judgeByEqual(String URL, String key) throws InvalidPlaylistURLException {
        URL = getTrunk(URL);
        playlistID = URL.substring(URL.indexOf("=") + 1, URL.length());
        // TODO validate the id with more details

        if (playlistID.equals("") || !URL.startsWith(key)) {
            throw new InvalidPlaylistURLException();
        }
        return playlistID;
    }

    private String getTrunk(String URL) {
        URL = URL.replace("http://", "").replace("https://", "");
        return URL;
    }
}
