package es.quantum.unitenfc;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.Window;
import android.view.WindowManager;

public class SplashActivity extends Activity {
	 
	   private static String TAG = SplashActivity.class.getName();
	   private static long SLEEP_TIME = (long) 2;    // Sleep for some time
	   private boolean flag = false;
	 
	   @Override
	   protected void onCreate(Bundle savedInstanceState) {
	      super.onCreate(savedInstanceState);
	 
	      //this.requestWindowFeature(Window.FEATURE_NO_TITLE);    // Removes title bar
	      //this.getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);    // Removes notification bar
	 
	      setContentView(R.layout.splash);
    	  IntentLauncher launcher = new IntentLauncher();
    	  launcher.start();
	      

	   }
	 
	   @Override
	   public void onBackPressed() {
		   super.onBackPressed();
	      flag = true;
	   }
	   
	   
	   private class IntentLauncher extends Thread {
	      @Override
	      /**
	       * Sleep for some time and than start new activity.
	       */
	      public void run() {
	         try {

	            // Sleeping
	            Thread.sleep(SLEEP_TIME*1000);
	         } catch (Exception e) {
	            Log.e(TAG, e.getMessage());
	         }
	         if(!flag){
	         // Start main activity
	         Intent intent = new Intent(SplashActivity.this, MainActivity.class);
	         SplashActivity.this.startActivity(intent);
	         SplashActivity.this.finish();
	         }
	      }
	   }
	}

