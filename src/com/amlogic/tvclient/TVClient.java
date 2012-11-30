package com.amlogic.tvclient;

import android.content.ServiceConnection;
import android.content.ComponentName;
import android.os.IBinder;
import android.os.RemoteException;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import com.amlogic.tvservice.ITVService;
import com.amlogic.tvutil.TVConst;
import com.amlogic.tvutil.TVProgramNumber;
import com.amlogic.tvutil.TVPlayParams;
import com.amlogic.tvutil.TVScanParams;
import com.amlogic.tvutil.TVMessage;
import com.amlogic.tvutil.TVConfigValue;
import com.amlogic.tvutil.ITVCallback;

/**
 *TV客户端
 */
abstract public class TVClient
{
    private static final String TAG="TVClient";
    private static final String SERVICE_NAME="com.amlogic.tvservice.TVService";

    private ITVService service;

    private ServiceConnection conn = new ServiceConnection() {
        public void onServiceConnected(ComponentName className, IBinder service) {
            Log.d(TAG, "onServiceConnected");
            synchronized(TVClient.this) {
                TVClient.this.service = ITVService.Stub.asInterface(service);
            }
            try {
                TVClient.this.service.registerCallback(TVClient.this.callback);
            } catch(RemoteException e) {
            }
            TVClient.this.onConnected();
        }

        public void onServiceDisconnected(ComponentName className) {
            Log.d(TAG, "onServiceDisconnected");
            try {
                TVClient.this.service.unregisterCallback(TVClient.this.callback);
            } catch(RemoteException e) {
            }

            synchronized(TVClient.this) {
                TVClient.this.service = null;
            }

            TVClient.this.onDisconnected();
        }
    };

    private ITVCallback callback = new ITVCallback.Stub() {
        public void onMessage(TVMessage msg) {
            TVClient.this.onMessage(msg);
        }
    };

    /**
     *连接到TVService
     *@param context client运行的Context
     */
    public void connect(Context context) {
        Log.d(TAG, "connect");
        context.bindService(new Intent(SERVICE_NAME), conn, Context.BIND_AUTO_CREATE);
    }

    /**
     *断开到TVService的连接
     *@param context client运行的Context
     */
    public void disconnect(Context context) {
        Log.d(TAG, "disconnect");
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
     *取TV当前时间(单位为毫秒)
     *@return 返回当前时间(单位为毫秒)
     */
    private synchronized long getTime() {
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
 		long offset = getIntConfig("tv:time:offset");

    	return getTime() - offset * 1000;
	}

    /**
     *设定TV信号源
     *@param source 信号输入源(TVStatus.SOURCE_XXXX)
     */
    public synchronized void setInputSource(int source) {
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
    public synchronized  TVConst.SourceInput getCurInputSource() {
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
    public synchronized void setProgramType(int type){
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
    public synchronized void playProgram(TVPlayParams tp) {
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
    public synchronized void stopPlaying() {
        if(service != null) {
            try {
                service.stopPlaying();
            } catch(RemoteException e) {
            }
        }
    }

    /**
     *开始时移播放
     */
    public synchronized void startTimeshifting() {
        if(service != null) {
            try {
                service.startTimeshifting();
            } catch(RemoteException e) {
            }
        }
    }

    /**
     *开始录制节目
     *@param bookingID 预约记录ID，-1表示录制当前节目
     */
    public synchronized void startRecording(int bookingID) {
        if(service != null) {
            try {
                service.startRecording(bookingID);
            } catch(RemoteException e) {
            }
        }
    }

    /**
     *停止录制当前节目
     */
    public synchronized void stopRecording() {
        if(service != null) {
            try {
                service.stopRecording();
            } catch(RemoteException e) {
            }
        }
    }

    /**
     *开始录制节目回放
     *@param bookingID 录制节目的预约记录ID
     */
    public synchronized void startPlayback(int bookingID) {
        if(service != null) {
            try {
                service.startPlayback(bookingID);
            } catch(RemoteException e) {
            }
        }
    }

    /**
     *开始频道搜索
     *@param sp 搜索参数
     */
    public synchronized void startScan(TVScanParams sp) {
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
    public synchronized void stopScan(boolean store) {
        if(service != null) {
            try {
                service.stopScan(store);
            } catch(RemoteException e) {
            }
        }
    }

    /**
     *暂停播放(回放和时移播放时有效)
     */
    public synchronized void pause() {
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
    public synchronized void resume() {
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
    public synchronized void fastForward(int speed) {
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
    public synchronized void fastBackward(int speed) {
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
    public synchronized void seekTo(int pos) {
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
    public synchronized void setConfig(String name, TVConfigValue value) {
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
    public synchronized TVConfigValue getConfig(String name) {
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
			Log.e(TAG, "The config is not a boolean value");
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
			Log.e(TAG, "The config is not an integer value");
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
			Log.e(TAG, "The config is not a string value");
		}

		return s;
	}

    /**
     *注册配置选项回调，当选项修改时，onMessage会被调用
     *@param name 配置选项名
     */
    public synchronized void registerConfigCallback(String name) {
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
    public synchronized void unregisterConfigCallback(String name) {
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
}

