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

    ShareActionProvider mShareActionProvider;
    String message;
    private LinearLayout mTagContent;
    private static final DateFormat TIME_FORMAT = SimpleDateFormat.getDateTimeInstance();
    OnReg a;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //setContentView(R.layout.splash);
        a = (OnReg) getParent();
        setContentView(R.layout.tag_viewer);
        mTagContent = (LinearLayout) findViewById(R.id.linear);
        resolveIntent(getIntent());

        /*
        setContentView(R.layout.nfc_dispatch);
        ListView v = (ListView)findViewById(R.id.tags);
        // see if app was started from a tag
        resolveIntent(getIntent());
        Intent i = getIntent();
        Log.i("INTENT", i.getType());
        if(i.getType() != null){
            //if (NfcAdapter.ACTION_NDEF_DISCOVERED.equals(getIntent().getAction()))

            if(i.getType().equals("text/plain")) {	//add any intent you are filtering for
                NdefMessage[] messages = null;
                Parcelable[] rawMsgs = i.getParcelableArrayExtra(NfcAdapter.EXTRA_NDEF_MESSAGES);
                if (rawMsgs != null) {
                    messages = new NdefMessage[rawMsgs.length];
                    for (int i1 = 0; i1 < rawMsgs.length; i1++) {
                        messages[i1] = (NdefMessage) rawMsgs[i1];
                        Log.i("BYTES", ""+messages[i1].toByteArray());
                    }
                }
                if(messages[0] != null) {
                    List<String> list = new ArrayList<String>();
                    for(int a= 0; a<messages.length;a++){
                        NdefRecord[] rec = messages[a].getRecords();
                        message = parseNFCRecords(rec[0]);
                        for(int c= 0; a<messages.length;a++){
                            list.add(parseNFCRecords(rec[c]));
                        }

                    }
                    List<RowItem> rowItems = new ArrayList<RowItem>();
                    for (String s11:list) {
                        RowItem item = new RowItem(null, s11);
                        rowItems.add(item);
                    }
                    CustomListViewAdapter adapter = new CustomListViewAdapter(this.getApplicationContext(),
                            R.layout.list, rowItems);
                    v.setAdapter(adapter);
                    Tag tagFromIntent = i.getParcelableExtra(NfcAdapter.EXTRA_TAG);
                    CheckNFCPoint checkNFCPoint = new CheckNFCPoint();
                    checkNFCPoint.execute(TopoosInterface.bytesToHexString(tagFromIntent.getId()));
                }
            }
            else if (i.getType().equals("text/plain")){
                //if (NfcAdapter.ACTION_NDEF_DISCOVERED.equals(getIntent().getAction()))
                // tagFromIntent.getTechList();
            }
            else {
                this.onBackPressed();
            }

            /*
             String result="";
             Toast.makeText(getApplicationContext(), ""+messages[0].getRecords()[0].getTnf(), Toast.LENGTH_SHORT).show();
             byte[] payload = messages[0].getRecords()[0].getPayload();
             // this assumes that we get back am SOH followed by host/code
             for (int b = 1; b<payload.length; b++) { // skip SOH
                  result += (char) payload[b];
             }
             Toast.makeText(getApplicationContext(), "Tag Contains " + result, Toast.LENGTH_SHORT).show();  */




    /*           MifareUltralight m = MifareUltralight.get(tagFromIntent);
                try {
                    m.connect();
                } catch (IOException e) {
                    e.printStackTrace();
                }*/


            /*
                //Log.i("TEST",intent.getAction());
                Intent intent = new Intent("android.nfc.action.TECH_DISCOVERED");
            PackageManager manager = getApplicationContext().getPackageManager();
            List<ResolveInfo> r = manager.queryIntentActivities(intent, 0);
            for(ResolveInfo e:r){
                Log.i("TEST",e.activityInfo.packageName);
            }


            ComponentName componentName = new ComponentName(r.get(0).activityInfo.packageName, r.get(0).activityInfo.name);
            //Intent i = new Intent("android.intent.action.MAIN");
            //i.addCategory("android.intent.category.LAUNCHER").setFlags(Intent.FLAG_ACTIVITY_NEW_TASK).setComponent(componentName);
            i.setAction(Intent.ACTION_MAIN);
            i.addCategory(Intent.CATEGORY_LAUNCHER);
            i.setComponent(componentName).setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);


            //i = manager.getLaunchIntentForPackage("app package name");
            //i.addCategory(Intent.CATEGORY_LAUNCHER);
            startActivity(i);
            //startActivityForResult(i,7);
            */
/*
        }
        else{this.onBackPressed();}

*/
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
        //DO SOMETHING SHOW!

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
                        Date d = new Date();
                        @SuppressWarnings("deprecation")
                        String title = name+";"+poi.getCategories().get(0).getId()+";"+d.toLocaleString().substring(0, 16)+"ñ";
                        editor.putString("checkpoints", title.concat(s));
                        editor.commit();

                        NFCPoint nfcp = new NFCPoint();
                        nfcp.setName(name);
                        nfcp.setPosId(Integer.toString(poi.getCategories().get(0).getId()));
                        String mes = FacebookLogic.createFacebookFeed(FacebookLogic.REGISTER, tagid, nfcp, "");
                        //a.onReg(mes);


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