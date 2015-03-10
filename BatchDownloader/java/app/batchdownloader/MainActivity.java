package app.batchdownloader;

import android.app.Activity;
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
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.CopyOnWriteArrayList;

public class MainActivity extends Activity {
	//private ProgressDialog simpleWaitDialog;
	private int spawnedTasks = 0;
	private int errorTasks = 0;
	private int finishedTasks = 0;
	MyExpandableListAdapter adapter;

	SparseArray<Group> groups = new SparseArray<Group>();
	Group inProgGroup = new Group("In Progress");
	Group finishedGroup = new Group("Finished");
	Group failedGroup = new Group("Failed");

	List<String> list = new CopyOnWriteArrayList<String>();
	Iterator<String> iter;

	@Override
	protected void onResume() {
		super.onResume();

		SharedPreferences pref = getApplicationContext().getSharedPreferences("MyPref", MODE_PRIVATE);
		String restoredurls = pref.getString("url", null);
		String restoredpath = pref.getString("path", null);
		if (null != restoredurls) {
			EditText edit = (EditText) findViewById(R.id.editText);
			edit.setText(restoredurls);
		}
		if (null != restoredpath) {
			EditText edit = (EditText) findViewById(R.id.editText2);
			edit.setText(restoredpath);
		}

	}

	@Override
	protected void onPause() {
		super.onPause();

		SharedPreferences pref = getApplicationContext().getSharedPreferences("MyPref", MODE_PRIVATE);
		SharedPreferences.Editor editor = pref.edit();

		EditText url_edit = (EditText) findViewById(R.id.editText);
		String url = url_edit.getEditableText().toString();
		EditText path = (EditText) findViewById(R.id.editText2);
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
		if (iter.hasNext()) {
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
				EditText url_edit = (EditText) findViewById(R.id.editText);
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

		groups.append(0, inProgGroup);
		groups.append(1, finishedGroup);
		groups.append(2, failedGroup);

		ExpandableListView listView = (ExpandableListView) findViewById(R.id.listView);
		adapter = new MyExpandableListAdapter(this, groups);
		listView.setAdapter(adapter);
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
			EditText filepath = (EditText) findViewById(R.id.editText2);
			String filepathstr = filepath.getEditableText().toString();

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

	public class Group {

		public String string;
		public final List<String> children = new ArrayList<String>();

		public Group(String string) {
			this.string = string;
		}

	}

	private class MyExpandableListAdapter extends BaseExpandableListAdapter {

		private final SparseArray<Group> groups;
		public LayoutInflater inflater;
		public Activity activity;


		public MyExpandableListAdapter(Activity act, SparseArray<Group> groups) {
			activity = act;
			this.groups = groups;
			inflater = act.getLayoutInflater();
		}

		@Override
		public Object getChild(int groupPosition, int childPosition) {
			return groups.get(groupPosition).children.get(childPosition);
		}

		@Override
		public long getChildId(int groupPosition, int childPosition) {
			return 0;
		}

		@Override
		public View getChildView(int groupPosition, final int childPosition,
		                         boolean isLastChild, View convertView, ViewGroup parent) {
			final String children = (String) getChild(groupPosition, childPosition);
			TextView text = null;
			if (convertView == null) {
				convertView = inflater.inflate(R.layout.row_layout, null);
			}
			text = (TextView) convertView.findViewById(R.id.textView1);
			text.setText(children);
			convertView.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					Toast.makeText(activity, children, Toast.LENGTH_SHORT).show();
				}
			});
			return convertView;
		}

		@Override
		public int getChildrenCount(int groupPosition) {
			return groups.get(groupPosition).children.size();
		}

		@Override
		public Object getGroup(int groupPosition) {
			return groups.get(groupPosition);
		}

		@Override
		public int getGroupCount() {
			return groups.size();
		}

		@Override
		public void onGroupCollapsed(int groupPosition) {
			super.onGroupCollapsed(groupPosition);
		}

		@Override
		public void onGroupExpanded(int groupPosition) {
			super.onGroupExpanded(groupPosition);
		}

		@Override
		public long getGroupId(int groupPosition) {
			return 0;
		}

		@Override
		public View getGroupView(int groupPosition, boolean isExpanded,
		                         View convertView, ViewGroup parent) {
			if (convertView == null) {
				convertView = inflater.inflate(R.layout.listrow_group, null);
			}
			Group group = (Group) getGroup(groupPosition);
			((CheckedTextView) convertView).setText(group.string);
			((CheckedTextView) convertView).setChecked(isExpanded);
			return convertView;
		}

		@Override
		public boolean hasStableIds() {
			return false;
		}

		@Override
		public boolean isChildSelectable(int groupPosition, int childPosition) {
			return false;
		}

	}
}
