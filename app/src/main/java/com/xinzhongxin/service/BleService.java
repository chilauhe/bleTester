package com.xinzhongxin.service;

import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Binder;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;

import com.xinzhongxin.bletester.DeviceConnect;
import com.xinzhongxin.utils.DateUtil;
import com.xinzhongxin.utils.Utils;

import java.util.List;
import java.util.UUID;

public class BleService extends Service {
    // 为了传送状态响应状态，要有几条ACTION
    public final static String ACTION_GATT_CONNECTED = "com.example.bluetooth.le.ACTION_GATT_CONNECTED";
    public final static String ACTION_GATT_DISCONNECTED = "com.example.bluetooth.le.ACTION_GATT_DISCONNECTED";
    public final static String ACTION_GATT_SERVICES_DISCOVERED = "com.example.bluetooth.le.ACTION_GATT_SERVICES_DISCOVERED";
    public final static String ACTION_DATA_AVAILABLE = "com.example.bluetooth.le.ACTION_DATA_AVAILABLE";
    public final static String ACTION_CHAR_READED = "com.example.bluetooth.le.ACTION_CHAR_READED";
    public final static String BATTERY_LEVEL_AVAILABLE = "com.example.bluetooth.le.BATTERY_LEVEL_AVAILABLE";
    public final static String EXTRA_DATA = "com.example.bluetooth.le.EXTRA_DATA";
    public final static String EXTRA_STRING_DATA = "com.example.bluetooth.le.EXTRA_STRING_DATA";
    public final static String EXTRA_DATA_LENGTH = "com.example.bluetooth.le.EXTRA_DATA_LENGTH";
    public final static String ACTION_GATT_RSSI = "com.example.bluetooth.le.ACTION_GATT_RSSI";
    public final static String EXTRA_DATA_RSSI = "com.example.bluetooth.le.ACTION_GATT_RSSI";
    // 集中常用的
    public static final UUID RX_ALART_UUID = UUID
            .fromString("00001802-0000-1000-8000-00805f9b34fb");
    public static final UUID RX_SERVICE_UUID = UUID
            .fromString("0000ffe0-0000-1000-8000-00805f9b34fb");// DE5BF728-D711-4E47-AF26-65E3012A5DC7
    public static final UUID MY_SERVICE_UUID = UUID
            .fromString("0000fff0-0000-1000-8000-00805f9b34fb");
    public static final UUID MY_CHAR_UUID = UUID
            .fromString("0000fff4-0000-1000-8000-00805f9b34fb");
    public static final UUID RX_CHAR_UUID = UUID
            .fromString("00002A06-0000-1000-8000-00805f9b34fb");// DE5BF729-D711-4E47-AF26-65E3012A5DC7
    public static final UUID TX_CHAR_UUID = UUID
            .fromString("0000ffe1-0000-1000-8000-00805f9b34fb");// DE5BF72A-D711-4E47-AF26-65E3012A5DC7
    public static final UUID CCCD = UUID
            .fromString("00002902-0000-1000-8000-00805f9b34fb");
    public static final UUID C22D = UUID
            .fromString("00002902-0000-1000-8000-00805f9b34fb");
    public static final UUID BATTERY_SERVICE_UUID = UUID
            .fromString("0000180f-0000-1000-8000-00805f9b34fb");
    public static final UUID BATTERY_CHAR_UUID = UUID
            .fromString("00002a19-0000-1000-8000-00805f9b34fb");
    private final static String TAG = "BleService";
    private static final int STATE_DISCONNECTED = 0;
    private static final int STATE_CONNECTING = 1;
    private static final int STATE_CONNECTED = 2;
    private final IBinder mBinder = new LocalBinder();
    public BluetoothManager mBluetoothManager;
    public BluetoothAdapter mBluetoothAdapter;
    public BluetoothGatt mBluetoothGatt;
    public int mConnectionState = STATE_DISCONNECTED;
    public String notify_result;
    public String notify_string_result;
    public int notify_result_length;
    public BluetoothGattCallback mBluetoothGattCallback = new BluetoothGattCallback() {

        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status,
                                            int newState) {
            String intentAction;
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                intentAction = ACTION_GATT_CONNECTED;
                mConnectionState = STATE_CONNECTED;
                broadcastUpdate(intentAction);
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                intentAction = ACTION_GATT_DISCONNECTED;
                mConnectionState = STATE_DISCONNECTED;
                broadcastUpdate(intentAction);
            }
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                broadcastUpdate(ACTION_GATT_SERVICES_DISCOVERED);
                List<BluetoothGattService> gattServices = mBluetoothGatt.getServices();
                Log.e("onServicesDiscovered", "Services count: " + gattServices.size());
                if (gattServices.size() > 0) {
                    for (BluetoothGattService gattService : gattServices) {
                        String serviceUUID = gattService.getUuid().toString();
                        Log.e("onServicesDiscovered", "Service uuid " + serviceUUID);
                    }
                    Log.i(TAG, "service discovered");
                } else {
                    refreshDeviceCache(mBluetoothGatt);
                    mBluetoothGatt.discoverServices();
                }
            } else {
                Log.w(TAG, "onServicesDiscovered received: " + status);
            }
        }

        @Override
        public void onCharacteristicRead(BluetoothGatt gatt,
                                         BluetoothGattCharacteristic characteristic, int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                getCharacteristicValue(characteristic);
            } else {
                Log.v(TAG, " BluetoothGatt Read Failed!");
            }

        }

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt,
                                            BluetoothGattCharacteristic characteristic) {
            broadcastUpdate(ACTION_DATA_AVAILABLE, characteristic);
        }

        @Override
        public void onReadRemoteRssi(BluetoothGatt gatt, int rssi, int status) {
            // TODO Auto-generated method stub
            super.onReadRemoteRssi(gatt, rssi, status);
            Intent rssiIntent = new Intent();
            rssiIntent.putExtra(EXTRA_DATA_RSSI, rssi);
            rssiIntent.setAction(ACTION_GATT_RSSI);
            sendBroadcast(rssiIntent);
            if (mBluetoothGatt != null) {
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        // TODO Auto-generated method stub
                        try {
                            Thread.sleep(2000);
                            mBluetoothGatt.readRemoteRssi();
                        } catch (InterruptedException e) {
                            // TODO Auto-generated catch block
                            e.printStackTrace();
                        }
                    }
                }).start();
            }

        }

    };
    private String mbluetoothDeviceAddress;

    private boolean refreshDeviceCache(BluetoothGatt gatt) {
        try {
            BluetoothGatt localBluetoothGatt = gatt;
            java.lang.reflect.Method localMethod = localBluetoothGatt.getClass().getMethod("refresh", new Class[0]);
            if (localMethod != null) {
                boolean bool = ((Boolean) localMethod.invoke(localBluetoothGatt, new Object[0])).booleanValue();
                return bool;
            }
        } catch (Exception localException) {
            Log.e(TAG, "An exception occured while refreshing device");
        }
        return false;
    }

    private void getCharacteristicValue(
            BluetoothGattCharacteristic characteristic) {
        // TODO Auto-generated method stub
        List<BluetoothGattDescriptor> des = characteristic.getDescriptors();
        Intent mIntent = new Intent(ACTION_CHAR_READED);
        if (des.size() != 0) {
            mIntent.putExtra("desriptor1", des.get(0).getUuid().toString());
            mIntent.putExtra("desriptor2", des.get(1).getUuid().toString());
        }
        mIntent.putExtra("StringValue", characteristic.getStringValue(0));
        String hexValue = Utils.bytesToHex(characteristic.getValue());
        mIntent.putExtra("HexValue", hexValue.toString());
        mIntent.putExtra("time", DateUtil.getCurrentDatatime());
        sendBroadcast(mIntent);
    }

    @Override
    public IBinder onBind(Intent intent) {
        // TODO Auto-generated method stub
        return mBinder;
    }

    private void broadcastUpdate(String action) {
        Intent mIntent = new Intent(action);
        sendBroadcast(mIntent);
    }

    private void broadcastUpdate(String action,
                                 BluetoothGattCharacteristic characteristic) {
        final Intent intent = new Intent();
        intent.setAction(action);
        final byte[] data = characteristic.getValue();
        final String stringData = characteristic.getStringValue(0);
        if (data != null && data.length > 0) {
            final StringBuilder stringBuilder = new StringBuilder(data.length);
            for (byte byteChar : data) {
                stringBuilder.append(String.format("%X", byteChar));
            }
            if (stringData != null) {
                intent.putExtra(EXTRA_STRING_DATA, stringData);
            } else {
                Log.v("tag", "characteristic.getStringValue is null");
            }
            notify_result = stringBuilder.toString();
            notify_string_result = stringData;
            notify_result_length = data.length;
            intent.putExtra(EXTRA_DATA, notify_result);
            intent.putExtra(EXTRA_DATA_LENGTH, notify_result_length);
        }
        sendBroadcast(intent);
    }

    public boolean init() {
        IntentFilter bleSeviceFilter = new IntentFilter();
        bleSeviceFilter.addAction(DeviceConnect.FIND_DEVICE_ALARM_ON);
        bleSeviceFilter.addAction(DeviceConnect.CANCEL_DEVICE_ALARM);

        if (mBluetoothAdapter != null) {
            Log.w(TAG, "BleService already initialized");
            return true;
        }

        if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            Log.e(TAG, "Unable to initialize BluetoothManager. BLE not supported");
            return false;
        }

        if (mBluetoothManager == null) {
            mBluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
            if (mBluetoothManager == null) {
                Log.e(TAG, "Unable to initialize BluetoothManager.");
                return false;
            }
        }

        mBluetoothAdapter = mBluetoothManager.getAdapter();
        if (mBluetoothAdapter == null) {
            Log.e(TAG, "Unable to obtain a BluetoothAdapter.");
            return false;
        }
        Log.i(TAG, "ble svc init ok");
        return true;
    }

    public boolean connect(String bleAddress) {
        // TODO Auto-generated method stub
        if (mBluetoothAdapter == null || bleAddress == null) {
            Log.w(TAG,
                    "BluetoothAdapter not initialized or unspecified address.");
            return false;
        }

        if (mbluetoothDeviceAddress != null
                && bleAddress.equals(mbluetoothDeviceAddress)
                && mBluetoothGatt != null) {
            if (mBluetoothGatt.connect()) {
                mConnectionState = STATE_CONNECTING;
                return true;
            } else {
                return false;
            }

        }
        final BluetoothDevice device = mBluetoothAdapter
                .getRemoteDevice(bleAddress);

        if (device == null) {
            Log.w(TAG, "Device not found.  Unable to connect.");
            return false;
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
            mBluetoothGatt = device
                    .connectGatt(this, false, mBluetoothGattCallback, BluetoothDevice.TRANSPORT_LE,
                            BluetoothDevice.PHY_LE_1M_MASK | BluetoothDevice.PHY_LE_2M_MASK | BluetoothDevice.PHY_LE_CODED_MASK
                    );
        else
            mBluetoothGatt = device
                    .connectGatt(this, false, mBluetoothGattCallback);
        refreshDeviceCache(mBluetoothGatt);
        mbluetoothDeviceAddress = bleAddress;
        mConnectionState = STATE_CONNECTING;
        return true;
    }

    public void disconnect() {
        if (mBluetoothAdapter == null || mBluetoothGatt == null) {
            Log.w(TAG, "BluetoothAdapter not initialized");
            return;
        }
        mBluetoothGatt.disconnect();
    }

    public void close(BluetoothGatt gatt) {
        gatt.disconnect();
        gatt.close();
        if (mBluetoothAdapter != null) {
            mBluetoothAdapter.cancelDiscovery();
            mBluetoothAdapter = null;
        }
    }

    @Override
    public boolean onUnbind(Intent intent) {
        // TODO Auto-generated method stub
        return super.onUnbind(intent);
    }

    @Override
    public void onDestroy() {
        // TODO Auto-generated method stub
        super.onDestroy();
        this.close(mBluetoothGatt);
    }

    public class LocalBinder extends Binder {
        public BleService getService() {
            return BleService.this;
        }
    }
}
