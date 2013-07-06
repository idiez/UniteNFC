package es.quantum.unitenfc;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.nfc.NdefMessage;
import android.nfc.NdefRecord;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.os.Bundle;
import android.os.Parcelable;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ListView;
import android.widget.ShareActionProvider;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import topoos.Exception.TopoosException;
import topoos.Objects.POI;
import topoos.Objects.Position;
import topoos.Objects.User;

public class Serve extends Activity {

	ShareActionProvider mShareActionProvider;
	String message;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		//setContentView(R.layout.splash);
		setContentView(R.layout.nfc_dispatch);
		ListView v = (ListView)findViewById(R.id.tags);
		
        // see if app was started from a tag
        Intent i = getIntent();
        Log.i("INTENT", i.getType());
        if(i.getType() != null && i.getType().equals("text/plain")) {	//add any intent you are filtering for
	        NdefMessage[] messages = null;  
	        Parcelable[] rawMsgs = i.getParcelableArrayExtra(NfcAdapter.EXTRA_NDEF_MESSAGES);  
	        if (rawMsgs != null) {  
	             messages = new NdefMessage[rawMsgs.length];  
	             for (int i1 = 0; i1 < rawMsgs.length; i1++) {  
	            	 
	                  messages[i1] = (NdefMessage) rawMsgs[i1];  
	                  Log.i("BYTES", ""+messages[i1].toByteArray());
	             }  
	        }  
	        if(messages[0] != null) {  
	        	
	        	List<String> list = new ArrayList<String>();
	        	for(int a= 0; a<messages.length;a++){
	        		NdefRecord[] rec = messages[a].getRecords();
	        		message = parseNFCRecords(rec[0]);
	        		for(int c= 0; a<messages.length;a++){
	        			list.add(parseNFCRecords(rec[c]));
	        		}
	        		
	        }
	        	
	        	
	        	List<RowItem> rowItems = new ArrayList<RowItem>();
	            for (String s11:list) {
	                RowItem item = new RowItem(null, s11);
	                rowItems.add(item);
	            }
	     
	            CustomListViewAdapter adapter = new CustomListViewAdapter(this.getApplicationContext(),
	                    R.layout.list, rowItems);
	            v.setAdapter(adapter);
	        	
	        }
	        	/*
	             String result="";  
	             Toast.makeText(getApplicationContext(), ""+messages[0].getRecords()[0].getTnf(), Toast.LENGTH_SHORT).show();  
	             byte[] payload = messages[0].getRecords()[0].getPayload();  
	             // this assumes that we get back am SOH followed by host/code  
	             for (int b = 1; b<payload.length; b++) { // skip SOH  
	                  result += (char) payload[b];  
	             }  
	             Toast.makeText(getApplicationContext(), "Tag Contains " + result, Toast.LENGTH_SHORT).show();  */
                
        
        
	        Tag tagFromIntent = i.getParcelableExtra(NfcAdapter.EXTRA_TAG);
	        final String tagid = TopoosInterface.bytesToHexString(tagFromIntent.getId());
        
        /*
        //Log.i("TEST",intent.getAction());
        Intent intent = new Intent("android.nfc.action.TECH_DISCOVERED");
        PackageManager manager = getApplicationContext().getPackageManager();
        List<ResolveInfo> r = manager.queryIntentActivities(intent, 0);
        for(ResolveInfo e:r){
        	Log.i("TEST",e.activityInfo.packageName);
        }
        
        
        ComponentName componentName = new ComponentName(r.get(0).activityInfo.packageName, r.get(0).activityInfo.name); 
        //Intent i = new Intent("android.intent.action.MAIN");
        //i.addCategory("android.intent.category.LAUNCHER").setFlags(Intent.FLAG_ACTIVITY_NEW_TASK).setComponent(componentName);
        i.setAction(Intent.ACTION_MAIN);
        i.addCategory(Intent.CATEGORY_LAUNCHER);
        i.setComponent(componentName).setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

        
        //i = manager.getLaunchIntentForPackage("app package name");
        //i.addCategory(Intent.CATEGORY_LAUNCHER);
        startActivity(i);
        //startActivityForResult(i,7);
        */
        
	        Thread t = new Thread(new Runnable(){
				@Override
				public void run() {				
					try{
						User me = topoos.Users.Operations.Get(getApplicationContext(), "me");
						Position current_pos = topoos.Positions.Operations.GetLastUser(getApplicationContext(), me.getId());
						List<POI> poi_list = TopoosInterface.GetNearNFCPOI(getApplicationContext(), new topoos.Objects.Location(current_pos.getLatitude(),current_pos.getLongitude()),10);				
					    for(POI poi:poi_list){			    	
					    	if(poi.getName().substring(0, 16).compareTo(tagid)==0){
					    		TopoosInterface.Checking(getApplicationContext(), poi);
					    		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
					    		String s = prefs.getString("checkpoints", "");
					    		Editor editor = prefs.edit();
					    		String name = poi.getName().substring(16);
					    		Date d = new Date();
					    		@SuppressWarnings("deprecation")
								String title = name+";"+poi.getCategories().get(0).getId()+";"+d.toLocaleString().substring(0, 16)+"ñ";
					    		editor.putString("checkpoints", title.concat(s));
					    		editor.commit();
					    		break;
					    	}
					    }
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					} catch (TopoosException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}        	
	        });
	        t.start();
        }
        else{this.onBackPressed();}
      
        
     
	}
	
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.menu2, menu);		
		MenuItem menuItem = menu.findItem(R.id.menu_share);
		
		
		if(menuItem != null);
		menuItem = menu.findItem(R.id.menu_share);
		if(menuItem != null)
		mShareActionProvider = (ShareActionProvider)menuItem.getActionProvider();
        mShareActionProvider.setShareIntent(TopoosInterface.createShareIntent("Just scanned tag "+message+" with UniteNFC."));
		return true;

	}
	
	
	
	public String parseNFCRecords(NdefRecord ndefr){
		short tnf = ndefr.getTnf();
		byte[] type = ndefr.getType();
		String result = "";
        for (int b = 1; b<ndefr.toByteArray().length; b++) { // skip SOH  
            result += (char) ndefr.toByteArray()[b];  
       }  

		return result.substring(6);
		
		
		
	}
	
	
	
	}