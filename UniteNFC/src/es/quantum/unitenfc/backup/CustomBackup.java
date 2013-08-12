package es.quantum.unitenfc.backup;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.graphics.Bitmap;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.util.Log;

import com.google.gson.Gson;

import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.StatusLine;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.protocol.HTTP;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import es.quantum.unitenfc.Objects.Friend;
import es.quantum.unitenfc.Objects.NFCPoint;
import es.quantum.unitenfc.Objects.UserInfo;
import es.quantum.unitenfc.TopoosInterface;
import topoos.AccessTokenOAuth;
import topoos.Exception.TopoosException;
import topoos.Objects.User;

public class CustomBackup {
	
	public static final String BACKUP_URI = "http://unitenfc.herokuapp.com/objects/users/";

	public boolean requestbackup(Context ctx){
        final Context ctxx = ctx;
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(ctx);
		AccessTokenOAuth token = topoos.AccessTokenOAuth.GetAccessToken(ctx);
    	if (token == null || !token.isValid()) return false;
		User usr = null;
		try {
			usr = topoos.Users.Operations.Get(ctx, "me");
			String filename = usr.getId();
			Editor editor = prefs.edit();
            final String session = usr.getId();
			editor.putString("session", session);				//saves last user session
			editor.commit();
			//user name
			final String username = usr.getName();
			//user image
			final String imageuri = prefs.getString("imageuri", "dummy_4");
            Thread t = new Thread(new Runnable(){
                @Override
                public void run() {
                try {
                    HttpClient httpclient = new DefaultHttpClient();
                    HttpResponse response = null;
                    String post_url = "http://unitenfc.herokuapp.com/objects/users/new/";
                    HttpPost socket = new HttpPost(post_url);
                    socket.setHeader( "Content-Type", "application/xml" );
                    socket.setHeader( "Accept", "*/*" );
                    JSONObject json = new JSONObject();
                    try {
                        json.put("user_id", session);
                        json.put("user_name", username);
                        json.put("user_pic_uri", imageuri);
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
                            requestrestore(ctxx);
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
			return false;
		} catch (TopoosException e) {
			e.printStackTrace();
			return false;
		}

		return true;

	}

	public boolean requestrestore(Context ctx){
		
		AccessTokenOAuth token = topoos.AccessTokenOAuth.GetAccessToken(ctx);    	
    	if (token == null || !token.isValid()) return false;
		User usr = null;
		final Context ctxx = ctx;
		try {
			usr = topoos.Users.Operations.Get(ctx, "me");
			SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(ctx);
			String filename = usr.getId();
			Editor editor = prefs.edit();
			editor.putString("session", usr.getId())
                    .putString("username","")
                    .remove("imageuri")
                    .putString("checkpoints","")
                    .putString("regpoints","")
                    .putString("friends","");

			editor.commit();
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		} catch (TopoosException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		final String filename = usr.getId();
		
		Thread t = new Thread(new Runnable(){

			@Override
			public void run() {
				// TODO Auto-generated method stub
				
    	
			HttpClient httpclient = new DefaultHttpClient();
    	    HttpResponse response = null;
			try {
                response = httpclient.execute(new HttpGet(BACKUP_URI+filename));
				//response = httpclient.execute(new HttpGet(BACKUP_URI+filename+FILE_TYPE));
			} catch (ClientProtocolException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
    	    StatusLine statusLine = response.getStatusLine();
    	    if(statusLine.getStatusCode() == HttpStatus.SC_OK){
    	        ByteArrayOutputStream out = new ByteArrayOutputStream();
    	        try {
					response.getEntity().writeTo(out);
					out.close();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
    	        
    	        String responseString = out.toString();

    	        Gson gson = new Gson();
    	        UserInfo session = gson.fromJson(responseString, UserInfo.class);

//    	        UserInfo session = gson.fromJson(responseString.substring(0, responseString.length()-2), UserInfo.class);
    	        
    	        //String[] param = responseString.split("\n");
    	        
    			SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(ctxx);
    			Editor editor = prefs.edit();
    			editor.putString("username", session.getUser_name());
    			editor.putString("imageuri", session.getPic_uri());
    			String visited = "";
    			for(NFCPoint element:session.getVisited()){
    				visited = visited+element.toString()+"ñ";
    			}
    			editor.putString("checkpoints", visited);
    			String reg = "";
    			for(NFCPoint element:session.getRegistered()){
    				reg = reg+element.toString()+"ñ";
    			}
    			editor.putString("regpoints", reg);
    			String friends = "";
    			for(Friend element:session.getFriends()){
    				friends = friends+element.toString()+"ñ";
    			}
    			editor.putString("friends", friends);
    			editor.commit();

    	        Log.i("REC", "USERNAME: "+session.getUser_name());
    	        Log.i("REC", "URI: "+session.getPic_uri());
    	        Log.i("REC", "CHECKPOINTS: "+visited);
    	        Log.i("REC", "REGPOINTS: "+reg);
    	        Log.i("REC", "FRIENDS: "+friends);
    	        
    	        
				String i = topoos.Images.Operations.GetImageURIThumb(session.getPic_uri(),topoos.Images.Operations.SIZE_SMALL);
				Bitmap bmp = TopoosInterface.LoadImageFromWebOperations(i);
				String path = Environment.getExternalStorageDirectory().toString()+"/unitenfc/";
				File file = new File(path,"profile.png");
				FileOutputStream out1;
				try {
					out1 = new FileOutputStream(file);
					bmp.compress(Bitmap.CompressFormat.PNG, 100, out1);
				} catch (FileNotFoundException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (NullPointerException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				}
				
				List<String> friendlist = TopoosInterface.itemize(friends);
				for(String element:friendlist){
					String i1 = topoos.Images.Operations.GetImageURIThumb(TopoosInterface.extract(element, 2),topoos.Images.Operations.SIZE_SMALL);
					Bitmap bmp1 = TopoosInterface.LoadImageFromWebOperations(i1);
                    int width = bmp1.getWidth();
                    int heigth = bmp1.getHeight();
                    Bitmap croppedBmp1;
                    if(width == heigth){
                        croppedBmp1 = bmp1;
                    }
                    else if(width > heigth){
                        croppedBmp1 = Bitmap.createBitmap(bmp1,(width-heigth)/2, 0, heigth, heigth );
                    }
                    else {
                        croppedBmp1 = Bitmap.createBitmap(bmp1,0, (heigth-width)/2, width, width );
                    }
					String path1 = Environment.getExternalStorageDirectory().toString()+"/unitenfc/";
					File file1 = new File(path1,TopoosInterface.extract(element, 0)+".png");
					FileOutputStream out11;
					try {
						out11 = new FileOutputStream(file1);
						Bitmap.createScaledBitmap(croppedBmp1,100,100,false).compress(Bitmap.CompressFormat.PNG, 100, out11);
					} catch (FileNotFoundException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
				    } catch (NullPointerException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				    }
				}
            }else if(statusLine.getStatusCode() == HttpStatus.SC_NOT_FOUND){
    	        Log.i("LLEGO","AQUI");
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        requestbackup(ctxx);
                    }
                }).start();
    	    } else{
    	        //Closes the connection.
    	        try {
					response.getEntity().getContent().close();
				} catch (IllegalStateException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
    	        try {
					throw new IOException(statusLine.getReasonPhrase());
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
    	    }
			}});
    	t.start();
		

		return true;
		
	}
	

	
	
	
}
