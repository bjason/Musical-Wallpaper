package grabber;

import com.wrapper.spotify.Api;
import com.wrapper.spotify.exceptions.WebApiException;
import com.wrapper.spotify.methods.PlaylistTracksRequest;
import com.wrapper.spotify.methods.authentication.ClientCredentialsGrantRequest;
import com.wrapper.spotify.models.*;

import util.InvalidPlaylistURLException;
import util.NoneInPlaylistException;
import util.PropertiesManager;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;

/// Downloads the album art for each song in the playlist
public class SpotifyGrabber extends Grabber {

	final int MAX_TRACKS_FROM_PLAYLIST = 100; // spotify enforces this limit
	public final static int[] SPOTIFY_IMAGE_SIZES = new int[] { 640, 300, 64 };
	// the error code is to get around SwingWorkers not using exception throwing
	// in doInBackground()

	public void main() throws IOException, WebApiException, InvalidPlaylistURLException, NoneInPlaylistException {
		Api api = getAuthorisedAPI();
		LinkedHashMap<String, String> trackNamesAndImageURLs = getAlbumImagesInPlaylist(playlistId, userId, api);

		downloadAlbumsToDirectory(trackNamesAndImageURLs);
		// setProgress(40); // move the progress bar a bit
	}

	/// Returns a HashMap mapping track names and artist
	/// to their biggest image's URL, for each album in the given playlist
	// TODO change the HASHMAP to List so that the serial order doens't have to
	/// appear in the track name
	private LinkedHashMap<String, String> getAlbumImagesInPlaylist(String playlistID, String userID, Api api)
			throws IOException, WebApiException, NoneInPlaylistException {
		LinkedHashMap<String, String> trackNamesAndImages = new LinkedHashMap<>();

		// Spotify enforces a maximum of 100 tracks retrieved from a playlist
		// per request.
		// Therefore this method executes multiple requests in the following for
		// loop

		// calculate the number of loops required to get all of the tracks from
		// the playlist
		Playlist playlist = api.getPlaylist(userID, playlistID).build().get();
		setNumTrack(playlist);
		setPlaylistName(playlist);
		int loopsRequired = numOfTracks / 100 + 1; // round up
		int j = 1;

		for (int i = 0; i <= loopsRequired; i++) {
			PlaylistTracksRequest playlistTracksRequest = api.getPlaylistTracks(userID, playlistID)
					.limit(MAX_TRACKS_FROM_PLAYLIST) // maximum number of tracks
														// to return
					// if this isn't the first loop, don't start at the top of
					// the playlist
					.offset(i * MAX_TRACKS_FROM_PLAYLIST).build();

			List<PlaylistTrack> tracks = playlistTracksRequest.get().getItems();
			for (PlaylistTrack track : tracks) {
				SimpleAlbum album = track.getTrack().getAlbum();
				// use IMAGE_NUMBER to select the desired resolution
				int imageNum = Integer.parseInt(PropertiesManager.getProperty("imageSizeCode"));
				if (imageNum > 2) {
					imageNum = 1;
				}
				getURLFromAPI(trackNamesAndImages, j, track, album, imageNum, "");
				j++;

				setProgress((int) 20 * j / numOfTracks);
			}
		}

		return trackNamesAndImages;
	}

	private void getURLFromAPI(LinkedHashMap<String, String> trackNamesAndImages, int order, PlaylistTrack track,
			SimpleAlbum album, int imageNum, String suffix) {
		String url = album.getImages().get(imageNum).getUrl();
		// albumNamesAndImages.put(album.getName(), url);
		// TODO fix this getartist problem
		String trackName = track.getTrack().getName();
		String artistName = track.getTrack().getArtists().get(0).getName();
		setFileName(trackNamesAndImages, order, suffix, url, trackName, artistName);
	}

	private Api getAuthorisedAPI() throws IOException, WebApiException {
		// TODO hide these somehow?
		String clientID = "e1706c058e6b4cf0b282597ad0022b1e";
		String clientSecret = "bde6b1541a9648a6ab763c05d26bf299";
		String redirectURI = "https://github.com/adam-binks";

		Api api = Api.builder().clientId(clientID).clientSecret(clientSecret).redirectURI(redirectURI).build();

		// Use the Client Credentials flow which limits access but does not
		// require user login
		ClientCredentialsGrantRequest request = api.clientCredentialsGrant().build();
		ClientCredentials credentials = request.get();
		String accessToken = credentials.getAccessToken();
		api.setAccessToken(accessToken); // store the access token in Api

		return api;
	}

	@Override
	protected <T> void setPlaylistName(T playlist) {
		playlistName = ((Playlist) playlist).getName();
	}

	@Override
	protected <T> void setNumTrack(T playlist) throws NoneInPlaylistException {
		numOfTracks = ((Playlist) playlist).getTracks().getTotal();
		isEmpty();
	}

}
