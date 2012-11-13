package com.amlogic.tvtest;

import android.util.Log;
import android.view.KeyEvent;
import android.widget.TextView;
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
	private int curTvMode = TVScanParams.TV_MODE_DTV;
	 private TextView  myTextView;
	public void onCreate(Bundle savedInstanceState){
		Log.d(TAG, "onCreate");

		super.onCreate(savedInstanceState);

		setContentView(R.layout.test);
		
		 myTextView   =  (TextView)findViewById(R.id.proname);
		 myTextView.setText(this.getResources().getString(R.string.warning));
		openVideo();
	}
	
	
	  @Override
	    protected void onPause() {
	        super.onPause();
	       
	    }
	

	public void onConnected(){
		Log.d(TAG, "connected");
		TVScanParams sp;
		//setInputSource(TVConst.SourceType.SOURCE_TYPE_ATV);
		
		if (curTvMode == TVScanParams.TV_MODE_DTV) {
			//sp = TVScanParams.atvManualScanParams(0, 144250000, 1);
			//sp = TVScanParams.atvAutoScanParams(0);
		//	Log.d(TAG, "Start Scan...");
			//startScan(sp);
			
			
		} else {
			//sp = TVScanParams.dtvAllbandScanParams(0, TVChannelParams.MODE_QAM);
			//sp = TVScanParams.dtvManualScanParams(0, TVChannelParams.dvbcParams(435000000, TVChannelParams.MODULATION_QAM_64, 6875000));
			//sp = TVScanParams.dtvManualScanParams(0, TVChannelParams.dvbtParams(474000000, TVChannelParams.BANDWIDTH_8_MHZ));
		}
		
		
		
		//Log.d(TAG, "Start Scan...");
		//startScan(sp);
	}

	public void onDisconnected(){
		Log.d(TAG, "disconnected");
	}

	
	  public boolean onKeyDown(int keyCode, KeyEvent event) {
		  TVProgram prog  = null;
	        switch (keyCode) {
	        case KeyEvent.KEYCODE_0:
            	Log.d(TAG, "00000000000000000000000000000000000 ");
            	 prog = TVProgram.selectByNumber(this, TVProgram.TYPE_TV, new TVProgramNumber(92));
				if(prog!=null){	
					playProgram(new TVProgramNumber(92));
					
					Log.d(TAG, "%%%00000000000000000000000000000000 ");
				
				}
				break;
	            case KeyEvent.KEYCODE_1:
	            	Log.d(TAG, "1111111111111111111111111111111111 ");
	            	 prog = TVProgram.selectByNumber(this, TVProgram.TYPE_TV, new TVProgramNumber(85));
					if(prog!=null){	
						playProgram(new TVProgramNumber(85));
						
						Log.d(TAG, "%%%1111111111111111111111111111111111 ");
					}
	                break;
	            case KeyEvent.KEYCODE_2:
	            	 setInputSource(TVConst.SourceType.SOURCE_TYPE_HDMI1);
	                break;
	           
	            	  
	    	  	case KeyEvent.KEYCODE_3:
	    	  		 setInputSource(TVConst.SourceType.SOURCE_TYPE_VGA);
	             break;
	    		case KeyEvent.KEYCODE_4:
	    			 setInputSource(TVConst.SourceType.SOURCE_TYPE_YPBPR1);
		         	
		             break;
	    		case KeyEvent.KEYCODE_5:
	    			 setInputSource(TVConst.SourceType.SOURCE_TYPE_AV1);
		             break;
		             
	    		case KeyEvent.KEYCODE_6:
		         	 prog = TVProgram.selectByNumber(this, TVProgram.TYPE_TV, new TVProgramNumber(9));
		    			if(prog!=null){	
		    				playProgram(new TVProgramNumber(9));
		    				Log.d(TAG, "22222222222222222222222222222222222 ");
		    			}
		         	
		             break;
		             
	    		case KeyEvent.KEYCODE_7:
	    			 setInputSource(TVConst.SourceType.SOURCE_TYPE_ATV);
		         	 //prog = TVProgram.selectByNumber(this, TVProgram.TYPE_TV, new TVProgramNumber(10));
		    		 //	if(prog!=null){	
		    		 //		playProgram(new TVProgramNumber(10));
		    		 //		Log.d(TAG, "22222222222222222222222222222222222 ");
		    		 //	}
		         	
		             break;
		             
	    		case KeyEvent.KEYCODE_8:
		         	 prog = TVProgram.selectByNumber(this, TVProgram.TYPE_TV, new TVProgramNumber(1));
		    			if(prog!=null){	
		    				Log.d(TAG, "22222222222222222222222222222222222 ");
		    				playProgram(new TVProgramNumber(1));
		    				
		    			}
		         	
		             break;
		             
	    		 case KeyEvent.KEYCODE_9:
	    				setInputSource(TVConst.SourceType.SOURCE_TYPE_DTV);
		            	//TVScanParams sp;
		            	//sp = TVScanParams.dtvManualScanParams(0, TVChannelParams.dvbcParams(259000000, TVChannelParams.MODULATION_QAM_64, 6875000));
		            	//startScan(sp);
		            	break;
	    		 case KeyEvent.KEYCODE_DPAD_UP:
	    			    TVScanParams sp;	
	    			    sp = TVScanParams.atvAutoScanParams(0);
	    				Log.d(TAG, "Start Scan...");
	    				startScan(sp);
	    			 break;
	    		 case KeyEvent.KEYCODE_DPAD_DOWN:
	    			 	Log.d(TAG, "stopScan");
	 					stopScan(true);
	    			 break;
	    	  
	    	}

	      
         
	        return super.onKeyDown(keyCode, event);
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
				if (curTvMode == TVScanParams.TV_MODE_ATV) {
					Log.d(TAG, "Scan update: frequency "+msg.getScanCurChanParams().getFrequency()+ " " + locked);
					try {
						Log.d(TAG, "Range: "+getConfig("tv:scan:atv:minfreq").getInt()+" ~ "+getConfig("tv:scan:atv:maxfreq").getInt());
					} catch (Exception e) {
						e.printStackTrace();
					}					
				} else {
					Log.d(TAG, "Scan update: frequency "+msg.getScanCurChanParams().getFrequency()+ " " + locked +
						", Channel "+(msg.getScanCurChanNo()+1)+"/"+msg.getScanTotalChanCount()+
						", Percent:"+msg.getScanProgress()+"%");
					if (msg.getScanProgramName() != null) {
						Log.d(TAG, "Scan update: new program >> "+ msg.getScanProgramName());
					}
				}
				break;
			case TVMessage.TYPE_SCAN_STORE_BEGIN:
				Log.d(TAG, "Storing ...");
				break;
			case TVMessage.TYPE_SCAN_STORE_END:
				Log.d(TAG, "Store Done !");
				TVProgram prog = TVProgram.selectByNumber(this, TVProgram.TYPE_TV, new TVProgramNumber(1));
				//if(prog!=null){	
				//		Log.d(TAG, "1111111111111111111111111111111111 ");
				//	playProgram(new TVProgramNumber(1));
				//}
				break;
			case TVMessage.TYPE_SCAN_END:
				Log.d(TAG, "Scan End");
				Log.d(TAG, "stopScan");
				stopScan(true);
				Log.d(TAG, "stopScan End");
			default:
				break;
	
		}
	}
}

