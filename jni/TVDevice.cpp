#include <am_av.h>
#include <am_dmx.h>
#include <am_fend_ctrl.h>
#include <jni.h>
#include <android/log.h>

extern "C" {

#ifdef LOG_TAG
#undef LOG_TAG
#endif

#define LOG_TAG    "jnitvdevice"
#define LOGI(...)  __android_log_print(ANDROID_LOG_INFO,LOG_TAG,__VA_ARGS__)
#define LOGE(...)  __android_log_print(ANDROID_LOG_ERROR,LOG_TAG,__VA_ARGS__)

typedef struct{
	AM_Bool_t dev_open;
	int       fend_mode;
	jobject   dev_obj;
	AM_DMX_Source_t ts_src;
}TVDevice;

#define FEND_DEV_NO    0
#define FEND_DEF_MODE  -1
#define AV_DEV_NO      0
#define DMX_DEV_NO     0

#define EVENT_SET_INPUT_SOURCE_OK     0
#define EVENT_SET_INPUT_SOURCE_FAILED 1
#define EVENT_VCHIP_BLOCKED           2
#define EVENT_VCHIP_UNBLOCKED         3
#define EVENT_FRONTEND                4
#define EVENT_DTV_NO_DATA             5
#define EVENT_DTV_CANNOT_DESCRAMLE    6
#define EVENT_RECORD                  7

static JavaVM   *gJavaVM = NULL;
static jclass    gEventClass;
static jclass    gChanParamsClass;
static jmethodID gEventInitID;
static jmethodID gChanParamsInitID;
static jmethodID gOnEventID;
static jfieldID  gHandleID;
static jfieldID  gChanParamsModeID;
static jfieldID  gChanParamsFreqID;
static jfieldID  gChanParamsModID;
static jfieldID  gChanParamsSymID;
static jfieldID  gChanParamsBWID;
static jfieldID  gEventFEParamsID;
static jfieldID  gEventFEStatusID;

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

static void dev_set_input_source(JNIEnv *env, jobject obj, int src)
{
	jobject evt;

	LOGE("dev_set_input_source %d", src);
	evt = create_event(env, obj, EVENT_SET_INPUT_SOURCE_OK);

	on_event(obj, evt);
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
	AM_AV_StartTS(AV_DEV_NO, vpid, apid, (AM_AV_VFormat_t)vfmt, (AM_AV_AFormat_t)afmt);
}

static void dev_stop_dtv(JNIEnv *env, jobject obj)
{
	AM_AV_StopTS(AV_DEV_NO);
}

static void dev_start_recording(JNIEnv *env, jobject obj, jobject params)
{
}

static jobject dev_stop_recording(JNIEnv *env, jobject obj)
{
	return NULL;
}

static void dev_start_timeshifting(JNIEnv *env, jobject obj, jobject params)
{
}

static jobject dev_stop_timeshifting(JNIEnv *env, jobject obj)
{
	return NULL;
}

static void dev_start_playback(JNIEnv *env, jobject obj, jobject params)
{
}

static void dev_stop_playback(JNIEnv *env, jobject obj)
{
}

static void dev_fast_forward(JNIEnv *env, jobject obj, jint speed)
{
}

static void dev_fast_backward(JNIEnv *env, jobject obj, jint speed)
{
}

static void dev_pause(JNIEnv *env, jobject obj)
{
}

static void dev_resume(JNIEnv *env, jobject obj)
{
}

static void dev_seek_to(JNIEnv *env, jobject obj, jint pos)
{
}

static JNINativeMethod gMethods[] = {
	/* name, signature, funcPtr */
	{"native_device_init", "()V", (void*)dev_init},
	{"native_device_destroy", "()V", (void*)dev_destroy},
	{"native_set_input_source", "(I)V", (void*)dev_set_input_source},
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
	{"native_stop_dtv", "()V", (void*)dev_stop_dtv},
	{"native_start_recording", "(Lcom/amlogic/tvservice/TVDevice$DTVRecordParams;)V", (void*)dev_start_recording},
	{"native_stop_recording", "()Lcom/amlogic/tvservice/TVDevice$DTVRecordParams;", (void*)dev_stop_recording},
	{"native_start_timeshifting", "(Lcom/amlogic/tvservice/TVDevice$DTVRecordParams;)V", (void*)dev_start_timeshifting},
	{"native_stop_timeshifting", "()Lcom/amlogic/tvservice/TVDevice$DTVRecordParams;", (void*)dev_stop_timeshifting},
	{"native_start_playback", "(Lcom/amlogic/tvservice/TVDevice$DTVRecordParams;)V", (void*)dev_start_playback},
	{"native_stop_playback", "()V", (void*)dev_stop_playback},
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

	clazz = env->FindClass("com/amlogic/tvservice/TVDevice");
	if(clazz == NULL){
		LOGE("FindClass com/amlogic/tvservice/TVDevice failed");
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
	gChanParamsClass  = env->FindClass("com/amlogic/tvutil/TVChannelParams");
	gChanParamsClass  = (jclass)env->NewGlobalRef((jobject)gChanParamsClass);
	gChanParamsModeID = env->GetFieldID(gChanParamsClass, "mode", "I");
	gChanParamsFreqID = env->GetFieldID(gChanParamsClass, "frequency", "I");
	gChanParamsModID  = env->GetFieldID(gChanParamsClass, "modulation", "I");
	gChanParamsSymID  = env->GetFieldID(gChanParamsClass, "symbolRate", "I");
	gChanParamsBWID   = env->GetFieldID(gChanParamsClass, "bandwidth", "I");
	gChanParamsInitID = env->GetMethodID(gChanParamsClass, "<init>", "(I)V");

	LOGI("load jnitvdevice ok");
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

