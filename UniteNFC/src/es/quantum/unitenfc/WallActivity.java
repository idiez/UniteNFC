package es.quantum.unitenfc;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.RatingBar;
import android.widget.ShareActionProvider;
import android.widget.TextView;

import com.google.gson.Gson;

import java.util.ArrayList;
import java.util.List;
import java.util.zip.Inflater;

import es.quantum.unitenfc.Objects.Wall;

/**
 * Created by root on 8/4/13.
 */
public class WallActivity extends Activity {

    ShareActionProvider mShareActionProvider;
    String message;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.wall);
        String wall = getIntent().getStringExtra("wall_values");
        if(wall.compareTo(getString(R.string.not_found))==0){
            finish();
            return;
        }
        Gson gson = new Gson();
        Wall w = gson.fromJson(wall, Wall.class);
        ((TextView)findViewById(R.id.wall_title)).setText(w.getTitle());
        ((TextView)findViewById(R.id.wall_description)).setText(w.getDescription());
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
        Bitmap bmp = BitmapFactory.decodeResource(getResources(), res_id);
        ((ImageView)findViewById(R.id.wall_image)).setImageBitmap(bmp);
        ((TextView)findViewById(R.id.last_seen_when)).setText(w.getLast_seen_when());
        ((TextView)findViewById(R.id.last_seen_where)).setText(w.getLast_seen_where());
        ((TextView)findViewById(R.id.rating_mean)).setText(Integer.toString(w.getMean_rating()));
        ((RatingBar)findViewById(R.id.ratingBar)).setRating(w.getMy_rating());
        //refreshlists
        List<RowItem> r1 = parseString("");

        ListView list = (ListView) findViewById(R.id.entries);
        LayoutInflater li = LayoutInflater.from(getApplicationContext());
        View header1 = li.inflate(R.layout.header_layout, null);
        //header1.setClickable(true);
        ((TextView)header1).setText("+ Añadir comentario");
        ((TextView)header1).setTextColor(Color.BLACK);
        list.addHeaderView(header1);
        CustomListViewAdapter adapter1 = new CustomListViewAdapter(getApplicationContext(),
                R.layout.list, r1);
        list.setAdapter(adapter1);


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


    public List<RowItem> parseString(String s){

        List<String> listcheck = TopoosInterface.itemize(s);
        List<RowItem> rows = new ArrayList<RowItem>();
        if(listcheck.isEmpty()){
            Bitmap bmp = BitmapFactory.decodeResource(getResources(), R.drawable.dummy);
            rows.add(new RowItem(bmp, getString(R.string.default_visited)));
        }

        return rows;

    }

}
