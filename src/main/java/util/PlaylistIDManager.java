package util;

import java.io.IOException;

import com.wrapper.spotify.UtilProtos.Url;

/// Utility class to extract userID and playlistID from the playlist URL
public class PlaylistIDManager {
	public static final String[] DEFAULT_URL = {
			"https://open.spotify.com/user/playlistmeukfeatured/playlist/0F2RaOrNectaIorC71tBQJ",
			"http://music.163.com/#/playlist?id=498231585",
			"https://www.youtube.com/playlist?list=PLgnqnloKbnvK6BsJhzfrY8fD_cEYFS6uS",
			"https://y.qq.com/n/yqq/playlist/1096418502.html#stat=y_new.profile.create_playlist.love.click&dirid=201",
			"http://www.kugou.com/yy/special/single/221986.html" };

	int sourceId;

	public PlaylistIDManager() {
		// TODO Auto-generated constructor stub
		getSourceId();
	}

	private void getSourceId() {
		try {
			sourceId = Integer.parseInt(PropertiesManager.getProperty("sourceId"));
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public String[] getPlaylistIDAndUserIDFromURL(String URL) throws InvalidPlaylistURLException {

		if (URL == null || URL.equals("")) {
			throw new InvalidPlaylistURLException();
		}
		String userID = "";
		String playlistID = "";
		String key;

		switch (sourceId) {
		case 0:
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
					if (part.equals("playlist")) {
						playlistID = parts[i + 1];// .split("?")[0];
					}
				}
			} catch (ArrayIndexOutOfBoundsException e) {
				throw new InvalidPlaylistURLException();
			}

			// there must be a userID and playlistID in the URL - and it has to
			// be from spotify of course!
			if (userID.equals("") || playlistID.equals("") || !URL.contains("spotify.com")) {
				throw new InvalidPlaylistURLException();
			}
			break;

		case 1: // 163 music
			key = "music.163.com/#/playlist?id=";
			playlistID = judgeAndgetId(URL, playlistID, key);
			break;
		case 2: // YouTube
			key = "www.youtube.com/playlist?list=";
			playlistID = judgeAndgetId(URL, playlistID, key);
			break;
		}

		return new String[] { playlistID, userID };
	}

	private String judgeAndgetId(String URL, String playlistID, String key) throws InvalidPlaylistURLException {
		URL = URL.replace("http://", "").replace("https://", "");
		playlistID = URL.substring(URL.indexOf("=") + 1, URL.length());
		// TODO validate the id with more details

		if (playlistID.equals("") || !URL.startsWith(key)) {
			throw new InvalidPlaylistURLException();
		}
		return playlistID;
	}
}
