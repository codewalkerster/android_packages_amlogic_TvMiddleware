#include <am_db.h>
#include <jni.h>
#include <android/log.h>
#include <unistd.h>

#define LOG_TAG "JNIDVBDATABASE"
#define log_info(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)
#define log_error(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

/** To avoid compile error when it's not defined */
#ifndef sqlite3_set_sync_flag
#define sqlite3_set_sync_flag(_f) log_error("sqlite3_set_sync_flag is NOT IMPLEMENTED !!")
#endif

static void db_setup(JNIEnv *env, jobject obj, jstring name, jboolean create, jobject db)
{
	const char *str;

	str = (*env)->GetStringUTFChars(env, name, NULL);
	if(str){
		jfieldID native_handle_id;
		sqlite3 *handle;
		jclass objclass=(*env)->FindClass(env,"android/database/sqlite/SQLiteDatabase");

		native_handle_id = (*env)->GetFieldID(env, objclass, "mNativeHandle", "I");
		if(native_handle_id == 0){
			handle = NULL;
		}else{
			handle = (sqlite3*) (*env)->GetIntField(env, db, native_handle_id);
		}

		log_info("setup database %p", handle);
		AM_DB_Setup((char*)str, handle);

		if(create){
			log_info("create tables");
			AM_DB_GetHandle(&handle);
			AM_DB_CreateTables(handle);
		}

		sqlite3_set_sync_flag(0);

		(*env)->ReleaseStringUTFChars(env, name, str);
	}
}

static void db_unsetup(JNIEnv *env, jobject obj)
{
	log_info("unsetup database");
	AM_DB_UnSetup();
}

static void db_sync(JNIEnv *env, jobject obj)
{
	log_info("sync database");
	sync();
}

static JNINativeMethod db_methods[] = 
{
	/* name, signature, funcPtr */
	{"native_db_setup", "(Ljava/lang/String;ZLandroid/database/sqlite/SQLiteDatabase;)V", (void*)db_setup},
	{"native_db_unsetup", "()V", (void*)db_unsetup},
	{"native_db_sync", "()V", (void*)db_sync},
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




