package es.quantum.unitenfc;

import android.app.Activity;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.List;

public class EntriesListViewAdapter extends ArrayAdapter<EntryItem> {

    private Context context;

    public EntriesListViewAdapter(Context context, int resourceId, List<EntryItem> items) {
        super(context, resourceId, items);
        this.context = context;
    }

    /*private view holder class*/
    private class ViewHolder {
        ImageView imageView;
        TextView txtmessage;
        TextView txtbydate;
        TextView txtdate;
    }

    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder holder = null;
        EntryItem rowItem = getItem(position);
        LayoutInflater mInflater = (LayoutInflater) context.getSystemService(Activity.LAYOUT_INFLATER_SERVICE);
        if (convertView == null) {
            convertView = mInflater.inflate(R.layout.entry, null);
            holder = new ViewHolder();
            holder.txtmessage = (TextView) convertView.findViewById(R.id.entry_comment);
            holder.txtbydate = (TextView) convertView.findViewById(R.id.entry_bydate);
            holder.txtdate = (TextView) convertView.findViewById(R.id.entry_date);
            holder.imageView = (ImageView) convertView.findViewById(R.id.entry_icon);
            convertView.setTag(holder);
        } else
        holder = (ViewHolder) convertView.getTag();
        holder.txtmessage.setText(rowItem.getMessage());
        holder.txtbydate.setText(rowItem.getAuthor());
        holder.txtdate.setText(rowItem.getDate());
        holder.imageView.setImageBitmap(rowItem.getImageId());
        return convertView;
    }
}