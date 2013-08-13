package es.quantum.unitenfc.background;

import android.app.IntentService;
import android.content.Intent;
import android.support.v4.content.LocalBroadcastManager;

public class ProximityNotifier extends IntentService {

    public ProximityNotifier() {
        super("proximity");
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        // Gets data from the incoming Intent
        String dataString = intent.getDataString();

        Intent localIntent = new Intent().setAction(Constants.BROADCAST_ACTION)
                        // Puts the status into the Intent
                        .putExtra(Constants.EXTENDED_DATA_STATUS, "OK");
        // Broadcasts the Intent to receivers in this app.
        sendBroadcast(localIntent);
    }



}
