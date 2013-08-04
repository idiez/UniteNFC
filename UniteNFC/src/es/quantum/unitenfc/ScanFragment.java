package es.quantum.unitenfc;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import es.quantum.unitenfc.Objects.Wall;
import topoos.Exception.TopoosException;
import topoos.Objects.Checkin;
import topoos.Objects.POI;
import android.app.Fragment;
import android.app.PendingIntent;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.nfc.NfcAdapter;
import android.nfc.tech.NfcF;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.gson.Gson;

import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.StatusLine;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;

public class ScanFragment extends Fragment implements OnClickListener, OnItemClickListener {
	
	private static final int NAME = 0; //NFC tag
	private static final int ID = 3; //general purpose user created NFC tag
	private static final int TYPE = 1; //promotion tag
	private static final int DATE = 2; //information tag (for example turistic, transport, etc)
	
	
	private ProgressDialog progressDialog;
	private List<Checkin> check;
	private List<String> reg;
	
	private NfcAdapter mAdapter;
	private Handler mHandler = new Handler();
	Runnable mMuestraMensaje = new Runnable() {
        public void run() {
           progressDialog.dismiss();
	       mAdapter.disableForegroundDispatch(getActivity());
        }
	};
	private ListView list1;
	private ListView list2;

	
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, 
        Bundle savedInstanceState) {
        // Inflate the layout for this fragment
    	View V = inflater.inflate(R.layout.scan_fragment, container, false);
    	
    	list1 = (ListView) V.findViewById(R.id.checking_list);
    	list2 = (ListView) V.findViewById(R.id.register_list);
    	
        View header1 = inflater.inflate(R.layout.header_layout, null);
        View header2 = inflater.inflate(R.layout.header_layout, null);
    	header1.setClickable(false);
        header2.setClickable(false);
        list1.addHeaderView(header1);
        ((TextView)header1).setText(getString(R.string.last_visited));
        list2.addHeaderView(header2);
        ((TextView)header2).setText(getString(R.string.last_registered));
        list1.setOnItemClickListener(this);
        list2.setOnItemClickListener(this);
        
    	refreshLists();
        
    	mAdapter = NfcAdapter.getDefaultAdapter(this.getActivity().getApplicationContext());
    	
    	V.findViewById(R.id.bttn_scan).setOnClickListener(this);
        return V;
    }
    
	@Override
	public void onResume() {
		super.onResume();
		if(progressDialog != null) progressDialog.dismiss();
    }
    
	@Override
	public void onPause() {
		super.onPause();
		mAdapter.disableForegroundDispatch(this.getActivity());
		mHandler.removeCallbacks(mMuestraMensaje);
	}
	
	
	@Override
	public void onClick(View v) {
		if (mAdapter != null && mAdapter.isEnabled()) {
		    // adapter exists and is enabled.
			PendingIntent pendingIntent = PendingIntent.getActivity(getActivity().getApplicationContext(), 0,
		            new Intent(getActivity().getApplicationContext(), getActivity().getClass()).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP), 0);
			
			IntentFilter [] intentfilter = new IntentFilter[] {new IntentFilter(NfcAdapter.ACTION_TAG_DISCOVERED)};
			String [][] techlist = new String[][] { new String[] { NfcF.class.getName() } };
			mAdapter.enableForegroundDispatch(getActivity(), pendingIntent, intentfilter, null);
	        mHandler.removeCallbacks(mMuestraMensaje);
	        mHandler.postDelayed(mMuestraMensaje, 5000);
			
	        progressDialog = ProgressDialog.show((Context)this.getActivity(), getString(R.string.reg_nfc),
                    getString(R.string.reg_nfc_wait),false,false);
		}
		else {
			Toast.makeText(this.getActivity(), getString(R.string.nfc_error), Toast.LENGTH_SHORT).show();
		}
		
	}

	public List<Checkin> getCheck() {
		return check;
	}

	public void setCheck(List<Checkin> check) {
		this.check = check;
	}
	
	public List<RowItem> parseString(String s){
		
    	List<String> listcheck = TopoosInterface.itemize(s);
    	List<RowItem> rows = new ArrayList<RowItem>();
    	if(listcheck.isEmpty()){
            Bitmap bmp = BitmapFactory.decodeResource(getResources(), R.drawable.dummy);
    		rows.add(new RowItem(bmp, getString(R.string.default_visited)));
    	}
    	else {
    		for(String element:listcheck){
        		String title =(TopoosInterface.extract(element, NAME)+"\n"+TopoosInterface.extract(element, DATE));
        		String type = TopoosInterface.extract(element, TYPE);
        		int res_id;
        		switch(Integer.parseInt(type)){
        			case POICategories.USER:
        				res_id = R.drawable.nfc_blue;
        				break;
        			case POICategories.PROMOTION:
        				res_id = R.drawable.nfc_orange;
        				break;
        			case POICategories.INFO:
        				res_id = R.drawable.nfc_violet;
        				break;
        			default:
        				res_id = R.drawable.nfc_green;
        				break;
        		
        		}
        		Bitmap bmp = BitmapFactory.decodeResource(getResources(), res_id);
                rows.add(new RowItem(bmp, title));
        	}
    	}
		
		return rows;
		
	}
	
	public void refreshLists(){
    	SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this.getActivity().getApplicationContext());
    	String s1 = prefs.getString("checkpoints", "");
    	String s2 = prefs.getString("regpoints", "");

        List<RowItem> r1 = parseString(s1);
        List<RowItem> r2 = parseString(s2);
        if(r1.size()>10) r1 = r1.subList(0, 10);
        if(r2.size()>10) r2 = r2.subList(0, 10);
        
        CustomListViewAdapter adapter1 = new CustomListViewAdapter(getActivity().getApplicationContext(),
                R.layout.list, r1);
        CustomListViewAdapter adapter2 = new CustomListViewAdapter(getActivity().getApplicationContext(),
                R.layout.list, r2);
        list1.setAdapter(adapter1);
        list2.setAdapter(adapter2);
	}

	@Override
	public void onItemClick(AdapterView<?> arg0, View arg1, int arg2, long arg3) {
		if(arg2 == 0) return;
       	final View argview = arg1;
       	TextView v = (TextView)arg1.findViewById(R.id.text1);
       	final Context ctx = getActivity().getApplicationContext();
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(ctx);
        final String user = prefs.getString("session","");
       	final String[] t = (v.getText().toString()).split("\n");

        AsyncTask<Void, Void, String> toast = new AsyncTask<Void, Void, String>(){

            @Override
            protected String doInBackground(Void... params) {
                List<POI> poi;
                try {
                    String address = "";
                    poi = topoos.POI.Operations.GetWhere(ctx, new Integer[]{POICategories.NFC} ,null, null, null, null, t[0]);
                    if(poi.isEmpty()){
                        return getString(R.string.not_found);
                    }
                    else {
                        address = poi.get(0).getAddress();
                    }
                    HttpClient httpclient = new DefaultHttpClient();
                    HttpResponse response = null;
                    try {
                        String wall_id = poi.get(0).getName().substring(0,16);
                        response = httpclient.execute(new HttpGet("http://unitenfc.herokuapp.com/objects/wall/"+wall_id+"/"+user+"/"));
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
                        Gson gson = new Gson();
                        Wall w = gson.fromJson(responseString, Wall.class);
                        w.setLast_seen_when(poi.get(0).getLastUpdate().toString());
                        w.setLast_seen_where(address);
                        return gson.toJson(w);
                    }
                    else {
                        return getString(R.string.not_found);
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                } catch (TopoosException e) {
                    e.printStackTrace();
                }
                return getString(R.string.not_found);
            }
            @Override
            protected void onPostExecute(String result) {
                //Toast.makeText(ctx , "" +result, Toast.LENGTH_SHORT).show();
                startActivity(new Intent(ctx, WallActivity.class).putExtra("wall_values",result));
            }

        };
      	toast.execute();
	}
	
}