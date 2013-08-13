package es.quantum.unitenfc.background;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.support.v4.app.NotificationCompat;

import es.quantum.unitenfc.MainActivity;
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

            NotificationCompat.Builder mBuilder =
                    new NotificationCompat.Builder(context)
                            .setSmallIcon(R.drawable.scan_tab)
                            .setLargeIcon(BitmapFactory.decodeResource(context.getResources(), R.drawable.nfc_blue))
                            .setContentTitle("My notification")
                            .setContentText("Hello World!");
            mBuilder.setContentIntent(contentIntent);
            mBuilder.setDefaults(Notification.DEFAULT_SOUND);
            mBuilder.setAutoCancel(true);
            NotificationManager mNotificationManager =
                    (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
            mNotificationManager.notify(1, mBuilder.build());
        }

        private Notification crear_Notificacion() {
            Notification notification = new Notification();
            notification.flags |= Notification.FLAG_AUTO_CANCEL;
            //notification.icon = R.drawable.brujula; //que salga con nuestro icono
            notification.when = System.currentTimeMillis();
            return notification;
        }
}
