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
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.protocol.HttpContext;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

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

            String connection_url = "https://login.topoos.com/oauth/authtoken?response_type=token&client_id="+client_id+"&redirect_uri="+redirect_url+"&agent=mobile&scope=profile";
            String answer = "";

            HttpClient httpclient = new DefaultHttpClient();
            HttpResponse response = null;
            HttpContext ctx = new BasicHttpContext();
            try {

                response = httpclient.execute(new HttpGet(connection_url),ctx);
                Header[] allHeaders = response.getHeaders("Location");

                for(Header element:allHeaders){
                    HeaderElement[] allElements =element.getElements();
                    for(HeaderElement helement:allElements){
                        NameValuePair[] nv = helement.getParameters();
                        for(NameValuePair nvelem: nv){
                            Log.i(nvelem.getName(), nvelem.getValue());
                        }
                    }

                }

                StatusLine statusLine = response.getStatusLine();
                if(statusLine.getStatusCode() == HttpStatus.SC_MOVED_TEMPORARILY){
                    // GET redirection
                    // Extract authreq
                    String authreq = "";
                    String redirection_url = "";
                    response = httpclient.execute(new HttpGet(redirection_url));
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

                    if(statusLine.getStatusCode() == HttpStatus.SC_OK){
                        String post_url = "https://login.topoos.com/?agent=mobile&oauth=True&authreq="+authreq;
                        HttpPost socket = new HttpPost(post_url);
                        //add name value pairs
                        response = httpclient.execute(socket);
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