package com.castel.obd.bluetooth;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.widget.Toast;

import com.castel.obd.OBD;
import com.castel.obd.data.OBDInfoSP;
import com.castel.obd.info.BasePackageInfo;
import com.castel.obd.info.DataPackageInfo;
import com.castel.obd.info.LoginPackageInfo;
import com.castel.obd.info.ParameterPackageInfo;
import com.castel.obd.info.ResponsePackageInfo;
import com.castel.obd.info.SendPackageInfo;
import com.castel.obd.util.JsonUtil;
import com.castel.obd.util.LogUtil;
import com.castel.obd.util.Utils;

import java.util.ArrayList;
import java.util.List;

@Deprecated
public class BluetoothManage {
	private final String BT_NAME = "IDD-212";// �������

	public final static int BLUETOOTH_CONNECT_SUCCESS = 0;// �������ӳɹ�
	public final static int BLUETOOTH_CONNECT_FAIL = 1;// ��������ʧ��
	public final static int BLUETOOTH_CONNECT_EXCEPTION = 2;// ���������쳣
	public final static int BLUETOOTH_READ_DATA = 4;// ����������쳣
	public final static int CANCEL_DISCOVERY = 5;// ȡ�������豸����

	public final static int CONNECTED = 0;// ����������
	public final static int DISCONNECTED = 1;// ����δ����
	public final static int CONNECTING = 2;// ��������������
	private int btConnectionState = DISCONNECTED;

	private static Context mContext;
	private static BluetoothManage mInstance;
	private BluetoothChat mBluetoothChat;
	private BluetoothAdapter mBluetoothAdapter;

	private BluetoothDataListener dataListener;

	public List<DataPackageInfo> dataPackages;

	private boolean isMacAddress = false;// �״����������Ƿ��п��Ե�MAC��ַ
	private boolean isParse = false;
	private List<String> dataLists = new ArrayList<String>();

	private String DTAG = "DEBUG_BLUETOOTH";

	public BluetoothManage() {
		Log.i(DTAG,"Initializing BluetoothManage");
		dataPackages = new ArrayList<DataPackageInfo>();
		mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
		mBluetoothChat = new BluetoothChat(mHandler);
		registerBluetoothReceiver();
		initOBD();
		mHandler.postDelayed(runnable, 500);
	}

	public static BluetoothManage getInstance(Context context) {
		Log.i("DEBUG_BLUETOOTH","Getting BluetoothManage instance");
		mContext = context;
		if (null == mInstance) {
			mInstance = new BluetoothManage();
		}
		return mInstance;
	}

	Runnable runnable = new Runnable() {
		@Override
		public void run() {
			if (!isParse && dataLists.size() > 0) {
				receiveDataAndParse(dataLists.get(0));
				dataLists.remove(0);
			}
			mHandler.postDelayed(this, 500);
		}
	};

	private Handler mHandler = new Handler() {
		@Override
		public void handleMessage(Message msg) {
			Log.i(DTAG, "BluetoothManage message handler");
			super.handleMessage(msg);
			switch (msg.what) {
				case CANCEL_DISCOVERY:
				{
					Log.i(DTAG,"CANCEL_DISCOVERY - BluetoothManage");
					if (mBluetoothAdapter.isDiscovering()) {
						mBluetoothAdapter.cancelDiscovery();
					}
					break;
				}
				case BLUETOOTH_CONNECT_SUCCESS:
				{
					Log.i(DTAG,"Bluetooth connect success - BluetoothManage");
					btConnectionState = CONNECTED;
					Log.i(DTAG, "Saving Mac Address - BluetoothManage");
					OBDInfoSP.saveMacAddress(mContext, (String) msg.obj);
					Log.i(DTAG, "setting dataListener - getting bluetooth state - BluetoothManage");
					dataListener.getBluetoothState(btConnectionState);
					LogUtil.i("Bluetooth state:CONNECTED");
					break;
				}
				case BLUETOOTH_CONNECT_FAIL:
				{
					btConnectionState = DISCONNECTED;
					LogUtil.i("Bluetooth state:DISCONNECTED");
					Log.i(DTAG, "Bluetooth connection failed - BluetoothManage");
					Log.i(DTAG, "Bluetooth connection failed - BluetoothManage: Bool - Value: "+isMacAddress);
					if (isMacAddress) {
						if (mBluetoothAdapter.isDiscovering()) {
							mBluetoothAdapter.cancelDiscovery();
						}
						Log.i(DTAG,"Retry connection");
						mBluetoothAdapter.startDiscovery();
					} else {
	//					Toast.makeText(mContext, R.string.bluetooth_connect_fail,
	//							Toast.LENGTH_LONG).show();
						Log.i(DTAG, "Sending out bluetooth state on dataListener");
						dataListener.getBluetoothState(btConnectionState);
					}
					break;
				}
				case BLUETOOTH_CONNECT_EXCEPTION:
				{
					btConnectionState = DISCONNECTED;
					LogUtil.i("Bluetooth state:DISCONNECTED");
					Log.i(DTAG,"Bluetooth connection exception - calling get bluetooth state on dListener");
					dataListener.getBluetoothState(btConnectionState);
					break;
				}
				case BLUETOOTH_READ_DATA:
				{
					if (!Utils.isEmpty(Utils.bytesToHexString((byte[]) msg.obj))) {
						Log.i(DTAG, "Bluetooth read data... - BluetoothManage");
						dataLists.add(Utils.bytesToHexString((byte[]) msg.obj));
					}
					break;
				}
			}
		}
	};

	private void registerBluetoothReceiver() {
		Log.i(DTAG,"Registering bluetooth intent receivers");
		IntentFilter filter = new IntentFilter();
		filter.addAction(BluetoothDevice.ACTION_FOUND);
		filter.addAction(BluetoothDevice.ACTION_ACL_CONNECTED);
		filter.addAction(BluetoothDevice.ACTION_BOND_STATE_CHANGED);
		filter.addAction(BluetoothDevice.ACTION_ACL_DISCONNECTED);
		filter.addAction(BluetoothAdapter.ACTION_SCAN_MODE_CHANGED);
		filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_STARTED);
		filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
		mContext.registerReceiver(mReceiver, filter);
	}


	private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			Log.i(DTAG, "BReceiver onReceive - BluetoothManage");

			String action = intent.getAction();
			// �����豸
			if (BluetoothDevice.ACTION_FOUND.equals(action)) {
				Log.i(DTAG,"A device found - BluetoothManage");
				BluetoothDevice device = intent
						.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
				LogUtil.i(device.getName() + device.getAddress());
				if (device.getName()!=null&&device.getName().contains(BT_NAME)) {
					Log.i(DTAG,"OBD device found... Connect to IDD-212 - BluetoothManage");
					mBluetoothChat.connectBluetooth(device);
					Log.i(DTAG,"Connecting to device - BluetoothManage");
					Toast.makeText(mContext, "Connecting to Device", Toast.LENGTH_SHORT).show();
				}
			} else if (BluetoothDevice.ACTION_ACL_CONNECTED.equals(action)) { // �������ӳɹ�
				Log.i(DTAG,"Phone is connected to a remote device - BluetoothManage");
				//LogUtil.i("CONNECTED");
				// Phone is not necessarily connected to device
				/*btConnectionState = CONNECTED;
				LogUtil.i("Bluetooth state:CONNECTED");
				dataListener.getBluetoothState(btConnectionState);*/
				BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);

				if(device.getName()!=null && device.getName().contains(BT_NAME)) {
					Log.i(DTAG, "Connected to device: " + device.getName());
					btConnectionState = CONNECTED;
					LogUtil.i("Bluetooth state:CONNECTED");
					dataListener.getBluetoothState(btConnectionState);
				}
			} else if (BluetoothDevice.ACTION_BOND_STATE_CHANGED.equals(action)) { // �������ӳɹ�
				Log.i(DTAG,"Pairing state changed - BluetoothManage");
				BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
				if(device.getBondState()==BluetoothDevice.BOND_BONDED &&
						(device.getName().contains(BT_NAME))) {
					Log.i(DTAG,"Connected to a PAIRED device - BluetoothManage");
					LogUtil.i("CONNECTED");
					btConnectionState = CONNECTED;
					LogUtil.i("Bluetooth state:CONNECTED");
					dataListener.getBluetoothState(btConnectionState);
				}
			} else if (BluetoothDevice.ACTION_ACL_DISCONNECTED.equals(action)) { // �������ӶϿ�

				Log.i(DTAG, "Disconnection from a remote device - BluetoothManage");

				BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);

				if(device.getName()!= null && device.getName().contains(BT_NAME)) {
					btConnectionState = DISCONNECTED;
					LogUtil.i("Bluetooth state:DISCONNECTED");
					dataListener.getBluetoothState(btConnectionState);
				}

			} else if (BluetoothAdapter.ACTION_DISCOVERY_STARTED.equals(action)) {
				Log.i(DTAG,"Bluetooth state:ACTION_DISCOVERY_STARTED - BluetoothManage");
				LogUtil.i("Bluetooth state:ACTION_DISCOVERY_STARTED");
			} else if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)) {
				Log.i(DTAG,"Bluetooth state:ACTION_DISCOVERY_FINISHED - BluetoothManage");
				LogUtil.i("Bluetooth state:ACTION_DISCOVERY_FINISHED");
				if (btConnectionState != CONNECTED) {
					btConnectionState = DISCONNECTED;
					Log.i(DTAG,"Not connected - setting get bluetooth state on dListeners");
					dataListener.getBluetoothState(btConnectionState);
				}
			}else if (BluetoothAdapter.ACTION_SCAN_MODE_CHANGED.equals(action)){
				if(mBluetoothAdapter.isEnabled())
					connectBluetooth();
				Log.i(DTAG,"Bluetooth state:SCAN_MODE_CHNAGED- setting dListeners btState");
				dataListener.getBluetoothState(btConnectionState);
			}
		}
	};

	/**
	 * ��OBD�豸����
	 */
	// Connect to OBD device
	public void connectBluetooth() {

		if (btConnectionState == CONNECTED) {
			Log.i(DTAG,"Bluetooth is connected - BluetoothManage");
			return;
		}

		LogUtil.i("Bluetooth state:CONNECTING");
		Log.i(DTAG,"Connecting to bluetooth - BluetoothManage");
		btConnectionState = CONNECTING;
		mBluetoothChat.closeConnect();

		// ֱ�Ӵ�����
		if (!mBluetoothAdapter.isEnabled()) {
			LogUtil.i("BluetoothAdapter.enable()");
			Log.i(DTAG,"Bluetooth not enabled");
			mBluetoothAdapter.enable();
		}

		Log.i(DTAG,"Getting saved macAddress - BluetoothManage");
		String macAddress = OBDInfoSP.getMacAddress(mContext);
//		 macAddress = "8C:DE:52:71:F7:71";
//		macAddress = "8C:DE:52:19:DB:86";
//		macAddress = "8C:DE:52:22:C8:B5";
		if (!"".equals(macAddress)) {
			isMacAddress = true;
			Log.i(DTAG,"Using macAddress "+macAddress+" to connect to device - BluetoothManage");
			BluetoothDevice device = mBluetoothAdapter
					.getRemoteDevice(macAddress);
			mBluetoothChat.connectBluetooth(device);// ֱ����MAC��ַ��������
		} else {
			LogUtil.i("startDiscovery()");
			Log.i(DTAG,"Starting discovery - BluetoothManage");
			if (mBluetoothAdapter.isDiscovering()) {
				mBluetoothAdapter.cancelDiscovery();
			}
			mBluetoothAdapter.startDiscovery();// ���������豸
		}
	}

	/**
	 * �ر���������
	 */
	public void close() {
		Log.i(DTAG,"Closing connection - BluetoothManage");
		btConnectionState = DISCONNECTED;
		mContext.unregisterReceiver(mReceiver);
		mBluetoothChat.closeConnect();
		mHandler.removeCallbacks(runnable);
	}

	/**
	 * ��������״̬
	 * 
	 * @return
	 */
	public int getState() {
		Log.i(DTAG, "Getting bluetooth state - BluetoothManage");
		return btConnectionState;
	}

	/**
	 * ��ʼ��OBD�豸���������豸ID����ݰ�ID���ʼ����������������ʼ��
	 */
	public void initOBD() {
		Log.i(DTAG,"Initialize obd func - BluetoothManage");
		String deviceId = OBDInfoSP.getDeviceId(mContext);
		String dataNum = OBDInfoSP.getDataNum(mContext);
		if (!Utils.isEmpty(deviceId) && !Utils.isEmpty(dataNum)) {
			LogUtil.i("deviceId:" + deviceId + "dataNum"
					+ OBDInfoSP.getDataNum(mContext));
			// ��ʼ�����������̬��
			Log.i(DTAG,"Initializing obd module - BluetoothManage");
			//OBD.init(deviceId, dataNum);
		}
	}

	/**
	 * ����������
	 * 
	 * @param type
	 */
	public void obdSetCtrl(int type) {
		if (btConnectionState != CONNECTED) {
//			Toast.makeText(mContext, R.string.bluetooth_disconnected,
//					Toast.LENGTH_LONG).show();
			return;
		}


		String result = OBD.setCtrl(type);
		sendCommand(result);
	}

	/**
	 * ���������
	 */
	public void obdSetMonitor(int type, String valueList) {
		if (btConnectionState != CONNECTED) {
			Log.i(DTAG,"obdSetMonitor bluetooth not connected - BluetoothManage");
//			Toast.makeText(mContext, R.string.bluetooth_disconnected,
//					Toast.LENGTH_LONG).show();
			return;
		}


		String result = OBD.setMonitor(type, valueList);
		sendCommand(result);
	}

	/**
	 * OBD��������
	 * 
	 * @param tlvTagList
	 *            ��������
	 * @param valueList
	 *            ����ֵ
	 */
	public void obdSetParameter(String tlvTagList, String valueList) {
		if (btConnectionState != CONNECTED) {
			Log.i(DTAG,"obd set parameter - bluetooth not connected");
//			Toast.makeText(mContext, R.string.bluetooth_disconnected,
//					Toast.LENGTH_LONG).show();
			return;
		}

		LogUtil.i("tlvTagList: " + tlvTagList);
		LogUtil.i("tlvTagList: " + valueList);

		Log.i(DTAG,"obd set parameter - BluetoothManage");
		String result = OBD.setParameter(tlvTagList, valueList);
		sendCommand(result);
	}

	/**
	 * ��ȡOBD����
	 * 
	 * @param tlvTag
	 *            ��������
	 */
	public void obdGetParameter(String tlvTag) {
		Log.i(DTAG,"Getting obd parameter - BluetoothManage");
		if (btConnectionState != CONNECTED) {
			Log.i(DTAG,"Getting obd parameter (bluetooth state not connected) - BluetoothManage");
//			Toast.makeText(mContext, R.string.bluetooth_disconnected,
//					Toast.LENGTH_LONG).show();
			return;
		}

		Log.i(DTAG, "Getting data from obd device - BluetoothManage");
		String result = OBD.getParameter(tlvTag);
		sendCommand(result);
	}

	/**
	 * ����д���
	 * 
	 * @param str
	 */
	public void sendCommand(String str) {
		LogUtil.i(str);
		if (Utils.isEmpty(str)) {
			return;
		}

		SendPackageInfo sendPackageInfo = JsonUtil.json2object(str,
				SendPackageInfo.class);
		if (null == sendPackageInfo) {
			return;
		}

		LogUtil.i("result:" + sendPackageInfo.result);
		LogUtil.i("instruction:" + sendPackageInfo.instruction);

		if (sendPackageInfo.result != 0
				|| Utils.isEmpty(sendPackageInfo.instruction)) {
			return;
		}

		byte[] bytes = Utils.hexStringToBytes(sendPackageInfo.instruction);

		if (btConnectionState == CONNECTED && null != mBluetoothChat.connectedThread) {
			mBluetoothChat.connectedThread.write(bytes);
		}
	}

	/**
	 * ����д���
	 * 
	 * @param msg
	 */
	public void sendCommandPassive(String msg) {
		if (Utils.isEmpty(msg)) {
			return;
		}

		byte[] bytes = Utils.hexStringToBytes(msg);

		if (btConnectionState == CONNECTED && null != mBluetoothChat.connectedThread) {
			mBluetoothChat.connectedThread.write(bytes);
		}
	}

	/**
	 * �������������
	 * 
	 * @param data
	 */
	public void receiveDataAndParse(String data) {
		Log.i(DTAG,"Calling receive data and parse - BluetoothManage");
		LogUtil.i("isParse:" + isParse);
		isParse = true;
		String info = OBD.getIOData(data);
		LogUtil.i("isParse:" + isParse);
		isParse = false;

		LogUtil.i(info);

		info = info.replace("obdData\":]","obdData\":[]");
		String[] infos = info.split("&");
		LogUtil.i("length:" + infos.length);
		for (int i = 0; i < infos.length; i++) {
			BasePackageInfo baseInfo = JsonUtil.json2object(infos[i],
					BasePackageInfo.class);
			if (null != baseInfo) {
				packageType(infos[i], baseInfo.result);
			}
		}

	}

	/**
	 * ������ͽ���
	 * 
	 * @param info
	 * @param result
	 */

	public void packageType(String info, int result) {
		Log.i(DTAG,"Checking package type - BluetoothManage");
		if (0 == result) {
			Log.i(DTAG,"Receiving result 0 - BluetoothManage");
			obdLoginPackageParse(info);
		} else if (2 == result) {
			Log.i(DTAG,"Receiving result 2 - BluetoothManage");
			obdResponsePackageParse(info);
		} else if (3 == result) {
			Log.i(DTAG,"Receiving result 3 - BluetoothManage");
			obdParameterPackageParse(info);
		} else if (4 == result || 5 == result || 6 == result) {
			Log.i(DTAG,"Receiving result 4 or 5 or 6 - BluetoothManage");
			obdIODataPackageParse(info);
		}
	}

	/**
	 * ��½���˳���
	 * 
	 * @param info
	 */
	private void obdLoginPackageParse(String info) {
		Log.i(DTAG,"Obd login package parse - BluetoothManage");
		LoginPackageInfo loginPackageInfo = JsonUtil.json2object(info,
				LoginPackageInfo.class);

		if (null == loginPackageInfo) {
			return;
		}

		if ("0".equals(loginPackageInfo.flag)) {
			// �˳���
			LogUtil.i("�˳���");
			dataListener.deviceLogin(loginPackageInfo);
		} else if ("1".equals(loginPackageInfo.flag)) {
			// ��½��
			LogUtil.i("��½��");

			sendCommandPassive(loginPackageInfo.instruction);
			dataListener.deviceLogin(loginPackageInfo);
		}
	}

	/**
	 * ���ơ����ò�����ն˻�Ӧ
	 * 
	 * @param info
	 */
	private static String OBDTAG = "DEBUG_OBD_RTC";
	private void obdResponsePackageParse(String info) {
		Log.i(DTAG,"calling obd response package - BluetoothManage");
		ResponsePackageInfo responsePackageInfo = JsonUtil.json2object(info,
				ResponsePackageInfo.class);

		if (null == responsePackageInfo) {
			Log.i(DTAG, "obd response package setting ctrl and parameter response dataListeners (null data)");
			dataListener.setCtrlResponse(null);
			dataListener.setParameterResponse(null);
		} else {
			if ("0".equals(responsePackageInfo.flag)) {
				Log.i(DTAG,"obd response package set ctrl resp dataListener - BluetoothManage");
				dataListener.setCtrlResponse(responsePackageInfo);
			} else if ("1".equals(responsePackageInfo.flag)) {
				Log.i(DTAG,"obd response package set parameter resp dataListener - BluetoothManage");
				Log.i(OBDTAG,"result: "+responsePackageInfo.result);
				Log.i(OBDTAG,"value: "+responsePackageInfo.value);
				Log.i(OBDTAG, "type: " + responsePackageInfo.type);

				dataListener.setParameterResponse(responsePackageInfo);
			}
		}
	}

	/**
	 * �ն������ϴ��޸Ĺ�Ĳ��������ѯ��Ӧ
	 * 
	 * @param info
	 */

	private void obdParameterPackageParse(String info) {
		ParameterPackageInfo parameterPackageInfo = JsonUtil.json2object(info,
				ParameterPackageInfo.class);
		Log.i(DTAG,"getting parameterData on dataListener - BluetoothManage");
		Log.i(OBDTAG,"result: "+parameterPackageInfo.result);
		Log.i(OBDTAG,"value: "+parameterPackageInfo.value);
		dataListener.getParameterData(parameterPackageInfo);
	}

	/**
	 * �г���ݡ�ʵʱ������ݡ�������
	 * 
	 * @param info
	 */
	public void obdIODataPackageParse(String info) {
		Log.i(DTAG,"obd io data package parse - BluetoothManage");
		DataPackageInfo dataPackageInfo = JsonUtil.json2object(info,
				DataPackageInfo.class);

		if (null != dataPackageInfo) {
			dataPackages.add(dataPackageInfo);

			if (!Utils.isEmpty(dataPackageInfo.dataNumber)) {
				Log.i(DTAG, "Saving OBDInfo (DeviceId and DataNumber) - BluetoothManage");
				OBDInfoSP.saveInfo(mContext, dataPackageInfo.deviceId,
						dataPackageInfo.dataNumber);
				LogUtil.i("dataNumber:" + dataPackageInfo.dataNumber);
			}
			Log.i(OBDTAG,"data package: "+dataPackageInfo.result);
			Log.i(OBDTAG,"data package: "+dataPackageInfo.rtcTime);
			Log.i(OBDTAG,"data package: "+dataPackageInfo.tripMileage);

		}
		Log.i(DTAG,"Setting getIOdata on dataListener - BluetoothManage");
		dataListener.getIOData(dataPackageInfo);
	}

	/**
	 * ����������ݼ���
	 * 
	 * @param dataListener
	 */
	public void setBluetoothDataListener(BluetoothDataListener dataListener) {
		Log.i(DTAG,"Setting up data listeners - BluetoothManage");
		this.dataListener = dataListener;
	}

	/**
	 * �����������
	 * 
	 */
	public interface BluetoothDataListener {
		public void getBluetoothState(int state);

		public void setCtrlResponse(ResponsePackageInfo responsePackageInfo);

		public void setParameterResponse(ResponsePackageInfo responsePackageInfo);

		public void getParameterData(ParameterPackageInfo parameterPackageInfo);

		public void getIOData(DataPackageInfo dataPackageInfo);

		public void deviceLogin(LoginPackageInfo loginPackageInfo);
	}

}
