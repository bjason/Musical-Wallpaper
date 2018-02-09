package grabber;

import org.json.JSONObject;

public class QMusicGrabber extends APlayerAPIGrabber {

	public QMusicGrabber(String transCode) {
		super(transCode);
		// TODO Auto-generated constructor stub
	}
	
	@Override
	protected void showErr(JSONObject data) {
		// TODO Auto-generated method stub
		if ((int) data.get("ResultCode") != 1) {
			errorCode = "Error processing data. " + (String) data.get("ErrCode");
			if ((String)data.get("ErrCode") == "3001") {
				errorCode += "\nIt may cause by your account is private.";
				errorCode += "\nGo to profile check if there is a lock under the avatar.";
			}
		}
	}
}
