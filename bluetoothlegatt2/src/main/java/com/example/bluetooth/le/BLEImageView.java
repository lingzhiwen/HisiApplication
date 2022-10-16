package com.example.bluetooth.le;

import android.annotation.SuppressLint;
import android.content.Context;
import android.util.AttributeSet;
import android.util.Log;
import android.view.KeyEvent;
import android.widget.ImageView;


@SuppressLint("AppCompatCustomView")
public class BLEImageView extends ImageView {
    private static final String TAG = BLEImageView.class.getName() + "-BLEService";

    public interface Callback{
        void onVoiceKeyUp();
        void onBackKeyUp();
    }

    private Callback mCallback;

    public void setCallback(Callback mCallback) {
        this.mCallback = mCallback;
    }

    public BLEImageView(Context context) {
        super(context);
        setImageResource(R.drawable.bootstrap_bg);
    }

    public BLEImageView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public BLEImageView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public BLEImageView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        Log.d(TAG, "onKeyDown got keyCode->" + keyCode);
        if((keyCode == KeyEvent.KEYCODE_F11) && (mCallback != null)){
            mCallback.onVoiceKeyUp();
        }else if((keyCode == KeyEvent.KEYCODE_BACK) && (mCallback != null)){
            mCallback.onBackKeyUp();
        }
        return super.onKeyDown(keyCode, event);
    }
}
