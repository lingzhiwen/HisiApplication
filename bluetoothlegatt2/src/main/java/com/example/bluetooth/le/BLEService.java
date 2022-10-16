package com.example.bluetooth.le;

import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.BluetoothHidDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Handler;
import android.os.IBinder;
import android.os.ParcelUuid;
import android.util.Log;
import android.widget.Toast;
import android.view.WindowManager;
import android.util.DisplayMetrics;
import android.graphics.PixelFormat;

import java.io.UnsupportedEncodingException;
import java.util.Iterator;
import java.util.Set;

public class BLEService extends Service implements BLEImageView.Callback{

    private static final String TAG = BLEService.class.getName();

    private static final int PROXMITY_RSSI_THRESHOLD = -35;
    private static final int PROXMITY_PATHLOSS_THRESHOLD = 35;

    private static final int INVALID_TX_POWER = 0xffff;
    private static final int TX_POWER_FLAG = 0x0a;
    private static final int COMPLETE_NAME_FLAG = 0x09;
    static final int UUID16_SERVICE_FLAG_MORE = 0x02;
    static final int UUID16_SERVICE_FLAG_COMPLETE = 0x03;
    static final int UUID32_SERVICE_FLAG_MORE = 0x04;
    static final int UUID32_SERVICE_FLAG_COMPLETE = 0x05;
    static final int UUID128_SERVICE_FLAG_MORE = 0x06;
    static final int UUID128_SERVICE_FLAG_COMPLETE = 0x07;


    static final int HOGP_UUID16 = 0x1812;

    // Stops scanning after 10 seconds.
    private static final long SCAN_STOP_PERIOD = 1000 * 10;
    // Start scan after 30 seconds.
    private static final long SCAN_START_PERIOD = 1000 * 30;

    private BluetoothHidDevice mService;
    private BluetoothAdapter mBluetoothAdapter;

    private WindowManager mWindowManager;
    private BLEImageView mBleImageView;
    private WindowManager.LayoutParams mParams;
    private int mWidth, mHeight;
    private boolean mIsBleImageShowing;

    private boolean mScanning;

    private boolean mIsConnected;

    private Toast mToast;

    private Handler mHandler = new Handler();


    public BLEService() {
    }

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        return null;
    }

    @Override
    public boolean onUnbind(Intent intent) {
        return super.onUnbind(intent);
    }

    private void registerBTBroadcast() {
        IntentFilter mIntentFilter = new IntentFilter();
        mIntentFilter.addAction(BluetoothDevice.ACTION_BOND_STATE_CHANGED);
        mIntentFilter.addAction(BluetoothDevice.ACTION_UUID);
        mIntentFilter.addAction(BluetoothHidDevice.ACTION_CONNECTION_STATE_CHANGED);
        //mIntentFilter.addAction(BluetoothHidHost.ACTION_CONNECTION_STATE_CHANGED);
        registerReceiver(mBluetoothReceiver, mIntentFilter);
    }

    private final BroadcastReceiver mBluetoothReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            Log.v(TAG, "Received intent: " + action);
            BluetoothDevice remoteDevice = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
            if (action.equals(BluetoothDevice.ACTION_BOND_STATE_CHANGED)) {
                Log.v(TAG, "Bond state change event is received");
                int bondState = intent.getIntExtra(BluetoothDevice.EXTRA_BOND_STATE, BluetoothDevice.BOND_NONE);
                Log.d(TAG, "bond state:" + bondState);
            } else if (action.equals(BluetoothDevice.ACTION_UUID)) {
                doHogpConnect(remoteDevice);
            } else if (action.equals(BluetoothHidDevice.ACTION_CONNECTION_STATE_CHANGED)) {
                int state = intent.getIntExtra(BluetoothProfile.EXTRA_STATE, BluetoothProfile.STATE_DISCONNECTED);
                Log.d(TAG, "connect state:" + state);
                mIsConnected = (state == BluetoothProfile.STATE_CONNECTED);
                if(mIsConnected && mIsBleImageShowing){
                    zoomInImage();
                }
                //toastRemoteState(state);
                if (state == BluetoothProfile.STATE_DISCONNECTED){
                    scanLeDevice(true);
                } else {
                    scanLeDevice(false);
                }

                Log.d(TAG, "--->>>mIsConnected:" + mIsConnected);
                /*if (mIsConnected) {
                    scanLeDevice(false);
                }*/
            }
        }
    };

    @Override
    public void onCreate() {
        super.onCreate();

        if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            showToast(getString(R.string.ble_not_supported));
            //Toast.makeText(this, R.string.ble_not_supported, Toast.LENGTH_SHORT).show();
            //finish();
            return;
        }

        BluetoothManager bluetoothManager =
                (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        mBluetoothAdapter = bluetoothManager.getAdapter();

        // mBluetoothAdapter.getProfileProxy(this, new InputDeviceServiceListener(),
        //         BluetoothProfile.HID_DEVICE);

        registerBTBroadcast();
        registerVoiceIRKeyReceiver();

        if(!checkOurRemoteIsBonded()){
            Log.d(TAG, "not found bonded device, scan it");
            //scanLeDevice(true);
        } else {
            showToast(getString(R.string.found_bonded_device));
        }

        initWindow();


        Log.d(TAG, "service started ...");
    }

    private void initWindow(){
        mWindowManager = (WindowManager) getSystemService(Context.WINDOW_SERVICE);
        mBleImageView = new BLEImageView(this);
        mBleImageView.setCallback(this);

        DisplayMetrics displayMetrics = new DisplayMetrics();
        mWindowManager.getDefaultDisplay().getMetrics(displayMetrics);
        mHeight = displayMetrics.heightPixels;
        mWidth = displayMetrics.widthPixels;
        Log.d(TAG, "width:" + mWidth + ",height:" + mHeight);

        mParams = new WindowManager.LayoutParams(
                mWidth, mHeight, //Must be at least 1x1
                WindowManager.LayoutParams.TYPE_SYSTEM_ALERT,
                WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE,
                //Don't know if this is a safe default
                PixelFormat.TRANSLUCENT);

        mParams.type = WindowManager.LayoutParams.TYPE_PHONE;
        //mParams.flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE;
        //params.format= PixelFormat.RGBA_4444;//設置背景圖片
        //Don't set the preview visibility to GONE or INVISIBLE
        //mWindowManager.addView(mBleImageView, mParams);
    }

    private void zoomOutImage(){
        //mWindowManager.removeView(mBleImageView);
        mParams.flags = ~WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE;
        mParams.width = mWidth;
        mParams.height = mHeight;
        mWindowManager.addView(mBleImageView, mParams);
        mIsBleImageShowing = true;
        showToast(getString(R.string.user_start_scan));
    }

    private void zoomInImage(){
        mWindowManager.removeView(mBleImageView);
        //mParams.flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE;
        //mParams.width = 1;
        //mParams.height = 1;
        //mWindowManager.addView(mBleImageView, mParams);
        mIsBleImageShowing = false;
        showToast(getString(R.string.user_cancel_scan));
    }

    @Override
    public void onVoiceKeyUp() {
        /*Log.d(TAG, "onVoiceKeyUp -> mIsBleImageShowing:" + mIsBleImageShowing);
        if(!mIsBleImageShowing){
            zoomOutImage();
        }*/
    }

    @Override
    public void onBackKeyUp() {
        Log.d(TAG, "onBackKeyUp -> mIsBleImageShowing:" + mIsBleImageShowing);
        if(mIsBleImageShowing){
            zoomInImage();
            scanLeDevice(false);
        }
    }

    private final BroadcastReceiver mVoiceIRKeyReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.d(TAG, "mVoiceIRKeyReceiver -> mIsBleImageShowing:" + mIsBleImageShowing);
            if(!mIsBleImageShowing){
                zoomOutImage();
                scanLeDevice(true);
            }
        }
    };

    private void registerVoiceIRKeyReceiver(){
        IntentFilter filter = new IntentFilter();
        filter.addAction("com.rockchip.keystone.VOICE_IR_KEY");
        registerReceiver(mVoiceIRKeyReceiver, filter);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onDestroy() {
        unregisterReceiver(mBluetoothReceiver);
        unregisterReceiver(mVoiceIRKeyReceiver);
        super.onDestroy();
    }

    private final class InputDeviceServiceListener
            implements BluetoothProfile.ServiceListener {

        public void onServiceConnected(int profile, BluetoothProfile proxy) {
            Log.d(TAG, "Bluetooth service connected, profile is " + profile);

//            if(rc_exist)
//                return;
            if (proxy == null) {
                Log.w(TAG, "a_bin------------->>>>BluetoothProfile is null");
                return;
            }
            mService = (BluetoothHidDevice) proxy;


            getBondedDevices();
            Log.d(TAG, "================>>>>123");
        }

        public void onServiceDisconnected(int profile) {
            Log.d(TAG, "Bluetooth service disconnected");
            //mIsProfileReady=false;
            mService = null;
        }
    }

    private void getBondedDevices() {
        Set<BluetoothDevice> devices = mBluetoothAdapter.getBondedDevices();
        Log.d(TAG, "===========>>>devices.size():" + devices.size());
        if (devices.size() > 0) {
            for (Iterator<BluetoothDevice> iterator = devices.iterator(); iterator.hasNext(); ) {
                BluetoothDevice bluetoothDevice = (BluetoothDevice) iterator.next();
                Log.i(TAG, "BT devices: " + bluetoothDevice.getName() + " -> " + bluetoothDevice.getAddress());
                if (bluetoothDevice.getName() == null) {
                    Log.w(TAG, "device name is null, continue ...");
                    continue;
                }
                if (bluetoothDevice.getName().startsWith(Constans.BT_NAME)) {
                    Log.i(TAG, "Found paired RC");
                    //mLeDeviceListAdapter.addDevice(bluetoothDevice);
                    if ((mService.getConnectionState(bluetoothDevice) != BluetoothProfile.STATE_CONNECTED)
                            && (mService.getConnectionState(bluetoothDevice) != BluetoothProfile.STATE_CONNECTING)) {
                        doHogpConnect(bluetoothDevice);
                    }
                }
            }
        }
        //mLeDeviceListAdapter.notifyDataSetChanged();
    }

    private void toastRemoteState(int connectState){
        String msg = "";
        switch (connectState){
            case BluetoothProfile.STATE_CONNECTED:
                msg = getString(R.string.remote_connected);
                break;
            case BluetoothProfile.STATE_CONNECTING:
                msg = getString(R.string.remote_connecting);
                break;
            case BluetoothProfile.STATE_DISCONNECTED:
                msg = getString(R.string.remote_disconnected);
                break;
            default:
                msg = getString(R.string.remote_disconnected);
                break;
        }
        showToast(msg);
    }

    private boolean checkOurRemoteIsBonded(){
        boolean isPaired = false;
        Set<BluetoothDevice> devices = mBluetoothAdapter.getBondedDevices();
        if (devices.size() > 0) {
            for(Iterator<BluetoothDevice> iterator = devices.iterator();iterator.hasNext();){
                BluetoothDevice bluetoothDevice=(BluetoothDevice)iterator.next();
                Log.i(TAG, "BT devices: "+bluetoothDevice.getName() + " -> " + bluetoothDevice.getAddress());
                if(bluetoothDevice.getName().startsWith(Constans.BT_NAME)){
                    Log.i(TAG, "Found paired RC");
                    isPaired = true;
                    break;
                }
            }
        }
        return isPaired;
    }

    private void doHogpConnect(BluetoothDevice device) {
        //mDeviceMsgView.setText(getString(R.string.bt_connecting));
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
            mService.connect(device);
            Log.d(TAG, "a_bin------>>>connecting bt device");
        } else {
            Log.v(TAG, "Bluetooth HID serivce is not ready");
        }
    }

    private boolean isNameMatchextracName(byte[] scanRecord) {
        int i, length = scanRecord.length;
        i = 0;
        byte[] RcName = new byte[32];
        String decodedName = null;
        while (i < length - 2) {
            int element_len = scanRecord[i];
            byte element_type = scanRecord[i + 1];
            if (element_type == COMPLETE_NAME_FLAG) {
                System.arraycopy(scanRecord, i + 2, RcName, 0, element_len - 1);
                try {
                    decodedName = new String(RcName, 0, 16, "UTF-8");
                } catch (UnsupportedEncodingException e) {
                    e.printStackTrace();
                }
                Log.i(TAG, "decodedName: " + decodedName);
                //String MyRcName1 = new String("Bluetooth");
                if (decodedName.contains(Constans.BT_NAME)) {
                    Log.i(TAG, "We found our RC");
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
        if (isMyRc && isHogpDevice) {
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

    private BluetoothAdapter.LeScanCallback mLeScanCallback = new BluetoothAdapter.LeScanCallback() {
        @Override
        public void onLeScan(final BluetoothDevice device, final int rssi, final byte[] scanRecord) {
            if (isGoodHogpRc(rssi, scanRecord)) {

                if (mScanning) {
                    mBluetoothAdapter.stopLeScan(mLeScanCallback);
                    mScanning = false;
                }

                if (device.getBondState() == BluetoothDevice.BOND_NONE) {
                    if (!device.createBond()) {
                        Log.i(TAG, "onLeScan->Start bond failed=" + device);
                    }
                }
            }
        }
    };

    private void scanLeDevice(final boolean enable) {
        mHandler.removeCallbacks(mStopScanRunnable);
        mHandler.removeCallbacks(mScanRunnable);

        if (enable) {
            // Stops scanning after a pre-defined scan period.

            mHandler.post(mScanRunnable);
        } else {
            mHandler.post(mStopScanRunnable);
        }
    }

    private Runnable mScanRunnable = new Runnable() {
        @Override
        public void run() {
            Log.d(TAG, "mScanRunnable is running... mIsConnected:" + mIsConnected);
            if (mIsConnected) {
                Log.d(TAG, "is connected, do not scan!");
                return;
            }
            mHandler.removeCallbacks(mStopScanRunnable);
            mHandler.removeCallbacks(mScanRunnable);

            mHandler.postDelayed(mStopScanRunnable, SCAN_STOP_PERIOD);
            mBluetoothAdapter.startLeScan(mLeScanCallback);

            /*if(!mIsConnected){
                mBluetoothAdapter.startLeScan(mLeScanCallback);
                mScanning = true;
                Log.d(TAG, "ble scan started");
            }*/


        }
    };

    private Runnable mStopScanRunnable = new Runnable() {
        @Override
        public void run() {
            Log.d(TAG, "mStopScanRunnable is running... mIsConnected:" + mIsConnected);
            mHandler.postDelayed(mScanRunnable, SCAN_START_PERIOD);
            mBluetoothAdapter.stopLeScan(mLeScanCallback);

            /*if(!mIsConnected){
                mBluetoothAdapter.stopLeScan(mLeScanCallback);
                mScanning = false;
            }*/
            mHandler.removeCallbacks(mStopScanRunnable);
            mHandler.removeCallbacks(mScanRunnable);
            if(!mIsBleImageShowing){
                Log.d(TAG, "user cancel scan ...");
                return;
            }
            if (!mIsConnected)
                mHandler.postDelayed(mScanRunnable, SCAN_START_PERIOD);
        }
    };

    private int extractTxPower(byte[] scanRecord){
        int i, length=scanRecord.length;
        i = 0;

        while (i< length-2) {
            int element_len = scanRecord[i];
            byte element_type = scanRecord[i+1];
            if(element_type == 0x0a) {
                Log.i(TAG, "extractTxPower Bingo, we TX power=" + scanRecord[i+2]);
                return scanRecord[i+2];
            }
            i+= element_len+1;
        }

        return INVALID_TX_POWER;
    }

    /*lgh we only care 16bit UUID now*/
    private boolean containHogpUUID(byte[] scanRecord){
        int i, j, length=scanRecord.length;
        i = 0;
        int uuid = 0;
        while (i< length-2) {
            int element_len = scanRecord[i];
            byte element_type = scanRecord[i+1];
            if(element_type == UUID16_SERVICE_FLAG_MORE
                    ||element_type == UUID16_SERVICE_FLAG_COMPLETE ) {
                for(j=0; j<element_len-1;j++,j++)
                {
                    uuid = scanRecord[i+j+2]+(scanRecord[i+j+3]<<8);
                    //Log.i(TAG, "containHogpUUID Got UUID uuid=0x" + Integer.toHexString(uuid));
                    if (uuid == HOGP_UUID16) {
                        return true;
                    }
                }
            } else if (element_type >= UUID32_SERVICE_FLAG_MORE
                    && element_type >= UUID128_SERVICE_FLAG_COMPLETE){
                //Log.i(TAG, "Do not support parsing 32bit or 12bit UUID now");
            }
            i+= element_len+1;
        }

        return false;
    }

    private void showToast(String msg) {
        if (mToast == null) {
            mToast = Toast.makeText(this, msg, Toast.LENGTH_LONG);
        } else {
            mToast.setText(msg);
        }
        mToast.show();
    }
}
