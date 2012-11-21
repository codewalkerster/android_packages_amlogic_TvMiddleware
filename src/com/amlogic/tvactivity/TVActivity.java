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
import android.util.Log;
import java.lang.StringBuilder;
import com.amlogic.tvclient.TVClient;
import com.amlogic.tvutil.TVConst;
import com.amlogic.tvutil.TVProgramNumber;
import com.amlogic.tvutil.TVPlayParams;
import com.amlogic.tvutil.TVScanParams;
import com.amlogic.tvutil.TVMessage;
import com.amlogic.tvutil.TVConfigValue;
import com.amlogic.tvsubtitle.TVSubtitleView;

/**
 *TV Activity
 */
abstract public class TVActivity extends Activity
{
    private static final String TAG = "TVActivity";
    private static final int MSG_CONNECTED    = 1949;
    private static final int MSG_DISCONNECTED = 1950;
    private static final int MSG_MESSAGE      = 1951;

    private VideoView videoView;
    private TVSubtitleView subtitleView;

    private TVClient client = new TVClient() {
        public void onConnected() {
            Message msg = handler.obtainMessage(MSG_CONNECTED);
            handler.sendMessage(msg);
        }

        public void onDisconnected() {
            Message msg = handler.obtainMessage(MSG_DISCONNECTED);
            handler.sendMessage(msg);
        }

        public void onMessage(TVMessage m) {
            Message msg = handler.obtainMessage(MSG_MESSAGE, m);
            handler.sendMessage(msg);
        }
    };

    private Handler handler = new Handler() {
        public void handleMessage(Message msg) {
            Log.d(TAG, "handle message "+msg.what);
            switch(msg.what) {
            case MSG_CONNECTED:
                onConnected();
                break;
            case MSG_DISCONNECTED:
                onDisconnected();
                break;
            case MSG_MESSAGE:
                onMessage((TVMessage)msg.obj);
                break;
            }
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
        client.disconnect(this);
        super.onDestroy();
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
        }
        if(videoView == null) {
            Log.d(TAG, "create video view");
            videoView = new VideoView(this);
            root.addView(videoView, 0);
            videoView.getHolder().addCallback(surfaceHolderCallback);
            videoView.getHolder().setFormat(PixelFormat.VIDEO_HOLE);
        }
    }

    /**
     *取得TV当前时间
     *@return 当前时间，单位为毫秒
     */
    public long getTime() {
        return client.getTime();
    }

    /**
     *设定TV输入源
     *@param source 输入源(TVStatus.SOURCE_XXXX)
     */
    public void setInputSource(TVConst.SourceInput source) {
        client.setInputSource(source.ordinal());
    }

   
	 public TVConst.SourceInput   getCurInputSource(){
	        return client.getCurInputSource();
	 }
    		
    /**
     *停止播放节目
     */
    public void stopPlaying() {
        client.stopPlaying();
    }

    /**
     *开始时移播放
     */
    public void startTimeshifting() {
        client.startTimeshifting();
    }

    /**
     *开始录制
     *@param bookingID 要录制节目的预约记录ID
     */
    public void startRecording(int bookingID) {
        client.startRecording(bookingID);
    }

    /**
     *开始录制当前节目
     */
    public void startRecording() {
        client.startRecording(-1);
    }

    /**
     *停止当前节目录制
     */
    public void stopRecording() {
        client.stopRecording();
    }

    /**
     *开始录制节目回放
     *@param bookingID 录制节目的预约记录ID
     */
    public void startPlayback(int bookingID) {
        client.startPlayback(bookingID);
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
    }

    /**
     *播放上一频道节目
     */
    public void channelDown() {
    }

    /**
     *根据频道号播放节目
     *@param no 频道号
     */
    public void playProgram(TVProgramNumber no) {
        TVPlayParams tp = TVPlayParams.playProgramByNumber(no);

        client.playProgram(tp);
    }

    /**
     *根据节目ID播放
     *@param id 节目ID
     */
    public void playProgram(int id) {
        TVPlayParams tp = TVPlayParams.playProgramByID(id);

        client.playProgram(tp);
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
    }

    /**
     *在回放和时移模式下快退播放
     *@param speed 播放速度，1为正常速度，2为2倍速播放
     */
    public void fastBackward(int speed) {
    }

    /**
     *在回放和时移模式下移动到指定位置
     *@param pos 移动位置，从文件头开始的秒数
     */
    public void seekTo(int pos) {
    }

    /**
     *显示Teletext
     */
    public void ttShow() {
        if(subtitleView == null)
            return;
    }

    /**
     *隐藏Teletext
     */
    public void ttHide() {
        if(subtitleView == null)
            return;
    }

    /**
     *跳到Teletext的下一页
     */
    public void ttGotoNextPage() {
        if(subtitleView == null)
            return;

        subtitleView.nextPage();
    }

    /**
     *跳到Teletext的上一页
     */
    public void ttGotoPreviousPage() {
        if(subtitleView == null)
            return;

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
}

