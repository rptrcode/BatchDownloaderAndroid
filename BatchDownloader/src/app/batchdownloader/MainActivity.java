package app.batchdownloader;

import android.app.Activity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.util.SparseArray;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseExpandableListAdapter;
import android.widget.Button;
import android.widget.CheckedTextView;
import android.widget.EditText;
import android.widget.ExpandableListView;
import android.widget.TextView;
import android.widget.Toast;
import app.batchdownloader.R;


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
	public int spawnedTasks = 0;
	public int errorTasks = 0;
	public int finishedTasks = 0;
	ExpandableListAdapter adapter;

	SparseArray<Group> tasksGroups = new SparseArray<Group>();
	Group spawnedTasksGroup = new Group("In Progress");
	Group finishedTasksGroup = new Group("Finished");
	Group errorTasksGroup = new Group("Failed");

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
		
		//without collapse clear will crash as view is still displayed.
		ExpandableListView listView = (ExpandableListView) findViewById(R.id.listView);
		listView.collapseGroup(0);
		listView.collapseGroup(1);
		listView.collapseGroup(2);
		spawnedTasksGroup.clear();
		errorTasksGroup.clear();
		finishedTasksGroup.clear();

	}

	public void batchDownload() {
		if (iter != null && iter.hasNext()) {
			String downloadUrl = iter.next();
			Log.e("PTRLOG", "batchDownload " + downloadUrl);
			new Downloader(MainActivity.this).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, downloadUrl, downloadUrl.substring(downloadUrl.lastIndexOf("/") + 1));
		}
	}
	
	public void singleFileDownload(String url) {
		Log.e("PTRLOG","singleFileDownload");

		String filename = url.substring(url.lastIndexOf("/") + 1);
		new Downloader(MainActivity.this).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, url, filename);
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
					//new Downloader(MainActivity.this).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, str, str.substring(str.lastIndexOf("/") + 1));
					singleFileDownload(str);
				}
			}
		});

		ClipboardManager clipBoard = (ClipboardManager)getSystemService(CLIPBOARD_SERVICE);
		clipBoard.addPrimaryClipChangedListener( new ClipboardListener() );

		tasksGroups.append(0, spawnedTasksGroup);
		tasksGroups.append(1, errorTasksGroup);
		tasksGroups.append(2, finishedTasksGroup);

		ExpandableListView listView = (ExpandableListView) findViewById(R.id.listView);
		adapter = new ExpandableListAdapter(this, tasksGroups);
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
					singleFileDownload(clipboardUrl);
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


@Override
public boolean onOptionsItemSelected(MenuItem item) {
	// TODO Auto-generated method stub
	return super.onOptionsItemSelected(item);
}
}
