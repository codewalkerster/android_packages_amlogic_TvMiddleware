package com.amlogic.tvservice;

import com.amlogic.tvutil.TVProgramNumber;
import com.amlogic.tvutil.TVPlayParams;
import com.amlogic.tvutil.TVScanParams;
import com.amlogic.tvutil.TVConfigValue;
import com.amlogic.tvutil.ITVCallback;
import com.amlogic.tvutil.TVStatus;
import com.amlogic.tvutil.DTVPlaybackParams;
import com.amlogic.tvutil.DTVRecordParams;
import com.amlogic.tvutil.TvinInfo;


//TV Service接口
interface ITVService{
	//取得当前状态
	TVStatus getStatus();

    
	//注册消息回调
	void registerCallback(ITVCallback cb);

	//注销消息回调
	void unregisterCallback(ITVCallback cb);

	//设定配置项
	void setConfig(String name, in TVConfigValue value);

	//读取配置项
	TVConfigValue getConfig(String name);

	//注册配置项回调
	void registerConfigCallback(String name, ITVCallback cb);

	//注销配置项回调
	void unregisterConfigCallback(String name, ITVCallback cb);

	//取得当前TV时间(单位为毫秒)
	long getTime();

	//设定输入源
	void setInputSource(int source);
	
	//得到输入源
	int getCurInputSource();

	//设定当前节目类型
	void setProgramType(int type);

	//设定视频窗口
	void setVideoWindow(int x, int y, int w, int h);

	//开始播放
	void playProgram(in TVPlayParams tp);

	//停止播放
	void stopPlaying();

	//切换音频
	void switchAudio(int id);

	//重新设定模拟电视制式
	void resetATVFormat();

	//开始时移播放
	void startTimeshifting();
	
	//停止时移播放
	void stopTimeshifting();

	//开始录像
	void startRecording();

	//停止录像
	void stopRecording();
	
	//获取当前录像信息
	DTVRecordParams getRecordingParams();

	//开始回放
	void startPlayback(int bookingID);
	
	//停止回放
	void stopPlayback();
		
	//获取当前回放信息
	DTVPlaybackParams getPlaybackParams();

	//开始搜索
	void startScan(in TVScanParams sp);

	//停止搜索
	void stopScan(boolean store);

	//暂停播放(回放和时移)
	void pause();

	//恢复播放(回放和时移)
	void resume();

	//快进(回放和时移)
	void fastForward(int speed);

	//快退(回放和时移)
	void fastBackward(int speed);

	//移动到指定位置(回放和时移)
	void seekTo(int pos);

	//取得前端锁定状态
	int getFrontendStatus();

	//取得信号强度
	int getFrontendSignalStrength();

	//取得信号SNR
	int getFrontendSNR();

	//取得信号BER
	int getFrontendBER();

	//模拟微调
	void fineTune(int freq);

	//恢复出厂设置
	void restoreFactorySetting(int flags);
	
	//播放上次播放的频道，如失败则播放第一个有效的频道
	void playValid();

	//VGA自动检测
	void setVGAAutoAdjust();
	
	//获取资源类型
	int GetSrcInputType();
	
	
    //获取tvin的状态
    TvinInfo getCurrentSignalInfo();
    
    //当用户改变播放级别设置后，执行replay来进行强制block检查
    void replay();
    
    //解锁并播放当前已加锁的频道，例如密码验证通过后，调用该方法进行解锁播放
    void unblock();
    
    //模拟CVBS AMP OUT
    void setCvbsAmpOut(int amp);
    
}

