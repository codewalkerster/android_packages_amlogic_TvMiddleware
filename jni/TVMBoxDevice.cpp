#include <am_av.h>
#include <am_dmx.h>
#include <am_fend_ctrl.h>
#include <am_misc.h>
#include <am_rec.h>
#include <jni.h>
#include <android/log.h>
#include <stdio.h>
#include <fcntl.h>
#include <cutils/properties.h>

extern "C" {

#ifdef LOG_TAG
#undef LOG_TAG
#endif

#define LOG_TAG    "jnitvmboxdevice"
#define LOGI(...)  __android_log_print(ANDROID_LOG_INFO,LOG_TAG,__VA_ARGS__)
#define LOGE(...)  __android_log_print(ANDROID_LOG_ERROR,LOG_TAG,__VA_ARGS__)

typedef struct {
	AM_Bool_t dev_open;
	AM_Bool_t in_timeshifting;
	int       fend_mode;
	int       rec_handle;
	jobject   dev_obj;
	AM_DMX_Source_t ts_src;
	AM_AV_TimeshiftPara_t timeshift_para;
} TVDevice;

#define FEND_DEV_NO    0
#define FEND_DEF_MODE  -1
#define AV_DEV_NO      0
#define DMX_DEV_NO     0
#define DVR_DEV_NO     2
#define ASYNC_FIFO_NO  0
#define PLAYBACK_DMX_DEV_NO 1

#define EVENT_SET_INPUT_SOURCE_OK     0
#define EVENT_SET_INPUT_SOURCE_FAILED 1
#define EVENT_VCHIP_BLOCKED           2
#define EVENT_VCHIP_UNBLOCKED         3
#define EVENT_FRONTEND                4
#define EVENT_DTV_NO_DATA             5
#define EVENT_DTV_CANNOT_DESCRAMLE    6
#define EVENT_RECORD_END              7
#define EVENT_PLAYBACK_START          10
#define EVENT_PLAYBACK_END            11
#define EVENT_DTV_DATA_RESUME         12


#define SOURCE_DTV	10

static JavaVM   *gJavaVM = NULL;
static jclass    gEventClass;
static jclass    gChanParamsClass;
static jclass    gRecParamsClass;
static jclass    gPlaybackParamsClass; 
static jclass    gVideoClass;
static jclass    gAudioClass;
static jclass    gSubtitleClass;
static jclass    gTeletextClass;
static jclass    gProgramClass;
static jmethodID gEventInitID;
static jmethodID gChanParamsInitID;
static jmethodID gOnEventID;
static jmethodID gRecParamsInitID;
static jmethodID gPlaybackParamsInitID;
static jmethodID gVideoInitID;
static jmethodID gAudioInitID;
static jmethodID gSubtitleInitID;
static jmethodID gTeletextInitID;
static jmethodID gProgramInitID;
static jfieldID  gHandleID;
static jfieldID  gChanParamsModeID;
static jfieldID  gChanParamsFreqID;
static jfieldID  gChanParamsModID;
static jfieldID  gChanParamsSymID;
static jfieldID  gChanParamsBWID;
static jfieldID  gChanParamsSatPolarID;
static jfieldID  gChanParamsSatParaID;
static jfieldID  gEventFEParamsID;
static jfieldID  gEventFEStatusID;
static jfieldID  gEventSourceID;
static jfieldID  gEventRecParamsID;
static jfieldID  gEventRecEndCodeID;
static jfieldID  gRecParamsFileID;
static jfieldID  gRecParamsStorageID;
static jfieldID  gRecParamsVideoID;
static jfieldID  gRecParamsAudioID;
static jfieldID  gRecParamsSubtitleID;
static jfieldID  gRecParamsTeletextID;
static jfieldID  gRecParamsRecSizeID;
static jfieldID  gRecParamsRecTimeID;
static jfieldID  gRecParamsTotalTimeID;
static jfieldID  gRecParamsTimeshiftID;
static jfieldID  gRecParamsPrefixNameID;
static jfieldID  gRecParamsSuffixNameID;
static jfieldID  gRecParamsPmtPidID;
static jfieldID  gRecParamsPmtProgramNumberID;
static jfieldID  gRecParamsProgramNameID;
static jfieldID  gPlaybackParamsFileID;
static jfieldID  gPlaybackParamsStatusID;
static jfieldID  gPlaybackParamsCurrtimeID;
static jfieldID  gPlaybackParamsTotaltimeID;
static jfieldID  gVideoPidID;
static jfieldID  gVideoFmtID;
static jfieldID  gAudioPidID;
static jfieldID  gAudioFmtID;
static jfieldID  gAudioLangID;
static jfieldID  gSubtitlePidID;
static jfieldID  gSubtitleTypeID;
static jfieldID  gSubtitleCompPageID;
static jfieldID  gSubtitleAnciPageID;
static jfieldID  gSubtitleMagNoID;
static jfieldID  gSubtitlePageNoID;
static jfieldID  gSubtitleLangID;
static jfieldID  gTeletextPidID;
static jfieldID  gTeletextMagNoID;
static jfieldID  gTeletextPageNoID;
static jfieldID  gTeletextLangID;

	
static TVDevice* get_dev(JNIEnv *env, jobject obj)
{
	TVDevice *dev;

	dev = (TVDevice*)env->GetIntField(obj, gHandleID);

	return dev;
}

static jobject create_event(JNIEnv *env, jobject dev, jint type)
{
	jobject obj = env->NewObject(gEventClass, gEventInitID, dev, type);

	return obj;
}

static void on_event(jobject obj, jobject event)
{
	JNIEnv *env;
	int ret;
	int attached = 0;
	
	ret = gJavaVM->GetEnv((void**) &env, JNI_VERSION_1_4);
	if(ret<0){
		ret = gJavaVM->AttachCurrentThread(&env, NULL);
		if(ret<0){
			LOGE("Can't attach thread");
			return;
		}
		attached = 1;
	}

	env->CallVoidMethod(obj, gOnEventID, event);

	if(attached){
		gJavaVM->DetachCurrentThread();
	}
}

static void chan_to_fpara(JNIEnv *env, jobject chan, AM_FENDCTRL_DVBFrontendParameters_t *para)
{
	jint mode, freq, mod, sym, bw, polar;

	memset(para, 0, sizeof(AM_FENDCTRL_DVBFrontendParameters_t));

	mode = env->GetIntField(chan, gChanParamsModeID);
	para->m_type = mode;

	freq = env->GetIntField(chan, gChanParamsFreqID);

	switch(mode){
		case FE_OFDM:
			bw = env->GetIntField(chan, gChanParamsBWID);
			para->cable.para.frequency = freq;
			para->cable.para.u.ofdm.bandwidth = (fe_bandwidth_t)bw;
			break;
		case FE_QAM:
			mod = env->GetIntField(chan, gChanParamsModID);
			sym = env->GetIntField(chan, gChanParamsSymID);
			para->terrestrial.para.frequency = freq;
			para->terrestrial.para.u.qam.modulation  = (fe_modulation_t)mod;
			para->terrestrial.para.u.qam.symbol_rate = sym;
			break;
		case FE_ATSC:
			para->atsc.para.frequency = freq;
			break;	
		case FE_QPSK:
			sym = env->GetIntField(chan, gChanParamsSymID);
			polar = env->GetIntField(chan, gChanParamsSatPolarID);
			para->sat.para.frequency = freq;
			para->sat.para.u.qpsk.symbol_rate = sym;
			para->sat.polarisation = (AM_FEND_Polarisation_t)polar;
			break;
		case FE_DTMB:
			bw = env->GetIntField(chan, gChanParamsBWID);
			para->dtmb.para.frequency = freq;
			para->dtmb.para.u.ofdm.bandwidth = (fe_bandwidth_t)bw;
			break;	
	}
}

static jobject fpara_to_chan(JNIEnv *env, int mode, struct dvb_frontend_parameters *para)
{
	jobject obj = env->NewObject(gChanParamsClass, gChanParamsInitID, mode);

	env->SetIntField(obj, gChanParamsModeID, mode);
	env->SetIntField(obj, gChanParamsFreqID, para->frequency);

	switch(mode){
		case FE_OFDM:
			env->SetIntField(obj, gChanParamsBWID, para->u.ofdm.bandwidth);
			break;
		case FE_QAM:
			env->SetIntField(obj, gChanParamsSymID, para->u.qam.symbol_rate);
			env->SetIntField(obj, gChanParamsModID, para->u.qam.modulation);
			break;
		case FE_QPSK:
			env->SetIntField(obj, gChanParamsSymID, para->u.qpsk.symbol_rate);
			break;	
		case FE_DTMB:
			env->SetIntField(obj, gChanParamsBWID, para->u.ofdm.bandwidth);
			break;
	}

	return obj;
}

static int get_sat_para(JNIEnv *env, jobject thiz, jobject para, AM_SEC_DVBSatelliteEquipmentControl_t *sat_para)
{
    jfieldID lof_hi,lof_lo,lof_threshold,signal_22khz,voltage_mode;
    jfieldID pos_num, lo, la, diseqc_mode, toneburst;
    jfieldID cdc, ucdc, repeats, cmd_order, fast_diseqc, seq_repeat, sat_lo;
    jfieldID ub, ub_freq;

    jclass objclass = env->FindClass("com/amlogic/tvutil/TVSatelliteParams");

    lof_hi = env->GetFieldID(objclass, "lnb_lof_hi", "I");
    lof_lo = env->GetFieldID(objclass, "lnb_lof_lo", "I");
    lof_threshold = env->GetFieldID(objclass, "lnb_lof_threadhold", "I");
    signal_22khz = env->GetFieldID(objclass, "sec_22k_status", "I");
    voltage_mode = env->GetFieldID(objclass, "sec_voltage_status", "I");
    pos_num = env->GetFieldID(objclass, "motor_position_num", "I");
    lo = env->GetFieldID(objclass, "local_longitude", "D");
    la = env->GetFieldID(objclass, "local_latitude", "D");
    diseqc_mode = env->GetFieldID(objclass, "diseqc_mode", "I");
    toneburst = env->GetFieldID(objclass, "sec_tone_burst", "I");
    cdc = env->GetFieldID(objclass, "diseqc_committed", "I");
    ucdc = env->GetFieldID(objclass, "diseqc_uncommitted", "I");
    repeats = env->GetFieldID(objclass, "diseqc_repeat_count", "I");
    cmd_order = env->GetFieldID(objclass, "diseqc_order", "I");
    fast_diseqc = env->GetFieldID(objclass, "diseqc_fast", "I");
    seq_repeat = env->GetFieldID(objclass, "diseqc_sequence_repeat", "I");
    sat_lo = env->GetFieldID(objclass, "sat_longitude", "D");
    ub = env->GetFieldID(objclass, "user_band", "I");
    ub_freq = env->GetFieldID(objclass, "ub_freq", "I");	    
    
    sat_para->m_lnbs.m_lof_hi = env->GetIntField(para, lof_hi);
    sat_para->m_lnbs.m_lof_lo = env->GetIntField(para, lof_lo);
    sat_para->m_lnbs.m_lof_threshold = env->GetIntField(para, lof_threshold);
    sat_para->m_lnbs.m_cursat_parameters.m_22khz_signal = (AM_SEC_22khz_Signal)(env->GetIntField(para, signal_22khz));
    sat_para->m_lnbs.m_cursat_parameters.m_voltage_mode = (AM_SEC_Voltage_Mode)(env->GetIntField(para, voltage_mode));
    sat_para->m_lnbs.m_cursat_parameters.m_rotorPosNum = env->GetIntField(para, pos_num);
    sat_para->m_lnbs.m_rotor_parameters.m_gotoxx_parameters.m_longitude = env->GetDoubleField(para, lo);
    sat_para->m_lnbs.m_rotor_parameters.m_gotoxx_parameters.m_latitude = env->GetDoubleField(para, la);
    sat_para->m_lnbs.m_diseqc_parameters.m_diseqc_mode = (AM_SEC_Diseqc_Mode)(env->GetIntField(para, diseqc_mode));
    sat_para->m_lnbs.m_diseqc_parameters.m_toneburst_param = (AM_SEC_Toneburst_Param)(env->GetIntField(para, toneburst));
    sat_para->m_lnbs.m_diseqc_parameters.m_committed_cmd = (unsigned char)env->GetIntField(para, cdc);
    sat_para->m_lnbs.m_diseqc_parameters.m_uncommitted_cmd = (unsigned char)env->GetIntField(para, ucdc);
    sat_para->m_lnbs.m_diseqc_parameters.m_repeats = env->GetIntField(para, repeats);
    sat_para->m_lnbs.m_diseqc_parameters.m_command_order = env->GetIntField(para, cmd_order);
    sat_para->m_lnbs.m_diseqc_parameters.m_use_fast = env->GetIntField(para, fast_diseqc);
    sat_para->m_lnbs.m_diseqc_parameters.m_seq_repeat = env->GetIntField(para, seq_repeat);
    sat_para->m_lnbs.m_rotor_parameters.m_gotoxx_parameters.m_sat_longitude = env->GetDoubleField(para, sat_lo);    
    sat_para->m_lnbs.LNBNum = sat_para->m_lnbs.m_diseqc_parameters.m_toneburst_param==B ? 2 : 1;
    sat_para->m_lnbs.SatCR_idx = env->GetIntField(para, ub);
    sat_para->m_lnbs.SatCRvco = env->GetIntField(para, ub_freq);	         

    return 0;
}

static jobject recendpara_to_para(JNIEnv *env, jobject object, AM_REC_RecEndPara_t *para)
{
	jobject obj = env->NewObject(gRecParamsClass, gRecParamsInitID, object);

	env->SetLongField(obj, gRecParamsRecSizeID, para->total_size);
	env->SetLongField(obj, gRecParamsTotalTimeID, (jlong)para->total_time*1000);

	return obj;
}

static void rec_to_createpara(JNIEnv *env, jobject rec, AM_REC_CreatePara_t *para)
{
	memset(para, 0, sizeof(AM_REC_CreatePara_t));
	jstring storage = (jstring)env->GetObjectField(rec, gRecParamsStorageID);
	const char *strpath = env->GetStringUTFChars(storage, 0);
	if (strpath != NULL){
		strncpy(para->store_dir, strpath, sizeof(para->store_dir));
		env->ReleaseStringUTFChars(storage, strpath);
	}
	
	para->fend_dev      = FEND_DEV_NO;
	para->dvr_dev        = DVR_DEV_NO;
	para->async_fifo_id = ASYNC_FIFO_NO;
}

static void rec_to_media_info(JNIEnv *env, jobject rec, AM_REC_MediaInfo_t *para)
{
	int i;
	jstring strlang;
	const char *lang;
	
	memset(para, 0, sizeof(AM_REC_MediaInfo_t));

	strlang = (jstring)env->GetObjectField(rec, gRecParamsProgramNameID);
	lang = env->GetStringUTFChars(strlang, 0);
	if (lang != NULL){
		snprintf(para->program_name, sizeof(para->program_name), "%s", lang);
		env->ReleaseStringUTFChars(strlang, lang);
	}
	
	jobject video = env->GetObjectField(rec, gRecParamsVideoID);
	if (video != NULL){
		para->vid_pid = env->GetIntField(video, gVideoPidID);
		para->vid_fmt= env->GetIntField(video, gVideoFmtID);
	}else{
		para->vid_pid = 0x1fff;
		para->vid_fmt = -1;
	}

	jobject audio;
	jobjectArray audios = (jobjectArray)env->GetObjectField(rec, gRecParamsAudioID);
	if (audios != NULL){
		para->aud_cnt = env->GetArrayLength(audios);
		if (para->aud_cnt > (int)AM_ARRAY_SIZE(para->audios)){
			para->aud_cnt = AM_ARRAY_SIZE(para->audios);
		}

		for (i=0; i<para->aud_cnt; i++){
			audio = env->GetObjectArrayElement(audios, i);
			if (audio != NULL){
				para->audios[i].pid = env->GetIntField(audio, gAudioPidID);
				para->audios[i].fmt = env->GetIntField(audio, gAudioFmtID);
				strlang = (jstring)env->GetObjectField(audio, gAudioLangID);
				const char *lang = env->GetStringUTFChars(strlang, 0);
				if (lang != NULL){
					strncpy(para->audios[i].lang, lang, 3);
					env->ReleaseStringUTFChars(strlang, lang);
				}
			}else{
				para->audios[i].pid = 0x1fff;
				para->audios[i].fmt = -1;
			}
		}
	}

	jobject subtitle;
	jobjectArray subtitles = (jobjectArray)env->GetObjectField(rec, gRecParamsSubtitleID);
	if (subtitles != NULL){
		para->sub_cnt= env->GetArrayLength(subtitles);
		if (para->sub_cnt > (int)AM_ARRAY_SIZE(para->subtitles)){
			para->sub_cnt = AM_ARRAY_SIZE(para->subtitles);
		}

		for (i=0; i<para->sub_cnt; i++){
			subtitle = env->GetObjectArrayElement(subtitles, i);
			if (subtitle != NULL){
				para->subtitles[i].pid  = env->GetIntField(subtitle, gSubtitlePidID);
				para->subtitles[i].type = env->GetIntField(subtitle, gSubtitleTypeID);
				para->subtitles[i].composition_page = env->GetIntField(subtitle, gSubtitleCompPageID);
				para->subtitles[i].ancillary_page   = env->GetIntField(subtitle, gSubtitleAnciPageID);
				para->subtitles[i].magzine_no = env->GetIntField(subtitle, gSubtitleMagNoID);
				para->subtitles[i].page_no = env->GetIntField(subtitle, gSubtitlePageNoID);
				strlang = (jstring)env->GetObjectField(subtitle, gSubtitleLangID);
				const char *lang = env->GetStringUTFChars(strlang, 0);
				if (lang != NULL){
					strncpy(para->subtitles[i].lang, lang, 3);
					env->ReleaseStringUTFChars(strlang, lang);
				}
			}else{
				para->subtitles[i].pid = 0x1fff;
			}
		}
	}

	jobject teletext;
	jobjectArray teletexts = (jobjectArray)env->GetObjectField(rec, gRecParamsTeletextID);
	if (teletexts != NULL){
		para->ttx_cnt= env->GetArrayLength(teletexts);
		if (para->ttx_cnt > (int)AM_ARRAY_SIZE(para->teletexts)){
			para->ttx_cnt = AM_ARRAY_SIZE(para->teletexts);
		}

		for (i=0; i<para->ttx_cnt; i++){
			teletext = env->GetObjectArrayElement(teletexts, i);
			if (teletext != NULL){
				para->teletexts[i].pid  = env->GetIntField(teletext, gTeletextPidID);
				para->teletexts[i].magzine_no = env->GetIntField(teletext, gTeletextMagNoID);
				para->teletexts[i].page_no = env->GetIntField(teletext, gTeletextPageNoID);
				strlang = (jstring)env->GetObjectField(teletext, gTeletextLangID);
				const char *lang = env->GetStringUTFChars(strlang, 0);
				if (lang != NULL){
					strncpy(para->teletexts[i].lang, lang, 3);
					env->ReleaseStringUTFChars(strlang, lang);
				}
			}else{
				para->teletexts[i].pid = 0x1fff;
			}
		}
	}
}

static void rec_to_recordpara(JNIEnv *env, jobject rec, AM_REC_RecPara_t *para)
{
	int vpid, len;
	int *apids, *opids;
	jintArray apid_array;
	jintArray otherpid_array;
	jstring strpath;
	const char *path;
	
	memset(para, 0, sizeof(AM_REC_RecPara_t));
	para->is_timeshift  = env->GetBooleanField(rec, gRecParamsTimeshiftID);
	para->program.i_pid = env->GetIntField(rec, gRecParamsPmtPidID);
	para->program.i_number = env->GetIntField(rec, gRecParamsPmtProgramNumberID);
	
	rec_to_media_info(env, rec, &para->media_info);

	LOGI("Program '%s': PMT pid 0x%x, number 0x%x", para->media_info.program_name, para->program.i_pid, para->program.i_number);

	strpath = (jstring)env->GetObjectField(rec, gRecParamsPrefixNameID);
	path = env->GetStringUTFChars(strpath, 0);
	if (path != NULL){
		strncpy(para->prefix_name, path, AM_REC_NAME_MAX);
		para->prefix_name[AM_REC_NAME_MAX - 1] = 0;
		env->ReleaseStringUTFChars(strpath, path);
	}

	strpath = (jstring)env->GetObjectField(rec, gRecParamsSuffixNameID);
	path = env->GetStringUTFChars(strpath, 0);
	if (path != NULL){
		strncpy(para->suffix_name, path, AM_REC_SUFFIX_MAX);
		para->suffix_name[AM_REC_SUFFIX_MAX - 1] = 0;
		env->ReleaseStringUTFChars(strpath, path);
	}
	
	para->total_time = (int)(env->GetLongField(rec, gRecParamsTotalTimeID)/1000);
}

static jobject recinfo_to_para(JNIEnv *env, jobject object, AM_REC_RecInfo_t *info)
{
	jobject obj = env->NewObject(gRecParamsClass, gRecParamsInitID, object);

	env->SetObjectField(obj, gRecParamsFileID, env->NewStringUTF(info->file_path));
	env->SetLongField(obj, gRecParamsRecSizeID, info->file_size);
	env->SetLongField(obj, gRecParamsRecTimeID, (jlong)info->cur_rec_time*1000);
	env->SetLongField(obj, gRecParamsTotalTimeID, (jlong)info->record_para.total_time*1000);

	return obj;
}

static void playback_to_tspara(JNIEnv *env, jobject playback, AM_AV_TimeshiftPara_t *para)
{
	memset(para, 0, sizeof(AM_AV_TimeshiftPara_t));
	jstring filePath = (jstring)env->GetObjectField(playback, gPlaybackParamsFileID);
	const char *strpath = env->GetStringUTFChars(filePath, 0);
	if (strpath != NULL){
		strncpy(para->file_path, strpath, sizeof(para->file_path));
		para->dmx_id = PLAYBACK_DMX_DEV_NO;
		para->media_info.duration = (int)(env->GetLongField(playback, gPlaybackParamsTotaltimeID)/1000);
		LOGI("timeshifting duration %d", para->media_info.duration);
		env->ReleaseStringUTFChars(filePath, strpath);
	}
}

static jobject tsinfo_to_playback(JNIEnv *env, jobject object, AM_AV_TimeshiftInfo_t *info)
{
	jobject obj = env->NewObject(gPlaybackParamsClass, gPlaybackParamsInitID, object);

	env->SetIntField(obj, gPlaybackParamsStatusID, info->status);
	env->SetLongField(obj, gPlaybackParamsCurrtimeID, (jlong)info->current_time*1000);
	env->SetLongField(obj, gPlaybackParamsTotaltimeID, (jlong)info->full_time*1000);

	return obj;
}

static jobject mediainfo_to_object(JNIEnv *env, jobject object, AM_AV_TimeshiftMediaInfo_t *info)
{
	int i;
	
	jobject obj = env->NewObject(gRecParamsClass, gRecParamsInitID);

	jobject prgram = env->NewObject(gProgramClass, gProgramInitID);
	jobject video = env->NewObject(gVideoClass, gVideoInitID, prgram, info->vid_pid, info->vid_fmt);
	
	jobjectArray audios = NULL;
	if (info->aud_cnt > 0){
		jobject audio;
		audios = env->NewObjectArray(info->aud_cnt, gAudioClass, NULL);
		for (i=0; i<info->aud_cnt; i++){
			audio = env->NewObject(gAudioClass, gAudioInitID, prgram, info->audios[i].pid, 
				env->NewStringUTF(info->audios[i].lang), info->audios[i].fmt);
			env->SetObjectArrayElement(audios, i, audio);
		} 
	}

	jobjectArray subtitles = NULL;
	if (info->sub_cnt > 0){
		jobject subtitle;
		subtitles = env->NewObjectArray(info->sub_cnt, gSubtitleClass, NULL);
		for (i=0; i<info->sub_cnt; i++){
			subtitle = env->NewObject(gSubtitleClass, gSubtitleInitID, prgram, info->subtitles[i].pid, 
				env->NewStringUTF(info->subtitles[i].lang), info->subtitles[i].type, 0, 0);
			env->SetIntField(subtitle, gSubtitleCompPageID, info->subtitles[i].composition_page);
			env->SetIntField(subtitle, gSubtitleAnciPageID, info->subtitles[i].ancillary_page);
			env->SetIntField(subtitle, gSubtitleMagNoID,    info->subtitles[i].magzine_no);
			env->SetIntField(subtitle, gSubtitlePageNoID,   info->subtitles[i].page_no);
			env->SetObjectArrayElement(subtitles, i, subtitle);
		} 
	}

	jobjectArray teletexts = NULL;
	if (info->ttx_cnt > 0){
		jobject teletext;
		teletexts = env->NewObjectArray(info->ttx_cnt, gTeletextClass, NULL);
		for (i=0; i<info->ttx_cnt; i++){
			teletext = env->NewObject(gTeletextClass, gTeletextInitID, prgram, 
				info->teletexts[i].pid, env->NewStringUTF(info->teletexts[i].lang), 
				info->teletexts[i].magzine_no, info->teletexts[i].page_no);
			env->SetObjectArrayElement(teletexts, i, teletext);
		} 
	}

	env->SetObjectField(obj, gRecParamsProgramNameID, env->NewStringUTF(info->program_name));
	env->SetObjectField(obj, gRecParamsVideoID, video);
	env->SetObjectField(obj, gRecParamsAudioID, audios);
	env->SetObjectField(obj, gRecParamsSubtitleID, subtitles);
	env->SetObjectField(obj, gRecParamsTeletextID, teletexts);

	return obj;
}

static int getRecordError(int error)
{
	const int REC_ERR_NONE        = 0; // Success, no error
	const int REC_ERR_OPEN_FILE   = 1; // Cannot open output record file
	const int REC_ERR_WRITE_FILE  = 2; // Cannot write data to record file
	const int REC_ERR_ACCESS_FILE = 3; // Cannot access record file
	const int REC_ERR_SYSTEM      = 4; // For other system reasons
	int ret;
	
	switch (error){
		case AM_SUCCESS:
			ret = REC_ERR_NONE;
			break;
		case AM_REC_ERR_CANNOT_OPEN_FILE:
			ret = REC_ERR_OPEN_FILE;
			break;
		case AM_REC_ERR_CANNOT_WRITE_FILE:
			ret = REC_ERR_WRITE_FILE;
			break;
		case AM_REC_ERR_CANNOT_ACCESS_FILE:
			ret = REC_ERR_ACCESS_FILE;
			break;
		default:
			ret = REC_ERR_SYSTEM;
			break;
	}
	
	return ret;
}

static int read_media_info_from_file(const char *file_path, AM_AV_TimeshiftMediaInfo_t *info)
{
	uint8_t buf[sizeof(AM_REC_MediaInfo_t) + 4];
	int pos = 0, info_len, i, fd, name_len;
	
#define READ_INT(_i)\
	AM_MACRO_BEGIN\
		if ((info_len-pos) >= 4){\
			pos += 4;\
		}else{\
			goto read_error;\
		}\
	AM_MACRO_END

	fd = open(file_path, O_RDONLY, 0666);
	if (fd < 0){
		LOGE("Cannot open file '%s'", file_path);
		return -1;
	}
	
	info_len = read(fd, buf, sizeof(buf));

	pos += 4; /*skip the packet header*/
	READ_INT(info->duration);
		
	name_len = sizeof(info->program_name);
	if ((info_len-pos) >= name_len){
		memcpy(info->program_name, buf+pos, name_len);
		info->program_name[name_len - 1] = 0;
		pos += name_len;
	}else{
		goto read_error;
	}
	READ_INT(info->vid_pid);
	READ_INT(info->vid_fmt);
	READ_INT(info->aud_cnt);
	for (i=0; i<info->aud_cnt; i++){
		READ_INT(info->audios[i].pid);
		READ_INT(info->audios[i].fmt);
		memcpy(info->audios[i].lang, buf+pos, 4);
		pos += 4;
	}
	READ_INT(info->sub_cnt);
	for (i=0; i<info->sub_cnt; i++){
		READ_INT(info->subtitles[i].pid);
		READ_INT(info->subtitles[i].type);
		READ_INT(info->subtitles[i].composition_page);
		READ_INT(info->subtitles[i].ancillary_page);
		READ_INT(info->subtitles[i].magzine_no);
		READ_INT(info->subtitles[i].page_no);
		memcpy(info->subtitles[i].lang, buf+pos, 4);
		pos += 4;
	}
	READ_INT(info->ttx_cnt);
	for (i=0; i<info->ttx_cnt; i++){
		READ_INT(info->teletexts[i].pid);
		READ_INT(info->teletexts[i].magzine_no);
		READ_INT(info->teletexts[i].page_no);
		memcpy(info->teletexts[i].lang, buf+pos, 4);
		pos += 4;
	}
	close(fd);
	return 0;

read_error:
	LOGE("Read media info from file error, len %d, pos %d", info_len, pos);
	close(fd);

	return -1;
}

static void fend_cb(int dev_no, struct dvb_frontend_event *evt, void *user_data)
{
	TVDevice *dev = (TVDevice*)user_data;
	jobject event;
	jobject chan;
	JNIEnv *env;
	int ret;
	int attached = 0;
	
	ret = gJavaVM->GetEnv((void**) &env, JNI_VERSION_1_4);
	if(ret<0){
		ret = gJavaVM->AttachCurrentThread(&env, NULL);
		if(ret<0){
			LOGE("Can't attach thread");
			return;
		}
		attached = 1;
	}

	event = create_event(env, dev->dev_obj, EVENT_FRONTEND);
	chan  = fpara_to_chan(env, dev->fend_mode, &evt->parameters);

	env->SetIntField(event, gEventFEStatusID, evt->status);
	env->SetObjectField(event, gEventFEParamsID, chan);
	
	env->CallVoidMethod(dev->dev_obj, gOnEventID, event);

	if(attached){
		gJavaVM->DetachCurrentThread();
	}
}

static void dev_rec_evt_cb(int dev_no, int event_type, void *param, void *data)
{
	TVDevice *dev;
	
	AM_REC_GetUserData(dev_no, (void**)&dev);
	
	if (! dev)
		return;
	
	if (event_type == AM_REC_EVT_RECORD_END){
		AM_REC_RecEndPara_t *endpara = (AM_REC_RecEndPara_t*)param;
		jobject event;
		jobject recpara;
		JNIEnv *env;
		int ret;
		int attached = 0;
	
		ret = gJavaVM->GetEnv((void**) &env, JNI_VERSION_1_4);
		if(ret<0){
			ret = gJavaVM->AttachCurrentThread(&env, NULL);
			if(ret<0){
				LOGE("Can't attach thread");
				return;
			}
			attached = 1;
		}
		
		event = create_event(env, dev->dev_obj, EVENT_RECORD_END);
		recpara  = recendpara_to_para(env, dev->dev_obj, endpara);

		env->SetIntField(event, gEventRecEndCodeID, getRecordError(endpara->error_code));
		env->SetObjectField(event, gEventRecParamsID, recpara);
	
		env->CallVoidMethod(dev->dev_obj, gOnEventID, event);

		if(attached){
			gJavaVM->DetachCurrentThread();
		}
	}
}

static void dev_av_evt_cb(int dev_no, int event_type, void *param, void *data)
{
	TVDevice *dev = (TVDevice*)data;
	jobject event;
	JNIEnv *env;
	int ret, evttype = -1;
	int attached = 0;
		
	if (! dev)
		return;
	
	if (event_type == AM_AV_EVT_PLAYER_UPDATE_INFO){
		AM_AV_TimeshiftInfo_t *info = (AM_AV_TimeshiftInfo_t*)param;

		if (info == NULL)
			return;

		if (info->status == 5){
			evttype	= EVENT_PLAYBACK_START;
		}else if (info->status == 4){
			evttype = EVENT_PLAYBACK_END;
		}
	}else if (event_type == AM_AV_EVT_AUDIO_SCAMBLED || event_type == AM_AV_EVT_VIDEO_SCAMBLED){
		evttype = EVENT_DTV_CANNOT_DESCRAMLE;
	}else if (event_type == AM_AV_EVT_AV_NO_DATA){
		evttype = EVENT_DTV_NO_DATA;
	}else if (event_type == AM_AV_EVT_AV_DATA_RESUME){
		evttype = EVENT_DTV_DATA_RESUME;
	}

	if (evttype < 0)
		return;
	
	ret = gJavaVM->GetEnv((void**) &env, JNI_VERSION_1_4);
	if(ret<0){
		ret = gJavaVM->AttachCurrentThread(&env, NULL);
		if(ret<0){
			LOGE("Can't attach thread");
			return;
		}
		attached = 1;
	}
	
	event = create_event(env, dev->dev_obj, evttype);

	if (evttype == EVENT_PLAYBACK_START){
		jobject info = mediainfo_to_object(env, dev->dev_obj, &dev->timeshift_para.media_info);
		env->SetBooleanField(info, gRecParamsTimeshiftID, dev->timeshift_para.mode == AM_AV_TIMESHIFT_MODE_TIMESHIFTING);
		env->SetObjectField(event, gEventRecParamsID, info);
	}

	env->CallVoidMethod(dev->dev_obj, gOnEventID, event);

	if(attached){
		gJavaVM->DetachCurrentThread();
	}
}

static void dev_init(JNIEnv *env, jobject obj)
{
	TVDevice *dev;

	dev = (TVDevice*)malloc(sizeof(TVDevice));
	if(!dev){
		LOGE("malloc new device failed");
		return;
	}

	memset(dev, 0, sizeof(TVDevice));

	env->SetIntField(obj, gHandleID, (jint)dev);

	dev->dev_obj = env->NewWeakGlobalRef(obj);

	dev->ts_src = (AM_DMX_Source_t)-1;

	AM_EVT_Subscribe(0, AM_AV_EVT_PLAYER_UPDATE_INFO, dev_av_evt_cb, (void*)dev);
	AM_EVT_Subscribe(0, AM_AV_EVT_AV_NO_DATA, dev_av_evt_cb, (void*)dev);
	AM_EVT_Subscribe(0, AM_AV_EVT_AUDIO_SCAMBLED, dev_av_evt_cb, (void*)dev);
	AM_EVT_Subscribe(0, AM_AV_EVT_VIDEO_SCAMBLED, dev_av_evt_cb, (void*)dev);
	AM_EVT_Subscribe(0, AM_AV_EVT_AV_DATA_RESUME, dev_av_evt_cb, (void*)dev);
}

static void dev_destroy(JNIEnv *env, jobject obj)
{
	TVDevice *dev = get_dev(env, obj);

	AM_DMX_Close(DMX_DEV_NO);
	AM_DMX_Close(DVR_DEV_NO);
	AM_AV_Close(AV_DEV_NO);

	if(dev->dev_open){
		AM_FEND_Close(FEND_DEV_NO);
		AM_DMX_Close(DMX_DEV_NO);
		AM_AV_Close(DMX_DEV_NO);
	}

	env->DeleteWeakGlobalRef(dev->dev_obj);

	AM_EVT_Unsubscribe(0, AM_AV_EVT_PLAYER_UPDATE_INFO, dev_av_evt_cb, (void*)dev);
	AM_EVT_Unsubscribe(0, AM_AV_EVT_AV_NO_DATA, dev_av_evt_cb, (void*)dev);
	AM_EVT_Unsubscribe(0, AM_AV_EVT_AUDIO_SCAMBLED, dev_av_evt_cb, (void*)dev);
	AM_EVT_Unsubscribe(0, AM_AV_EVT_VIDEO_SCAMBLED, dev_av_evt_cb, (void*)dev);
	AM_EVT_Unsubscribe(0, AM_AV_EVT_AV_DATA_RESUME, dev_av_evt_cb, (void*)dev);
	
	free(dev);
}

unsigned long getSDKVersion(){
    char prop_value[PROPERTY_VALUE_MAX];
    const char *strDelimit = ".";
	unsigned long version=0;
   
    memset(prop_value, '\0', PROPERTY_VALUE_MAX);
    property_get("ro.build.version.sdk",prop_value,"SDK_VERSION_ERR");
	LOGE("VERSION_NUM %s", prop_value);
    if (strcmp(prop_value, "\0") != 0) {
		version = strtol(prop_value, NULL, 10);
		LOGE("VERSION_NUM --- %ld", version);
    }
	return version;
}

static void dev_set_input_source(JNIEnv *env, jobject obj, jint src)
{
	jobject evt;

	LOGE("dev_set_input_source %d", src);
	evt = create_event(env, obj, EVENT_SET_INPUT_SOURCE_OK);

	env->SetIntField(evt, gEventSourceID, src);

	on_event(obj, evt);

	TVDevice *dev = get_dev(env, obj);
	if(!dev->dev_open){
		AM_FEND_OpenPara_t fend_para;
		AM_AV_OpenPara_t av_para;
		AM_DMX_OpenPara_t dmx_para;

		memset(&fend_para, 0, sizeof(fend_para));
		fend_para.mode = FEND_DEF_MODE;

		AM_FEND_Open(FEND_DEV_NO, &fend_para);
		AM_FEND_SetCallback(FEND_DEV_NO, fend_cb, dev);

		memset(&dmx_para, 0, sizeof(dmx_para));
		AM_DMX_Open(DMX_DEV_NO, &dmx_para);
		/*open demux for corresponding dvr*/
		AM_DMX_Open(DVR_DEV_NO, &dmx_para);

		memset(&av_para, 0, sizeof(av_para));
		AM_AV_Open(AV_DEV_NO, &av_para);

		dev->dev_open = AM_TRUE;
	}

	int di = 1;

	if(src == SOURCE_DTV)
	{
		if(!di)
		{
			/*disable deinterlace*/
			if(getSDKVersion()<17){
				AM_AV_SetVPathPara(AV_DEV_NO, AM_AV_FREE_SCALE_DISABLE, AM_AV_DEINTERLACE_DISABLE, AM_AV_PPMGR_ENABLE);
				LOGE("AM_AV_SetVPathPara enter disable deinterlace\n");
			}	
		}
		else
		{
			/*enable deinterlace*/
			if(getSDKVersion()<17){
				AM_AV_SetVPathPara(AV_DEV_NO, AM_AV_FREE_SCALE_DISABLE, AM_AV_DEINTERLACE_ENABLE, AM_AV_PPMGR_DISABLE);
				LOGE("AM_AV_SetVPathPara enter enable deinterlace\n");
			}	
		}

		AM_AV_SetTSSource(AV_DEV_NO, AM_AV_TS_SRC_TS2);
		AM_AV_ClearVideoBuffer(AV_DEV_NO);
	}
	else
	{
		if(getSDKVersion()<17){	
			AM_AV_SetVPathPara(AV_DEV_NO, AM_AV_FREE_SCALE_ENABLE, AM_AV_DEINTERLACE_DISABLE, AM_AV_PPMGR_ENABLE);
			LOGE("AM_AV_SetVPathPara exit\n");
		}
	}
	
}

static jint vidoview_x=0;
static jint vidoview_y=0;
static jint vidoview_w=0;
static jint vidoview_h=0;
static void dev_set_video_window(JNIEnv *env, jobject obj, jint x, jint y, jint w, jint h)
{
	//LOGE("--dev_set_video_window--%d ---%d---%d---%d\n",x,y,w,h);
	char buf[64];
	char outputmode[64]= {'\0'};
	jint x_t=x;
	jint y_t=y;
	jint w_t=w;
	jint h_t=h;

	/*	
	property_get("ubootenv.var.outputmode",outputmode,NULL);

	if(strstr(outputmode,"1080p")!=NULL){
		x_t=x*1920/1280;
		y_t=y*1080/720;
		w_t=w*1920/1280;
		h_t=h*1080/720;
	}
	else if(strstr(outputmode,"720p")!=NULL){
		
	}
	else if(strstr(outputmode,"1080i")!=NULL){
		x_t=x*1920/1280;
		y_t=y*1080/720;
		w_t=w*1920/1280;
		h_t=h*1080/720;
	}
	else if(strstr(outputmode,"576i")!=NULL){
		x_t=x*720/1280;
		y_t=y*576/720;
		w_t=w*720/1280;
		h_t=h*576/720;
	}
	else if(strstr(outputmode,"576p")!=NULL){
		x_t=x*720/1280;
		y_t=y*576/720;
		w_t=w*720/1280;
		h_t=h*576/720;
	}
	else if(strstr(outputmode,"480i")!=NULL){
		x_t=x*720/1280;
		y_t=y*480/720;
		w_t=w*720/1280;
		h_t=h*480/720;
	}
	else if(strstr(outputmode,"480p")!=NULL){
		x_t=x*720/1280;
		y_t=y*480/720;
		w_t=w*720/1280;
		h_t=h*480/720;
	}
	*/
	snprintf(buf, sizeof(buf), "%d %d %d %d", x_t, y_t, x_t+w_t, y_t+h_t);
	vidoview_x=x;
	vidoview_y=y;
	vidoview_w=w;
	vidoview_h=h;	
	
	AM_FileEcho("/sys/class/video/axis", buf);
}

static void dev_set_frontend(JNIEnv *env, jobject obj, jobject params)
{
	AM_FENDCTRL_DVBFrontendParameters_t fpara;
	AM_SEC_DVBSatelliteEquipmentControl_t sec;
	AM_DMX_Source_t src;

	TVDevice *dev = get_dev(env, obj);

	if(!dev->dev_open){
		AM_FEND_OpenPara_t fend_para;
		AM_AV_OpenPara_t av_para;
		AM_DMX_OpenPara_t dmx_para;

		memset(&fend_para, 0, sizeof(fend_para));
		fend_para.mode = FEND_DEF_MODE;

		AM_FEND_Open(FEND_DEV_NO, &fend_para);
		AM_FEND_SetCallback(FEND_DEV_NO, fend_cb, dev);

		memset(&dmx_para, 0, sizeof(dmx_para));
		AM_DMX_Open(DMX_DEV_NO, &dmx_para);
		/*open demux for corresponding dvr*/
		AM_DMX_Open(DVR_DEV_NO, &dmx_para);

		memset(&av_para, 0, sizeof(av_para));
		AM_AV_Open(AV_DEV_NO, &av_para);

		dev->dev_open = AM_TRUE;
	}

	chan_to_fpara(env, params, &fpara);

        if (fpara.m_type == FE_QPSK) {
            jobject sp = env->GetObjectField(params, gChanParamsSatParaID);
            memset(&sec, 0, sizeof(sec));            
            get_sat_para(env, obj, sp, &sec);   

            AM_SEC_SetSetting(FEND_DEV_NO, &sec);           
        }

	dev->fend_mode = fpara.m_type;

	AM_FENDCTRL_SetPara(FEND_DEV_NO, &fpara);

	AM_FEND_GetTSSource(FEND_DEV_NO, &src);

	if(src != dev->ts_src){
		AM_AV_SetTSSource(AV_DEV_NO, (AM_AV_TSSource_t)src);
		AM_DMX_SetSource(DMX_DEV_NO, src);
		AM_DMX_SetSource(DVR_DEV_NO, src);

		dev->ts_src = src;
	}
}

static jobject dev_get_frontend(JNIEnv *env, jobject obj)
{
	struct dvb_frontend_parameters fpara;
	jobject params;

	TVDevice *dev = get_dev(env, obj);
	if(!dev->dev_open)
		return NULL;
	
	AM_FEND_GetPara(FEND_DEV_NO, &fpara);

	params = fpara_to_chan(env, dev->fend_mode, &fpara);

	return params;
}

static jint dev_get_frontend_status(JNIEnv *env, jobject obj)
{
	fe_status_t status;
	TVDevice *dev = get_dev(env, obj);
	if(!dev->dev_open)
		return 0;
	
	AM_FEND_GetStatus(FEND_DEV_NO, &status);
	
	return (jint)status;
}

static jint dev_get_frontend_signal_strength(JNIEnv *env, jobject obj)
{
	int strength;
	TVDevice *dev = get_dev(env, obj);
	if(!dev->dev_open)
		return 0;
	
	AM_FEND_GetStrength(FEND_DEV_NO, &strength);
	return strength;
}

static jint dev_get_frontend_snr(JNIEnv *env, jobject obj)
{
	int snr;
	TVDevice *dev = get_dev(env, obj);
	if(!dev->dev_open)
		return 0;
	
	AM_FEND_GetSNR(FEND_DEV_NO, &snr);
	return snr;
}

static jint dev_get_frontend_ber(JNIEnv *env, jobject obj)
{
	int ber;
	TVDevice *dev = get_dev(env, obj);
	if(!dev->dev_open)
		return 0;
	
	AM_FEND_GetBER(FEND_DEV_NO, &ber);
	return ber;
}

static void dev_free_frontend(JNIEnv *env, jobject obj)
{
	TVDevice *dev = get_dev(env, obj);
	if(!dev->dev_open)
		return;
	
	LOGI("free frontend");
	AM_FEND_Close(FEND_DEV_NO);
	AM_DMX_Close(DMX_DEV_NO);
	AM_AV_Close(AV_DEV_NO);

	dev->dev_open = AM_FALSE;
}

static void dev_start_vbi(JNIEnv *env, jobject obj, jint flags)
{
}

static void dev_stop_vbi(JNIEnv *env, jobject obj, jint flags)
{
}

static void dev_play_atv(JNIEnv *env, jobject obj)
{
}

static void dev_stop_atv()
{
}

static void dev_play_dtv(JNIEnv *env, jobject obj, jint vpid, jint vfmt, jint apid, jint afmt)
{
	TVDevice *dev = get_dev(env, obj);
	if(!dev->dev_open)
		return;

	AM_AV_StartTS(AV_DEV_NO, vpid, apid, (AM_AV_VFormat_t)vfmt, (AM_AV_AFormat_t)afmt);
	//LOGE("dev_play_dtv---%d---%d---%d---%d\n",vidoview_x,vidoview_y,vidoview_w,vidoview_h);
	dev_set_video_window(env,obj,vidoview_x,vidoview_y,vidoview_w,vidoview_h);
}

static void dev_switch_dtv_audio(JNIEnv *env, jobject obj, jint apid, jint afmt)
{
	TVDevice *dev = get_dev(env, obj);
	if(!dev->dev_open)
		return;

	if(dev->in_timeshifting){
		AM_AV_SwitchTimeshiftAudio(AV_DEV_NO, apid, (AM_AV_AFormat_t)afmt);
	}else{
		AM_AV_SwitchTSAudio(AV_DEV_NO, apid, (AM_AV_AFormat_t)afmt);
	}
}

static void dev_stop_dtv(JNIEnv *env, jobject obj)
{
	TVDevice *dev = get_dev(env, obj);
	if(!dev->dev_open)
		return;
	AM_AV_StopTS(AV_DEV_NO);
}

static void dev_start_recording(JNIEnv *env, jobject obj, jobject params)
{
	AM_REC_CreatePara_t cpara;
	AM_REC_RecPara_t rpara;
	int hrec;
	TVDevice *dev = get_dev(env, obj);
	if(!dev->dev_open)
		return;
		
	rec_to_createpara(env, params, &cpara);
	if (AM_REC_Create(&cpara, &hrec) != AM_SUCCESS){
		LOGE("Rec create failed");
		return;
	}
	
	AM_EVT_Subscribe(hrec, AM_REC_EVT_RECORD_END, dev_rec_evt_cb, NULL);
	AM_REC_SetUserData(hrec, (void*)dev);
	
	rec_to_recordpara(env, params, &rpara);

	/* start playback first */
	if (dev->in_timeshifting && rpara.is_timeshift){
		int duration = dev->timeshift_para.media_info.duration;
		dev->timeshift_para.media_info = rpara.media_info;
		dev->timeshift_para.media_info.duration = duration;
		dev->timeshift_para.mode = AM_AV_TIMESHIFT_MODE_TIMESHIFTING;
		
		if (AM_AV_StartTimeshift(AV_DEV_NO, &dev->timeshift_para) != AM_SUCCESS){
			LOGE("Device start timeshifting failed");
			AM_REC_Destroy(hrec);
			return;
		}
	}
		
	if (AM_REC_StartRecord(hrec, &rpara) != AM_SUCCESS){
		LOGE("Start record failed");
		AM_REC_Destroy(hrec);
		return;
	}
	
	dev->rec_handle = hrec;
}

static void dev_stop_recording(JNIEnv *env, jobject obj)
{
	TVDevice *dev = get_dev(env, obj);
	if(!dev->dev_open)
		return;
	if (dev->rec_handle != 0){
		AM_REC_Destroy(dev->rec_handle);
		AM_EVT_Unsubscribe(dev->rec_handle, AM_REC_EVT_RECORD_END, dev_rec_evt_cb, NULL);
		dev->rec_handle = 0;
	}
}

static jobject dev_get_recording_params(JNIEnv *env, jobject obj)
{
	TVDevice *dev = get_dev(env, obj);
	if(!dev->dev_open)
		return NULL;
	if (dev->rec_handle != 0){
		AM_REC_RecInfo_t info;
		AM_REC_GetRecordInfo(dev->rec_handle, &info);
		
		return recinfo_to_para(env, obj, &info);
	}
	
	return NULL;
}

static void dev_start_timeshifting(JNIEnv *env, jobject obj, jobject params)
{
	TVDevice *dev = get_dev(env, obj);
	if(!dev->dev_open)
		return;

	playback_to_tspara(env, params, &dev->timeshift_para);

	dev->in_timeshifting = AM_TRUE;
}

static void dev_stop_timeshifting(JNIEnv *env, jobject obj)
{
	TVDevice *dev = get_dev(env, obj);
	if(!dev->dev_open)
		return;

	AM_AV_StopTimeshift(AV_DEV_NO);
	dev->in_timeshifting = AM_FALSE;
}

static void dev_start_playback(JNIEnv *env, jobject obj, jobject params)
{
	AM_AV_TimeshiftPara_t para;
	TVDevice *dev = get_dev(env, obj);
	if(!dev->dev_open)
		return;
		
	playback_to_tspara(env, params, &para);

	if (read_media_info_from_file(para.file_path, &para.media_info) < 0)
		return;
	para.mode = AM_AV_TIMESHIFT_MODE_PLAYBACK;
	if (AM_AV_StartTimeshift(AV_DEV_NO, &para) != AM_SUCCESS){
		LOGE("Device start plaback failed");
	}else{
		dev->in_timeshifting = AM_TRUE;

		dev->timeshift_para.media_info = para.media_info;
		dev->timeshift_para.mode = para.mode;
	}
}

static void dev_stop_playback(JNIEnv *env, jobject obj)
{
	TVDevice *dev = get_dev(env, obj);
	if(!dev->dev_open)
		return;
	AM_AV_StopTimeshift(AV_DEV_NO);
	dev->in_timeshifting = AM_FALSE;
}

static jobject dev_get_playback_params(JNIEnv *env, jobject obj)
{
	AM_AV_TimeshiftInfo_t info;
	TVDevice *dev = get_dev(env, obj);
	if(!dev->dev_open)
		return NULL;
	
	if (AM_AV_GetTimeshiftInfo(AV_DEV_NO, &info) == AM_SUCCESS){
		return tsinfo_to_playback(env, obj, &info);
	}
	
	return NULL;
}

static void dev_fast_forward(JNIEnv *env, jobject obj, jint speed)
{
	AM_AV_FastForwardTimeshift(AV_DEV_NO, speed);
}

static void dev_fast_backward(JNIEnv *env, jobject obj, jint speed)
{
	AM_AV_FastBackwardTimeshift(AV_DEV_NO, speed);
}

static void dev_pause(JNIEnv *env, jobject obj)
{
	AM_AV_PauseTimeshift(AV_DEV_NO);
}

static void dev_resume(JNIEnv *env, jobject obj)
{
	AM_AV_ResumeTimeshift(AV_DEV_NO);
}

static void dev_seek_to(JNIEnv *env, jobject obj, jint pos)
{
	AM_AV_SeekTimeshift(AV_DEV_NO, pos, AM_TRUE);
}

static void dev_setSecRequest(JNIEnv *env, jobject obj, jint secType, jobject secCurParams, jint secPositionerMoveUnit)
{
	AM_FENDCTRL_DVBFrontendParameters_t fpara;
	AM_SEC_DVBSatelliteEquipmentControl_t sec;
	AM_DMX_Source_t src;

	TVDevice *dev = get_dev(env, obj);

	if(!dev->dev_open){
		AM_FEND_OpenPara_t fend_para;
		AM_AV_OpenPara_t av_para;
		AM_DMX_OpenPara_t dmx_para;

		memset(&fend_para, 0, sizeof(fend_para));
		fend_para.mode = FEND_DEF_MODE;

		AM_FEND_Open(FEND_DEV_NO, &fend_para);
		AM_FEND_SetCallback(FEND_DEV_NO, fend_cb, dev);

		memset(&dmx_para, 0, sizeof(dmx_para));
		AM_DMX_Open(DMX_DEV_NO, &dmx_para);
		/*open demux for corresponding dvr*/
		AM_DMX_Open(DVR_DEV_NO, &dmx_para);

		memset(&av_para, 0, sizeof(av_para));
		AM_AV_Open(AV_DEV_NO, &av_para);

		dev->dev_open = AM_TRUE;
	}

	memset(&sec, 0, sizeof(sec));

	if((secType == TYPE_SEC_LNBSSWITCHCFGVALID)
		|| (secType == TYPE_SEC_POSITIONEREAST)
		|| (secType == TYPE_SEC_POSITIONERWEST)
		|| (secType == TYPE_SEC_POSITIONERSTORE)
		|| (secType == TYPE_SEC_POSITIONERGOTO)
		|| (secType == TYPE_SEC_POSITIONERGOTOX)){
		chan_to_fpara(env, secCurParams, &fpara);

		if (fpara.m_type == FE_QPSK) {
			jobject sp = env->GetObjectField(secCurParams, gChanParamsSatParaID);
			get_sat_para(env, obj, sp, &sec);
		}

		if((secType == TYPE_SEC_POSITIONEREAST) || (secType == TYPE_SEC_POSITIONERWEST)){
			sec.m_lnbs.m_rotor_parameters.m_rotor_move_unit = secPositionerMoveUnit;
		}		
		
	}
	else if((secType == TYPE_SEC_POSITIONERSTOP)
			|| (secType == TYPE_SEC_POSITIONERDISABLELIMIT)
			|| (secType == TYPE_SEC_POSITIONEREASTLIMIT)
			|| (secType == TYPE_SEC_POSITIONERWESTLIMIT)){
		memset(&fpara, 0, sizeof(AM_FENDCTRL_DVBFrontendParameters_t));
		/*not change sec setting, exclude sec_cmd*/
		AM_SEC_GetSetting(FEND_DEV_NO, &sec);	
	}

	sec.sec_cmd = (AM_SEC_Cmd_t)secType;

	AM_SEC_SetSetting(FEND_DEV_NO, &sec);	
	
	dev->fend_mode = fpara.m_type;

	AM_SEC_ExecSecCmd(FEND_DEV_NO, &fpara);

	AM_FEND_GetTSSource(FEND_DEV_NO, &src);

	if(src != dev->ts_src){
		AM_AV_SetTSSource(AV_DEV_NO, (AM_AV_TSSource_t)src);
		AM_DMX_SetSource(DMX_DEV_NO, src);
		AM_DMX_SetSource(DVR_DEV_NO, src);

		dev->ts_src = src;
	}
}

static JNINativeMethod gMethods[] = {
	/* name, signature, funcPtr */
	{"native_device_init", "()V", (void*)dev_init},
	{"native_device_destroy", "()V", (void*)dev_destroy},
	{"native_set_input_source", "(I)V", (void*)dev_set_input_source},
	{"native_set_video_window", "(IIII)V", (void*)dev_set_video_window},
	{"native_set_frontend", "(Lcom/amlogic/tvutil/TVChannelParams;)V", (void*)dev_set_frontend},
	{"native_get_frontend", "()Lcom/amlogic/tvutil/TVChannelParams;", (void*)dev_get_frontend},
	{"native_get_frontend_status", "()I", (void*)dev_get_frontend_status},
	{"native_get_frontend_signal_strength", "()I", (void*)dev_get_frontend_signal_strength},
	{"native_get_frontend_snr", "()I", (void*)dev_get_frontend_snr},
	{"native_get_frontend_ber", "()I", (void*)dev_get_frontend_ber},
	{"native_free_frontend", "()V", (void*)dev_free_frontend},
	{"native_start_vbi", "(I)V", (void*)dev_start_vbi},
	{"native_stop_vbi", "(I)V", (void*)dev_stop_vbi},
	{"native_play_atv", "()V", (void*)dev_play_atv},
	{"native_stop_atv", "()V", (void*)dev_stop_atv},
	{"native_play_dtv", "(IIII)V", (void*)dev_play_dtv},
	{"native_switch_dtv_audio", "(II)V", (void*)dev_switch_dtv_audio},
	{"native_stop_dtv", "()V", (void*)dev_stop_dtv},
	{"native_start_recording", "(Lcom/amlogic/tvutil/DTVRecordParams;)V", (void*)dev_start_recording},
	{"native_stop_recording", "()V", (void*)dev_stop_recording},
	{"native_get_recording_params", "()Lcom/amlogic/tvutil/DTVRecordParams;", (void*)dev_get_recording_params},
	{"native_start_timeshifting", "(Lcom/amlogic/tvutil/DTVPlaybackParams;)V", (void*)dev_start_timeshifting},
	{"native_stop_timeshifting", "()V", (void*)dev_stop_timeshifting},
	{"native_start_playback", "(Lcom/amlogic/tvutil/DTVPlaybackParams;)V", (void*)dev_start_playback},
	{"native_stop_playback", "()V", (void*)dev_stop_playback},
	{"native_get_playback_params", "()Lcom/amlogic/tvutil/DTVPlaybackParams;", (void*)dev_get_playback_params},
	{"native_fast_forward", "(I)V", (void*)dev_fast_forward},
	{"native_fast_backward", "(I)V", (void*)dev_fast_backward},
	{"native_pause", "()V", (void*)dev_pause},
	{"native_resume", "()V", (void*)dev_resume},
	{"native_seek_to", "(I)V", (void*)dev_seek_to},
	{"native_setSecRequest", "(ILcom/amlogic/tvutil/TVChannelParams;I)V", (void*)dev_setSecRequest}
};

JNIEXPORT jint
JNI_OnLoad(JavaVM* vm, void* reserved)
{
	JNIEnv* env = NULL;
	jclass clazz;
	int rc;

	gJavaVM = vm;

	if(vm->GetEnv((void**) &env, JNI_VERSION_1_4) != JNI_OK){
		LOGE("GetEnv failed");
		return -1;
	}

	clazz = env->FindClass("com/amlogic/tvservice/TVDeviceImpl");
	if(clazz == NULL){
		LOGE("FindClass com/amlogic/tvservice/TVDeviceImpl failed");
		return -1;
	}

	if((rc = (env->RegisterNatives(clazz, gMethods, sizeof(gMethods)/sizeof(gMethods[0])))) < 0){
		LOGE("RegisterNatives failed");
		return -1;
	}

	gOnEventID = env->GetMethodID(clazz, "onEvent", "(Lcom/amlogic/tvservice/TVDevice$Event;)V");
	gHandleID  = env->GetFieldID(clazz, "native_handle", "I");
	gEventClass       = env->FindClass("com/amlogic/tvservice/TVDevice$Event");
	gEventClass       = (jclass)env->NewGlobalRef((jobject)gEventClass);
	gEventInitID      = env->GetMethodID(gEventClass, "<init>", "(Lcom/amlogic/tvservice/TVDevice;I)V");
	gEventFEParamsID  = env->GetFieldID(gEventClass, "feParams", "Lcom/amlogic/tvutil/TVChannelParams;");
	gEventFEStatusID  = env->GetFieldID(gEventClass, "feStatus", "I");
	gEventSourceID    = env->GetFieldID(gEventClass, "source", "I");
	gEventRecEndCodeID = env->GetFieldID(gEventClass, "recEndCode", "I");
	gEventRecParamsID = env->GetFieldID(gEventClass, "recParams", "Lcom/amlogic/tvutil/DTVRecordParams;");
	gChanParamsClass  = env->FindClass("com/amlogic/tvutil/TVChannelParams");
	gChanParamsClass  = (jclass)env->NewGlobalRef((jobject)gChanParamsClass);
	gChanParamsModeID = env->GetFieldID(gChanParamsClass, "mode", "I");
	gChanParamsFreqID = env->GetFieldID(gChanParamsClass, "frequency", "I");
	gChanParamsModID  = env->GetFieldID(gChanParamsClass, "modulation", "I");
	gChanParamsSymID  = env->GetFieldID(gChanParamsClass, "symbolRate", "I");
	gChanParamsBWID   = env->GetFieldID(gChanParamsClass, "bandwidth", "I");
	gChanParamsSatPolarID   = env->GetFieldID(gChanParamsClass, "sat_polarisation", "I");
	gChanParamsSatParaID = env->GetFieldID(gChanParamsClass, "tv_satparams", "Lcom/amlogic/tvutil/TVSatelliteParams;");
	gChanParamsInitID = env->GetMethodID(gChanParamsClass, "<init>", "(I)V");
	gRecParamsClass   = env->FindClass("com/amlogic/tvutil/DTVRecordParams");
	gRecParamsClass   = (jclass)env->NewGlobalRef((jobject)gRecParamsClass);
	gVideoClass       = env->FindClass("com/amlogic/tvutil/TVProgram$Video");
	gVideoClass       = (jclass)env->NewGlobalRef((jobject)gVideoClass);
	gVideoInitID      = env->GetMethodID(gVideoClass, "<init>", "(Lcom/amlogic/tvutil/TVProgram;II)V");
	gVideoPidID       = env->GetFieldID(gVideoClass, "pid", "I");
	gVideoFmtID       = env->GetFieldID(gVideoClass, "format", "I");
	gAudioClass       = env->FindClass("com/amlogic/tvutil/TVProgram$Audio");
	gAudioClass       = (jclass)env->NewGlobalRef((jobject)gAudioClass);
	gAudioInitID      = env->GetMethodID(gAudioClass, "<init>", "(Lcom/amlogic/tvutil/TVProgram;ILjava/lang/String;I)V");
	gAudioPidID       = env->GetFieldID(gAudioClass, "pid", "I");
	gAudioFmtID       = env->GetFieldID(gAudioClass, "format", "I");
	gAudioLangID      = env->GetFieldID(gAudioClass, "lang", "Ljava/lang/String;");
	gSubtitleClass    = env->FindClass("com/amlogic/tvutil/TVProgram$Subtitle");
	gSubtitleClass    = (jclass)env->NewGlobalRef((jobject)gSubtitleClass);
	gSubtitleInitID      = env->GetMethodID(gSubtitleClass, "<init>", "(Lcom/amlogic/tvutil/TVProgram;ILjava/lang/String;III)V");
	gSubtitlePidID    = env->GetFieldID(gSubtitleClass, "pid", "I");
	gSubtitleTypeID   = env->GetFieldID(gSubtitleClass, "type", "I");
	gSubtitleCompPageID   = env->GetFieldID(gSubtitleClass, "compositionPage", "I");
	gSubtitleAnciPageID   = env->GetFieldID(gSubtitleClass, "ancillaryPage", "I");
	gSubtitleMagNoID   = env->GetFieldID(gSubtitleClass, "magazineNo", "I");
	gSubtitlePageNoID   = env->GetFieldID(gSubtitleClass, "pageNo", "I");
	gSubtitleLangID   = env->GetFieldID(gSubtitleClass, "lang", "Ljava/lang/String;");
	gTeletextClass    = env->FindClass("com/amlogic/tvutil/TVProgram$Teletext");
	gTeletextClass    = (jclass)env->NewGlobalRef((jobject)gTeletextClass);
	gTeletextInitID   = env->GetMethodID(gTeletextClass, "<init>", "(Lcom/amlogic/tvutil/TVProgram;ILjava/lang/String;II)V");
	gTeletextPidID    = env->GetFieldID(gTeletextClass, "pid", "I");
	gTeletextMagNoID   = env->GetFieldID(gTeletextClass, "magazineNo", "I");
	gTeletextPageNoID   = env->GetFieldID(gTeletextClass, "pageNo", "I");
	gTeletextLangID   = env->GetFieldID(gTeletextClass, "lang", "Ljava/lang/String;");
	gRecParamsFileID  = env->GetFieldID(gRecParamsClass, "recFilePath", "Ljava/lang/String;");
	gRecParamsStorageID  = env->GetFieldID(gRecParamsClass, "storagePath", "Ljava/lang/String;");
	gRecParamsVideoID  = env->GetFieldID(gRecParamsClass, "video", "Lcom/amlogic/tvutil/TVProgram$Video;");
	gRecParamsAudioID  = env->GetFieldID(gRecParamsClass, "audios", "[Lcom/amlogic/tvutil/TVProgram$Audio;");
	gRecParamsSubtitleID  = env->GetFieldID(gRecParamsClass, "subtitles", "[Lcom/amlogic/tvutil/TVProgram$Subtitle;");
	gRecParamsTeletextID  = env->GetFieldID(gRecParamsClass, "teletexts", "[Lcom/amlogic/tvutil/TVProgram$Teletext;");
	gRecParamsRecSizeID   = env->GetFieldID(gRecParamsClass, "currRecordSize", "J");
	gRecParamsRecTimeID   = env->GetFieldID(gRecParamsClass, "currRecordTime", "J");
	gRecParamsTotalTimeID   = env->GetFieldID(gRecParamsClass, "recTotalTime", "J");
	gRecParamsTimeshiftID   = env->GetFieldID(gRecParamsClass, "isTimeshift", "Z");
	gRecParamsPrefixNameID  = env->GetFieldID(gRecParamsClass, "prefixFileName", "Ljava/lang/String;");
	gRecParamsSuffixNameID  = env->GetFieldID(gRecParamsClass, "suffixFileName", "Ljava/lang/String;");
	gRecParamsPmtPidID      = env->GetFieldID(gRecParamsClass, "pmtPID", "I");
	gRecParamsPmtProgramNumberID = env->GetFieldID(gRecParamsClass, "pmtProgramNumber", "I");
	gRecParamsProgramNameID = env->GetFieldID(gRecParamsClass, "programName", "Ljava/lang/String;");
	gRecParamsInitID  = env->GetMethodID(gRecParamsClass, "<init>", "()V");
	gPlaybackParamsClass   = env->FindClass("com/amlogic/tvutil/DTVPlaybackParams");
	gPlaybackParamsClass   = (jclass)env->NewGlobalRef((jobject)gPlaybackParamsClass);
	gPlaybackParamsFileID  = env->GetFieldID(gPlaybackParamsClass, "filePath", "Ljava/lang/String;");
	gPlaybackParamsStatusID     = env->GetFieldID(gPlaybackParamsClass, "status", "I");
	gPlaybackParamsCurrtimeID    = env->GetFieldID(gPlaybackParamsClass, "currentTime", "J");
	gPlaybackParamsTotaltimeID  = env->GetFieldID(gPlaybackParamsClass, "totalTime", "J");
	gPlaybackParamsInitID       = env->GetMethodID(gPlaybackParamsClass, "<init>", "()V");
	gProgramClass       = env->FindClass("com/amlogic/tvutil/TVProgram");
	gProgramClass       = (jclass)env->NewGlobalRef((jobject)gProgramClass);
	gProgramInitID      = env->GetMethodID(gProgramClass, "<init>", "()V");
	
	LOGI("load jnitvmboxdevice ok");
	return JNI_VERSION_1_4;
}

JNIEXPORT void
JNI_OnUnload(JavaVM* vm, void* reserved)
{
	JNIEnv* env = NULL;

	env->DeleteGlobalRef((jobject)gEventClass);
	env->DeleteGlobalRef((jobject)gChanParamsClass);

	if (vm->GetEnv((void**) &env, JNI_VERSION_1_4) != JNI_OK) {
		return;
	}
}

} /*extern "C"*/

