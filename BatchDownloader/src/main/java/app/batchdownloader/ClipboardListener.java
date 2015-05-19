package app.batchdownloader;

import android.content.ClipData;
import android.content.ClipboardManager;

public class ClipboardListener implements ClipboardManager.OnPrimaryClipChangedListener {
	String clipText = "";
	ClipboardManager mClipboard;
	MainActivity mActivity;

	public ClipboardListener(MainActivity activity) {
		mActivity = activity;
		mClipboard = (ClipboardManager) mActivity.getSystemService(mActivity.CLIPBOARD_SERVICE);
		mClipboard.addPrimaryClipChangedListener(this);
	}


	public void onPrimaryClipChanged() {

		ClipData clipData = mClipboard.getPrimaryClip();
		ClipData.Item item = clipData.getItemAt(0);
		if (clipText.equals(item.getText().toString())) {
			return;
		} else {
			clipText = item.getText().toString();
			if (clipText.startsWith("http://") || clipText.startsWith("https://")) {
				//clipboardUrl = clipText;
				mActivity.alert("Started downloading " + clipText);
				mActivity.singleFileDownload(clipText);
			}
		}

	}
}