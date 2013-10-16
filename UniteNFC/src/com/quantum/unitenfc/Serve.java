package com.quantum.unitenfc;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
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
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ShareActionProvider;
import android.widget.TextView;
import android.widget.Toast;

import com.facebook.Request;
import com.facebook.Response;
import com.facebook.Session;
import com.facebook.SessionState;
import com.facebook.UiLifecycleHelper;
import com.facebook.model.GraphUser;
import com.google.analytics.tracking.android.EasyTracker;
import com.google.gson.Gson;

import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.StatusLine;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
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

import com.quantum.unitenfc.Objects.NFCPoint;
import com.quantum.unitenfc.Objects.Wall;
import com.quantum.unitenfc.nfc_reader.NdefMessageParser;
import com.quantum.unitenfc.nfc_reader.record.ParsedNdefRecord;
import topoos.Exception.TopoosException;
import topoos.Objects.POI;
import topoos.Objects.Position;
import topoos.Objects.User;

public class Serve extends Activity {

    private ShareActionProvider mShareActionProvider;
    private String message;
    private String id;
    private LinearLayout mTagContent;
    private static final DateFormat TIME_FORMAT = SimpleDateFormat.getDateTimeInstance();
    private OnReg a;
    private String mes;
    private ProgressDialog progressDialog;

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
    }
    private final SimpleDateFormat formatter = new SimpleDateFormat("dd/MM/yyyy HH:mm");

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        a = (OnReg) getParent();
        setContentView(R.layout.tag_viewer);
        fb_dialog = new FacebookDialog();
        uiHelper = new UiLifecycleHelper(this, callback);
        uiHelper.onCreate(savedInstanceState);
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        boolean showfbdialog = prefs.getBoolean("showfbdialog", true);
        Session session = Session.getActiveSession();
        if(!(session.getState() == SessionState.CREATED_TOKEN_LOADED || session.isOpened())&& showfbdialog) {
            fb_dialog.setCancelable(false);
            fb_dialog.show(getFragmentManager(), "fb log");
        }
        mTagContent = (LinearLayout) findViewById(R.id.linear);
        final Context ctx = this;
        final String user = prefs.getString("session","");
        ((Button) findViewById(R.id.bttn_wall)).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                AsyncTask<Void, Void, String> toast = new AsyncTask<Void, Void, String>(){

                    @Override
                    protected void onPreExecute(){
                        progressDialog = new ProgressDialog(ctx);
                        progressDialog.setCancelable(false);
                        progressDialog.setMessage(getString(R.string.loading));
                        progressDialog.show();
                    }

                    @Override
                    protected String doInBackground(Void... params) {
                        List<POI> poi;
                        try {
                            String address;
                            poi = topoos.POI.Operations.GetWhere(ctx, new Integer[]{POICategories.NFC} ,null, null, null, null, id);
                            POI nfcpoi = null;
                            if(poi.isEmpty()){
                                return getString(R.string.not_found);
                            }
                            nfcpoi = poi.get(0);
                            address = nfcpoi.getAddress();
                            HttpClient httpclient = new DefaultHttpClient();
                            HttpResponse response = null;
                            try {
                                String wall_id = nfcpoi.getName().substring(0,16);
                                response = httpclient.execute(new HttpGet("http://unitenfc.herokuapp.com/objects/wall/"+wall_id+"/"+user+"/"));
                            } catch (ClientProtocolException e) {
                                e.printStackTrace();
                                return getString(R.string.internet_failure);
                            } catch (IOException e) {
                                e.printStackTrace();
                                return getString(R.string.internet_failure);
                            }
                            StatusLine statusLine = response.getStatusLine();
                            if(statusLine.getStatusCode() == HttpStatus.SC_OK){
                                ByteArrayOutputStream out = new ByteArrayOutputStream();
                                try {
                                    response.getEntity().writeTo(out);
                                    out.close();
                                } catch (IOException e) {
                                    e.printStackTrace();
                                    return getString(R.string.internet_failure);
                                }
                                String responseString = out.toString();
                                Gson gson = new Gson();
                                Wall w = gson.fromJson(responseString, Wall.class);
                                String date = formatter.format(poi.get(0).getLastUpdate());
                                w.setLast_seen_when(date);
                                w.setLast_seen_where(address);
                                return gson.toJson(w)+";"+nfcpoi.getName().substring(0,16);
                            }
                            else {
                                return getString(R.string.not_found);
                            }
                        } catch (IOException e) {
                            e.printStackTrace();
                            return getString(R.string.internet_failure);
                        } catch (TopoosException e) {
                            e.printStackTrace();
                            return getString(R.string.internet_failure);
                        }
                    }
                    @Override
                    protected void onPostExecute(String result) {
                        //Toast.makeText(ctx , "" +result, Toast.LENGTH_SHORT).show();
                        progressDialog.dismiss();
                        if(result.compareTo(getString(R.string.not_found))==0){
                            Toast.makeText(getApplicationContext(),getString(R.string.not_found),Toast.LENGTH_LONG).show();
                        }
                        else if(result.compareTo(getString(R.string.internet_failure))==0){
                            Toast.makeText(getApplicationContext(),getString(R.string.internet_failure),Toast.LENGTH_LONG).show();
                        }
                        else {
                            startActivity(new Intent(ctx, WallActivity.class).putExtra("wall_values",result));
                        }
                    }
                };
                toast.execute();
            }
        });
        resolveIntent(getIntent());
    }

    @Override
    public void onStart() {
        super.onStart();
        // The rest of your onStart() code.
        EasyTracker.getInstance(this).activityStart(this);  // Add this method.
    }

    @Override
    public void onStop(){
        super.onStop();
        EasyTracker.getInstance(this).activityStop(this);
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

    protected void onDestroy() {
        super.onDestroy();
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
                id = TopoosInterface.bytesToHexString(tagFromIntent.getId());
                checkNFCPoint.execute(id);
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

    public static String dumpTagData(Parcelable p) {
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

    public static String getHex(byte[] bytes) {
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

    public static long getDec(byte[] bytes) {
        long result = 0;
        long factor = 1;
        for (int i = 0; i < bytes.length; ++i) {
            long value = bytes[i] & 0xffl;
            result += value * factor;
            factor *= 256l;
        }
        return result;
    }

    public static long getReversed(byte[] bytes) {
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
        getMenuInflater().inflate(R.menu.empty_menu, menu);
        MenuItem menuItem = menu.findItem(R.id.menu_share);
        if(menuItem != null);
        menuItem = menu.findItem(R.id.menu_share);
        if(menuItem != null)
            mShareActionProvider = (ShareActionProvider)menuItem.getActionProvider();
        mShareActionProvider.setShareIntent(TopoosInterface.createShareIntent(getString(R.string.share_1)+" "+message+" "+getString(R.string.share_2)));
        return true;
    }

    public static String parseNFCRecords(NdefRecord ndefr){
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
                List<POI> poi_list = TopoosInterface.GetNearNFCPOI(getApplicationContext(), new topoos.Objects.Location(current_pos.getLatitude(),current_pos.getLongitude()),10000);
                for(POI poi:poi_list){
                    if(poi.getName().substring(0, 16).compareTo(tagid)==0){
                        TopoosInterface.Checking(getApplicationContext(), poi);
                        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
                        String s = prefs.getString("checkpoints", "");
                        SharedPreferences.Editor editor = prefs.edit();
                        String name = poi.getName().substring(16);
                        String wall = poi.getName().substring(0,16);
                        String date = formatter.format(new Date());
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
                        mes = FacebookLogic.createFacebookFeed(FacebookLogic.VISIT, tagid, nfcp, "");

//                        a.onReg(mes);
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
                                return false;
                            }
                            StringEntity entity = new StringEntity(json.toString(), HTTP.UTF_8);
                            socket.setEntity(entity);
                            try {
                                response = httpclient.execute(socket);
                            } catch (ClientProtocolException e) {
                                e.printStackTrace();
                                return false;
                            } catch (IOException e) {
                                e.printStackTrace();
                                return false;
                            }
                            StatusLine statusLine = response.getStatusLine();
                            if(statusLine.getStatusCode() == HttpStatus.SC_OK){
                                ByteArrayOutputStream out = new ByteArrayOutputStream();
                                try {
                                    response.getEntity().writeTo(out);
                                    out.close();
                                } catch (IOException e) {
                                    e.printStackTrace();
                                    return false;
                                }
                                String responseString = out.toString();
                                return true;
                            }
                            return false;
                        } catch (Exception e) {
                            e.printStackTrace();
                            return false;
                        }
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

        @Override
        protected void onPostExecute(Boolean result) {
            if(result){
                if(mes != null) {
                    String ac = Session.getActiveSession().getAccessToken();
                    if(ac != null){
                        if(!ac.isEmpty()){
                            FacebookLogic.publishStory(Serve.this, mes);
                        }
                    }
                }
            }
            else {
                Toast.makeText(getApplicationContext(),getString(R.string.wall_not_found), Toast.LENGTH_LONG).show();
            }
        }
    }
}