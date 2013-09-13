package com.quantum.unitenfc;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Toast;

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
import java.text.SimpleDateFormat;
import java.util.Date;

import com.quantum.unitenfc.Objects.NFCPoint;

public class RegisterPOIFragment extends DialogFragment {
	
	private String idd;
    private String tag_content;
	private int poiType;
	private topoos.Objects.Location loc;
	private String name;
	private String description;
	private OnReg mListener;
    private final SimpleDateFormat formatter = new SimpleDateFormat("dd/MM/yyyy HH:mm");

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
                            switch (which) {
                                case 1:
                                    poiType = POICategories.TURISM;
                                    break;
                                case 2:
                                    poiType = POICategories.LEISURE;
                                    break;
                                case 3:
                                    poiType = POICategories.EVENT;
                                    break;
                                default:
                                    poiType = POICategories.INFO;
                                    break;
                            }
                        }
                    })
            .setPositiveButton(getString(R.string.new_nfc_ok), new DialogInterface.OnClickListener() {

                public void onClick(DialogInterface dialog, int id) {
                    name = ((EditText) getDialog().findViewById(R.id.name)).getText().toString().trim();
                    description = ((EditText) getDialog().findViewById(R.id.description)).getText().toString().trim();
                    if (!(name.isEmpty() || description.isEmpty())) {
                        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getActivity().getApplicationContext());
                        final String s = prefs.getString("regpoints", "");
                        final String user_id = prefs.getString("session", "");
                        final Editor editor = prefs.edit();
                        String date = formatter.format(new Date());
                        final String title = name + ";" + poiType + ";" + date + "ñ";
                        RegisterPOIWorker wrk = new RegisterPOIWorker();
                        Thread thread = new Thread(wrk);
                        thread.start();
                        final NFCPoint nfcp = new NFCPoint();
                        nfcp.setName(name);
                        nfcp.setPosId(Integer.toString(poiType));
                        nfcp.setDate(date);
                        nfcp.setWall(idd);
                        final boolean wall_private = ((CheckBox) getDialog().findViewById(R.id.checkprivate)).isChecked();
                        final NFCPoint np = nfcp;
                        new AsyncTask<Void, Void, Boolean>() {
                            @Override
                            protected Boolean doInBackground(Void... voids) {
                                try {
                                    HttpClient httpclient = new DefaultHttpClient();
                                    HttpResponse response = null;
                                    String post_url = "http://unitenfc.herokuapp.com/objects/nfcp/" + user_id + "/True/";
                                    HttpPost socket = new HttpPost(post_url);
                                    socket.setHeader("Content-Type", "application/xml");
                                    socket.setHeader("Accept", "*/*");
                                    JSONObject json = new JSONObject();
                                    try {
                                        json.put("name", np.getName());
                                        json.put("posId", np.getPosId());
                                        json.put("date", np.getDate());
                                        json.put("wall", np.getWall());
                                        json.put("wall_description", description);
                                        json.put("wall_tag_content", tag_content);
                                        json.put("wall_tag_private", wall_private);
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
                                    if (statusLine.getStatusCode() == HttpStatus.SC_OK) {
                                        ByteArrayOutputStream out = new ByteArrayOutputStream();
                                        try {
                                            response.getEntity().writeTo(out);
                                            out.close();
                                        } catch (IOException e) {
                                            e.printStackTrace();
                                            return false;
                                        }
                                        String responseString = out.toString();
                                    }
                                } catch (Exception e) {
                                    e.printStackTrace();
                                    return false;
                                }
                                return true;
                            }

                            @Override
                            protected void onPostExecute(Boolean result) {
                                if (!result) {
                                    Context ctx = getActivity().getApplicationContext();
                                    Toast.makeText(ctx, ctx.getString(R.string.internet_failure), Toast.LENGTH_LONG).show();
                                } else {
                                    editor.putString("regpoints", title.concat(s));
                                    editor.commit();
                                    String mes = FacebookLogic.createFacebookFeed(FacebookLogic.REGISTER, idd, np, "");
                                    mListener.onReg(mes);
                                }
                            }
                        }.execute();
                    } else {
                        Toast.makeText(getActivity().getApplicationContext(), getString(R.string.new_nfc_fill), Toast.LENGTH_SHORT).show();
                    }
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

    public void setArguments(String id, String tag_content, topoos.Objects.Location loc){
    	this.idd=id;
        this.tag_content = tag_content;
    	this.loc= loc;
    	poiType = POICategories.INFO;
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
}
