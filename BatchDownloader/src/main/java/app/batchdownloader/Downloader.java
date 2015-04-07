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
	private String filename;

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

				int pos = mainContext.spawnedTasksGroup.children.indexOf(filename);
				if (pos != -1) {//already existing files wont be pushed
					mainContext.spawnedTasksGroup.children.remove(pos);
				}
				TextView path = (TextView) mainContext.findViewById(R.id.textView);
				if (!error) {
					mainContext.finishedTasks++;
					path.setText(String.valueOf(mainContext.spawnedTasks) + "/" + String.valueOf(mainContext.errorTasks) + "/" + String.valueOf(mainContext.finishedTasks));
					mainContext.finishedTasksGroup.children.add(filename);
				} else {
					mainContext.errorTasks++;
					path.setText(String.valueOf(mainContext.spawnedTasks) + "/" + String.valueOf(mainContext.errorTasks) + "/" + String.valueOf(mainContext.finishedTasks));
					mainContext.errorTasksGroup.children.add(filename);
				}
				mainContext.adapter.notifyDataSetChanged();
				mainContext.batchDownload();
			}
		});

	}

	@Override
	protected Void doInBackground(Object... params) {
		TextView filepath = (TextView) mainContext.findViewById(R.id.path_text);
		TextView urlview = (TextView) mainContext.findViewById(R.id.url_edit_text);
		String urlString = urlview.getText().toString();
		String str = urlString.substring(urlString.indexOf("http://") + 6, urlString.lastIndexOf("/"));

		String filepathstr = filepath.getText().toString() + str;

		File directory = new File(filepathstr);
		if (!directory.exists()) {
			final boolean mkdirs = directory.mkdirs();
		}

		filename = (String) params[1];
		final File file = new File(filepathstr, filename);
		if (file.exists()) {
			error = true;
			return null;
		}

		mainContext.runOnUiThread(new Runnable() {
			public void run() {
				mainContext.spawnedTasksGroup.children.add(filename);
				mainContext.adapter.notifyDataSetChanged();
			}
		});

		try {
			URL url = new URL((String) params[0]);
			HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
			if (urlConnection.getContentLength() < 5000) {
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
