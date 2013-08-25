package es.quantum.unitenfc;

import android.app.Fragment;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.nfc.NfcAdapter;
import android.os.Bundle;
import android.os.Environment;
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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class SocialFragment extends Fragment implements OnClickListener{
	
	
	private static final int NAME = 1; 
	private static final int ID = 0;
	
	private ListView list;
	private NfcAdapter mAdapter;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // Inflate the layout for this fragment
    	View V = inflater.inflate(R.layout.social_fragment, container, false);
    	V.findViewById(R.id.add_friend).setOnClickListener((OnClickListener) this);
    	list = (ListView) V.findViewById(R.id.friend_list);
        View header = inflater.inflate(R.layout.header_layout, null);
        list.addHeaderView(header);
        refreshLists();
        mAdapter = NfcAdapter.getDefaultAdapter(getActivity().getApplicationContext());
        return V;
    }

    public List<RowItem> parseString(String s){
    	List<String> listcheck = TopoosInterface.itemize(s);
    	List<RowItem> rows = new ArrayList<RowItem>();
    	if(listcheck.isEmpty()){
            Bitmap bmp = BitmapFactory.decodeResource(getResources(), R.drawable.dummy);
    		rows.add(new RowItem(bmp, getString(R.string.add_friend_ms)));
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
                if(text.compareTo(getString(R.string.add_friend_ms)) != 0)
                startActivity(new Intent(ctx, UserCard.class).setType("NOBEAM").putExtra("NAME", text));
            }
        });
    }

    @Override
    public void onClick(View arg0) {
        if (mAdapter != null && mAdapter.isEnabled()) {
            // adapter exists and is enabled.
            startActivityForResult(new Intent(getActivity().getApplicationContext(), UserCard.class).setType("BEAM"),1);
        }
        else {
            Toast.makeText(this.getActivity(), getString(R.string.nfc_error), Toast.LENGTH_SHORT).show();
            startActivity(new Intent(android.provider.Settings.ACTION_NFC_SETTINGS));
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent ReturnedIntent) {
        refreshLists();
    }
}