package es.quantum.unitenfc.background;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.support.v4.app.NotificationCompat;

import es.quantum.unitenfc.MainActivity;
import es.quantum.unitenfc.POICategories;
import es.quantum.unitenfc.R;

/**
 * Created by root on 8/14/13.
 */
public class ResponseReceiver extends BroadcastReceiver{

        // Called when the BroadcastReceiver gets an Intent it's registered to receive
        @Override
        public void onReceive(Context context, Intent intent) {
            PendingIntent contentIntent = PendingIntent.getActivity(context, 0,
                    new Intent(context, MainActivity.class), 0);
            int count = intent.getIntExtra("counter", 1);
            int poiType = intent.getIntExtra(Constants.EXTENDED_DATA_STATUS, POICategories.INFO);
            int res = R.drawable.nfc_blue;
            switch (poiType) {
                case POICategories.EVENT:
                    res = R.drawable.nfc_green;
                    break;
                case POICategories.LEISURE:
                    res = R.drawable.nfc_violet;
                    break;
                case POICategories.TURISM:
                    res = R.drawable.nfc_orange;
                    break;
                default:
                    break;
            }
            NotificationCompat.Builder mBuilder =
                    new NotificationCompat.Builder(context)
                            .setSmallIcon(R.drawable.scan_tab)
                            .setLargeIcon(BitmapFactory.decodeResource(context.getResources(),res))
                            .setContentTitle(context.getString(R.string.app_name))
                            .setContentText(context.getString(R.string.notification_text));
            mBuilder.setContentIntent(contentIntent);
            mBuilder.setDefaults(Notification.DEFAULT_SOUND);
            mBuilder.setAutoCancel(true);
            mBuilder.setVibrate(new long[]{100, 100, 100, 400});
            mBuilder.setLights(Color.CYAN, 300, 300);
            mBuilder.setNumber(count);
            NotificationManager mNotificationManager =
                    (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
            mNotificationManager.notify(1, mBuilder.build());
        }
}
