package es.quantum.unitenfc;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

import topoos.Exception.TopoosException;
import topoos.Objects.User;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.preference.Preference;
import android.preference.PreferenceManager;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.PreferenceActivity;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.ListView;

public class Settings extends PreferenceActivity implements OnPreferenceChangeListener{
	
	private CustomPrefDialog mCompoundEditTextPref;
	
    @SuppressWarnings("deprecation")
	@Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
 
        
        this.addPreferencesFromResource(R.xml.first_configuration);
    
        mCompoundEditTextPref = (CustomPrefDialog) findPreference("comp_edittext_pref");
        mCompoundEditTextPref.setOnPreferenceChangeListener(this);
        mCompoundEditTextPref.setPath("Some value");
  	  	mCompoundEditTextPref.setCompoundButtonText(getString(R.string.gallery));

  	  	mCompoundEditTextPref.setCompoundButtonListener(new View.OnClickListener() {			
      @Override
	      public void onClick(View v) {
	    	  Intent photoPickerIntent = new Intent(Intent.ACTION_PICK);
	    	  photoPickerIntent.setType("image/*");
	    	  startActivityForResult(photoPickerIntent, 3);  
	    	  mCompoundEditTextPref.getDialog().cancel();
	      }
	    });
    }
 
    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        return true;
    }

    @Override
    public void onStop(){
    	super.onStop();
    }
    
    @Override
    protected void onActivityResult(int requestCode, int resultCode, 
    	       Intent imageReturnedIntent) {
    	    super.onActivityResult(requestCode, resultCode, imageReturnedIntent); 

    	    switch(requestCode) { 
    	    case 3:
    	        if(resultCode == RESULT_OK){  
    	            Uri selectedImage = imageReturnedIntent.getData();
    	            String[] filePathColumn = {MediaStore.Images.Media.DATA};

    	            Cursor cursor = getContentResolver().query(
    	                               selectedImage, filePathColumn, null, null, null);
    	            cursor.moveToFirst();

    	            int columnIndex = cursor.getColumnIndex(filePathColumn[0]);
    	            final String filePath = cursor.getString(columnIndex);
    	            cursor.close();

    	            Thread t = new Thread(new Runnable(){

    					@Override
    					public void run() {
    						
    						try {
    							User usr = null;
    							try {
    								usr = topoos.Users.Operations.Get(getApplicationContext(), "me");
    							} catch (IOException e1) {
    								e1.printStackTrace();
    							} catch (TopoosException e1) {
    								e1.printStackTrace();
    							}
    							String unique = TopoosInterface.UploadPIC(getApplicationContext(), (usr != null)?usr.getName():"test",filePath);
    			    			SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
    			    			Editor editor = prefs.edit();
    			    			editor.putString("imageuri", unique);
    			    			editor.commit();
    			    			TopoosInterface.setProfilePicture(getApplicationContext());
    						} catch (IOException e) {
    							e.printStackTrace();
    						} catch (TopoosException e) {
    							e.printStackTrace();
    						}
    					}

    	        	});
    	        	t.start();
    	        }
    	    }
    	}
    
}