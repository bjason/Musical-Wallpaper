package grabber;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.LinkedHashMap;
import java.util.Map;

import org.json.*;

import com.wrapper.spotify.exceptions.WebApiException;

import util.InvalidPlaylistURLException;
import util.NoneInPlaylistException;

public class APlayerAPIGrabber extends Grabber {
	public static final String[] TransCode_INDEX = {"", "020111", "", "020331", "020221"};
	private String TransCode;

	public APlayerAPIGrabber(String transCode) {
		TransCode = transCode;
	}

	private static String readAll(Reader rd) throws IOException {
		StringBuilder sb = new StringBuilder();
		int cp;
		while ((cp = rd.read()) != -1) {
			sb.append((char) cp);
		}
		return sb.toString();
	}

	@Override
	public void main() throws IOException, WebApiException, InvalidPlaylistURLException, NoneInPlaylistException {

		// using api provided by dongyonghui
		// api description
		// https://www.dongyonghui.com/default/20180128-ÍøÒ×ÔÆ¡¢¿á¹·¡¢QQÒôÀÖ¸èµ¥½Ó¿ÚAPI.html
		URL reqUrl = new URL("https://api.hibai.cn/api/index/index");

		// used to be "http://music.163.com/api/playlist/detail?id="
		String postContent = new JSONObject().put("TransCode", TransCode).put("OpenId", "123456789")
				.put("Body", new JSONObject().put("SongListId", playlistId)).toString();

		// create connection
		HttpURLConnection connection = (HttpURLConnection) reqUrl.openConnection();
		connection.setDoOutput(true);
		connection.setDoInput(true);
		connection.setRequestMethod("POST");
		connection.setUseCaches(false);
		connection.setInstanceFollowRedirects(true);
		connection.setRequestProperty("Content-Type", "application/json");
		connection.setRequestProperty("Accept", "text/plain");
		connection.connect();

		// post the request
		DataOutputStream out = new DataOutputStream(connection.getOutputStream());
		out.writeBytes(postContent);
		out.flush();
		out.close();

		// obtain the json file
		LinkedHashMap<String, String> titleAndImageNames = new LinkedHashMap<>();
		InputStream inputStream = connection.getInputStream();
		JSONObject data = getJson(inputStream);

		showErr(data);
		JSONArray tracks = data.getJSONArray("Body");
		setPlaylistName(tracks);

		if (tracks != null) {
			setNumTrack(tracks);

			for (int i = 0; i < numOfTracks; i++) {
				JSONObject track = tracks.getJSONObject(i);

				String trackName = track.getString("title");
				String artistName = track.getString("author");
				String imageUrl = track.getString("pic");

				setFileName(titleAndImageNames, i + 1, "", imageUrl, trackName, artistName);
			}
		} else throw new InvalidPlaylistURLException();
		downloadAlbumsToDirectory(titleAndImageNames);

		// close the connection
		connection.disconnect();
	}

	protected void showErr(JSONObject data) {
		if ((int) data.get("ResultCode") != 1) {
			errorCode = "Error processing data. " + (String) data.get("ErrCode");
			
		}
	}

	private JSONObject getJson(InputStream data) throws IOException {
		try {
			BufferedReader rd = new BufferedReader(new InputStreamReader(data, Charset.forName("UTF-8")));
			String jsonText = readAll(rd);
			JSONObject json = new JSONObject(jsonText);
			return json;
		} finally {
			data.close();
		}
	}

	@Override
	protected <T> void setPlaylistName(T playlist) {
		playlistName = playlistId.toString();
	}

	@Override
	protected <T> void setNumTrack(T playlist) throws NoneInPlaylistException {
		numOfTracks = ((JSONArray) playlist).length();
		isEmpty();
	}
}
