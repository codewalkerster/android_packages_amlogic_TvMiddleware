package com.amlogic.tvservice;

import android.os.SystemProperties;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;



public class TVServiceReceiver extends BroadcastReceiver {
	static final String TAG = "TvServiceReceiver";
	private static final String ACTION_BOOT_COMPLETED ="android.intent.action.BOOT_COMPLETED";
	@Override
	public void onReceive(Context context, Intent intent) {
 
		if (ACTION_BOOT_COMPLETED.equals(intent.getAction())) {
            if(!SystemProperties.getBoolean("persist.tv.first_start", true))
  			{
  				Log.d(TAG,"TVService Start*******************************************");
  				context.startService(new Intent(context, TVService.class));			
			}
        }
	}
	
}
