package es.quantum.unitenfc;

import android.app.Activity;
import android.app.ProgressDialog;
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

import es.quantum.unitenfc.Objects.Friend;
import es.quantum.unitenfc.Objects.NFCPoint;
import es.quantum.unitenfc.Objects.UserInfo;
import es.quantum.unitenfc.backup.CustomBackup;
import es.quantum.unitenfc.backup.GMailSender;
import topoos.Exception.TopoosException;
import topoos.Objects.User;

public class UserCard extends Activity implements CreateNdefMessageCallback, OnNdefPushCompleteCallback {
	
	public final String MIME_TYPE = "application/es.quantum.unitenfc";

	private ProgressDialog progressDialog;
    private ShareActionProvider mShareActionProvider;
    private ImageView pic;
    private TextView name, acc;
    private ListView check;
	boolean beam;
	boolean newf;
	boolean timeout;
	private NfcAdapter mAdapter;
    private String blue_mac;
    private SharedPreferences prefs;
	
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
		progressDialog = new ProgressDialog(this);
		progressDialog.setMessage(getString(R.string.loading));
		beam = false;
		pic = (ImageView) findViewById(R.id.profile);
		name = (TextView) findViewById(R.id.username);
		acc = (TextView) findViewById(R.id.useraccount);
		check = (ListView) findViewById(R.id.tags);
		prefs = PreferenceManager.getDefaultSharedPreferences(this);
		// see if app was started from a tag
		Intent i = getIntent();
		if(i.getType().equals(MIME_TYPE)) {
			newf = true;
			Parcelable[] rawMsgs = i.getParcelableArrayExtra(NfcAdapter.EXTRA_NDEF_MESSAGES);
            NdefMessage msg = (NdefMessage) rawMsgs[0];
            NdefRecord cardRecord = msg.getRecords()[0];
            String[] rcv_data = new String(cardRecord.getPayload()).split(" ");
            String user_data = rcv_data[0];
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
							response = httpclient.execute(new HttpGet(CustomBackup.BACKUP_URI+filename));
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
                            UserInfo session = gson.fromJson(responseString, UserInfo.class);
                            List<Friend> friends = session.getFriends();
                            Friend me = new Friend();
                            me.setFriend_id(prefs.getString("session", ""));
                            me.setFriend_name(prefs.getString("username", ""));
                            me.setFriend_pic_uri(prefs.getString("imageuri", ""));
                            friends.add(me);
                            session.setFriends(friends);
                            String json = gson.toJson(session);
			                GMailSender sender = new GMailSender("unitenfc@gmail.com", "unitenfctopoos");
			                try {
								sender.sendMail(filename,   
								        json,
								        "unitenfc",   
								        "izan_005d@sendtodropbox.com");
							} catch (Exception e) {
								e.printStackTrace();
							}
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
                        String i1 = topoos.Images.Operations.GetImageURIThumb(TopoosInterface.extract(element[0], 2),topoos.Images.Operations.SIZE_SMALL);
                        Bitmap bmp1 = TopoosInterface.LoadImageFromWebOperations(i1);
                        String path1 = Environment.getExternalStorageDirectory().toString()+"/unitenfc/";
                        File file1 = new File(path1,TopoosInterface.extract(element[0], 0)+".png");
                        FileOutputStream out11;
                        try {
                            out11 = new FileOutputStream(file1);
                            bmp1.compress(Bitmap.CompressFormat.PNG, 100, out11);
                        } catch (FileNotFoundException e) {
                            e.printStackTrace();
                        } catch (NullPointerException e) {
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
		}
		else if(i.getType().compareTo("BEAM") == 0){
		    beam = true;
			String path = Environment.getExternalStorageDirectory().toString();
			Bitmap bmp = BitmapFactory.decodeFile(path+"/unitenfc/profile.png");
			pic.setImageBitmap(bmp);
			name.setText(prefs.getString("username", ""));
			LoadCard l = new LoadCard();
			l.execute(PreferenceManager.getDefaultSharedPreferences(getApplicationContext()).getString("session",""));
	    	View header = this.getLayoutInflater().inflate(R.layout.header_layout, null);
            if(check.getHeaderViewsCount() < 1){
                check.addHeaderView(header);
                ((TextView)header).setText(getString(R.string.last_visited));
            }
			mAdapter = NfcAdapter.getDefaultAdapter(getApplicationContext());
			mAdapter.setNdefPushMessageCallback(this,this);
			mAdapter.setOnNdefPushCompleteCallback(this, this);
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
        mShareActionProvider.setShareIntent(TopoosInterface.createShareIntent(getString(R.string.share_1)+getString(R.string.share_2)));
		return true;
	}

    private class LoadCard extends AsyncTask<String,Void,UserInfo>{

		private String iid;
		private String uname = "";
		
		@Override
		protected UserInfo doInBackground(String... id) {
            iid = id[0];
			User usr;
			HttpClient httpclient = new DefaultHttpClient();
    	    HttpResponse response = null;

    	    try {
				usr = topoos.Users.Operations.Get(getApplicationContext(), iid);
				uname = usr.getName();
			} catch (IOException e1) {
				e1.printStackTrace();
			} catch (TopoosException e1) {
				e1.printStackTrace();
			}
			try {
				response = httpclient.execute(new HttpGet(CustomBackup.BACKUP_URI+iid));
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
                return gson.fromJson(responseString, UserInfo.class);
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
				Toast.makeText(getApplicationContext(), getString(R.string.beam), Toast.LENGTH_LONG).show();
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
					friendso = friendso+element+"ñ";
				}
				Editor editor = prefs.edit();
				editor.putString("friends", friendso);
				editor.commit();
				//
				List<NFCPoint> vis = result.getVisited();
				for(NFCPoint element: vis){
		    		int res_id;
		    		switch(Integer.parseInt(element.getPosId())){
		    			case POICategories.INFO:
		    				res_id = R.drawable.nfc_blue;
		    				break;
			    		case POICategories.TURISM:
			    			res_id = R.drawable.nfc_orange;
			    			break;
			    		case POICategories.LEISURE:
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
	    		rows.add(new RowItem(bmp1, getString(R.string.default_visited)));
			}
			CustomListViewAdapter adapter1 = new CustomListViewAdapter(getApplicationContext(), R.layout.list, rows);
			check.setAdapter(adapter1);
			
		}
    }

	private NdefMessage createMessage(){
		//NdefRecord appRecord = NdefRecord.createApplicationRecord("es.quantum.unitenfc");
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
		byte[] payload = (prefs.getString("session", "")+";"+prefs.getString("username", "")+";"+prefs.getString("imageuri", "dummy_4")+" "+blue_mac).getBytes();
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
		// RELOAD INFO CARD
		final String friendfield = prefs.getString("friends", "");
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
	}

    //MOVE TO TOPOOS INTERFACE
	public boolean addFriend(String friend_data){
        String[] data = friend_data.split(";");
        String friend = data[1];
        boolean duplicated = TopoosInterface.isFriendDuplicated(friend, this);
		if(!duplicated){
            TopoosInterface.saveFriend(friend_data, this);
		}
        else {
            Toast.makeText(this, getString(R.string.add_already), Toast.LENGTH_LONG).show();
            showFriend(friend);
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
                    ((TextView)header).setText(getString(R.string.last_visited));
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