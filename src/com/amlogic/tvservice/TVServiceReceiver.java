package com.amlogic.tvservice;

import android.os.SystemProperties;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.os.PowerManager;



public class TVServiceReceiver extends BroadcastReceiver {
	static final String TAG = "TvServiceReceiver";
	private static final String ACTION_BOOT_COMPLETED ="android.intent.action.BOOT_COMPLETED";
	private static final String ACTION_BOOKING_WAKEUP = "com.amlogic.tvservice.booking_wakeup";
	private static PowerManager.WakeLock wakeLock = null;
	
	@Override
	public void onReceive(Context context, Intent intent) {
		if (ACTION_BOOT_COMPLETED.equals(intent.getAction())) {
			//if(!SystemProperties.getBoolean("persist.tv.first_start", true))
			{
				Log.d(TAG,"TVService Start*******************************************");
				context.startService(new Intent(context, TVService.class));			
			}
		}else if (ACTION_BOOKING_WAKEUP.equals(intent.getAction())){
			/* extra wakelock process, to keep the screen on */
			if (wakeLock != null){
				wakeLock.release();
				wakeLock = null;
			}
			
			PowerManager pm = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
			wakeLock = pm.newWakeLock(PowerManager.FULL_WAKE_LOCK |
				PowerManager.ACQUIRE_CAUSES_WAKEUP |
				PowerManager.ON_AFTER_RELEASE, "AlarmReceiver");
			wakeLock.acquire();
		}
	}
	
}
