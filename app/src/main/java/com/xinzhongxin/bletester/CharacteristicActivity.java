package com.xinzhongxin.bletester;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ListView;
import android.widget.Toast;

import com.xinzhongxin.adapter.CharacteristicListAdapter;
import com.xinzhongxin.service.BleService;
import com.xinzhongxin.utils.Utils;
import com.xinzhongxinbletester.R;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

public class CharacteristicActivity extends Activity {
    ListView lv;
    BluetoothAdapter mBluetoothAdapter;
    CharacteristicListAdapter charListAdapter;
    UUID uuid;
    BleService bleService;
    BluetoothGattService gattService;
    private final ServiceConnection conn = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName arg0, IBinder service) {
            bleService = ((BleService.LocalBinder) service).getService();
            gattService = bleService.mBluetoothGatt.getService(uuid);
            bleService.mBluetoothGatt.readRemoteRssi();
            final ArrayList<HashMap<String, String>> charNames = new ArrayList<HashMap<String, String>>();
            final List<BluetoothGattCharacteristic> gattchars = gattService
                    .getCharacteristics();
            for (BluetoothGattCharacteristic c : gattchars) {
                HashMap<String, String> currentCharData = new HashMap<String, String>();
                String uuidStr = c.getUuid().toString();
                currentCharData.put("Name", Utils.attributes
                        .containsKey(uuidStr) ? Utils.attributes.get(uuidStr)
                        : "Unknown Characteristics");
                charNames.add(currentCharData);
            }
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    charListAdapter.addCharNames(charNames);
                    charListAdapter.addChars(gattchars);
                    charListAdapter.notifyDataSetChanged();
                }
            });
            Log.d("service", arg0.toString());
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            bleService = null;
        }
    };
    int rssi;
    BroadcastReceiver mBroadcastReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context arg0, Intent intent) {
            // TODO Auto-generated method stub
            String action = intent.getAction();
            Log.d("receive", "action " + action.toString());
            if (BleService.ACTION_GATT_RSSI.equals(action)) {
                rssi = intent.getExtras().getInt(BleService.EXTRA_DATA_RSSI);
                CharacteristicActivity.this.invalidateOptionsMenu();
            }
            if (BleService.ACTION_GATT_DISCONNECTED.equals(action)) {
                Toast.makeText(CharacteristicActivity.this, "设备连接断开",
                        Toast.LENGTH_SHORT).show();
                bleService.connect(DeviceConnect.bleAddress);
            }
            if (BleService.ACTION_GATT_SERVICES_DISCOVERED.equals(action)) {
                gattService = bleService.mBluetoothGatt.getService(uuid);
                bleService.mBluetoothGatt.readRemoteRssi();
                final ArrayList<HashMap<String, String>> charNames = new ArrayList<HashMap<String, String>>();
                final List<BluetoothGattCharacteristic> gattchars = gattService
                        .getCharacteristics();
                for (BluetoothGattCharacteristic c : gattchars) {
                    HashMap<String, String> currentCharData = new HashMap<String, String>();
                    String uuidStr = c.getUuid().toString();
                    currentCharData.put("Name", Utils.attributes
                            .containsKey(uuidStr) ? Utils.attributes.get(uuidStr)
                            : "Unknown Characteristics");
                    charNames.add(currentCharData);
                }
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        // TODO Auto-generated method stub
                        charListAdapter.addCharNames(charNames);
                        charListAdapter.addChars(gattchars);
                        charListAdapter.notifyDataSetChanged();
                    }
                });
            }
        }
    };

    private static IntentFilter makeGattUpdateIntentFilter() {
        final IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(BleService.ACTION_GATT_CONNECTED);
        intentFilter.addAction(BleService.ACTION_GATT_DISCONNECTED);
        intentFilter.addAction(BleService.ACTION_GATT_SERVICES_DISCOVERED);
        intentFilter.addAction(BleService.ACTION_DATA_AVAILABLE);
        intentFilter.addAction(BleService.BATTERY_LEVEL_AVAILABLE);
        intentFilter.addAction(BleService.ACTION_GATT_RSSI);
        return intentFilter;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chars);
        getActionBar().setTitle("特性列表");
        uuid = (UUID) getIntent().getExtras().get("serviceUUID");
        init();
        registerReceiver(mBroadcastReceiver, makeGattUpdateIntentFilter());
        bindService(new Intent(this, BleService.class), conn, BIND_AUTO_CREATE);
    }

    private void init() {
        lv = (ListView) findViewById(R.id.lv_charList);
        lv.setEmptyView(findViewById(R.id.pb_empty2));
        charListAdapter = new CharacteristicListAdapter(this);
        lv.setAdapter(charListAdapter);
        lv.setOnItemClickListener(new OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> arg0, View view,
                                    int position, long arg3) {
                Intent mIntent = new Intent(CharacteristicActivity.this,
                        ChangeCharActivity.class);
                UUID charUuid = bleService.mBluetoothGatt.getService(uuid)
                        .getCharacteristics().get(position).getUuid();
                mIntent.putExtra("charUUID", charUuid);
                mIntent.putExtra("properties", bleService.mBluetoothGatt
                        .getService(uuid).getCharacteristics().get(position)
                        .getProperties());
                mIntent.putExtra("serUUID", uuid);
                startActivity(mIntent);
            }
        });

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.services, menu);
        menu.getItem(1).setVisible(false);
        menu.getItem(0).setTitle(rssi + "");
        return true;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unbindService(conn);
        unregisterReceiver(mBroadcastReceiver);
    }
}
