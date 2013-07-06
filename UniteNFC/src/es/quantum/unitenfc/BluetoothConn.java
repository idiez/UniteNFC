package es.quantum.unitenfc;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Method;
import java.util.UUID;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothSocket;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothDevice;
import android.os.AsyncTask;
import android.os.Handler;
import android.util.Log;

public class BluetoothConn {

	
	private boolean isServer;
	private BluetoothAdapter bluetooth;
	private BluetoothSocket socket;
	private String targetMAC;
	private UUID uuid = UUID.fromString("BA619000-AE57-11E2-9E96-0800200C9A66"); //generated with http://www.famkruithof.net/uuid/uuidgen
	private String msg;
	private OnSrvRcv mlis;

	public BluetoothConn(boolean isServer){
		this.isServer = isServer;
	}
	
	
	public void configureBluetooth() {
		bluetooth = BluetoothAdapter.getDefaultAdapter();
		bluetooth.enable();
		String name = "unitenfc";
		if(isServer){
			try {
				final BluetoothServerSocket btserver = bluetooth.listenUsingRfcommWithServiceRecord(name, uuid);
				AsyncTask<Integer, Void, BluetoothSocket> acceptThread =	new AsyncTask<Integer, Void, BluetoothSocket>() {
					@Override
					protected BluetoothSocket doInBackground(Integer... params) {
						try {
							socket = btserver.accept(params[0]*1000);
							return socket;
						} catch (IOException e) {
							e.printStackTrace();
						}
						return socket;
					}
					
					@Override
					protected void onPostExecute(BluetoothSocket result) {
						Thread bt = new Thread(new BluetoothSocketListener());
						bt.start();
					}
				};
				acceptThread.execute(5);	
			}catch (IOException e) {
				e.printStackTrace();
			}
		}
		else {
			BluetoothDevice bd = bluetooth.getRemoteDevice(targetMAC);
			try {
	            Log.d("pairDevice()", "Start Pairing...");
	            Method m = bd.getClass().getMethod("createBond", (Class[]) null);
	            m.invoke(bd, (Object[]) null);
	            Log.d("pairDevice()", "Pairing finished.");
	        } catch (Exception e) {
	            Log.e("pairDevice()", e.getMessage());
	        }
			
			try {
				socket = bd.createRfcommSocketToServiceRecord(uuid);
				socket.connect();
				OutputStream outStream;
					outStream = socket.getOutputStream();
					byte[] byteString = (msg + " ").getBytes();
					boolean connected = socket.isConnected();
					
					outStream.write(byteString);
					//socket.close();
					//bluetooth.disable();
					Log.d("BLUETOOTH_COMMS", "llego1");
				} catch (IOException e) {
				// TODO Auto-generated catch block
					Log.i("BLUETOOTH_COMMS", "llego2");
				e.printStackTrace();
			}
			
		}

	
				
	}
	
	public void setMAC(String mac){
		this.targetMAC = mac;
	}
	
	public void setMes(String mes){
		this.msg = mes;
	}
			
	public OnSrvRcv getMlis() {
		return mlis;
	}


	public void setMlis(OnSrvRcv mlis) {
		this.mlis = mlis;
	}

	private class BluetoothSocketListener implements Runnable{
		

		public void run() {
			int bufferSize = 1024;
			byte[] buffer = new byte[bufferSize];
			try {
				InputStream instream = socket.getInputStream();
				int bytesRead = -1;
				String message = "";
				while (message.isEmpty()) {
					message = "";
					bytesRead = instream.read(buffer);
					if (bytesRead != -1) {
						while ((bytesRead==bufferSize)&&(buffer[bufferSize-1] != 0)) {
							message = message + new String(buffer, 0, bytesRead);
							bytesRead = instream.read(buffer);
						}
						message = message + new String(buffer, 0, bytesRead - 1);
						//mlis.onSrvRcv(message);
						//socket.close();
						//bluetooth.disable();
						socket.getInputStream();
					}
				}
			} catch (IOException e) {
				Log.d("BLUETOOTH_COMMS", e.getMessage());
			}
		}
	}
	
    public interface OnSrvRcv {
        public void onSrvRcv(String msgrcv);
    }
	
}
