package es.quantum.unitenfc;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.Context;
import android.content.DialogInterface;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.widget.EditText;
import android.widget.Toast;

import java.util.Date;

import es.quantum.unitenfc.backup.GMailSender;

public class ReportBug extends DialogFragment {

    private OnReg mListener;
    private String comment;
    private String date;
    private Context ctx;

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        // Use the Builder class for convenient dialog construction
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        LayoutInflater inflater = getActivity().getLayoutInflater();
        final String wall_id = this.getTag();
        builder.setView(inflater.inflate(R.layout.comment, null));
        builder
                .setTitle(getString(R.string.report_title))
                .setPositiveButton(getString(R.string.report_send), new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        comment = ((EditText) getDialog().findViewById(R.id.commentline)).getText().toString().trim();
                        if (comment.compareTo("") == 0) {
                            Toast.makeText(getActivity().getApplicationContext(), getString(R.string.dialog_empty_comment), Toast.LENGTH_LONG).show();
                        } else {
                            new SendBug().execute();
                        }
                    }

                })
                .setNegativeButton(getString(R.string.new_nfc_no), new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        // User cancelled the dialog
                    }
                });
        // Create the AlertDialog object and return it
        ctx = getActivity().getApplicationContext();
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

    private class SendBug extends AsyncTask<Void,Void,Boolean>{

        @Override
        protected Boolean doInBackground(Void... voids) {
            GMailSender sender = new GMailSender("unitenfc@gmail.com", "unitenfctopoos");
            Date d = new Date();
            try {
                sender.sendMail("BUG "+d.toLocaleString(),
                        comment,
                        "unitenfc",
                        "izan10saz@gmail.com");
                return true;
            } catch (Exception e) {
                e.printStackTrace();
                return false;
            }
        }

        @Override
        protected void onPostExecute(Boolean result) {
            String mes = "";
            if(result) {
                mes = getString(R.string.report_ok);
            }
            else {
                mes = getString(R.string.report_fail);
            }
            Toast.makeText(ctx,mes, Toast.LENGTH_LONG).show();
        }
    }
}