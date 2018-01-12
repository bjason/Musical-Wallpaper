/// Utility class to extract userID and playlistID from the playlist URL
public class PlaylistIDManager {
    public static String[] getPlaylistIDAndUserIDFromURL(String URL) throws InvalidPlaylistURLException {
        if (URL == null || URL.equals("")) {
            throw new InvalidPlaylistURLException();
        }

        String[] parts = URL.split("/");
        // example URL: https://open.spotify.com/user/jellyberg/playlist/5P7onC083Jj3A78VSyk4ns
        // note that userID always follows /user/ and playlistID always follows /playlist/
        String userID = "";
        String playlistID = "";
        try {
            for (int i = 0; i < parts.length; i++) {
                String part = parts[i];
                if (part.equals("user")) {
                    userID = parts[i + 1];
                }
                if (part.equals("playlist")) {
                    playlistID = parts[i + 1];
                }
            }
        } catch(ArrayIndexOutOfBoundsException e) {
            throw new InvalidPlaylistURLException();
        }

        // there must be a userID and playlistID in the URL - and it has to be from spotify of course!
        if (userID.equals("") || playlistID.equals("") || !URL.contains("spotify.com")) {
            throw new InvalidPlaylistURLException();
        }

        return new String[] {playlistID, userID};
    }
}
