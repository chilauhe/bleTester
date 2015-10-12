package com.xinzhongxin.bletester;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothAdapter.LeScanCallback;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ListView;

import com.xinzhongxin.adapter.BleDeviceListAdapter;
import com.xinzhongxinbletester.R;

public class MainActivity extends Activity {
	ListView listView;

	BluetoothAdapter mBluetoothAdapter;
	private LeScanCallback mLeScanCallback;
	BleDeviceListAdapter mBleDeviceListAdapter;
	private boolean scanning = true;

	Handler handler;

	@SuppressLint("NewApi")
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		getActionBar().setTitle(R.string.app_title);
		init();
		getBleAdapter();
		getScanResualt();
		mBluetoothAdapter.startLeScan(mLeScanCallback);
	}

	private void init() {
		// TODO Auto-generated method stub
		listView = (ListView) findViewById(R.id.lv_deviceList);
		mBleDeviceListAdapter = new BleDeviceListAdapter(this);
		listView.setAdapter(mBleDeviceListAdapter);
		setListItemListener();
	}

	@SuppressLint("NewApi")
	private void getBleAdapter() {
		final BluetoothManager bluetoothManager = (BluetoothManager) this
				.getSystemService(Context.BLUETOOTH_SERVICE);
		mBluetoothAdapter = bluetoothManager.getAdapter();
	}

	@SuppressLint("NewApi")
	private void getScanResualt() {

		mLeScanCallback = new LeScanCallback() {

			@Override
			public void onLeScan(final BluetoothDevice device, final int rssi,
					final byte[] scanRecord) {
				MainActivity.this.runOnUiThread(new Runnable() {
					public void run() {
						mBleDeviceListAdapter.addDevice(device, rssi,
								bytesToHex(scanRecord));
						mBleDeviceListAdapter.notifyDataSetChanged();

					}
				});

			}
		};

	}

	static final char[] hexArray = "0123456789ABCDEF".toCharArray();

	public static String bytesToHex(byte[] bytes) {
		char[] hexChars = new char[bytes.length * 2];
		for (int j = 0; j < bytes.length; j++) {
			int v = bytes[j] & 0xFF;
			hexChars[j * 2] = hexArray[v >>> 4];
			hexChars[j * 2 + 1] = hexArray[v & 0x0F];
		}
		return new String(hexChars);
	}

	private void setListItemListener() {
		listView.setOnItemClickListener(new OnItemClickListener() {
			@SuppressLint("NewApi")
			@Override
			public void onItemClick(AdapterView<?> parent, View view,
					int position, long id) {
				// TODO Auto-generated method stub
				mBluetoothAdapter.stopLeScan(mLeScanCallback);

				BluetoothDevice device = mBleDeviceListAdapter
						.getDevice(position);
				final Intent intent = new Intent(MainActivity.this,
						DeviceConnect.class);
				intent.putExtra(DeviceConnect.EXTRAS_DEVICE_NAME,
						device.getName());
				intent.putExtra(DeviceConnect.EXTRAS_DEVICE_ADDRESS,
						device.getAddress());

				startActivity(intent);

			}
		});
	}

	@SuppressLint("NewApi")
	@Override
	protected void onDestroy() {
		// TODO Auto-generated method stub
		super.onDestroy();
		mBluetoothAdapter.stopLeScan(mLeScanCallback);
	}

	@SuppressLint("NewApi")
	@Override
	protected void onResume() {
		// TODO Auto-generated method stub
		super.onResume();
		mBleDeviceListAdapter.clear();
		mBluetoothAdapter.startLeScan(mLeScanCallback);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}

	@SuppressLint("NewApi")
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {

		switch (item.getItemId()) {
		case R.id.menu_stop:
			mBluetoothAdapter.stopLeScan(mLeScanCallback);
			scanning = false;
			break;
		case R.id.menu_scan:
			mBleDeviceListAdapter.clear();
			mBluetoothAdapter.startLeScan(mLeScanCallback);
			scanning = true;
			break;

		}

		return super.onOptionsItemSelected(item);
	}

}