package es.quantum.unitenfc;

import android.app.Activity;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import org.apache.http.Header;
import org.apache.http.HeaderElement;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.NameValuePair;
import org.apache.http.StatusLine;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.params.HttpClientParams;
import org.apache.http.cookie.Cookie;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.client.RequestWrapper;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.params.HttpParams;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.protocol.HttpContext;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

import topoos.AccessTokenOAuth;

/**
 * Created by idiez on 10/07/13.
 */
public class OwnLogin extends Activity implements View.OnClickListener {

    Button login;

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.own_login);
        login = (Button) findViewById(R.id.logbttn);
        login.setOnClickListener(this);

    }

    @Override
    public void onClick(View view) {
        String username = ((TextView)findViewById(R.id.username)).getText().toString();
        String userpass = ((TextView)findViewById(R.id.userpass)).getText().toString();

        LoginFlow login = new LoginFlow();
        login.execute(username,userpass);
    }

    public class LoginFlow extends AsyncTask<String, Void, Boolean> {

        @Override
        protected Boolean doInBackground(String... strings) {

            String username = strings[0];
            String userpass = strings[1];
            String redirect_url= "https://login.topoos.com/oauth/dummy";
            String client_id = TopoosInterface.CLIENT_ID;

            String connection_url = "https://login.topoos.com/oauth/authtoken?response_type=token&client_id="+client_id+"&redirect_uri="+redirect_url+"&agent=mobile";
            String answer = "";

            HttpClient httpclient = new DefaultHttpClient();
            HttpResponse response = null;
            HttpContext ctx = new BasicHttpContext();
            try {
                HttpGet first = new HttpGet(connection_url);
                response = httpclient.execute(first,ctx);

                StatusLine statusLine = response.getStatusLine();
                if(statusLine.getStatusCode() == HttpStatus.SC_OK){
                    // GET redirection
                    RequestWrapper hm = (RequestWrapper) ctx.getAttribute("http.request");
                    String referer = hm.getURI().toString();
                    // Extract authreq
                    String authreq = ((referer).replaceAll("&agent=mobile", "")).substring(2).replaceAll("oauth=True&authreq=","");
                    String post_url = "https://login.topoos.com/?agent=mobile&oauth=True&authreq="+authreq;
                    HttpPost socket = new HttpPost(post_url);
                    //add name value pairs
                    List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>(4);
                    nameValuePairs.add(new BasicNameValuePair("LogOnUserName", username));
                    nameValuePairs.add(new BasicNameValuePair("LogOnPassword", userpass));
                    nameValuePairs.add(new BasicNameValuePair("oauth", "True"));
                    nameValuePairs.add(new BasicNameValuePair("authreq", authreq));
                    socket.setEntity(new UrlEncodedFormEntity(nameValuePairs));
                    socket.addHeader("Referer","https://login.topoos.com"+referer);
                    HttpParams params = httpclient.getParams();
                    HttpClientParams.setRedirecting(params, false);

                    response = httpclient.execute(socket,ctx);
                    statusLine = response.getStatusLine();
                /*
                ByteArrayOutputStream out = new ByteArrayOutputStream();
                try {
                    response.getEntity().writeTo(out);
                    out.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                answer = out.toString();
*/

                    if(statusLine.getStatusCode() == HttpStatus.SC_MOVED_TEMPORARILY){
                        String cookie = response.getHeaders("Set-Cookie")[0].getValue();
                        HttpGet second = new HttpGet("https://login.topoos.com/OAuth/AccreditationConfirm?showSettings=False&authreq="+authreq);
                        second.setHeader("Cookie",cookie);
                        HttpClientParams.setRedirecting(params, true);
                        response = httpclient.execute(second,ctx);
                        hm = (RequestWrapper) ctx.getAttribute("http.request");
                        referer = hm.getURI().toString();
                        statusLine = response.getStatusLine();
                        if(referer.contains("token")){

                            String plain_token = referer.substring(26,62);
                            long expires_in = Long.valueOf(referer.substring(92,100));
                            AccessTokenOAuth token = new AccessTokenOAuth(plain_token);
                            token.save_Token(getApplicationContext());
                            if(token.isValid()) {
                                return true;
                            }
                            else {
                                return false;
                            }
                        }
                    }
                }
                else {
                    return false;
                }


            } catch (ClientProtocolException e) {
                e.printStackTrace();
                return false;
            } catch (IOException e) {
                e.printStackTrace();
                return false;
            }
            catch (RuntimeException e){
                e.printStackTrace();
                return false;
            }
            return true;
        }

        @Override
        protected void onPostExecute(Boolean abolean){

        }
    }
}