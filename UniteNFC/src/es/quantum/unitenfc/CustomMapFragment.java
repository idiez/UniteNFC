package es.quantum.unitenfc;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.Context;
import android.content.DialogInterface;
import android.os.AsyncTask;
import android.os.Bundle;
import android.widget.Toast;

import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.GoogleMap.OnInfoWindowClickListener;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import java.io.IOException;
import java.util.List;
import topoos.Exception.TopoosException;
import topoos.Objects.POI;

public class CustomMapFragment extends MapFragment implements OnInfoWindowClickListener{
	
	int view_type;
	LatLng pos;
	Marker main_marker;
	List<POI> poi_list;
	boolean [] poi_vis;
	CameraPosition cam;
	
	public CustomMapFragment() {
		super();
	
	}

	public void setPos(LatLng pos) {
		this.pos = pos;
	}
	
	
	public void setPOIVis() {
		poi_vis = new boolean[]{true,true,true,true};
	}	
		
	public void POIVis() {
		VisiblePOIFragment vp = new VisiblePOIFragment();
	    vp.show(getFragmentManager(), "visible");
	}
	
	
	
	public void setPOIList(List<POI> poi_list) {
		this.poi_list = poi_list;
	}
	
	@Override
	public void onResume() {
		super.onResume();
		
		getActivity().invalidateOptionsMenu();

		getMap().setOnInfoWindowClickListener(this);
		
		
		
		
	    switch(view_type) { 
        case 0:
            this.getMap().setMapType(GoogleMap.MAP_TYPE_NORMAL);
            break;
        case 1:
        	this.getMap().setMapType(GoogleMap.MAP_TYPE_HYBRID);
            break;
    }
		centerMapAndRefresh(false);
		POIMarkers();
		UpdateMarker();

	}
	
	@Override
	public void onPause() {
		super.onPause();
		getActivity().invalidateOptionsMenu();
		cam = getMap().getCameraPosition();
	}
	
	public void switchMapType() {
	    view_type = (view_type + 1) % 2;
	    switch(view_type) { 
	        case 0:
	            this.getMap().setMapType(GoogleMap.MAP_TYPE_NORMAL);
	            break;
	        case 1:
	        	this.getMap().setMapType(GoogleMap.MAP_TYPE_HYBRID);
	            break;
	    }
	}
		
	public void MoveTo(LatLng latlng) {
		CameraPosition camPos = new CameraPosition.Builder()
        .target(latlng)   
        .zoom(19)         
        .bearing(45)      
        .tilt(70)         
        .build();
 		CameraUpdate camUpd3 =	CameraUpdateFactory.newCameraPosition(camPos);
		this.getMap().animateCamera(camUpd3);
	}
	
	public void UpdateMarker() throws NullPointerException{
		
		if (main_marker != null) main_marker.remove();
		main_marker = this.getMap().addMarker(new MarkerOptions()
		.icon(BitmapDescriptorFactory.fromResource(R.drawable.marker))
        .position(pos)
        .title(getString(R.string.stick_man)))
        ;
		main_marker.showInfoWindow();
	}
		
	public void centerMapAndRefresh(boolean center) {
		if (pos != null) {
			CameraPosition camPos;
			
			if(cam == null || center){
				camPos = new CameraPosition.Builder()
		        .target(pos)   //Centramos el mapa en Madrid
		        .zoom(18)         //Establecemos el zoom en 19
		        .bearing(0)      //Establecemos la orientaci�n con el noreste arriba
		        .tilt(0)         //Bajamos el punto de vista de la c�mara 70 grados
		        .build();
				
			
			}
			else {
				camPos = new CameraPosition.Builder()
		        .target(cam.target)   //Centramos el mapa en Madrid
		        .zoom(cam.zoom)         //Establecemos el zoom en 19
		        .bearing(cam.bearing)      //Establecemos la orientaci�n con el noreste arriba
		        .tilt(cam.tilt)         //Bajamos el punto de vista de la c�mara 70 grados
		        .build();
			}

			CameraUpdate camUpd3 =	CameraUpdateFactory.newCameraPosition(camPos);
			if(center) this.getMap().animateCamera(camUpd3);
			else this.getMap().moveCamera(camUpd3);
			
			}
	}

	public void POIMarkers() throws NullPointerException{
		
		if(poi_list !=null){
			getMap().clear();
			for(POI poi:poi_list){
				int poiType = poi.getCategories().get(0).getId();
				LatLng POIloc = new LatLng(poi.getLatitude(), poi.getLongitude());
				BitmapDescriptor icon;
				String title = poi.getName().substring(16);
				String description = poi.getDescription();
				boolean visibility;
				switch(poiType){
					case POICategories.PROMOTION:	icon= BitmapDescriptorFactory.fromResource(R.drawable.nfc_orange);
													visibility = poi_vis[1];
													break;
					case POICategories.INFO:		icon= BitmapDescriptorFactory.fromResource(R.drawable.nfc_violet);
													visibility = poi_vis[2];
													break;
					case POICategories.HOTSPOT:		icon= BitmapDescriptorFactory.fromResource(R.drawable.nfc_green);
													visibility = poi_vis[3];
													break;
					default: 						icon= BitmapDescriptorFactory.fromResource(R.drawable.nfc_blue);
													visibility = poi_vis[0];
													break;
				}
				this.getMap().addMarker(new MarkerOptions().icon(icon).position(POIloc).title(title).snippet(description)).setVisible(visibility);
			}
						

		}
		
	}
	

	@SuppressLint("ValidFragment")
	private class VisiblePOIFragment extends DialogFragment {
		
			
	    @Override
	    public Dialog onCreateDialog(Bundle savedInstanceState) {
	        // Use the Builder class for convenient dialog construction
	        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
	        
	        builder
	        		.setTitle(getString(R.string.nfc_action))
	        		.setMultiChoiceItems(R.array.poiType, new boolean[]{poi_vis[0],poi_vis[1],poi_vis[2],poi_vis[3]},
			                new DialogInterface.OnMultiChoiceClickListener() {
			            
			         @Override
						public void onClick(DialogInterface arg0, int which, boolean isChecked) {
							switch(which){
								case 1:
									poi_vis[1] = isChecked;
									break;
								case 2:
									poi_vis[2] = isChecked;
									break;
								case 3:
									poi_vis[3] = isChecked;
									break;
								default:
									poi_vis[0] = isChecked;
									break;								
							}
							
						}
			         

			         
			        })
			        

   	        		.setPositiveButton("OK", new DialogInterface.OnClickListener() {
	                   public void onClick(DialogInterface dialog, int id) {
	                	   POIMarkers();
	               		UpdateMarker();  	   
	                	 
	                	   }
	        		})
		      		;
	        
	        		Dialog dialog = builder.create();
	        // Create the AlertDialog object and return it
	        return dialog;
	    }
	    
	    @Override
	    public void onCancel (DialogInterface dialog){
	    	POIMarkers();
           	UpdateMarker(); 
	    }
	    
	}



	@Override
	public void onInfoWindowClick(Marker marker) {
		final String name = marker.getTitle();
		final Context ctx = getActivity().getApplicationContext();
		
		AsyncTask<Void, Void, String> toast = new AsyncTask<Void, Void, String>(){

			@Override
			protected String doInBackground(Void... params) {
				List<POI> poi;
				try {
					poi = topoos.POI.Operations.GetWhere(ctx, new Integer[]{POICategories.NFC} ,null, null, null, null, name);
					if(poi.isEmpty()) return getString(R.string.not_found);
					else return poi.get(0).getAddress();
				} catch (IOException e) {
					e.printStackTrace();
				} catch (TopoosException e) {
					e.printStackTrace();
				}
				return getString(R.string.not_found);
			}
			@Override
			protected void onPostExecute(String result) {
				Toast.makeText(ctx , "" +result, Toast.LENGTH_SHORT).show();
			}
      		
      	};
      	if(!name.equals(getString(R.string.stick_man)))	toast.execute();
	}
	

}