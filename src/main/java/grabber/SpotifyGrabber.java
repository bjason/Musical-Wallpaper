package grabber;

import com.wrapper.spotify.SpotifyApi;
import com.wrapper.spotify.exceptions.SpotifyWebApiException;
import com.wrapper.spotify.exceptions.detailed.TooManyRequestsException;
import com.wrapper.spotify.model_objects.credentials.ClientCredentials;
import com.wrapper.spotify.model_objects.specification.*;
import com.wrapper.spotify.requests.authorization.client_credentials.ClientCredentialsRequest;

import com.wrapper.spotify.requests.data.albums.GetAlbumRequest;
import com.wrapper.spotify.requests.data.playlists.GetPlaylistRequest;
import com.wrapper.spotify.requests.data.playlists.GetPlaylistsTracksRequest;
import org.json.JSONArray;
import ui.VisualizorUI;
import util.InvalidPlaylistURLException;
import util.NoneInPlaylistException;
import util.PropertiesManager;

import java.awt.*;
import java.io.IOException;
import java.util.*;

/// Downloads the album art for each song in the playlist
public class SpotifyGrabber extends Grabber {
    final int MAX_TRACKS_FROM_PLAYLIST = 100; // spotify enforces this limit
    public final static int[] SPOTIFY_IMAGE_SIZES = new int[]{640, 300, 64};

    // TODO hide these somehow?
    private final String clientID = "c4d868dd2f78422a8b1871e42c8e141b";
    private final String clientSecret = "7f8ec31bcc1345708e94f427be4ffc40";

    private SpotifyApi spotifyApi;

    private void getAuthorisedAPI() throws IOException, SpotifyWebApiException {
        spotifyApi = new SpotifyApi.Builder()
                .setClientId(clientID)
                .setClientSecret(clientSecret)
                .build();
        final ClientCredentialsRequest clientCredentialsRequest = spotifyApi.clientCredentials()
                .build();

        final ClientCredentials clientCredentials = clientCredentialsRequest.execute();

        // Set access token for further "spotifyApi" object usage
        spotifyApi.setAccessToken(clientCredentials.getAccessToken());
    }

    // the error code is to get around SwingWorkers not using exception throwing
    // in doInBackground()

    public void main() throws IOException,
            SpotifyWebApiException, InvalidPlaylistURLException, NoneInPlaylistException {
        setProgress(0);
        getAuthorisedAPI();
        getAlbumImagesInPlaylist();
        downloadAlbumsToDirectory();
        // setProgress(40); // move the progress bar a bit
    }

    private int processedNumber;

    /// Returns a HashMap mapping track names and artist
    /// to their biggest image's URL, for each aslbum in the given playlist
    private void getAlbumImagesInPlaylist()
            throws IOException, SpotifyWebApiException, NoneInPlaylistException {
        int loopsRequired;
        JSONArray jsonArray = new JSONArray();

        if (allTracksInfo == null) {
            allTracksInfo = new ArrayList<>();

            GetPlaylistRequest getPlaylistRequest = spotifyApi.getPlaylist(playlistId)
                    .build();

            Playlist playlist = getPlaylistRequest.execute();
            System.out.println("Name: " + playlist.getName());

            // Spotify enforces a maximum of 100 tracks retrieved from a playlist
            // per request.
            // Therefore this method executes multiple requests in the following for
            // loop

            // calculate the number of loops required to get all of the tracks from
            // the playlist
            setNumTrack(playlist);
            setPlaylistName(playlist);

            processedNumber = 0;
        }

        VisualizorUI.progressBar.setString("Downloading covers... (Total: " + numOfTracks + ")");

        int tracksToBeProcessed = numOfTracks - processedNumber;
        loopsRequired = tracksToBeProcessed / 100 + 1; // round up

        for (int i = 0; i < loopsRequired; i++) {
            GetPlaylistsTracksRequest getPlaylistsTracksRequest = spotifyApi.getPlaylistsTracks(playlistId)
//                  .fields("description")
                    .limit(MAX_TRACKS_FROM_PLAYLIST)
                    .offset(processedNumber)
//                  .market(CountryCode.SE)
                    .build();

            Paging<PlaylistTrack> playlistTrackPaging = null;
            try {
                playlistTrackPaging = getPlaylistsTracksRequest.execute();
            } catch (TooManyRequestsException e) {
                try {
                    synchronized (this) {
//                        wait();
                        Thread.sleep(e.getRetryAfter() * 1000);
                        System.out.println(e.getMessage() + " sleep for " + e.getRetryAfter());
                        playlistTrackPaging = getPlaylistsTracksRequest.execute();
                    }
                } catch (Exception ex) {
                    System.out.println("Paging error");
                    System.out.println(ex.getMessage());
                }
            }

            if (playlistTrackPaging != null) {
                for (PlaylistTrack playlistTrack : playlistTrackPaging.getItems()) {
                    Track track = playlistTrack.getTrack();

                    AlbumSimplified album = track.getAlbum();
                    // use IMAGE_NUMBER to select the desired resolution
                    int imageNum = Integer.parseInt(PropertiesManager.getProperty("imageSizeCode"));
                    if (imageNum > 2) {
                        imageNum = 1;
                    }
                    processedNumber++;
                    getURLFromAPI(processedNumber, track, album, imageNum);

                    setProgress((int) (20 * ((float) processedNumber / numOfTracks)));
                }
            }
        }

        // add track manually example.
        // After input correct information, go to the directory:
        // C:\Users\YOUR USER NAME\Pictures\Spotify Playlist Visualizor\Album art
        // And add the album cover to it naming in the format of "Title - Artist"
        // In this case: Good Intentions Paving Company - Joanna Newsom.jpg
        addTrackManually("2010",
                "Good Intentions Paving Company",
                "Joanna Newsom",
                10,
                "Have One on Me");

        Collections.reverse(allTracksInfo);

        // save allTracksInfo as json

    }

    private void addTrackManually(String releaseDate, String trackTitle, String artistName, int order, String album) {
        HashMap<String, String> curr = saveBasicInfo(order, trackTitle, artistName, "", album);
        curr.put("ReleaseDate", releaseDate);

        allTracksInfo.add(order - 1, curr);
        numOfTracks++;
    }

    private void getURLFromAPI(int order, Track track,
                               AlbumSimplified albumSimplified, int imageNum)
            throws IOException, SpotifyWebApiException {

        // artist info
        ArtistSimplified[] artistsList = track.getArtists();
        String artistName = artistsList[0].getName();
        for (int i = 1; i < artistsList.length; i++) {
            artistName += ", " + artistsList[i].getName();
        }

        // title
        String trackTitle = track.getName();
        if (trackTitle.contains("An Adventure in The Dark Clouds Market"))
            trackTitle = "大石碎胸口";

        // determine to retrieve track info or not

        String url = albumSimplified.getImages()[imageNum].getUrl();
        String albumTitle = albumSimplified.getName();

        Album album = null;
        GetAlbumRequest getAlbumRequest = spotifyApi.getAlbum(albumSimplified.getId()).build();
        try {
            album = getAlbumRequest.execute();
        } catch (TooManyRequestsException e) {
            try {
                synchronized (this) {
                    System.out.println(e.getMessage() + " sleep for " + e.getRetryAfter());
//                    wait();
                    Thread.sleep(e.getRetryAfter() * 1000);
                    album = getAlbumRequest.execute();
                }
            } catch (Exception ex) {
                System.out.println("another exception");
                System.out.println(ex.getMessage());
            }
        }

        String releaseDate = album.getReleaseDate();
        String label = album.getLabel();

        HashMap<String, String> curr = saveBasicInfo(order, trackTitle, artistName, url, albumTitle);
        curr.put("ReleaseDate", releaseDate);
        curr.put("Label", label);

        allTracksInfo.add(curr);
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
