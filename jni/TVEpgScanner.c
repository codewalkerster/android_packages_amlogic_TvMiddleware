#include <am_db.h>
#include <am_epg.h>
#include <jni.h>
#include <android/log.h>

#define LOG_TAG "JNIDVBEPG"
#define log_info(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)
#define log_error(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

/* EPG notify events*/
#define EVENT_PF_EIT_END             1
#define EVENT_SCH_EIT_END            2
#define EVENT_PMT_END                3
#define EVENT_SDT_END                4
#define EVENT_TDT_END                5
#define EVENT_NIT_END                6
#define EVENT_PROGRAM_AV_UPDATE      7
#define EVENT_PROGRAM_NAME_UPDATE    8
#define EVENT_PROGRAM_EVENTS_UPDATE  9
#define EVENT_TS_UPDATE              10 

static JavaVM   *gJavaVM = NULL;
static jclass    gEventClass;
static jmethodID gEventInitID;
static jmethodID gOnEventID;
static jfieldID  gHandleID;

typedef struct{
	int dmx_id;
    int fend_id;
	int handle;
	jobject obj;
}EPGData;

typedef struct {
	int type;
	int channelID;
	int programID;
	int dvbOrigNetID;
	int dvbTSID;
	int dvbServiceID;
	long time;
	int dvbVersion;
}EPGEventData;

static void epg_on_event(jobject obj, EPGEventData *evt_data)
{
	JNIEnv *env;
	int ret;
	int attached = 0;
	
	ret = (*gJavaVM)->GetEnv(gJavaVM, (void**) &env, JNI_VERSION_1_4);
    if(ret <0) {
        ret = (*gJavaVM)->AttachCurrentThread(gJavaVM,&env,NULL);
        if(ret <0) {
            log_error("callback handler:failed to attach current thread");
            return;
        }
        attached = 1;
    }
    jobject event = (*env)->NewObject(env, gEventClass, gEventInitID, obj, evt_data->type);
    (*env)->SetIntField(env,event,(*env)->GetFieldID(env, gEventClass, "channelID", "I"), evt_data->channelID);
    (*env)->SetIntField(env,event,(*env)->GetFieldID(env, gEventClass, "programID", "I"), evt_data->programID);
    (*env)->SetIntField(env,event,(*env)->GetFieldID(env, gEventClass, "dvbOrigNetID", "I"), evt_data->dvbOrigNetID);
    (*env)->SetIntField(env,event,(*env)->GetFieldID(env, gEventClass, "dvbTSID", "I"), evt_data->dvbTSID);
    (*env)->SetIntField(env,event,(*env)->GetFieldID(env, gEventClass, "dvbServiceID", "I"), evt_data->dvbServiceID);
    (*env)->SetLongField(env,event,(*env)->GetFieldID(env, gEventClass, "time", "J"), evt_data->time);
    (*env)->SetIntField(env,event,(*env)->GetFieldID(env, gEventClass, "dvbVersion", "I"), evt_data->dvbVersion);
	(*env)->CallVoidMethod(env, obj, gOnEventID, event);

	 if(attached) {
        (*gJavaVM)->DetachCurrentThread(gJavaVM);
    }
}

static void epg_evt_callback(int dev_no, int event_type, void *param, void *user_data)
{
	EPGData *priv_data;
	EPGEventData edata;
	
	AM_EPG_GetUserData(dev_no, (void**)&priv_data);
	if(!priv_data)
		return;
	memset(&edata, 0, sizeof(edata));
	switch(event_type){
		case AM_EPG_EVT_NEW_TDT:
		case AM_EPG_EVT_NEW_STT:
		{
			int utc_time;
			
			AM_EPG_GetUTCTime(&utc_time);
			edata.type = EVENT_TDT_END;
			edata.time = (long)utc_time;
			epg_on_event(priv_data->obj, &edata);
		}
			break;
		case AM_EPG_EVT_UPDATE_EVENTS:
			edata.type = EVENT_PROGRAM_EVENTS_UPDATE;
			edata.programID = (int)param;
			epg_on_event(priv_data->obj, &edata);
			break;
		case AM_EPG_EVT_UPDATE_PROGRAM_AV:
			edata.type = EVENT_PROGRAM_AV_UPDATE;
			edata.programID = (int)param;
			epg_on_event(priv_data->obj, &edata);
			break;
		case AM_EPG_EVT_UPDATE_PROGRAM_NAME:
			edata.type = EVENT_PROGRAM_NAME_UPDATE;
			edata.programID = (int)param;
			epg_on_event(priv_data->obj, &edata);
			break;
		case AM_EPG_EVT_UPDATE_TS:
			edata.type = EVENT_TS_UPDATE;
			edata.channelID = (int)param;
			epg_on_event(priv_data->obj, &edata);
			break;
		default:
			break;
	}
}

static void epg_create(JNIEnv* env, jobject obj, jint fend_id, jint dmx_id, jint src)
{
	AM_EPG_CreatePara_t para;
	EPGData *data;
	AM_ErrorCode_t ret;
	AM_FEND_OpenPara_t fend_para;
    AM_DMX_OpenPara_t dmx_para;

	data = (EPGData*)malloc(sizeof(EPGData));
	if(!data){
		log_error("malloc failed");
		return;
	}
	data->dmx_id = dmx_id;
    log_info("Opening demux%d ...", dmx_id);
    memset(&dmx_para, 0, sizeof(dmx_para));
    AM_DMX_Open(dmx_id, &dmx_para);
    
	para.fend_dev = fend_id;
	para.dmx_dev  = dmx_id;
	para.source   = src;
	para.hdb      = NULL;

	ret = AM_EPG_Create(&para, &data->handle);
	if(ret != AM_SUCCESS){
		free(data);
		log_error("AM_EPG_Create failed");
		return;
	}
	
	data->obj = (*env)->NewGlobalRef(env,obj);

	(*env)->SetIntField(env, obj, gHandleID, (jint)data);

	/*注册EIT通知事件*/
	AM_EVT_Subscribe(data->handle,AM_EPG_EVT_NEW_TDT,epg_evt_callback,NULL);
	AM_EVT_Subscribe(data->handle,AM_EPG_EVT_NEW_STT,epg_evt_callback,NULL);
	AM_EVT_Subscribe(data->handle,AM_EPG_EVT_UPDATE_EVENTS,epg_evt_callback,NULL);
	AM_EVT_Subscribe(data->handle,AM_EPG_EVT_UPDATE_PROGRAM_AV,epg_evt_callback,NULL);
	AM_EVT_Subscribe(data->handle,AM_EPG_EVT_UPDATE_PROGRAM_NAME,epg_evt_callback,NULL);
	AM_EVT_Subscribe(data->handle,AM_EPG_EVT_UPDATE_TS,epg_evt_callback,NULL);
	AM_EPG_SetUserData(data->handle, (void*)data);
}

static void epg_destroy(JNIEnv* env, jobject obj)
{
	EPGData *data;

	data = (EPGData*)(*env)->GetIntField(env, obj, gHandleID);

	/*反注册EIT通知事件*/
	if (data) {
		AM_EVT_Unsubscribe(data->handle,AM_EPG_EVT_NEW_TDT,epg_evt_callback,NULL);
		AM_EVT_Unsubscribe(data->handle,AM_EPG_EVT_NEW_STT,epg_evt_callback,NULL);
		AM_EVT_Unsubscribe(data->handle,AM_EPG_EVT_UPDATE_EVENTS,epg_evt_callback,NULL);
		AM_EVT_Unsubscribe(data->handle,AM_EPG_EVT_UPDATE_PROGRAM_AV,epg_evt_callback,NULL);
		AM_EVT_Unsubscribe(data->handle,AM_EPG_EVT_UPDATE_PROGRAM_NAME,epg_evt_callback,NULL);
		AM_EVT_Unsubscribe(data->handle,AM_EPG_EVT_UPDATE_TS,epg_evt_callback,NULL);
		AM_EPG_Destroy(data->handle);
		log_info("EPGScanner on demux%d sucessfully destroyed", data->dmx_id);
		log_info("Closing demux%d ...", data->dmx_id);
		AM_DMX_Close(data->dmx_id);
		if (data->obj)
			(*env)->DeleteGlobalRef(env, data->obj);
		free(data);
	}
}

static void epg_change_mode(JNIEnv* env, jobject obj, jint op, jint mode)
{
	EPGData *data;
	AM_ErrorCode_t ret;

	data = (EPGData*)(*env)->GetIntField(env, obj, gHandleID);

	ret = AM_EPG_ChangeMode(data->handle, op, mode);
	if(ret != AM_SUCCESS)
		log_error("AM_EPG_ChangeMode failed");
}

static void epg_monitor_service(JNIEnv* env, jobject obj, jint srv_id)
{
	EPGData *data;
	AM_ErrorCode_t ret;

	data = (EPGData*)(*env)->GetIntField(env, obj, gHandleID);

	ret = AM_EPG_MonitorService(data->handle, srv_id);
	if(ret != AM_SUCCESS)
		log_error("AM_EPG_MonitorService failed");
}

static void epg_set_dvb_text_coding(JNIEnv* env, jobject obj, jstring coding)
{
	const char *str = (*env)->GetStringUTFChars(env, coding, 0);

	if (str != NULL){
		if (!strcmp(str, "standard"))
			str = "";
		
		AM_SI_SetDefaultDVBTextCoding(str);
	
		(*env)->ReleaseStringUTFChars(env ,coding, str);
	}
}

static JNINativeMethod epg_methods[] = 
{
	/* name, signature, funcPtr */
	{"native_epg_create", "(III)V", (void*)epg_create},
	{"native_epg_destroy", "()V", (void*)epg_destroy},
	{"native_epg_change_mode", "(II)V", (void*)epg_change_mode},
	{"native_epg_monitor_service", "(I)V", (void*)epg_monitor_service},
	{"native_epg_set_dvb_text_coding", "(Ljava/lang/String;)V", (void*)epg_set_dvb_text_coding}
};

//JNIHelp.h ????
#ifndef NELEM
# define NELEM(x) ((int) (sizeof(x) / sizeof((x)[0])))
#endif
static int registerNativeMethods(JNIEnv* env, const char* className,
                                 const JNINativeMethod* methods, int numMethods)
{
	int rc;
	jclass clazz;

	clazz = (*env)->FindClass(env, className);

	if (clazz == NULL) 
		return -1;

	if ((rc = ((*env)->RegisterNatives(env, clazz, methods, numMethods))) < 0) 
		return -1;

	return 0;
}

JNIEXPORT jint
JNI_OnLoad(JavaVM* vm, void* reserved)
{
	JNIEnv* env = NULL;
	jclass clazz;

	if ((*vm)->GetEnv(vm, (void**) &env, JNI_VERSION_1_4) != JNI_OK) 
		return -1;
	
	if (registerNativeMethods(env, "com/amlogic/tvservice/TVEpgScanner", epg_methods, NELEM(epg_methods)) < 0)
		return -1;

	gJavaVM = vm;

	clazz = (*env)->FindClass(env, "com/amlogic/tvservice/TVEpgScanner");
	if(clazz == NULL){
		log_error("FindClass com/amlogic/tvservice/TVEpgScanner failed");
		return -1;
	}

	gOnEventID = (*env)->GetMethodID(env, clazz, "onEvent", "(Lcom/amlogic/tvservice/TVEpgScanner$Event;)V");
	gHandleID = (*env)->GetFieldID(env, clazz, "native_handle", "I");
	gEventClass       = (*env)->FindClass(env, "com/amlogic/tvservice/TVEpgScanner$Event");
	gEventClass       = (jclass)(*env)->NewGlobalRef(env, (jobject)gEventClass);
	gEventInitID      = (*env)->GetMethodID(env, gEventClass, "<init>", "(Lcom/amlogic/tvservice/TVEpgScanner;I)V");

	return JNI_VERSION_1_4;
}

