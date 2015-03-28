package app.batchdownloader;

import android.os.AsyncTask;
import android.util.Log;
import android.widget.TextView;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
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
				Log.e("PTRLOG", "onPostExecute batchDownload");
				mainContext.batchDownload();
			}
		});

	}

	@Override
	protected Void doInBackground(Object... params) {
		TextView filepath = (TextView) mainContext.findViewById(R.id.path_text);
		String filepathstr = filepath.getText().toString();

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

		URL url = null;
		try {
			url = new URL((String) params[0]);
		} catch (MalformedURLException e1) {
			error = true;
			e1.printStackTrace();
		}
		HttpURLConnection urlConnection = null;
		if (url != null) {
			try {
				urlConnection = (HttpURLConnection) url.openConnection();
			} catch (IOException e1) {
				error = true;
				e1.printStackTrace();
			}
		}
		if (urlConnection == null) {
			error = true;
			return null;
		}
		if (urlConnection.getContentLength() < 10000) {
			Log.d("PTRLOG", "getContentLength()<10000");
			error = true;
			return null;
		}
		InputStream inputStream = null;
		try {
			inputStream = urlConnection.getInputStream();
		} catch (IOException e) {
			e.printStackTrace();
		}

		if (inputStream == null) {
			Log.d("PTRLOG", "inputstream null");
			error = true;
			return null;
		}


		FileOutputStream outStream = null;
		try {
			outStream = new FileOutputStream(file);
		} catch (FileNotFoundException e1) {
			error = true;
			e1.printStackTrace();
		}
		try {
			final byte[] buffer = new byte[1024];
			int bufferLength = 0;

			while ((bufferLength = inputStream.read(buffer)) > 0) {
				outStream.write(buffer, 0, bufferLength);
			}
		} catch (Exception e) {
			Log.d("PTRLOG", "doinback file exception=" + e.getMessage());
			error = true;
		} finally {
			if (outStream != null) {
				try {
					outStream.close();
				} catch (IOException e) {
					error = true;
					e.printStackTrace();
				}
			}
		}
		return null;
	}
}
