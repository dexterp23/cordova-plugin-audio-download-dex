/*
       Licensed to the Apache Software Foundation (ASF) under one
       or more contributor license agreements.  See the NOTICE file
       distributed with this work for additional information
       regarding copyright ownership.  The ASF licenses this file
       to you under the Apache License, Version 2.0 (the
       "License"); you may not use this file except in compliance
       with the License.  You may obtain a copy of the License at

         http://www.apache.org/licenses/LICENSE-2.0

       Unless required by applicable law or agreed to in writing,
       software distributed under the License is distributed on an
       "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
       KIND, either express or implied.  See the License for the
       specific language governing permissions and limitations
       under the License.
*/

package org.apache.cordova.audiodownload;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaPlugin;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import android.content.Intent;
import android.app.Activity;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import android.content.Context;
import android.os.AsyncTask;
import java.io.File;
import java.net.URL;
import java.io.FileOutputStream;
import java.io.IOException;
import android.util.Log;
import org.apache.cordova.PermissionHelper;
import android.Manifest;
import org.apache.cordova.PluginResult;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;;


public class AudioDownload extends CordovaPlugin {
	
	private static final String TAG = "FBLOG";
	
	public static final int PERMISSION_DENIED_ERROR = 20;
	private static final int DOWNLOAD_AUDIO = 0;
	String url_download = null;
	String audio_title = null;
    String fileName = null;
    InputStream input = null;
    OutputStream output = null;
    HttpURLConnection connection = null;
	

    public boolean execute(String action, JSONArray args, CallbackContext callbackContext) throws JSONException {
		
		JSONObject options = args.optJSONObject(0);
		
		if (action.equals("PostAudio")) { 
			
			if (options != null) {
				
				url_download = options.getString("AudioURI");
				audio_title = options.getString("AudioTitle");
				if (audio_title == null || audio_title.isEmpty() || audio_title.equals("null")) audio_title = "audio_file";
        		String Return = startDownload();
				/*
				JSONObject r = new JSONObject();	
				r.put("AudioURI", options.getString("AudioURI"));
				callbackContext.success(r);
				*/
				callbackContext.success(Return);
			} else {
				callbackContext.error("There Is No Audio");	
				return false;
			}
			
        } else {
			callbackContext.error("Action Not Recognised");
            return false;
        }
        return true;
		
    }
	
	
	private void startDownload() {
		
		boolean needExternalStoragePermission = !PermissionHelper.hasPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE);
		
		if (needExternalStoragePermission) {
            
        	PermissionHelper.requestPermission(this, DOWNLOAD_AUDIO, Manifest.permission.READ_EXTERNAL_STORAGE);

        } else {
			
			AsyncTask<Void, Void, Void> task = new AsyncTask<Void, Void, Void>() { 
	
				@Override
				protected void onPreExecute() {
					fileName = getAudioFilePath(getActivity());
					File file = new File(fileName);
					if(file.exists()) file.delete();
				}
	
				@Override
				protected Void doInBackground(Void... arg0) {
					
					try {
						URL url = new URL(url_download);
						connection = (HttpURLConnection) url.openConnection();
						connection.connect();
	
						// expect HTTP 200 OK, so we don't mistakenly save error report
						// instead of the file
						if (connection.getResponseCode() != HttpURLConnection.HTTP_OK) {
							//Log.i(TAG, String.valueOf("no file"));
						}
	
						// this will be useful to display download percentage
						// might be -1: server did not report the length
						int fileLength = connection.getContentLength();
	
						// download the file
						input = connection.getInputStream();
						output = new FileOutputStream(fileName);
	
						byte data[] = new byte[4096];
						long total = 0;
						int count;
						while ((count = input.read(data)) != -1) {
							total += count;
							// publishing the progress....
							if (fileLength > 0) // only if total length is known
								//Log.i(TAG, String.valueOf("progress: " + (int) (total * 100 / fileLength)));
							output.write(data, 0, count);
						}
						
						return fileName;
						
					} catch (Exception e) {
						//return e.toString();
						//Log.i(TAG, String.valueOf("no connection"));
					} finally {
						try {
							if (output != null)
								output.close();
							if (input != null)
								input.close();
						} catch (IOException ignored) {
						}
	
						if (connection != null)
							connection.disconnect();
					}
					
					return null;
				}
	
				@Override
				protected void onPostExecute(Void result) {
					//Log.i(TAG, String.valueOf("done"));
				}
	
			};
			task.execute((Void[])null);
			
		}
        
    }
	
	
	private Activity getActivity() {
		return cordova.getActivity();
	}
	
	private String getAudioFilePath(Context context) {
    	return context.getExternalFilesDir(null).getAbsolutePath() + "/"+audio_title+".mp3";
    }
	
	public void onRequestPermissionResult(int requestCode, String[] permissions,
                                          int[] grantResults) throws JSONException
    {
        for(int r:grantResults)
        {
            if(r == PackageManager.PERMISSION_DENIED)
            {
                return;
            }
        }
        switch(requestCode)
        {
            case DOWNLOAD_AUDIO:
                startDownload();
            break;
        }
    }



}
