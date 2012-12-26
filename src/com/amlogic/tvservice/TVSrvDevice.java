package com.amlogic.tvservice;


import com.amlogic.tvutil.TVChannelParams;
import com.amlogic.tvutil.TVConfigValue;
import com.amlogic.tvutil.TVConfigValue.TypeException;
import com.amlogic.tvutil.TVProgram;
import com.amlogic.tvutil.TVConst;
import java.io.File;
import android.util.Log;
import android.amlogic.Tv;
import android.amlogic.Tv.Frontend_Para;
import android.amlogic.Tv.SourceSwitchListener;
import android.amlogic.Tv.SrcInput;
import android.amlogic.Tv.StatusTVChangeListener;
import android.amlogic.Tv.tvin_info_t;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;

public abstract class TVDeviceImpl extends TVDevice implements StatusTVChangeListener, SourceSwitchListener
{
    private boolean destroy;
    private String TAG = "TVDeviceImpl";
    public static Tv tv = null;
    private Looper mylooper = null;
    public static final int NATVIVE_EVENT_FRONTEND = 1;
    public static final int NATVIVE_EVENT_PLAYER = 2;
    public static final int NATVIVE_EVENT_SIGNAL_OK = 1;
    public static final int NATVIVE_EVENT_SIGNAL_NOT_OK = 0;

    public static final int EVENT_FRONTEND = 1 << 1;
    public static final int EVENT_SOURCE_SWITCH = 1 << 2;
    deviceHandler myHandler = null;
	public TVDeviceImpl()
    {
        super();
        tv = SingletonTv.getTvInstance();
        tv.SetStatusTVChangeListener(this);
        tv.SetSourceSwitchListener(this);
        // tv.INIT_TV();

    }
    
    public TVDeviceImpl(Looper looper)
    {
        super();
        mylooper = looper;
    
        tv = SingletonTv.getTvInstance();
        tv.SetStatusTVChangeListener(this);
        tv.SetSourceSwitchListener(this);
        // tv.INIT_TV();
        myHandler = new deviceHandler(looper);
    }

    public class TVException extends Exception
    {
    }

    private class deviceHandler extends Handler
    {

        public deviceHandler(Looper looper)
        {
            super(looper);

        }

        @Override
        public void handleMessage(Message msg)
        {
            switch (msg.what)
            {
                case 1:
                    
                    Log.v(TAG, "deviceHandler handleMessage");
                    Bundle bundle = msg.getData();
                    int type = bundle.getInt("type");
                    int state = bundle.getInt("state");
                    int mode = bundle.getInt("mode");
                    int freq = bundle.getInt("freq");
                    int para1 = bundle.getInt("para1");
                    int para2 = bundle.getInt("para2");

                    if (type == NATVIVE_EVENT_FRONTEND)
                    { // frontEnd
                        Log.v(TAG, "handleMessage onStatusDTVChange:  " + type + "  " + state + "  " + mode + "  " + freq + "  " + para1 + "  "
                                + para2);
                        Event myEvent = new Event(Event.EVENT_FRONTEND);
                        if (state == NATVIVE_EVENT_SIGNAL_OK)
                        {
                            Log.v(TAG, "NATVIVE_EVENT_SIGNAL_OK");
                            myEvent.feStatus = TVChannelParams.FE_HAS_LOCK;
                        }
                        else if (state == NATVIVE_EVENT_SIGNAL_NOT_OK)
                        {
                            Log.v(TAG, "NATVIVE_EVENT_SIGNAL_NOT_OK");
                            myEvent.feStatus = TVChannelParams.FE_TIMEDOUT;
                        }
                        myEvent.feParams = fpara2chanpara(mode, freq, para1, para2);
                        
                        onEvent(myEvent);
                    }

                    break;
            }
        }
    }
    
  

    public void setInputSource(TVConst.SourceInput source)
    {
        // native_set_input_source(source.ordinal());
        Log.v(TAG, "setInputSource " + source.toString());
        Log.v(TAG, "^^^^^^^^^^^^^^^^^ & ^------^^ & ^^^^^^^^^^^^^^^^^^^^^^^^^^^");
        if (source == TVConst.SourceInput.SOURCE_DTV)
            tv.SetSourceInput(Tv.SrcInput.DTV);
        else if (source == TVConst.SourceInput.SOURCE_ATV)
            tv.SetSourceInput(Tv.SrcInput.TV);
        else if (source == TVConst.SourceInput.SOURCE_AV1)
            tv.SetSourceInput(Tv.SrcInput.AV1);
        else if (source == TVConst.SourceInput.SOURCE_AV2)
            tv.SetSourceInput(Tv.SrcInput.AV2);
        else if (source == TVConst.SourceInput.SOURCE_YPBPR1)
            tv.SetSourceInput(Tv.SrcInput.YPBPR1);
        else if (source == TVConst.SourceInput.SOURCE_YPBPR2)
            tv.SetSourceInput(Tv.SrcInput.YPBPR2);
        else if (source == TVConst.SourceInput.SOURCE_HDMI1)
            tv.SetSourceInput(Tv.SrcInput.HDMI1);
        else if (source == TVConst.SourceInput.SOURCE_HDMI2)
            tv.SetSourceInput(Tv.SrcInput.HDMI2);
        else if (source == TVConst.SourceInput.SOURCE_HDMI3)
            tv.SetSourceInput(Tv.SrcInput.HDMI3);
        else if (source == TVConst.SourceInput.SOURCE_VGA)
            tv.SetSourceInput(Tv.SrcInput.VGA);
        else if (source == TVConst.SourceInput.SOURCE_MPEG)
            tv.SetSourceInput(Tv.SrcInput.MPEG);
        else if (source == TVConst.SourceInput.SOURCE_SVIDEO)
            tv.SetSourceInput(Tv.SrcInput.SVIDEO);
        // **********************temp************************
        // Event myEvent = new Event(Event.EVENT_SET_INPUT_SOURCE_OK);
        // this.onEvent(myEvent);
        // *********************finish************************

    }

    public TVConst.SourceInput getCurInputSource()
    {

        int val = tv.GetCurrentSourceInput();
        TVConst.SourceInput source = TVConst.SourceInput.SOURCE_ATV;

        Log.v(TAG, "^^^^^^^^^^^^^^^^getCurInputSource^^^^^^^^^^^^^^^^^^^^^^^^^^^" + val);

        if (val == Tv.SrcInput.DTV.toInt())
        {
            source = TVConst.SourceInput.SOURCE_DTV;
        }
        else if (val == Tv.SrcInput.TV.toInt())
        {
            source = TVConst.SourceInput.SOURCE_ATV;
        }
        else if (val == Tv.SrcInput.AV1.toInt())
        {
            source = TVConst.SourceInput.SOURCE_AV1;
        }
        else if (val == Tv.SrcInput.AV2.toInt())
        {
            source = TVConst.SourceInput.SOURCE_AV2;
        }
        else if (val == Tv.SrcInput.YPBPR1.toInt())
        {
            source = TVConst.SourceInput.SOURCE_YPBPR1;
        }
        else if (val == Tv.SrcInput.YPBPR2.toInt())
        {
            source = TVConst.SourceInput.SOURCE_YPBPR2;
        }
        else if (val == Tv.SrcInput.HDMI1.toInt())
        {
            source = TVConst.SourceInput.SOURCE_HDMI1;
        }
        else if (val == Tv.SrcInput.HDMI2.toInt())
        {
            source = TVConst.SourceInput.SOURCE_HDMI2;
        }
        else if (val == Tv.SrcInput.HDMI3.toInt())
        {
            source = TVConst.SourceInput.SOURCE_HDMI3;
        }
        else if (val == Tv.SrcInput.VGA.toInt())
        {
            source = TVConst.SourceInput.SOURCE_VGA;
        }
        else if (val == Tv.SrcInput.MPEG.toInt())
        {
            source = TVConst.SourceInput.SOURCE_MPEG;
        }
        else if (val == Tv.SrcInput.SVIDEO.toInt())
        {
            source = TVConst.SourceInput.SOURCE_SVIDEO;
        }

        return source;
    }

    public void setVideoWindow(int x, int y, int w, int h)
    {
    }

    public void switchDTVAudio(int pid, int afmt)
    {
    }

    public void setFrontend(TVChannelParams params)
    {
        // native_set_frontend(params);
        tv.INIT_TV();
        if (params.mode == TVChannelParams.MODE_QAM)
            tv.SetFrontEnd(params.mode, params.frequency, params.symbolRate, params.modulation);
        else if (params.mode == TVChannelParams.MODE_ANALOG)
            tv.SetFrontEnd(params.mode, params.frequency, params.standard, 0);
        else if (params.mode == TVChannelParams.MODE_OFDM)
            tv.SetFrontEnd(params.mode, params.frequency, params.bandwidth, 0);
    }

    public TVChannelParams getFrontend()
    {
        TVChannelParams params;
        // return native_get_frontend();
        // tv.INIT_TV();
        Tv.Frontend_Para fpara = tv.GetFrontEnd();
        params = fpara2chanpara(fpara.mode, fpara.frequency, fpara.para1, fpara.para2);

        return params;
    }

    public int getFrontendStatus()
    {
        // return native_get_frontend_status();
        Log.v(TAG, "getfrontendstatus is" + tv.Get_FrontendStatus());
        return tv.Get_FrontendStatus();
    }

    public int getFrontendSignalStrength()
    {
        // return native_get_frontend_signal_strength();
        Log.v(TAG, "getfrontendsignalstrength is" + tv.Get_FrontendSignalStrength());
        return tv.Get_FrontendSignalStrength();
    }

    public int getFrontendSNR()
    {
        // return native_get_frontend_snr();
        Log.v(TAG, "getfrontendsnr" + tv.Get_FrontendSNR());
        return tv.Get_FrontendSNR();
    }

    public int getFrontendBER()
    {
        // return native_get_frontend_ber();
        Log.v(TAG, "getfrontendber" + tv.Get_FrontendBER());
        return tv.Get_FrontendBER();
    }

    public void freeFrontend()
    {
        // native_free_frontend();
        tv.FreeFrontEnd();

    }

    public void startVBI(int flags)
    {
        Log.e(TAG, "*********startVBI have not realize");
        // native_start_vbi(flags);
    }

    public void stopVBI(int flags)
    {
        Log.e(TAG, "*********stopVBI have not realize");
        // native_stop_vbi(flags);
    }

    public void playATV()
    {
        // native_play_atv();
        tv.StartTV((int) TVConst.SourceInput.SOURCE_ATV.ordinal(), 0, 0, 0, 0);
    }

    public void resetATVFormat(TVChannelParams params)
    {
        Log.v(TAG, "resetATVFormat");
        if (params.mode == TVChannelParams.MODE_ANALOG)
            tv.SetFrontEnd(params.mode, params.frequency, params.standard, 0);
    }

    public void stopATV()
    {
        // native_stop_atv();
        Log.v(TAG, "temp not handle stop atv");
        // tv.StopTV((int)TVConst.SourceInput.SOURCE_ATV.ordinal());
    }

    public void playDTV(int vpid, int vfmt, int apid, int afmt)
    {
        // native_play_dtv(vpid, vfmt, apid, afmt);
        Log.v(TAG, "SourceInput SOURCE_DTV" + (int) TVConst.SourceInput.SOURCE_DTV.ordinal());
        tv.StartTV((int) TVConst.SourceInput.SOURCE_DTV.ordinal(), vpid, apid, vfmt, afmt);
    }

    public void stopDTV()
    {
        // native_stop_dtv();
        Log.v(TAG, "stopDTV");
        tv.StopTV((int) TVConst.SourceInput.SOURCE_DTV.ordinal());
    }

    public void startRecording(DTVRecordParams params)
    {
        Log.e(TAG, "*********startRecording have not realize");
        // native_start_recording(params);
    }

    public DTVRecordParams stopRecording()
    {
        Log.e(TAG, "*********stopRecording have not realize");
        // return native_stop_recording();
        return null;
    }

    public void startTimeshifting(DTVRecordParams params)
    {
    }

    public DTVRecordParams stopTimeshifting()
    {
        // return native_stop_timeshifting();
        Log.e(TAG, "*********stopTimeshifting have not realize");
        return null;
    }

    public void startPlayback(DTVRecordParams params)
    {
        // native_start_playback(params);
        Log.e(TAG, "*********startPlayback have not realize");
    }

    public void stopPlayback()
    {
        // native_stop_playback();
        Log.e(TAG, "*********stopPlayback have not realize");
    }

    public void pause()
    {
        // native_pause();
        Log.e(TAG, "*********pause have not realize");
    }

    public void resume()
    {
        // native_resume();
        Log.e(TAG, "*********resume have not realize");
    }

    public void fastForward(int speed)
    {
        // native_fast_forward(speed);
        Log.e(TAG, "*********fastForward have not realize");
    }

    public void fastBackward(int speed)
    {
        // native_fast_backward(speed);
        Log.e(TAG, "*********fastBackward have not realize");
    }

    public void seekTo(int pos)
    {
        // native_seek_to(pos);
        Log.e(TAG, "*********seekTo have not realize");
    }

    protected void finalize() throws Throwable
    {
        if (!destroy)
        {
            destroy = false;
            // native_device_destroy();
            Log.e(TAG, "*********finalize have not realize");
        }
    }

    public void onStatusTVChange(int type, int state, int mode, int freq, int para1, int para2)
    {
        // TODO Auto-generated method stub
        Log.v(TAG, "onStatusDTVChange:  " + type + "  " + state + "  " + mode + "  " + freq + "  " + para1 + "  " + para2);
        if (type == NATVIVE_EVENT_FRONTEND)
        {
            // frontEnd
            if (mode == TVChannelParams.MODE_ANALOG)
            {
                myHandler.removeMessages(1);
                Message msg = myHandler.obtainMessage(1);
                Bundle bundle = new Bundle();
                bundle.putInt("type", type);
                bundle.putInt("state", state);
                bundle.putInt("mode", mode);
                bundle.putInt("freq", freq);
                bundle.putInt("para1", para1);
                bundle.putInt("para2", para2);
                msg.setData(bundle);
                myHandler.sendMessageDelayed(msg, 1000);
            }
            else
            {
                Event myEvent = new Event(Event.EVENT_FRONTEND);
                if (state == NATVIVE_EVENT_SIGNAL_OK)
                {
                    Log.v(TAG, "NATVIVE_EVENT_SIGNAL_OK");
                    myEvent.feStatus = TVChannelParams.FE_HAS_LOCK;
                }
                else if (state == NATVIVE_EVENT_SIGNAL_NOT_OK)
                {
                    Log.v(TAG, "NATVIVE_EVENT_SIGNAL_NOT_OK");
                    myEvent.feStatus = TVChannelParams.FE_TIMEDOUT;
                }

                myEvent.feParams = fpara2chanpara(mode, freq, para1, para2);
                onEvent(myEvent);
            }
        }

    }

    public void onSourceSwitchStatusChange(SrcInput input, int state)
    {
        Log.v(TAG, "onSourceSwitchStatusChange:  " + input.toString() + state);

        Event myEvent = new Event(Event.EVENT_SET_INPUT_SOURCE_OK);
        myEvent.source = input.ordinal();
        onEvent(myEvent);
    }

    TVChannelParams fpara2chanpara(int mode, int freq, int para1, int para2)
    {
        Log.v(TAG, "mode freq para1 para2:" + mode + "," + freq + "," + para1 + "," + para2);
        TVChannelParams tvChannelPara = null;
        switch (mode)
        {
            case TVChannelParams.MODE_OFDM:
                Log.v(TAG, " MODE_OFDM");
                tvChannelPara = TVChannelParams.dvbtParams(freq, para1);
                break;
            case TVChannelParams.MODE_QAM:
                Log.v(TAG, " MODE_QAM");
                tvChannelPara = TVChannelParams.dvbcParams(freq, para2, para1);
                break;
            case TVChannelParams.MODE_ANALOG:

                Log.v(TAG, " MODE_ANALOG");
                tvChannelPara = TVChannelParams.analogParams(freq, para1, 0);
                break;
        }

        return tvChannelPara;
    }

    @Override
    public void onUpdate(String name, TVConfigValue value)
    {
        // TODO Auto-generated method stub
        /*****************tv setting***********************/
        Log.v(TAG, "SET " + name + ": " + value);
        String configType = "Set";
        if (TVDeviceImpl.tv == null)
            try
            {
                throw new TVException();
            }
            catch (TVException e)
            {
                e.printStackTrace();
            }

        int pos = name.indexOf(":");
        if (pos == -1)
            Log.e(TAG, "set name fail");
        name = name.substring(pos + 1);
        name = configType + name;
        Log.v(TAG, "SET NEW " + name);

        int type = value.getType();
        switch (type)
        {
            case TVConfigValue.TYPE_INT:
                int userValue = 0;
                try
                {
                    userValue = value.getInt();
                }
                catch (TypeException e)
                {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
                // int val = 0;
                int val = TVDeviceImpl.tv.GetSrcInputType().ordinal();
                int status3D = TVDeviceImpl.tv.Get3DMode();
                tvin_info_t sig_fmt = TVDeviceImpl.tv.GetCurrentSignalInfo();

                int fmt = sig_fmt.fmt.ordinal();
                int ON = 1;
                if (name.equals("SetSharpness"))
                {
                    TVDeviceImpl.tv.TvITFExecute(name, userValue, val, ON, status3D);
                }
                else if (name.equals("SetSaturation") || name.equals("SetDisplayMode") || name.equals("SetHue"))
                {
                    TVDeviceImpl.tv.TvITFExecute(name, userValue, val, fmt);
                }
                else if (name.equals("SetAudioSoundMode") || name.equals("SetAudioBalance") || name.equals("SetAudioTrebleVolume")
                        || name.equals("SetAudioBassVolume") || name.equals("SetAudioSupperBassVolume") || name.equals("SetAudioSRSSurround")
                        || name.equals("SetAudioSrsDialogClarity") || name.equals("SetAudioSrsTruBass"))
                {
                    TVDeviceImpl.tv.TvITFExecute(name, userValue);
                }
                else
                {
                    TVDeviceImpl.tv.TvITFExecute(name, userValue, val);
                }

                break;
        }

    }

    @Override
    public TVConfigValue read(String name)
    {
        // TODO Auto-generated method stub
        /*****************tv setting***********************/
        Log.v(TAG, "GET : " + name);
        String configType = "Get";
        if (TVDeviceImpl.tv == null)
            try
            {
                throw new TVException();
            }
            catch (TVException e)
            {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        int pos = name.indexOf(":");
        if (pos == -1)
            Log.e(TAG, "set name fail");
        name = name.substring(pos + 1);
        name = configType + name;
        Log.v(TAG, "GET NEW " + name);
        TVConfigValue myvalue = null;
        int val = TVDeviceImpl.tv.GetSrcInputType().ordinal();
        // int val = 0;
        if (name.equals("GetAudioBalance") || name.equals("GetAudioSoundMode") || name.equals("GetAudioTrebleVolume")
                || name.equals("GetAudioBassVolume") || name.equals("GetAudioSupperBassVolume") || name.equals("GetAudioSRSSurround")
                || name.equals("GetAudioSrsDialogClarity") || name.equals("GetAudioSrsTruBass"))
        {
            myvalue = new TVConfigValue(TVDeviceImpl.tv.TvITFExecute(name));
            return myvalue;
        }
        else
        {
            myvalue = new TVConfigValue(TVDeviceImpl.tv.TvITFExecute(name, val));
            return myvalue;
        }

    }

   
}

class SingletonTv
{
    static Tv instance = null;

    public synchronized static Tv getTvInstance()
    {
        if (instance == null)
        {
            instance = Tv.open();
        }
        return instance;
    }
}
