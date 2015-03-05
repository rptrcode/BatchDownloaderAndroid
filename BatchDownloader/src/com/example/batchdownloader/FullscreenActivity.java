package com.example.batchdownloader;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;

import android.R.string;
import android.annotation.TargetApi;
import android.app.Activity;
import android.app.ProgressDialog;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.text.Editable;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.view.View.OnClickListener;

public class FullscreenActivity extends Activity {
	private ProgressDialog simpleWaitDialog;
	List<String> list = new ArrayList<String>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_fullscreen);

        Button gobtn = (Button)findViewById(R.id.go_btn);
        gobtn.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
			   // String downloadUrl = "https://www.google.co.in/images/srpr/logo11w.png";
			
				
		        EditText url_edit = (EditText)findViewById(R.id.url);
    			String str = url_edit.getEditableText().toString();
    			parse(str);
    			
    			if(!list.isEmpty()) {
    				Iterator<String> iter = list.iterator();
    				while(iter.hasNext()){
    					String downloadUrl = iter.next();
    		            new Downloader().execute(downloadUrl, downloadUrl.substring(downloadUrl.lastIndexOf("/")+1));
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
    			simpleWaitDialog = ProgressDialog.show(FullscreenActivity.this,
    					"Wait", "Downloading Image");
    		}
            
    		@Override
    		protected void onPostExecute(Object result) {
    			Log.i("Async-Example", "onPostExecute Called");
    			simpleWaitDialog.dismiss();
    		}
    		
        	@Override
        	protected Object doInBackground(Object... params) {
       		 Log.d("PTRLOG","doinback");

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
            	 Log.d("PTRLOG","doinback file");	 
            	 final File file = new File(Environment.getExternalStorageDirectory().getPath(), (String)params[1]);
            	 OutputStream outstream = new FileOutputStream(file);
            	 try{
                     final byte[] buffer = new byte[1024];
                     int read;
                     while ((read = inputstream.read(buffer)) != -1)
                    	 outstream.write(buffer, 0, read);
                     
            	 } finally {
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
        


}
