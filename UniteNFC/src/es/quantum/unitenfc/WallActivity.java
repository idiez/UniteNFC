package es.quantum.unitenfc;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.RatingBar;
import android.widget.ShareActionProvider;
import android.widget.TextView;
import android.widget.Toast;

import com.google.gson.Gson;

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
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.Inflater;

import es.quantum.unitenfc.Objects.Entry;
import es.quantum.unitenfc.Objects.Wall;

/**
 * Created by root on 8/4/13.
 */
public class WallActivity extends Activity implements OnReg {

    ShareActionProvider mShareActionProvider;
    String message;
    private String wall_id;
    private List<Entry> entries;
    private ListView list;
    private List<EntryItem> r1;
    private ProgressDialog progressDialog;
    private RatingBar ratingBar;
    private TextView mean;
    private ProgressBar pg;
    private boolean sudo;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.wall);
        String i = getIntent().getStringExtra("wall_values");
        String [] wallinfo = i.split(";");
        String wall = wallinfo[0];
        wall_id = wallinfo[1];
        if(wall.compareTo(getString(R.string.not_found))==0){
            finish();
            return;
        }
        Gson gson = new Gson();
        Wall w = gson.fromJson(wall, Wall.class);
        ((TextView)findViewById(R.id.wall_title)).setText(w.getTitle());
        ((TextView)findViewById(R.id.wall_description)).setText(w.getDescription());
        entries = w.getEntry_list();
        sudo = w.isSudo();
        int res_id;
        switch(Integer.parseInt(w.getType())){
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
        String when = w.getLast_seen_when();
        Bitmap bmp = BitmapFactory.decodeResource(getResources(), res_id);
        ((ImageView)findViewById(R.id.wall_image)).setImageBitmap(bmp);
        ((TextView)findViewById(R.id.last_seen_when)).setText(when.substring(4,16));
        ((TextView)findViewById(R.id.last_seen_where)).setText(w.getLast_seen_where());
        mean = ((TextView)findViewById(R.id.rating_mean));
        mean.setText(Integer.toString(w.getMean_rating()));
        int my_rating = w.getMy_rating();
        ratingBar = ((RatingBar)findViewById(R.id.ratingBar));
        ratingBar.setRating(my_rating);
        pg = (ProgressBar) findViewById(R.id.commentprogress);
        if(my_rating != 0){
            ratingBar.setOnTouchListener(new View.OnTouchListener() {
                public boolean onTouch(View v, MotionEvent event) {
                    return true;
                }
            });
        }
        else {
            final Context ctxx = (Context) this;
            ((Button) findViewById(R.id.rate)).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    final int rating = (int) ratingBar.getRating();
                    if(rating != 0){
                        new AsyncTask<Void,Void,String>(){

                            @Override
                            protected String doInBackground(Void... voids) {
                                SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
                                HttpClient httpclient = new DefaultHttpClient();
                                HttpResponse response = null;
                                String post_url = "http://unitenfc.herokuapp.com/objects/wall/rate/";
                                HttpPost socket = new HttpPost(post_url);
                                socket.setHeader( "Content-Type", "application/xml" );
                                socket.setHeader( "Accept", "*/*" );
                                JSONObject json = new JSONObject();
                                try {
                                    json.put("wall", wall_id);
                                    json.put("value", rating);
                                    json.put("user_id", prefs.getString("session",""));
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
                                        String r = out.toString();
                                        out.close();
                                        return r;
                                    } catch (IOException e) {
                                        e.printStackTrace();
                                    }
                                }
                                return "";
                            }

                            @Override
                            protected void onPostExecute(String result) {
                                if(result.compareTo("")!=0) {
                                    Toast.makeText(getApplicationContext(), "Puntuación enviada", Toast.LENGTH_LONG).show();
                                    mean.setText(result);
                                    ratingBar.setOnTouchListener(new View.OnTouchListener() {
                                        public boolean onTouch(View v, MotionEvent event) {
                                            return true;
                                        }
                                    });
                                }
                                else {
                                    Toast.makeText(getApplicationContext(), "Algo fue mal, prueba de nuevo", Toast.LENGTH_LONG).show();
                                }
                                refreshLists();
                            }

                        }.execute();
                        new Thread(new Runnable() {
                            @Override
                            public void run() {

                            }
                        }).start();
                    }
                    else {
                        Toast.makeText(ctxx,"Da una puntuación primero",Toast.LENGTH_LONG).show();
                    }
                }
            });
        }
        list = (ListView) findViewById(R.id.entries);
        if(sudo) {
            ((TextView) findViewById(R.id.admin)).setText("(Admin)");
            registerForContextMenu(list);
        }
        LayoutInflater li = LayoutInflater.from(getApplicationContext());
        View header1 = li.inflate(R.layout.add_entry, null);
        header1.setClickable(true);
        header1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                new AddCommentFragment().show(getFragmentManager(),wall_id);
            }
        });
        list.addHeaderView(header1);
        new loadComments().execute();
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

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo)
    {
        super.onCreateContextMenu(menu, v, menuInfo);
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_ctx, menu);
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) item.getMenuInfo();
        switch (item.getItemId()) {
            case R.id.delete_comment:
                r1.remove(info.position-1);
                final String line = ((TextView)info.targetView.findViewById(R.id.entry_comment)).getText().toString();
                new AsyncTask<Void, Void, String>() {
                    @Override
                    protected String doInBackground(Void... voids) {
                        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
                        HttpClient httpclient = new DefaultHttpClient();
                        HttpResponse response = null;
                        String post_url = "http://unitenfc.herokuapp.com/objects/entry/delete/";
                        HttpPost socket = new HttpPost(post_url);
                        socket.setHeader( "Content-Type", "application/xml" );
                        socket.setHeader( "Accept", "*/*" );
                        JSONObject json = new JSONObject();
                        try {
                            json.put("wall", wall_id);
                            json.put("message", line);
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
                                String responseString = out.toString();
                                out.close();
                                return responseString;
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }
                        return null;
                    }

                    @Override
                    protected void onPostExecute(String result) {
                        mean.setText(result);
                        Toast.makeText(getApplicationContext(), "Comentario eliminado", Toast.LENGTH_LONG).show();
                        refreshLists();
                    }

                }.execute();
                return true;
            default:
                return super.onContextItemSelected(item);
        }
    }

    public void refreshLists(){
        EntriesListViewAdapter adapter1 = new EntriesListViewAdapter(getApplicationContext(),R.layout.entry, r1);
        list.setAdapter(adapter1);
        //list.setOnItemClickListener(this);
    }

    @Override
    public void onReg(String mes) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        String[]mess = mes.split(";");
        Entry new_entry = new Entry();
        new_entry.setAuthor_name(prefs.getString("username",""));
        new_entry.setAuthor_pic_uri(prefs.getString("imageuri",""));
        new_entry.setTime_stamp(mess[1]);
        new_entry.setMessage(mess[0]);
        entries.add(0,new_entry);
        new loadComments().execute();
    }

    public class loadComments extends AsyncTask<Void, Void, Void>{

        @Override
        protected void onPreExecute() {
            pg.setVisibility(View.VISIBLE);
        }

        @Override
        protected Void doInBackground(Void... voids) {
            r1 = new ArrayList<EntryItem>();
            if(entries.isEmpty()){
                Bitmap bmp = BitmapFactory.decodeResource(getResources(), R.drawable.dummy);
                r1.add(new EntryItem(bmp,"dummy", "date","Ningún comentario aún"));
            }
            else {
                for(Entry e:entries){
                    //String i = topoos.Images.Operations.GetImageURIThumb(e.getAuthor_pic_uri(),topoos.Images.Operations.SIZE_SMALL);
                    //Bitmap bmp = TopoosInterface.LoadImageFromWebOperations(i);
                    URL url = null;
                    try {
                        String uri = e.getAuthor_pic_uri();
                        url = new URL(("https://pic.topoos.com/"+uri+"?size=small"));
                    } catch (MalformedURLException e1) {
                        e1.printStackTrace();
                    }
                    Bitmap bmp;
                    try {
                        bmp = BitmapFactory.decodeStream(url.openConnection().getInputStream());
                    } catch (IOException e1) {
                        bmp = BitmapFactory.decodeResource(getResources(), R.drawable.dummy);
                        e1.printStackTrace();
                    }
                    r1.add(new EntryItem(bmp,e.getAuthor_name(),e.getTime_stamp(), e.getMessage()));
                }
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void result) {
            pg.setVisibility(View.GONE);
            refreshLists();
        }
    }
}
