package es.quantum.unitenfc;


import android.app.ActionBar;
import android.app.ActionBar.Tab;
import android.app.Activity;
import android.app.Fragment;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.nfc.NdefMessage;
import android.nfc.NdefRecord;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.os.Bundle;
import android.os.Handler;
import android.os.Parcelable;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ShareActionProvider;
import android.widget.Toast;

import com.google.android.gms.maps.model.LatLng;

import java.io.IOException;
import java.util.List;

import es.quantum.unitenfc.RegisterPOIFragment.OnReg;
import es.quantum.unitenfc.backup.CustomBackup;
import topoos.AccessTokenOAuth;
import topoos.Exception.TopoosException;
import topoos.LoginActivity;
import topoos.Objects.POI;
import topoos.Objects.User;
import topoos.Objects.UserIdPosition;

public class MainActivity extends Activity implements OnReg{

	ProgressDialog progressDialog;
	ShareActionProvider mShareActionProvider;
	CustomMapFragment map;
	ScanFragment scan;
	CustomTabListener maptablistener;
	CustomTabListener scantablistener;
	boolean first;
	String userid;
	List <UserIdPosition> friends;
	
	private Location current_pos;
	private LocationManager mLocationManager;
	private RegisterPosition mCustomLocationListener;
	
	private List<POI> poi_list;
	
	private Handler mHandler = new Handler();
	Runnable mUpdateMap = new Runnable() {
        public void run() {
           Log.i("UPDATE", "working");
           map.setPOIList(poi_list);
           if(maptablistener.isActive()){
        	   try{
        		   map.POIMarkers();
        		   map.UpdateMarker();
        	   }
        	   catch(NullPointerException e){
        		   e.printStackTrace();
        	   }
           }
        }
	};
	Runnable mCentreMap = new Runnable() {
        public void run() {
           if(maptablistener.isActive()){
        	   	map.centerMapAndRefresh(true);
           }
        }
	};
	
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		

		
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		/*
	     int code = GooglePlayServicesUtil.isGooglePlayServicesAvailable(getApplicationContext());
	     if(code == ConnectionResult.SUCCESS){	      
	    	  // Start timer and launch main activity

	      }
	      else{
	    	  GooglePlayServicesUtil.getErrorDialog(code, this, 1);
	      }
		
		*/
		TopoosInterface.initializeTopoosSession(this);	//initiate topoos session
		
		
	
		/*	REGISTER CATEEGORIES FOR THE FIRST TIME
		RegisterCategoryWorker worker = new RegisterCategoryWorker();
		Thread thread = new Thread(worker);
		thread.start();*/		
		

		
		map =  new CustomMapFragment();	//create map
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
	    float lat = prefs.getFloat("lastlat", 0);
	    float lon = prefs.getFloat("lastlong", 0);
		map.setPos(new LatLng(lat,lon));
	    
	    
	    map.setPOIVis();

	    first = true;
	    //set listeners
	    mCustomLocationListener = new RegisterPosition();
        mLocationManager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);
        mLocationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 1000, 0, mCustomLocationListener);
        mLocationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1000, 0, mCustomLocationListener);
        
        //create action bar
        ActionBar actionBar = getActionBar();
	    actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);
	    
	    SocialFragment social = new SocialFragment();
	    scan = new ScanFragment();
	    
	    maptablistener = new CustomTabListener(map);
	    scantablistener = new CustomTabListener(scan);
	    Tab map_tab = actionBar.newTab().setText("MAP").setTabListener(maptablistener);
	    Tab scan_tab = actionBar.newTab().setText("NFC POINTS").setTabListener(scantablistener);
	    Tab social_tab = actionBar.newTab().setText("SOCIAL").setTabListener(new CustomTabListener(social));
	    actionBar.addTab(map_tab);
	    actionBar.addTab(scan_tab);
	    actionBar.addTab(social_tab);

	    
	    /* Thread d = new Thread(new FetchPositionWorker());
	    d.start();*/
		
        
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		if(maptablistener.isActive())getMenuInflater().inflate(R.menu.menu1, menu);
		else getMenuInflater().inflate(R.menu.menu2, menu);
		
		MenuItem menuItem = menu.findItem(R.id.menu_share);
		if(menuItem != null) mShareActionProvider = (ShareActionProvider)menuItem.getActionProvider();

		int state = getState();
		if(state == 1){
			mShareActionProvider.setShareIntent(TopoosInterface.createShareIntent("Exploring NFC Points with UniteNFC."));
		}
		else{
			mShareActionProvider.setShareIntent(TopoosInterface.createShareIntent((Context)this, state));
		}		
        
		return true;
	}
	
	@Override
	public void onAttachFragment(Fragment fragment){

		if(mShareActionProvider != null)
        mShareActionProvider.setShareIntent(TopoosInterface.createShareIntent((Context)this, getState()));
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
	    int itemId = item.getItemId();
		if (itemId == android.R.id.home) {
			// app icon in action bar clicked; go home
			return true;
		} else if (itemId == R.id.center) {
			if(maptablistener.isActive()){
				map.centerMapAndRefresh(true);
				map.UpdateMarker();
			}
			return true;
		} else if (itemId == R.id.map_type) {
			if(maptablistener.isActive())map.switchMapType();
			return true;
		} else if (itemId == R.id.POI) {
			map.POIVis();
			return true;
		} else if (itemId == R.id.settings) {
			startActivityForResult(new Intent(getApplicationContext(),Settings.class),2);
			return true;
		} else if (itemId == R.id.about) {
			//ABOUT ACTIVITY
			startActivity(new Intent(getApplicationContext(),About.class));
			return true;
		} else if (itemId == R.id.exit) {
			SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
			Editor editor = prefs.edit();
			editor.putBoolean("saveuser", false);
			editor.commit();
			this.onBackPressed();
			return true;
		} else {
			return super.onOptionsItemSelected(item);
		}
	}

	protected void onDestroy() {
		super.onDestroy();
    	if (mLocationManager != null && mCustomLocationListener != null) {
    		mLocationManager.removeUpdates(mCustomLocationListener);
    		mLocationManager = null;
    	}
		Thread b = new Thread(new Runnable(){

			@Override
			public void run() {
				CustomBackup c = new CustomBackup();
				c.requestbackup(getApplicationContext());
			}
    		
    	});
    	b.start();
		
	}
	
	public void showToast(final String toast) {
	    runOnUiThread(new Runnable() {
	        public void run()
	        {
	            Toast.makeText(MainActivity.this, toast, Toast.LENGTH_SHORT).show();
	        }
	    });
	}
	
	public void onNewIntent(Intent intent) {
		//showToast("TEST");
		Parcelable[] rawMsgs = intent.getParcelableArrayExtra(NfcAdapter.EXTRA_NDEF_MESSAGES);
		NdefMessage[] messages = null; 
        if (rawMsgs != null) {  
             messages = new NdefMessage[rawMsgs.length];  
             for (int i1 = 0; i1 < rawMsgs.length; i1++) {  
                  messages[i1] = (NdefMessage) rawMsgs[i1];  
                  Log.i("BYTES", ""+messages[i1].toByteArray());
             }  
        }  
        if(messages[0] != null) {  
     		NdefRecord[] rec = (messages[0].getRecords());
     		byte[] ans = rec[0].getPayload();
     		String str = new String(ans);
			Log.i("PAYLOAD", str);
        }
        //Log.i("INTENT", intent.getType());
        if(intent.getType() != null && intent.getType().equals("application/es.quantum.unitenfc")) {
	        /*Parcelable[] rawMsgs = intent.getParcelableArrayExtra(NfcAdapter.EXTRA_NDEF_MESSAGES);  
	        NdefMessage[] messages = null; 
	        if (rawMsgs != null) {  
	             messages = new NdefMessage[rawMsgs.length];  
	             for (int i1 = 0; i1 < rawMsgs.length; i1++) {  
	                  messages[i1] = (NdefMessage) rawMsgs[i1];  
	                  Log.i("BYTES", ""+messages[i1].toByteArray());
	             }  
	        } 
	        if(messages[0] != null) {  
	             		NdefRecord[] rec = (messages[0].getRecords());
	             		byte[] ans = rec[0].getPayload();
	             		String str;
						try {
							str = new String(ans, "UTF-8");
							Log.i("BTRCV", str);
							String friend = str.substring(0, 35);
							SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences((Context)this);
							String friendlist = pref.getString("friends", "");
							List<String> list = TopoosInterface.itemize(friendlist);
							boolean duplicated = false;
							for(String element:list){
								if(element.compareTo(friend) == 0){
									showToast("You are already friends!");
									duplicated = true;
									break;
								}
				        	}
							if(!duplicated){		
								User usr = topoos.Users.Operations.Get(getApplicationContext(), friend);
								Editor editor = pref.edit();
								String title = usr.getName()+";"+friend+"�";
					    		editor.putString("checkpoints", title.concat(friendlist));
					    		editor.commit();
								String MAC = str.substring(36);
								//INICIAR BLUETOOTH
								BluetoothConn btc = new BluetoothConn(false);
								btc.setMAC(MAC);
								btc.setMes(pref.getString("session", ""));
								btc.configureBluetooth();
							}

						} catch (UnsupportedEncodingException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						} catch (IOException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						} catch (TopoosException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
	             		
	        }*/
        }
        else {
		    Tag tagFromIntent = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);
		    String tagid = TopoosInterface.bytesToHexString(tagFromIntent.getId());
		    boolean isRegistered = false;
		    for(POI poi:poi_list){
		    	
		    	if(poi.getName().substring(0, 16).compareTo(tagid)==0){
		    		isRegistered = true;
		    		break;
		    	}
		    		    	
		    }
		    if(!isRegistered){	//!isRegistered
		        RegisterPOIFragment newRegisterFragment = new RegisterPOIFragment();
			    topoos.Objects.Location loc = new topoos.Objects.Location(current_pos.getLatitude(),current_pos.getLongitude());
			    newRegisterFragment.setArguments(tagid,loc);
			    //DialogFragment dialog = newRegisterFragment;
			    newRegisterFragment.show(getFragmentManager(), "register");
			    //do something with tagFromIntent
		    }
		    else{
		    	showToast("NFC Point already registered.");
		    }
        }
	}
	
	
	
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {

		switch(requestCode) {
			case 1:
				switch (resultCode) {
					case LoginActivity.RESULT_OK:
						AccessTokenOAuth token = AccessTokenOAuth.GetAccessToken(getApplicationContext());
						Log.i("TOKEN",token.getAccessToken());
						Log.i("TOKEN",token.isValid()?"Valid":"Not valid");
						if (token.isValid()) {

							//showToast("Valid!");
							progressDialog = ProgressDialog.show((Context)this, "",
						            "Loading user data...",false,false);
							Thread b = new Thread(new Runnable(){

								@Override
								public void run() {
									/*ProgressDialog progressDialog = ProgressDialog.show(ctx, "",
								            "Loading user data...",false,false);*/
									CustomBackup c = new CustomBackup();
									c.requestrestore(getApplicationContext());
									TopoosInterface.setProfilePicture(getApplicationContext());
									progressDialog.dismiss();
								}
				        		
				        	});
				        	b.start();
							Thread t = new Thread(new FetchUser());
							t.start();
							
							mHandler.removeCallbacks(mUpdateMap);
						    mHandler.postDelayed(mCentreMap, 0);
				        	SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
			    			Editor editor = prefs.edit();
			    			editor.putBoolean("saveuser", true);
			    			editor.commit();
						}
						
						break;
					case LoginActivity.RESULT_CANCELED:
						this.onBackPressed();
						break;
					case LoginActivity.RESULT_TOPOOSERROR:
						if (!AccessTokenOAuth.GetAccessToken(getApplicationContext())
								.isValid()) {
							showToast("Not valid!");
						}
						break;
					case LoginActivity.RESULT_FIRST_USER:
						final Context ctx = getApplicationContext();
						Thread b = new Thread(new Runnable(){
							@Override
							public void run() {
								CustomBackup c = new CustomBackup();
								c.requestbackup(ctx);
								c.requestrestore(ctx);
								TopoosInterface.setProfilePicture(getApplicationContext());
							}
			        	});
			        	b.start();
						
						
						//Create new group for user friends
						//topoos.Users.Operations.GroupSet(MainActivity.this, userid, );
						
						
						Thread r = new Thread(new Runnable(){

							@Override
							public void run() {
						    	Integer[] categories = new Integer[1];
								categories[0] = POICategories.USER_DATA;
								try {
									User usr = topoos.Users.Operations.Get(getApplicationContext(), "me");
									POI newPoi = topoos.POI.Operations.Add(getApplicationContext(),usr.getId(),-58.077876, 41.484375, categories, (double)0, (double)0, (double)0, "", null, null, null, null, null, null, null);
								} catch (IOException e) {
									// TODO Auto-generated catch block
									e.printStackTrace();
								} catch (TopoosException e) {
									// TODO Auto-generated catch block
									e.printStackTrace();
								}
							}
							
						});
						r.start();
						
						
						break;
					default:
						break;
					}
				break;
			case 2:
					FetchPOIWorker wrk = new FetchPOIWorker();
					Thread thread1 = new Thread(wrk);
					thread1.start();
				
				break;
			case 3:
				Log.i("onAc", "llego");
				break;
			default:
				Log.i("onAc", "nollego");
				break;
		}
	}
	
	

	
	private class RegisterPosition implements LocationListener{
		
		@Override
		public void onLocationChanged(Location arg0) {
			//showToast("NEW!");
			map.setPos(new LatLng(arg0.getLatitude(),arg0.getLongitude()));
			//discriminar medidas aqu�!
			if(maptablistener.isActive()){
				
				SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
		    	boolean foo = prefs.getBoolean("track", false);
				if(first) {
					first = false;
					map.centerMapAndRefresh(false);
				}
			     map.UpdateMarker();
			     if(foo) map.centerMapAndRefresh(true);
			     current_pos = arg0;
			     

			     RegisterPositionWorker worker = new RegisterPositionWorker(arg0);
			     Thread thread = new Thread(worker);
			     thread.start();
		
			     FetchPOIWorker wrk = new FetchPOIWorker();
			     Thread thread1 = new Thread(wrk);
			     thread1.start();
			     
			     SharedPreferences.Editor editor = prefs.edit();
			     editor.putFloat("lastlat", (float)arg0.getLatitude());
			     editor.putFloat("lastlong", (float)arg0.getLongitude());
			     editor.commit();

			}
		}

		@Override
		public void onProviderDisabled(String arg0) {
			// TODO Auto-generated method stub
			
		}

		@Override
		public void onProviderEnabled(String arg0) {
			// TODO Auto-generated method stub
			
		}

		@Override
		public void onStatusChanged(String arg0, int arg1, Bundle arg2) {
			// TODO Auto-generated method stub
			
		}

   }

	private class FetchUser implements Runnable {

		@Override
		public void run() {
			// TODO Auto-generated method stub
			try {
				User u = topoos.Users.Operations.Get(MainActivity.this, "me");
				userid = u.getId();
				//friends = topoos.Users.Operations.NearPositionGet(getApplicationContext(), current_pos.getLatitude(), current_pos.getLongitude(), 8000000, groupID, 1000, null);
				if(u!=null)Log.i("ID", u.getId());
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (TopoosException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	
	}
	
	private class RegisterPositionWorker implements Runnable {
		
    	public Location position;
    	
    	public RegisterPositionWorker(Location loc)	{
    		position = loc;
    	}
    	
		public void run(){
			try {
					topoos.Positions.Operations.Add(getApplicationContext(), position.getLatitude(), position.getLongitude(), null, null, null, null, null, null, null);
					Log.i("POS", "WORKING!");
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}
	
	private class RegisterCategoryWorker implements Runnable {
		
    public void run(){
			try {
				TopoosInterface.PreregisterPOICategories();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}
	
	
	private class FetchPOIWorker implements Runnable{
		@Override
		public void run() {
			// TODO Auto-generated method stub
					try {
						poi_list = TopoosInterface.GetNearNFCPOI(getApplicationContext(), new topoos.Objects.Location(current_pos.getLatitude(),current_pos.getLongitude()),0);
						mHandler.removeCallbacks(mUpdateMap);
					    mHandler.postDelayed(mUpdateMap, 0);
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					} catch (TopoosException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					} catch (NullPointerException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
			}
	}
	
	
	
	
	private class FetchPositionWorker implements Runnable{
		@Override
		public void run() {
			// TODO Auto-generated method stub
					try {
						//topoos.AccessTokenOAuth token1 = new topoos.AccessTokenOAuth("771697e2-c59d-468e-b937-ac9d3632d67b");
						topoos.Positions.Operations.GetLastUser(getApplicationContext(), topoos.Users.Operations.Get(MainActivity.this, "me").getId());
						showToast("NEW!");
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					} catch (TopoosException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
		}
	}

	public int getState(){
		if(maptablistener.isActive()) return 1;
		else if(scantablistener.isActive()) return 2;
		else return 3;
	}


	@Override
	public void onReg() {
		scan.refreshLists();
	}


	
}
