package com.amlogic.tvservice;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.content.Context;
import android.os.Message;
import android.os.Looper;
import android.os.Handler;
import android.database.Cursor;
import java.util.Date;
import android.os.RemoteCallbackList;
import android.util.Log;
import com.amlogic.tvutil.TVConst;
import com.amlogic.tvutil.TVProgramNumber;
import com.amlogic.tvutil.TVPlayParams;
import com.amlogic.tvutil.TVScanParams;
import com.amlogic.tvutil.TVChannelParams;
import com.amlogic.tvutil.ITVCallback;
import com.amlogic.tvutil.TVConfigValue;
import com.amlogic.tvutil.TVProgram;
import com.amlogic.tvutil.TVChannel;
import com.amlogic.tvutil.TVMessage;
import com.amlogic.tvdataprovider.TVDataProvider;
//import com.amlogic.tvservice.TVScanner.TVScannerParams;

public class TVService extends Service
{
    private static final String TAG = "TVService";

    /*Message types*/
    private static final int MSG_SET_SOURCE    = 1949;
    private static final int MSG_PLAY_PROGRAM  = 1950;
    private static final int MSG_STOP_PLAYING  = 1951;
    private static final int MSG_START_TIMESHIFTING = 1952;
    private static final int MSG_START_PLAYBACK  = 1953;
    private static final int MSG_START_SCAN      = 1954;
    private static final int MSG_STOP_SCAN       = 1955;
    private static final int MSG_START_RECORDING = 1956;
    private static final int MSG_STOP_RECORDING  = 1957;
    private static final int MSG_PAUSE           = 1958;
    private static final int MSG_RESUME          = 1959;
    private static final int MSG_FAST_FORWARD    = 1960;
    private static final int MSG_FAST_BACKWARD   = 1961;
    private static final int MSG_SEEK_TO         = 1962;
    private static final int MSG_DEVICE_EVENT    = 1963;
    private static final int MSG_EPG_EVENT       = 1964;
    private static final int MSG_SCAN_EVENT      = 1965;


    final RemoteCallbackList<ITVCallback> callbacks
    = new RemoteCallbackList<ITVCallback>();

    private void sendMessage(TVMessage msg) {
        final int N = callbacks.beginBroadcast();
        for (int i = 0; i < N; i++) {
            try {
                callbacks.getBroadcastItem(i).onMessage(msg);
            } catch(Exception e) {
                e.printStackTrace();
            }
        }
        callbacks.finishBroadcast();
    }

    private final ITVService.Stub mBinder = new ITVService.Stub() {

        public int getFrontendStatus() {
            int ret;

            synchronized(TVService.this) {
                if(IsScanning)
                    ret = scanner.getFrontendStatus();
                else
                    ret = device.getFrontendStatus();
            }

            return ret;
        }

        public int getFrontendSignalStrength() {
            int ret;

            synchronized(TVService.this) {
                if(IsScanning)
                    ret = scanner.getFrontendSignalStrength();
                else
                    ret = device.getFrontendSignalStrength();
            }

            return ret;
        }

        public int getFrontendSNR() {
            int ret;

            synchronized(TVService.this) {
                if(IsScanning)
                    ret = scanner.getFrontendSNR();
                else
                    ret = device.getFrontendSNR();
            }

            return ret;
        }

        public int getFrontendBER() {
            int ret;

            synchronized(TVService.this) {
                if(IsScanning)
                    ret = scanner.getFrontendBER();
                else
                    ret = device.getFrontendBER();
            }

            return ret;
        }

        public synchronized void registerCallback(ITVCallback cb) {
            if(cb !=null) {
                callbacks.register(cb);
            }
        }

        public synchronized void unregisterCallback(ITVCallback cb) {
            if(cb != null) {
                callbacks.unregister(cb);
            }
        }

        public void setConfig(String name, TVConfigValue value) {
            try {
                config.set(name, value);
            } catch(Exception e) {
                Log.e(TAG, "failed to set config "+name);
            }
        }

        public TVConfigValue getConfig(String name) {
            TVConfigValue v = null;

            try {
                v = config.get(name);
            } catch(Exception e) {
                Log.e(TAG, "failed to get config "+name);
            }

            return v;
        }

        public void registerConfigCallback(String name, ITVCallback cb) {
            try {
                config.registerRemoteCallback(name, cb);
            } catch(Exception e) {
                Log.d(TAG, "registerRemoteCallback "+name+" failed");
            }
        }

        public void unregisterConfigCallback(String name, ITVCallback cb) {
            try {
                config.unregisterRemoteCallback(name, cb);
            } catch(Exception e) {
                Log.e(TAG, "unregisterRemoteCallback "+name+" failed");
            }
        }

        public long getTime() {
            return time.getTime();
        }

        public void setInputSource(int source) {
            Message msg = handler.obtainMessage(MSG_SET_SOURCE, new Integer(source));
            handler.sendMessage(msg);
        }
        
        public  int getCurInputSource() {
        	return device.getCurInputSource();
        }

        public void playProgram(TVPlayParams tp) {
            Message msg = handler.obtainMessage(MSG_PLAY_PROGRAM, tp);
            handler.sendMessage(msg);
        }

        public void stopPlaying() {
            Message msg = handler.obtainMessage(MSG_STOP_PLAYING);
            handler.sendMessage(msg);
        }

        public void startTimeshifting() {
            Message msg = handler.obtainMessage(MSG_START_TIMESHIFTING);
            handler.sendMessage(msg);
        }

        public void startRecording(int bookingID) {
            Message msg = handler.obtainMessage(MSG_START_RECORDING, new Integer(bookingID));
            handler.sendMessage(msg);
        }

        public void stopRecording() {
            Message msg = handler.obtainMessage(MSG_STOP_RECORDING);
            handler.sendMessage(msg);
        }

        public void startPlayback(int bookingID) {
            Message msg = handler.obtainMessage(MSG_START_PLAYBACK);
            handler.sendMessage(msg);
        }

        public void startScan(TVScanParams sp) {
            Message msg = handler.obtainMessage(MSG_START_SCAN, sp);
            handler.sendMessage(msg);
        }

        public void stopScan(boolean store) {
            Message msg = handler.obtainMessage(MSG_STOP_SCAN, new Boolean(store));
            handler.sendMessage(msg);
        }

        public void pause() {
            Message msg = handler.obtainMessage(MSG_PAUSE);
            handler.sendMessage(msg);
        }

        public void resume() {
            Message msg = handler.obtainMessage(MSG_RESUME);
            handler.sendMessage(msg);
        }

        public void fastForward(int speed) {
            Message msg = handler.obtainMessage(MSG_FAST_FORWARD, new Integer(speed));
            handler.sendMessage(msg);
        }

        public void fastBackward(int speed) {
            Message msg = handler.obtainMessage(MSG_FAST_BACKWARD, new Integer(speed));
            handler.sendMessage(msg);
        }

        public void seekTo(int pos) {
            Message msg = handler.obtainMessage(MSG_SEEK_TO, new Integer(pos));
            handler.sendMessage(msg);
        }
    };

    /*Message handler*/
    private Handler device_handler = new Handler() {
        public void handleMessage(Message msg) {
            switch(msg.what) {
            case MSG_DEVICE_EVENT:
                Log.v(TAG,"%%%%%%%%%%%% MSG_DEVICE_EVENT" );
                resolveDeviceEvent((TVDevice.Event)msg.obj);
                break;

            }
        }
    };


    private Handler handler = new Handler() {
        public void handleMessage(Message msg) {
            switch(msg.what) {
            case MSG_SET_SOURCE:
                int val = (Integer)msg.obj;
                TVConst.SourceInput sourceinput = TVConst.SourceInput.values()[val];
                resolveSetInputSource(sourceinput);
                break;
            case MSG_PLAY_PROGRAM:
                resolvePlayProgram((TVPlayParams)msg.obj);
                break;
            case MSG_STOP_PLAYING:
                resolveStopPlaying();
                break;
            case MSG_START_TIMESHIFTING:
                resolveStartTimeshifting();
                break;
            case MSG_START_PLAYBACK:
                resolveStartPlayback((Integer)msg.obj);
                break;
            case MSG_START_SCAN:
                resolveStartScan((TVScanParams)msg.obj);
                break;
            case MSG_STOP_SCAN:
                resolveStopScan((Boolean)msg.obj);
                break;
            case MSG_START_RECORDING:
                resolveStartRecording((Integer)msg.obj);
                break;
            case MSG_STOP_RECORDING:
                resolveStopRecording();
                break;
            case MSG_PAUSE:
                resolvePause();
                break;
            case MSG_RESUME:
                resolveResume();
                break;
            case MSG_FAST_FORWARD:
                resolveFastForward((Integer)msg.obj);
                break;
            case MSG_FAST_BACKWARD:
                resolveFastBackward((Integer)msg.obj);
                break;
            case MSG_SEEK_TO:
                resolveSeekTo((Integer)msg.obj);
                break;


            case MSG_EPG_EVENT:
                resolveEpgEvent((TVEpgScanner.Event)msg.obj);
                break;
            case MSG_SCAN_EVENT:
                resolveScanEvent((TVScanner.Event)msg.obj);
                break;
            }
        }
    };

    private TVTime time = new TVTime();
    private TVConfig config;
    private TVDevice device = new TVDevice() {
        /*Device event device_handler*/
        public void onEvent(TVDevice.Event event) {
            Log.v(TAG,"onEvent  type:   " + event.type  );
            Message msg = device_handler.obtainMessage(MSG_DEVICE_EVENT, event);
            device_handler.sendMessage(msg);
            //resolveDeviceEvent(event);
        }
    };

    private TVScanner scanner = new TVScanner() {
        /*Scanner event handler*/
        public void onEvent(TVScanner.Event event) {
            Message msg = handler.obtainMessage(MSG_SCAN_EVENT, event);
            handler.sendMessage(msg);
        }
    };

    private TVEpgScanner epgScanner = new TVEpgScanner() {
        /*EPG event handler*/
        public void onEvent(TVEpgScanner.Event event) {
            switch(event.type) {
            case TVEpgScanner.Event.EVENT_TDT_END:
                time.setTime(event.time);
                return;
            }

            Message msg = handler.obtainMessage(MSG_EPG_EVENT, event);
            handler.sendMessage(msg);
        }
    };

    private enum TVStatus {
        STATUS_SET_INPUT_SOURCE,
        STATUS_SET_FRONTEND,
        STATUS_PLAY_ATV,
        STATUS_PLAY_DTV,
        STATUS_TIMESHIFTING,
        STATUS_PLAYBACK,
        STATUS_STOPPED,
        STATUS_SCAN
    }

    private TVStatus Tv_Status;
    private TVConst.SourceInput CurrentSourceInput;
    private TVConst.SourceInput ReqSourceInput;
    private TVPlayParams AtvPlayParams;
    private TVPlayParams DtvPlayParams;
    private int dtvProgramType = TVProgram.TYPE_TV;
    private TVChannelParams ChannelParams;
    private int ChannelID;
    private int ProgramID;
    private boolean ChannelLocked = false;
    private boolean Recording = false;
    private boolean IsScanning = false;

    private void stopPlaying() {
        if(Tv_Status == TVStatus.STATUS_PLAY_ATV) {
            device.stopATV();
            sendMessage(TVMessage.programStop(ProgramID));
            Tv_Status = TVStatus.STATUS_STOPPED;
        } else if(Tv_Status == TVStatus.STATUS_PLAY_DTV) {
            device.stopDTV();
            sendMessage(TVMessage.programStop(ProgramID));
            Tv_Status = TVStatus.STATUS_STOPPED;
        } else if(Tv_Status == TVStatus.STATUS_TIMESHIFTING) {
            device.stopTimeshifting();
            Tv_Status = TVStatus.STATUS_STOPPED;
        } else if(Tv_Status == TVStatus.STATUS_PLAYBACK) {
            device.stopPlayback();
            Tv_Status = TVStatus.STATUS_STOPPED;
        }
    }

    private void stopScan(boolean store) {
        if(Tv_Status == TVStatus.STATUS_SCAN) {
            scanner.stop(store);
            Tv_Status = TVStatus.STATUS_STOPPED;
        }
    }

    private void stopRecording() {
        if(Recording) {
            TVDevice.DTVRecordParams params;

            params = device.stopRecording();
            Recording = false;
        }
    }

    private boolean isInDTVMode() {
        if (CurrentSourceInput == TVConst.SourceInput.SOURCE_DTV)
            return true;
        return false;
    }

    private boolean isInATVMode() {
        if (CurrentSourceInput == TVConst.SourceInput.SOURCE_ATV)
            return true;
        return false;
    }

    private boolean isInFileMode() {
        if(isInDTVMode() || !isInATVMode())
            return false;

        if((Tv_Status == TVStatus.STATUS_TIMESHIFTING) ||
           (Tv_Status == TVStatus.STATUS_PLAYBACK))
            return true;

        return false;
    }

    private TVProgram playParamsToProgram(TVPlayParams params) {
        TVProgram p = null;

        try {
            switch(params.getType()) {
            case TVPlayParams.PLAY_PROGRAM_NUMBER:
                int type;

                if(CurrentSourceInput == TVConst.SourceInput.SOURCE_DTV)
                    type = dtvProgramType;
                else
                    type = TVProgram.TYPE_ATV;

                p = TVProgram.selectByNumber(this, type, params.getProgramNumber());
                break;
            case TVPlayParams.PLAY_PROGRAM_ID:
                p = TVProgram.selectByID(this, params.getProgramID());
                break;
            }
        } catch(Exception e) {
            Log.e(TAG, "playParamsToProgram failed");
        }

        return p;
    }

    private void playCurrentProgramAV() {
        if(CurrentSourceInput == TVConst.SourceInput.SOURCE_ATV) {
            TVProgram p;

            p = playParamsToProgram(AtvPlayParams);
            ProgramID = p.getID();

            device.playATV();

            sendMessage(TVMessage.programStart(ProgramID));

            Tv_Status = TVStatus.STATUS_PLAY_ATV;
        } else if(CurrentSourceInput == TVConst.SourceInput.SOURCE_DTV) {
            TVProgram p;
            TVProgram.Video video;
            TVProgram.Audio audio;
            int vpid = 0x1fff, apid = 0x1fff, vfmt = -1, afmt = -1;

            p = playParamsToProgram(DtvPlayParams);

            ProgramID = p.getID();

            video = p.getVideo();

            try {
                String lang = config.getString("player:audio:language");
                audio = p.getAudio(lang);
                apid = audio.getPID();
                afmt = audio.getFormat();
            } catch(Exception e) {
                audio = p.getAudio();
                if(audio != null) {
                    apid = audio.getPID();
                    afmt = audio.getFormat();
                }
            }

            if(video != null) {
                vpid = video.getPID();
                vfmt = video.getFormat();
            }

            Log.d(TAG, "play dtv video "+vpid+" format "+vfmt+" audio "+apid+" format "+vfmt);
            device.playDTV(vpid, vfmt, apid, vfmt);

            sendMessage(TVMessage.programStart(ProgramID));

            Tv_Status = TVStatus.STATUS_PLAY_DTV;
        }
    }

    private void playCurrentProgram() {
        TVProgram p = null;
        TVChannelParams fe_params;

        if(CurrentSourceInput == TVConst.SourceInput.SOURCE_ATV) {
            if(AtvPlayParams == null) {
                Tv_Status = TVStatus.STATUS_STOPPED;
                return;
            }
            p = playParamsToProgram(AtvPlayParams);
        } else if(CurrentSourceInput == TVConst.SourceInput.SOURCE_DTV) {
            if(DtvPlayParams == null) {
                Tv_Status = TVStatus.STATUS_STOPPED;
                return;
            }
            p = playParamsToProgram(DtvPlayParams);
        }

        ChannelID = p.getChannel().getID();
        fe_params = p.getChannel().getParams();

        if((ChannelParams == null) || !ChannelParams.equals(fe_params)) {
            ChannelParams = fe_params;
            ChannelLocked = false;

            device.setFrontend(fe_params);
            Tv_Status = TVStatus.STATUS_SET_FRONTEND;
            return;
        }

        playCurrentProgramAV();
    }

    /*Reset the input source.*/
    private void resolveSetInputSource(TVConst.SourceInput src) {

        if(src == ReqSourceInput)
            return;

        ReqSourceInput = src;

        if(isInDTVMode()) {
            stopPlaying();
            stopRecording();
            stopScan(false);
            Log.d(TAG, "Stop playing, recording and scaning in " + src.name());
        }
        Tv_Status = TVStatus.STATUS_SET_INPUT_SOURCE;
        device.setInputSource(src);
    }

    /*Play a program.*/
    private void resolvePlayProgram(TVPlayParams tp) {
        Log.d(TAG, "try to play program");

        TVProgram prog = playParamsToProgram(tp);
        if(prog == null)
            return;

        TVChannel chan = prog.getChannel();
        if(chan == null)
            return;

        if(chan.isAnalogMode() && (CurrentSourceInput == TVConst.SourceInput.SOURCE_ATV)) {
            AtvPlayParams = tp;
            playCurrentProgram();
        } else if(!chan.isAnalogMode() && (CurrentSourceInput == TVConst.SourceInput.SOURCE_DTV)) {
            DtvPlayParams = tp;
            playCurrentProgram();
        } else {
            if(chan.isAnalogMode())
                AtvPlayParams = tp;
            else
                DtvPlayParams = tp;
        }
    }

    /*Stop playing.*/
    private void resolveStopPlaying() {
        if(!isInDTVMode())
            return;
        stopPlaying();
    }

    /*Start timeshifting.*/
    private void resolveStartTimeshifting() {
        if(!isInDTVMode())
            return;
    }

    /*Start DVR playback.*/
    private void resolveStartPlayback(int bookingID) {
        if(!isInDTVMode())
            return;
    }

    /*Start channel scanning.*/
    private void resolveStartScan(TVScanParams sp) {
        /*if(!isInDTVMode())
            return;
        */

        /** Configure scan */
        TVScanner.TVScannerParams tsp = new TVScanner.TVScannerParams(sp);
        /** Set params from config */
        try {
            tsp.setAtvParams(config.getInt("tv:scan:atv:minfreq") , config.getInt("tv:scan:atv:maxfreq"),
                             config.getInt("tv:scan:atv:vidstd"), config.getInt("tv:scan:atv:audstd"));
        } catch (Exception e) {
            e.printStackTrace();
            Log.d(TAG, "Cannot read atv config !!!");
            return;
        }

        int[] freqList = null;
        if (sp.getTvMode() != TVScanParams.TV_MODE_ATV &&
            sp.getDtvMode() == TVScanParams.DTV_MODE_ALLBAND) {
            String region;
            /** load the frequency list */
            try {
                region = config.getString("tv:scan:dtv:region");
            } catch (Exception e) {
                e.printStackTrace();
                Log.d(TAG, "Cannot read dtv region !!!");
                return;
            }

            Cursor c = this.getContentResolver().query(TVDataProvider.RD_URL, null,
                       "select * from region_table where name='" + region + "' and source=" + sp.getTsSourceID(),
                       null, null);
            if(c != null) {
                if(c.moveToFirst()) {
                    int col = c.getColumnIndex("frequencies");
                    String freqs = c.getString(col);

                    if (freqs != null && freqs.length() > 0) {
                        String[] flist = freqs.split(" ");

                        if (flist !=null && flist.length > 0) {
                            freqList = new int[flist.length];
                            /** get each frequency */
                            for (int i=0; i<freqList.length; i++) {
                                freqList[i] = Integer.parseInt(flist[i]);
                            }
                        }
                    }
                }
                c.close();
            }
        }

        tsp.setDtvParams(TVDataProvider.getDatabaseNativeHandle(), 0, freqList);

        /** No exceptions, start scan */
        stopPlaying();
        stopRecording();
        stopScan(false);

        ChannelParams = null;

        synchronized(this) {
            device.freeFrontend();
            IsScanning = true;
        }

        scanner.scan(tsp);
        Tv_Status = TVStatus.STATUS_SCAN;
    }

    /*Stop scanning process.*/
    private void resolveStopScan(boolean store) {
        /*if(!isInDTVMode())
            return;
            */

        stopScan(store);

        synchronized(this) {
            IsScanning = false;
        }

        //playCurrentProgram();
    }

    /*Start Recording.*/
    private void resolveStartRecording(int bookingID) {
        stopRecording();
    }

    /*Stop Recording.*/
    private void resolveStopRecording() {
    }

    /*Pause.*/
    private void resolvePause() {
        if(!isInFileMode())
            return;

        device.pause();
    }

    /*Resume.*/
    private void resolveResume() {
        if(!isInFileMode())
            return;

        device.resume();
    }

    /*Fast forward.*/
    private void resolveFastForward(int speed) {
        if(!isInFileMode())
            return;

        device.fastForward(speed);
    }

    /*Fast backward.*/
    private void resolveFastBackward(int speed) {
        if(!isInFileMode())
            return;

        device.fastBackward(speed);
    }

    /*Seek to a new position.*/
    private void resolveSeekTo(int pos) {
        if(!isInFileMode())
            return;

        device.seekTo(pos);
    }

    /*Solve the events from the device.*/
    private void resolveDeviceEvent(TVDevice.Event event) {
        Log.d(TAG, "resolveDeviceEvent type :"+event.type);
        switch(event.type) {
        case TVDevice.Event.EVENT_SET_INPUT_SOURCE_OK:
            Log.d(TAG, "set input source to "+ReqSourceInput.name()+" ok");
            CurrentSourceInput = ReqSourceInput;
            if(isInDTVMode() || isInATVMode()) {
                if (Tv_Status == TVStatus.STATUS_SET_INPUT_SOURCE) {
                    Log.d(TAG, "%%%%set input source == TVStatus.STATUS_SET_INPUT_SOURCE");
                    //playCurrentProgram();
                }
            }
            break;
        case TVDevice.Event.EVENT_SET_INPUT_SOURCE_FAILED:
            Log.e(TAG, "set input source to "+ReqSourceInput.name()+" failed");
            break;
        case TVDevice.Event.EVENT_FRONTEND:
            if(isInDTVMode() || isInATVMode()) {
                if(ChannelParams!=null )
                    if( event.feParams.isAnalogMode() || (event.feParams.equals(ChannelParams))) {
                        if((Tv_Status == TVStatus.STATUS_SET_FRONTEND) && (event.feStatus & TVChannelParams.FE_HAS_LOCK)!=0) {
                            Log.v(TAG, "STATUS_SET_FRONTEND playCurrentProgramAV");
                            playCurrentProgramAV();
                        }
                        Log.v(TAG, "Tv_Status  "+Tv_Status+ "event.feStatus  " +event.feStatus);
                        if(ChannelLocked && (event.feStatus & TVChannelParams.FE_HAS_LOCK)!=0) {
                            Log.d(TAG, "signal resume");
                            sendMessage(TVMessage.signalResume(ChannelID));
                        } else if(!ChannelLocked && (event.feStatus & TVChannelParams.FE_TIMEDOUT)!=0) {
                            Log.d(TAG, "signal lost");
                            sendMessage(TVMessage.signalLost(ChannelID));
                        }
                    }
            }


            break;
        }
    }

    /*Solve the events from the EPG scanner.*/
    private void resolveEpgEvent(TVEpgScanner.Event event) {
    }

    /*Solve the events from the channel scanner.*/
    private void resolveScanEvent(TVScanner.Event event) {
        Log.d(TAG, "Channel scan event: " + event.type);
        switch (event.type) {
        case TVScanner.Event.EVENT_SCAN_PROGRESS:
            Log.d(TAG, "Progress: " + event.percent + "%" + ", channel no. "+event.channelNumber);
            if (event.programName != null) {
                Log.d(TAG, "New Program : "+ event.programName + ", type "+ event.programType);
            }
            sendMessage(TVMessage.scanUpdate(event.percent, event.channelNumber, event.totalChannelCount,
                                             event.channelParams, event.lockedStatus, event.programName, event.programType));
            break;
        case TVScanner.Event.EVENT_STORE_BEGIN:
            Log.d(TAG, "Store begin...");
            sendMessage(TVMessage.scanStoreBegin());
            break;
        case TVScanner.Event.EVENT_STORE_END:
            TVDataProvider.syncToFile();
            Log.d(TAG, "Store end");
            sendMessage(TVMessage.scanStoreEnd());
            break;
        case TVScanner.Event.EVENT_SCAN_END:
            Log.d(TAG, "Scan end");
            sendMessage(TVMessage.scanEnd());
            break;
        default:
            break;

        }
    }

    public IBinder onBind (Intent intent) {
        return mBinder;
    }

    public void onCreate() {
        super.onCreate();
        TVDataProvider.openDatabase(this);
        config = new TVConfig(this);
    }

    public void onDestroy() {
        callbacks.kill();
        TVDataProvider.closeDatabase(this);
        super.onDestroy();
    }
}

