package com.amlogic.tvtest;

import android.util.Log;
import android.view.KeyEvent;
import android.widget.TextView;
import android.os.Bundle;
import android.graphics.Rect;
import com.amlogic.tvutil.TVMessage;
import com.amlogic.tvutil.TVConst;
import com.amlogic.tvutil.TVProgram;
import com.amlogic.tvutil.TVProgramNumber;
import com.amlogic.tvactivity.TVActivity;
import com.amlogic.tvutil.TVChannelParams;
import com.amlogic.tvutil.TVScanParams;
import com.amlogic.tvutil.TVConst;
import com.amlogic.tvutil.TVConst.CC_ATV_AUDIO_STANDARD;
import com.amlogic.tvutil.TVConst.CC_ATV_VIDEO_STANDARD;
import com.amlogic.tvutil.TVEvent;
import java.text.SimpleDateFormat;


public class TVTest extends TVActivity{
	private static final String TAG="TVTest";
	private int curTvMode = TVScanParams.TV_MODE_ATV;
	 private TextView  myTextView;
	 private TextView  myTextView_number;
	public void onCreate(Bundle savedInstanceState){
		Log.d(TAG, "onCreate");

		super.onCreate(savedInstanceState);
		setContentView(R.layout.test);
		 myTextView   =  (TextView)findViewById(R.id.proname);
		 myTextView.setText(this.getResources().getString(R.string.warning));

		openVideo();
		 myTextView_number  =  (TextView)findViewById(R.id.proname1);
	}
	
	
	  @Override
	    protected void onPause() {
	        super.onPause();
	       
	    }
	

	public void onConnected(){
		Log.d(TAG, "connected");

		TVScanParams sp;

		setVideoWindow(new Rect(100, 100, 500, 500));

		//setInputSource(TVConst.SourceType.SOURCE_TYPE_ATV);
		
		if (curTvMode == TVScanParams.TV_MODE_ATV) {
		
		} else {
			//sp = TVScanParams.dtvAllbandScanParams(0, TVChannelParams.MODE_QAM);
			//sp = TVScanParams.dtvManualScanParams(0, TVChannelParams.dvbcParams(474000000, TVChannelParams.MODULATION_QAM_64, 6875000));
			//sp = TVScanParams.dtvManualScanParams(0, TVChannelParams.dvbtParams(474000000, TVChannelParams.BANDWIDTH_8_MHZ));
		}
	
		//Log.d(TAG, "Start Scan...");
		//startScan(sp);
	}

	public void onDisconnected(){
		Log.d(TAG, "disconnected");
	}
	
	private void printEvent(String strCag, TVEvent evt) {
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm");
		String str_start = sdf.format(evt.getStartTime()*1000);
		String str_end = sdf.format(evt.getEndTime()*1000);
		Log.d(TAG, strCag+" "+str_start+"  ~  "+str_end+"  "+evt.getName());
	}
	
	private void showProgramEPG(TVProgram prog) {
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm");
		String str_utc = sdf.format(getUTCTime());
		Log.d(TAG, "current UTC time: "+str_utc);
		TVEvent evt = prog.getPresentEvent(this, getUTCTime());
		if (evt != null) {
			printEvent("Present    ", evt);
		}
		evt = prog.getFollowingEvent(this, getUTCTime());
		if (evt != null) {
			printEvent("Following  ", evt);
		}
		/* 24 hours schedule EPG */
		TVEvent[] evts = prog.getScheduleEvents(this, getUTCTime(), (long)1*24*3600*1000);
		if (evts != null && evts.length > 0) {
			for (int i=0; i<evts.length; i++) {
				printEvent("Schedule",evts[i]);
			}
		}		
	}

	int count = 0;
	  public boolean onKeyDown(int keyCode, KeyEvent event) {
		  TVProgram prog  = null;
		  TVScanParams sp;	
	        switch (keyCode) {
	           case KeyEvent.KEYCODE_0:
            	 ttGotoNextPage();
				break;
	            case KeyEvent.KEYCODE_1:
	            if(!isInTeletextMode())
		    		 	 ttShow();
					 else
					 	 ttHide();
	                break;
	            case KeyEvent.KEYCODE_2:
	            	 setInputSource(TVConst.SourceInput.SOURCE_HDMI1);
	                break;
	           
	            	  
	    	  	case KeyEvent.KEYCODE_3:
	    	  		 setInputSource(TVConst.SourceInput.SOURCE_VGA);
	             break;
	    		case KeyEvent.KEYCODE_4:
	    			 setInputSource(TVConst.SourceInput.SOURCE_YPBPR1);
		         	
		             break;
	    		case KeyEvent.KEYCODE_5:
	    			 setInputSource(TVConst.SourceInput.SOURCE_AV1);
		             break;
		             
	    		case KeyEvent.KEYCODE_6:
		         	 /*prog = TVProgram.selectByNumber(this, TVProgram.TYPE_TV, new TVProgramNumber(9));
		    			if(prog!=null){	
		    				playProgram(new TVProgramNumber(9));
		    				Log.d(TAG, "22222222222222222222222222222222222 ");
		    			}*/
		         	 // ttGotoPreviousPage();
					 prog = TVProgram.selectByNumber(this, TVProgram.TYPE_ATV, new TVProgramNumber(count));
					 if(prog!=null){	
								
						 TVChannelParams params = prog.getChannel().getParams();
						 int std = params.getStandard();
						 Log.v(TAG,"std" + std);
						 CC_ATV_AUDIO_STANDARD audio_std = TVChannelParams.AudioStd2Enum(std);
						 CC_ATV_VIDEO_STANDARD vidio_std = TVChannelParams.VideoStd2Enum(std);
						 Log.v(TAG,"audio_std" + audio_std.ordinal()  +  "vidio_std" + vidio_std.ordinal() );
						 audio_std = CC_ATV_AUDIO_STANDARD.CC_ATV_AUDIO_STD_BG; 
						 int changeSTD = TVChannelParams.getTunerStd(vidio_std.ordinal(),audio_std.ordinal()) ;
						  Log.v(TAG,"changeSTD" + changeSTD );
				
					 	}
					 count ++;
		             break;
		             
	    		case KeyEvent.KEYCODE_7:
	    			Log.v(TAG,"setInputSource SOURCE_ATV)");
	    			 setInputSource(TVConst.SourceInput.SOURCE_ATV);
		         	
		             break;
		             
	    		case KeyEvent.KEYCODE_8:
		         	 boolean sub = getBooleanConfig("tv:subtitle:enable");
		         	 Log.d(TAG, "reset tv:subtitle:enable "+ sub + "->"+!sub);
		         	 sub = !sub;
		         	 setConfig("tv:subtitle:enable", sub);
		             break;
		             
	    		 case KeyEvent.KEYCODE_9:
	    				setInputSource(TVConst.SourceInput.SOURCE_DTV);
		            	break;
		         case KeyEvent.KEYCODE_DPAD_LEFT:
		        	
		        	 sp = TVScanParams.dtvManualScanParams(0, TVChannelParams.dvbcParams(435000000, TVChannelParams.MODULATION_QAM_64, 6875000));
		        	 Log.d(TAG, "Start Scan...dvb-c");
	    			 startScan(sp);
		        	 break;
		         case KeyEvent.KEYCODE_DPAD_RIGHT:
		        	 sp = TVScanParams.dtvManualScanParams(0, TVChannelParams.dvbtParams(474000000, TVChannelParams.BANDWIDTH_8_MHZ));
		        	 Log.d(TAG, "Start Scan...dvb-t");
					 startScan(sp);
		        	 break;
	    		 case KeyEvent.KEYCODE_DPAD_UP:
	    			    sp = TVScanParams.atvAutoScanParams(0);
	    			    //sp = TVScanParams.dtvManualScanParams(0, TVChannelParams.dvbtParams(474000000, TVChannelParams.BANDWIDTH_8_MHZ));
	    			    //sp = TVScanParams.dtvManualScanParams(0, TVChannelParams.dvbcParams(474000000, TVChannelParams.MODULATION_QAM_64, 6875000));
	    				Log.d(TAG, "Start Scan...atv");
	    				startScan(sp);
	    			 break;
	    		 case KeyEvent.KEYCODE_DPAD_DOWN:
	    			 	Log.d(TAG, "stopScan");
	 					stopScan(true);
	    			 break;
	    			 
	    		 case KeyEvent.KEYCODE_CHANNEL_UP:
	    			 	mychannelUp();
	    			 break;
	    		 case KeyEvent.KEYCODE_CHANNEL_DOWN:
	    				mychannelDown();
	    			 break;
	    	}

	      
         
	        return super.onKeyDown(keyCode, event);
	    }
	  
	  int dtv_prog_number = 0; 
	  int atv_prog_number = 0;
	  void mychannelUp(){
		  Log.d(TAG, "channelUp");
			TVConst.SourceInput sinput  = this.getCurInputSource();
			Log.d(TAG, " ************" + sinput.ordinal());
			
			if(sinput == TVConst.SourceInput.SOURCE_DTV){
				TVProgram prog  = null;
				Log.d(TAG, "mychannelUp "+dtv_prog_number);
				prog = TVProgram.selectByNumber(this, TVProgram.TYPE_TV, new TVProgramNumber(dtv_prog_number));
				if(prog!=null){	
					playProgram(new TVProgramNumber(dtv_prog_number));
					Log.d(TAG, "************play program");
					showProgramEPG(prog);
				}
				myTextView_number.setText(" "+ sinput.ordinal() + dtv_prog_number);
				dtv_prog_number ++ ;
			}else
			if(sinput == TVConst.SourceInput.SOURCE_ATV){
				TVProgram prog  = null;
				Log.d(TAG, "mychannelUp "+atv_prog_number);
				prog = TVProgram.selectByNumber(this, TVProgram.TYPE_ATV, new TVProgramNumber(atv_prog_number));
				if(prog!=null){	
					playProgram(new TVProgramNumber(atv_prog_number));
					Log.d(TAG, "************play program");
				}
				myTextView_number.setText(" "+ sinput.ordinal() + atv_prog_number);
				atv_prog_number ++ ;
			}
			
		
	  }
	  
	  void mychannelDown(){
		  Log.d(TAG, "mychannelDown");
			TVConst.SourceInput sinput  = this.getCurInputSource();
			Log.d(TAG, " ************" + sinput.ordinal());
			
			if(sinput == TVConst.SourceInput.SOURCE_DTV){
				
				TVProgram prog  = null;
				Log.d(TAG, "mychannelUp "+dtv_prog_number);
				prog = TVProgram.selectByNumber(this, TVProgram.TYPE_TV, new TVProgramNumber(dtv_prog_number));
				if(prog!=null){	
					playProgram(new TVProgramNumber(dtv_prog_number));
					Log.d(TAG, "************play program");
					showProgramEPG(prog);
				}
				myTextView_number.setText(" "+ sinput.ordinal() + dtv_prog_number);
				if(dtv_prog_number > 1)
				dtv_prog_number -- ;
			}else
			if(sinput == TVConst.SourceInput.SOURCE_ATV){
				TVProgram prog  = null;
				Log.d(TAG, "mychannelUp "+atv_prog_number);
				prog = TVProgram.selectByNumber(this, TVProgram.TYPE_ATV, new TVProgramNumber(atv_prog_number));
				if(prog!=null){	
					playProgram(new TVProgramNumber(atv_prog_number));
					Log.d(TAG, "%%%00000000000000000000000000000000 ");
					showProgramEPG(prog);
				}
				myTextView_number.setText(" "+ sinput.ordinal() + atv_prog_number);
				if(atv_prog_number > 1)
					atv_prog_number -- ;
			}
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

				TVProgram plist[] = TVProgram.selectAll(this, false);
				for(int i=0; i<plist.length; i++){
					TVProgram p = plist[i];
					Log.d(TAG, "program "+p.getNumber().getNumber()+": "+p.getName()+"("+p.getType()+")");
				}

				TVProgram prog = TVProgram.selectFirstValid(this, TVProgram.TYPE_TV);
				if(prog != null)
					playProgram(prog.getNumber());
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

