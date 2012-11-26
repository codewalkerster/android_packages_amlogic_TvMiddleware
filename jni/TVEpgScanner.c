#include <am_db.h>
#include <am_epg.h>
#include <jni.h>
#include <android/log.h>

#define LOG_TAG "JNIDVBEPG"
#define log_info(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)
#define log_error(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

static JavaVM   *gJavaVM = NULL;
static jmethodID gOnEventID;
static jfieldID  gHandleID;

typedef struct{
	int handle;
}EPGData;

static void epg_evt_callback(int dev_no, int event_type, void *param, void *user_data)
{
	EPGData *priv_data;
	
	AM_EPG_GetUserData(dev_no, (void**)&priv_data);
	if(!priv_data)
		return;

#if 0
	switch(event_type){
		case AM_EPG_EVT_NEW_EIT:
			parse_new_eit(priv_data, (dvbpsi_eit_t*)param);
			break;
		case AM_EPG_EVT_NEW_TDT:
			break;
		case AM_EPG_EVT_NEW_SDT:
			update_services(priv_data, (dvbpsi_sdt_t*)param);
			break;
		case AM_EPG_EVT_EIT_UPDATE:
			break;
		case AM_EPG_EVT_NEW_STT:
			break;
		case AM_EPG_EVT_NEW_RRT:
			parse_new_rrt(priv_data, (rrt_section_info_t*)param);
			break;
		case AM_EPG_EVT_NEW_PSIP_EIT:
			parse_new_psip_eit(priv_data, (eit_section_info_t*)param);
			break;
	}
#endif
}

static void epg_create(JNIEnv* env, jobject obj, jint fend_id, jint dmx_id, jint src)
{
	AM_EPG_CreatePara_t para;
	EPGData *data;
	AM_ErrorCode_t ret;

	data = (EPGData*)malloc(sizeof(EPGData));
	if(!data){
		log_error("malloc failed");
		return;
	}

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

	(*env)->SetIntField(env, obj, gHandleID, (jint)data);

	/*注册EIT通知事件*/
	AM_EVT_Subscribe(data->handle,AM_EPG_EVT_NEW_EIT,epg_evt_callback,NULL);
	AM_EVT_Subscribe(data->handle,AM_EPG_EVT_NEW_TDT,epg_evt_callback,NULL);
	AM_EVT_Subscribe(data->handle,AM_EPG_EVT_EIT_UPDATE,epg_evt_callback,NULL);
	AM_EVT_Subscribe(data->handle,AM_EPG_EVT_NEW_PSIP_EIT,epg_evt_callback,NULL);
	AM_EVT_Subscribe(data->handle,AM_EPG_EVT_NEW_STT,epg_evt_callback,NULL);
	AM_EVT_Subscribe(data->handle,AM_EPG_EVT_NEW_RRT,epg_evt_callback,NULL);
	AM_EVT_Subscribe(data->handle,AM_EPG_EVT_NEW_SDT,epg_evt_callback,NULL);
	AM_EPG_SetUserData(data->handle, (void*)data);
}

static void epg_destroy(JNIEnv* env, jobject obj)
{
	EPGData *data;

	data = (EPGData*)(*env)->GetIntField(env, obj, gHandleID);

	AM_EPG_Destroy(data->handle);
	free(data);
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

static JNINativeMethod epg_methods[] = 
{
	/* name, signature, funcPtr */
	{"native_epg_create", "(III)V", (void*)epg_create},
	{"native_epg_destroy", "()V", (void*)epg_destroy},
	{"native_epg_change_mode", "(II)V", (void*)epg_change_mode},
	{"native_epg_monitor_service", "(I)V", (void*)epg_monitor_service}
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

	if (rc = ((*env)->RegisterNatives(env, clazz, methods, numMethods)) < 0) 
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

	return JNI_VERSION_1_4;
}

