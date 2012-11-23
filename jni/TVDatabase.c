#include <am_db.h>
#include <jni.h>
#include <android/log.h>

#define LOG_TAG "JNIDVBDATABASE"
#define log_info(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)
#define log_error(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)


static void db_setup(JNIEnv *env, jstring name, jboolean create)
{
	const char *str;

	str = (*env)->GetStringUTFChars(env, name, NULL);
	if(str){

		log_info("setup database");
		AM_DB_Setup((char*)str, NULL);

		if(create){
			sqlite3 *handle;

			log_info("create tables");
			AM_DB_GetHandle(&handle);
			AM_DB_CreateTables(handle);
			sqlite3_close(handle);
		}

		(*env)->ReleaseStringUTFChars(env, name, str);
	}
}

static void db_unsetup(JNIEnv *env)
{
	log_info("unsetup database");
	AM_DB_UnSetup();
}

static JNINativeMethod db_methods[] = 
{
	/* name, signature, funcPtr */
	{"native_db_setup", "(Ljava/lang/String;Z)V", (void*)db_setup},
	{"native_db_unsetup", "()V", (void*)db_unsetup},
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




