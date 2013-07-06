package es.quantum.unitenfc;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.nfc.NdefMessage;
import android.nfc.NdefRecord;
import android.nfc.NfcAdapter;
import android.nfc.NfcAdapter.CreateNdefMessageCallback;
import android.nfc.NfcAdapter.OnNdefPushCompleteCallback;
import android.nfc.NfcEvent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Parcelable;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.ShareActionProvider;
import android.widget.TextView;
import android.widget.Toast;

import com.google.gson.Gson;

import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.StatusLine;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;

import es.quantum.unitenfc.BluetoothConn.OnSrvRcv;
import es.quantum.unitenfc.Objects.NFCPoint;
import es.quantum.unitenfc.Objects.UserInfo;
import es.quantum.unitenfc.backup.CustomBackup;
import es.quantum.unitenfc.backup.GMailSender;
import topoos.Exception.TopoosException;
import topoos.Objects.User;

public class UserCard extends Activity implements CreateNdefMessageCallback, OnNdefPushCompleteCallback, OnSrvRcv{
	
	public final String MIME_TYPE = "application/es.quantum.unitenfc";
	public static final String BACKUP_URI = "https://dl.dropboxusercontent.com/u/20933121/"; 
	public static final String FILE_TYPE = ".txt"; 
	
	ProgressDialog progressDialog;
	ShareActionProvider mShareActionProvider;
	ImageView pic;
	TextView name, acc;
	ListView check;
	boolean beam;
	boolean newf;
	boolean timeout;
	private NfcAdapter mAdapter;
	String blue_mac;
	SharedPreferences prefs;
	
	private Handler mHandler = new Handler();
	Runnable mShow = new Runnable() {
        public void run() {
          progressDialog.show();
          progressDialog.setCancelable(false);
        }
	};
	Runnable mHide = new Runnable() {
        public void run() {
          progressDialog.dismiss();
        }
	};
	
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.usercard);
		progressDialog = new ProgressDialog((Context)this);
		progressDialog.setMessage("Loading...");
		beam = false;
		pic = (ImageView) findViewById(R.id.profile);
		name = (TextView) findViewById(R.id.username);
		acc = (TextView) findViewById(R.id.useraccount);
		check = (ListView) findViewById(R.id.tags);
		
		prefs = PreferenceManager.getDefaultSharedPreferences((Context)this);
		// see if app was started from a tag
		Intent i = getIntent();
		if(i.getType().equals(MIME_TYPE)) {
			newf = true;
			Parcelable[] rawMsgs = i.getParcelableArrayExtra(NfcAdapter.EXTRA_NDEF_MESSAGES);
            NdefMessage msg = (NdefMessage) rawMsgs[0];
            NdefRecord cardRecord = msg.getRecords()[0];
            String[] rcv_data = new String(cardRecord.getPayload()).split(" ");
            String user_data = rcv_data[0];
            String mac = rcv_data[1];
			//save friend data
            boolean correct = addFriend(user_data);
            final String[] names = user_data.split(";");
            //download image
            if(correct){
	            new AsyncTask<String,Void,Void>(){	            	
	            	@Override
	            	protected void onPreExecute(){
	            		progressDialog.show();
	            		progressDialog.setCancelable(false);
	            	}		
	            	
					@Override
					protected Void doInBackground(String... element) {
						String[] fields = element[0].split(";");
						String filename = fields[0];
						HttpClient httpclient = new DefaultHttpClient();
			    	    HttpResponse response = null;
						try {
							response = httpclient.execute(new HttpGet(CustomBackup.BACKUP_URI+filename+CustomBackup.FILE_TYPE));
						} catch (ClientProtocolException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						} catch (IOException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
			    	    StatusLine statusLine = response.getStatusLine();
			    	    if(statusLine.getStatusCode() == HttpStatus.SC_OK){
			    	        ByteArrayOutputStream out = new ByteArrayOutputStream();
			    	        try {
								response.getEntity().writeTo(out);
								out.close();
							} catch (IOException e) {
								// TODO Auto-generated catch block
								e.printStackTrace();
							}
			    	        
			    	        String responseString = out.toString();
			    	        
			    	        String[] param = responseString.split("\n");
			    	        String myuser = prefs.getString("session", "")+";"+prefs.getString("username", "")+";"+prefs.getString("imageuri", "")+"�";
			    	        String content = param[0]+"\n"+param[1]+"\n"+param[2]+"\n"+param[3]+"\n"+myuser.concat(param[4])+"\nend";
			    	        
			                GMailSender sender = new GMailSender("unitenfc@gmail.com", "unitenfctopoos");
			                try {
								sender.sendMail(filename,   
								        content,   
								        "unitenfc",   
								        "izan_005d@sendtodropbox.com");
							} catch (Exception e) {
								// TODO Auto-generated catch block
								e.printStackTrace();
							}  
			    	        
			    	    } else{
			    	        //Closes the connection.
			    	        try {
								response.getEntity().getContent().close();
							} catch (IllegalStateException e) {
								// TODO Auto-generated catch block
								e.printStackTrace();
							} catch (IOException e) {
								// TODO Auto-generated catch block
								e.printStackTrace();
							}
			    	        try {
								throw new IOException(statusLine.getReasonPhrase());
							} catch (IOException e) {
								// TODO Auto-generated catch block
								e.printStackTrace();
							}
			    	    }
			    	        
			    	   
			                String i1 = topoos.Images.Operations.GetImageURIThumb(TopoosInterface.extract(element[0], 2),topoos.Images.Operations.SIZE_SMALL);
							Bitmap bmp1 = TopoosInterface.LoadImageFromWebOperations(i1);
							String path1 = Environment.getExternalStorageDirectory().toString()+"/unitenfc/";
							File file1 = new File(path1,TopoosInterface.extract(element[0], 0)+".png");
							FileOutputStream out11;
							try {
								out11 = new FileOutputStream(file1);
								bmp1.compress(Bitmap.CompressFormat.PNG, 100, out11);
							} catch (FileNotFoundException e) {
								// TODO Auto-generated catch block
								e.printStackTrace();
							} catch (NullPointerException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
							}
						return null;
					}
	
	            	@Override
	            	protected void onPostExecute(Void a){
	            		showFriend(names[1]);
	            	}				
	            
	            }.execute(user_data);
            }

            //MANDAR DATOS PROPIOS
			
		}
		else if(i.getType().compareTo("BEAM") == 0){
			beam = true;
			String path = Environment.getExternalStorageDirectory().toString();
			Bitmap bmp = BitmapFactory.decodeFile(path+"/unitenfc/profile.png");
			pic.setImageBitmap(bmp);
			name.setText(prefs.getString("username", "empty"));
			LoadCard l = new LoadCard();
			l.execute("me");

	    	
	    	 View header = this.getLayoutInflater().inflate(R.layout.header_layout, null);
	         check.addHeaderView(header);
	         ((TextView)header).setText("Last NFC Points visited:");
            
			
			
			mAdapter = NfcAdapter.getDefaultAdapter(getApplicationContext());
			mAdapter.setNdefPushMessageCallback(this,this);
			mAdapter.setOnNdefPushCompleteCallback(this, this);
			//NDEF PUSH!!!
			
		}
		else{
			String names = i.getStringExtra("NAME");
			showFriend(names);

		}
		

	}
	
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.empty_menu, menu);		
		MenuItem menuItem = menu.findItem(R.id.menu_share);
		
		
		if(menuItem != null);
		menuItem = menu.findItem(R.id.menu_share);
		if(menuItem != null)
		mShareActionProvider = (ShareActionProvider)menuItem.getActionProvider();
        mShareActionProvider.setShareIntent(TopoosInterface.createShareIntent("Just scanned tag with UniteNFC."));
		return true;

	}
	
	private class LoadCard extends AsyncTask<String,Void,UserInfo>{

		String iid;
		String uname = "";
		
		@Override
		protected UserInfo doInBackground(String... id) {
			User usr = null;
			HttpClient httpclient = new DefaultHttpClient();
    	    HttpResponse response = null;
    	    iid = id[0];
    	    try {
				usr = topoos.Users.Operations.Get(getApplicationContext(), iid);
				uname = usr.getName();
			} catch (IOException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			} catch (TopoosException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
			try {
				response = httpclient.execute(new HttpGet(BACKUP_URI+iid+FILE_TYPE));
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
	    	    Gson gson = new Gson();
	    	    UserInfo session = gson.fromJson(responseString.substring(0, responseString.length()-2), UserInfo.class);
		    	return session;
    	    } else{
	    	    //Closes the connection.
	    	    try {
	    	    	response.getEntity().getContent().close();
				} catch (IllegalStateException e) {
					e.printStackTrace();
				} catch (IOException e) {
					e.printStackTrace();
				}
	    	    try {
	    	    	throw new IOException(statusLine.getReasonPhrase());
				} catch (IOException e) {
					e.printStackTrace();
				}
    	    }
			return null;
		}
		
		@Override
		protected void onPreExecute(){

			progressDialog.show();
			progressDialog.setCancelable(false);
		}
		
		
		@Override
		protected void onPostExecute(UserInfo result) {
			progressDialog.dismiss();
			if(beam){
				beam = false;
				Toast.makeText(getApplicationContext(), "Beam your User Card to add a friend", Toast.LENGTH_LONG).show();
			}
			List<RowItem> rows = new ArrayList<RowItem>();
			if(result != null){
				name.setText(result.getUser_name());
				acc.setText(uname);
				//
				SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
				String friends = prefs.getString("friends", "");
				String friendso = "";
				List<String> friendl = TopoosInterface.itemize(friends);
				for(String element:friendl){
					if(iid.compareTo(TopoosInterface.extract(element, 0))==0){
						element = iid+";"+result.getUser_name()+";"+result.getPic_uri();
					}
					friendso = friendso+element+"�";
				}
				Editor editor = prefs.edit();
				editor.putString("friends", friendso);
				editor.commit();
				//
				List<NFCPoint> vis = result.getVisited();
				for(NFCPoint element: vis){
		    		int res_id;
		    		switch(Integer.parseInt(element.getPosId())){
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
			    		Bitmap bmp1 = BitmapFactory.decodeResource(getResources(), res_id);
			            rows.add(new RowItem(bmp1, element.getName()+"\n"+element.getDate()));
				}
		    }
			else{
				acc.setText("");
	            Bitmap bmp1 = BitmapFactory.decodeResource(getResources(), R.drawable.dummy);
	    		rows.add(new RowItem(bmp1, "No NFC Point visited yet"));
			}
			CustomListViewAdapter adapter1 = new CustomListViewAdapter(getApplicationContext(), R.layout.list, rows);
			check.setAdapter(adapter1);
			
		}
}

	private NdefMessage createMessage(){
		//NdefRecord appRecord = NdefRecord.createApplicationRecord("es.quantum.unitenfc");
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
		byte[] payload = (prefs.getString("session", "")+";"+prefs.getString("username", "")+";"+prefs.getString("imageuri", "dummy_4")+"�"+blue_mac).getBytes();
		byte[] mimeBytes = MIME_TYPE.getBytes(Charset.forName("US-ASCII"));
		NdefRecord cardRecord = new NdefRecord(NdefRecord.TNF_MIME_MEDIA, mimeBytes, new byte[0], payload);
		//MANDAR URI Y NOMBRE APARTE DE ID
		//MANDAR TAMBI�N MAC BLUETOOTH	
		return new NdefMessage(new NdefRecord[] { cardRecord});
	}

	@Override
	public NdefMessage createNdefMessage(NfcEvent arg0) {
		return createMessage();
	}

	@Override
	public void onNdefPushComplete(NfcEvent event) {
		// RELOAD USER CARD
		final String friendfield = prefs.getString("friends", "");
		/*progressDialog.show();
		progressDialog.setCancelable(false);*/
		AsyncTask<Void, Void, Void> b = new AsyncTask<Void, Void, Void>(){

			@Override 
			protected void onPreExecute(){
				mHandler.removeCallbacks(mShow);
				mHandler.postDelayed(mShow, 0);
			}
			
			@Override
			protected Void doInBackground(Void... arg0) {
				long milis = System.currentTimeMillis();
				timeout = false;
				/*ProgressDialog progressDialog = ProgressDialog.show(ctx, "",
	            "Loading user data...",false,false);*/
				do{
				CustomBackup c = new CustomBackup();
				c.requestrestore(getApplicationContext());
				TopoosInterface.setProfilePicture(getApplicationContext());
				timeout = System.currentTimeMillis()-milis>10000;
				} while(friendfield.compareTo(prefs.getString("friends", "")) == 0 && !timeout);
				return null;
			}
			
			@Override
			protected void onPostExecute(Void a){
				if(!timeout){
					String[] name = TopoosInterface.itemize(prefs.getString("friends", "")).get(0).split(";");
					showFriend(name[1]);
				}
				else{
					mHandler.removeCallbacks(mHide);
					mHandler.postDelayed(mHide, 0);
				}
			}
		};
		b.execute();
    	
		Log.i("BTSRV", "SECOND");
	}
	
	@Override
	public void onSrvRcv(String msgrcv) {
		String friend_data = msgrcv;
		//STORE FRIEND DATA AND SHOW
		Log.i("BTSRV", "FIRST");

		
		
	}
	
	public boolean addFriend(String friend_data){
		SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences((Context)this);
		String friendlist = pref.getString("friends", "");
		List<String> list = TopoosInterface.itemize(friendlist);
		String[] data = friend_data.split(";");
		String friend = data[1];		
		boolean duplicated = false;
		for(String element:list){
			String[] s = element.split(";"); 
			if(s[1].compareTo(friend) == 0){
				Toast.makeText((Context)this, "You are already friends!", Toast.LENGTH_LONG).show();
				showFriend(s[1]);
				duplicated = true;
				break;
			}
    	}
		if(!duplicated){
			Editor editor = pref.edit();
			editor.putString("friends", (friend_data+"�").concat(friendlist));
			editor.commit();
		}
		return !duplicated;
		
	}
	
	public void showFriend(String names){
		
		String friends = prefs.getString("friends", "");
		List<String> flist = TopoosInterface.itemize(friends);
		String[] params;
			for(String element:flist){
				params = element.split(";");
				if(params[1].compareTo(names)==0){
					String path = Environment.getExternalStorageDirectory().toString();
					Bitmap bmp = BitmapFactory.decodeFile(path+"/unitenfc/"+params[0]+".png");
					pic.setImageBitmap(bmp);
					name.setText(params[1]);
					LoadCard l = new LoadCard();
					l.execute(params[0]);

			    	 View header = this.getLayoutInflater().inflate(R.layout.header_layout, null);
			    	 if(check.getHeaderViewsCount() < 1){
				         check.addHeaderView(header);
				         ((TextView)header).setText("Last NFC Points visited:");
			    	 }
					break;
				}	
			}
	}
	
	protected void onDestroy() {
		super.onDestroy();
		if(newf){
			Thread b = new Thread(new Runnable(){
	
				@Override
				public void run() {
					CustomBackup c = new CustomBackup();
					c.requestbackup(getApplicationContext());
				}
	    		
	    	});
	    	b.start();
		}
	}
}