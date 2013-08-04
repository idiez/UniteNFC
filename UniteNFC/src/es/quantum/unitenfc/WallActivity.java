package es.quantum.unitenfc;

import android.app.Activity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ShareActionProvider;

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

}
