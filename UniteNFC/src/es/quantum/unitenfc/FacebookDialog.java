package es.quantum.unitenfc;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.CheckBox;
import android.widget.Toast;

import com.facebook.widget.LoginButton;

import java.util.Arrays;

/**
 * Created by root on 7/20/13.
 */
public class FacebookDialog extends DialogFragment {


    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        // Use the Builder class for convenient dialog construction
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        LayoutInflater inflater = getActivity().getLayoutInflater();
        View v = inflater.inflate(R.layout.fb, null);
        builder.setView(v);
        builder.setNegativeButton("Ahora no", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                boolean nomore =  ((CheckBox)getDialog().findViewById(R.id.checkBox)).isChecked();
                SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getActivity().getApplicationContext());
                SharedPreferences.Editor editor = prefs.edit();
                editor.putBoolean("showfbdialog", !nomore);
                editor.commit();

            }
        });
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getActivity().getApplicationContext());
        boolean showfbdialog = prefs.getBoolean("showfbdialog", true);
        CheckBox cb = ((CheckBox) v.findViewById(R.id.checkBox));
        cb.setChecked(!showfbdialog);


        return builder.create();
    }

}
