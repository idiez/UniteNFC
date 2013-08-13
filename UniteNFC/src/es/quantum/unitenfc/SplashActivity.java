package es.quantum.unitenfc;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;

public class SplashActivity extends Activity {

    private static long SLEEP_TIME = (long) 2;    // Sleep for some time
    private boolean flag = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
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
                e.printStackTrace();
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

