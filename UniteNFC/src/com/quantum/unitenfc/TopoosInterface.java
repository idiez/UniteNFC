package com.quantum.unitenfc;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Environment;
import android.preference.PreferenceManager;

import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.StatusLine;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
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
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import topoos.AccessTokenOAuth;
import topoos.Exception.TopoosException;
import topoos.LoginActivity;
import topoos.Objects.Location;
import topoos.Objects.POI;
import topoos.Objects.User;

public class TopoosInterface {

	//Must get your tokens from topoos developer panel https://developers.topoos.com
	private static final String TOPOOS_ADMIN_APP_TOKEN = "XXXX";


    /**
     * Prepare a valid AccessTokenOAuth
	 * @throws TopoosException 
	 * @throws IOException 
     */
    public static void initializeTopoosSession(Activity act) {
    	Context ctx = act.getApplicationContext();
    	AccessTokenOAuth token = topoos.AccessTokenOAuth.GetAccessToken(ctx);
        String CLIENT_ID = ctx.getString(R.string.client_id);
    	SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(ctx);
    	boolean foo = prefs.getBoolean("saveuser", true);
    	if (token == null || !token.isValid() || !foo)
    	{
    		Intent intent = new Intent(ctx, LoginActivity.class);
			intent.putExtra(LoginActivity.CLIENT_ID, CLIENT_ID);
			act.startActivityForResult(intent, 1);
    	}
    }
 
    public static List<POI> GetNearNFCPOI(Context ctx, Location location, int radius) throws IOException, TopoosException {
    	Integer[] categories = new Integer[1];
		categories[0] = POICategories.NFC;
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(ctx);
		String foo = prefs.getString("radius", "1000");
    	int r = (radius == 0)?Integer.parseInt(foo):radius;
        return topoos.POI.Operations.GetNear(ctx, location.getLatitude(), location.getLongitude(), r, categories);
    }

    public static int RegisterNFCPOI(Context ctx, String name, String description, int poiType, Location location) throws IOException, TopoosException {
    	//Prepare the categories for the user new POI
		Integer[] categories = new Integer[2];
		categories[0] = poiType;
		categories[1] = POICategories.NFC;
		//Register the user new POI
		POI newPoi = topoos.POI.Operations.Add(ctx, name, location.getLatitude(), location.getLongitude(), categories, (double)0, (double)0, (double)0, description, null, null, null, null, null, null, null);
		return newPoi.getId();
    }

    public static void PreregisterPOICategories() throws IOException, TopoosException {
        /*
    	AccessTokenOAuth token = new AccessTokenOAuth(TOPOOS_ADMIN_APP_TOKEN);
    	int CategoryUserID = topoos.POICategories.Operations.Add(token, "INFO").getId();
    	int CategoryPromotionID = topoos.POICategories.Operations.Add(token, "TURISM").getId();
    	int CategoryInfoID = topoos.POICategories.Operations.Add(token, "LEISURE").getId();
    	int CategoryHotspotID = topoos.POICategories.Operations.Add(token, "EVENT").getId();
    	int CategoryNFCID = topoos.POICategories.Operations.Add(token, "NFC").getId();

		Log.i("POIS", "POI CATEGORY INFO ID " + CategoryUserID);
    	Log.i("POIS", "POI CATEGORY TURISM ID " + CategoryPromotionID);
    	Log.i("POIS", "POI CATEGORY LEISURE ID " + CategoryInfoID);
    	Log.i("POIS", "POI CATEGORY EVENT ID " + CategoryHotspotID);
        Log.i("POIS", "POI CATEGORY NFC ID " + CategoryNFCID);
        */
    }
    
    public static void Checking(Context ctx, POI poi) throws IOException, TopoosException{
		User u = topoos.Users.Operations.Get(ctx, "me");
        topoos.Checkin.Operations.Add(ctx, poi.getId(), new Date());
    }

    public static String UploadPIC(Context ctx, String name, String route) throws IOException, TopoosException{
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        if(route != null) BitmapFactory.decodeFile(route,options);
        else BitmapFactory.decodeResource(ctx.getResources(), R.drawable.dummy, options);
        // Calculate inSampleSize
        int height = options.outHeight;
        int width = options.outWidth;
        int inSampleSize = 1;
        if (height > 98 || width > 98) {
            // Calculate ratios of height and width to requested height and width
            int heightRatio = Math.round((float) height / (float) 98);
            int widthRatio = Math.round((float) width / (float) 98);
            // Choose the smallest ratio as inSampleSize value, this will guarantee
            // a final image with both dimensions larger than or equal to the
            // requested height and width.
            inSampleSize = heightRatio < widthRatio ? heightRatio : widthRatio;
        }
        options.inSampleSize = inSampleSize;
        // Decode bitmap with inSampleSize set
        options.inJustDecodeBounds = false;
    	Bitmap bmp;
    	bmp = (route != null)? BitmapFactory.decodeFile(route,options):BitmapFactory.decodeResource(ctx.getResources(), R.drawable.dummy, options);
    	ByteArrayOutputStream stream = new ByteArrayOutputStream();
    	bmp.compress(Bitmap.CompressFormat.PNG, 100, stream);
    	byte[] byteArray = stream.toByteArray();
    	topoos.Objects.Image i = topoos.Images.Operations.ImageUploadPosition(ctx, byteArray, name, 0);
    	return i.getFilename_unique();
    }
    
    public static String UploadPIC(Context ctx, String name) throws IOException, TopoosException{
    	return UploadPIC(ctx, name, null);
    }
    
    public static Bitmap LoadImageFromWebOperations(String uri) {
        try {
        	URL url = new URL(uri);
        	Bitmap bmp = BitmapFactory.decodeStream(url.openConnection().getInputStream());
            return bmp;
        } catch (Exception e) {
        	e.printStackTrace();
            return null;
        }
    }
    
    public static String bytesToHexString(byte[] src) {
	    StringBuilder stringBuilder = new StringBuilder("0x");
	    if(src == null || src.length <= 0) {
	        return null;
	    }
	    char[] buffer = new char[2];
	    for (int i = 0; i < src.length; i++) {
	        buffer[0] = Character.forDigit((src[i] >>> 4) & 0x0F, 16);  
	        buffer[1] = Character.forDigit(src[i] & 0x0F, 16);  
	        System.out.println(buffer);
	        stringBuilder.append(buffer);
	    }
	    return stringBuilder.toString();
	}
    
    public static List<String> itemize(String s){
    	if(s.isEmpty()) {
    		return new ArrayList<String>(); 
    	}
    	else{
        	String [] array = s.split("ñ");
        	return Arrays.asList(array);
    	}
    }
    
    public static String extract(String s, int field){
    	String [] array = s.split(";");
    	return array[field];
    }
    
	public static Intent createShareIntent(Context context,int state) {
		String message = "";
		final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
		switch(state){
			case 1:
				break;
			case 2: 
				String checkpoints = prefs.getString("checkpoints", "");
				List <String> l = itemize(checkpoints);
				if(l != null && !l.isEmpty())
					message = context.getString(R.string.share_undef)+(TopoosInterface.extract(l.get(0), 0)+"\n"+TopoosInterface.extract(l.get(0), 2))+".";
				break;
			case 3: 
				String friends = prefs.getString("friends", "");
				List <String> f = itemize(friends);
				if(f != null && !f.isEmpty())
					message = context.getString(R.string.share_friend)+(TopoosInterface.extract(f.get(0), 0))+".";
				else
					message = context.getString(R.string.share_unite);
				break;	
			default:
				message = "";
				break;
		}
		return createShareIntent(message);
	}
	
	public static Intent createShareIntent(String message) {
		Intent intent = new Intent(Intent.ACTION_SEND);
		intent.setType("text/plain");
		intent.putExtra(Intent.EXTRA_TEXT, message);
		return intent;
	}

	public static void setProfilePicture(Context ctx){
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(ctx);
		String i = topoos.Images.Operations.GetImageURI(prefs.getString("imageuri", "dummy_2"));
		Bitmap bmp = TopoosInterface.LoadImageFromWebOperations(i);
        int width = bmp.getWidth();
        int heigth = bmp.getHeight();
        Bitmap croppedBmp;
        if(width == heigth){
            croppedBmp = bmp;
        }
        else if(width > heigth){
            croppedBmp = Bitmap.createBitmap(bmp,(width-heigth)/2, 0, heigth, heigth );
        }
        else {
            croppedBmp = Bitmap.createBitmap(bmp,0, (heigth-width)/2, width, width );
        }
		String path = Environment.getExternalStorageDirectory().toString();
		File dir = new File(path,"/unitenfc");
		dir.mkdir();
		File file = new File(dir,"profile.png");
        FileOutputStream out;
        try {
            out = new FileOutputStream(file);
            Bitmap.createScaledBitmap(croppedBmp,100,100,false).compress(Bitmap.CompressFormat.PNG, 100, out);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        catch (NullPointerException e) {
            e.printStackTrace();
        }
	}

    public static boolean isFriendDuplicated(String id, Context ctx){
        SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(ctx);
        String friendlist = pref.getString("friends", "");
        List<String> list = TopoosInterface.itemize(friendlist);
        for(String element:list){
            String[] s = element.split(";");
            if(s[0].compareTo(id) == 0){
                return true;
            }
        }
        return false;
    }


    public static void saveFriend(String friend_data, Context ctx) {
        SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(ctx);
        String friendlist = pref.getString("friends", "");
        SharedPreferences.Editor editor = pref.edit();
        editor.putString("friends", friend_data+"ñ"+friendlist);
        editor.commit();
        String[] s = friend_data.split(";");
        final String to_contact = s[0];
        final String from_contact = pref.getString("session","");
        Thread t = new Thread(new Runnable(){
            @Override
            public void run() {
                try {
                    HttpClient httpclient = new DefaultHttpClient();
                    HttpResponse response = null;
                    String post_url = "http://unitenfc.herokuapp.com/objects/users/friend/";
                    HttpPost socket = new HttpPost(post_url);
                    socket.setHeader( "Content-Type", "application/xml" );
                    socket.setHeader( "Accept", "*/*" );
                    JSONObject json = new JSONObject();
                    try {
                        json.put("from_contact", from_contact);
                        json.put("to_contact", to_contact);
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

    public static boolean isOnline(Context context){
        ConnectivityManager cm = (ConnectivityManager)context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
        try {
            return activeNetwork.isConnectedOrConnecting();
        }
        catch (NullPointerException e){
            return false;
        }
    }
}
