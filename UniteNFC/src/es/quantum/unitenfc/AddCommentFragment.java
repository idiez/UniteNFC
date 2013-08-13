package es.quantum.unitenfc;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.LayoutInflater;
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
import java.io.UnsupportedEncodingException;
import java.util.Date;

public class AddCommentFragment extends DialogFragment {

    private OnReg mListener;
    private String comment;
    private String date;

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        // Use the Builder class for convenient dialog construction
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        LayoutInflater inflater = getActivity().getLayoutInflater();
        final String wall_id = this.getTag();
        builder.setView(inflater.inflate(R.layout.comment, null));
        builder
                .setTitle("Añadir comentario")
                .setPositiveButton(getString(R.string.new_nfc_ok), new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        comment = ((EditText) getDialog().findViewById(R.id.commentline)).getText().toString().trim();
                        if (comment.compareTo("") == 0) {
                            Toast.makeText(getActivity().getApplicationContext(), "Escribe algo", Toast.LENGTH_LONG).show();
                        } else {
                            new RegisterEntry().execute(wall_id);
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

    private class RegisterEntry extends AsyncTask<String,Void,Boolean>{

        @Override
        protected Boolean doInBackground(String... strings) {
            Date d = new Date();
            date = d.toLocaleString().substring(0, 16);
            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getActivity().getApplicationContext());
            HttpClient httpclient = new DefaultHttpClient();
            HttpResponse response = null;
            String post_url = "http://unitenfc.herokuapp.com/objects/entry/"+strings[0]+"/";
            HttpPost socket = new HttpPost(post_url);
            socket.setHeader( "Content-Type", "application/xml" );
            socket.setHeader( "Accept", "*/*" );
            JSONObject json = new JSONObject();
            try {
                json.put("author_name", prefs.getString("username",""));
                json.put("message", comment);
                json.put("author_pic_uri", prefs.getString("imageuri",""));
                json.put("time_stamp", date);
            } catch (JSONException e) {
                e.printStackTrace();
            }
            StringEntity entity = null;
            try {
                entity = new StringEntity(json.toString(), HTTP.UTF_8);
            } catch (UnsupportedEncodingException e1) {
                e1.printStackTrace();
            }
            socket.setEntity(entity);
            Log.i("REQUEST", socket.getRequestLine().toString());
            try {
                response = httpclient.execute(socket);
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
                return true;
            }
            return false;
        }

        @Override
        protected void onPostExecute(Boolean result) {
            if(result) {
                mListener.onReg(comment+";"+date);
            }
        }
    }
}