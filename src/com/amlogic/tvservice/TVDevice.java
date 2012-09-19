package com.amlogic.tvservice;

import com.amlogic.tvutil.TVChannelParams;
import com.amlogic.tvutil.TVProgram;
import com.amlogic.tvutil.TVConst;
import java.io.File;

abstract public class TVDevice{

	public class DTVRecordParams{
		public File file;
		public TVProgram.Video video;
		public TVProgram.Audio audio[];
		public TVProgram.Subtitle subtitle[];
		public TVProgram.Teletext teletext[];
		public int currAudio;
		public int currSubtitle;
		public int currTeletext;
		public int recTotalSize;
		public int recTotalTime;
		public int recCurrPos;
	}

	public class Event{
		public static final int EVENT_SET_INPUT_SOURCE_OK     = 0;
		public static final int EVENT_SET_INPUT_SOURCE_FAILED = 1;
		public static final int EVENT_VCHIP_BLOCKED           = 2;
		public static final int EVENT_VCHIP_UNBLOCKED         = 3;
		public static final int EVENT_FRONTEND                = 4;
		public static final int EVENT_DTV_NO_DATA             = 5;
		public static final int EVENT_DTV_CANNOT_DESCRAMLE    = 6;
		public static final int EVENT_RECORD                  = 7;

		public int             type;
		public TVChannelParams feParams;
		public int             feStatus;
		public DTVRecordParams recParams;
	}

	public void setInputSource(TVConst.SourceType source){
	}

	public void setFrontend(TVChannelParams params){
	}

	public void getFrontend(TVChannelParams params){
	}

	public int getFrontendStatus(){
		return 0;
	}

	public int getFrontendSignalStrength(){
		return 0;
	}

	public int getFrontendSNR(){
		return 0;
	}

	public int getFrontendBER(){
		return 0;
	}

	public void freeFrontend(){
	}

	public void startVBI(int flags){
	}

	public void stopVBI(int flags){
	}

	public void playATV(){
	}

	public void stopATV(){
	}

	public void playDTV(int vpid, int vfmt, int apid, int afmt){
	}

	public void stopDTV(){
	}

	public void startRecording(DTVRecordParams params){
	}

	public DTVRecordParams stopRecording(){
		return null;
	}

	public void startTimeshifting(DTVRecordParams params){
	}

	public DTVRecordParams stopTimeshifting(){
		return null;
	}

	public void startPlayback(DTVRecordParams params){
	}

	public void stopPlayback(){
	}

	public void pause(){
	}

	public void resume(){
	}

	public void fastForward(int speed){
	}

	public void fastBackward(int speed){
	}

	public void seekTo(int pos){
	}

	abstract public void onEvent(Event event);
}

