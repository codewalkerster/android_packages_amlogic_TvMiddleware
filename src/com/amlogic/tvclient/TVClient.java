package com.amlogic.tvclient;

import android.content.ServiceConnection;
import android.content.ComponentName;
import android.os.IBinder;
import android.os.RemoteException;
import android.os.Handler;
import android.os.Message;
import android.os.Looper;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import com.amlogic.tvservice.ITVService;
import com.amlogic.tvutil.TVConst;
import com.amlogic.tvutil.TVProgram;
import com.amlogic.tvutil.TVProgramNumber;
import com.amlogic.tvutil.TVPlayParams;
import com.amlogic.tvutil.TVScanParams;
import com.amlogic.tvutil.TVMessage;
import com.amlogic.tvutil.TVConfigValue;
import com.amlogic.tvutil.ITVCallback;
import com.amlogic.tvutil.TVStatus;
import com.amlogic.tvutil.DTVPlaybackParams;
import com.amlogic.tvutil.DTVRecordParams;
import com.amlogic.tvutil.TvinInfo;
import com.amlogic.tvutil.TVChannelParams;

/**
 *TV功能客户端
 */
abstract public class TVClient
{
	/*Restore flags*/
	public static final int RESTORE_FL_DATABASE  = 0x1;
	public static final int RESTORE_FL_CONFIG    = 0x2;
	public static final int RESTORE_FL_ALL       = RESTORE_FL_DATABASE | RESTORE_FL_CONFIG;

    private static final String TAG="TVClient";
    private static final String SERVICE_NAME="com.amlogic.tvservice.TVService";

    private static final int MSG_CONNECTED    = 1949;
    private static final int MSG_DISCONNECTED = 1950;
    private static final int MSG_MESSAGE      = 1951;
    
    private ITVService service;
    private Context context;
    private Handler handler;

    private int currProgramType;
    private TVProgramNumber currProgramNo;
    private int currProgramID = -1;

    private ServiceConnection conn = new ServiceConnection() {
        public void onServiceConnected(ComponentName className, IBinder service) {
            Log.d(TAG, "onServiceConnected");
			Message msg = handler.obtainMessage(MSG_CONNECTED, service);
            handler.sendMessage(msg);
		}

        public void onServiceDisconnected(ComponentName className) {
            Log.d(TAG, "onServiceDisconnected");
			Message msg = handler.obtainMessage(MSG_DISCONNECTED);
            handler.sendMessage(msg);
        }
    };

    private ITVCallback callback = new ITVCallback.Stub() {
        public void onMessage(TVMessage msg) {
            Log.d(TAG, "onMessage");
			Message m = handler.obtainMessage(MSG_MESSAGE, msg);
            handler.sendMessage(m);
        }
    };

    /**
     *连接到TVService
     *@param context client运行的Context
     */
    public void connect(Context context) {
        Log.d(TAG, "connect");
        this.context = context;

		handler = new Handler() {
			public void handleMessage(Message msg) {
				Log.d(TAG, "handle message "+msg.what);
				switch(msg.what) {
				case MSG_CONNECTED:
					IBinder binder = (IBinder)msg.obj;
					service = ITVService.Stub.asInterface(binder);

					try{
						service.registerCallback(TVClient.this.callback);
					}catch(android.os.RemoteException e){
					}

					TVStatus s = getStatus();
					if(s!=null){
						currProgramType = s.programType;
						currProgramNo   = s.programNo;
						currProgramID   = s.programID;
					}
					onConnected();
					break;
				case MSG_DISCONNECTED:
					if(service!=null){
						onDisconnected();
						try{
							service.unregisterCallback(TVClient.this.callback);
						}catch(android.os.RemoteException e){
						}
						service = null;
					}
					break;
				case MSG_MESSAGE:
					TVMessage tvmsg = (TVMessage)msg.obj;
					switch(tvmsg.getType()){
						case TVMessage.TYPE_PROGRAM_NUMBER:
							currProgramType = tvmsg.getProgramType();
							currProgramNo = tvmsg.getProgramNumber();
							break;
						case TVMessage.TYPE_PROGRAM_START:
							currProgramID = tvmsg.getProgramID();

							TVProgram prog = TVProgram.selectByID(TVClient.this.context, currProgramID);
							if(prog != null){
								currProgramType = prog.getType();
								currProgramNo = prog.getNumber();
							}
							break;
						case TVMessage.TYPE_PROGRAM_STOP:
							currProgramID = -1;
							currProgramNo = null;
							break;
						case TVMessage.TYPE_PLAYBACK_START:
							DTVRecordParams minfo = tvmsg.getPlaybackMediaInfo();
							if (minfo != null && currProgramType != TVProgram.TYPE_PLAYBACK){
								TVProgram playbackProg = null;

								if (minfo.getTimeshiftMode()){
									/* right the last played program */
									playbackProg = TVProgram.selectByID(TVClient.this.context,
														getIntConfig("tv:last_program_id"));
								}

								if (playbackProg == null){
									/* construct a new playplack type program */
									playbackProg = new TVProgram(
										TVClient.this.context, 
										minfo.getProgramName(), 
										TVProgram.TYPE_PLAYBACK, 
										minfo.getVideo(), 
										minfo.getAllAudio(), 
										minfo.getAllSubtitle(),
										minfo.getAllTeletext());
								}
								
								if(playbackProg != null){
									currProgramType = TVProgram.TYPE_PLAYBACK;
									currProgramNo   = playbackProg.getNumber();
									currProgramID   = playbackProg.getID();
								}
							}
							break;
						case TVMessage.TYPE_PLAYBACK_STOP:
							currProgramID = -1;
							currProgramNo = null;
							currProgramType = TVProgram.TYPE_UNKNOWN;
							break;
					}

					onMessage((TVMessage)msg.obj);
					break;
				}
			}
		};

        context.bindService(new Intent(SERVICE_NAME), conn, Context.BIND_AUTO_CREATE);
    }

    /**
     *断开到TVService的连接
     *@param context client运行的Context
     */
    public void disconnect(Context context) {
	if(handler!=null){
		Message msg = handler.obtainMessage(MSG_DISCONNECTED);
		handler.sendMessage(msg);
	}		
        context.unbindService(conn);
    }

    /**
     *当TVService的连接建立好后被调用，在子类中重载
     */
    abstract public void onConnected();

    /**
     *当TVService的连接断开后被调用，在子类中重载
     */
    abstract public void onDisconnected();

    /**
     *当收到TVService发送的消息时被调用，在子类中重载
     *@param msg TVService发送的消息
     */
    abstract public void onMessage(TVMessage msg);

	/**
	 *取得当前状态
	 *@return 返回当前状态
	 */
    public TVStatus getStatus(){
    	TVStatus s = null;

		if(service != null){
			try {
				s = service.getStatus();
			} catch(RemoteException e) {
			}
		}

		return s;
	}

	/**
	 *设定视频窗口大小
	 *@param x 左上角X坐标
	 *@param y 左上角Y坐标
	 *@param w 窗口宽度
	 *@param h 窗口高度
	 */
	public void setVideoWindow(int x, int y, int w, int h){
		if(service != null){
			try {
				service.setVideoWindow(x, y, w, h);
			} catch(RemoteException e) {
			}
		}
	}

    /**
     *取TV当前时间(单位为毫秒)
     *@return 返回当前时间(单位为毫秒)
     */
    private long getTime() {
        long ret = 0;

        if(service != null) {
            try {
                ret = service.getTime();
            } catch(RemoteException e) {
            }
        }

        return ret;
    }

	/**
	 *计算本地时间
	 *@param utc UTC时间
	 *@return 返回本地时间
	 */
    public long getLocalTime(long utc){
    	long offset = getIntConfig("tv:time:offset");

    	return offset * 1000 + utc;
	}

	/**
	 *取得当前本地时间
	 *@return 返回本地时间
	 */
    public long getLocalTime(){
    	long offset = getIntConfig("tv:time:offset");

    	return offset * 1000 + getTime();
	}

	/**
	 *计算UTC时间
	 *@param local 本地时间
	 *@return 返回UTC时间
	 */
	public long getUTCTime(long local){
 		long offset = getIntConfig("tv:time:offset");

    	return local - offset * 1000;
	}

	/**
	 *取得当前UTC时间
	 *@return 返回UTC时间
	 */
	public long getUTCTime(){
    	return getTime();
	}

    /**
     *设定TV信号源
     *@param source 信号输入源(TVStatus.SOURCE_XXXX)
     */
    public void setInputSource(int source) {
        if(service != null) {
            try {
                service.setInputSource(source);
            } catch(RemoteException e) {
            }
        }
    }
    
    /**
     *得到当前信号源
     *@return 返回当前信号源
     */
    public TVConst.SourceInput getCurInputSource() {
        if(service != null) {
            try {
                int sourceInt = service.getCurInputSource();
                TVConst.SourceInput source = TVConst.SourceInput.values()[sourceInt];
                return source;
            } catch(RemoteException e) {
            }
        }
		return null;
    }

	/**
	 *在数字电视模式下，设定节目类型是电视或广播
	 *@param type 节目类型TVProgram.TYPE_TV/TVProgram.TYPE_RADIO
	 */
    public void setProgramType(int type){
 		if(service != null) {
            try {
                service.setProgramType(type);
            } catch(RemoteException e) {
            }
        }
	}

    /**
     *开始电视节目播放
     *@param tp 播放参数
     */
    public void playProgram(TVPlayParams tp) {
        if(service != null) {
            try {
                service.playProgram(tp);
            } catch(RemoteException e) {
            }
        }
    }

    /**
     *开始电视节目播放
     *@param id 节目ID
     */
    public void playProgram(int id){
    	playProgram(TVPlayParams.playProgramByID(id));
	}

    /**
     *开始电视节目播放
     *@param num 节目号
     */
	public void playProgram(TVProgramNumber num){
		playProgram(TVPlayParams.playProgramByNumber(num));
	}
	
	/**
	 *播放下一节目号的节目
	 */
	public void channelUp(){
		playProgram(TVPlayParams.playProgramUp());
	}
	
	/**
	 *播放下一节目号的节目
	 */
	public void channelDown(){
		playProgram(TVPlayParams.playProgramDown());
	}

    /**
     *停止电视节目播放
     */
    public void stopPlaying() {
        if(service != null) {
            try {
                service.stopPlaying();
            } catch(RemoteException e) {
            }
        }
    }

	/**
	 *切换音频
	 *@param id 音频ID
	 */
    public void switchAudio(int id){
    	if(service != null) {
            try {
                service.switchAudio(id);
            } catch(RemoteException e) {
            }
        }
	}

	/**
	 *重新设定模拟电视制式
	 */
	public void resetATVFormat(){
		if(service != null){
			try{
				service.resetATVFormat();
			}catch(RemoteException e){
			}
		}
	}

    /**
     *开始时移播放
     */
    public void startTimeshifting() {
        if(service != null) {
            try {
                service.startTimeshifting();
            } catch(RemoteException e) {
            }
        }
    }
    
     /**
     *停止时移播放
     */
    public void stopTimeshifting() {
        if(service != null) {
            try {
                service.stopTimeshifting();
            } catch(RemoteException e) {
            }
        }
    }

    /**
     *开始录制当前播放的节目
     *@param duration 录像时长，ms单位, duration <= 0 表示无时间限制，一直录像。
     */
    public synchronized void startRecording(long duration) {
        if(service != null) {
            try {
                service.startRecording(duration);
            } catch(RemoteException e) {
            }
        }
    }

	   /**
     *切换声道
     *@param aud_track 0 立体声 1 左声道 2 右声道。
     */
		public void switchAudioTrack(int aud_track){
			if(service != null) {
	            try {
	                service.switchAudioTrack(aud_track);
	            } catch(RemoteException e) {
	            }
	        }
		}

    /**
     *停止录制当前节目
     */
    public void stopRecording() {
        if(service != null) {
            try {
                service.stopRecording();
            } catch(RemoteException e) {
            }
        }
    }
    
    /**
     *获取当前录像信息
     */
	public DTVRecordParams getRecordingParams() {
		if(service != null) {
            try {
                return service.getRecordingParams();
            } catch(RemoteException e) {
            }
        }
        
        return null;
	}

    /**
     *开始录制节目回放
     *@param filePath 回放文件的全路径
     */
    public void startPlayback(String filePath) {
        if(service != null) {
            try {
                service.startPlayback(filePath);
            } catch(RemoteException e) {
            }
        }
    }
    
    /**
     *停止录制回放
     */
	public void stopPlayback() {
		if(service != null) {
            try {
                service.stopPlayback();
            } catch(RemoteException e) {
            }
        }
	}
		
	/**
     *获取当前回放信息
     */
	public DTVPlaybackParams getPlaybackParams() {
		if(service != null) {
            try {
                return service.getPlaybackParams();
            } catch(RemoteException e) {
            }
        }
        
        return null;
	}

    /**
     *开始频道搜索
     *@param sp 搜索参数
     */
    public void startScan(TVScanParams sp) {
        if(service != null) {
            try {
                service.startScan(sp);
            } catch(RemoteException e) {
            }
        }
    }

    /**
     *停止频道搜索
     *@param store true表示保存搜索到的节目，flase表示不保存直接退出
     */
    public void stopScan(boolean store) {
        if(service != null) {
            try {
                service.stopScan(store);
            } catch(RemoteException e) {
            }
        }
    }

    /**
     *开始一个预约处理
     *@param bookingID 预约ID
     */
    public void startBooking(int bookingID) {
        if(service != null) {
            try {
                service.startBooking(bookingID);
            } catch(RemoteException e) {
            }
        }
    }

    /**
     *暂停播放(回放和时移播放时有效)
     */
    public void pause() {
        if(service != null) {
            try {
                service.pause();
            } catch(RemoteException e) {
            }
        }
    }

    /**
     *恢复播放(回放和时移播放时有效)
     */
    public void resume() {
        if(service != null) {
            try {
                service.resume();
            } catch(RemoteException e) {
            }
        }
    }

    /**
     *快进播放(回放和时移播放时有效)
     *@param speed 播放速度(1表示正常速度，2表示2倍速播放)
     */
    public void fastForward(int speed) {
        if(service != null) {
            try {
                service.fastForward(speed);
            } catch(RemoteException e) {
            }
        }
    }

    /**
     *快退播放(回放和时移播放时有效)
     *@param speed 播放速度(1表示正常速度，2表示2倍速播放)
     */
    public void fastBackward(int speed) {
        if(service != null) {
            try {
                service.fastBackward(speed);
            } catch(RemoteException e) {
            }
        }
    }

    /**
     *移动到指定位置(回放和时移播放时有效)
     *@param pos 位置(从文件头开始的秒数)
     */
    public void seekTo(int pos) {
        if(service != null) {
            try {
                service.seekTo(pos);
            } catch(RemoteException e) {
            }
        }
    }

    /**
     *设定配置选项
     *@param name 配置选项名
     *@param value 设定值
     */
    public void setConfig(String name, TVConfigValue value) {
        if(service != null) {
            try {
                service.setConfig(name, value);
            } catch(RemoteException e) {
            }
        }
    }

    /**
     *读取配置选项
     *@param name 配置选项名
     *@return 返回配置选项值
     */
    public TVConfigValue getConfig(String name) {
        TVConfigValue value = null;

        if(service != null) {
            try {
                value = service.getConfig(name);
            } catch(RemoteException e) {
            }
        }

        return value;
    }

    /**
     *设定配置选项
     *@param name 配置选项名
     *@param value 设定值
     */
    public void setConfig(String name, String value){
    	setConfig(name, new TVConfigValue(value));
	}

    /**
     *设定配置选项
     *@param name 配置选项名
     *@param value 设定值
     */
	public void setConfig(String name, boolean value){
		setConfig(name, new TVConfigValue(value));
	}

    /**
     *设定配置选项
     *@param name 配置选项名
     *@param value 设定值
     */
	public void setConfig(String name, int value){
		setConfig(name, new TVConfigValue(value));
	}

	/**
	 *取得布尔型配置项值
	 *@param name 配置项名称
	 *@return 返回配置项值
	 */
	public boolean getBooleanConfig(String name){
		TVConfigValue value = getConfig(name);
		boolean b = false;

		try{
			b = value.getBoolean();
		}catch(Exception e){
			Log.e(TAG, "The config is not a boolean value: " + name);
		}

		return b;
	}

	/**
	 *取得整型配置项值
	 *@param name 配置项名称
	 *@return 返回配置项值
	 */
	public int getIntConfig(String name){
		TVConfigValue value = getConfig(name);
		int i = 0;

		try{
			i = value.getInt();
		}catch(Exception e){
			Log.e(TAG, "The config is not an integer value: " + name);
		}

		return i;
	}

	/**
	 *取得字符串型配置项值
	 *@param name 配置项名称
	 *@return 返回配置项值
	 */
	public String getStringConfig(String name){
		TVConfigValue value = getConfig(name);
		String s = "";

		try{
			s = value.getString();
		}catch(Exception e){
			Log.e(TAG, "The config is not a string value: " + name);
		}

		return s;
	}

    /**
     *注册配置选项回调，当选项修改时，onMessage会被调用
     *@param name 配置选项名
     */
    public void registerConfigCallback(String name) {
        if(service != null) {
            try {
                service.registerConfigCallback(name, callback);
            } catch(RemoteException e) {
            }
        }
    }

    /**
     *注销配置选项
     *@param name 配置选项名
     */
    public void unregisterConfigCallback(String name) {
        if(service != null) {
            try {
                service.unregisterConfigCallback(name, callback);
            } catch(RemoteException e) {
            }
        }
    }

    /**
     *取得前端锁定状态
     *@return 返回锁定状态
     */
    public int getFrontendStatus() {
        int ret = 0;

        if(service != null) {
            try {
                ret = service.getFrontendStatus();
            } catch(RemoteException e) {
            }
        }

        return ret;
    }

    /**
     *取得前端信号强度
     *@return 返回信号强度
     */
    public int getFrontendSignalStrength() {
        int ret = 0;

        if(service != null) {
            try {
                ret = service.getFrontendSignalStrength();
            } catch(RemoteException e) {
            }
        }

        return ret;
    }

    /**
     *取得前端SNR值
     *@return 返回SNR值
     */
    public int getFrontendSNR() {
        int ret = 0;

        if(service != null) {
            try {
                ret = service.getFrontendSNR();
            } catch(RemoteException e) {
            }
        }

        return ret;
    }

    /**
     *取得前端BER值
     *@return 返回BER值
     */
    public int getFrontendBER() {
        int ret = 0;

        if(service != null) {
            try {
                ret = service.getFrontendBER();
            } catch(RemoteException e) {
            }
        }

        return ret;
    }

	/**
	 *取得当前正在播放的节目ID
	 *@return 返回正在播放的节目ID
	 */
    public int getCurrentProgramID(){
    	return currProgramID;
	}

	/**
	 *取得当前设定的节目类型
	 *@return 返回当前设定的节目类型
	 */
	public int getCurrentProgramType(){
		return currProgramType;
	}

	/**
	 *取得当前设定的节目号
	 *@return 返回当前设定的节目号
	 */
	public TVProgramNumber getCurrentProgramNumber(){
		return currProgramNo;
	}

	/**
	 *模拟微调
	 *@param freq  频率，单位为Hz
	 */
	public void fineTune(int freq){
		if(service != null){
			try {
				service.fineTune(freq);
            } catch(RemoteException e) {
            }
		}
	}

	/**
	 *模拟CVBS AMP OUT
	 *@param amp 新设定CVBS放大值 
	 */
	public void setCvbsAmpOut(int amp){
		if(service != null){
			try {
				service.setCvbsAmpOut(amp);
            } catch(RemoteException e) {
            }
		}
	}
    
	/**
	 *恢复出厂设置
	 */
	public void restoreFactorySetting(){
		restoreFactorySetting(RESTORE_FL_ALL);
	}
	
	/**
	 *恢复出厂设置
	 *@param flags 需要恢复的项，如DATABASE CONFIG等
	 */
	public void restoreFactorySetting(int flags){
		if(service != null){
			try{
				service.restoreFactorySetting(flags);
			}catch(RemoteException e){
			}
		}
	}
	
	/**
	 *播放上次播放的频道，如失败则播放第一个有效的频道
	 */
	public void playValid(){
		if(service != null){
			try{
				service.playValid();
			}catch(RemoteException e){
			}
		}
	}

	/**
	 *设定VGA自动检测
	 */
	public void setVGAAutoAdjust(){
		if(service != null){
			try{
				service.setVGAAutoAdjust();
			}catch(RemoteException e){
			}
		}
	}
	

    /**
     *得到设备的类型
     */
	public int GetSrcInputType(){
	    int type = 0;
	    if(service != null){
            try{
                type = service.GetSrcInputType();
            }catch(RemoteException e){
            }
        }
	    return type;
	}
	
	
	 /**
     *得到设备的类型
     */
    public TvinInfo getCurrentSignalInfo(){
        TvinInfo tvinInfo = null;
        if(service != null){
            try{
                tvinInfo = service.getCurrentSignalInfo();
            }catch(RemoteException e){
            }
        }
        return tvinInfo;
    }
	
	/**
	*当用户改变播放级别设置后，执行replay来进行强制block检查
	*/
    public void replay(){
		if(service != null){
			try{
				service.replay();
			}catch(RemoteException e){
			}
		}
    }
    
    /**
	*解锁并播放当前已加锁的频道，例如密码验证通过后，调用该方法进行解锁播放
	*/
    public void unblock(){
    	if(service != null){
			try{
				service.unblock();
			}catch(RemoteException e){
			}
		}
    }

	/**
	 *锁频，用于信号测试等
	 *@param curParams 频点信息
	 */
	public void lock(TVChannelParams curParams){
		if(service != null){
			try{
				service.lock(curParams);
			}catch(RemoteException e){
			}
		}
	}

    /**
     *卫星设备控制
     *@param sec_msg 卫星设备控制 消息
     */
    public void secRequest(TVMessage sec_msg) {
        if(service != null) {
            try {
                service.secRequest(sec_msg);
            } catch(RemoteException e) {
            }
        }
    }	

   public void switch_video_blackout(int val){
	  if(service != null) {
            try {
                service.switch_video_blackout(val);
            } catch(RemoteException e) {
            }
        }
   }	

	/**
	 *将指定xml文件导入到当前数据库
	 *@param inputXmlPath xml文件全路径
	 */
	public void importDatabase(String inputXmlPath){
		if(service != null){
			try{
				service.importDatabase(inputXmlPath);
			}catch(RemoteException e){
			}
		}
	}

	/**
	 *将当前数据库导出到指定xml文件
	 *@param outputXmlPath xml文件全路径
	 */
	public void exportDatabase(String outputXmlPath){
 		if(service != null){
			try{
				service.exportDatabase(outputXmlPath);
			}catch(RemoteException e){
			}
		}
	}
    
}

