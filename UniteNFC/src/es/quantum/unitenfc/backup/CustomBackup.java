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
import org.apache.http.impl.client.DefaultHttpClient;

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
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(ctx);
		AccessTokenOAuth token = topoos.AccessTokenOAuth.GetAccessToken(ctx);    	
    	if (token == null || !token.isValid()) return false;
		User usr = null;
		try {
			usr = topoos.Users.Operations.Get(ctx, "me");
			String filename = usr.getId();
			Editor editor = prefs.edit();
			editor.putString("session", usr.getId());				//saves last user session
			editor.commit();
			//new user object
			UserInfo session = new UserInfo();
			//user name
			String username = prefs.getString("username", "");
			if(username.compareTo("")==0) username = usr.getName();
			session.setUser_name(username);
			//user image
			String imageuri = prefs.getString("imageuri", "dummy_4");
			session.setPic_uri(imageuri);
			//user checked NFCPoints
			String checkpoints = prefs.getString("checkpoints", "");
			List<String> nfcpc = TopoosInterface.itemize(checkpoints);
			List<NFCPoint> nfcp = new ArrayList<NFCPoint>();
			for(String element:nfcpc){
				NFCPoint elem = new NFCPoint();
				elem.setName(TopoosInterface.extract(element, 0));
				elem.setPosId(TopoosInterface.extract(element, 1));
				elem.setDate(TopoosInterface.extract(element, 2));
				nfcp.add(elem);
			}
			session.setVisited(nfcp);
			//user registered NFCPoints
			String regpoints = prefs.getString("regpoints", "");
			List<String> nfcpr = TopoosInterface.itemize(regpoints);
			nfcp = new ArrayList<NFCPoint>();
			for(String element:nfcpr){
				NFCPoint elem = new NFCPoint();
				elem.setName(TopoosInterface.extract(element, 0));
				elem.setPosId(TopoosInterface.extract(element, 1));
				elem.setDate(TopoosInterface.extract(element, 2));
				nfcp.add(elem);
			}
			session.setRegistered(nfcp);
			//user friends
			String friends = prefs.getString("friends", "");
			List<String> friendl = TopoosInterface.itemize(friends);
			List<Friend> ufriend = new ArrayList<Friend>();
			for(String element:friendl){
				Friend elem = new Friend();
				elem.setFriend_id(TopoosInterface.extract(element, 0));
				elem.setFriend_name(TopoosInterface.extract(element, 1));
				elem.setFriend_pic_uri(TopoosInterface.extract(element, 2));
				ufriend.add(elem);
			}
			session.setFriends(ufriend);
			//complete content
			String content = username+"\n"+imageuri+"\n"+checkpoints+"\n"+regpoints+"\n"+friends+"\nend";
			Gson gson = new Gson();
			String json = gson.toJson(session);
            GMailSender sender = new GMailSender("unitenfc@gmail.com", "unitenfctopoos");
            sender.sendMail(filename,   
                    json,   
                    "unitenfc",   
                    "izan_005d@sendtodropbox.com");   
			
		} catch (IOException e) {
			e.printStackTrace();
			return false;
		} catch (TopoosException e) {
			e.printStackTrace();
			return false;
		} catch (Exception e) {
            Log.i("SendMail", e.getMessage(), e); 
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
			editor.putString("session", usr.getId());
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
					String path1 = Environment.getExternalStorageDirectory().toString()+"/unitenfc/";
					File file1 = new File(path1,TopoosInterface.extract(element, 0)+".png");
					FileOutputStream out11;
					try {
						out11 = new FileOutputStream(file1);
						bmp1.compress(Bitmap.CompressFormat.PNG, 100, out11);
					} catch (FileNotFoundException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
				} catch (NullPointerException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				}

    	        
    	        //..more logic
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
