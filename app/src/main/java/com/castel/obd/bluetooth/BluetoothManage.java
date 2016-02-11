package com.castel.obd.bluetooth;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Handler;
import android.os.Message;
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

public class BluetoothManage {
	private final String BT_NAME = "IDD-212";// �������

	public final static int BLUETOOTH_CONNECT_SUCCESS = 0;// �������ӳɹ�
	public final static int BLUETOOTH_CONNECT_FAIL = 1;// ��������ʧ��
	public final static int BLUETOOTH_CONNECT_EXCEPTION = 2;// ���������쳣
	public final static int BLUETOOTH_READ_DATA = 4;// ����������쳣
	public final static int CANCEL_DISCOVERY = 5;// ȡ�������豸����

	public final static int CONNECTED = 0;// ����������
	public final static int DISCONNECTED = 1;// ����δ����
	public final static int CONNECTTING = 2;// ��������������
	private int btState = DISCONNECTED;

	private static Context mContext;
	private static BluetoothManage mInstance;
	private BluetoothChat mBluetoothChat;
	private BluetoothAdapter mBluetoothAdapter;

	private BluetoothDataListener dataListener;

	public List<DataPackageInfo> dataPackages;

	private boolean isMacAddress = false;// �״����������Ƿ��п��Ե�MAC��ַ

	private boolean isParse = false;
	private List<String> dataLists = new ArrayList<String>();
	private boolean isDeviceSynced = false;

	public BluetoothManage() {
		dataPackages = new ArrayList<DataPackageInfo>();
		mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
		mBluetoothChat = new BluetoothChat(mHandler);
		registerBluetoothReceiver();
		initOBD();
		mHandler.postDelayed(runnable, 500);
	}

	public static BluetoothManage getInstance(Context context) {
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
			super.handleMessage(msg);
			switch (msg.what) {
			case CANCEL_DISCOVERY:
				if (mBluetoothAdapter.isDiscovering()) {
					mBluetoothAdapter.cancelDiscovery();
				}
				break;
			case BLUETOOTH_CONNECT_SUCCESS:
				btState = CONNECTED;
				OBDInfoSP.saveMacAddress(mContext, (String) msg.obj);
				dataListener.getBluetoothState(btState);
				LogUtil.i("Bluetooth state:CONNECTED");
				break;
			case BLUETOOTH_CONNECT_FAIL:
				btState = DISCONNECTED;
				LogUtil.i("Bluetooth state:DISCONNECTED");

				if (isMacAddress) {
					if (mBluetoothAdapter.isDiscovering()) {
						mBluetoothAdapter.cancelDiscovery();
					}
					mBluetoothAdapter.startDiscovery();
				} else {
//					Toast.makeText(mContext, R.string.bluetooth_connect_fail,
//							Toast.LENGTH_LONG).show();
					dataListener.getBluetoothState(btState);
				}
				break;
			case BLUETOOTH_CONNECT_EXCEPTION:
				btState = DISCONNECTED;
				LogUtil.i("Bluetooth state:DISCONNECTED");
				dataListener.getBluetoothState(btState);
				break;
			case BLUETOOTH_READ_DATA:
				if (!Utils.isEmpty(Utils.bytesToHexString((byte[]) msg.obj))) {
					dataLists.add(Utils.bytesToHexString((byte[]) msg.obj));
				}
				break;
			}
		}
	};

	private void registerBluetoothReceiver() {
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
			String action = intent.getAction();
			LogUtil.i(action);
			// �����豸
			if (BluetoothDevice.ACTION_FOUND.equals(action)) {
				BluetoothDevice device = intent
						.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
				LogUtil.i(device.getName() + device.getAddress());
				if (device.getName()!=null&&device.getName().contains(BT_NAME)) {
					mBluetoothChat.connectBluetooth(device);
					Toast.makeText(mContext, "Connecting to Device", Toast.LENGTH_SHORT).show();
				}
			} else if (BluetoothDevice.ACTION_ACL_CONNECTED.equals(action)) { // �������ӳɹ�
				LogUtil.i("CONNECTED");
				btState = CONNECTED;
				LogUtil.i("Bluetooth state:CONNECTED");
				dataListener.getBluetoothState(btState);
			}else if (BluetoothDevice.ACTION_BOND_STATE_CHANGED.equals(action)) { // �������ӳɹ�
				BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
				if(device.getBondState()==BluetoothDevice.BOND_BONDED){
					LogUtil.i("CONNECTED");
					btState = CONNECTED;
					LogUtil.i("Bluetooth state:CONNECTED");
					dataListener.getBluetoothState(btState);
				}
			} else if (BluetoothDevice.ACTION_ACL_DISCONNECTED.equals(action)) { // �������ӶϿ�
				btState = DISCONNECTED;
				LogUtil.i("Bluetooth state:DISCONNECTED");
				dataListener.getBluetoothState(btState);
			} else if (BluetoothAdapter.ACTION_DISCOVERY_STARTED.equals(action)) {
				LogUtil.i("Bluetooth state:ACTION_DISCOVERY_STARTED");
			} else if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED
					.equals(action)) { // ������������
				LogUtil.i("Bluetooth state:ACTION_DISCOVERY_FINISHED");
				if (btState != CONNECTED) {
					btState = DISCONNECTED;
					dataListener.getBluetoothState(btState);
				}
				}else if (BluetoothAdapter.ACTION_SCAN_MODE_CHANGED.equals(action)){
					if(mBluetoothAdapter.isEnabled())
						connectBluetooth();
					dataListener.getBluetoothState(btState);
				}
		}
	};

	/**
	 * ��OBD�豸����
	 */
	public void connectBluetooth() {
		if (btState == CONNECTED) {
			return;
		}

		LogUtil.i("Bluetooth state:CONNECTTING");
		btState = CONNECTTING;
		mBluetoothChat.closeConnect();

		// ֱ�Ӵ�����
		if (!mBluetoothAdapter.isEnabled()) {
			LogUtil.i("BluetoothAdapter.enable()");
			mBluetoothAdapter.enable();
		}

		String macAddress = OBDInfoSP.getMacAddress(mContext);
//		 macAddress = "8C:DE:52:71:F7:71";
//		macAddress = "8C:DE:52:19:DB:86";
//		macAddress = "8C:DE:52:22:C8:B5";
		if (!"".equals(macAddress)) {
			isMacAddress = true;
			BluetoothDevice device = mBluetoothAdapter
					.getRemoteDevice(macAddress);
			mBluetoothChat.connectBluetooth(device);// ֱ����MAC��ַ��������
		} else {
			LogUtil.i("startDiscovery()");
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
		btState = DISCONNECTED;
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
		return btState;
	}

	/**
	 * ��ʼ��OBD�豸���������豸ID����ݰ�ID���ʼ����������������ʼ��
	 */
	public void initOBD() {
		String deviceId = OBDInfoSP.getDeviceId(mContext);
		String dataNum = OBDInfoSP.getDataNum(mContext);
		if (!Utils.isEmpty(deviceId) && !Utils.isEmpty(dataNum)) {
			LogUtil.i("deviceId:" + deviceId + "dataNum"
					+ OBDInfoSP.getDataNum(mContext));
			// ��ʼ�����������̬��
			OBD.init(deviceId, dataNum);
		}
	}

	/**
	 * ����������
	 * 
	 * @param type
	 */
	public void obdSetCtrl(int type) {
		if (btState != CONNECTED) {
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
		if (btState != CONNECTED) {
//			Toast.makeText(mContext, R.string.bluetooth_disconnected,
//					Toast.LENGTH_LONG).show();
			return;
		}


		String result = OBD.setMonitor(type,valueList);
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
		if (btState != CONNECTED) {
//			Toast.makeText(mContext, R.string.bluetooth_disconnected,
//					Toast.LENGTH_LONG).show();
			return;
		}

		LogUtil.i("tlvTagList: " + tlvTagList);
		LogUtil.i("tlvTagList: " + valueList);


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
		if (btState != CONNECTED) {
//			Toast.makeText(mContext, R.string.bluetooth_disconnected,
//					Toast.LENGTH_LONG).show();
			return;
		}


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

		if (btState == CONNECTED && null != mBluetoothChat.connectedThread) {
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

		if (btState == CONNECTED && null != mBluetoothChat.connectedThread) {
			mBluetoothChat.connectedThread.write(bytes);
		}
	}

	/**
	 * �������������
	 * 
	 * @param data
	 */
	public void receiveDataAndParse(String data) {
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
		if (0 == result) {
			if(!isDeviceSynced) {
				long systemTime = System.currentTimeMillis();
				obdSetParameter("1A01", String.valueOf(systemTime / 1000));
			}
			obdLoginPackageParse(info);
		} else if (2 == result) {
			obdResponsePackageParse(info);
		} else if (3 == result) {
			obdParameterPackageParse(info);
		} else if (4 == result || 5 == result || 6 == result) {
			isDeviceSynced = true;
			obdIODataPackageParse(info);
		}
	}

	/**
	 * ��½���˳���
	 * 
	 * @param info
	 */
	private void obdLoginPackageParse(String info) {
		LoginPackageInfo loginPackageInfo = JsonUtil.json2object(info,
				LoginPackageInfo.class);

		if (null == loginPackageInfo) {
			return;
		}

		if ("0".equals(loginPackageInfo.flag)) {
			// �˳���
			LogUtil.i("�˳���");
		} else if ("1".equals(loginPackageInfo.flag)) {
			// ��½��
			LogUtil.i("��½��");

			sendCommandPassive(loginPackageInfo.instruction);
		}
	}

	/**
	 * ���ơ����ò�����ն˻�Ӧ
	 * 
	 * @param info
	 */
	private void obdResponsePackageParse(String info) {
		ResponsePackageInfo responsePackageInfo = JsonUtil.json2object(info,
				ResponsePackageInfo.class);

		if (null == responsePackageInfo) {
			dataListener.setCtrlResponse(null);
			dataListener.setParamaterResponse(null);
		} else {
			if ("0".equals(responsePackageInfo.flag)) {
				dataListener.setCtrlResponse(responsePackageInfo);
			} else if ("1".equals(responsePackageInfo.flag)) {
				dataListener.setParamaterResponse(responsePackageInfo);
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

		dataListener.getParamaterData(parameterPackageInfo);
	}

	/**
	 * �г���ݡ�ʵʱ������ݡ�������
	 * 
	 * @param info
	 */
	public void obdIODataPackageParse(String info) {
		DataPackageInfo dataPackageInfo = JsonUtil.json2object(info,
				DataPackageInfo.class);

		if (null != dataPackageInfo) {
			dataPackages.add(dataPackageInfo);

			if (!Utils.isEmpty(dataPackageInfo.dataNumber)) {
				OBDInfoSP.saveInfo(mContext, dataPackageInfo.deviceId,
						dataPackageInfo.dataNumber);
				LogUtil.i("dataNumber:" + dataPackageInfo.dataNumber);
			}
		}

		dataListener.getIOData(dataPackageInfo);
	}

	/**
	 * ����������ݼ���
	 * 
	 * @param dataListener
	 */
	public void setBluetoothDataListener(BluetoothDataListener dataListener) {
		this.dataListener = dataListener;
	}

	/**
	 * �����������
	 * 
	 */
	public interface BluetoothDataListener {
		public void getBluetoothState(int state);

		public void setCtrlResponse(ResponsePackageInfo responsePackageInfo);

		public void setParamaterResponse(ResponsePackageInfo responsePackageInfo);

		public void getParamaterData(ParameterPackageInfo parameterPackageInfo);

		public void getIOData(DataPackageInfo dataPackageInfo);
	}

}
