package app.batchdownloader;

import android.os.AsyncTask;
import android.widget.TextView;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

public class Downloader extends AsyncTask<Object, Void, Void> {
	private MainActivity mainContext;
	private boolean error = false;
	private FileInfo mFileInfo;

	public Downloader(MainActivity context) {
		mainContext = context;
	}

	@Override
	protected void onPreExecute() {
		mainContext.runOnUiThread(new Runnable() {
			public void run() {
				TextView path = (TextView) mainContext.findViewById(R.id.textView);
				mainContext.spawnedTasks++;
				path.setText(String.valueOf(mainContext.spawnedTasks) + "/" + String.valueOf(mainContext.errorTasks) + "/" + String.valueOf(mainContext.finishedTasks));
			}
		});

	}

	@Override
	protected void onPostExecute(Void aVoid) {
		super.onPostExecute(aVoid);

		mainContext.runOnUiThread(new Runnable() {
			public void run() {

				int pos = mainContext.spawnedTasksGroup.children().indexOf(mFileInfo);
				if (pos != -1) {//already existing files wont be pushed
					mainContext.spawnedTasksGroup.children().remove(pos);
				}
				TextView path = (TextView) mainContext.findViewById(R.id.textView);
				if (!error) {
					mainContext.finishedTasks++;
					path.setText(String.valueOf(mainContext.spawnedTasks) + "/" + String.valueOf(mainContext.errorTasks) + "/" + String.valueOf(mainContext.finishedTasks));
					mainContext.finishedTasksGroup.children().add(mFileInfo);
				} else {
					mainContext.errorTasks++;
					path.setText(String.valueOf(mainContext.spawnedTasks) + "/" + String.valueOf(mainContext.errorTasks) + "/" + String.valueOf(mainContext.finishedTasks));
					mainContext.errorTasksGroup.children().add(mFileInfo);
				}
				mainContext.adapter.notifyDataSetChanged();
				mainContext.batchDownload();
			}
		});

	}

	@Override
	protected Void doInBackground(Object... params) {
		TextView filepath = (TextView) mainContext.findViewById(R.id.path_text);
//		TextView urlview = (TextView) mainContext.findViewById(R.id.url_edit_text);
		String urlString = (String) params[0];
		String str = "";
		if (urlString.startsWith("http://"))
			str = urlString.substring(urlString.indexOf("http://") + 6, urlString.lastIndexOf("/"));
		else if (urlString.startsWith("https://"))
			str = urlString.substring(urlString.indexOf("https://") + 7, urlString.lastIndexOf("/"));
		else
			return null;

		String filepathStr = filepath.getText().toString() + str;
		File directory = new File(filepathStr);
		if (!directory.exists()) {
			final boolean mkdirs = directory.mkdirs();
		}

		String filename = (String) params[1];
		mFileInfo = new FileInfo(filepathStr, filename);

		final File file = new File(filepathStr, filename);
		if (file.exists()) {
			error = true;
			mFileInfo.setError("File Already Exists");
			return null;
		}

		mainContext.runOnUiThread(new Runnable() {
			public void run() {
				mainContext.spawnedTasksGroup.children().add(mFileInfo);
				mainContext.adapter.notifyDataSetChanged();
			}
		});

		try {
			URL url = new URL((String) params[0]);
			HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
			if (urlConnection.getContentLength() < 5000) {
				mFileInfo.setError("File size < 5KB");
				error = true;
				return null;
			}
			InputStream inputStream = urlConnection.getInputStream();
			FileOutputStream outStream = new FileOutputStream(file);
			final byte[] buffer = new byte[1024];
			int bufferLength = 0;

			while ((bufferLength = inputStream.read(buffer)) > 0) {
				outStream.write(buffer, 0, bufferLength);
			}
			outStream.close();
		} catch (Exception e1) {
			error = true;
			e1.printStackTrace();
		}
		return null;
	}
}
