#include <am_db.h>
#include <jni.h>
#include <android/log.h>

#define LOG_TAG "JNIDVBDATABASE"
#define log_info(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)
#define log_error(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)


static jint db_init(JNIEnv *env, jobject obj, jobject db)
{
	jfieldID native_handle;
	jclass objclass=(*env)->FindClass(env,"android/database/sqlite/SQLiteDatabase");
	int hdb;

	native_handle = (*env)->GetFieldID(env, objclass, "mNativeHandle", "I"); 
	if (native_handle == 0) 
		return -1; 
	hdb = (*env)->GetIntField(env, db, native_handle);

	AM_DB_CreateTables((sqlite3*)hdb);

	return hdb;
}

static JNINativeMethod db_methods[] = 
{
	/* name, signature, funcPtr */
	{"native_db_init", "(Landroid/database/sqlite/SQLiteDatabase;)I", (void*)db_init},
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

	if ((*vm)->GetEnv(vm, (void**) &env, JNI_VERSION_1_4) != JNI_OK) 
		return -1;
	
	if (registerNativeMethods(env, "com/amlogic/tvdataprovider/TVDatabase", db_methods, NELEM(db_methods)) < 0)
		return -1;

	return JNI_VERSION_1_4;
}




