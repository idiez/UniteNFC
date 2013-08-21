package es.quantum.unitenfc.background;

import android.app.IntentService;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.content.LocalBroadcastManager;

import com.google.android.gms.maps.model.LatLng;

import java.io.IOException;
import java.util.List;

import es.quantum.unitenfc.TopoosInterface;
import topoos.AccessTokenOAuth;
import topoos.Exception.TopoosException;
import topoos.Objects.POI;

public class ProximityNotifier extends IntentService {

    private Location current_pos;
    private LocationManager mLocationManager;
    private RegisterPosition mCustomLocationListener;

    public ProximityNotifier() {
        super("proximity");
    }

    @Override
    public void onCreate() {
        super.onCreate();

    }

    public void onDestroy() {
        super.onDestroy();
        if (mLocationManager != null && mCustomLocationListener != null) {
            mLocationManager.removeUpdates(mCustomLocationListener);
            mLocationManager = null;
        }
    }


    @Override
    protected void onHandleIntent(Intent intent) {
        // Gets data from the incoming Intent
        mCustomLocationListener = new RegisterPosition();
        mLocationManager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);
        mLocationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 1000, 0, mCustomLocationListener);
        mLocationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1000, 0, mCustomLocationListener);
        current_pos = mLocationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
        new Thread(new Runnable() {
            @Override
            public void run() {
                List<POI> poi_list = null;
                AccessTokenOAuth token = topoos.AccessTokenOAuth.GetAccessToken(getApplicationContext());
                SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
                int count = prefs.getInt("count", 0);
                boolean foo = prefs.getBoolean("saveuser", true);
                if(!(token == null || !token.isValid() || !foo) && prefs.getBoolean("notify", true)) {
                    try {
                        if(current_pos != null){
                            poi_list = TopoosInterface.GetNearNFCPOI(getApplicationContext(), new topoos.Objects.Location(current_pos.getLatitude(), current_pos.getLongitude()),5);
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    } catch (TopoosException e) {
                        e.printStackTrace();
                    }
                    if(poi_list == null) {

                    }
                    else if(!poi_list.isEmpty()){
                        POI poi = poi_list.get(0);
                        int counter = prefs.getInt("poicount",0);
                        int last = prefs.getInt("lastpoi", 0);
                        SharedPreferences.Editor editor = prefs.edit();
                        if(poi.getId() == last && counter < 2*30){
                            editor.putInt("poicount", counter+1);
                        }
                        else {
                            Intent localIntent = new Intent().setAction(Constants.BROADCAST_ACTION)
                                    // Puts the status into the Intent
                                    .putExtra(Constants.EXTENDED_DATA_STATUS,(poi.getCategories().get(0)).getId())
                                    .putExtra("lat",poi.getLatitude())
                                    .putExtra("lon",poi.getLongitude());
                            // Broadcasts the Intent to receivers in this app.
                            sendBroadcast(localIntent);
                            editor.putInt("poicount", 0);
                            editor.putInt("lastpoi", poi.getId());
                        }
                        editor.commit();
                    }
                }
            }
        }).start();
    }


    private class RegisterPosition implements LocationListener {

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
            if(isBetterLocation(arg0,current_pos)){
                current_pos = arg0;
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
}
