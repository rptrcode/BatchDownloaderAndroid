package app.batchdownloader;

import android.app.Activity;
import android.app.ListActivity;
import android.app.ProgressDialog;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Environment;
import android.os.SystemClock;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;


import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.MalformedInputException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.CopyOnWriteArrayList;

public class MainActivity extends Activity {
    //private ProgressDialog simpleWaitDialog;
    List<String> list = new CopyOnWriteArrayList<String>();
    private int count1=0;
    private int count2=0;
    private int count3=0;


//    private static final String[] items={"lorem", "ipsum", "dolor",
//            "sit", "amet", "consectetuer",
//            "adipiscing", "elit", "morbi",
//            "vel", "ligula", "vitae",
//            "arcu", "aliquet", "mollis",
//            "etiam", "vel", "erat",
//            "placerat", "ante",
//            "porttitor", "sodales",
//            "pellentesque", "augue",
//            "purus"};

    @Override
    protected void onResume() {
        super.onResume();

        SharedPreferences pref = getApplicationContext().getSharedPreferences("MyPref", MODE_PRIVATE);
        String restoredurls = pref.getString("url",null);
        String restoredpath = pref.getString("path", null);
        if(null != restoredurls){
            EditText edit = (EditText) findViewById(R.id.editText);
            edit.setText(restoredurls);
        }
        if(null != restoredpath){
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
        editor.putString("path",pathname);
        editor.commit();

    }


    private void reset(){
        count1=count2=count3=0;
        list.clear();
    }
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Button gobtn = (Button)findViewById(R.id.button);
        gobtn.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                EditText url_edit = (EditText) findViewById(R.id.editText);
                String str = url_edit.getEditableText().toString();
                reset();
                parse(str);
                if (!list.isEmpty()) {
                    Toast.makeText(MainActivity.this, "batch download "+list.size()+" files..", Toast.LENGTH_SHORT).show();
                    final Iterator<String> iter = list.iterator();
                    final Timer timer = new Timer();
//                    while (iter.hasNext()) {
                        timer.scheduleAtFixedRate(new TimerTask() {
                            public void run() {
                                Log.d("PTRLOG", "batch ");
                                for (int i = 0; i < 5 && iter.hasNext(); i++) {
                                    String downloadUrl = iter.next();
                                    Log.d("PTRLOG", "download " + downloadUrl);
                                    new Downloader(MainActivity.this).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, downloadUrl, downloadUrl.substring(downloadUrl.lastIndexOf("/") + 1));
                                }
                                if (!iter.hasNext()) {
                                    timer.cancel();
                                }
                            }
                        }, 1000, 10000);
//                    }
                }else{
                    new Downloader(MainActivity.this).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, str, str.substring(str.lastIndexOf("/") + 1));
                }
            }
        });


    }
    private void parse(String url){
        int start = url.indexOf("[");
        int mid = url.lastIndexOf(":");
        int end = url.indexOf("]");

        if(start == -1 || mid == -1|| end == -1){
            return;
        }

        String range1  = url.substring(start+1, mid);
        String range2  = url.substring(mid+1, end);

        String result;
        for(int i=Integer.parseInt(range1);i<=Integer.parseInt(range2);i++){
            result = url.substring(0,start)+i+url.substring(end+1);
            list.add(result);
        }
    }
//
//    class AddStringTask extends AsyncTask<Void, String, Void> {
//        @Override
//        protected Void doInBackground(Void... unused) {
//            for (String item : items) {
//                publishProgress(item);
//                SystemClock.sleep(200);
//            }
//
//            return(null);
//        }
//
//        @SuppressWarnings("unchecked")
//        @Override
//        protected void onProgressUpdate(String... item) {
//            ((ArrayAdapter<String>)getListAdapter()).add(item[0]);
//        }
//
//        @Override
//        protected void onPostExecute(Void unused) {
//        }
//    }
//
    class Downloader extends AsyncTask<Object,Void, Void>  {
    private MainActivity mainContext;
    private boolean error = false;
    private String filename;

    public Downloader(MainActivity context){
        mainContext = context;
    }

    @Override
    protected void onPreExecute() {
        MainActivity.this.runOnUiThread(new Runnable() {
            public void run() {
                TextView path = (TextView) mainContext.findViewById(R.id.textView);
                count1++;
                path.setText(String.valueOf(count1)+"/"+String.valueOf(count2)+"/"+String.valueOf(count3));
            }
        });

    }

    @Override
    protected void onPostExecute(Void aVoid) {
        super.onPostExecute(aVoid);

        MainActivity.this.runOnUiThread(new Runnable() {
            public void run() {
                TextView path = (TextView) mainContext.findViewById(R.id.textView);


                if(!error){
                    TextView log = (TextView) mainContext.findViewById(R.id.logView);
                    log.setText(filename+", "+log.getText());
                    count3++;
                    path.setText(String.valueOf(count1)+"/"+String.valueOf(count2)+"/"+String.valueOf(count3));
                }else{
                    count2++;
                    path.setText(String.valueOf(count1)+"/"+String.valueOf(count2)+"/"+String.valueOf(count3));
                }
            }
        });

    }

    @Override
        protected Void doInBackground(Object... params) {
            EditText filepath = (EditText) findViewById(R.id.editText2);
            String filepathstr = filepath.getEditableText().toString();

            final File file = new File(filepathstr, (String)params[1]);
            if(file.exists()) {
                error = true;
                MainActivity.this.runOnUiThread(new Runnable() {
                    public void run() {
                        Toast.makeText(MainActivity.this, file.toURI()+" exists!", Toast.LENGTH_SHORT).show();
                    }
                });

                return null;
            }

            filename = (String)params[1];

            URL url = null;
            try {
                url = new URL((String)params[0]);
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
            if(urlConnection == null){
                error = true;
                return null;
            }
            if(urlConnection.getContentLength()<10000){
                Log.d("PTRLOG","getContentLength()<10000");
                error = true;
                return null;
            }
            InputStream inputStream = null;
            try {
                inputStream  = urlConnection.getInputStream();
            } catch (IOException e) {
                e.printStackTrace();
            }

            if(inputStream == null) {
                Log.d("PTRLOG","inputstream null");
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

                while ((bufferLength = inputStream.read(buffer)) > 0){
                    outStream.write(buffer, 0, bufferLength);
                }
                } catch(Exception e) {
                    Log.d("PTRLOG", "doinback file exception=" + e.getMessage());
                error = true;
                } finally{
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
