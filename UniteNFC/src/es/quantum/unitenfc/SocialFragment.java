package es.quantum.unitenfc;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import topoos.Exception.TopoosException;
import topoos.Objects.POI;
import topoos.Objects.User;
import topoos.Objects.UserIdPosition;

import android.app.Fragment;
import android.app.PendingIntent;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.nfc.NdefMessage;
import android.nfc.NdefRecord;
import android.nfc.NfcAdapter;
import android.nfc.NfcAdapter.CreateNdefMessageCallback;
import android.nfc.NfcAdapter.OnNdefPushCompleteCallback;
import android.nfc.NfcEvent;
import android.nfc.tech.NfcF;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

public class SocialFragment extends Fragment implements OnClickListener{
	
	
	private static final int NAME = 1; 
	private static final int ID = 0; 
	
	
	private ListView list;
	List<UserIdPosition> friends;
	List<RowItem> rowItems;
	private List<String> names;
	private ProgressDialog progressDialog;
	Runnable mMuestraMensaje = new Runnable() {
        public void run() {
           Toast.makeText(getActivity(), "Lanzado temporizador", Toast.LENGTH_LONG).show();
           progressDialog.dismiss();
	       //mAdapter.disableForegroundDispatch(getActivity());
        }
	};
	private Handler mHandler = new Handler();
	private NfcAdapter mAdapter;
	
	
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, 
        Bundle savedInstanceState) {
        // Inflate the layout for this fragment
    	View V = inflater.inflate(R.layout.social_fragment, container, false);
    	V.findViewById(R.id.add_friend).setOnClickListener((OnClickListener) this);
    	
    	list = (ListView) V.findViewById(R.id.friend_list);
    	
        View header = inflater.inflate(R.layout.header_layout, null);
        list.addHeaderView(header);
    	
       // V.findViewById(R.id.add_friend).setOnClickListener(this);
        refreshLists();
        
        mAdapter = NfcAdapter.getDefaultAdapter(getActivity().getApplicationContext());
        
        return V;
    }
    
    public void setFriendList(List<UserIdPosition> friends){
    	this.friends = friends;
    }
    
    
      
public List<RowItem> parseString(String s){
		
    	List<String> listcheck = TopoosInterface.itemize(s);
    	List<RowItem> rows = new ArrayList<RowItem>();
    	if(listcheck.isEmpty()){
            Bitmap bmp = BitmapFactory.decodeResource(getResources(), R.drawable.dummy);
    		rows.add(new RowItem(bmp, "Tap and add friends"));
    	}
    	else {
    		for(String element:listcheck){
        		String title =(TopoosInterface.extract(element, NAME));
        		String id = TopoosInterface.extract(element, ID);
        		String path = Environment.getExternalStorageDirectory().toString();
				Bitmap bmp = BitmapFactory.decodeFile(path+"/unitenfc/"+id+".png");
				if(bmp == null){
					BitmapFactory.Options options = new BitmapFactory.Options();
		            options.inJustDecodeBounds = true;
		            bmp = BitmapFactory.decodeResource(getResources(), R.drawable.dummy, options);
				}
                rows.add(new RowItem(bmp, title));
        	}
    	}
    	Collections.sort(rows);
		return rows;
		
	}

public void refreshLists(){
	SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this.getActivity().getApplicationContext());
	String s1 = prefs.getString("friends", "");
	List<RowItem> r1 = parseString(s1);

    CustomListViewAdapter adapter = new CustomListViewAdapter(getActivity().getApplicationContext(),
            R.layout.list, r1);
    list.setAdapter(adapter);

    list.setOnItemClickListener(new OnItemClickListener(){

        @Override
        public void onItemClick(AdapterView<?> arg0, View arg1, int arg2, long arg3) {
        	if(arg2 == 0) return;
        	Context ctx = getActivity().getApplicationContext();
			String text = (String) ((TextView) arg1.findViewById(R.id.text1)).getText();
			if(text.compareTo("Tap and add friends") != 0)
			startActivity(new Intent(ctx, UserCard.class).setType("NOBEAM").putExtra("NAME", text));
        }
    }
    		
    		);
}

@Override
public void onClick(View arg0) {
	
	if (mAdapter != null && mAdapter.isEnabled()) {
	    // adapter exists and is enabled.
		startActivityForResult(new Intent(getActivity().getApplicationContext(), UserCard.class).setType("BEAM"),1);
		
		

	}
	else {
		Toast.makeText(this.getActivity(), "Please, enable NFC in your settings.", Toast.LENGTH_SHORT).show();
	}

	
	
}


	
@Override
public void onActivityResult(int requestCode, int resultCode, Intent ReturnedIntent) {
	refreshLists();
}
    
}