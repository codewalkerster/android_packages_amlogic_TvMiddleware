package com.amlogic.tvactivity;

import java.io.BufferedWriter;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.FileReader;

import android.app.Activity;
import android.os.Handler;
import android.os.Message;
import android.os.Looper;
import android.os.Bundle;
import android.widget.VideoView;
import android.view.View;
import android.view.ViewGroup;
import android.view.SurfaceHolder;
import android.graphics.Canvas;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.util.Log;
import java.lang.StringBuilder;
import com.amlogic.tvclient.TVClient;
import com.amlogic.tvutil.TVConst;
import com.amlogic.tvutil.TVProgramNumber;
import com.amlogic.tvutil.TVProgram;
import com.amlogic.tvutil.TVChannel;
import com.amlogic.tvutil.TVChannelParams;
import com.amlogic.tvutil.TVPlayParams;
import com.amlogic.tvutil.TVScanParams;
import com.amlogic.tvutil.TVMessage;
import com.amlogic.tvutil.TVConfigValue;
import com.amlogic.tvutil.TVStatus;
import com.amlogic.tvutil.DTVPlaybackParams;
import com.amlogic.tvutil.DTVRecordParams;
import com.amlogic.tvsubtitle.TVSubtitleView;
import com.amlogic.tvutil.TvinInfo;

/**
 *TV Activity
 *此类包含TVClient和字幕,TV播放等TV基本功能.
 *是TV应用Activity的父类.
 */
abstract public class TVActivity extends Activity
{
    private static final String TAG = "TVActivity";

	private static final int SUBTITLE_NONE = 0;
    private static final int SUBTITLE_SUB  = 1;
    private static final int SUBTITLE_TT   = 2;

    private VideoView videoView;
    private TVSubtitleView subtitleView;
	private boolean connected = false;
	private boolean externalVideoView = false;
	private boolean externalSubtitleView = false;
	private boolean subtitleViewActive=false;

    private int currSubtitleMode = SUBTITLE_NONE;
    private int currSubtitlePID = -1;
    private int currTeletextPID = -1;
    private int currSubtitleID1 = -1;
    private int currSubtitleID2 = -1;

    private TVClient client = new TVClient() {
        public void onConnected() {
        	connected = true;
        	initSubtitle();
        	updateVideoWindow();
        	TVActivity.this.onConnected();
        }

        public void onDisconnected() {
        	connected = false;
        	TVActivity.this.onDisconnected();
        }

        public void onMessage(TVMessage m) {
        	solveMessage(m);
        	TVActivity.this.onMessage(m);
        }
    };

	protected void onPause(){
		Log.d(TAG, "onPause");
		subtitleViewActive=false;
		if(subtitleView != null){
			subtitleView.setActive(false);
			if(getBooleanConfig("tv:subtitle:enable"))
				subtitleView.hide();
			
		}
		super.onPause();
	}

	protected void onResume(){
		Log.d(TAG, "onResume");
		
        	super.onResume();
		subtitleViewActive=true;
		updateVideoWindow();
		if(subtitleView != null){
			subtitleView.setActive(true);	
			if(getBooleanConfig("tv:subtitle:enable"))
				subtitleView.show();
		}
	}
	
    public void onCreate(Bundle savedInstanceState) {
        Log.d(TAG, "onCreate");
        super.onCreate(savedInstanceState);
        client.connect(this);
    }

    @Override
    protected void onDestroy() {
	Log.d(TAG, "onDestroy");

	if(subtitleView != null) {
		unregisterConfigCallback("tv:subtitle:enable");
		unregisterConfigCallback("tv:subtitle:language");
		unregisterConfigCallback("tv:teletext:language");
		unregisterConfigCallback("tv:atsc:cc:caption");
		unregisterConfigCallback("tv:atsc:cc:foregroundcolor");
		unregisterConfigCallback("tv:atsc:cc:foregroundopacity");
		unregisterConfigCallback("tv:atsc:cc:backgroundcolor");
		unregisterConfigCallback("tv:atsc:cc:backgroundopacity");
		unregisterConfigCallback("tv:atsc:cc:fontstyle");
		unregisterConfigCallback("tv:atsc:cc:fontsize");
		unregisterConfigCallback("tv:atsc:cc:enable");
		subtitleView.dispose();
		subtitleView = null;
	}

	client.disconnect(this);
	super.onDestroy();
    }
	
	private int getTeletextRegionID(String ttxRegionName){
		final String[] supportedRegions= {"English", "Deutsch", "Svenska/Suomi/Magyar",
                                          "Italiano", "Français", "Português/Español", 
                                          "Cesky/Slovencina", "Türkçe", "Ellinika","Alarabia / English"};
		final int[] regionIDMaps = {16, 17, 18, 19, 20, 21, 14, 22, 55 , 64};

		int i;
		for (i=0; i<supportedRegions.length; i++){
			if (supportedRegions[i].equals(ttxRegionName))
				break;
		}

		if (i >= supportedRegions.length){
			Log.d(TAG, "Teletext defaut region " + ttxRegionName + 
				" not found, using 'English' as default!");
			i = 0;
		}

		Log.d(TAG, "Teletext default region id: " + regionIDMaps[i]);
		return regionIDMaps[i];
	}

	private void resetProgramCC(TVProgram prog){
		subtitleView.stop();

		if (! getBooleanConfig("tv:atsc:cc:enable")){
			Log.d(TAG, "CC is disabled !");
			return;
		}
		
		if (prog.getType() == TVProgram.TYPE_ATV){

		}else{
			TVSubtitleView.DTVCCParams subp;
			
			subp = new TVSubtitleView.DTVCCParams(
				getIntConfig("tv:atsc:cc:caption"),
				getIntConfig("tv:atsc:cc:foregroundcolor"),
				getIntConfig("tv:atsc:cc:foregroundopacity"),
				getIntConfig("tv:atsc:cc:backgroundcolor"),
				getIntConfig("tv:atsc:cc:backgroundopacity"),
				getIntConfig("tv:atsc:cc:fontstyle"),
				getIntConfig("tv:atsc:cc:fontsize")
				);
			subtitleView.setSubParams(subp);
		}

		subtitleView.startSub();
		subtitleView.show();
	}

	private void resetSubtitle(int mode){
		resetSubtitle(mode, -1);
	}

    private void resetSubtitle(int mode, int sub_id){
		if(subtitleView == null)
			return;

		if(mode == SUBTITLE_NONE){
			subtitleView.stop();
			subtitleView.hide();

			currSubtitleMode = mode;
			currSubtitlePID = -1;
			currSubtitleID1 = -1;
			currSubtitleID2 = -1;
			return;
		}

		int prog_id = client.getCurrentProgramID();

		Log.d(TAG, "reset subtitle, current program id " + prog_id);
		if(prog_id == -1)
			return;

		TVProgram prog = TVProgram.selectByID(this, prog_id);
		if(prog == null)
			return;

		int pid = -1, id1 = -1, id2 = -1, pm = -1;

		if(mode == SUBTITLE_SUB){
			if (! getStringConfig("tv:dtv:mode").equals("atsc")){
	    		TVProgram.Subtitle sub = null;
	    		
	    		if(sub_id >= 0){
	    			sub = prog.getSubtitle(sub_id);
					if (sub != null){
						prog.setCurrentSubtitle(sub_id);
					}
				}else{
					int sub_idx = prog.getCurrentSubtitle(getStringConfig("tv:subtitle:language"));
					if (sub_idx >= 0){
						sub = prog.getSubtitle(sub_idx);
					}
				}

	    		
				if(sub == null){
					resetSubtitle(SUBTITLE_NONE);
					return;
				}

	       		switch(sub.getType()){
					case TVProgram.Subtitle.TYPE_DVB_SUBTITLE:
						pid = sub.getPID();
						id1 = sub.getCompositionPageID();
						id2 = sub.getAncillaryPageID();
						pm  = SUBTITLE_SUB;
						break;
					case TVProgram.Subtitle.TYPE_DTV_TELETEXT:
						pid = sub.getPID();
						id1 = sub.getMagazineNumber();
						id2 = sub.getPageNumber();
						pm  = SUBTITLE_TT;
						break;
				}
			}else{
				resetProgramCC(prog);
				return;
			}
		}else if(mode == SUBTITLE_TT){
			TVProgram.Teletext tt = null;
			
			if(sub_id >= 0){
				tt = prog.getTeletext(sub_id);
				if (tt != null){
					prog.setCurrentTeletext(sub_id);
				}
			}else{
				int tt_idx = prog.getCurrentTeletext(getStringConfig("tv:teletext:language"));
				if (tt_idx >= 0){
					tt = prog.getTeletext(tt_idx);
				}
			}

			if(tt == null)
				return;

			pid = tt.getPID();
			id1 = tt.getMagazineNumber();
			id2 = tt.getPageNumber();
			pm  = SUBTITLE_TT;
		}

		if(mode == currSubtitleMode && pid == currSubtitlePID && id1 == currSubtitleID1 && id2 == currSubtitleID2){
			return;
		}

		subtitleView.stop();

		if(pm == SUBTITLE_TT && pid != currTeletextPID){
			subtitleView.clear();
		}

		TVSubtitleView.DVBSubParams subp;
		TVSubtitleView.DTVTTParams ttp;
		int dmx_id = (getPlaybackParams() != null) ? 1 : 0;

		if(pm == SUBTITLE_SUB){
			subp = new TVSubtitleView.DVBSubParams(dmx_id, pid, id1, id2);
			subtitleView.setSubParams(subp);
		}else{
			int pgno;

			pgno = (id1==0) ? 800 : id1*100;
			pgno += (id2 & 15) + ((id2 >> 4) & 15) * 10 + ((id2 >> 8) & 15) * 100;
			ttp = new TVSubtitleView.DTVTTParams(dmx_id, pid, pgno, 0x3F7F, 
				getTeletextRegionID(getStringConfig("tv:teletext:region")));

			if(mode == SUBTITLE_SUB){
				subtitleView.setSubParams(ttp);
			}else{
				subtitleView.setTTParams(ttp);
			}
		}

		boolean show_flag = true;
		if(mode == SUBTITLE_SUB){
			if(getBooleanConfig("tv:subtitle:enable"))
				show_flag=true;
			else
				show_flag=false;
			
			subtitleView.startSub();
		}else{
			subtitleView.startTT();
			show_flag=true;
		}

		if(show_flag)
			subtitleView.show();
		else 
			subtitleView.hide();

		currSubtitleMode = mode;
		currSubtitlePID  = pid;
		currSubtitleID1  = id1;
		currSubtitleID2  = id2;

		if(pm == SUBTITLE_TT)
			currTeletextPID = pid;
	}

	/*On program started*/
    private void onProgramStart(int prog_id){
    	TVProgram prog;

    	Log.d(TAG, "onProgramStart");
		
		/*Start subtitle*/
		resetSubtitle(SUBTITLE_SUB);
	}

	/*On program stopped*/
	private void onProgramStop(int prog_id){
		Log.d(TAG, "onProgramStop");

    	/*Stop subtitle.*/
    	resetSubtitle(SUBTITLE_NONE);
    	currTeletextPID = -1;
	}

	/*On playback started*/
    private void onPlaybackStart(){
    	TVProgram prog;

    	Log.d(TAG, "onPlaybackStart");
		
		/*Start subtitle*/
		resetSubtitle(SUBTITLE_SUB);
	}

	/*On playback stopped*/
	private void onPlaybackStop(){
		Log.d(TAG, "onPlaybackStop");

    	/*Stop subtitle.*/
    	resetSubtitle(SUBTITLE_NONE);
    	currTeletextPID = -1;
	}

	/*On configure entry changed*/
	private void onConfigChanged(String name, TVConfigValue val) throws Exception{
		Log.d(TAG, "config "+name+" changed");

		if(name.equals("tv:subtitle:enable")){
			boolean v = val.getBoolean();

			Log.d(TAG, "tv:subtitle:enable changed -> "+v);
			if(subtitleView != null && currSubtitleMode == SUBTITLE_SUB){
				if(v){
					subtitleView.show();
				}else{
					subtitleView.hide();
				}
			}
		}else if(name.equals("tv:subtitle:language")){
			String lang = val.getString();

			Log.d(TAG, "tv:subtitle:language changed -> "+lang);
			if(currSubtitleMode == SUBTITLE_SUB){
				//resetSubtitle(SUBTITLE_SUB);
			}
		}else if(name.equals("tv:teletext:language")){
			String lang = val.getString();

			Log.d(TAG, "tv:teletext:language changed -> "+lang);
			if(currSubtitleMode == SUBTITLE_TT){
				//resetSubtitle(SUBTITLE_TT);
			}
		}else if(name.equals("tv:atsc:cc:caption") ||
			name.equals("tv:atsc:cc:foregroundcolor") ||
			name.equals("tv:atsc:cc:foregroundopacity") ||
			name.equals("tv:atsc:cc:backgroundcolor") ||
			name.equals("tv:atsc:cc:backgroundopacity") ||
			name.equals("tv:atsc:cc:fontstyle") ||
			name.equals("tv:atsc:cc:fontsize") ||
			name.equals("tv:atsc:cc:enable")){

			
			Log.d(TAG, name +" changed, reset cc now.");
			
			int prog_id = client.getCurrentProgramID();

			if(prog_id == -1)
				return;

			TVProgram prog = TVProgram.selectByID(this, prog_id);
			if(prog == null)
				return;
			resetProgramCC(prog);
		}
	}

	/*Solve the TV message*/
    private void solveMessage(TVMessage msg){
    	switch(msg.getType()){
			case TVMessage.TYPE_PROGRAM_START:
				onProgramStart(msg.getProgramID());
				break;
			case TVMessage.TYPE_PROGRAM_STOP:
				onProgramStop(msg.getProgramID());
				break;
			case TVMessage.TYPE_PLAYBACK_START:
				onPlaybackStart();
				break;
			case TVMessage.TYPE_PLAYBACK_STOP:
				onPlaybackStop();
				break;
			case TVMessage.TYPE_CONFIG_CHANGED:
				try{
					onConfigChanged(msg.getConfigName(), msg.getConfigValue());
				}catch(Exception e){
					Log.e(TAG, "error in onConfigChanged");
				}
				break;
		}
	}

    /**
     *在到TVService的连接建立成功后被调用，子类中重载
     */
    abstract public void onConnected();

    /**
     *在到TVService的连接断开后被调用，子类中重载
     */
    abstract public void onDisconnected();

    /**
     *当接收到TVService发送的消息时被调用，子类中重载
     *@param msg TVService 发送的消息
     */
    abstract public void onMessage(TVMessage msg);

    SurfaceHolder.Callback surfaceHolderCallback = new SurfaceHolder.Callback() {
        public void surfaceChanged(SurfaceHolder holder, int format, int w, int h) {
            Log.d(TAG, "surfaceChanged");
			try{
            	initSurface(holder);
		 updateVideoWindow();
			} catch(Exception e){
			}
        }
        public void surfaceCreated(SurfaceHolder holder) {
            Log.d(TAG, "surfaceCreated");
            try {
                initSurface(holder);
            } catch(Exception e) {
            }
        }
        public void surfaceDestroyed(SurfaceHolder holder) {
            Log.d(TAG, "surfaceDestroyed");
        }
        private void initSurface(SurfaceHolder h) {
            Canvas c = null;
            try {
                Log.d(TAG, "initSurface");
                c = h.lockCanvas();
            }
            finally {
                if (c != null)
                    h.unlockCanvasAndPost(c);
            }
        }
    };

    private void initSubtitle(){
    	if(subtitleView == null)
    		return;

    	if(!connected)
    		return;

		subtitleView.setMargin(
				getIntConfig("tv:subtitle:margin_left"),
				getIntConfig("tv:subtitle:margin_top"),
				getIntConfig("tv:subtitle:margin_right"),
				getIntConfig("tv:subtitle:margin_bottom"));

		Log.d(TAG, "register subtitle/teletext config callbacks");
		registerConfigCallback("tv:subtitle:enable");
		registerConfigCallback("tv:subtitle:language");
		registerConfigCallback("tv:teletext:language");
		registerConfigCallback("tv:atsc:cc:caption");
		registerConfigCallback("tv:atsc:cc:foregroundcolor");
		registerConfigCallback("tv:atsc:cc:foregroundopacity");
		registerConfigCallback("tv:atsc:cc:backgroundcolor");
		registerConfigCallback("tv:atsc:cc:backgroundopacity");
		registerConfigCallback("tv:atsc:cc:fontstyle");
		registerConfigCallback("tv:atsc:cc:fontsize");
		registerConfigCallback("tv:atsc:cc:enable");
	}

	private void updateVideoWindow(){
		if(videoView == null)
			return;

		if(!connected)
			return;

		int[] loc = new int[2];

		videoView.getLocationOnScreen(loc);
		//Log.d(TAG,"--"+loc[0]+"---"+loc[1]+"---"+ videoView.getWidth()+"---"+ videoView.getHeight());
		client.setVideoWindow(loc[0], loc[1], videoView.getWidth(), videoView.getHeight());
	}

	public void setVideoWindow(int x,int y,int w,int h){
		if(client!=null)
		client.setVideoWindow(x,y,w,h);
	}

    /**
     *在Activity上创建VideoView和SubtitleView
     */
    public void openVideo(VideoView view, TVSubtitleView subv) {
        Log.d(TAG, "openVideo");
		
        ViewGroup root = (ViewGroup)getWindow().getDecorView().findViewById(android.R.id.content);

		if(subv!=null){
			subtitleView = subv;
			externalSubtitleView = true;
			subtitleView.setLayerType(View.LAYER_TYPE_SOFTWARE, null); 
			initSubtitle();
		}else if(subtitleView == null) {
            Log.d(TAG, "create subtitle view");
            subtitleView = new TVSubtitleView(this);
            externalSubtitleView = false;
	     subtitleView.setLayerType(View.LAYER_TYPE_SOFTWARE, null); 	
            root.addView(subtitleView, 0);
            initSubtitle();
        }

		if(view!=null){
			videoView = view;
			externalVideoView = true;
			updateVideoWindow();
        }else if(videoView == null) {
			Log.d(TAG, "create video view");
            videoView = new VideoView(this);
			externalVideoView = false;
            root.addView(videoView, 0);
            videoView.getHolder().addCallback(surfaceHolderCallback);
	    //videoView.getHolder().setFormat(PixelFormat.RGBA_8888);
	    videoView.getHolder().setFormat(PixelFormat.VIDEO_HOLE_REAL);
            updateVideoWindow();
        }

		if(subtitleViewActive&&subtitleView!=null){
			subtitleView.setActive(true);
			if(getBooleanConfig("tv:subtitle:enable"))
				subtitleView.show();
		}	
    }

	public void openVideo(){
		openVideo(null, null);
	}

	/**
	 *设定视频窗口的大小
	 *@param r 窗口矩形
	 */
    public void setVideoWindow(Rect r){
    	if(videoView != null && !externalVideoView){
			videoView.layout(r.left, r.top, r.right, r.bottom);
            updateVideoWindow();
		}

		if(subtitleView != null && !externalSubtitleView){
			subtitleView.layout(r.left, r.top, r.right, r.bottom);
		}
	}

	/**
	*设置由外层创建的SubtitleView
	*/
	public void setSubtitleView(TVSubtitleView subView) {
		Log.d(TAG, "setSubtitleView");
		subtitleView = subView;
		externalSubtitleView = true;
		initSubtitle();
	}

    /**
	 *计算本地时间
	 *@param utc UTC时间
	 *@return 返回本地时间
	 */
    public long getLocalTime(long utc){
    	return client.getLocalTime(utc);
	}

	/**
	 *取得当前本地时间
	 *@return 返回本地时间
	 */
    public long getLocalTime(){
    	return client.getLocalTime();
	}

	/**
	 *计算UTC时间
	 *@param local 本地时间
	 *@return 返回UTC时间
	 */
	public long getUTCTime(long local){
    	return client.getUTCTime(local);
	}

	/**
	 *取得当前UTC时间
	 *@return 返回UTC时间
	 */
	public long getUTCTime(){
		return client.getUTCTime();
	}

    /**
     *设定TV输入源
     *@param source 输入源(TVStatus.SOURCE_XXXX)
     */
    public void setInputSource(TVConst.SourceInput source) {
        client.setInputSource(source.ordinal());
    }
    
    /**
     *得到当前信号源
     *@return 返回当前信号源
     */
    public TVConst.SourceInput getCurInputSource(){
    	return client.getCurInputSource();
    }

	/**
	 *在数字电视模式下，设定节目类型是电视或广播
	 *@param type 节目类型TVProgram.TYPE_TV/TVProgram.TYPE_RADIO
	 */
    public void setProgramType(int type){
    	client.setProgramType(type);
	}
    		
    /**
     *停止播放节目
     */
    public void stopPlaying() {
        client.stopPlaying();
    }

	/**
	 *切换字幕
	 *@param id 字幕ID
	 */
    public void switchSubtitle(int id){
    	if(currSubtitleMode == SUBTITLE_SUB){
    		resetSubtitle(SUBTITLE_SUB, id);
		}
	}

	/**
	 *切换音频
	 *@param id 音频ID
	 */
	public void switchAudio(int id){
		int prog_id = client.getCurrentProgramID();

		if(prog_id == -1)
			return;

		TVProgram prog;
		TVChannel chan; 

		prog = TVProgram.selectByID(this, prog_id);
		if(prog == null)
			return;

		chan = prog.getChannel();
		if(chan == null && prog.getType() != TVProgram.TYPE_PLAYBACK)
			return;

		if(chan != null && chan.isAnalogMode()){
			TVChannelParams params = chan.getParams();

			if(params.setATVAudio(id)){
				client.resetATVFormat();
			}
		}else{
			client.switchAudio(id);
		}
	}

	/**
	 *切换模拟视频制式
	 *@param fmt 视频制式
	 */
	public void switchATVVideoFormat(TVConst.CC_ATV_VIDEO_STANDARD fmt){
		int prog_id = client.getCurrentProgramID();
		TVProgram prog;
		TVChannel chan;
		TVChannelParams params;

		if(prog_id == -1)
			return;

		prog = TVProgram.selectByID(this, prog_id);
		if(prog == null)
			return;

		chan = prog.getChannel();
		if(chan == null)
			return;

		params = chan.getParams();
		if(params == null && !params.isAnalogMode())
			return;

		//if(params.setATVVideoFormat(fmt)){
		if(chan.setATVVideoFormat(fmt)){
			Log.v(TAG,"setATVVideoFormat");
			client.resetATVFormat();
		}
	}

	/**
	 *切换模拟音频制式
	 *@param fmt 音频制式
	 */
	public void switchATVAudioFormat(TVConst.CC_ATV_AUDIO_STANDARD fmt){
		int prog_id = client.getCurrentProgramID();
		TVProgram prog;
		TVChannel chan;
		TVChannelParams params;

		if(prog_id == -1)
			return;

		prog = TVProgram.selectByID(this, prog_id);
		if(prog == null)
			return;

		chan = prog.getChannel();
		if(chan == null)
			return;

		params = chan.getParams();
		if(params == null && !params.isAnalogMode())
			return;

		//if(params.setATVAudioFormat(fmt)){
		if(chan.setATVAudioFormat(fmt)){
			Log.v(TAG,"setATVAudioFormat");
			client.resetATVFormat();
		}
	}

    /**
     *开始时移播放
     */
    public void startTimeshifting() {
        client.startTimeshifting();
    }
    
     /**
     *停止时移播放
     */
    public void stopTimeshifting() {
        client.stopTimeshifting();
    }

    /**
     *开始录制当前正在播放的节目
     *@param duration 录像时长，ms单位, duration <= 0 表示无时间限制，一直录像。
     */
    public void startRecording(long duration) {
        client.startRecording(duration);
    }

    /**
     *停止当前节目录制
     */
    public void stopRecording() {
        client.stopRecording();
    }

	/**
     *获取当前录像信息
     */
	public DTVRecordParams getRecordingParams() {
		return client.getRecordingParams();
	}
	
    /**
     *开始录制节目回放
     *@param filePath 回放文件全路径
     */
    public void startPlayback(String filePath) {
        client.startPlayback(filePath);
    }
    
     /**
     *停止录制回放
     */
	public void stopPlayback() {
		client.stopPlayback();
	}
		
	/**
     *获取当前回放信息
     */
	public DTVPlaybackParams getPlaybackParams() {
		return client.getPlaybackParams();
	}

    /**
     *开始搜索频道
     *@param sp 搜索参数
     */
    public void startScan(TVScanParams sp) {
        client.startScan(sp);
    }

    /**
     *停止频道搜索
     *@param store true表示保存搜索结果，false表示不保存直接退出
     */
    public void stopScan(boolean store) {
        client.stopScan(store);
    }

    /**
     *开始一个预约处理
     *@param bookingID 预约ID
     */
    public void startBooking(int bookingID) {
        client.startBooking(bookingID);
    }

    /**
     *播放下一频道节目
     */
    public void channelUp() {
        client.channelUp();
    }

    /**
     *播放上一频道节目
     */
    public void channelDown() {
        client.channelDown();
    }

    /**
     *根据频道号播放节目
     *@param no 频道号
     */
    public void playProgram(TVProgramNumber no) {
        client.playProgram(no);
    }
    
    /**
     *根据节目ID播放
     *@param id 节目ID
     */
    public void playProgram(int id) {
        client.playProgram(id);
    }

    /**
     *在回放和时移模式下暂停播放
     */
    public void pause() {
        client.pause();
    }

    /**
     *在回放和时移模式下恢复播放
     */
    public void resume() {
        client.resume();
    }

    /**
     *在回放和时移模式下快进播放
     *@param speed 播放速度，1为正常速度，2为2倍速播放
     */
    public void fastForward(int speed) {
    	client.fastForward(speed);
    }

    /**
     *在回放和时移模式下快退播放
     *@param speed 播放速度，1为正常速度，2为2倍速播放
     */
    public void fastBackward(int speed) {
    	client.fastBackward(speed);
    }

    /**
     *在回放和时移模式下移动到指定位置
     *@param pos 移动位置，从文件头开始的秒数
     */
    public void seekTo(int pos) {
    	client.seekTo(pos);
    }

	/**
	 *检测是否正在显示teletext模式下
	 *@return true表示正在teletext模式下，false表示不在teletext模式下
	 */
    public boolean isInTeletextMode(){
    	return currSubtitleMode==SUBTITLE_TT;
	}

    /**
     *显示Teletext
     */
    public void ttShow() {
        if(subtitleView == null)
            return;

		Log.d(TAG, "show teletext");
        resetSubtitle(SUBTITLE_TT);
    }

    /**
     *隐藏Teletext
     */
    public void ttHide() {
        if(subtitleView == null)
            return;

		Log.d(TAG, "hide teletext");
        resetSubtitle(SUBTITLE_SUB);
    }

    /**
     *跳到Teletext的下一页
     */
    public void ttGotoNextPage() {
        if(subtitleView == null)
            return;

		Log.d(TAG, "goto next teletext page");
        subtitleView.nextPage();
    }

    /**
     *跳到Teletext的上一页
     */
    public void ttGotoPreviousPage() {
        if(subtitleView == null)
            return;

		Log.d(TAG, "goto next previous page");
        subtitleView.previousPage();
    }

    /**
     *跳到Teletext的指定页
     *@param page 页号
     */
    public void ttGotoPage(int page) {
        if(subtitleView == null)
            return;

        subtitleView.gotoPage(page);
    }

    /**
     *跳转到Teletext home页
     */
    public void ttGoHome() {
        if(subtitleView == null)
            return;

        subtitleView.goHome();
    }

    /**
     *根据颜色进行Teletext跳转
     *@param color 0:红 1:绿 2:黄 3:蓝
     */
    public void ttGotoColorLink(int color) {
        if(subtitleView == null)
            return;

        subtitleView.colorLink(color);
    }

    /**
     *设定Teletext搜索匹配字符串
     *@param pattern 匹配字符串
     *@param casefold 分否区分大小写
     */
    public void ttSetSearchPattern(String pattern, boolean casefold) {
        if(subtitleView == null)
            return;

        subtitleView.setSearchPattern(pattern, casefold);
    }

    /**
     *根据设定的匹配字符串搜索下一个Teletext页
     */
    public void ttSearchNext() {
        if(subtitleView == null)
            return;

        subtitleView.searchNext();
    }

    /**
     *根据设定的匹配字符串搜索上一个Teletext页
     */
    public void ttSearchPrevious() {
        if(subtitleView == null)
            return;

        subtitleView.searchPrevious();
    }

    /**
     *设定配置选项
     *@param name 配置选项名
     *@param value 设定值
     */
    public void setConfig(String name, TVConfigValue value) {
        client.setConfig(name, value);
    }
    
    /**
     *设定配置选项
     *@param name 配置选项名
     *@param value 设定值
     */
    public void setConfig(String name, boolean value) {
        client.setConfig(name, value);
    }
    
    /**
     *设定配置选项
     *@param name 配置选项名
     *@param value 设定值
     */
    public void setConfig(String name, int value) {
        client.setConfig(name, value);
    }
    
    /**
     *设定配置选项
     *@param name 配置选项名
     *@param value 设定值
     */
    public void setConfig(String name, String value) {
        client.setConfig(name, value);
    }

	/**
	 *取得布尔型配置项值
	 *@param name 配置项名称
	 *@return 返回配置项值
	 */
	public boolean getBooleanConfig(String name){
		return client.getBooleanConfig(name);
	}

	/**
	 *取得整型配置项值
	 *@param name 配置项名称
	 *@return 返回配置项值
	 */
	public int getIntConfig(String name){
		return client.getIntConfig(name);
	}

	/**
	 *取得字符串型配置项值
	 *@param name 配置项名称
	 *@return 返回配置项值
	 */
	public String getStringConfig(String name){
		return client.getStringConfig(name);
	}

    /**
     *读取配置选项
     *@param name 配置选项名
     *@return 返回配置选项值
     */
    public TVConfigValue getConfig(String name) {
        return client.getConfig(name);
    }

    /**
     *注册配置选项回调，当选项修改时，onMessage会被调用
     *@param name 配置选项名
     */
    public void registerConfigCallback(String name) {
        client.registerConfigCallback(name);
    }

    /**
     *注销配置选项
     *@param name 配置选项名
     */
    public void unregisterConfigCallback(String name) {
        client.unregisterConfigCallback(name);
    }

    /**
     *取得前端锁定状态
     *@return 返回锁定状态
     */
    public int getFrontendStatus() {
        return client.getFrontendStatus();
    }

    /**
     *取得前端信号强度
     *@return 返回信号强度
     */
    public int getFrontendSignalStrength() {
        return client.getFrontendSignalStrength();
    }

    /**
     *取得前端SNR值
     *@return 返回SNR值
     */
    public int getFrontendSNR() {
        return client.getFrontendSNR();
    }

    /**
     *取得前端BER值
     *@return 返回BER值
     */
    public int getFrontendBER() {
        return client.getFrontendBER();
    }

	/**
	 *取得当前正在播放的节目ID
	 *@return 返回正在播放的节目ID
	 */
    public int getCurrentProgramID(){
    	return client.getCurrentProgramID();
	}

	/**
	 *取得当前设定的节目类型
	 *@return 返回当前设定的节目类型
	 */
	public int getCurrentProgramType(){
		return client.getCurrentProgramType();
	}

	/**
	 *取得当前设定的节目号
	 *@return 返回当前设定的节目号
	 */
	public TVProgramNumber getCurrentProgramNumber(){
		return client.getCurrentProgramNumber();
	}

	/**
	 *模拟微调
	 *@param freq  频率，单位为Hz
	 */
	public void fineTune(int freq){
		client.fineTune(freq);
	}

	/**
	 *恢复出厂设置
	 */
	public void restoreFactorySetting(){
		client.restoreFactorySetting();
	}
	
	/**
	 *恢复出厂设置
	 *@param flags 指定需要恢复的项
	 */
	public void restoreFactorySetting(int flags){
		client.restoreFactorySetting(flags);
	}
	
	/**
	 *播放上次播放的频道，如失败则播放第一个有效的频道
	 */
	public void playValid(){
		client.playValid();
	}

	/**
	 *设定VGA自动检测
	 */
	public void setVGAAutoAdjust(){
		client.setVGAAutoAdjust();
	}
	/*get TV Info*/
	public TvinInfo getCurrentSignalInfo(){
		return client.getCurrentSignalInfo();
	}
	/**/
	public int GetSrcInputType(){
		return client.GetSrcInputType();
	} 
	
	/**
	*当用户改变播放级别设置后，执行replay来进行强制block检查
	*/
    public void replay(){
		client.replay();
    }
    
    /**
	*解锁并播放当前已加锁的频道，例如密码验证通过后，调用该方法进行解锁播放
	*/
    public void unblock(){
    	client.unblock();
    }

	/**
	 *锁频，用于信号测试等
	 *@param curParams 频点信息
	 */
	public void lock(TVChannelParams curParams){
		client.lock(curParams);
	}

	/**
	 *卫星设备LNB与Switch配置生效
	 *@param curParams 配置信息
	 */
	public void sec_setLnbsSwitchCfgValid(TVChannelParams curParams){
		client.secRequest(TVMessage.secRequest(TVMessage.TYPE_SEC_LNBSSWITCHCFGVALID, curParams));
	}

	/**
	 *diseqc马达停止转动
	 */	
	public void diseqcPositionerStopMoving() {
		client.secRequest(TVMessage.secRequest(TVMessage.TYPE_SEC_POSITIONERSTOP));
	}

	/**
	 *diseqc马达禁止限制
	 */	
	public void diseqcPositionerDisableLimit() {
		client.secRequest(TVMessage.secRequest(TVMessage.TYPE_SEC_POSITIONERDISABLELIMIT));
	}

	/**
	 *diseqc马达东向限制设置	
	*/	
	public void diseqcPositionerSetEastLimit() {
		client.secRequest(TVMessage.secRequest(TVMessage.TYPE_SEC_POSITIONEREASTLIMIT));
	}

	/**
	 *diseqc马达西向限制设置	
	*/	
	public void diseqcPositionerSetWestLimit() {
		client.secRequest(TVMessage.secRequest(TVMessage.TYPE_SEC_POSITIONERWESTLIMIT));
	}

	/**
	 *diseqc马达东向转动	
	 *@param curParams 配置信息	 
	 *@param unit positioner转动单位	00 continuously 01-7F(单位second, e.g 01-one second 02-two second) 80-FF (单位step，e.g FF-one step FE-two step)  
	*/	
	public void diseqcPositionerMoveEast(TVChannelParams curParams, int unit) {
		Log.d(TAG, "diseqcPositionerMoveEast " + unit);
		client.secRequest(TVMessage.secRequest(TVMessage.TYPE_SEC_POSITIONEREAST, curParams, unit));
	}

	/**
	 *diseqc马达西向转动	
	 *@param curParams 配置信息	 
	 *@param unit positioner转动单位	00 continuously 01-7F(单位second, e.g 01-one second 02-two second) 80-FF (单位step，e.g FF-one step FE-two step) 	 
	*/	
	public void diseqcPositionerMoveWest(TVChannelParams curParams, int unit) {
		Log.d(TAG, "diseqcPositionerMoveWest " + unit);
		client.secRequest(TVMessage.secRequest(TVMessage.TYPE_SEC_POSITIONERWEST, curParams, unit));
	}

	/**
	 *diseqc马达存储位置
	 *@param curParams 配置信息	 
	*/
	public void diseqcPositionerStorePosition(TVChannelParams curParams) {
		client.secRequest(TVMessage.secRequest(TVMessage.TYPE_SEC_POSITIONERSTORE, curParams));
	}

	/**
	 *diseqc马达转动到指定位置
	 *@param curParams 配置信息
	 */
	public void diseqcPositionerGotoPosition(TVChannelParams curParams) {
		client.secRequest(TVMessage.secRequest(TVMessage.TYPE_SEC_POSITIONERGOTO, curParams));
	}

	/**
	 *diseqc马达转动根据本地经纬度以及卫星经度
	 *@param curParams 配置信息	 
	 */	
	public void diseqcPositionerGotoX(TVChannelParams curParams) {
		client.secRequest(TVMessage.secRequest(TVMessage.TYPE_SEC_POSITIONERGOTOX, curParams));
	}	

	

	/**
	 *导入指定xml到当前数据库
	 *@param inputXmlPath xml文件全路径	 
	 */	
	public void importDatabase(String inputXmlPath){
		client.importDatabase(inputXmlPath);
	}

	/**
	 *导出当前数据库到指定xml文件
	 *@param outputXmlPath xml文件全路径	 
	 */	
	public void exportDatabase(String outputXmlPath){
		client.exportDatabase(outputXmlPath);
	}

	/**
	 *切换声道
	 *@param mode 声道 0 立体声 1 左声道 2 右声道
	 */
	public void switchAudioTrack(int mode){
		client.switchAudioTrack(mode);
		
		int prog_id = client.getCurrentProgramID();

		if(prog_id == -1)
			return ;

		TVProgram prog;
		
		prog = TVProgram.selectByID(this, prog_id);
		if(prog == null)
			return ;

		prog.setAudTrack(mode);
	}

	/**
	*获取声道状态
	*/
	public int getAudioTrack(){
		int prog_id = client.getCurrentProgramID();

		if(prog_id == -1)
			return 0;

		TVProgram prog;
		
		prog = TVProgram.selectByID(this, prog_id);
		if(prog == null)
			return 0;

		return prog.getAudTrack();
	}

	/**
	 *切换声道
	 *@param mode screen type  0：normal 2：4-3 3：16-9
	 */
	public void switchScreenType(int mode){
		/************************************
		VIDEO_WIDEOPTION_4_3_IGNORE       = 6,
		VIDEO_WIDEOPTION_4_3_LETTER_BOX   = 7,
		VIDEO_WIDEOPTION_4_3_PAN_SCAN     = 8,
		VIDEO_WIDEOPTION_4_3_COMBINED     = 9,
		VIDEO_WIDEOPTION_16_9_IGNORE      = 10,
		VIDEO_WIDEOPTION_16_9_LETTER_BOX  = 11,
		VIDEO_WIDEOPTION_16_9_PAN_SCAN    = 12,
		VIDEO_WIDEOPTION_16_9_COMBINED    = 13,
		***************************************/
	
		String value=String.valueOf(mode);

		try {
			BufferedWriter writer = new BufferedWriter(new FileWriter("/sys/class/video/screen_mode"));
			try {
				writer.write(value);
			} finally {
				writer.close();
			}
		}catch (FileNotFoundException e) {
			e.printStackTrace();
		}catch (Exception e) {
		        Log.e(TAG,"set screen_mode ERROR!",e);
			return;
		} 

	}
	
	/**
	*获取screen type
	*/
	public int getScreenType(){
		String val=null;
		
		File file = new File("/sys/class/video/screen_mode");
		if (!file.exists()) {        	
			return 0;
		}

		//read
		try {
			BufferedReader in = new BufferedReader(new FileReader("/sys/class/video/screen_mode"), 1);
			try {
				val=in.readLine();
			} finally {
				in.close();
    			}
		} catch (Exception e) {
			Log.e(TAG, "IOException when read screen_mode");
		}

		if(val!=null){
			Log.d(TAG,"---"+val);
			//return Integer.valueOf(val);
			if(val.contains("1:full stretch")){				
				return 1;			
			}
			if(val.contains("6:4-3 ignore")){				
				return 6;			
			}			
			else  if(val.equals("7:4-3 letter box")){				
				return 7;		
			}			
			else  if(val.equals("8:4-3 pan scan")){			
				return 8;	
			}
			else  if(val.equals("9:4-3 combined")){			
				return 9;	
			}
			else  if(val.equals("10:16-9 ignore")){			
				return 10;	
			}
			else  if(val.equals("11:16-9 letter box")){			
				return 11;	
			}
			else  if(val.equals("12:16-9 pan scan")){			
				return 12;	
			}
			else  if(val.equals("13:4-3 combined")){			
				return 13;	
			}
		}
		
		return 1;
	}

	public String getScreenTypeStrings(){
		String val= "Full";
		
		File file = new File("/sys/class/video/screen_mode");
		if (!file.exists()) {        	
			return val;
		}

		//read
		try {
			BufferedReader in = new BufferedReader(new FileReader("/sys/class/video/screen_mode"), 1);
			try {
				val=in.readLine();
			} finally {
				in.close();
    			}
		} catch (Exception e) {
			Log.e(TAG, "IOException when read screen_mode");
		}
		
		return val;
	}


	public String getVideoWindowSize(){
		String val=null;
		
		File file = new File("/sys/class/video/axis");
		if (!file.exists()) {        	
			return "0 0 0 0";
		}

		//read
		try {
			BufferedReader in = new BufferedReader(new FileReader("/sys/class/video/axis"), 1);
			try {
				val=in.readLine();
			} finally {
				in.close();
    			}
		} catch (Exception e) {
			Log.e(TAG, "IOException when read screen_mode");
		}

		if(val!=null){
			return val;
		}
		
		return "0 0 0 0";
	}

	public void setVideoWindowSize(String val){
		
		try {
            BufferedWriter writer = new BufferedWriter(new FileWriter("/sys/class/video/axis"));
            try {
                writer.write(val);
                } finally {
                    writer.close();
                }
        }catch (FileNotFoundException e) {
            e.printStackTrace();
        }catch (Exception e) {
                Log.e(TAG,"setVideoWindowSize ERROR!",e);
				return;
        } 
	}

	public void setBlackoutPolicy(int val){
		client.switch_video_blackout(val);
	}

	public void FeConfigAndDmxConfig(){
		client.resetFeConfigAndDmxConfig();
	}

	public void controlUpdate(int cmd, int param, String str){
		client.controlUpdate(cmd, param, str);
	}

}

