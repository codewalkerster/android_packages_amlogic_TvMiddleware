#include <am_db.h>
#include <am_epg.h>
#include <jni.h>
#include <android/log.h>

#define LOG_TAG "JNIDVBEPG"
#define log_info(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)
#define log_error(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

static JavaVM   *gJavaVM = NULL;
static jmethodID gUpdateID;
static jfieldID  gHandleID;

static void epg_create(JNIEnv* env, jobject obj, jint fend_id, jint dmx_id, jint src)
{
	AM_EPG_CreatePara_t para;
	int handle;
	AM_ErrorCode_t ret;

	para.fend_dev = fend_id;
	para.dmx_dev  = dmx_id;
	para.source   = src;
	para.hdb      = NULL;

	ret = AM_EPG_Create(&para, &handle);
	if(ret != AM_SUCCESS)
		log_error("AM_EPG_Create failed");

	(*env)->SetIntField(env, obj, gHandleID, (jint)handle);
}

static void epg_destroy(JNIEnv* env, jobject obj)
{
	int handle;

	handle = (*env)->GetIntField(env, obj, gHandleID);

	AM_EPG_Destroy(handle);
}

static void epg_change_mode(JNIEnv* env, jobject obj, jint op, jint mode)
{
	int handle;
	AM_ErrorCode_t ret;

	handle = (*env)->GetIntField(env, obj, gHandleID);

	ret = AM_EPG_ChangeMode(handle, op, mode);
	if(ret != AM_SUCCESS)
		log_error("AM_EPG_ChangeMode failed");
}

static void epg_monitor_service(JNIEnv* env, jobject obj, jint srv_id)
{
	int handle;
	AM_ErrorCode_t ret;

	handle = (*env)->GetIntField(env, obj, gHandleID);

	ret = AM_EPG_MonitorService(handle, srv_id);
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

	gUpdateID = (*env)->GetMethodID(env, clazz, "update", "()V");
	gHandleID = (*env)->GetFieldID(env, clazz, "native_handle", "I");

	return JNI_VERSION_1_4;
}




