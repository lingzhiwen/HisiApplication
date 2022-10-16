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

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothDevice;
import java.util.Iterator;
import java.util.Set;
import android.content.ComponentName;
import android.app.ActivityManager;
import java.util.List;

public class BootReceiver extends BroadcastReceiver {
    private String TAG = "BootReceiver_BLE";
    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        Log.d(TAG,"========action: " + intent.getAction());
//        if(intent.getAction().equals("android.intent.action.BOOT_COMPLETED")) {
//            startBleService(context);
//			/*boolean isPaired = false;
//			final BluetoothManager bluetoothManager =
//                (BluetoothManager) context.getSystemService(Context.BLUETOOTH_SERVICE);
//			BluetoothAdapter mBluetoothAdapter = bluetoothManager.getAdapter();
//			Set<BluetoothDevice> devices = mBluetoothAdapter.getBondedDevices();
//			if (devices.size()>0) {
//				for(Iterator<BluetoothDevice> iterator=devices.iterator();iterator.hasNext();){
//					BluetoothDevice bluetoothDevice=(BluetoothDevice)iterator.next();
//					Log.i(TAG, "BT devices: "+bluetoothDevice.getName() + " -> " + bluetoothDevice.getAddress());
//					if(bluetoothDevice.getName().startsWith(Constans.BT_NAME)){
//						Log.i(TAG, "Found paired RC");
//						isPaired = true;
//					}
//				}
//			}
//
//			if(!isPaired && !isAppRunning(context, "com.android.provision")){
//				//showBLE(context);
//				startBleService(context);
//			}*/
//        }
    }

    private void showBLE(Context context){
        Intent bintent = new Intent();
        bintent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        bintent.setComponent(new ComponentName("com.example.bluetooth.le", "com.example.bluetooth.le.DeviceScanActivity"));
        context.startActivity(bintent);
    }

    private void startBleService(Context context){
        Intent service = new Intent(context, BLEService.class);
        context.startService(service);
    }

    private static boolean isAppRunning(Context context, String packageName) {
        ActivityManager am = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        List<ActivityManager.RunningTaskInfo> list = am.getRunningTasks(100);
        if (list.size() <= 0) {
            return false;
        }
        for (ActivityManager.RunningTaskInfo info : list) {
            if (info.baseActivity.getPackageName().equals(packageName)) {
                return true;
            }
        }
        return false;
    }
}
