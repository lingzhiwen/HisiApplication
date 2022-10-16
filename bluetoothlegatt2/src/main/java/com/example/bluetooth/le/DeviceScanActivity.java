

/*
 * Copyright (C) 2013 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.example.bluetooth.le;

import android.Manifest;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothHidDevice;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;

import android.os.Build;
import android.os.ParcelUuid;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;

import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Toast;

import java.util.ArrayList;

import android.util.Log;


import java.io.UnsupportedEncodingException;
import java.util.Iterator;
import java.util.Set;

/**
 * Activity for scanning and displaying available Bluetooth LE devices.
 */
public class DeviceScanActivity extends Activity {
    private final static String TAG = "DeviceScanActivity";
    private BluetoothAdapter mBluetoothAdapter;
    private boolean mScanning;
    private Handler mHandler;
    private IntentFilter mIntentFilter;
    private ArrayList<BluetoothDevice> mLeDevices;
    private BluetoothHidDevice mService;
    private boolean mIsProfileReady;

    private static final int PROXMITY_RSSI_THRESHOLD = -35;
    private static final int PROXMITY_PATHLOSS_THRESHOLD = 35;

    private static final int INVALID_TX_POWER = 0xffff;
    static final int TX_POWER_FLAG = 0x0a;
    static final int COMPLETE_NAME_FLAG = 0x09;
    static final int UUID16_SERVICE_FLAG_MORE = 0x02;
    static final int UUID16_SERVICE_FLAG_COMPLETE = 0x03;
    static final int UUID32_SERVICE_FLAG_MORE = 0x04;
    static final int UUID32_SERVICE_FLAG_COMPLETE = 0x05;
    static final int UUID128_SERVICE_FLAG_MORE = 0x06;
    static final int UUID128_SERVICE_FLAG_COMPLETE = 0x07;


    static final int HOGP_UUID16 = 0x1812;

    private static final int REQUEST_ENABLE_BT = 1;
    // Stops scanning after 10 seconds.
    private static final long SCAN_PERIOD = 1000000;
    private static final int PERMISSION_REQUEST_COARSE_LOCATION = 1;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
//        getActionBar().setTitle(R.string.title_devices);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            // Android M Permission check
            if (this.checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{Manifest.permission.ACCESS_COARSE_LOCATION}, PERMISSION_REQUEST_COARSE_LOCATION);
            }
        }
        requestWindowFeature(Window.FEATURE_NO_TITLE);

        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.activity_devicescan);
        mHandler = new Handler();
        Log.i(TAG, "onCreate");

        // Use this check to determine whether BLE is supported on the device.  Then you can
        // selectively disable BLE-related features.
        if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            Toast.makeText(this, R.string.ble_not_supported, Toast.LENGTH_SHORT).show();
            finish();
        }
        mIntentFilter = new IntentFilter();
        mIntentFilter.addAction(BluetoothDevice.ACTION_BOND_STATE_CHANGED);
        mIntentFilter.addAction(BluetoothDevice.ACTION_UUID);
        mIntentFilter.addAction(BluetoothHidDevice.ACTION_CONNECTION_STATE_CHANGED);

        // Initializes a Bluetooth adapter.  For API level 18 and above, get a reference to
        // BluetoothAdapter through BluetoothManager.
        final BluetoothManager bluetoothManager =
                (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        mBluetoothAdapter = bluetoothManager.getAdapter();

        mBluetoothAdapter.getProfileProxy(this, new InputDeviceServiceListener(), BluetoothProfile.HID_DEVICE);

        // Checks if Bluetooth is supported on the device.
        if (mBluetoothAdapter == null) {
            Toast.makeText(this, R.string.error_bluetooth_not_supported, Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);
        if (!mScanning) {
            menu.findItem(R.id.menu_stop).setVisible(false);
            menu.findItem(R.id.menu_scan).setVisible(true);
            menu.findItem(R.id.menu_refresh).setActionView(null);
        } else {
            menu.findItem(R.id.menu_stop).setVisible(true);
            menu.findItem(R.id.menu_scan).setVisible(false);
            menu.findItem(R.id.menu_refresh).setActionView(
                    R.layout.actionbar_indeterminate_progress);
        }
        return true;
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        Log.d(TAG,"summer onKeyDown keyCode="+keyCode);
        if ((keyCode == KeyEvent.KEYCODE_HOME)) {
            Toast.makeText(this, "按下了e键", Toast.LENGTH_SHORT).show();
            return true;
        }else {
            return super.onKeyDown(keyCode, event);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        // Ensures Bluetooth is enabled on the device.  If Bluetooth is not currently enabled,
        // fire an intent to display a dialog asking the user to grant permission to enable it.
        if (!mBluetoothAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
        }

        //getBondedDevices();
        registerReceiver(mBluetoothReceiver, mIntentFilter);
        scanLeDevice(true);

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        // User chose not to enable Bluetooth.
        Log.d(TAG, "summer requestCode=" + requestCode + ",resultCode=" + resultCode);
        if (requestCode == REQUEST_ENABLE_BT && resultCode == Activity.RESULT_CANCELED) {
            finish();
            return;
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver(mBluetoothReceiver);
        scanLeDevice(false);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            mBluetoothAdapter.closeProfileProxy(BluetoothProfile.HID_DEVICE, mService);
        }
    }

    void doHogpConnect(BluetoothDevice device) {
        ParcelUuid[] uuids = device.getUuids();
        if (uuids == null) return;
        Log.v(TAG, "uuid update change event is received");

        if ((device.getType() == BluetoothDevice.DEVICE_TYPE_CLASSIC)) {
            Log.v(TAG, "Not a LE device, ignore");
            return;
        }

        if (!BluetoothUuid.isUuidPresent(uuids, BluetoothUuid.Hogp)) {
            Log.v(TAG, "Not support HOGP, ignore");
            return;
        }

        if (mService != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                Log.d(TAG,"summer connect success="+device.getName());
                boolean connected = mService.connect(device);
                Log.d(TAG,"summer connected="+connected);
                finish();
            }
        } else {
            Log.v(TAG, "Bluetooth HID serivce is not ready");
        }
    }

    private void scanLeDevice(final boolean enable) {
        if (enable) {
            // Stops scanning after a pre-defined scan period.
            mHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    mScanning = false;
                    mBluetoothAdapter.stopLeScan(mLeScanCallback);
                }
            }, SCAN_PERIOD);

            mScanning = true;
            mBluetoothAdapter.startLeScan(mLeScanCallback);
        } else if (mScanning) {
            mScanning = false;
            mBluetoothAdapter.stopLeScan(mLeScanCallback);
        }
    }

    private final class InputDeviceServiceListener
            implements BluetoothProfile.ServiceListener {

        public void onServiceConnected(int profile, BluetoothProfile proxy) {
            Log.d(TAG, "Bluetooth service connected");
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                mService = (BluetoothHidDevice) proxy;
            }
            // We just bound to the service
            mIsProfileReady = true;
            getBondedDevices();
            scanLeDevice(true);
        }

        public void onServiceDisconnected(int profile) {
            Log.d(TAG, "Bluetooth service disconnected");
            mIsProfileReady = false;
            mService = null;
        }
    }

    private final BroadcastReceiver mBluetoothReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            Log.v(TAG, "Received intent: " + action);
            BluetoothDevice remoteDevice = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
            if (action.equals(BluetoothDevice.ACTION_BOND_STATE_CHANGED)) {
                Log.v(TAG, "Bond state change event is received");
            } else if (action.equals(BluetoothDevice.ACTION_UUID)) {
                doHogpConnect(remoteDevice);
            } else if (action.equals(BluetoothHidDevice.ACTION_CONNECTION_STATE_CHANGED)) {
                Log.v(TAG, "Connecton state changed");
                if (mService.getConnectionState(remoteDevice) == BluetoothProfile.STATE_CONNECTED) {
                    finish();
                }
            }
        }
    };

    private int extractTxPower(byte[] scanRecord) {
        int i, length = scanRecord.length;
        i = 0;

        while (i < length - 2) {
            int element_len = scanRecord[i];
            byte element_type = scanRecord[i + 1];
            if (element_type == 0x0a) {
                Log.i(TAG, "extractTxPower Bingo, we TX power=" + scanRecord[i + 2]);
                return scanRecord[i + 2];
            }
            i += element_len + 1;
        }

        return INVALID_TX_POWER;
    }

    /*lgh we only care 16bit UUID now*/
    private boolean containHogpUUID(byte[] scanRecord) {
        int i, j, length = scanRecord.length;
        i = 0;
        int uuid = 0;
        while (i < length - 2) {
            int element_len = scanRecord[i];
            byte element_type = scanRecord[i + 1];
            if (element_type == UUID16_SERVICE_FLAG_MORE
                    || element_type == UUID16_SERVICE_FLAG_COMPLETE) {
                for (j = 0; j < element_len - 1; j++, j++) {
                    uuid = scanRecord[i + j + 2] + (scanRecord[i + j + 3] << 8);
                    Log.i(TAG, "containHogpUUID Got UUID uuid=0x" + Integer.toHexString(uuid));
                    if (uuid == HOGP_UUID16) {
                        return true;
                    }
                }
            } else if (element_type >= UUID32_SERVICE_FLAG_MORE
                    && element_type >= UUID128_SERVICE_FLAG_COMPLETE) {
                Log.i(TAG, "Do not support parsing 32bit or 12bit UUID now");
            }
            i += element_len + 1;
        }

        return false;
    }

    void printArray(byte[] array, String preTag) {
        if (array != null) {
            for (int i = 0; i < array.length; i++) {
                Log.v(TAG, preTag + " result:0X" + Integer.toHexString((int) (array[i] & 0x000000FF)));
            }
        } else {
            Log.e(TAG, preTag + " return array is null!");
        }
    }

    private boolean isNameMatchextracName(byte[] scanRecord) {
        int i, length = scanRecord.length;
        i = 0;
        byte[] RcName = new byte[50];
        String decodedName = null;
        while (i < length - 2) {
            int element_len = scanRecord[i];
            byte element_type = scanRecord[i + 1];
            if (element_type == COMPLETE_NAME_FLAG) {
                System.arraycopy(scanRecord, i + 2, RcName, 0, element_len - 1);
                try {
                    decodedName = new String(RcName, "UTF-8");
                } catch (UnsupportedEncodingException e) {
                }
                Log.i(TAG, "decodedName: " + decodedName);
                String MyRcName1 = new String("语音助手");
                if (decodedName.startsWith(MyRcName1)) {
                    Log.i(TAG, "we found our RC");
                    return true;
                }
            }
            i += element_len + 1;
        }

        return false;
    }

    private boolean isGoodHogpRc(final int rssi, byte[] scanRecord) {
        int tx_power = extractTxPower(scanRecord);
        boolean isHogpDevice = containHogpUUID(scanRecord);
        boolean isMyRc = isNameMatchextracName(scanRecord);
        if (isMyRc) {
            if ((tx_power - rssi) <= PROXMITY_PATHLOSS_THRESHOLD) {
                Log.i(TAG, "we found our RC that is closed enough");
                //return true;
            } else {
                Log.i(TAG, "tx_power: " + tx_power + ", rssi: " + rssi);
            }
            return true;
        }
        return false;
    }

    // Device scan callback.
    private BluetoothAdapter.LeScanCallback mLeScanCallback =
            new BluetoothAdapter.LeScanCallback() {
                @Override
                public void onLeScan(final BluetoothDevice device, final int rssi, final byte[] scanRecord) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Log.i(TAG, "onLeScan device=" + device.getName() + "rssi=" + rssi);
                            /*lgh we might add some more criteria here*/
                            if (isGoodHogpRc(rssi, scanRecord) == true) {
                                if (mScanning) {
                                    mBluetoothAdapter.stopLeScan(mLeScanCallback);
                                    mScanning = false;
                                }

                                if (device.getBondState() == BluetoothDevice.BOND_NONE) {
                                    if (device.createBond() == false) {
                                        Log.i(TAG, "onLeScan->Start bond failed=" + device);
                                    }
                                }
                            }
                        }
                    });
                }
            };


    private void getBondedDevices() {
        Set<BluetoothDevice> devices = mBluetoothAdapter.getBondedDevices();
        Log.d(TAG,devices.size()+"<<size devices");
        if (devices.size()>0) {
            for(Iterator<BluetoothDevice> iterator = devices.iterator(); iterator.hasNext();){
                BluetoothDevice bluetoothDevice=(BluetoothDevice)iterator.next();
                Log.i(TAG, "BT devices: "+bluetoothDevice.getName() + " -> " + bluetoothDevice.getAddress());
                if(bluetoothDevice.getName().startsWith(Constans.BT_NAME)){
                    Log.i(TAG, "Found paired RC");
                    if((mService.getConnectionState(bluetoothDevice) != BluetoothProfile.STATE_CONNECTED) && (mService.getConnectionState(bluetoothDevice) != BluetoothProfile.STATE_CONNECTING)){
                        doHogpConnect(bluetoothDevice);
                    }
                }
            }
        }
    }


    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        switch (requestCode) {
            case PERMISSION_REQUEST_COARSE_LOCATION:
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // TODO request success
                }
                break;
        }
    }

    @Override
    protected void onUserLeaveHint() {
        //super.onUserLeaveHint();
        Log.d(TAG,"summer onKeyDown onUserLeaveHint");
        //Toast.makeText(this, "summer onUserLeaveHint", Toast.LENGTH_SHORT).show();
    }
}
