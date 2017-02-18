package com.martianLife.bleNative;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import com.facebook.react.HeadlessJsTaskService;

/**
 * Created by Administrator on 2017/1/20.
 */
public class BleBroadcastReceiver extends BroadcastReceiver {
    private static final String TAG = BleNative.class.getSimpleName();
    @Override
    public void onReceive(Context context, Intent intent) {
        Intent intent2 = new Intent(context, BleEventListener.class);
        intent2.putExtras(intent.getExtras());
        Log.w(TAG, "BleBroadcastReceiver ONRECEIVE");
        /*context.startService(intent2);
        HeadlessJsTaskService.acquireWakeLockNow(context);*/
    }
}
