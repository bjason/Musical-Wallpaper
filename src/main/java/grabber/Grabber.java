package grabber;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.LinkedHashMap;

import javax.imageio.ImageIO;
import javax.swing.SwingWorker;

import com.wrapper.spotify.exceptions.WebApiException;

import image.Cover;
import util.InvalidPlaylistURLException;
import util.NoneInPlaylistException;
import util.PropertiesManager;

public abstract class Grabber extends SwingWorker<Void, Void> {
	public static int numOfTracks = 0;
	public static String playlistName;
	protected final String DIRECTORY = System.getProperty("user.home") + File.separator + "Pictures" + File.separator
			+ "Spotify Playlist Visualizor" + File.separator + "Album art";
	protected int sourceId;
	protected String playlistId;
	protected String userId;

	public String errorCode = null; // only non-null if an exception was
									// encountered downloading album art

	@Override
	protected Void doInBackground() {
		try {
			setProgress(0);
			getPlaylistInfo();
			main();
		} catch (RuntimeException e) {
			System.out.println(e.getClass().getName());
		} catch (Exception e) {
			errorCode = "Could not download album art. Error: " + e.getMessage();
		}
		return null;
	}

	protected void getPlaylistInfo() throws NumberFormatException, IOException {
		sourceId = Integer.parseInt(PropertiesManager.getProperty("sourceId"));
		playlistId = PropertiesManager.getProperty(sourceId + "playlistId");
		userId = PropertiesManager.getProperty(sourceId + "userId");
	}

	public abstract void main()
			throws IOException, WebApiException, InvalidPlaylistURLException, NoneInPlaylistException;

	protected abstract <T> void setPlaylistName(T playlist);

	protected void isEmpty() throws NoneInPlaylistException {
		if (numOfTracks == 0) {
			throw new NoneInPlaylistException();
		}
	}

	protected abstract <T> void setNumTrack(T playlist) throws NoneInPlaylistException;

	protected void downloadAlbumsToDirectory(LinkedHashMap<String, String> titleAndImages) throws IOException {
		int i = 1;
		// clear any previously downloaded album art
		new File(DIRECTORY).mkdirs(); // make sure the DIRECTORY exists
		for (File file : new File(DIRECTORY).listFiles()) {
			file.delete();
		}
		
		for (String title : titleAndImages.keySet()) {
			// create a file in the DIRECTORY, named after the track
			// String cleanedAlbum = getCleanedFilename(album); // remove
			// invalid characters
			String path = DIRECTORY + File.separator + title + ".jpg";
			File file = new File(path);
			// if this album has already been downloaded, skip it
			if (file.exists()) {
				continue;
			}
			// if the DIRECTORY does not exist, create it
			file.mkdirs();

			// download the image
			URL url = new URL(titleAndImages.get(title));
			BufferedImage image = ImageIO.read(url);

			// write the downloaded image to the file
			ImageIO.write(image, "jpg", file);

			setProgress(20 + 80 / numOfTracks * i);
			i++;
		}
	}

	@SuppressWarnings("unused")
	protected String getCleanedFilename(String oldFilename) {
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

	void setFileName(LinkedHashMap<String, String> trackNamesAndImages, int order, String suffix, String url,
			String trackName, String artistName) {
		trackNamesAndImages.put(
				String.format("%02d", order) + ". " + trackName + Cover.ARTIST_SEPARATOR + artistName + suffix, url);
	}
}
