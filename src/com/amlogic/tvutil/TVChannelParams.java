package com.amlogic.tvutil;

import java.lang.UnsupportedOperationException;

import com.amlogic.tvutil.TVConst.CC_ATV_AUDIO_STANDARD;
import com.amlogic.tvutil.TVConst.CC_ATV_VIDEO_STANDARD;

import android.os.Parcel;
import android.os.Parcelable;
import android.util.Log;

/**
 *频道参数
 */
public class TVChannelParams  implements Parcelable {
    private static String TAG = "TVChannelParams";
	public static final int FE_HAS_SIGNAL   = 0x01;
	public static final int FE_HAS_CARRIER  = 0x02;
	public static final int FE_HAS_VITERBI  = 0x04;
	public static final int FE_HAS_SYNC     = 0x08;
	/**锁定*/
	public static final int FE_HAS_LOCK     = 0x10;
	/**超时*/
	public static final int FE_TIMEDOUT     = 0x20;
	public static final int FE_REINIT       = 0x40;

	/**QPSK模式*/
	public static final int MODE_QPSK = 0;
	/**QAM模式*/
	public static final int MODE_QAM  = 1;
	/**OFDM模式*/
	public static final int MODE_OFDM = 2;
	/**ATSC模式*/
	public static final int MODE_ATSC = 3;
	/**模拟模式*/
	public static final int MODE_ANALOG = 4;

	/**
	 *由字符串获得调制模式
	 *@param str 字符串
	 *@return 返回调制模式
	 */
	public static int getModeFromString(String str){
		if(str.equals("dvbt"))
			return MODE_OFDM;
		else if(str.equals("dvbc"))
			return MODE_QAM;
		else if(str.equals("dvbs"))
			return MODE_QPSK;
		else if(str.equals("atsc"))
			return MODE_ATSC;
		else if(str.equals("analog"))
			return MODE_ANALOG;

		return -1;
	}

	/**8MHz带宽*/
	public static final int BANDWIDTH_8_MHZ = 0;
	/**7MHz带宽*/
	public static final int BANDWIDTH_7_MHZ = 1;
	/**6MHz带宽*/
	public static final int BANDWIDTH_6_MHZ = 2;
	/**自动带宽检测*/
	public static final int BANDWIDTH_AUTO  = 3;
	/**5MHZ带宽*/
	public static final int BANDWIDTH_5_MHZ = 4;
	/**10MHZ带宽*/
	public static final int BANDWIDTH_10_MHZ = 5;

	/**QPSK调制*/
	public static final int MODULATION_QPSK    = 0;
	/**QAM16调制*/
	public static final int MODULATION_QAM_16  = 1;
	/**QAM32调制*/
	public static final int MODULATION_QAM_32  = 2;
	/**QAM64调制*/
	public static final int MODULATION_QAM_64  = 3;
	/**QAM128调制*/
	public static final int MODULATION_QAM_128 = 4;
	/**QAM256调制*/
	public static final int MODULATION_QAM_256 = 5;
	/**QAM调制(自动检测)*/
	public static final int MODULATION_QAM_AUTO= 6;
	/**VSB8调制*/
	public static final int MODULATION_VSB_8   = 7;
	/**VSB16调制*/
	public static final int MODULATION_VSB_16  = 8;
	/**PSK8调制*/
	public static final int MODULATION_PSK_8   = 9;
	/**APSK16调制*/
	public static final int MODULATION_APSK_16 = 10;
	/**APSK32调制*/
	public static final int MODULATION_APSK_32 = 11;
	/**DQPSK调制*/
	public static final int MODULATION_DQPSK   = 12;

	/**单声道*/
	public static final int AUDIO_MONO   = 0x0000;
	/**立体声*/
	public static final int AUDIO_STEREO = 0x0001;
	/**语言2*/
	public static final int AUDIO_LANG2  = 0x0002;
	/**SAP*/
	public static final int AUDIO_SAP    = 0x0002;
	/**语言1*/
	public static final int AUDIO_LANG1  = 0x0003;
	/**语言1/2*/
	public static final int AUDIO_LANG1_LANG2 = 0x0004;

	/**PAL B*/
	public static final int STD_PAL_B     = 0x00000001;
	/**PAL B1*/
	public static final int STD_PAL_B1    = 0x00000002;
	/**PAL G*/
	public static final int STD_PAL_G     = 0x00000004;
	/**PAL H*/
	public static final int STD_PAL_H     = 0x00000008;
	/**PAL I*/
	public static final int STD_PAL_I     = 0x00000010;
	/**PAL D*/
	public static final int STD_PAL_D     = 0x00000020;
	/**PAL D1*/
	public static final int STD_PAL_D1    = 0x00000040;
	/**PAL K*/
	public static final int STD_PAL_K     = 0x00000080;
	/**PAL M*/
	public static final int STD_PAL_M     = 0x00000100;
	/**PAL N*/
	public static final int STD_PAL_N     = 0x00000200;
	/**PAL Nc*/
	public static final int STD_PAL_Nc    = 0x00000400;
	/**PAL 60*/
	public static final int STD_PAL_60    = 0x00000800;
	/**NTSC M*/
	public static final int STD_NTSC_M    = 0x00001000;
	/**NTSC M JP*/
	public static final int STD_NTSC_M_JP = 0x00002000;
	/**NTSC 443*/
	public static final int STD_NTSC_443  = 0x00004000;
	/**NTSC M KR*/
	public static final int STD_NTSC_M_KR = 0x00008000;
	/**SECAM B*/
	public static final int STD_SECAM_B   = 0x00010000;
	/**SECAM D*/
	public static final int STD_SECAM_D   = 0x00020000;
	/**SECAM G*/
	public static final int STD_SECAM_G   = 0x00040000;
	/**SECAM H*/
	public static final int STD_SECAM_H   = 0x00080000;
	/**SECAM K*/
	public static final int STD_SECAM_K   = 0x00100000;
	/**SECAM K1*/
	public static final int STD_SECAM_K1  = 0x00200000;
	/**SECAM L*/
	public static final int STD_SECAM_L   = 0x00400000;
	/**SECAM LC*/
	public static final int STD_SECAM_LC  = 0x00800000;
	/**ATSC VSB8*/
	public static final int STD_ATSC_8_VSB  = 0x01000000;
	/**ATSC VSB16*/
	public static final int STD_ATSC_16_VSB = 0x02000000;
	/**NTSC*/
	public static final int STD_NTSC      = STD_NTSC_M|STD_NTSC_M_JP|STD_NTSC_M_KR;
	/**SECAM DK*/
	public static final int STD_SECAM_DK  = STD_SECAM_D|STD_SECAM_K|STD_SECAM_K1;
	/**SECAM*/
	public static final int STD_SECAM     = STD_SECAM_B|STD_SECAM_G|STD_SECAM_H|STD_SECAM_DK|STD_SECAM_L|STD_SECAM_LC;
	/**PAL BG*/
	public static final int STD_PAL_BG    = STD_PAL_B|STD_PAL_B1|STD_PAL_G;
	/**PAL DK*/
	public static final int STD_PAL_DK    = STD_PAL_D|STD_PAL_D1|STD_PAL_K;
	/**PAL*/
	public static final int STD_PAL       = STD_PAL_BG|STD_PAL_DK|STD_PAL_H|STD_PAL_I;
	
	public static final int TUNER_STD_MN  =STD_PAL_M|STD_PAL_N|STD_PAL_Nc| STD_NTSC;
    public static final int STD_B         = STD_PAL_B    |STD_PAL_B1   |STD_SECAM_B;
    public static final int STD_GH        = STD_PAL_G    | STD_PAL_H   |STD_SECAM_G  |STD_SECAM_H;
    public static final int STD_DK        = STD_PAL_DK   | STD_SECAM_DK;
    public static final int STD_M         = STD_PAL_M    | STD_NTSC_M;
    public static final int STD_BG        = STD_PAL_BG   | STD_SECAM_B | STD_SECAM_G ;

	public static final int COLOR_PAL   =0x04000000;
    public static final int COLOR_NTSC  =0x08000000;
    public static final int COLOR_SECAM =0x10000000;
    
	public int mode;
	public int frequency;
	public int symbolRate;
	public int modulation;
	public int bandwidth;
	public int audio;
	public int standard;

	public static final Parcelable.Creator<TVChannelParams> CREATOR = new Parcelable.Creator<TVChannelParams>(){
		public TVChannelParams createFromParcel(Parcel in) {
			return new TVChannelParams(in);
		}
		public TVChannelParams[] newArray(int size) {
			return new TVChannelParams[size];
		}
	};

	public void readFromParcel(Parcel in){
		mode      = in.readInt();
		frequency = in.readInt();
		if((mode == MODE_QAM) || (mode == MODE_QPSK))
			symbolRate = in.readInt();
		if(mode == MODE_QAM)
			modulation = in.readInt();
		if(mode == MODE_OFDM)
			bandwidth = in.readInt();
		if(mode == MODE_ANALOG){
			audio = in.readInt();
			standard = in.readInt();
		}
	}

	public void writeToParcel(Parcel dest, int flags){
		dest.writeInt(mode);
		dest.writeInt(frequency);
		if((mode == MODE_QAM) || (mode == MODE_QPSK))
			dest.writeInt(symbolRate);
		if(mode == MODE_QAM)
			dest.writeInt(modulation);
		if(mode == MODE_OFDM)
			dest.writeInt(bandwidth);
		if(mode == MODE_ANALOG){
			dest.writeInt(audio);
			dest.writeInt(standard);
		}
	}

	public TVChannelParams(Parcel in){
		readFromParcel(in);
	}


	TVChannelParams(int mode){
		this.mode = mode;
	}

	/**
	 *创建DVBC参数
	 *@param frequency 频率Hz为单位
	 *@param modulation 调制方式
	 *@param symbolRate 符号率
	 *@return 返回新创建的参数
	 */
	public static TVChannelParams dvbcParams(int frequency, int modulation, int symbolRate){
		TVChannelParams tp = new TVChannelParams(MODE_QAM);

		tp.frequency  = frequency;
		tp.modulation = modulation;
		tp.symbolRate = symbolRate;

		return tp;
	}

	/**
	 *创建DVBT参数
	 *@param frequency 频率Hz为单位
	 *@param bandwidth 带宽
	 *@return 返回新创建的参数
	 */
	public static TVChannelParams dvbtParams(int frequency, int bandwidth){
		TVChannelParams tp = new TVChannelParams(MODE_OFDM);

		tp.frequency = frequency;
		tp.bandwidth = bandwidth;

		return tp;
	}

	/**
	 *创建DVBS参数
	 *@param frequency 频率Hz为单位
	 *@param symbolRate 符号率
	 *@return 返回新创建的参数
	 */
	public static TVChannelParams dvbsParams(int frequency, int symbolRate){
		TVChannelParams tp = new TVChannelParams(MODE_QPSK);

		tp.frequency  = frequency;
		tp.symbolRate = symbolRate;

		return tp;
	}

	/**
	 *创建模拟参数
	 *@param frequency 频率Hz为单位
	 *@param std 视频标准
	 *@param audio 伴音选择
	 *@return 返回新创建的参数
	 */
	public static TVChannelParams analogParams(int frequency, int std, int audio){
		TVChannelParams tp = new TVChannelParams(MODE_ANALOG);

		tp.frequency = frequency;
		tp.audio     = audio;
		tp.standard  = std;

		return tp;
	}

	
	public static CC_ATV_AUDIO_STANDARD AudioStb2Enum(int data){
	    if((data & STD_DK) == STD_DK){
	        return CC_ATV_AUDIO_STANDARD.CC_ATV_AUDIO_STD_DK;
	    }else
        if((data & STD_PAL_I) == STD_PAL_I){
            return CC_ATV_AUDIO_STANDARD.CC_ATV_AUDIO_STD_I;
        }else
        if((data & STD_BG) == STD_BG){
            return CC_ATV_AUDIO_STANDARD.CC_ATV_AUDIO_STD_BG;
        }
    	else
        if((data & STD_M) == STD_M){
            return CC_ATV_AUDIO_STANDARD.CC_ATV_AUDIO_STD_M;
        }
	    return null;
	    
	}
	
	   
	public static CC_ATV_VIDEO_STANDARD VideoStb2Enum(int data){
        if((data & COLOR_PAL) == COLOR_PAL){
            return CC_ATV_VIDEO_STANDARD.CC_ATV_VIDEO_STD_PAL;
        }else
        if((data & COLOR_NTSC) == COLOR_NTSC){
            return CC_ATV_VIDEO_STANDARD.CC_ATV_VIDEO_STD_NTSC;
        }else
        if((data & COLOR_SECAM) == COLOR_SECAM){
            return CC_ATV_VIDEO_STANDARD.CC_ATV_VIDEO_STD_SECAM;
        }
      
        return null;
        
    }
	
	public static int ChangeVideoStd(int data){
	    int videostd = 0;
        if(data == CC_ATV_VIDEO_STANDARD.CC_ATV_VIDEO_STD_AUTO.ordinal()){
            Log.e(TAG,"video stb auto is not function");
            videostd = 0;
        }else
        if(data == CC_ATV_VIDEO_STANDARD.CC_ATV_VIDEO_STD_PAL.ordinal())
            videostd = COLOR_PAL;
        else
        if(data == CC_ATV_VIDEO_STANDARD.CC_ATV_VIDEO_STD_NTSC.ordinal())
            videostd = COLOR_NTSC;
        else
        if(data == CC_ATV_VIDEO_STANDARD.CC_ATV_VIDEO_STD_SECAM.ordinal())
            videostd = COLOR_SECAM;
        return videostd;
    }
	
	public static int Change2AudioStd(int video_std,int audio_std) {
	    int tmpTunerStd = 0;;
	   
        if (audio_std >= 0) {
            if(audio_std < CC_ATV_AUDIO_STANDARD.CC_ATV_AUDIO_STD_START.ordinal()
               || audio_std >CC_ATV_AUDIO_STANDARD. CC_ATV_AUDIO_STD_END.ordinal() ){
                //audio_std = ATVHandleAudioStdCfg();
            }
        }
        if (video_std == CC_ATV_VIDEO_STANDARD.CC_ATV_VIDEO_STD_AUTO.ordinal()) {
            //tmpTunerStd |= CC_ATV_ADUIO_STANDARD.TUNER_COLOR_PAL;
            if (audio_std ==CC_ATV_AUDIO_STANDARD.CC_ATV_AUDIO_STD_DK.ordinal()) {
                tmpTunerStd |= STD_PAL_DK;
            } else if (audio_std == CC_ATV_AUDIO_STANDARD.CC_ATV_AUDIO_STD_I.ordinal()) {
                tmpTunerStd |= STD_PAL_I;
            } else if (audio_std == CC_ATV_AUDIO_STANDARD.CC_ATV_AUDIO_STD_BG.ordinal()) {
                tmpTunerStd |= STD_PAL_BG;
            } else if (audio_std == CC_ATV_AUDIO_STANDARD.CC_ATV_AUDIO_STD_M.ordinal()) {
                tmpTunerStd |= STD_NTSC_M;
            }
        } else if (video_std == CC_ATV_VIDEO_STANDARD.CC_ATV_VIDEO_STD_PAL.ordinal()) {
           // tmpTunerStd |= CC_ATV_ADUIO_STANDARDCOLOR_PAL;
            if (audio_std == CC_ATV_AUDIO_STANDARD.CC_ATV_AUDIO_STD_DK.ordinal()) {
                tmpTunerStd |= STD_PAL_DK;
            } else if (audio_std == CC_ATV_AUDIO_STANDARD.CC_ATV_AUDIO_STD_I.ordinal()) {
                tmpTunerStd |= STD_PAL_I;
            } else if (audio_std == CC_ATV_AUDIO_STANDARD.CC_ATV_AUDIO_STD_BG.ordinal()) {
                tmpTunerStd |= STD_PAL_BG;
            } else if (audio_std == CC_ATV_AUDIO_STANDARD.CC_ATV_AUDIO_STD_M.ordinal()) {
                tmpTunerStd |= STD_PAL_M;
            }
        } else if (video_std == CC_ATV_VIDEO_STANDARD.CC_ATV_VIDEO_STD_NTSC.ordinal()) {
           // tmpTunerStd |= CC_ATV_ADUIO_STANDARD.COLOR_NTSC;

            if (audio_std == CC_ATV_AUDIO_STANDARD.CC_ATV_AUDIO_STD_DK.ordinal()) {
                tmpTunerStd |= STD_PAL_DK;
            } else if (audio_std == CC_ATV_AUDIO_STANDARD.CC_ATV_AUDIO_STD_I.ordinal()) {
                tmpTunerStd |= STD_PAL_I;
            } else if (audio_std == CC_ATV_AUDIO_STANDARD.CC_ATV_AUDIO_STD_BG.ordinal()) {
                tmpTunerStd |= STD_PAL_BG;
            } else if (audio_std == CC_ATV_AUDIO_STANDARD.CC_ATV_AUDIO_STD_M.ordinal()) {
                tmpTunerStd |= STD_NTSC_M;
            }
        } else if (video_std == CC_ATV_VIDEO_STANDARD.CC_ATV_VIDEO_STD_SECAM.ordinal()) {
           // tmpTunerStd |= TUNER_COLOR_SECAM;
            if (audio_std == CC_ATV_AUDIO_STANDARD.CC_ATV_AUDIO_STD_DK.ordinal()) {
                tmpTunerStd |= STD_SECAM_DK;
            } else if (audio_std == CC_ATV_AUDIO_STANDARD.CC_ATV_AUDIO_STD_I.ordinal()) {
                tmpTunerStd |= STD_PAL_I;
            } else if (audio_std == CC_ATV_AUDIO_STANDARD.CC_ATV_AUDIO_STD_BG.ordinal()) {
                tmpTunerStd |= (STD_SECAM_B | STD_SECAM_G);
            } else if (audio_std == CC_ATV_AUDIO_STANDARD.CC_ATV_AUDIO_STD_M.ordinal()) {
                tmpTunerStd |= STD_NTSC_M;
            } else if (audio_std == CC_ATV_AUDIO_STANDARD.CC_ATV_AUDIO_STD_L.ordinal()) {
                tmpTunerStd |= STD_SECAM_L;
            }
        }
        
	    return tmpTunerStd;
	}
	
	static CC_ATV_AUDIO_STANDARD ATVHandleAudioStdCfg(String key) {
	    if (key.equals( "DK") ) {
	        return CC_ATV_AUDIO_STANDARD.CC_ATV_AUDIO_STD_DK;
	    } else if (key.equals( "I")) {
	        return CC_ATV_AUDIO_STANDARD.CC_ATV_AUDIO_STD_I;
	    } else if (key.equals( "BG") ) {
	        return CC_ATV_AUDIO_STANDARD.CC_ATV_AUDIO_STD_BG;
	    } else if (key.equals( "M") ) {
	        return CC_ATV_AUDIO_STANDARD.CC_ATV_AUDIO_STD_M;
	    } else if ( key.equals("L") ) {
	        return CC_ATV_AUDIO_STANDARD.CC_ATV_AUDIO_STD_L;
	    } else if (key.equals( "AUTO") ) {
	        return CC_ATV_AUDIO_STANDARD.CC_ATV_AUDIO_STD_AUTO;
	    }

	    return null;
	}

	/**
	 *修改模拟音频
	 *@return true 表示已经修改,false表示制式已经设置无需修改
	 */
	public boolean setATVAudio(int audio){
		if(this.audio == audio)
			return false;

		this.audio = audio;
		return true;
	}

	/**
	 *修改模拟视频制式
	 *@param fmt 视频制式
	 *@return true 表示已经修改制式,false表示制式已经设置无需修改
	 */
	public boolean setATVVideoFormat(TVConst.CC_ATV_VIDEO_STANDARD fmt){
		int std, afmt;

		afmt = AudioStb2Enum(standard).ordinal();
		std  = ChangeVideoStd(fmt.ordinal()) | Change2AudioStd(fmt.ordinal(), afmt);

		if(std == standard)
			return false;

		standard = std;
		return true;
	}

	/**
	 *修改模拟音频制式
	 *@param fmt 音频制式
	 *@return true 表示已经修改制式,false表示制式已经设置无需修改
	 */
	public boolean setATVAudioFormat(TVConst.CC_ATV_AUDIO_STANDARD fmt){
		int std, vfmt;

		vfmt = VideoStb2Enum(standard).ordinal();
		std  = ChangeVideoStd(vfmt) | Change2AudioStd(vfmt, fmt.ordinal());

		if(std == standard)
			return false;

		standard = std;
		return true;
	}
	
	/**
	 *取得参数模式
	 *@return 返回模式
	 */
	public int getMode(){
		return mode;
	}

	/**
	 *判断参数是否为DVB模式
	 *@return true表示是DVB模式，false表示不是DVB模式
	 */
	public boolean isDVBMode(){
		if((mode==MODE_QPSK) || (mode==MODE_QAM) || (mode==MODE_OFDM)){
			return true;
		}

		return false;
	}

	/**
	 *判断是参数否为DVBC模式
	 *@return true表示是DVBC模式，false表示不是DVBC模式
	 */
	public boolean isDVBCMode(){
		if(mode==MODE_QAM){
			return true;
		}

		return false;
	}

	/**
	 *判断是参数否为DVBT模式
	 *@return true表示是DVBT模式，false表示不是DVBT模式
	 */
	public boolean isDVBTMode(){
		if(mode==MODE_OFDM){
			return true;
		}

		return false;
	}

	/**
	 *判断是参数否为DVBS模式
	 *@return true表示是DVBS模式，false表示不是DVBS模式
	 */
	public boolean isDVBSMode(){
		if(mode==MODE_QPSK){
			return true;
		}

		return false;
	}

	/**
	 *判断参数是否为ATSC模式
	 *@return true表示是ATSC模式，false表示不是ATSC模式
	 */
	public boolean isATSCMode(){
		if(mode==MODE_ATSC){
			return true;
		}

		return false;
	}

	/**
	 *判断参数是否为模拟模式
	 *@return true表示是模拟模式，false表示不是模拟模式
	 */
	public boolean isAnalogMode(){
		if(mode==MODE_ANALOG){
			return true;
		}

		return false;
	}

	/**
	 *取得频率(单位Hz)
	 @return 返回频率
	 */
	public int getFrequency(){
		return frequency;
	}

	/**
	 *取得伴音模式(模拟模式)
	 *@return 返回伴音模式
	 */
	public int getAudioMode(){
		if(!isAnalogMode())
			throw new UnsupportedOperationException();

		return audio;
	}

	/**
	 *取得视频标准(模拟模式)
	 *@return 返回视频标准
	 */
	public int getStandard(){
		if(!isAnalogMode())
			throw new UnsupportedOperationException();

		return standard;
	}

	/**
	 *取得带宽(OFDM模式)
	 *@return 返回带宽
	 */
	public int getBandwidth(){
		if(mode != MODE_OFDM)
			throw new UnsupportedOperationException();

		return bandwidth;
	}

	/**
	 *取得调制方式(QAM模式)
	 *@return 返回调试方式
	 */
	public int getModulation(){
		if(mode != MODE_QAM)
			throw new UnsupportedOperationException();

		return modulation;
	}

	/**
	 *取得符号率(QPSK/QAM模式)
	 *@return 返回符号率
	 */
	public int getSymbolRate(){
		if((mode != MODE_QPSK) || (mode != MODE_QAM))
			throw new UnsupportedOperationException();

		return symbolRate;
	}

	/**
	 *检测前端参数和当前参数是否相等
	 *@param params 前端参数
	 *@return 如果相等返回true，不等返回false
	 */
	public boolean equals(TVChannelParams params){
		if(this.mode != params.mode)
			return false;

		if(this.frequency != params.frequency)
			return false;

		return true;
	}

	public int describeContents(){
		return 0;
	}

	public static Parcelable.Creator<TVChannelParams> getCreator() {
		return CREATOR;
	}
}

