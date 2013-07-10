package es.quantum.unitenfc;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.util.Log;

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
	private static final String TOPOOS_ADMIN_APP_TOKEN = "44ef11c4-c7bc-47a1-8b9b-fad6bcc71024";
	private static final String TOPOOS_USER_APP_TOKEN = "771697e2-c59d-468e-b937-ac9d3632d67b";
	public static final String CLIENT_ID = "2886137d-5535-444b-82c9-9826d8025deb";
	//private static final int REQUESTCODE_LOGIN = 1;
	
	public static final int SEARCH_RADIUS_METERS = 1000;

	/**
     * Prepare a valid AccessTokenOAuth
	 * @throws TopoosException 
	 * @throws IOException 
     */
    public static void initializeTopoosSession(Activity act){
    	
    	Context ctx = act.getApplicationContext();
    	AccessTokenOAuth token = topoos.AccessTokenOAuth.GetAccessToken(ctx);
    	SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(ctx);
    	boolean foo = prefs.getBoolean("saveuser", true);
    	
    	if (token == null || !token.isValid() || !foo)
    	{
    		Intent intent = new Intent(ctx, OwnLogin.class);
			intent.putExtra(LoginActivity.CLIENT_ID, CLIENT_ID);
			act.startActivityForResult(intent, 1);
			//	token = new AccessTokenOAuth(TOPOOS_USER_APP_TOKEN);
			//	token.save_Token(ctx); //save on preferences
    	}
    	

    }
    
  /* public static void validToken(){
    	return (token == null || !token.isValid());
    }
 */   
 
    public static List<POI> GetNearNFCPOI(Context ctx, Location location, int radius) throws IOException, TopoosException
    {
    	Integer[] categories = new Integer[1];
		categories[0] = POICategories.NFC;
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(ctx);
		String foo = prefs.getString("radius", "1000");
    	int r = (radius == 0)?Integer.parseInt(foo):radius;
    	List<POI> pois = topoos.POI.Operations.GetNear(ctx, location.getLatitude(), location.getLongitude(), r, categories);
    	return pois;
    }
    

    public static int RegisterNFCPOI(Context ctx, String name, String description, int poiType, Location location) throws IOException, TopoosException
    {
    	//Prepare the categories for the user new POI
		Integer[] categories = new Integer[2];
		categories[0] = poiType;
		categories[1] = POICategories.NFC;

		//Register the user new POI
		POI newPoi = topoos.POI.Operations.Add(ctx, name, location.getLatitude(), location.getLongitude(), categories, (double)0, (double)0, (double)0, description, null, null, null, null, null, null, null);
		return newPoi.getId();
    }
    
    
    
    public static void PreregisterPOICategories() throws IOException, TopoosException
    {
    	AccessTokenOAuth token = new AccessTokenOAuth(TOPOOS_ADMIN_APP_TOKEN);
    	/*
    	int CategoryUserID = topoos.POICategories.Operations.Add(token, "USER").getId();
    	int CategoryPromotionID = topoos.POICategories.Operations.Add(token, "PROMOTION").getId();
    	int CategoryInfoID = topoos.POICategories.Operations.Add(token, "INFO").getId();
    	int CategoryHotspotID = topoos.POICategories.Operations.Add(token, "HOTSPOT").getId();
    	int CategoryNFCID = topoos.POICategories.Operations.Add(token, "NFC").getId();
    	*/
    	
    	int CategoryNFCID = topoos.POICategories.Operations.Add(token, "USER_DATA").getId();
    	 Log.i("POIS", "POI CATEGORY NFC ID " + CategoryNFCID);
    	//contador mediante incidencias
    	//POI poi = topoos.POI.Operations.Add(ctx, "GlobalCounter", (double)0, (double)0, categories, (double)0, (double)0, (double)0, "GlobalCounter", null, null, null, null, null, null, null);
    	
    	/*
		Log.i("POIS", "POI CATEGORY USER ID " + CategoryUserID);
    	Log.i("POIS", "POI CATEGORY PROMOTION ID " + CategoryPromotionID);
    	Log.i("POIS", "POI CATEGORY INFO ID " + CategoryInfoID);
    	Log.i("POIS", "POI CATEGORY HOTSPOT ID " + CategoryHotspotID);
        Log.i("POIS", "POI CATEGORY NFC ID " + CategoryNFCID);
        */
    }
    
    public static void Checking(Context ctx, POI poi) throws IOException, TopoosException{
		User u = topoos.Users.Operations.Get(ctx, "me");
		String userid = u.getId();
		
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

        if (height > 98 || width > 98) {	//power of 2

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
					message = "Last NFC Point checked: "+(TopoosInterface.extract(l.get(0), 0)+"\n"+TopoosInterface.extract(l.get(0), 2))+".";
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
		//String path = Environment.getExternalStorageDirectory().toString()+"/unitenfc";
		String path = Environment.getExternalStorageDirectory().toString();
		File dir = new File(path,"/unitenfc");
		dir.mkdir();
		File file = new File(dir,"profile.png");
       FileOutputStream out;
	try {
		out = new FileOutputStream(file);
	       bmp.compress(Bitmap.CompressFormat.PNG, 100, out);
	} catch (FileNotFoundException e) {
		e.printStackTrace();
	}
	catch (NullPointerException e) {
		e.printStackTrace();
	}

	}
	
	
}
