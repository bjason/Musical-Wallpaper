package grabber;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Array;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.*;

import javax.imageio.ImageIO;
import javax.swing.SwingWorker;

import com.wrapper.spotify.exceptions.SpotifyWebApiException;

import image.Collage;
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

    protected static ArrayList<HashMap<String, String>> allTracksInfo;

    public static ArrayList<HashMap<String, String>> getAllTracksInfo() {
        return allTracksInfo;
    }

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
            throws IOException, SpotifyWebApiException, InvalidPlaylistURLException, NoneInPlaylistException;

    protected abstract <T> void setPlaylistName(T playlist);

    protected void isEmpty() throws NoneInPlaylistException {
        if (numOfTracks == 0) {
            throw new NoneInPlaylistException();
        }
    }

    protected abstract <T> void setNumTrack(T playlist) throws NoneInPlaylistException;

    protected void downloadAlbumsToDirectory() throws IOException {
        System.out.println("download");

        int i = 1;
        // clear any previously downloaded album art
        new File(DIRECTORY).mkdirs(); // make sure the DIRECTORY exists
        for (File file : new File(DIRECTORY).listFiles()) {
            file.delete();
        }

        for (HashMap<String, String> trackInfo : allTracksInfo) {
            // create a file in the DIRECTORY, named after the track
            // String cleanedAlbum = getCleanedFilename(album); // remove
            // invalid characters
            String fileName = Collage.getFileName(trackInfo.get("order"), trackInfo.get("Title"),
                    trackInfo.get("Artist"), "");

            String path = DIRECTORY + File.separator + fileName + ".jpg";
            File file = new File(path);

            // if this album has already been downloaded, skip it
            if (file.exists()) {
                continue;
            }

            // download the image
            URL url = new URL(trackInfo.get("url").replace("https", "http"));
            BufferedImage image;
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setReadTimeout(5000);

            System.out.println("Request URL ... " + url);
            // normally, 3xx is redirect
            int status = conn.getResponseCode();
            image = ImageIO.read(url);

            while (status != HttpURLConnection.HTTP_OK && image == null) {
                if (status == HttpURLConnection.HTTP_MOVED_TEMP || status == HttpURLConnection.HTTP_MOVED_PERM
                        || status == HttpURLConnection.HTTP_SEE_OTHER) {

                    // get redirect url from "location" header field
                    String newUrl = conn.getHeaderField("Location");

                    // get the cookie if need, for login
                    String cookies = conn.getHeaderField("Set-Cookie");

                    // open the new connnection again
                    conn = (HttpURLConnection) new URL(newUrl).openConnection();
                    conn.setRequestProperty("Cookie", cookies);

                    image = ImageIO.read(new URL(newUrl));
                    System.out.println("Redirect to URL : " + newUrl);
                }

            }

            // if the DIRECTORY does not exist, create it
            file.mkdirs();

            // write the downloaded image to the file
            if (image == null) {
                errorCode = "Can't download the cover image, please try again later.";
                throw new IOException();
            }
            ImageIO.write(image, "jpg", file);

            setProgress((int) (20 + ((float) 80 / numOfTracks) * i));
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
                String.format("%03d", order) + ". " + trackName + Cover.ARTIST_SEPARATOR + artistName + suffix, url);
    }
}
