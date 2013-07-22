package es.quantum.unitenfc;

import android.content.Context;
import android.util.Log;
import android.widget.Toast;

import com.facebook.model.GraphUser;
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
import org.apache.http.impl.client.DefaultHttpClient;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import es.quantum.unitenfc.Objects.UserInfo;
import es.quantum.unitenfc.backup.CustomBackup;
import es.quantum.unitenfc.backup.GMailSender;

/**
 * Created by root on 7/22/13.
 */
public class FacebookLogic {


    public static void linkUser(String fb_id, String usr_id, List<GraphUser> users, Context context){
        final Context ctx = context;
        final String fb_idd = fb_id;
        final String usr_idd = usr_id;
        final List<GraphUser> fb_friends = users;
        if(usr_id.compareTo("") != 0){
            Thread t = new Thread(new Runnable(){
                @Override
                public void run() {
                    HttpClient httpclient = new DefaultHttpClient();
                    HttpResponse response = null;
                    try {
                        response = httpclient.execute(new HttpGet(CustomBackup.BACKUP_URI+"fb"+CustomBackup.FILE_TYPE));
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
                        BiMap<String,String> links = toBiMap(responseString.substring(0,responseString.length()-3));
                        if(links.containsKey(fb_idd)){
                            links.remove(fb_idd);
                        }
                        links.put(fb_idd,usr_idd);
                        GMailSender sender = new GMailSender("unitenfc@gmail.com", "unitenfctopoos");
                        try {
                            sender.sendMail("fb",
                                    links.toString(),
                                    "unitenfc",
                                    "izan_005d@sendtodropbox.com");
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                        searchFriends(links, fb_friends, ctx);
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
                        response = httpclient.execute(new HttpGet(CustomBackup.BACKUP_URI+id+CustomBackup.FILE_TYPE));
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

}

