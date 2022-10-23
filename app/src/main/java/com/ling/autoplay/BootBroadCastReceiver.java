package com.ling.autoplay;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class BootBroadCastReceiver extends BroadcastReceiver {
    public static final String ACTION = "android.intent.action.BOOT_COMPLETED";

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent.getAction().equals(ACTION)) {
            Log.i("PlayActivity", "onReceive");
//            Intent playIntent = new Intent(context, Videoplayer.class);
//            playIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
//            context.startActivity(playIntent);
        }
    }
}
