package es.quantum.unitenfc;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.nfc.NdefMessage;
import android.nfc.NdefRecord;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.nfc.tech.MifareClassic;
import android.nfc.tech.MifareUltralight;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Parcelable;
import android.preference.PreferenceManager;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.LinearLayout;
import android.widget.ShareActionProvider;
import android.widget.TextView;

import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.StatusLine;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.protocol.HTTP;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import es.quantum.unitenfc.Objects.NFCPoint;
import es.quantum.unitenfc.nfc_reader.NdefMessageParser;
import es.quantum.unitenfc.nfc_reader.record.ParsedNdefRecord;
import topoos.Exception.TopoosException;
import topoos.Objects.POI;
import topoos.Objects.Position;
import topoos.Objects.User;

public class Serve extends Activity {

    private ShareActionProvider mShareActionProvider;
    private String message;
    private LinearLayout mTagContent;
    private static final DateFormat TIME_FORMAT = SimpleDateFormat.getDateTimeInstance();
    private OnReg a;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        a = (OnReg) getParent();
        setContentView(R.layout.tag_viewer);
        mTagContent = (LinearLayout) findViewById(R.id.linear);
        resolveIntent(getIntent());
    }

    private void resolveIntent(Intent intent) {
        String action = intent.getAction();
        if (NfcAdapter.ACTION_TAG_DISCOVERED.equals(action)
            || NfcAdapter.ACTION_TECH_DISCOVERED.equals(action)
            || NfcAdapter.ACTION_NDEF_DISCOVERED.equals(action)) {
            Parcelable[] rawMsgs = intent.getParcelableArrayExtra(NfcAdapter.EXTRA_NDEF_MESSAGES);
            NdefMessage[] msgs;
            if (rawMsgs != null) {
                msgs = new NdefMessage[rawMsgs.length];
                for (int i = 0; i < rawMsgs.length; i++) {
                    msgs[i] = (NdefMessage) rawMsgs[i];
                }
                Tag tagFromIntent = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);
                CheckNFCPoint checkNFCPoint = new CheckNFCPoint();
                checkNFCPoint.execute(TopoosInterface.bytesToHexString(tagFromIntent.getId()));
                message = parseNFCRecords(msgs[0].getRecords()[0]);
            } else {
                // Unknown tag type
                byte[] empty = new byte[0];
                byte[] id = intent.getByteArrayExtra(NfcAdapter.EXTRA_ID);
                Parcelable tag = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);
                byte[] payload = dumpTagData(tag).getBytes();
                NdefRecord record = new NdefRecord(NdefRecord.TNF_UNKNOWN, empty, id, payload);
                message = parseNFCRecords(record);
                NdefMessage msg = new NdefMessage(new NdefRecord[] { record });
                msgs = new NdefMessage[] { msg };
            }
            // Setup the views
            buildTagViews(msgs);
        }
    }

    private String dumpTagData(Parcelable p) {
        StringBuilder sb = new StringBuilder();
        Tag tag = (Tag) p;
        byte[] id = tag.getId();
        sb.append("Tag ID (hex): ").append(getHex(id)).append("\n");
        sb.append("Tag ID (dec): ").append(getDec(id)).append("\n");
        sb.append("ID (reversed): ").append(getReversed(id)).append("\n");
        String prefix = "android.nfc.tech.";
        sb.append("Technologies: ");
        for (String tech : tag.getTechList()) {
            sb.append(tech.substring(prefix.length()));
            sb.append(", ");
        }
        sb.delete(sb.length() - 2, sb.length());
        for (String tech : tag.getTechList()) {
            if (tech.equals(MifareClassic.class.getName())) {
                sb.append('\n');
                MifareClassic mifareTag = MifareClassic.get(tag);
                String type = "Unknown";
                switch (mifareTag.getType()) {
                    case MifareClassic.TYPE_CLASSIC:
                        type = "Classic";
                        break;
                    case MifareClassic.TYPE_PLUS:
                        type = "Plus";
                        break;
                    case MifareClassic.TYPE_PRO:
                        type = "Pro";
                        break;
                }
                sb.append("Mifare Classic type: ");
                sb.append(type);
                sb.append('\n');

                sb.append("Mifare size: ");
                sb.append(mifareTag.getSize() + " bytes");
                sb.append('\n');

                sb.append("Mifare sectors: ");
                sb.append(mifareTag.getSectorCount());
                sb.append('\n');

                sb.append("Mifare blocks: ");
                sb.append(mifareTag.getBlockCount());
            }
            if (tech.equals(MifareUltralight.class.getName())) {
                sb.append('\n');
                MifareUltralight mifareUlTag = MifareUltralight.get(tag);
                String type = "Unknown";
                switch (mifareUlTag.getType()) {
                    case MifareUltralight.TYPE_ULTRALIGHT:
                        type = "Ultralight";
                        break;
                    case MifareUltralight.TYPE_ULTRALIGHT_C:
                        type = "Ultralight C";
                        break;
                }
                sb.append("Mifare Ultralight type: ");
                sb.append(type);
            }
        }
        return sb.toString();
    }

    private String getHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (int i = bytes.length - 1; i >= 0; --i) {
            int b = bytes[i] & 0xff;
            if (b < 0x10)
                sb.append('0');
            sb.append(Integer.toHexString(b));
            if (i > 0) {
                sb.append(" ");
            }
        }
        return sb.toString();
    }

    private long getDec(byte[] bytes) {
        long result = 0;
        long factor = 1;
        for (int i = 0; i < bytes.length; ++i) {
            long value = bytes[i] & 0xffl;
            result += value * factor;
            factor *= 256l;
        }
        return result;
    }

    private long getReversed(byte[] bytes) {
        long result = 0;
        long factor = 1;
        for (int i = bytes.length - 1; i >= 0; --i) {
            long value = bytes[i] & 0xffl;
            result += value * factor;
            factor *= 256l;
        }
        return result;
    }

    void buildTagViews(NdefMessage[] msgs) {
        if (msgs == null || msgs.length == 0) {
            return;
        }
        LayoutInflater inflater = LayoutInflater.from(this);
        LinearLayout content = mTagContent;
        // Parse the first message in the list
        // Build views for all of the sub records
        Date now = new Date();
        List<ParsedNdefRecord> records = NdefMessageParser.parse(msgs[0]);
        final int size = records.size();
        for (int i = 0; i < size; i++) {
            TextView timeView = new TextView(this);
            timeView.setText(TIME_FORMAT.format(now));
            content.addView(timeView, 0);
            ParsedNdefRecord record = records.get(i);
            content.addView(record.getView(this, inflater, content, i), 1 + i);
            content.addView(inflater.inflate(R.layout.tag_divider, content, false), 2 + i);
        }
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
        mShareActionProvider.setShareIntent(TopoosInterface.createShareIntent(getString(R.string.share_1)+" "+message+" "+getString(R.string.share_2)));
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


    public class CheckNFCPoint extends AsyncTask<String,Void,Boolean>{

        @Override
        protected Boolean doInBackground(String... strings) {
            String tagid = strings[0];
            try{
                User me = topoos.Users.Operations.Get(getApplicationContext(), "me");
                Position current_pos = topoos.Positions.Operations.GetLastUser(getApplicationContext(), me.getId());
                List<POI> poi_list = TopoosInterface.GetNearNFCPOI(getApplicationContext(), new topoos.Objects.Location(current_pos.getLatitude(),current_pos.getLongitude()),10);
                for(POI poi:poi_list){
                    if(poi.getName().substring(0, 16).compareTo(tagid)==0){
                        TopoosInterface.Checking(getApplicationContext(), poi);
                        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
                        String s = prefs.getString("checkpoints", "");
                        SharedPreferences.Editor editor = prefs.edit();
                        String name = poi.getName().substring(16);
                        String wall = poi.getName().substring(0,16);
                        Date d = new Date();
                        String date = d.toLocaleString().substring(0, 16);
                        @SuppressWarnings("deprecation")
                        String title = name+";"+poi.getCategories().get(0).getId()+";"+date+"ñ";
                        editor.putString("checkpoints", title.concat(s));
                        editor.commit();
                        NFCPoint nfcp = new NFCPoint();
                        nfcp.setName(name);
                        nfcp.setPosId(Integer.toString(poi.getCategories().get(0).getId()));
                        nfcp.setDate(date);
                        nfcp.setWall(wall);
                        final NFCPoint np = nfcp;
                        String mes = FacebookLogic.createFacebookFeed(FacebookLogic.REGISTER, tagid, nfcp, "");
                        a.onReg(mes);
                        Thread t = new Thread(new Runnable(){
                            @Override
                            public void run() {
                                try {
                                    SharedPreferences prefss = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
                                    HttpClient httpclient = new DefaultHttpClient();
                                    HttpResponse response = null;
                                    String post_url = "http://unitenfc.herokuapp.com/objects/nfcp/"+prefss.getString("session","")+"/False/";
                                    HttpPost socket = new HttpPost(post_url);
                                    socket.setHeader( "Content-Type", "application/xml" );
                                    socket.setHeader( "Accept", "*/*" );
                                    JSONObject json = new JSONObject();
                                    try {
                                        json.put("name", np.getName());
                                        json.put("posId", np.getPosId());
                                        json.put("date", np.getDate());
                                        json.put("wall", np.getWall());
                                    } catch (JSONException e) {
                                        e.printStackTrace();
                                    }
                                    StringEntity entity = new StringEntity(json.toString(), HTTP.UTF_8);
                                    socket.setEntity(entity);
                                    try {
                                        response = httpclient.execute(socket);
                                    } catch (ClientProtocolException e) {
                                        e.printStackTrace();
                                    } catch (IOException e) {
                                        e.printStackTrace();
                                    }
                                    StatusLine statusLine = response.getStatusLine();
                                    if(statusLine.getStatusCode() == HttpStatus.SC_OK){
                                        ByteArrayOutputStream out = new ByteArrayOutputStream();
                                        try {
                                            response.getEntity().writeTo(out);
                                            out.close();
                                        } catch (IOException e) {
                                            e.printStackTrace();
                                        }
                                        String responseString = out.toString();
                                    }
                                } catch (Exception e) {
                                    e.printStackTrace();
                                }
                            }
                        });
                        t.start();
                        break;
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
                return false;
            } catch (TopoosException e) {
                e.printStackTrace();
                return false;
            }
            return true;
        }
    }
}