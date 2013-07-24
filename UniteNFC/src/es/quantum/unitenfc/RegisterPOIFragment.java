package es.quantum.unitenfc;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.LayoutInflater;
import android.widget.EditText;
import android.widget.Toast;

import java.util.Date;

import es.quantum.unitenfc.Objects.NFCPoint;

public class RegisterPOIFragment extends DialogFragment {
	
	private String idd;
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
        		.setTitle(getString(R.string.new_nfc))
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
		        
        		.setPositiveButton(getString(R.string.new_nfc_ok), new DialogInterface.OnClickListener() {
                   public void onClick(DialogInterface dialog, int id) {
                	               	   
                	   name = ((EditText) getDialog().findViewById(R.id.name)).getText().toString().trim();
                	   description = ((EditText) getDialog().findViewById(R.id.description)).getText().toString().trim();
                	   if(!(name.isEmpty()||description.isEmpty())){
                		   SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getActivity().getApplicationContext());
           	    		    String s = prefs.getString("regpoints", "");
           	    		    Editor editor = prefs.edit();
           	    		    Date d = new Date();
           	    		    @SuppressWarnings("deprecation")
           				    String title = name+";"+poiType+";"+d.toLocaleString().substring(0, 16)+"ñ";
           	    		    editor.putString("regpoints", title.concat(s));
           	    		    editor.commit();
                		   
                	        RegisterPOIWorker wrk = new RegisterPOIWorker();
                		    Thread thread = new Thread(wrk);
           					thread.start();
                            NFCPoint nfcp = new NFCPoint();
                            nfcp.setName(name);
                            nfcp.setPosId(Integer.toString(poiType));
                            String mes = FacebookLogic.createFacebookFeed(FacebookLogic.REGISTER, idd, nfcp, "");
           				    mListener.onReg(mes);
                	   }
                	   else
                           Toast.makeText(getActivity().getApplicationContext(), getString(R.string.new_nfc_fill), Toast.LENGTH_SHORT).show();;
                   }
               })
               .setNegativeButton(getString(R.string.new_nfc_no), new DialogInterface.OnClickListener() {
                   public void onClick(DialogInterface dialog, int id) {
                       // User cancelled the dialog
                   }
               });
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
    	this.idd=id;
    	this.loc= loc;
    	poiType = POICategories.USER;
    }
    
	private class RegisterPOIWorker implements Runnable {

		public void run(){
			try {
				TopoosInterface.RegisterNFCPOI(getActivity().getApplicationContext(), idd+name, description, poiType, loc);
               
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}
    
    public interface OnReg {
        public void onReg(String mes);
    }
	
}
