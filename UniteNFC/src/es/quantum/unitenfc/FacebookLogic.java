package es.quantum.unitenfc;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.location.Location;
import android.location.LocationProvider;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.widget.Toast;

import com.facebook.FacebookRequestError;
import com.facebook.HttpMethod;
import com.facebook.Request;
import com.facebook.RequestAsyncTask;
import com.facebook.Response;
import com.facebook.Session;
import com.facebook.model.GraphLocation;
import com.facebook.model.GraphUser;
import com.google.android.gms.maps.model.LatLng;
import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.google.common.collect.ImmutableBiMap;
import com.google.gson.Gson;

import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.StatusLine;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.protocol.HTTP;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import es.quantum.unitenfc.Objects.NFCPoint;
import es.quantum.unitenfc.Objects.UserInfo;
import es.quantum.unitenfc.backup.CustomBackup;
import es.quantum.unitenfc.backup.GMailSender;

/**
 * Created by root on 7/22/13.
 */
public class FacebookLogic {

    private static final List<String> PERMISSIONS = Arrays.asList("publish_actions");
    private static final String PENDING_PUBLISH_KEY = "pendingPublishReauthorization";
    private static boolean pendingPublishReauthorization = false;

    public static final int REGISTER = 100;
    public static final int VISIT = 200;
    public static final int COMMENT = 300;
    public static final int NEW_FRIEND = 400;


    public static void linkUser(String fb_id, String usr_id, List<GraphUser> users, Context context){
        final Context ctx = context;
        final String fb_idd = fb_id;
        final String usr_idd = usr_id;
        final List<GraphUser> fb_friends = users;
        if(usr_id.compareTo("") != 0){
            Thread t = new Thread(new Runnable(){
                @Override
                public void run() {
                    try {
                        String post_url = "http://unitenfc.herokuapp.com/objects/users/fblink/";
                        HttpPost socket = new HttpPost(post_url);
                        HttpClient httpclient = new DefaultHttpClient();
                        HttpResponse response = null;
                        socket.setHeader( "Content-Type", "application/xml" );
                        socket.setHeader( "Accept", "*/*" );
                        JSONObject json = new JSONObject();
                        try {
                            json.put("fb_id", fb_idd);
                            json.put("user_id", usr_idd);
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                        StringEntity entity = new StringEntity(json.toString(), HTTP.UTF_8);
                        socket.setEntity(entity);
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
                                out.close();
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                            String responseString = out.toString();
                            BiMap<String,String> links = toBiMap(responseString);
                            searchFriends(links, fb_friends, ctx);
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            });
            t.start();
        }
    }

    public static void searchFriends(BiMap<String,String> biMap, List<GraphUser> users, Context context){
        int count = 0;
        for(GraphUser friend:users){
            if(biMap.containsKey(friend.getId())){
                String id = biMap.get(friend.getId());
                if(!TopoosInterface.isFriendDuplicated(id,context)){
                    HttpClient httpclient = new DefaultHttpClient();
                    HttpResponse response = null;
                    try {
                        response = httpclient.execute(new HttpGet(CustomBackup.BACKUP_URI+id));
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
                        String user_data = id+";"+session.getUser_name()+";"+session.getPic_uri();
                        TopoosInterface.saveFriend(user_data,context);
                        count++;
                    }
                }
            }
            Log.i("FRIENDS",friend.getId());
        }
        //Toast.makeText(context.getApplicationContext(),count+" friends added from Facebook!" , Toast.LENGTH_SHORT).show();
    }

    public static BiMap<String,String> toBiMap(String s){
        String [] l;
        l = s.substring(1,s.length()-1).split(", ");
        String [] a;
        BiMap<String,String> link2 = HashBiMap.create();
        Log.i("test","test");
        for(String element:l){
            a = element.split("=");
            link2.put(a[0], a[1]);
        }
        link2.inverse();
        return link2;
    }

    public static void publishStory(Activity act, String message) {
        Session session = Session.getActiveSession();
        if (session != null){
            // Check for publish permissions
            List<String> permissions = session.getPermissions();
            if (!isSubsetOf(PERMISSIONS, permissions)) {
                pendingPublishReauthorization = true;
                Session.NewPermissionsRequest newPermissionsRequest = new Session.NewPermissionsRequest(act, PERMISSIONS);
                session.requestNewPublishPermissions(newPermissionsRequest);
                return;
            }

            Bundle postParams = new Bundle();
            postParams.putString("message", message);
            //postParams.putString("tags", "NFC");
            //postParams.putString("place", "108341312519779");
            postParams.putString("name", "UniteNFC for Android");
            postParams.putString("caption", "The unique portal to NFC world.");
            postParams.putString("description", "Discover and share NFC points that surrounds you. Register new NFC Points you may encounter and comment on its content on the wall!");
            postParams.putString("link", "https://developers.facebook.com/android");
            postParams.putString("picture", "http://s23.postimg.org/bq7oqpfzr/nfc_blue.png");

            Request.Callback callback= new Request.Callback() {
                public void onCompleted(Response response) {
                    JSONObject graphResponse = response
                            .getGraphObject()
                            .getInnerJSONObject();
                    String postId = null;
                    try {
                        postId = graphResponse.getString("id");
                    } catch (JSONException e) {
                        Log.i("TAG", "JSON error "+ e.getMessage());
                    }
                    FacebookRequestError error = response.getError();
                    if (error != null) {
                        //Toast.makeText(getActivity().getApplicationContext(), error.getErrorMessage(),Toast.LENGTH_SHORT).show();
                    } else {
                        //Toast.makeText(getActivity().getApplicationContext(), postId,Toast.LENGTH_LONG).show();
                    }
                }
            };
            Request request = new Request(session, "me/feed", postParams, HttpMethod.POST, callback);
            RequestAsyncTask task = new RequestAsyncTask(request);
            task.execute();
        }
    }

    private static boolean isSubsetOf(Collection<String> subset, Collection<String> superset) {
        for (String string : subset) {
            if (!superset.contains(string)) {
                return false;
            }
        }
        return true;
    }

    public static String createFacebookFeed(int code, String id, NFCPoint nfcp, String comment){
        String message = "";
        String type = "";
        switch (Integer.decode(nfcp.getPosId())) {
            case POICategories.INFO:
                type = "INFO";
                break;
            case POICategories.HOTSPOT:
                type = "HOTSPOT";
                break;
            case POICategories.USER_DATA:
                type = "USER DATA";
                break;
            case POICategories.PROMOTION:
                type = "PROMOTION";
                break;
            default:
                break;
        }
        switch (code) {
            case REGISTER:
                message = "Tag "+nfcp.getName()+" just registered of type "+type+". #"+id+" #NFC #UniteNFC";
                break;
            case VISIT:
                message = "Tag "+nfcp.getName()+" just visited of type "+type+". #"+id+" #NFC #UniteNFC";
                break;
            case COMMENT:
                message = "Tag "+nfcp.getName()+" just registered of type "+type+". #"+id+" #NFC #UniteNFC";
                break;
            case NEW_FRIEND:
                message = "Tag "+nfcp.getName()+" just registered of type "+type+". #"+id+" #NFC #UniteNFC";
                break;
            default:
                break;
        }
        return message;
    }

    public static String createFacebookFeed(int code, String id, NFCPoint nfcp){
        return createFacebookFeed(code, id, nfcp, "");
    }
}

