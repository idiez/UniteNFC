package es.quantum.unitenfc;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;

import topoos.Exception.TopoosException;
import topoos.Objects.User;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.preference.Preference;
import android.preference.PreferenceManager;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.PreferenceActivity;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.CheckBox;
import android.widget.ListView;

import com.google.analytics.tracking.android.EasyTracker;

import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.NameValuePair;
import org.apache.http.StatusLine;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.apache.http.protocol.HTTP;
import org.json.JSONException;
import org.json.JSONObject;

public class Settings extends PreferenceActivity implements OnPreferenceChangeListener{
	
	private CustomPrefDialog mCompoundEditTextPref;
    private SharedPreferences.OnSharedPreferenceChangeListener listener;

    @SuppressWarnings("deprecation")
	@Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.addPreferencesFromResource(R.xml.first_configuration);
        mCompoundEditTextPref = (CustomPrefDialog) findPreference("comp_edittext_pref");
        mCompoundEditTextPref.setOnPreferenceChangeListener(this);
        mCompoundEditTextPref.setPath("Some value");
  	  	mCompoundEditTextPref.setCompoundButtonText(getString(R.string.gallery));
  	  	mCompoundEditTextPref.setCompoundButtonListener(new View.OnClickListener() {			
      @Override
	      public void onClick(View v) {
	    	  Intent photoPickerIntent = new Intent(Intent.ACTION_PICK);
	    	  photoPickerIntent.setType("image/*");
	    	  startActivityForResult(photoPickerIntent, 3);  
	    	  mCompoundEditTextPref.getDialog().cancel();
	      }
	    });
        Preference p = findPreference("fb_connect");
        p.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {

                FacebookDialog fbd = new FacebookDialog();
                fbd.setCancelable(false);
                fbd.show(getFragmentManager(), "fb log");
                return false;
            }
        });
        listener = new SharedPreferences.OnSharedPreferenceChangeListener() {
            public void onSharedPreferenceChanged(SharedPreferences prefs, String key) {
                if(key.compareTo("username")==0){
                    Thread t = new Thread(new Runnable(){
                        @Override
                        public void run() {
                            try {
                                SharedPreferences prefss = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
                                String new_user_name = prefss.getString("username","");
                                HttpClient httpclient = new DefaultHttpClient();
                                HttpResponse response = null;
                                String post_url = "http://unitenfc.herokuapp.com/objects/users/name/"+prefss.getString("session","")+"/";
                                HttpPost socket = new HttpPost(post_url);
                                socket.setHeader( "Content-Type", "application/xml" );
                                socket.setHeader( "Accept", "*/*" );
                                JSONObject json = new JSONObject();
                                try {
                                    json.put("user_name", new_user_name);
                                } catch (JSONException e) {
                                    e.printStackTrace();
                                }
                                StringEntity entity = new StringEntity(json.toString(), HTTP.UTF_8);
                                socket.setEntity(entity);
                                try {
                                    response = httpclient.execute(socket);
                                } catch (ClientProtocolException e) {
                                    e.printStackTrace();
                                } catch (IOException e) {
                                    e.printStackTrace();
                                }
                                StatusLine statusLine = response.getStatusLine();
                                if(statusLine.getStatusCode() == HttpStatus.SC_OK){
                                    ByteArrayOutputStream out = new ByteArrayOutputStream();
                                    try {
                                        response.getEntity().writeTo(out);
                                        out.close();
                                    } catch (IOException e) {
                                        e.printStackTrace();
                                    }
                                    String responseString = out.toString();
                                }
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }
                    });
                    t.start();
                }
            }
        };
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        prefs.registerOnSharedPreferenceChangeListener(listener);

    }
 
    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        return true;
    }

    @Override
    public void onStart() {
        super.onStart();
        // The rest of your onStart() code.
        EasyTracker.getInstance(this).activityStart(this);  // Add this method.
    }

    @Override
    public void onStop(){
        super.onStop();
        EasyTracker.getInstance(this).activityStop(this);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent imageReturnedIntent) {
        super.onActivityResult(requestCode, resultCode, imageReturnedIntent);
        switch(requestCode) {
            case 3:
                if(resultCode == RESULT_OK){
                    Uri selectedImage = imageReturnedIntent.getData();
                    String[] filePathColumn = {MediaStore.Images.Media.DATA};
                    Cursor cursor = getContentResolver().query(
                                       selectedImage, filePathColumn, null, null, null);
                    cursor.moveToFirst();
                    int columnIndex = cursor.getColumnIndex(filePathColumn[0]);
                    final String filePath = cursor.getString(columnIndex);
                    cursor.close();
                    Thread t = new Thread(new Runnable(){

                        @Override
                        public void run() {

                            try {
                                User usr = null;
                                try {
                                    usr = topoos.Users.Operations.Get(getApplicationContext(), "me");
                                } catch (IOException e1) {
                                    e1.printStackTrace();
                                } catch (TopoosException e1) {
                                    e1.printStackTrace();
                                }
                                final String unique = TopoosInterface.UploadPIC(getApplicationContext(), (usr != null)?usr.getName():"test",filePath);
                                SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
                                Editor editor = prefs.edit();
                                editor.putString("imageuri", unique);
                                editor.commit();
                                TopoosInterface.setProfilePicture(getApplicationContext());
                                Thread t = new Thread(new Runnable(){
                                    @Override
                                    public void run() {
                                    try {
                                        SharedPreferences prefss = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
                                        String new_user_name = prefss.getString("username","");
                                        HttpClient httpclient = new DefaultHttpClient();
                                        HttpResponse response = null;
                                        String post_url = "http://unitenfc.herokuapp.com/objects/users/picuri/"+prefss.getString("session","")+"/";
                                        HttpPost socket = new HttpPost(post_url);
                                        socket.setHeader( "Content-Type", "application/xml" );
                                        socket.setHeader( "Accept", "*/*" );
                                        JSONObject json = new JSONObject();
                                        try {
                                            json.put("pic_uri", unique);
                                        } catch (JSONException e) {
                                            e.printStackTrace();
                                        }
                                        String deb = json.toString();
                                        StringEntity entity = new StringEntity(json.toString(), HTTP.UTF_8);

                                        socket.setEntity(entity);

                                        Log.i("REQUEST",socket.getRequestLine().toString());
                                        try {
                                            response = httpclient.execute(socket);
                                        } catch (ClientProtocolException e) {
                                            e.printStackTrace();
                                        } catch (IOException e) {
                                            e.printStackTrace();
                                        }
                                        StatusLine statusLine = response.getStatusLine();
                                        if(statusLine.getStatusCode() == HttpStatus.SC_OK){
                                            ByteArrayOutputStream out = new ByteArrayOutputStream();
                                            try {
                                                response.getEntity().writeTo(out);
                                                out.close();
                                            } catch (IOException e) {
                                                e.printStackTrace();
                                            }
                                            String responseString = out.toString();
                                        }
                                    } catch (Exception e) {
                                        e.printStackTrace();
                                    }
                                    }
                                });
                                t.start();
                                } catch (IOException e) {
                                e.printStackTrace();
                            } catch (TopoosException e) {
                                e.printStackTrace();
                            }
                        }
                    });
                    t.start();
                }
                break;
        }
    }
}