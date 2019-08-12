package grabber;

import com.wrapper.spotify.SpotifyApi;
import com.wrapper.spotify.exceptions.SpotifyWebApiException;
import com.wrapper.spotify.model_objects.credentials.ClientCredentials;
import com.wrapper.spotify.model_objects.specification.*;
import com.wrapper.spotify.requests.authorization.client_credentials.ClientCredentialsRequest;

import com.wrapper.spotify.requests.data.playlists.GetPlaylistRequest;
import com.wrapper.spotify.requests.data.playlists.GetPlaylistsTracksRequest;
import ui.VisualizorUI;
import util.InvalidPlaylistURLException;
import util.NoneInPlaylistException;
import util.PropertiesManager;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URL;
import java.util.*;

/// Downloads the album art for each song in the playlist
public class SpotifyGrabber extends Grabber {
    final int MAX_TRACKS_FROM_PLAYLIST = 100; // spotify enforces this limit
    public final static int[] SPOTIFY_IMAGE_SIZES = new int[]{640, 300, 64};

    // TODO hide these somehow?
    private final String clientID = "c4d868dd2f78422a8b1871e42c8e141b";
    private final String clientSecret = "7f8ec31bcc1345708e94f427be4ffc40";

    private SpotifyApi getAuthorisedAPI() throws IOException, SpotifyWebApiException {
        final SpotifyApi spotifyApi = new SpotifyApi.Builder()
                .setClientId(clientID)
                .setClientSecret(clientSecret)
                .build();
        final ClientCredentialsRequest clientCredentialsRequest = spotifyApi.clientCredentials()
                .build();

        final ClientCredentials clientCredentials = clientCredentialsRequest.execute();

        // Set access token for further "spotifyApi" object usage
        spotifyApi.setAccessToken(clientCredentials.getAccessToken());

        return spotifyApi;
    }

    // the error code is to get around SwingWorkers not using exception throwing
    // in doInBackground()

    public void main() throws IOException,
            SpotifyWebApiException, InvalidPlaylistURLException, NoneInPlaylistException {
        SpotifyApi api = getAuthorisedAPI();
        LinkedHashMap<String, String> trackNamesAndImageURLs = getAlbumImagesInPlaylist(api);

        downloadAlbumsToDirectory();
        // setProgress(40); // move the progress bar a bit
    }

    /// Returns a HashMap mapping track names and artist
    /// to their biggest image's URL, for each aslbum in the given playlist
    // TODO change the HASHMAP to List so that the serial order doens't have to
    /// appear in the track name
    private LinkedHashMap<String, String> getAlbumImagesInPlaylist(SpotifyApi spotifyApi)
            throws IOException, SpotifyWebApiException, NoneInPlaylistException {
        LinkedHashMap<String, String> trackNamesAndImages = new LinkedHashMap<>();
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

        int loopsRequired = numOfTracks / 100 + 1; // round up
        int j = 1;

        for (int i = 0; i < loopsRequired; i++) {
            GetPlaylistsTracksRequest getPlaylistsTracksRequest = spotifyApi.getPlaylistsTracks(playlistId)
//                  .fields("description")
                    .limit(MAX_TRACKS_FROM_PLAYLIST)
                    .offset(i * MAX_TRACKS_FROM_PLAYLIST)
//                  .market(CountryCode.SE)
                    .build();

            Paging<PlaylistTrack> playlistTrackPaging = getPlaylistsTracksRequest.execute();

            for (PlaylistTrack playlistTrack :
                    playlistTrackPaging.getItems()) {
                Track track = playlistTrack.getTrack();

                AlbumSimplified album = track.getAlbum();
                // use IMAGE_NUMBER to select the desired resolution
                int imageNum = Integer.parseInt(PropertiesManager.getProperty("imageSizeCode"));
                if (imageNum > 2) {
                    imageNum = 1;
                }
                getURLFromAPI(trackNamesAndImages, j, track, album, imageNum, "");
                j++;

                setProgress((int) (20 * ((float) j / numOfTracks)));
                VisualizorUI.progressBar.setString("Downloading album art... (Total: " + numOfTracks + ")");
            }
        }

        return trackNamesAndImages;

        /*

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
        */
    }

    private void getURLFromAPI(LinkedHashMap<String, String> trackNamesAndImages, int order, Track track,
                               AlbumSimplified album, int imageNum, String suffix) {
        String url = album.getImages()[imageNum].getUrl();
        // albumNamesAndImages.put(album.getName(), url);
        // TODO fix this getartist problem
        String trackTitle = track.getName();
        String artistName = track.getArtists()[0].getName();
        String trackName_forSave = trackTitle.replaceAll("[\\\\/:*?\"<>|]", "_");
        String artistName_forSave = artistName.replaceAll("[\\\\/:*?\"<>|]", "_");

        HashMap<String, String> curr = new HashMap<>();
        curr.put("order", order + "");
        curr.put("Title", trackTitle);
        curr.put("Artist", artistName);
        curr.put("url", url);

        allTracksInfo.add(curr);

        setFileName(trackNamesAndImages, order, suffix, url, trackName_forSave, artistName_forSave);
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
