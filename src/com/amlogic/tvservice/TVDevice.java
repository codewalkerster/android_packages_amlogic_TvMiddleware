package com.amlogic.tvservice;

import com.amlogic.tvutil.TVChannelParams;
import com.amlogic.tvutil.TVProgram;
import com.amlogic.tvutil.TVConst;
import java.io.File;
import android.util.Log;
import android.amlogic.Tv;
import android.amlogic.Tv.Frontend_Para;
import android.amlogic.Tv.SourceSwitchListener;
import android.amlogic.Tv.SrcInput;
import android.amlogic.Tv.StatusTVChangeListener;
import android.os.Handler;
import android.os.Message;

abstract public class TVDevice implements StatusTVChangeListener,SourceSwitchListener
{
    public class DTVRecordParams
    {
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

    public class Event
    {
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

        public Event(int type) {
            this.type = type;
        }
    }

    Handler handler = new Handler() {
        //Event event  = new Event();
        public void handleMessage(Message msg) {
            int status = 0;
            switch (msg.what) {
            case EVENT_FRONTEND:
                status = (Integer)msg.obj;
                Event myEvent = new Event(Event.EVENT_FRONTEND);
                if(status == NATVIVE_EVENT_SIGNAL_OK ) {
                    Log.v(TAG,"NATVIVE_EVENT_SIGNAL_OK");
                    myEvent.feStatus = TVChannelParams.FE_HAS_LOCK;
                } else if(status == NATVIVE_EVENT_SIGNAL_NOT_OK ) {
                    Log.v(TAG,"NATVIVE_EVENT_SIGNAL_NOT_OK");
                    myEvent.feStatus = TVChannelParams.FE_TIMEDOUT;
                }

                int mode = msg.getData().getInt("mode");
                int freq = msg.getData().getInt("freq");
                int para1 = msg.getData().getInt("para1");
                int para2 = msg.getData().getInt("para2");
                Log.v(TAG,"mode freq para1 para2:" + mode + "," + freq+ "," + para1+ "," + para2);
                myEvent.feParams   = fpara2chanpara(mode,freq,para1,para2);
                /*
                switch(mode){
                    case TVChannelParams.MODE_OFDM:

                        break;
                    case TVChannelParams.MODE_QAM:
                        Log.v(TAG,"NATVIVE_EVENT_SIGNAL_OK MODE_QAM");
                        myEvent.feParams  = TVChannelParams.dvbcParams(freq, para2, para1);
                        break;
                    case TVChannelParams.MODE_ANALOG:
                        //*****************temp default set pat I************************
                        Log.v(TAG,"NATVIVE_EVENT_SIGNAL_OK MODE_ANALOG");
                        myEvent.feParams  = TVChannelParams.analogParams(freq, TVChannelParams.STD_PAL_I, 0);
                        break;
                }*/
                TVDevice.this.onEvent(myEvent);
                break;


            case EVENT_SOURCE_SWITCH:
                status = (Integer)msg.obj;
                Event myEvent1 = new Event(Event.EVENT_SET_INPUT_SOURCE_OK);
                TVDevice.this.onEvent(myEvent1);
                break;
            }
        }
    };

	
    private boolean destroy;
    private String  TAG = "TVDevice";
    public static Tv tv = null;
    public static final int NATVIVE_EVENT_FRONTEND      =   1;
    public static final int NATVIVE_EVENT_PLAYER        =   2;
    public static final int NATVIVE_EVENT_SIGNAL_OK     =   1;
    public static final int NATVIVE_EVENT_SIGNAL_NOT_OK     =   0;

    public static final int EVENT_FRONTEND              =   1<<1;
    public static final int EVENT_SOURCE_SWITCH             =   1<<2;




    public TVDevice() {
        destroy = false;
        //native_device_init();

        tv = SingletonTv.getTvInstance();
        tv.SetStatusTVChangeListener(this);
        tv.SetSourceSwitchListener(this);
        //tv.INIT_TV();

    }

    public void setInputSource(TVConst.SourceInput source) {
        //native_set_input_source(source.ordinal());
        Log.v(TAG,"setInputSource " + source.toString());
        Log.v(TAG, "^^^^^^^^^^^^^^^^^ & ^------^^ & ^^^^^^^^^^^^^^^^^^^^^^^^^^^");
        if(source == TVConst.SourceInput.SOURCE_DTV)
            tv.SetSourceInput(Tv.SrcInput.DTV);
        else if(source == TVConst.SourceInput.SOURCE_ATV)
            tv.SetSourceInput(Tv.SrcInput.TV);
        else if(source == TVConst.SourceInput.SOURCE_AV1)
            tv.SetSourceInput(Tv.SrcInput.AV1);
        else if(source == TVConst.SourceInput.SOURCE_AV2)
            tv.SetSourceInput(Tv.SrcInput.AV2);
        else if(source == TVConst.SourceInput.SOURCE_YPBPR1)
            tv.SetSourceInput(Tv.SrcInput.YPBPR1);
        else if(source == TVConst.SourceInput.SOURCE_YPBPR2)
            tv.SetSourceInput(Tv.SrcInput.YPBPR2);
        else if(source == TVConst.SourceInput.SOURCE_HDMI1)
            tv.SetSourceInput(Tv.SrcInput.HDMI1);
        else if(source == TVConst.SourceInput.SOURCE_HDMI2)
            tv.SetSourceInput(Tv.SrcInput.HDMI2);
        else if(source == TVConst.SourceInput.SOURCE_HDMI3)
            tv.SetSourceInput(Tv.SrcInput.HDMI3);
        else if(source == TVConst.SourceInput.SOURCE_VGA)
            tv.SetSourceInput(Tv.SrcInput.VGA);
        else if(source == TVConst.SourceInput.SOURCE_MPEG)
            tv.SetSourceInput(Tv.SrcInput.MPEG);
        else if(source == TVConst.SourceInput.SOURCE_SVIDEO)
            tv.SetSourceInput(Tv.SrcInput.SVIDEO);
        //**********************temp************************
        //Event myEvent = new Event(Event.EVENT_SET_INPUT_SOURCE_OK);
        //this.onEvent(myEvent);
        //*********************finish************************

    }

    public int getCurInputSource() {
     
        int source = tv.GetCurrentSourceInput();
        Log.v(TAG, "^^^^^^^^^^^^^^^^getCurInputSource^^^^^^^^^^^^^^^^^^^^^^^^^^^" + source);
        return source;
    }
    
    
    public void setFrontend(TVChannelParams params) {
        //native_set_frontend(params);
        tv.INIT_TV();
        if(params.mode == TVChannelParams.MODE_QAM)
            tv.SetFrontEnd(params.mode,params.frequency,params.symbolRate,params.modulation);
        if(params.mode == TVChannelParams.MODE_ANALOG)
            tv.SetFrontEnd(params.mode,params.frequency,params.standard,0);
    }

    public void getFrontend(TVChannelParams params) {
        //return native_get_frontend();
        //tv.INIT_TV();
        Tv.Frontend_Para fpara = tv.GetFrontEnd();
        params = fpara2chanpara(fpara.mode,fpara.frequency,fpara.para1,fpara.para2);
    }

    public int getFrontendStatus() {
        //return native_get_frontend_status();
        Log.v(TAG,"getfrontendstatus is"+tv.Get_FrontendStatus());
        return tv.Get_FrontendStatus();
    }

    public int getFrontendSignalStrength() {
        //return native_get_frontend_signal_strength();
        Log.v(TAG,"getfrontendsignalstrength is"+tv.Get_FrontendSignalStrength());
        return tv.Get_FrontendSignalStrength();
    }

    public int getFrontendSNR() {
        //return native_get_frontend_snr();
        Log.v(TAG,"getfrontendsnr"+tv.Get_FrontendSNR());
        return tv.Get_FrontendSNR();
    }

    public int getFrontendBER() {
        //return native_get_frontend_ber();
        Log.v(TAG,"getfrontendber"+tv.Get_FrontendBER());
        return tv.Get_FrontendBER();
    }

    public void freeFrontend() {
        //native_free_frontend();
        tv.FreeFrontEnd();

    }

    public void startVBI(int flags) {
        Log.e(TAG,"*********startVBI have not realize");
        //native_start_vbi(flags);
    }

    public void stopVBI(int flags) {
        Log.e(TAG,"*********stopVBI have not realize");
        //native_stop_vbi(flags);
    }

    public void playATV() {
        //native_play_atv();
        tv.StartTV((int)TVConst.SourceInput.SOURCE_ATV.ordinal(),  0 , 0 , 0 , 0);
    }

    public void stopATV() {
        //native_stop_atv();
        Log.v(TAG,"stopATV");
        tv.StopTV((int)TVConst.SourceInput.SOURCE_ATV.ordinal());
    }

    public void playDTV(int vpid, int vfmt, int apid, int afmt) {
        //native_play_dtv(vpid, vfmt, apid, afmt);
        Log.v(TAG,"playDTV" + (int)TVConst.SourceInput.SOURCE_DTV.ordinal());
        tv.StartTV((int)TVConst.SourceInput.SOURCE_DTV.ordinal(),  vpid ,  apid , vfmt , afmt);
    }



    public void stopDTV() {
        //native_stop_dtv();
        Log.v(TAG,"stopDTV");
        tv.StopTV((int)TVConst.SourceInput.SOURCE_DTV.ordinal());
    }

    public void startRecording(DTVRecordParams params) {
        Log.e(TAG,"*********startRecording have not realize");
        //native_start_recording(params);
    }

    public DTVRecordParams stopRecording() {
        Log.e(TAG,"*********stopRecording have not realize");
        //return native_stop_recording();
        return null;
    }

    public void startTimeshifting(DTVRecordParams params) {
    }

    public DTVRecordParams stopTimeshifting() {
        //return native_stop_timeshifting();
        Log.e(TAG,"*********stopTimeshifting have not realize");
        return null;
    }

    public void startPlayback(DTVRecordParams params) {
        //native_start_playback(params);
        Log.e(TAG,"*********startPlayback have not realize");
    }

    public void stopPlayback() {
        //native_stop_playback();
        Log.e(TAG,"*********stopPlayback have not realize");
    }

    public void pause() {
        //native_pause();
        Log.e(TAG,"*********pause have not realize");
    }

    public void resume() {
        //native_resume();
        Log.e(TAG,"*********resume have not realize");
    }

    public void fastForward(int speed) {
        //native_fast_forward(speed);
        Log.e(TAG,"*********fastForward have not realize");
    }

    public void fastBackward(int speed) {
        //native_fast_backward(speed);
        Log.e(TAG,"*********fastBackward have not realize");
    }

    public void seekTo(int pos) {
        //native_seek_to(pos);
        Log.e(TAG,"*********seekTo have not realize");
    }

    abstract public void onEvent(Event event);

    protected void finalize() throws Throwable {
        if(!destroy) {
            destroy = false;
            //native_device_destroy();
            Log.e(TAG,"*********finalize have not realize");
        }
    }

    public void onStatusTVChange(int type,int state,int mode,int freq,int para1,int para2) {
        // TODO Auto-generated method stub
        Log.v(TAG, "onStatusDTVChange:  " +type + "  " +state + "  " +mode + "  " +freq + "  " +para1 + "  " +para2);
        Message msg;

        if(type == NATVIVE_EVENT_FRONTEND ) { //frontEnd
            msg = handler.obtainMessage(EVENT_FRONTEND, new Integer(state));
            msg.getData().clear();
            msg.getData().putInt("mode", mode);
            msg.getData().putInt("freq", freq);
            msg.getData().putInt("para1", para1);
            msg.getData().putInt("para2", para2);
            handler.sendMessage(msg);
        }
    }


    public void onSourceSwitchStatusChange(SrcInput input, int state) {
        Log.v(TAG,"onSourceSwitchStatusChange:  " + input.toString() + state);
        Message  msg = handler.obtainMessage(EVENT_SOURCE_SWITCH, new Integer(state));
        handler.sendMessage(msg);
    }


    TVChannelParams fpara2chanpara(int mode,int freq,int para1,int para2) {
        Log.v(TAG,"mode freq para1 para2:" + mode + "," + freq+ "," + para1+ "," + para2);
        TVChannelParams tvChannelPara = null;
        switch(mode) {
        case TVChannelParams.MODE_OFDM:

            break;
        case TVChannelParams.MODE_QAM:
            Log.v(TAG,"NATVIVE_EVENT_SIGNAL_OK MODE_QAM");
            tvChannelPara  = TVChannelParams.dvbcParams(freq, para2, para1);
            break;
        case TVChannelParams.MODE_ANALOG:
            //*****************temp default set pat I************************
            Log.v(TAG,"NATVIVE_EVENT_SIGNAL_OK MODE_ANALOG");
            tvChannelPara  = TVChannelParams.analogParams(freq, TVChannelParams.STD_PAL_I, 0);
            break;
        }

        return tvChannelPara;
    }
}

class SingletonTv
{
    static   Tv  instance = null;
    public synchronized static  Tv getTvInstance() {
        if (instance == null) {
            instance = Tv.open();
        }
        return instance;
    }
}

