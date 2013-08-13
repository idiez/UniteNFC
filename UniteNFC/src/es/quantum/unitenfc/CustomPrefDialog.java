package es.quantum.unitenfc;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Environment;
import android.preference.DialogPreference;
import android.util.AttributeSet;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;

public class CustomPrefDialog extends DialogPreference {
	 
	private ImageView mEditText;	
	private Button mButton;
	private String mPath;
	private CharSequence mCompoundButtonText;
	private View.OnClickListener mCompoundButtonCallback;

    public CustomPrefDialog(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        setDialogLayoutResource(R.layout.cprefdialog);
    }
	 
    public CustomPrefDialog(Context context, AttributeSet attrs) {
        super(context, attrs);
        setDialogLayoutResource(R.layout.cprefdialog);
    }

    @Override
    protected void onPrepareDialogBuilder(AlertDialog.Builder builder) {
        builder.setPositiveButton(null, null);
        //builder.setNegativeButton(null, null);
        super.onPrepareDialogBuilder(builder);
    }
	  
    @Override
    protected View onCreateDialogView() {
        View root = super.onCreateDialogView();
        mEditText = (ImageView) root.findViewById(R.id.image2);
        mButton = (Button) root.findViewById(R.id.button2);
        return root;
    }
		
    public void setPath(String text) {
        mPath = text;
    }
				
    public String getPath() {
        return mPath;
    }
			 
    public void setCompoundButtonText(CharSequence text) {
        mCompoundButtonText = text;
    }

    @Override
    protected void onBindDialogView(View view) {
        String path = Environment.getExternalStorageDirectory().toString();
        Bitmap bmp = BitmapFactory.decodeFile(path+"/unitenfc/profile.png");
        mEditText.setImageBitmap(bmp);
        mButton.setText(mCompoundButtonText);
        // Set a callback to our button.
        mButton.setOnClickListener(mCompoundButtonCallback);
    }
			
    public void setCompoundButtonListener(View.OnClickListener callback) {
        mCompoundButtonCallback = callback;
    }
			
    @Override
    public void onClick(DialogInterface dialog, int which) {
        switch(which) {
            case DialogInterface.BUTTON_POSITIVE: // User clicked OK!
                break;
            default:
                break;
        }
        super.onClick(dialog, which);
    }
}