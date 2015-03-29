package app.batchdownloader;

import android.app.Activity;
import android.app.Dialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.util.SparseArray;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ExpandableListView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import ar.com.daidalos.afiledialog.FileChooserDialog;

public class MainActivity extends Activity {
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

	String clipboardUrl = "";


	FileChooserDialog fileDialog;


	@Override
	protected void onResume() {
		super.onResume();

		SharedPreferences pref = getApplicationContext().getSharedPreferences("MyPref", MODE_PRIVATE);
		String restoredUrls;
		if (clipboardUrl.isEmpty()) {
			restoredUrls = pref.getString("url", null);
		} else {
			restoredUrls = clipboardUrl;
			clipboardUrl = "";
		}
		if (null != restoredUrls) {
			EditText edit = (EditText) findViewById(R.id.url_edit_text);
			edit.setText(restoredUrls);
		}


		String restoredPath = pref.getString("path", null);
		if (null != restoredPath) {
			TextView edit = (TextView) findViewById(R.id.path_text);
			edit.setText(restoredPath);
		}

	}

	@Override
	protected void onPause() {
		super.onPause();

		SharedPreferences pref = getApplicationContext().getSharedPreferences("MyPref", MODE_PRIVATE);
		SharedPreferences.Editor editor = pref.edit();

		EditText url_edit = (EditText) findViewById(R.id.url_edit_text);
		if (url_edit != null) {
			String url = url_edit.getEditableText().toString();
			editor.putString("url", url);
		}
		TextView path = (TextView) findViewById(R.id.path_text);
		if (path != null) {
			String pathname = path.getText().toString();
			editor.putString("path", pathname);
		}


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
			new Downloader(MainActivity.this).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, downloadUrl, downloadUrl.substring(downloadUrl.lastIndexOf("/") + 1));
		}
	}

	public void singleFileDownload(String url) {
		//called by clipboard
		clipboardUrl = url;

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
					for (int i = 0; i < 10 && iter.hasNext(); i++) {
						batchDownload();
					}
				} else {
					//new Downloader(MainActivity.this).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, str, str.substring(str.lastIndexOf("/") + 1));
					singleFileDownload(str);
				}
			}
		});


		tasksGroups.append(0, spawnedTasksGroup);
		tasksGroups.append(1, errorTasksGroup);
		tasksGroups.append(2, finishedTasksGroup);

		ExpandableListView listView = (ExpandableListView) findViewById(R.id.listView);
		adapter = new ExpandableListAdapter(this, tasksGroups);
		listView.setAdapter(adapter);

		ClipboardListener mClipboardListener = new ClipboardListener(MainActivity.this);


		TextView path = (TextView) findViewById(R.id.path_text);
		path.setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View v) {
				Log.e("PTRLOG", "edit onclick");

				fileDialog.show();

			}
		});

		fileDialog = new FileChooserDialog(MainActivity.this);
		fileDialog.setFolderMode(true);

		fileDialog.addListener(new FileChooserDialog.OnFileSelectedListener() {
			public void onFileSelected(Dialog source, File folder) {
				source.hide();
				Toast toast = Toast.makeText(source.getContext(), "Folder selected: " + folder.getPath(), Toast.LENGTH_LONG);
				toast.show();
				TextView path = (TextView) findViewById(R.id.path_text);
				path.setText(folder.getPath());
			}

			public void onFileSelected(Dialog source, File folder, String name) {
				source.hide();
				Toast toast = Toast.makeText(source.getContext(), "File created: " + folder.getPath() + "/" + name, Toast.LENGTH_LONG);
				toast.show();
				TextView path = (TextView) findViewById(R.id.path_text);
				path.setText(folder.getPath());
			}
		});


	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {

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

}
