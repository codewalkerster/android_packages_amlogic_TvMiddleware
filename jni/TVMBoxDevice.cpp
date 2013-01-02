#include <am_av.h>
#include <am_dmx.h>
#include <am_fend_ctrl.h>
#include <am_misc.h>
#include <am_rec.h>
#include <jni.h>
#include <android/log.h>
#include <stdio.h>

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
} TVDevice;

#define FEND_DEV_NO    0
#define FEND_DEF_MODE  -1
#define AV_DEV_NO      0
#define DMX_DEV_NO     0
#define DVR_DEV_NO     0
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

static JavaVM   *gJavaVM = NULL;
static jclass    gEventClass;
static jclass    gChanParamsClass;
static jclass    gRecParamsClass;
static jclass    gPlaybackParamsClass; 
static jmethodID gEventInitID;
static jmethodID gChanParamsInitID;
static jmethodID gOnEventID;
static jmethodID gRecParamsInitID;
static jmethodID gPlaybackParamsInitID;
static jfieldID  gHandleID;
static jfieldID  gChanParamsModeID;
static jfieldID  gChanParamsFreqID;
static jfieldID  gChanParamsModID;
static jfieldID  gChanParamsSymID;
static jfieldID  gChanParamsBWID;
static jfieldID  gEventFEParamsID;
static jfieldID  gEventFEStatusID;
static jfieldID  gEventSourceID;
static jfieldID  gEventRecParamsID;
static jfieldID  gEventRecEndCodeID;
static jfieldID  gRecParamsFileID;
static jfieldID  gRecParamsStorageID;
static jfieldID  gRecParamsVPidID;
static jfieldID  gRecParamsAPidID;
static jfieldID  gRecParamsOtherPidID;
static jfieldID  gRecParamsRecSizeID;
static jfieldID  gRecParamsRecTimeID;
static jfieldID  gRecParamsTotalTimeID;
static jfieldID  gRecParamsTimeshiftID;
static jfieldID  gPlaybackParamsFileID;
static jfieldID  gPlaybackParamsVPidID;
static jfieldID  gPlaybackParamsAPidID;
static jfieldID  gPlaybackParamsVFmtID;
static jfieldID  gPlaybackParamsAFmtID;
static jfieldID  gPlaybackParamsStatusID;
static jfieldID  gPlaybackParamsCurrtimeID;
static jfieldID  gPlaybackParamsTotaltimeID;

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
	jint mode, freq, mod, sym, bw;

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
	}

	return obj;
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

static void rec_to_recordpara(JNIEnv *env, jobject rec, AM_REC_RecPara_t *para)
{
	int vpid, len;
	int *apids, *opids;
	jintArray apid_array;
	jintArray otherpid_array;
	
	memset(para, 0, sizeof(AM_REC_RecPara_t));
	para->is_timeshift = env->GetBooleanField(rec, gRecParamsTimeshiftID);
	para->pid_count = 0;
	
	vpid = env->GetIntField(rec, gRecParamsVPidID);
	if (vpid >= 0 && vpid < 0x1fff){
		para->pids[para->pid_count++] = vpid;
		para->has_video = AM_TRUE;
	}
	apid_array = (jintArray)env->GetObjectField(rec, gRecParamsAPidID);
	apids = env->GetIntArrayElements(apid_array, NULL);
	if (apids != NULL){
		len = env->GetArrayLength(apid_array);
		LOGE("auds %d", len);
		if (len > 0){
			memcpy(para->pids+para->pid_count, apids, len*sizeof(int));
			para->pid_count += len;
			para->has_audio = AM_TRUE;
		}
		env->ReleaseIntArrayElements(apid_array, apids, 0);
	}
	otherpid_array = (jintArray)env->GetObjectField(rec, gRecParamsOtherPidID);
	opids = env->GetIntArrayElements(otherpid_array, NULL);
	if (opids != NULL){
		len = env->GetArrayLength(otherpid_array);
		LOGE("others %d", len);
		if (len > 0){
			memcpy(para->pids+para->pid_count, opids, len*sizeof(int));
			para->pid_count += len;
		}
		env->ReleaseIntArrayElements(otherpid_array, opids, 0);
	}
	
	for (int i=0; i<para->pid_count; i++){
		LOGE("%d ", para->pids[i]);
	}
	
	para->total_time = env->GetLongField(rec, gRecParamsTotalTimeID);
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
		para->aud_fmt = (AM_AV_AFormat_t)env->GetIntField(playback, gPlaybackParamsAFmtID);
		para->vid_fmt = (AM_AV_VFormat_t)env->GetIntField(playback, gPlaybackParamsVFmtID);
		para->aud_id = env->GetIntField(playback, gPlaybackParamsAPidID);
		para->vid_id = env->GetIntField(playback, gPlaybackParamsVPidID);
		para->duration = env->GetLongField(playback, gPlaybackParamsTotaltimeID)/1000;
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

		env->SetIntField(event, gEventRecEndCodeID, endpara->error_code);
		env->SetObjectField(event, gEventRecParamsID, recpara);
	
		env->CallVoidMethod(dev->dev_obj, gOnEventID, event);

		if(attached){
			gJavaVM->DetachCurrentThread();
		}
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
}

static void dev_destroy(JNIEnv *env, jobject obj)
{
	TVDevice *dev = get_dev(env, obj);

	AM_DMX_Close(DMX_DEV_NO);
	AM_AV_Close(AV_DEV_NO);

	if(dev->dev_open){
		AM_FEND_Close(FEND_DEV_NO);
		AM_DMX_Close(DMX_DEV_NO);
		AM_AV_Close(DMX_DEV_NO);
	}

	env->DeleteWeakGlobalRef(dev->dev_obj);

	free(dev);
}

static void dev_set_input_source(JNIEnv *env, jobject obj, jint src)
{
	jobject evt;

	LOGE("dev_set_input_source %d", src);
	evt = create_event(env, obj, EVENT_SET_INPUT_SOURCE_OK);

	env->SetIntField(evt, gEventSourceID, src);

	on_event(obj, evt);
}

static void dev_set_video_window(JNIEnv *env, jobject obj, jint x, jint y, jint w, jint h)
{
	char buf[64];

	snprintf(buf, sizeof(buf), "%d %d %d %d", x, y, x+w, y+h);

	AM_FileEcho("/sys/class/video/axis", buf);
}

static void dev_set_frontend(JNIEnv *env, jobject obj, jobject params)
{
	AM_FENDCTRL_DVBFrontendParameters_t fpara;
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

		memset(&av_para, 0, sizeof(av_para));
		AM_AV_Open(AV_DEV_NO, &av_para);

		dev->dev_open = AM_TRUE;
	}

	chan_to_fpara(env, params, &fpara);

	dev->fend_mode = fpara.m_type;

	AM_FENDCTRL_SetPara(FEND_DEV_NO, &fpara);

	AM_FEND_GetTSSource(FEND_DEV_NO, &src);

	if(src != dev->ts_src){
		AM_AV_SetTSSource(AV_DEV_NO, (AM_AV_TSSource_t)src);
		AM_DMX_SetSource(DMX_DEV_NO, src);

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
		dev->rec_handle = NULL;
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
	AM_AV_TimeshiftPara_t para;
	TVDevice *dev = get_dev(env, obj);
	if(!dev->dev_open)
		return;
		
	playback_to_tspara(env, params, &para);
	para.playback_only = AM_FALSE;
	if (AM_AV_StartTimeshift(AV_DEV_NO, &para) != AM_SUCCESS){
		LOGE("Device start plaback failed");
	}
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
	para.playback_only = AM_TRUE;
	if (AM_AV_StartTimeshift(AV_DEV_NO, &para) != AM_SUCCESS){
		LOGE("Device start plaback failed");
	}
}

static void dev_stop_playback(JNIEnv *env, jobject obj)
{
	TVDevice *dev = get_dev(env, obj);
	if(!dev->dev_open)
		return;
	AM_AV_StopTimeshift(AV_DEV_NO);
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
	{"native_seek_to", "(I)V", (void*)dev_seek_to}
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
	gChanParamsInitID = env->GetMethodID(gChanParamsClass, "<init>", "(I)V");
	gRecParamsClass   = env->FindClass("com/amlogic/tvutil/DTVRecordParams");
	gRecParamsClass   = (jclass)env->NewGlobalRef((jobject)gRecParamsClass);
	gRecParamsFileID  = env->GetFieldID(gRecParamsClass, "recFilePath", "Ljava/lang/String;");
	gRecParamsStorageID  = env->GetFieldID(gRecParamsClass, "storagePath", "Ljava/lang/String;");
	gRecParamsVPidID  = env->GetFieldID(gRecParamsClass, "vidPid", "I");
	gRecParamsAPidID  = env->GetFieldID(gRecParamsClass, "audPids", "[I");
	gRecParamsOtherPidID  = env->GetFieldID(gRecParamsClass, "otherPids", "[I");
	gRecParamsRecSizeID   = env->GetFieldID(gRecParamsClass, "currRecordSize", "J");
	gRecParamsRecTimeID   = env->GetFieldID(gRecParamsClass, "currRecordTime", "J");
	gRecParamsTotalTimeID   = env->GetFieldID(gRecParamsClass, "recTotalTime", "J");
	gRecParamsTimeshiftID   = env->GetFieldID(gRecParamsClass, "isTimeshift", "Z");
	gRecParamsInitID  = env->GetMethodID(gRecParamsClass, "<init>", "()V");
	gPlaybackParamsClass   = env->FindClass("com/amlogic/tvutil/DTVPlaybackParams");
	gPlaybackParamsClass   = (jclass)env->NewGlobalRef((jobject)gPlaybackParamsClass);
	gPlaybackParamsFileID  = env->GetFieldID(gPlaybackParamsClass, "filePath", "Ljava/lang/String;");
	gPlaybackParamsVPidID  = env->GetFieldID(gPlaybackParamsClass, "vPid", "I");
	gPlaybackParamsAPidID  = env->GetFieldID(gPlaybackParamsClass, "aPid", "I");
	gPlaybackParamsVFmtID  = env->GetFieldID(gPlaybackParamsClass, "vFmt", "I");
	gPlaybackParamsAFmtID  = env->GetFieldID(gPlaybackParamsClass, "aFmt", "I");
	gPlaybackParamsStatusID     = env->GetFieldID(gPlaybackParamsClass, "status", "I");
	gPlaybackParamsCurrtimeID    = env->GetFieldID(gPlaybackParamsClass, "currentTime", "J");
	gPlaybackParamsTotaltimeID  = env->GetFieldID(gPlaybackParamsClass, "totalTime", "J");
	gPlaybackParamsInitID       = env->GetMethodID(gPlaybackParamsClass, "<init>", "()V");
	
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

