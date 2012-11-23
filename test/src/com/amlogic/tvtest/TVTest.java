package com.amlogic.tvtest;

import android.util.Log;
import android.os.Bundle;
import com.amlogic.tvutil.TVMessage;
import com.amlogic.tvutil.TVConst;
import com.amlogic.tvutil.TVProgram;
import com.amlogic.tvutil.TVProgramNumber;
import com.amlogic.tvactivity.TVActivity;
import com.amlogic.tvutil.TVChannelParams;
import com.amlogic.tvutil.TVScanParams;
import com.amlogic.tvutil.TVConst;

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

		//TVScanParams sp = TVScanParams.dtvAllbandScanParams(0, TVChannelParams.MODE_QAM);
		//TVScanParams sp = TVScanParams.dtvManualScanParams(0, TVChannelParams.dvbcParams(474000000, TVChannelParams.MODULATION_QAM_64, 6875000));
		TVScanParams sp = TVScanParams.dtvManualScanParams(0, TVChannelParams.dvbtParams(474000000, TVChannelParams.BANDWIDTH_8_MHZ));

		setInputSource(TVConst.SourceType.SOURCE_TYPE_DTV);
		Log.d(TAG, "Start Scan...");
		startScan(sp);
	}

	public void onDisconnected(){
		Log.d(TAG, "disconnected");
	}

	public void onMessage(TVMessage msg){
		Log.d(TAG, "message "+msg.getType());
		switch (msg.getType()) {
			case TVMessage.TYPE_SCAN_PROGRESS:
				String locked;
				if (msg.getScanCurChanLockStatus()!=0) {
					locked = "Locked!";
				} else {
					locked = "Unlocked!";
				}
				Log.d(TAG, "Scan update: frequency "+msg.getScanCurChanParams().getFrequency()+ " " + locked +
					", Channel "+(msg.getScanCurChanNo()+1)+"/"+msg.getScanTotalChanCount()+
					", Percent:"+msg.getScanProgress()+"%");
				if (msg.getScanProgramName() != null) {
					Log.d(TAG, "Scan update: new program >> "+ msg.getScanProgramName());
				}
				if (msg.getScanProgress() == 100) {
					Log.d(TAG, "stopScan");
					stopScan(true);
					Log.d(TAG, "stopScan End");
				}
				break;
			case TVMessage.TYPE_SCAN_STORE_BEGIN:
				Log.d(TAG, "Storing ...");
				break;
			case TVMessage.TYPE_SCAN_STORE_END:
				Log.d(TAG, "Store Done !");
				TVProgram prog = TVProgram.selectByNumber(this, TVProgram.TYPE_TV, new TVProgramNumber(1));
				if(prog!=null){
					playProgram(new TVProgramNumber(1));
				}
				break;
			default:
				break;
	
		}
	}
}

