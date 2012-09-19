package com.amlogic.tvtest;

import android.util.Log;
import android.os.Bundle;
import com.amlogic.tvutil.TVMessage;
import com.amlogic.tvactivity.TVActivity;

public class TVTest extends TVActivity{
	private static final String TAG="TVTest";

	public void onCreate(Bundle savedInstanceState){
		Log.d(TAG, "onCreate");

		super.onCreate(savedInstanceState);

		setContentView(R.layout.test);

		openVideo();
	}

	public void onConnected(){
		Log.d(TAG, "connected");
	}

	public void onDisconnected(){
		Log.d(TAG, "disconnected");
	}

	public void onMessage(TVMessage msg){
		Log.d(TAG, "message "+msg.getType());
	}
}

