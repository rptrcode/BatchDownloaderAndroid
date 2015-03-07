package app.batchdownloader;

import android.app.ProgressDialog;
import android.os.AsyncTask;
import android.os.Environment;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;


public class MainActivity extends ActionBarActivity {
    private ProgressDialog simpleWaitDialog;
    List<String> list = new ArrayList<String>();


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Button gobtn = (Button)findViewById(R.id.button);
        gobtn.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                // String downloadUrl = "https://www.google.co.in/images/srpr/logo11w.png";
                EditText url_edit = (EditText) findViewById(R.id.editText);
                String str = url_edit.getEditableText().toString();
                parse(str);

                if (!list.isEmpty()) {
                    Iterator<String> iter = list.iterator();
                    while (iter.hasNext()) {
                        String downloadUrl = iter.next();
                        new Downloader().execute(downloadUrl, downloadUrl.substring(downloadUrl.lastIndexOf("/") + 1));
                    }
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

    final class Downloader extends AsyncTask {

        @Override
        protected void onPreExecute() {
            Log.i("PTRLOG", "onPreExecute Called");
            simpleWaitDialog = ProgressDialog.show(MainActivity.this,
                    "Wait", "Downloading Image");
        }

        @Override
        protected void onPostExecute(Object result) {
            Log.i("PTRLOG", "onPostExecute Called");
            simpleWaitDialog.dismiss();
        }

        @Override
        protected Object doInBackground(Object... params) {
            Log.d("PTRLOG", "doinback");

            final DefaultHttpClient client = new DefaultHttpClient();
            HttpGet getRequest = new HttpGet((String)params[0]);
            try {
                HttpResponse response = client.execute(getRequest);
                int status = response.getStatusLine().getStatusCode();
                if (status != HttpStatus.SC_OK){
                    Log.d("PTRLOG","http status error "+status);
                    return null;
                }

                Header[] h = response.getAllHeaders();
                for (int i = 0; i < h.length; i++) {
                    Log.d("PTRLOG",h[i].getName() + "  " + h[i].getValue());
                }

                HttpEntity entity = response.getEntity();
                if(entity == null) {
                    Log.d("PTRLOG","http entity null");
                    return null;
                }

                InputStream inputstream = entity.getContent();
                if(inputstream == null) {
                    Log.d("PTRLOG","inputstream null");
                    return null;
                }
                Log.d("PTRLOG","Doinback file");

                EditText filepath = (EditText) findViewById(R.id.editText2);
                String filepathstr = filepath.getEditableText().toString();
                Log.d("PTRLOG","filepathstr "+filepathstr);
                final File file = new File(filepathstr, (String)params[1]);
                OutputStream outstream = new FileOutputStream(file);
                try{
                    final byte[] buffer = new byte[1024];
                    int read;
                    while ((read = inputstream.read(buffer)) != -1)
                        outstream.write(buffer, 0, read);

                } catch(Exception e) {
                    Log.d("PTRLOG","doinback file exception="+e.getMessage());
                } finally{
                    Log.d("PTRLOG","doinback file close");
                    outstream.close();
                }
            } catch(Exception e) {
                Log.d("PTRLOG","doinback exception");
                e.printStackTrace();
            }
            return null;
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}
