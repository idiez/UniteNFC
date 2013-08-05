package es.quantum.unitenfc;

import android.app.Activity;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.RatingBar;
import android.widget.ShareActionProvider;
import android.widget.TextView;

import com.google.gson.Gson;

import java.io.IOException;
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
public class WallActivity extends Activity implements AdapterView.OnItemClickListener, OnReg {

    ShareActionProvider mShareActionProvider;
    String message;
    private String wall_id;
    private List<Entry> entries;
    private ListView list;
    private List<EntryItem> r1;
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
        ((TextView)findViewById(R.id.rating_mean)).setText(Integer.toString(w.getMean_rating()));
        ((RatingBar)findViewById(R.id.ratingBar)).setRating(w.getMy_rating());
        list = (ListView) findViewById(R.id.entries);
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

    public void refreshLists(){
        EntriesListViewAdapter adapter1 = new EntriesListViewAdapter(getApplicationContext(),R.layout.entry, r1);
        list.setAdapter(adapter1);
        list.setOnItemClickListener(this);
    }


    @Override
    public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {


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
            refreshLists();
        }
    }
}
