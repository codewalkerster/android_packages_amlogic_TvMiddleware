package com.amlogic.tvservice;

import com.amlogic.tvutil.TVChannelParams;
import com.amlogic.tvutil.TVProgram;
import com.amlogic.tvutil.TVConst;
import com.amlogic.tvutil.DTVPlaybackParams;
import com.amlogic.tvutil.DTVRecordParams;
import com.amlogic.tvutil.TvinInfo;

import java.io.File;



abstract public class TVDevice  implements TVConfig.Update,TVConfig.Read{
	public class Event{
		public static final int EVENT_SET_INPUT_SOURCE_OK     = 0;
		public static final int EVENT_SET_INPUT_SOURCE_FAILED = 1;
		public static final int EVENT_VCHIP_BLOCKED           = 2;
		public static final int EVENT_VCHIP_UNBLOCKED         = 3;
		public static final int EVENT_FRONTEND                = 4;
		public static final int EVENT_DTV_NO_DATA             = 5;
		public static final int EVENT_DTV_CANNOT_DESCRAMLE    = 6;
		public static final int EVENT_RECORD_END              = 7;
		public static final int EVENT_VGA_ADJUST_STATUS       = 8;

		
		
		public int                                type;
		public TVChannelParams                    feParams;
		public int                                feStatus;
		public int                                recEndCode;
		public DTVRecordParams                    recParams;
		public int                                source;
		public TVConst.VGA_ADJUST_STATUS          vga_adjust_status;

		public Event(int type){
			this.type = type;
		}
	}

	public TVDevice(){
	}

	
	abstract public void setInputSource(TVConst.SourceInput source);

	abstract public TVConst.SourceInput getCurInputSource();

	abstract public void setVideoWindow(int x, int y, int w, int h);

	abstract public void setFrontend(TVChannelParams params);

	abstract public TVChannelParams getFrontend();

	abstract public int getFrontendStatus();

	abstract public int getFrontendSignalStrength();

	abstract public int getFrontendSNR();

	abstract public int getFrontendBER();

	abstract public void freeFrontend();

	abstract public void startVBI(int flags);

	abstract public void stopVBI(int flags);

	abstract public void playATV();

	abstract public void resetATVFormat(TVChannelParams params);

	abstract public void stopATV();

	abstract public void playDTV(int vpid, int vfmt, int apid, int afmt);

	abstract public void switchDTVAudio(int pid, int afmt);

	abstract public void stopDTV();

	abstract public void startRecording(DTVRecordParams params);

	abstract public void stopRecording();
	
	abstract public DTVRecordParams getRecordingParams();

	abstract public void startTimeshifting(DTVPlaybackParams params);

	abstract public void stopTimeshifting();

	abstract public void startPlayback(DTVPlaybackParams params);

	abstract public void stopPlayback();
	
	abstract public DTVPlaybackParams getPlaybackParams();

	abstract public void pause();

	abstract public void resume();

	abstract public void fastForward(int speed);

	abstract public void fastBackward(int speed);

	abstract public void seekTo(int pos);

	abstract public void onEvent(Event event);
	
	abstract public void ATVChannelFineTune(int fre);
	
	abstract public void setVGAAutoAdjust();

    public int GetSrcInputType()
    {
        return 0;
    }

    public TvinInfo GetCurrentSignalInfo()
	{
		return null;
	}
}

