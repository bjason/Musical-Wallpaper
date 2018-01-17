import com.wrapper.spotify.Api;
import com.wrapper.spotify.exceptions.WebApiException;
import com.wrapper.spotify.methods.PlaylistTracksRequest;
import com.wrapper.spotify.methods.authentication.ClientCredentialsGrantRequest;
import com.wrapper.spotify.models.*;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.HashMap;
import java.util.List;

/// Downloads the album art for each song in the playlist
public class AlbumArtGrabber extends SwingWorker<Void, Void> { // extends
																// SwingWorker<Void,
																// Void> {
	final int MAX_TRACKS_FROM_PLAYLIST = 100; // spotify enforces this limit
	private static int numTracks = 0;
	static String playlistName;
	final static int[] SPOTIFY_IMAGE_SIZES = new int[] { 640, 300, 64 };
	final private String DIRECTORY = System.getProperty("user.home") + File.separator + "Pictures" + File.separator
			+ "Spotify Playlist Visualizor" + File.separator + "Album art";

	protected String errorCode = null; // only non-null if an exception was
										// encountered downloading album art
	// the error code is to get around SwingWorkers not using exception throwing
	// in doInBackground()

	@Override
	protected Void doInBackground() {
		try {
			setProgress(0);
			downloadAlbumArt();
		} catch (RuntimeException e) {
			System.out.println(e.getClass().getName());
		} catch (Exception e) {
			errorCode = "Could not download album art. Error: " + e.getMessage();
		}
		return null;
	}

	public void downloadAlbumArt() throws IOException, WebApiException, InvalidPlaylistURLException {
		Api api = getAuthorisedAPI();
		String[] IDs = PlaylistIDManager.getPlaylistIDAndUserIDFromURL(PropertiesManager.getProperty("playlistURL"));
		String playlistID = IDs[0];
		String userID = IDs[1];
		HashMap<String, String> trackNamesAndImageURLs = getAlbumImagesInPlaylist(playlistID, userID, api);
		
		// clear any previously downloaded album art
		new File(DIRECTORY).mkdirs(); // make sure the DIRECTORY exists
		for (File file : new File(DIRECTORY).listFiles()) {
			file.delete();
		}
		downloadAlbumsToDirectory(trackNamesAndImageURLs, DIRECTORY);
		// setProgress(40); // move the progress bar a bit
	}

	/// Returns a HashMap mapping track names and artist
	/// to their biggest image's URL, for each album in the given playlist
	private HashMap<String, String> getAlbumImagesInPlaylist(String playlistID, String userID, Api api)
			throws IOException, WebApiException {
		HashMap<String, String> trackNamesAndImages = new HashMap<>();

		// Spotify enforces a maximum of 100 tracks retrieved from a playlist
		// per request.
		// Therefore this method executes multiple requests in the following for
		// loop

		// calculate the number of loops required to get all of the tracks from
		// the playlist
		Playlist playlist = api.getPlaylist(userID, playlistID).build().get();
		numTracks = playlist.getTracks().getTotal();
		playlistName = playlist.getName();
		int loopsRequired = numTracks / 100 + 1; // round up
		int order = 1;

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
				getURLFromAPI(trackNamesAndImages, order, track, album, imageNum, "");
				order++;

				setProgress((int) 20 * order / numTracks);
			}
		}

		return trackNamesAndImages;

	}

	private void getURLFromAPI(HashMap<String, String> trackNamesAndImages, int order, PlaylistTrack track,
			SimpleAlbum album, int imageNum, String suffix) {
		String url = album.getImages().get(imageNum).getUrl();
		// albumNamesAndImages.put(album.getName(), url);
		// TODO fix this getartist problem
		trackNamesAndImages.put(String.format("%02d", order) + ". " + track.getTrack().getName() + AlbumCover.ARTIST_SEPARATOR
				+ track.getTrack().getArtists().get(0).getName() + suffix, url);
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

	private void downloadAlbumsToDirectory(HashMap<String, String> trackNamesAndImages, String directory)
			throws IOException {
		int i = 1;
		for (String album : trackNamesAndImages.keySet()) {
			// create a file in the DIRECTORY, named after the track
			// String cleanedAlbum = getCleanedFilename(album); // remove
			// invalid
			// // characters
			String cleanedAlbum = album;
			String path = directory + File.separator + cleanedAlbum + ".jpg";
			File file = new File(path);
			// if this album has already been downloaded, skip it
			if (file.exists()) {
				continue;
			}
			// if the DIRECTORY does not exist, create it
			file.mkdirs();

			// download the image
			URL url = new URL(trackNamesAndImages.get(album));
			BufferedImage image = ImageIO.read(url);

			// write the downloaded image to the file
			ImageIO.write(image, "jpg", file);

			setProgress(20 + 80 / numTracks * i);
			i++;
		}
	}

	@SuppressWarnings("unused")
	private String getCleanedFilename(String oldFilename) {
		StringBuilder filename = new StringBuilder();

		for (char c : oldFilename.toCharArray()) {
			// only allow java identifiers into the filename
			// this is a bit conservative but that shouldn't matter
			if (c == ' ' | Character.isJavaIdentifierPart(c)) {
				filename.append(c);
			} else {
				// invalid characters are replaced by hyphens
				filename.append('-');
			}
		}

		return filename.toString();
	}
}
