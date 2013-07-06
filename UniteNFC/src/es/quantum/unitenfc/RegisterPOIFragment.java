package es.quantum.unitenfc;

import java.io.IOException;
import java.util.Date;

import topoos.Exception.TopoosException;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.location.Location;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.LayoutInflater;
import android.widget.EditText;
import android.widget.Toast;

public class RegisterPOIFragment extends DialogFragment {
	
	private String id;
	private int poiType;
	private topoos.Objects.Location loc;
	private String name;
	private String description;
	private OnReg mListener;
	
	
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        // Use the Builder class for convenient dialog construction
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        
        LayoutInflater inflater = getActivity().getLayoutInflater();
        
        builder.setView(inflater.inflate(R.layout.registerpoi, null));
        builder
        		.setTitle("New NFC Point. Register?")
        		.setSingleChoiceItems(R.array.poiType, 0,
		                new DialogInterface.OnClickListener() {
		            
		         @Override
					public void onClick(DialogInterface arg0, int which) {
						switch(which){
							case 1:
								poiType = POICategories.PROMOTION;
								break;
							case 2:
								poiType = POICategories.INFO;
								break;
							case 3:
								poiType = POICategories.HOTSPOT;
								break;
							default:
								poiType = POICategories.USER;
								break;								
						}
						
					}
		        })
		        
        		.setPositiveButton("Register", new DialogInterface.OnClickListener() {
                   public void onClick(DialogInterface dialog, int id) {
                	               	   
                	   name = ((EditText) getDialog().findViewById(R.id.name)).getText().toString().trim();
                	   description = ((EditText) getDialog().findViewById(R.id.description)).getText().toString().trim();
                	   if(!(name.isEmpty()||description.isEmpty())){
                		   SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getActivity().getApplicationContext());
           	    		String s = prefs.getString("regpoints", "");
           	    		Editor editor = prefs.edit();
           	    		Date d = new Date();
           	    		@SuppressWarnings("deprecation")
           				String title = name+";"+poiType+";"+d.toLocaleString().substring(0, 16)+"�";
           	    		editor.putString("regpoints", title.concat(s));
           	    		editor.commit();
                		   
                		   RegisterPOIWorker wrk = new RegisterPOIWorker();
                		   Thread thread = new Thread(wrk);
           					thread.start();
           					
           				mListener.onReg();
                	   }
                	   else Toast.makeText(getActivity().getApplicationContext(), "Fill all the data.", Toast.LENGTH_SHORT).show();;
                		   
           			
                   }
               })
               .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                   public void onClick(DialogInterface dialog, int id) {
                       // User cancelled the dialog
                   }
               })
              
               ;
        
        // Create the AlertDialog object and return it
        return builder.create();
    }
    
    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        try {
            mListener = (OnReg) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString() + " must implement OnReg");
        }
    }
    
    
    public void setArguments(String id, topoos.Objects.Location loc){
    	this.id=id;
    	this.loc= loc;
    	poiType = POICategories.USER;
    }
    
	private class RegisterPOIWorker implements Runnable {
		
    		
    	
		public void run(){
			try {
				TopoosInterface.RegisterNFCPOI(getActivity().getApplicationContext(), id+name, description, poiType, loc);
	    		
			
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}
    
    public interface OnReg {
        public void onReg();
    }
	
}
