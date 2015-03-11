package app.batchimagedownloader;

import android.app.Activity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.util.SparseArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseExpandableListAdapter;
import android.widget.Button;
import android.widget.CheckedTextView;
import android.widget.EditText;
import android.widget.ExpandableListView;
import android.widget.TextView;
import android.widget.Toast;


import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class MainActivity extends Activity {
	//private ProgressDialog simpleWaitDialog;
	private int spawnedTasks = 0;
	private int errorTasks = 0;
	private int finishedTasks = 0;
	ExpandableListAdapter adapter;

	SparseArray<Group> groups = new SparseArray<Group>();
	Group inProgGroup = new Group("In Progress");
	Group finishedGroup = new Group("Finished");
	Group failedGroup = new Group("Failed");

	List<String> list = new CopyOnWriteArrayList<String>();
	Iterator<String> iter;

	String clipboardUrl="";
	@Override
	protected void onResume() {
		super.onResume();

		SharedPreferences pref = getApplicationContext().getSharedPreferences("MyPref", MODE_PRIVATE);
		String restoredUrls;
		if(clipboardUrl.isEmpty()) {
			restoredUrls = pref.getString("url", null);
		}
		else {
			restoredUrls = clipboardUrl;
			clipboardUrl = "";
		}
		String restoredPath = pref.getString("path", null);
		if (null != restoredUrls) {
			EditText edit = (EditText) findViewById(R.id.url_edit_text);
			edit.setText(restoredUrls);
		}
		if (null != restoredPath) {
			EditText edit = (EditText) findViewById(R.id.path_edit_text);
			edit.setText(restoredPath);
		}

	}

	@Override
	protected void onPause() {
		super.onPause();

		SharedPreferences pref = getApplicationContext().getSharedPreferences("MyPref", MODE_PRIVATE);
		SharedPreferences.Editor editor = pref.edit();

		EditText url_edit = (EditText) findViewById(R.id.url_edit_text);
		String url = url_edit.getEditableText().toString();
		EditText path = (EditText) findViewById(R.id.path_edit_text);
		String pathname = path.getEditableText().toString();

		editor.putString("url", url);
		editor.putString("path", pathname);
		editor.commit();

	}

	private void reset() {
		spawnedTasks = errorTasks = finishedTasks = 0;
		list.clear();
	}

	private void batchDownload() {
		if (iter != null && iter.hasNext()) {
			String downloadUrl = iter.next();
			Log.e("PTRLOG", "batchDownload " + downloadUrl);
			new Downloader(MainActivity.this).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, downloadUrl, downloadUrl.substring(downloadUrl.lastIndexOf("/") + 1));
		}
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		Button gobtn = (Button) findViewById(R.id.button);
		gobtn.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				EditText url_edit = (EditText) findViewById(R.id.url_edit_text);
				String str = url_edit.getEditableText().toString();
				reset();
				parse(str);
				if (!list.isEmpty()) {
					Log.e("PTRLOG", "onclick batchdownload");
					for (int i = 0; i < 10 && iter.hasNext(); i++) {
						batchDownload();
					}
				} else {
					new Downloader(MainActivity.this).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, str, str.substring(str.lastIndexOf("/") + 1));
				}
			}
		});

		ClipboardManager clipBoard = (ClipboardManager)getSystemService(CLIPBOARD_SERVICE);
		clipBoard.addPrimaryClipChangedListener( new ClipboardListener() );

		groups.append(0, inProgGroup);
		groups.append(1, finishedGroup);
		groups.append(2, failedGroup);

		ExpandableListView listView = (ExpandableListView) findViewById(R.id.listView);
		adapter = new ExpandableListAdapter(this, groups);
		listView.setAdapter(adapter);
	}

	public class ClipboardListener implements ClipboardManager.OnPrimaryClipChangedListener
	{
		String clipText ="";
		ClipboardManager clipBoard = (ClipboardManager)getSystemService(CLIPBOARD_SERVICE);

		public void onPrimaryClipChanged()
		{
			Log.e("PTRLOG","ClipboardListener onPrimaryClipChanged");
			ClipData clipData = clipBoard.getPrimaryClip();
			ClipData.Item item = clipData.getItemAt(0);
			if(clipText.equals(item.getText().toString())) {
				return;
			} else {
				clipText = item.getText().toString();
				Log.e("PTRLOG", "ClipboardListener onPrimaryClipChanged " + clipText);
				if(clipText.startsWith("http://")) {
					clipboardUrl = clipText;
					Log.e("PTRLOG", "ClipboardListener clipboardUrl set");
				}
			}

		}
	}

	private void parse(String url) {
		int start = url.indexOf("[");
		int mid = url.lastIndexOf(":");
		int end = url.indexOf("]");

		if (start == -1 || mid == -1 || end == -1) {
			return;
		}

		String range1 = url.substring(start + 1, mid);
		String range2 = url.substring(mid + 1, end);

		String result;
		for (int i = Integer.parseInt(range1); i <= Integer.parseInt(range2); i++) {
			result = url.substring(0, start) + i + url.substring(end + 1);
			list.add(result);
		}

		iter = list.iterator();
	}

	class Downloader extends AsyncTask<Object, Void, Void> {
		private MainActivity mainContext;
		private boolean error = false;
		private String filename;

		public Downloader(MainActivity context) {
			mainContext = context;
		}

		@Override
		protected void onPreExecute() {
			MainActivity.this.runOnUiThread(new Runnable() {
				public void run() {
					TextView path = (TextView) mainContext.findViewById(R.id.textView);
					spawnedTasks++;
					path.setText(String.valueOf(spawnedTasks) + "/" + String.valueOf(errorTasks) + "/" + String.valueOf(finishedTasks));
				}
			});

		}

		@Override
		protected void onPostExecute(Void aVoid) {
			super.onPostExecute(aVoid);

			MainActivity.this.runOnUiThread(new Runnable() {
				public void run() {

					int pos = inProgGroup.children.indexOf(filename);
					if (pos != -1) {//already existing files wont be pushed
						inProgGroup.children.remove(pos);
					}
					TextView path = (TextView) mainContext.findViewById(R.id.textView);
					if (!error) {
						finishedTasks++;
						path.setText(String.valueOf(spawnedTasks) + "/" + String.valueOf(errorTasks) + "/" + String.valueOf(finishedTasks));
						finishedGroup.children.add(filename);
					} else {
						errorTasks++;
						path.setText(String.valueOf(spawnedTasks) + "/" + String.valueOf(errorTasks) + "/" + String.valueOf(finishedTasks));
						failedGroup.children.add(filename);
					}
					mainContext.adapter.notifyDataSetChanged();
					Log.e("PTRLOG", "onPostExecute batchDownload");
					mainContext.batchDownload();
				}
			});

		}

		@Override
		protected Void doInBackground(Object... params) {
			EditText filepath = (EditText) findViewById(R.id.path_edit_text);
			String filepathstr = filepath.getEditableText().toString();

			File directory = new File(filepathstr);
			if(!directory.exists()) {
				final boolean mkdirs = directory.mkdirs();
			}

			filename = (String) params[1];
			final File file = new File(filepathstr, filename);
			if (file.exists()) {
				error = true;
				return null;
			}

			MainActivity.this.runOnUiThread(new Runnable() {
				public void run() {
					inProgGroup.children.add(filename);
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
}
