package com.amlogic.tvactivity;

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

		if(prog_id == -1)
			return;

		TVProgram prog = TVProgram.selectByID(this, prog_id);
		if(prog == null)
			return;

		int pid = -1, id1 = -1, id2 = -1, pm = -1;

		if(mode == SUBTITLE_SUB){
			if (! getStringConfig("tv:dtv:mode").equals("atsc")){
	    		TVProgram.Subtitle sub;
	    		
	    		if(sub_id >= 0){
	    			sub = prog.getSubtitle(sub_id);
				}else{
	    			sub = prog.getSubtitle(getStringConfig("tv:subtitle:language"));
				}

	    		if(sub == null)
	    			return;

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
			TVProgram.Teletext tt;
			
			if(sub_id >= 0){
				tt = prog.getTeletext(sub_id);
			}else{
				tt = prog.getTeletext(getStringConfig("tv:teletext:language"));
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

		if(pm == SUBTITLE_SUB){
			subp = new TVSubtitleView.DVBSubParams(0, pid, id1, id2);
			subtitleView.setSubParams(subp);
		}else{
			int pgno;

			pgno = (id1==0) ? 800 : id1*100;
			pgno += id2;
			ttp = new TVSubtitleView.DTVTTParams(0, pid, pgno, 0x3F7F);

			if(mode == SUBTITLE_SUB){
				subtitleView.setSubParams(ttp);
			}else{
				subtitleView.setTTParams(ttp);
			}
		}

		if(mode == SUBTITLE_SUB){
			subtitleView.startSub();
		}else{
			subtitleView.startTT();
		}

		subtitleView.show();

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
    	resetSubtitle(SUBTITLE_SUB);
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
            //initSurface(holder);
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

		client.setVideoWindow(loc[0], loc[1], videoView.getWidth(), videoView.getHeight());
	}

    /**
     *在Activity上创建VideoView和SubtitleView
     */
    public void openVideo() {
        Log.d(TAG, "openVideo");

        ViewGroup root = (ViewGroup)getWindow().getDecorView().findViewById(android.R.id.content);

        if(subtitleView == null) {
            Log.d(TAG, "create subtitle view");
            subtitleView = new TVSubtitleView(this);
            root.addView(subtitleView, 0);
            initSubtitle();
        }

        if(videoView == null) {
            Log.d(TAG, "create video view");
            videoView = new VideoView(this);
            root.addView(videoView, 0);
            videoView.getHolder().addCallback(surfaceHolderCallback);
            videoView.getHolder().setFormat(PixelFormat.VIDEO_HOLE);
            updateVideoWindow();
        }
    }

	/**
	 *设定视频窗口的大小
	 *@param r 窗口矩形
	 */
    public void setVideoWindow(Rect r){
    	if(videoView != null){
    		videoView.layout(r.left, r.top, r.right, r.bottom);
            updateVideoWindow();
		}

		if(subtitleView != null){
			subtitleView.layout(r.left, r.top, r.right, r.bottom);
		}
	}

	/**
	*设置由外层创建的SubtitleView
	*/
	public void setSubtitleView(TVSubtitleView subView) {
		Log.d(TAG, "setSubtitleView");
		subtitleView = subView;
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
		if(chan == null)
			return;

		if(chan.isAnalogMode()){
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
     */
    public void startRecording() {
        client.startRecording();
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
     *@param bookingID 录制节目的预约记录ID
     */
    public void startPlayback(int bookingID) {
        client.startPlayback(bookingID);
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
}

