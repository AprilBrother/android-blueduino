package com.aprilbrother.blueduino;

import java.util.ArrayList;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.TextView;

public class MainActivity extends Activity {

	private final int SCAN_PERIOD = 60000;

	private final int REQUEST_ENABLE_BT = 1;


	private BluetoothAdapter mBluetoothAdapter;
	private BluetoothAdapter.LeScanCallback mScanCallback;

	private Handler mHandler;

	private ArrayList<BluetoothDevice> devices;
	private ArrayList<String> device_macs;


	private ListView lv;
	private MyAdapter adapter;


	private Handler handler = new Handler() {
		@Override
		public void handleMessage(Message msg) {
			adapter.setDeviceList(devices);
			super.handleMessage(msg);
		}
	};

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		init();
		searchDevice();
		showDeviceList();

	}

	private void init() {
		adapter = new MyAdapter();
		mHandler = new Handler();
		BluetoothManager bluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
		mBluetoothAdapter = bluetoothManager.getAdapter();
		devices = new ArrayList<BluetoothDevice>();
		device_macs = new ArrayList<String>();
	}

	/**
	 * 查找蓝牙设备
	 */
	private void searchDevice() {
		if (mBluetoothAdapter == null || !mBluetoothAdapter.isEnabled()) {
			Intent enableBtIntent = new Intent(
					BluetoothAdapter.ACTION_REQUEST_ENABLE);
			startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
		} else {
			startScanning();
		}
	}

	private void showDeviceList() {
		lv = (ListView) findViewById(R.id.lv_blueuino_main);
		lv.setAdapter(adapter);
		lv.setOnItemClickListener(new OnItemClickListener() {

			@Override
			public void onItemClick(AdapterView<?> parent, View view,
					final int position, long id) {

				 Intent intent = new
				 Intent(MainActivity.this,OperateActivity.class);
				 Bundle bundle = new Bundle();
				 bundle.putParcelable("device", devices.get(position));
				 intent.putExtras(bundle);
				 mBluetoothAdapter.stopLeScan(mScanCallback);
				 startActivity(intent);
			}
		});
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		switch (requestCode) {
		case REQUEST_ENABLE_BT:
			startScanning();
		}
	}

	private void startScanning() {
		mScanCallback = new BluetoothAdapter.LeScanCallback() {
			@Override
			public void onLeScan(BluetoothDevice device, int rssi,
					byte[] scanRecord) {
				if (!device_macs.contains(device.getAddress())) {
					if(device.getName()!= null && device.getName().contains("ZeroBeacon"))
					devices.add(device);
					device_macs.add(device.getAddress());
					handler.sendEmptyMessage(0);
				}
			}
		};
		mBluetoothAdapter.startLeScan(mScanCallback);
		mHandler.postDelayed(new Runnable() {
			@Override
			public void run() {
				mBluetoothAdapter.stopLeScan(mScanCallback);
			}
		}, SCAN_PERIOD);
	}

	public class MyAdapter extends BaseAdapter {

		private ArrayList<BluetoothDevice> myDevices = new ArrayList<BluetoothDevice>();

		@SuppressWarnings("unchecked")
		public void setDeviceList(ArrayList<BluetoothDevice> list) {
			if (list != null) {
				myDevices = (ArrayList<BluetoothDevice>) list.clone();
				notifyDataSetChanged();
			}
		}

		public void clearDeviceList() {
			if (myDevices != null) {
				myDevices.clear();
			}
			notifyDataSetChanged();
		}

		@Override
		public int getCount() {
			return myDevices.size();
		}

		@Override
		public Object getItem(int position) {
			return null;
		}

		@Override
		public long getItemId(int position) {
			return 0;
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			View view;
			ViewHolder holder;
			if (convertView == null) {
				view = View.inflate(MainActivity.this, R.layout.ble_list_item,
						null);
				holder = new ViewHolder();
				holder.tv = (TextView) view
						.findViewById(R.id.tv_ble_list_item_name);
				view.setTag(holder);
			} else {
				view = convertView;
				holder = (ViewHolder) view.getTag();
			}
			holder.tv.setText(myDevices.get(position).getName());
			return view;
		}
	}

	class ViewHolder {
		TextView tv;
	}
}
