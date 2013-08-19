package es.quantum.unitenfc;

import android.app.ActionBar;
import android.app.ActionBar.Tab;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Fragment;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
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

import com.facebook.Request;
import com.facebook.Response;
import com.facebook.Session;
import com.facebook.SessionState;
import com.facebook.UiLifecycleHelper;
import com.facebook.model.GraphUser;
import com.google.android.gms.maps.model.LatLng;

import java.io.IOException;
import java.util.List;

import es.quantum.unitenfc.background.Constants;
import es.quantum.unitenfc.background.ProximityNotifier;
import es.quantum.unitenfc.backup.CustomBackup;
import topoos.AccessTokenOAuth;
import topoos.Exception.TopoosException;
import topoos.LoginActivity;
import topoos.Objects.POI;
import topoos.Objects.User;

public class MainActivity extends Activity implements OnReg{

	private ProgressDialog progressDialog;
    private ShareActionProvider mShareActionProvider;
    private CustomMapFragment map;
    private ScanFragment scan;
    private CustomTabListener maptablistener;
    private CustomTabListener scantablistener;
    private boolean first;
    private Location current_pos;
	private LocationManager mLocationManager;
	private RegisterPosition mCustomLocationListener;
	private List<POI> poi_list;
	private Handler mHandler = new Handler();
	Runnable mUpdateMap = new Runnable() {

        public void run() {
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
    Runnable mLaunchPoll = new Runnable() {

        public void run() {
            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
            if(prefs.getBoolean("poll",false)){
                Editor editor = prefs.edit();
                editor.putBoolean("poll", false);				//saves last user session
                editor.commit();
                PollDialog newRegisterFragment = new PollDialog();
                newRegisterFragment.show(getFragmentManager(), "register");

            }

        }
    };
    private FacebookDialog fb_dialog;
    private UiLifecycleHelper uiHelper;
    private Session.StatusCallback callback = new Session.StatusCallback() {
        @Override
        public void call(Session session, SessionState state, Exception exception) {
            onSessionStateChange(session, state, exception);
        }
    };
    private void onSessionStateChange(Session session, SessionState state, Exception exception) {
        if(fb_dialog.isAdded())fb_dialog.dismiss();
        if (state.isOpened()) {
            final Session s = session;
            Request.executeMeRequestAsync(session, new Request.GraphUserCallback() {
                // callback after Graph API response with user object
                @Override
                public void onCompleted(GraphUser user, Response response) {
                if (user != null) {
                    final GraphUser usr = user;
                    showToast(getString(R.string.fb_salutation)+" "+ user.getName() + "!");
                    Request.executeMyFriendsRequestAsync(s, new Request.GraphUserListCallback() {
                        @Override
                        public void onCompleted(List<GraphUser> users, Response response) {
                            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
                            FacebookLogic.linkUser(usr.getId(), prefs.getString("session",""), users, MainActivity.this);
                        }
                    });
                }
                }
            });
            Log.i("TAG", "Logged in...");
        } else if (state.isClosed()) {
            Log.i("TAG", "Logged out...");
        }
    }

    @Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
        if(!TopoosInterface.isOnline(getApplicationContext())) {
            new AlertDialog.Builder(this).setTitle(getString(R.string.internet_no_connection)).setMessage(getString(R.string.internet_required)).setCancelable(false).setNeutralButton("Ok", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {
                    onBackPressed();
                }
            }).show();
        }
		TopoosInterface.initializeTopoosSession(this);	//initiate topoos session
        fb_dialog = new FacebookDialog();
        uiHelper = new UiLifecycleHelper(this, callback);
        uiHelper.onCreate(savedInstanceState);
		/*	REGISTER CATEEGORIES FOR THE FIRST TIME
		RegisterCategoryWorker worker = new RegisterCategoryWorker();
		Thread thread = new Thread(worker);
		thread.start();*/
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        boolean showfbdialog = prefs.getBoolean("showfbdialog", true);
        Session session = Session.getActiveSession();
        if(!(session.getState() == SessionState.CREATED_TOKEN_LOADED || session.isOpened())&& showfbdialog) {
            fb_dialog.setCancelable(false);
            fb_dialog.show(getFragmentManager(), "fb log");
        }
		map =  new CustomMapFragment();	//create map
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
        current_pos = mLocationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
        //create action bar
        ActionBar actionBar = getActionBar();
	    actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);
	    SocialFragment social = new SocialFragment();
	    scan = new ScanFragment();
	    maptablistener = new CustomTabListener(map);
	    scantablistener = new CustomTabListener(scan);
	    Tab map_tab = actionBar.newTab().setText(getString(R.string.map_tab)).setTabListener(maptablistener);
	    Tab scan_tab = actionBar.newTab().setText(getString(R.string.nfc_points_tab)).setTabListener(scantablistener);
	    Tab social_tab = actionBar.newTab().setText(getString(R.string.social_tab)).setTabListener(new CustomTabListener(social));
	    actionBar.addTab(map_tab);
	    actionBar.addTab(scan_tab);
	    actionBar.addTab(social_tab);

        IntentFilter mStatusIntentFilter = new IntentFilter(
                Constants.BROADCAST_ACTION);
        mStatusIntentFilter.addDataScheme("http");
        //mDownloadStateReceiver = new ResponseReceiver();
        // Registers the DownloadStateReceiver and its intent filters
        //LocalBroadcastManager.getInstance(this).registerReceiver(mDownloadStateReceiver,mStatusIntentFilter);
        // Adds a data filter for the HTTP scheme


        //this.startService(new Intent(this, ProximityNotifier.class));


        mHandler.removeCallbacks(mLaunchPoll);
        mHandler.postDelayed(mLaunchPoll, 0);

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
			mShareActionProvider.setShareIntent(TopoosInterface.createShareIntent(getString(R.string.share_explore)));
		}
		else{
			mShareActionProvider.setShareIntent(TopoosInterface.createShareIntent(this, state));
		}
		return true;
	}

	@Override
	public void onAttachFragment(Fragment fragment){
		if(mShareActionProvider != null) {
            mShareActionProvider.setShareIntent(TopoosInterface.createShareIntent((Context)this, getState()));
        }
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
			startActivity(new Intent(getApplicationContext(),About.class));
			return true;
        } else if (itemId == R.id.report) {
            new ReportBug().show(this.getFragmentManager(),"");
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

    @Override
    public void onResume() {
        super.onResume();
        uiHelper.onResume();
    }

    @Override
    public void onPause() {
        super.onPause();
        uiHelper.onPause();
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        uiHelper.onSaveInstanceState(outState);
    }

    @Override
    public void onStop(){
        super.onStop();
    }

	protected void onDestroy() {
		super.onDestroy();
        uiHelper.onDestroy();
    	if (mLocationManager != null && mCustomLocationListener != null) {
    		mLocationManager.removeUpdates(mCustomLocationListener);
    		mLocationManager = null;
    	}
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
		Parcelable[] rawMsgs = intent.getParcelableArrayExtra(NfcAdapter.EXTRA_NDEF_MESSAGES);
		NdefMessage[] messages = null; 
        if (rawMsgs != null) {  
             messages = new NdefMessage[rawMsgs.length];  
             for (int i1 = 0; i1 < rawMsgs.length; i1++) {  
                  messages[i1] = (NdefMessage) rawMsgs[i1];
             }  
        }  
        if(messages[0] != null) {  
     		NdefRecord[] rec = (messages[0].getRecords());
     		byte[] ans = rec[0].getPayload();
     		String str = new String(ans);
        }
        if(intent.getType() != null && intent.getType().equals("application/es.quantum.unitenfc")) {
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
			    newRegisterFragment.show(getFragmentManager(), "register");
		    }
		    else{
		    	showToast(getString(R.string.nfc_duplicated));
		    }
        }
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        uiHelper.onActivityResult(requestCode, resultCode, data);
		switch(requestCode) {
			case 1:
				switch (resultCode) {
					case LoginActivity.RESULT_OK:
						AccessTokenOAuth token = AccessTokenOAuth.GetAccessToken(getApplicationContext());
						if (token.isValid()) {
							progressDialog = ProgressDialog.show((Context)this, "",
						            getString(R.string.welcome),false,false);
							Thread b = new Thread(new Runnable(){

								@Override
								public void run() {
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
							showToast(getString(R.string.invalid));
						}
						break;
					case LoginActivity.RESULT_FIRST_USER:
						final Context ctx = getApplicationContext();
						Thread b = new Thread(new Runnable(){
							@Override
							public void run() {
								CustomBackup c = new CustomBackup();
								c.requestrestore(ctx);
								TopoosInterface.setProfilePicture(getApplicationContext());
							}
			                     });
			        	b.start();
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
				break;
			default:
				break;
		}
	}

	private class RegisterPosition implements LocationListener{

        private static final int TWO_MINUTES = 1000 * 60 * 2;

        /** Determines whether one Location reading is better than the current Location fix
         * @param location  The new Location that you want to evaluate
         * @param currentBestLocation  The current Location fix, to which you want to compare the new one
         */
        protected boolean isBetterLocation(Location location, Location currentBestLocation) {
            if (currentBestLocation == null) {
                // A new location is always better than no location
                return true;
            }
            // Check whether the new location fix is newer or older
            long timeDelta = location.getTime() - currentBestLocation.getTime();
            boolean isSignificantlyNewer = timeDelta > TWO_MINUTES;
            boolean isSignificantlyOlder = timeDelta < -TWO_MINUTES;
            boolean isNewer = timeDelta > 0;
            // If it's been more than two minutes since the current location, use the new location
            // because the user has likely moved
            if (isSignificantlyNewer) {
                return true;
                // If the new location is more than two minutes older, it must be worse
            } else if (isSignificantlyOlder) {
                return false;
            }
            // Check whether the new location fix is more or less accurate
            int accuracyDelta = (int) (location.getAccuracy() - currentBestLocation.getAccuracy());
            boolean isLessAccurate = accuracyDelta > 0;
            boolean isMoreAccurate = accuracyDelta < 0;
            boolean isSignificantlyLessAccurate = accuracyDelta > 200;
            // Check if the old and new location are from the same provider
            boolean isFromSameProvider = isSameProvider(location.getProvider(), currentBestLocation.getProvider());
            // Determine location quality using a combination of timeliness and accuracy
            if (isMoreAccurate) {
                return true;
            } else if (isNewer && !isLessAccurate) {
                return true;
            } else if (isNewer && !isSignificantlyLessAccurate && isFromSameProvider) {
                return true;
            }
            return false;
        }

        /** Checks whether two providers are the same */
        private boolean isSameProvider(String provider1, String provider2) {
            if (provider1 == null) {
                return provider2 == null;
            }
            return provider1.equals(provider2);
        }

		@Override
		public void onLocationChanged(Location arg0) {
            if(!isBetterLocation(arg0,current_pos)) return;
			map.setPos(new LatLng(arg0.getLatitude(),arg0.getLongitude()));
			//discriminar medidas aquí!
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

		}

		@Override
		public void onProviderEnabled(String arg0) {

		}

		@Override
		public void onStatusChanged(String arg0, int arg1, Bundle arg2) {
		}
   }

	private class FetchUser implements Runnable {

		@Override
		public void run() {
			try {
				User u = topoos.Users.Operations.Get(MainActivity.this, "me");
                if(u!=null)Log.i("ID", u.getId());
			} catch (IOException e) {
				e.printStackTrace();
			} catch (TopoosException e) {
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

	private class FetchPOIWorker implements Runnable {
		@Override
		public void run() {
            try {
                poi_list = TopoosInterface.GetNearNFCPOI(getApplicationContext(), new topoos.Objects.Location(current_pos.getLatitude(),current_pos.getLongitude()),0);
                mHandler.removeCallbacks(mUpdateMap);
                mHandler.postDelayed(mUpdateMap, 0);
            } catch (IOException e) {
                e.printStackTrace();
            } catch (TopoosException e) {
                e.printStackTrace();
            } catch (NullPointerException e) {
                e.printStackTrace();
            }
		}
	}

	private class FetchPositionWorker implements Runnable{
		@Override
		public void run() {
            try {
                topoos.Positions.Operations.GetLastUser(getApplicationContext(), topoos.Users.Operations.Get(MainActivity.this, "me").getId());
            } catch (IOException e) {
                e.printStackTrace();
            } catch (TopoosException e) {
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
	public void onReg(String mes) {
        FacebookLogic.publishStory(MainActivity.this, mes);
	    //	scan.refreshLists();
	}

    // Broadcast receiver for receiving status updates from the IntentService

}
