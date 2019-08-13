package grabber;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.extensions.java6.auth.oauth2.AuthorizationCodeInstalledApp;
import com.google.api.client.extensions.jetty.auth.oauth2.LocalServerReceiver;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.util.store.FileDataStoreFactory;
import com.google.api.services.youtube.YouTubeScopes;
import com.google.api.services.youtube.model.Playlist;
import com.google.api.services.youtube.model.PlaylistItem;
import com.google.api.services.youtube.model.PlaylistItemListResponse;
import com.google.api.services.youtube.model.PlaylistListResponse;
import com.google.common.collect.Lists;
import com.wrapper.spotify.exceptions.SpotifyWebApiException;

import util.YouTubeAuth;
import util.InvalidPlaylistURLException;
import util.NoneInPlaylistException;
import util.PropertiesManager;

import com.google.api.services.youtube.YouTube;

public class YouTubeGrabber extends Grabber {

	/** Directory to store user credentials for this application. */
	private static final java.io.File DATA_STORE_DIR = new java.io.File(System.getProperty("user.home"),
			".credentials/youtube-java-quickstart");

	/** Global instance of the {@link FileDataStoreFactory}. */
	private static FileDataStoreFactory DATA_STORE_FACTORY;

	/** Global instance of the JSON factory. */
	private static final JsonFactory JSON_FACTORY = JacksonFactory.getDefaultInstance();

	/** Global instance of the HTTP transport. */
	private static HttpTransport HTTP_TRANSPORT;

	private static final List<String> SCOPES = Arrays.asList(YouTubeScopes.YOUTUBE_READONLY);
	
	// TODO split the playlist when it is too long

	@Override
	public void main() throws IOException, SpotifyWebApiException, InvalidPlaylistURLException, NoneInPlaylistException {
		// get youtube api
		List<String> scopes = Lists.newArrayList("https://www.googleapis.com/auth/youtube.readonly");
		// YouTube youTube = getYouTubeService();
		
		//TODO get a playlist request and implement playlistname and numtracks method

		// Authorize the request.
		Credential credential = YouTubeAuth.authorize(scopes, "youtubegrabber");
		YouTube youTube = new YouTube.Builder(YouTubeAuth.HTTP_TRANSPORT, YouTubeAuth.JSON_FACTORY, credential)
				.setApplicationName("Spotify-Playlist-Visualizor").build();
		
		// get the information of the playlist
		// especially get playlist name
		//Playlist playlist = getPlaylist(playlistId, youTube);
		
		//setPlaylistName(playlist);

		// Define a list to store items in the list of uploaded videos.
		List<PlaylistItem> playlistItemList = new ArrayList<PlaylistItem>();

		// Retrieve the playlist of the uploaded videos.
		YouTube.PlaylistItems.List playlistItemRequest = youTube.playlistItems().list("snippet");
		playlistItemRequest.setPlaylistId(playlistId);
		playlistItemRequest.setFields("items(snippet(position,thumbnails,title))");

		String nextToken = "";

		// Call the API one or more times to retrieve all items in the
		// list. As long as the API response returns a nextPageToken,
		// there are still more items to retrieve.
		do {
			playlistItemRequest.setPageToken(nextToken);
			PlaylistItemListResponse playlistItemResult = playlistItemRequest.execute();

			playlistItemList.addAll(playlistItemResult.getItems());

			nextToken = playlistItemResult.getNextPageToken();
		} while (nextToken != null);

		Iterator<PlaylistItem> items = playlistItemList.iterator();
		LinkedHashMap<String, String> titleAndImageNames = new LinkedHashMap<>();

		int order = 1;
		while (items.hasNext()) {
			PlaylistItem item = items.next();
			String title = item.getSnippet().getTitle().toString();

			String[] parts = title.split(" - ");
			String artistName = parts[0];
			String trackName = parts[1].replace(" (Official Video)", "")
					.replace(" [Official Video]", "").replace(" (Official Music Video)", "");
			
			String url = item.getSnippet().getThumbnails().getMaxres().getUrl();

			order++;
		}
		setNumTrack(order);
		
		downloadAlbumsToDirectory();

	}
	
	// this method get a playlist instance which provides the name of the playlist
	private Playlist getPlaylist(String playlistId, YouTube youTube) throws IOException {
		YouTube.Playlists.List playlistRequest = youTube.playlists().list("snippet");
		playlistRequest.setId(playlistId);
		playlistRequest.setFields("items(snippet(title))");
		
		PlaylistListResponse playlistResult = playlistRequest.execute();
		Playlist playlist = playlistResult.getItems().get(0);
		return playlist;
	}

//	public static YouTube getYouTubeService() throws IOException {
//		Credential credential = authorize();
//		return new YouTube.Builder(HTTP_TRANSPORT, JSON_FACTORY, credential)
//				.setApplicationName("Spotify Playlist Visualizor").build();
//	}

//	public static Credential authorize() throws IOException {
//		// Load client secrets.
//		InputStream in = YouTubeGrabber.class.getResourceAsStream("/client_secret.json");
//		GoogleClientSecrets clientSecrets = GoogleClientSecrets.load(JSON_FACTORY, new InputStreamReader(in));
//
//		// Build flow and trigger user authorization request.
//		GoogleAuthorizationCodeFlow flow = new GoogleAuthorizationCodeFlow.Builder(HTTP_TRANSPORT, JSON_FACTORY,
//				clientSecrets, SCOPES).setDataStoreFactory(DATA_STORE_FACTORY).setAccessType("offline").build();
//		Credential credential = new AuthorizationCodeInstalledApp(flow, new LocalServerReceiver()).authorize("user");
//		return credential;
//	}

	@Override
	protected <T> void setPlaylistName(T playlist) {
		playlistName = ((Playlist) playlist).getSnippet().getTitle().toString();
	}

	@Override
	protected <T> void setNumTrack(T playlist) throws NoneInPlaylistException {
//		numOfTracks = (int) playlist;
		isEmpty();
	}
}
