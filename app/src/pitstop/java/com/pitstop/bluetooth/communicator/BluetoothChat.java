package com.pitstop.bluetooth.communicator;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.os.Handler;
import android.util.Log;

import com.castel.obd.util.Utils;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.UUID;

public class BluetoothChat {
	private final UUID MY_UUID = UUID
			.fromString("00001101-0000-1000-8000-00805F9B34FB");

	private static final String TAG = BluetoothChat.class.getSimpleName();

	public ConnectThread connectThread;
	public ConnectedThread connectedThread;

	public Handler mHandler;

	public BluetoothChat(Handler handler) {
		mHandler = handler;
	}

	public synchronized void connectBluetooth(BluetoothDevice device) {
		if (isConnecting()) return;
		connectThread = new ConnectThread(device);
		connectThread.start();
	}

	public void closeConnect() {
		Log.w(TAG, "Closing connection threads");
		if (null != connectedThread) {
			connectedThread.cancel();
			connectedThread = null;
		}
		if (null != connectThread) {
			connectThread.cancel();
			connectThread = null;
		}
	}

	public boolean isConnecting(){
		return connectThread != null && connectThread.isAlive();
	}

	public boolean isConnected(){
		return connectedThread != null && connectedThread.isAlive();
	}

	// establish connection with socket
	private class ConnectThread extends Thread {
		private final BluetoothSocket mmSocket;
		private BluetoothDevice mmDevice;

		@SuppressLint("NewApi")
		public ConnectThread(BluetoothDevice device) {
			BluetoothSocket temp = null;
			mmDevice = device;
			
			try {

				temp = mmDevice.createInsecureRfcommSocketToServiceRecord(MY_UUID);
			} catch (IOException e1) {
				e1.printStackTrace();
			}
			
			mmSocket = temp;
		}

		@Override
		public void run() {
			Log.i(TAG, "Creating connect thread");

			mHandler.sendEmptyMessage(IBluetoothCommunicator.CANCEL_DISCOVERY);
			try {
				if(mmSocket!=null) {
					Log.i(TAG, "Connecting to socket");

					if(!mmSocket.isConnected()) {
						mmSocket.connect();
					}

					mHandler.sendMessage(mHandler.obtainMessage(
							IBluetoothCommunicator.BLUETOOTH_CONNECT_SUCCESS,
							mmDevice.getAddress()));

					connectedThread = new ConnectedThread(mmSocket);
					connectedThread.start();
				}

			} catch (IOException connectException) {
				if(mmSocket.isConnected()) {
					Log.e(TAG, "Already connected to socket");
				} else {
					connectException.printStackTrace();

					try {
						Log.i(TAG, "Couldn't connect to socket");
						mHandler.sendEmptyMessage(IBluetoothCommunicator.BLUETOOTH_CONNECT_FAIL);
						mmSocket.close();
					} catch (IOException e2) {
						e2.printStackTrace();
					}
				}
			}
		}

		public void cancel() {
			try {
				if (null != mmSocket) {
					mmSocket.close();
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	// handle read and write from device
	public class ConnectedThread extends Thread {
		private BluetoothSocket mmSocket;
		private InputStream mmInStream;
		private OutputStream mmOutStream;

		public ConnectedThread(BluetoothSocket socket) {
			Log.i(TAG, "Creating connected thread");

			mmSocket = socket;
			InputStream tmpIn = null;
			OutputStream tmpOut = null;

			try {
				tmpIn = socket.getInputStream();
				tmpOut = socket.getOutputStream();
			} catch (IOException e) {
				e.printStackTrace();
			}
			mmInStream = tmpIn;
			mmOutStream = tmpOut;
		}

		@Override
		public void run() {
			Log.w(TAG, "Running connected thread");
			byte[] buffer = new byte[1024];
			int count;

			while (mmInStream != null) {
				try {
					count = mmInStream.read(buffer);

					if (-1 == count) {
						Log.i(TAG, "read exception");
						closeConnect();
						mHandler.sendEmptyMessage(IBluetoothCommunicator.BLUETOOTH_CONNECT_EXCEPTION);
						break;
					}

					byte[] data = new byte[count];
					System.arraycopy(buffer, 0, data, 0, count);

					Log.d("Reading Raw Data", Utils.bytesToHexString(data));

					mHandler.sendMessage(mHandler.obtainMessage(
							IBluetoothCommunicator.BLUETOOTH_READ_DATA, data));
				} catch (IOException e) {
					Log.i(TAG, "read exception");
					closeConnect();
					mHandler.sendEmptyMessage(IBluetoothCommunicator.BLUETOOTH_CONNECT_EXCEPTION);
					break;
				}
			}
		}

		public void write(byte[] bytes) {
			Log.i("Writing Raw Data", Utils.bytesToHexString(bytes));
			try {
				if (null != mmOutStream) {
					mmOutStream.write(bytes);
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

		public void cancel() {
			try {
				if (null != mmOutStream) {
					mmOutStream.close();
					mmOutStream = null;
				}
				if (null != mmInStream) {
					mmInStream.close();
					mmInStream = null;
				}
				if ( mmSocket != null) {
					mmSocket.close();
					mmSocket = null;
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
}
