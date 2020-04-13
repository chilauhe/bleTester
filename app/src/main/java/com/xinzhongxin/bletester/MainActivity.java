package com.xinzhongxin.bletester;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.Bundle;
import android.os.Handler;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ListView;
import android.widget.Toast;

import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout.OnRefreshListener;

import com.xinzhongxin.adapter.BleDeviceListAdapter;
import com.xinzhongxin.utils.Utils;
import com.xinzhongxinbletester.R;

import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import static android.bluetooth.le.ScanSettings.CALLBACK_TYPE_ALL_MATCHES;
import static android.bluetooth.le.ScanSettings.CALLBACK_TYPE_FIRST_MATCH;
import static android.bluetooth.le.ScanSettings.CALLBACK_TYPE_MATCH_LOST;

public class MainActivity extends Activity {
    ListView listView;
    SwipeRefreshLayout swagLayout;
    BluetoothAdapter mBluetoothAdapter;
    BluetoothLeScanner mLeScanner;
    List<ScanFilter> mFilter;
    ScanSettings mLeScanSetting;
    BleDeviceListAdapter mBleDeviceListAdapter;
    boolean isExit;
    Handler handler;

    SharedPreferences sharedPreferences;
    Editor editor;
    private ScanCallback mLeScanCallback;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        getActionBar().setTitle(R.string.app_title);
        sharedPreferences = getPreferences(0);
        editor = sharedPreferences.edit();
        //TODO: migrate BLE logic to independent class
        new Scanner(this, mBluetoothAdapter);
        mBluetoothAdapter = ((BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE)).getAdapter();
        if (!mBluetoothAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(
                    BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, 100);
        }
        while (!mBluetoothAdapter.isEnabled()) {
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                //e.printStackTrace();
            }
        }
        mLeScanner = mBluetoothAdapter.getBluetoothLeScanner();
        initScanCallback();
        mLeScanSetting = (new ScanSettings.Builder()).setReportDelay(0).setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY).build();
        //mFilter = new ArrayList<>();
        mLeScanner.startScan(mFilter, mLeScanSetting, mLeScanCallback);
        init();
        getBleAdapter();
    }

    private void init() {
        listView = (ListView) findViewById(R.id.lv_deviceList);
        listView.setEmptyView(findViewById(R.id.pb_empty));
        swagLayout = (SwipeRefreshLayout) findViewById(R.id.swagLayout);
        swagLayout.setVisibility(View.VISIBLE);
        swagLayout.setOnRefreshListener(new OnRefreshListener() {
            @Override
            public void onRefresh() {
                // TODO Auto-generated method stub
                mBleDeviceListAdapter.clear();
                mLeScanner.startScan(mFilter, mLeScanSetting, mLeScanCallback);
                swagLayout.setRefreshing(false);
            }
        });

        mBleDeviceListAdapter = new BleDeviceListAdapter(this);
        listView.setAdapter(mBleDeviceListAdapter);
        setListItemListener();
    }

    private void getBleAdapter() {
        final BluetoothManager bluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        mBluetoothAdapter = bluetoothManager.getAdapter();
    }

    private void initScanCallback() {
        mLeScanCallback = new ScanCallback() {
            @Override
            public void onScanResult(int callbackType, final ScanResult result) {
                super.onScanResult(callbackType, result);
                switch (callbackType) {
                    case CALLBACK_TYPE_ALL_MATCHES:
                        MainActivity.this.runOnUiThread(new Runnable() {
                            public void run() {
                                mBleDeviceListAdapter.addDevice(result.getDevice(), result.getRssi(),
                                        Utils.bytesToHex(result.getScanRecord().getBytes()));
                                mBleDeviceListAdapter.notifyDataSetChanged();
                                invalidateOptionsMenu();
                            }
                        });
                    case CALLBACK_TYPE_FIRST_MATCH:
                        break;
                    case CALLBACK_TYPE_MATCH_LOST:
                        break;
                }
            }
        };
    }

    private void setListItemListener() {
        listView.setOnItemClickListener(new OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view,
                                    int position, long id) {
                // TODO Auto-generated method stub
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

    @Override
    protected void onResume() {
        // TODO Auto-generated method stub
        super.onResume();
    }

    @Override
    protected void onDestroy() {
        // TODO Auto-generated method stub
        super.onDestroy();
        mLeScanner.stopScan(mLeScanCallback);
        mBleDeviceListAdapter.clear();
        mBluetoothAdapter.cancelDiscovery();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);
        menu.getItem(0).setTitle("共" + mBleDeviceListAdapter.getCount() + "个");
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_stop:
                mLeScanner.stopScan(mLeScanCallback);
                break;
            case R.id.menu_autoconnect:
                if (sharedPreferences.getBoolean("AutoConnect", true)) {
                    editor.putBoolean("AutoConnect", false);
                    editor.commit();
                    Toast.makeText(this, "取消自动连接", Toast.LENGTH_SHORT).show();
                } else {
                    editor.putBoolean("AutoConnect", true);
                    editor.commit();
                    Toast.makeText(this, "已设置为断开后自动连接", Toast.LENGTH_SHORT).show();
                }
                break;
            case R.id.menu_about:
                MainActivity.this.startActivity(new Intent(this,
                        AboutActivity.class));
            case R.id.menu_qrcode:
                MainActivity.this.startActivity(new Intent(this,
                        QrcodeActivity.class));
        }
        return super.onOptionsItemSelected(item);
    }

    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            exitBy2Click();
        }
        return false;
    }

    private void exitBy2Click() {
        if (!isExit) {
            isExit = true;
            Toast.makeText(this, "再按一次退出", Toast.LENGTH_SHORT).show();
            new Timer().schedule(new TimerTask() {
                public void run() {
                    isExit = false;
                }
            }, 2000);
        } else {
            onDestroy();
            finish();
            System.exit(0);
        }
    }

}
