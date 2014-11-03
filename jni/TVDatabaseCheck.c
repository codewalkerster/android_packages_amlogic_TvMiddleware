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

#define 	SQLITE_OK           0
#define  	CORRUPTION_ERROR  11
#define		FORMAT_OK				0
#define		MINOR_FORMAT_ERROR		1
#define 	MAJOR_FORMAT_ERROR		2
#define		FORMAT_ERROR			3

static jint db_check(JNIEnv *env, jobject obj)
{
	sqlite3 *pdb = NULL;
	char *errmsg;
	int rc = 0,bak_rc = 0,ret = -1;
	if(!access("/data/data/com.amlogic.tvservice/databases/dvb.db", F_OK))
	{
		if (sqlite3_open("/data/data/com.amlogic.tvservice/databases/dvb.db", &pdb) != SQLITE_OK)
		{
			log_info("db_check can't open db");
			rc = -1;
		}
		else
		{
			rc = sqlite3_exec(pdb, "select db_id from srv_table", NULL, NULL, &errmsg);
			log_info("db_check open handle:%p rc=%d, msg=%s", pdb, rc, errmsg);
			sqlite3_close(pdb);
			log_info("db_check close db");
		}
	}
	else
	{
		log_info("db_check db_file is not exisits");
		rc = -1;
	}




	sqlite3 *pdb_bak = NULL;
	if(!access("/data/data/com.amlogic.tvservice/databases/dvb.db.bak", F_OK))
	{
		if (sqlite3_open("/data/data/com.amlogic.tvservice/databases/dvb.db.bak", &pdb_bak) != SQLITE_OK)
		{
			log_info("@@db_check can't open db.bak");
			bak_rc = -1;
		}
		else
		{
			bak_rc = sqlite3_exec(pdb_bak, "select db_id from srv_table", NULL, NULL, &errmsg);
			log_info("@@db_check open bak_handle:%p rc=%d, msg=%s", pdb_bak, bak_rc, errmsg);
			sqlite3_close(pdb_bak);
		}
	}
	else
	{
		log_info("db_check db_bak_file is not exisits");
		bak_rc = -1;
	}



	if(rc == SQLITE_OK && bak_rc == SQLITE_OK)
	{
		ret = FORMAT_OK;
	}
	else if(rc == SQLITE_OK && bak_rc != SQLITE_OK)
	{
		ret = MINOR_FORMAT_ERROR;
	}
	else if(rc != SQLITE_OK && bak_rc == SQLITE_OK)
	{
		ret = MAJOR_FORMAT_ERROR;
	}
	else if(rc != SQLITE_OK && bak_rc != SQLITE_OK)
	{
		ret = FORMAT_ERROR;
	}
	log_info("db_check return = %d, rc=%d, bak_rc=%d", ret, rc, bak_rc);
	return ret;
}

static JNINativeMethod db_methods[] =
{
	{"native_db_check", "()I", (void*)db_check},
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

    if (registerNativeMethods(env, "com/amlogic/tvdataprovider/TVDataProvider", db_methods, NELEM(db_methods)) < 0)
        return -1;

    return JNI_VERSION_1_4;
}




