#include <stdio.h>
#include <string.h>

#include <jni.h>
#include <android/log.h>
#include <cutils/properties.h>

#include <am_debug.h>
#include "am_time.h"

#include "am_dmx.h"
#include "am_upd.h"
#include "am_si.h"

extern "C" {

#define LOG_TAG "JNIDVBUPD"
#define log_info(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)
#define log_error(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

static JavaVM   *gJavaVM = NULL;
static jclass    gEventClass;
static jmethodID gEventInitID;
static jmethodID gOnEventID;
static jfieldID  gMonHandleID;
static jfieldID  gDLHandleID;

 

typedef struct{
	int dmx_id;
	AM_TSUPD_MonHandle_t handle;
	jobject obj;

	unsigned int oui;
	unsigned int hw_ver;
	unsigned char serial[16];

	unsigned char sw_ver[32];
}UPDData_t;

typedef struct {
	int type;
	int param1;
	int param2;
	char *msg;

	unsigned char sw_ver[32];
	unsigned char sw_ver_new[32];

	unsigned int	 dl_frequency;						  /*!< frequency */
	unsigned char 	 dl_fec_outer;						  /*!< FEC outer */
	unsigned char 	 dl_modulation_type; 				  /*!< modulation type */
	unsigned int	 dl_symbol_rate; 					  /*!< symbol rate */
	unsigned char 	 dl_fec_inner;						  /*!< FEC inner */

	unsigned short download_pid;
	unsigned char download_tableid;

	unsigned int control;
}UPDEventData_t;

struct code_s{
	char name[16];
	int code;
}
OUI[] = {
	{"amlogic", 0x01}
},
HW[] = {
	{"K200", 0x01}
};

#define FUNC_get_code(what) \
static int get_##what##_code(char *name) \
{ \
	unsigned int i; \
	for(i=0; i<sizeof(what)/sizeof(what[0]); i++) \
		if(!strcmp(what[i].name, name)) \
			return what[i].code; \
	return 0; \
}
#define get_code(w, name)\
	get_##w##_code(name)


FUNC_get_code(OUI)
FUNC_get_code(HW)

#define NoThInG \
	{}

enum upd_event{
  EVENT_UPDATE_NOTFOUND	    = 0,
  EVENT_UPDATE_TIMEOUT		= 1,
  EVENT_UPDATE_FOUND	    = 2,
  EVENT_UPDATE_DL_NOTFOUND	= 3,
  EVENT_UPDATE_DL_TIMEOUT	= 4,
  EVENT_UPDATE_DL_PROGRESS	= 5,
  EVENT_UPDATE_DL_DONE      = 6
};


UPDData_t UPDData;


static void upd_event(jobject obj, UPDEventData_t *evt_data)
{
	JNIEnv *env;
	int ret;
	int attached = 0;

	ret = gJavaVM->GetEnv((void**) &env, JNI_VERSION_1_4);
    if(ret <0) {
        ret = gJavaVM->AttachCurrentThread(&env,NULL);
        if(ret <0) {
            log_error("callback handler:failed to attach current thread");
            return;
        }
        attached = 1;
    }

	log_info(">>evt: type[%d] param1[%d] param2[%d] control[%d] pid[%d] msg[%s]",
			evt_data->type,
			evt_data->param1,
			evt_data->param2,
			evt_data->control,
			evt_data->download_pid,
			evt_data->msg? evt_data->msg:"null");
    jobject event = env->NewObject(gEventClass, gEventInitID, obj, evt_data->type);
    env->SetIntField(event,env->GetFieldID( gEventClass, "type", "I"), evt_data->type);
    env->SetIntField(event,env->GetFieldID( gEventClass, "param1", "I"), evt_data->param1);
    env->SetIntField(event,env->GetFieldID( gEventClass, "param2", "I"), evt_data->param2);
    env->SetIntField(event,env->GetFieldID( gEventClass, "control", "I"), evt_data->control);
    env->SetObjectField(event,env->GetFieldID( gEventClass, "sw_ver", "Ljava/lang/String;"),
				env->NewStringUTF((char*)evt_data->sw_ver));

	if(evt_data->type==EVENT_UPDATE_FOUND) {
	    env->SetObjectField(event,env->GetFieldID( gEventClass, "sw_ver_new", "Ljava/lang/String;"),
							env->NewStringUTF((char*)evt_data->sw_ver_new));
	    env->SetIntField(event,env->GetFieldID( gEventClass, "dl_frequency", "I"), evt_data->dl_frequency);
	    env->SetIntField(event,env->GetFieldID( gEventClass, "dl_fec_outer", "I"), evt_data->dl_fec_outer);
	    env->SetIntField(event,env->GetFieldID( gEventClass, "dl_modulation_type", "I"), evt_data->dl_modulation_type);
	    env->SetIntField(event,env->GetFieldID( gEventClass, "dl_symbol_rate", "I"), evt_data->dl_symbol_rate);
	    env->SetIntField(event,env->GetFieldID( gEventClass, "dl_fec_inner", "I"), evt_data->dl_fec_inner);
	    env->SetIntField(event,env->GetFieldID( gEventClass, "download_pid", "I"), evt_data->download_pid);
	    env->SetIntField(event,env->GetFieldID( gEventClass, "download_tableid", "I"), evt_data->download_tableid);
	    env->SetObjectField(event,
					env->GetFieldID( gEventClass, "msg", "Ljava/lang/String;"),
					evt_data->msg[0]? env->NewStringUTF(evt_data->msg) : NULL);
	}

	env->CallVoidMethod(obj, gOnEventID, event);

	 if(attached) {
        gJavaVM->DetachCurrentThread();
    }
}


static int check_oui(unsigned int oui_code)
{
	return (UPDData.oui == oui_code)? 1 : 0;
}
static int check_hw(unsigned int hw)
{
	return (UPDData.hw_ver == hw)? 1 : 0;
}
static int check_sw(const unsigned char ver[8])
{
	unsigned int i,j;

	/*
		sw_ver :a.b.c.c1-x.y.z.z1
		0.0.0.0 - ignore this field
	*/
	char *tmp = strchr((char*)UPDData.sw_ver, '-');
	if(!tmp)
		return 1;

	char *pver1=(char*)UPDData.sw_ver,
		*pver2=tmp+1;
	unsigned int x,y,z,z1;

	i = (ver[0]<<24)+(ver[1]<<16)+(ver[2]<<8)+ver[3];

	x=y=z=z1=0;
	sscanf(pver1, "%d.%d.%d.%d", &x, &y, &z, &z1);
	j = (x<<24)+(y<<16)+(z<<8)+z1;
	if(i&&(i>j))
		return 1;

	i = (ver[4]<<24)+(ver[5]<<16)+(ver[6]<<8)+ver[7];

	x=y=z=z1=0;
	sscanf(pver2, "%d.%d.%d.%d", &x, &y, &z, &z1);
	j = (x<<24)+(y<<16)+(z<<8)+z1;
	if(i&&(i>j))
		return 1;

	return 0;
}
static int check_serial(unsigned char serial_start[16], unsigned char serial_end[16])
{
	int i;

	//2 serial_end=0 means ignore serial
	unsigned int tmp=0;
	for(i=0; i<16; i++)
		tmp+=serial_end[i];
	if(tmp==0)
		return 1;

	for(i=0; i<16; i++) {
		if(UPDData.serial[i]<serial_start[i])
			return 0;
	}
	for(i=0; i<16; i++) {
		if(UPDData.serial[i]>serial_end[i])
			return 0;
	}

	return 1;
}

static int grab_board_info(UPDData_t *upd, const char *sw_ver)
{
	/*
	[ro.hardware]: [amlogic]
	[ro.product.model]: [K200]
	[ro.serialno]: [12345678900]

	[ro.build.product]: [k200]
	[ro.build.date.utc]: [1412825313]
	[ro.build.version.release]: [4.4.2]
	*/
    char value[PROPERTY_VALUE_MAX]={0};

	log_info("----== Board info ==-----");

	//2 get oui code
    if(property_get("ro.hardware",value,NULL) > 0)
        upd->oui = get_code(OUI, value);
	log_info("OUI: %d (%s)", upd->oui, value);

	//2 get hardware version
    if(property_get("ro.product.model",value,NULL) > 0)
        upd->hw_ver = get_code(HW, value);
	log_info("HW: %d (%s)", upd->hw_ver, value);

	//2 get software version
	/*sw_ver :a.b.c-x.y.z*/
	strncpy((char*)upd->sw_ver, sw_ver, 31);
	log_info("SW: %s", upd->sw_ver);

	//2 get serial code
    if(property_get("ro.serialno",value,NULL) > 0) {
		int len=strlen(value);
		int i;
		for(i=0; i<len; i++)
	        upd->serial[16-1-i] = value[len-1-i]-'0';
    }
	log_info("SN: xxx%d%d%d%d (%s)", upd->serial[12], upd->serial[13], upd->serial[14], upd->serial[15], value);

	return 0;
}

static int upd_check_descriptor(unsigned char* p_desc)
{
/*
Linkage_Descriptor() {
  Descriptor_tag                          8                 uimsbf            0xA1
  Descriptor_length                      8                uimsbf
  OUI_data_length                      16                uimsbf
  for(I=0;I<N;I++) {
    OUI                                     16                uimsbf
    Selector_length                     16                uimsbf
    For(i=0;i<N;i++){
      Update_control                    16               uimsbf
      Hardware_version                  8               uimsbf
      Software_version                 64               uimsbf
      SerialNo_start                    128               uimsbf
      SerialNo_end                      128               uimsbf              0 for ignore serialno
      Delivery_system_descriptor
      Download_pid                      16               uimsbf
      Download_tableid                  8                uimsbf
    }
    Private_length                      16                uimsbf
    For(i=0;i<N;i++){
      Private_data_type                 8                uimsbf
    }
  }
  Private_length				 16			uimsbf
  For(i=0;i<N;i++){
    Private_data_type                   8                uimsbf
  }
}
Update_control:
  bit 0:1  -  0-manual, 1-force
  bit 2:15 -  reserved

Delivery_system_descriptor() {
  descriptor_tag                   8       uimsbf          0x44
  descriptor_length               8       uimsbf          0x0B
  frequency                       32         bslbf
  reserved_future_use         12         bslbf
  FEC_outer                        4         bslbf
  modulation                       8         bslbf
  symbol_rate                    28        bslbf
  FEC_inner                        4         bslbf
}

*/

#define B2S(b1, b2)         (((b1)<<8)|(b2))
#define B2I(b1, b2, b3, b4) (((b1)<<24)|((b2)<<16)|((b3)<<8)|(b4))

#define UPDATE_DESCRIPTOR_TAG 0xA1

    unsigned char desc_tag = p_desc[0];

    if(desc_tag == UPDATE_DESCRIPTOR_TAG)
    {
		unsigned short desc_len = p_desc[1];
		unsigned short oui_len = B2S(p_desc[2],p_desc[3]);
		unsigned char *poui = p_desc+4;

		log_info("ouilen:%d\n", oui_len);

		log_info("UPDATE Descriptor found.\n");

		while(oui_len) {

			UPDEventData_t *evt = NULL;
			unsigned short sel_len = B2S(poui[2],poui[3]);
			unsigned char *psel = poui+4;
			unsigned char *psel_end = psel+sel_len;
			unsigned short priv_len;

			log_info("check oui: %x",B2S(poui[0],poui[1]));

			//2 check oui
			if(!check_oui(B2S(poui[0],poui[1])))
				goto oui_end;

			while(psel < psel_end) {

				unsigned short deli_len = psel[44];
				unsigned short update_control = B2S(psel[0],psel[1]);
				dvbpsi_descriptor_t *desc;
				dvbpsi_cable_delivery_dr_t *cable;

				//2 check hareware
				unsigned char hw_version = psel[2];

				log_info("check hw: %x",hw_version);
				if(!check_hw(hw_version))
					goto sel_end;

				//2 check serialno
				unsigned char serial_start[16],
								serial_end[16];
				int i;
				for(i=0; i<16; i++)
					serial_start[i] = psel[11+i];
				for(i=0; i<16; i++)
					serial_end[i] = psel[27+i];
				log_info("check_serial");
				if(!check_serial(serial_start, serial_end))
					goto sel_end;

				//2 check software
				log_info("check_soft: %x.%x.%x.%x-%x.%x.%x.%x",
								psel[3], psel[4], psel[5], psel[6],
								psel[7], psel[8], psel[9], psel[10] );
				if(!check_sw(&psel[3]))
					goto sel_end;

				if(evt){
					log_error("more than one update found, previous update will be lost.");
					free(evt);
				}

				evt = (UPDEventData_t*)malloc(sizeof(UPDEventData_t));
				if(!evt) {
					log_error("update evt no memory!");
					goto sel_end;
				}

				//2 update found!
				memset(evt, 0, sizeof(UPDEventData_t));
				strncpy((char*)evt->sw_ver, (char*)UPDData.sw_ver, 31);
				sprintf((char*)evt->sw_ver_new, "%d.%d.%d-%d.%d.%d",
						psel[3], psel[4], psel[5],
						psel[7], psel[8], psel[9]);

				desc = dvbpsi_NewDescriptor(psel[43], psel[44], &psel[45]);
				cable=dvbpsi_DecodeCableDeliveryDr(desc);
				evt->dl_frequency = cable->i_frequency;
				evt->dl_fec_outer = cable->i_fec_outer;
				evt->dl_modulation_type = cable->i_modulation_type;
				evt->dl_symbol_rate = cable->i_symbol_rate;
				evt->dl_fec_inner = cable->i_fec_inner;
				dvbpsi_DeleteDescriptors(desc);

				log_info("Delivery: freq[%d].mod[%d]", evt->dl_frequency, evt->dl_modulation_type);

				evt->download_pid = ((psel[56]<<8) | psel[57]) & 0x1fff;
				evt->download_tableid = psel[58];

				evt->control = update_control;
				log_info("Download pid[%d].tid[%d], control[%x]",
								evt->download_pid, evt->download_tableid, evt->control);

sel_end:
				psel += 59;
			}

oui_end:
			priv_len = B2S(psel_end[0], psel_end[1]);
			if(evt) {
				evt->msg = priv_len? (char*)&psel[2] : NULL;
				evt->type = EVENT_UPDATE_FOUND;
				upd_event(UPDData.obj, evt);
				free(evt);
				evt = NULL;
			}

			log_info("sellen %d, priv_len:%d", sel_len, priv_len);
			oui_len -= (sel_len+priv_len+6);
			poui += (sel_len+priv_len+6);
		}

		log_info("oui end");
	}

	return 0;
}

static int monitor_callback(unsigned char* p_nit, unsigned int len, void *user)
{
	unsigned int network_des_len = 0, desc_len = 0, loop_count = 0;
	unsigned char desc_tag = 0;
	unsigned char *loop_header = NULL;

	if(!len)
	{
		//Timeout
		UPDEventData_t evt;
		memset(&evt, 0, sizeof(evt));
		evt.type = EVENT_UPDATE_TIMEOUT;
		upd_event(UPDData.obj, &evt);
		return 0;
	}

	network_des_len = (p_nit[8]&0x0f)<<8 | p_nit[9];
	log_info("net_des_len %d\n", network_des_len);

	loop_header = p_nit + 10;
	while (network_des_len - loop_count)
	{
		desc_tag = loop_header[0];
		desc_len = loop_header[1];

		log_info("des[%x] len[%d]\n", desc_tag, desc_len);
		if(desc_tag == 0xA1)
			upd_check_descriptor(loop_header);

		loop_count += (desc_len + 2);
		loop_header += (desc_len + 2);
	}

	return 0;
}

static int upd_startMonitor(JNIEnv* env, jobject obj, jint dmx_id, jstring sw_ver)
{
	AM_ErrorCode_t ret;
	AM_TSUPD_OpenMonitorParam_t mopenpara;
	AM_TSUPD_MonitorParam_t mpara;

	memset(&UPDData, 0, sizeof(UPDData_t));

	//2 grab board info
	const char* str;
	str = env->GetStringUTFChars(sw_ver, NULL);
	char *tmp=strdup(str);
	grab_board_info(&UPDData, tmp);
	if(str) {
		env->ReleaseStringUTFChars(sw_ver, str);
		free(tmp);
	}

	memset(&mopenpara, 0 , sizeof(mopenpara));
	mopenpara.dmx  = dmx_id;
	ret = AM_TSUPD_OpenMonitor(&mopenpara, &UPDData.handle);
	log_info("AM_TSUPD_OpenMonitor ret[%x]", ret);
	if(ret != AM_SUCCESS){
		log_error("AM_TSUPD_OpenMonitor fail.");
		return -1;
	}
	UPDData.dmx_id = dmx_id;

	memset(&mpara, 0, sizeof(mpara));
	mpara.callback = monitor_callback;
	mpara.callback_args = &UPDData;
	mpara.use_ext_nit = 0;
	ret = AM_TSUPD_StartMonitor(UPDData.handle, &mpara);
	log_info("AM_TSUPD_StartMonitor ret[%x]", ret);
	if(ret != AM_SUCCESS){
		log_error("AM_TSUPD_StartMonitor fail.");
		ret=AM_TSUPD_CloseMonitor(UPDData.handle);
		log_error("TSUPD_CloseMonitor ret[%d]", ret);
		return -1;
	}

	UPDData.obj = env->NewGlobalRef(obj);

	env->SetLongField( obj, gMonHandleID, (jlong)&UPDData);

	log_info("monitor start.");
	return 0;
}

static int upd_stopMonitor(JNIEnv* env, jobject obj)
{
	AM_ErrorCode_t ret;
	UPDData_t *data;

	data = (UPDData_t*)env->GetLongField(obj, gMonHandleID);

	if (data==&UPDData) {
		ret=AM_TSUPD_StopMonitor(data->handle);
		log_info("TSUPD_StopMonitor ret[%d]", ret);
		ret=AM_TSUPD_CloseMonitor(data->handle);
		log_info("TSUPD_CloseMonitor ret[%d]", ret);

		env->SetLongField( obj, gMonHandleID, 0);

		if (data->obj)
			env->DeleteGlobalRef(data->obj);

		return 0;
	}
	return -1;
}



typedef struct dpt_s dpt_t;

typedef struct sargs_s{
	int slot;
	dpt_t *dpt;
}sargs_t;

struct dpt_s{
	pthread_mutex_t     lock;
	pthread_cond_t      cond;
	pthread_t          	thread;
	int                 quit;

	int dmx;
	int pid;
	int tid;

#define MAX_PART_SUPPORT 256
	unsigned int last_part;

	struct {
		unsigned int stat;
#define slot_idle    0
#define slot_started 1
#define slot_timeout 2
#define slot_complete 3
#define slot_done    4

		AM_TSUPD_DlHandle_t did;

		sargs_t sargs;
	}part_slot[MAX_PART_SUPPORT];
	pthread_mutex_t    slot_lock;

#define MAX_DP 5

	FILE *fp;
	unsigned int part_size_max;

	unsigned int total_size;

	unsigned int timeout;

	unsigned int dl_size;

	char *store;

	jobject obj;

};

static int dl_part_callback(unsigned char *pdata, unsigned int len, void *user)
{
	sargs_t *sargs = (sargs_t*)user;

	if(!len)
		log_info("DL (%x) timeout\n", (int)(long)user);
/*	else
		log_info("DATA[%d](%x): %02x %02x %02x %02x %02x %02x %02x %02x %02x %02x\n",
			len, (int)user,
			pdata[0], pdata[1], pdata[2], pdata[3], pdata[4], pdata[5], pdata[6], pdata[7], pdata[8], pdata[9]);
*/
	if(!sargs) {
		log_error("dl_part FATAL ERROR.");
		return 0;
	}

	pthread_mutex_lock(&sargs->dpt->slot_lock);

	sargs->dpt->part_slot[(int)sargs->slot].stat = len? slot_complete : slot_timeout;

	pthread_mutex_unlock(&sargs->dpt->slot_lock);

	pthread_cond_signal(&sargs->dpt->cond);

	return 0;
}

AM_TSUPD_DlHandle_t dl_part_start(int dmx, int pid, int tid, int ext, int timeout, void *args)
{
	AM_TSUPD_DlHandle_t did;
	AM_ErrorCode_t err = AM_SUCCESS;
	AM_TSUPD_OpenDownloaderParam_t openpara;
	AM_TSUPD_DownloaderParam_t dlpara;

	log_info("dl_part_start %x %x %x\n", pid, tid, ext);

	memset(&openpara, 0 , sizeof(openpara));

	openpara.dmx = dmx;
	err = AM_TSUPD_OpenDownloader(&openpara, &did);
	log_info("open did(%p) err[%x]\n", did, err);
	if(err)
		return 0;

	memset(&dlpara, 0, sizeof(dlpara));

	dlpara.callback = dl_part_callback;
	dlpara.callback_args = args;
	dlpara.pid = pid;
	dlpara.tableid = tid;
	dlpara.ext = ext;
	dlpara.timeout = timeout;
	err = AM_TSUPD_StartDownloader(did, &dlpara);
	log_info("start downloader(%p) err[%x]\n", did, err);
	if(err) {
		AM_TSUPD_CloseDownloader(did);
		return 0;
	}

	return did;
}


int dl_part_stop(AM_TSUPD_DlHandle_t did)
{
	AM_ErrorCode_t err = AM_SUCCESS;

	err = AM_TSUPD_StopDownloader(did);
	log_info("stop download(%p) err[%x]\n", did, err);


	err = AM_TSUPD_CloseDownloader(did);
	log_info("close download(%p) err[%x]\n", did, err);

	return 0;
}

int part_start_slots(dpt_t *dpt, int num, int part_timeout)
{
	unsigned int i;
	int running=0;
	int start=0;
	int need_start=0;

	pthread_mutex_lock(&dpt->slot_lock);

	for(i=0; i<(dpt->last_part+1); i++) {
		if(dpt->part_slot[i].stat==slot_started)
			running++;
	}

	need_start = (num<0)? MAX_DP-running : ((running>=num)? 0 : (num-running));

	for(i=0; i<(dpt->last_part+1) && (start<need_start); i++) {
		if(dpt->part_slot[i].stat==slot_idle) {
			dpt->part_slot[i].sargs.dpt = dpt;
			dpt->part_slot[i].sargs.slot = i;
			dpt->part_slot[i].did = dl_part_start(dpt->dmx, dpt->pid, dpt->tid, i, part_timeout, &dpt->part_slot[i].sargs);
			if(dpt->part_slot[i].did) {
				dpt->part_slot[i].stat = slot_started;
				start++;
			}
		}
	}

	pthread_mutex_unlock(&dpt->slot_lock);

	return start+running;
}

int part_stop_slots(dpt_t *dpt)
{
	unsigned int i;
	int cnt=0;

	pthread_mutex_lock(&dpt->slot_lock);

	for(i=0; i<(dpt->last_part+1); i++) {
		if(dpt->part_slot[i].stat==slot_started) {
			dl_part_stop(dpt->part_slot[i].did);
			dpt->part_slot[i].stat = 0;
			cnt++;
		}
	}

	pthread_mutex_unlock(&dpt->slot_lock);

	return cnt;
}



static int save_data(dpt_t *dpt, unsigned char *pdata, unsigned int size)
{
	unsigned int part_no = (pdata[0]<<8) | pdata[1];
	unsigned int part_last=(pdata[2]<<8) | pdata[3];

	if(part_no==0)
	{
		unsigned int valid_size = (pdata[4]<<24)|(pdata[5]<<16)|(pdata[6]<<8)|pdata[7];
		dpt->total_size = valid_size;
	}

	unsigned int offset = (part_no==0)? 0 :
						(dpt->part_size_max-4)+((part_no-1)*dpt->part_size_max);


	pdata = (part_no==0)? (pdata+8) : (pdata+4);
	size = (part_no==0)? (size-8) : (size-4);

	if((offset+size) > dpt->total_size)
		size -= ((offset+size) - dpt->total_size);

	log_info("part %d of %d arrive, save data(%d) @ %d\n", part_no, part_last, size, offset);

	dpt->dl_size += size;

	UPDEventData_t evt;
	memset(&evt, 0, sizeof(evt));
	evt.type = EVENT_UPDATE_DL_PROGRESS;
	evt.param1 = dpt->dl_size*100/dpt->total_size;
	upd_event(dpt->obj, &evt);

	if(dpt->fp){
		int ret;
		fseek(dpt->fp, offset, SEEK_SET);
		ret = fwrite(pdata, 1, size, dpt->fp);
		log_info("write %d\n", ret);
	}
	return 0;
}

static void *dpt_thread(void *para)
{
	dpt_t *dpt = (dpt_t*)para;

	int ret = 0;
	struct timespec to;
	unsigned int i;
	UPDEventData_t evt;

	log_info("dpt thread start.\n");

	memset(&evt, 0, sizeof(evt));

	/*request first part to get the total parts number*/
	if(part_start_slots(dpt, 1, 30*1000/*30s*/))
	{
		int ret;
		unsigned char *pdata=NULL;
		unsigned int len=0;

		pthread_mutex_lock(&dpt->lock);

		ret = pthread_cond_wait(&dpt->cond, &dpt->lock);

		pthread_mutex_lock(&dpt->slot_lock);
		if(dpt->part_slot[0].stat == slot_complete) {
			ret = AM_TSUPD_GetDownloaderData(dpt->part_slot[0].did, &pdata, &len);
			if(ret) {
				log_error("get download data fail.[ret:%x]\n", ret);
			} else {
				dpt->last_part = (pdata[2]<<8 | pdata[3]);
				dpt->part_size_max = len-4;
				save_data(dpt, pdata, len);
				dpt->part_slot[0].stat = slot_done;

				unsigned int part_no = (pdata[0]<<8 | pdata[1]);
				log_info("part %d got, last part: %d, part_size(%d)\n", part_no, dpt->last_part, dpt->part_size_max);
			}
		}
		pthread_mutex_unlock(&dpt->slot_lock);

		pthread_mutex_unlock(&dpt->lock);

		dl_part_stop(dpt->part_slot[0].did);
		if(!dpt->quit && (!pdata || !len)) {
			/*no data recerived*/
			log_info("data timeout.\n");
			evt.type = EVENT_UPDATE_DL_TIMEOUT;
			upd_event(dpt->obj, &evt);
			return NULL;
		}
	}

	if(dpt->last_part) {

		int parts_running=0;

		parts_running = part_start_slots(dpt, -1, 30*1000/*30s*/);

		AM_TIME_GetTimeSpecTimeout(dpt->timeout, &to);

		while (!dpt->quit && parts_running)
		{
			pthread_mutex_lock(&dpt->lock);

			ret = pthread_cond_timedwait(&dpt->cond, &dpt->lock, &to);

			if (ret == ETIMEDOUT) {
				log_error("dpt thread Timeout.\n");
				evt.type = EVENT_UPDATE_DL_TIMEOUT;
				upd_event(dpt->obj, &evt);
				break;
			}

			pthread_mutex_lock(&dpt->slot_lock);
			for(i=0; i<(dpt->last_part+1); i++) {
				if(dpt->part_slot[i].stat==slot_complete) {
					unsigned char *pdata=NULL;
					unsigned int len=0;
					int ret = AM_TSUPD_GetDownloaderData(dpt->part_slot[i].did, &pdata, &len);
					if(ret){
						log_error("get download data fail.[ret=%x]\n", ret);
					} else {
						log_info("got data part %d of %d\n", (pdata[0]<<8)|pdata[1], (pdata[2]<<8)|pdata[3]);
						save_data(dpt, pdata, len);
						dl_part_stop(dpt->part_slot[i].did);
						dpt->part_slot[i].stat = slot_done;
					}
				} else if(dpt->part_slot[i].stat==slot_timeout) {
					dl_part_stop(dpt->part_slot[i].did);
					dpt->part_slot[i].stat = 0;
				}
			}
			pthread_mutex_unlock(&dpt->slot_lock);

			parts_running = part_start_slots(dpt, -1, 30*1000/*30s*/);

			pthread_mutex_unlock(&dpt->lock);
		}

		if(parts_running) {
			/*quit force*/
			dpt->total_size = 0;
			part_stop_slots(dpt);
		}else{
			evt.type = EVENT_UPDATE_DL_DONE;
			evt.param1 = 100;
			upd_event(dpt->obj, &evt);
		}
	}

	log_info("dpt thread exit.");
	return NULL;
}



int download_parts(dpt_t *dpt, int dmx, int pid, int tid, FILE *fp, int timeout)
{
	int rc;
	int i;

	pthread_mutexattr_t mta;

	pthread_mutexattr_init(&mta);
//	pthread_mutexattr_settype(&mta, PTHREAD_MUTEX_RECURSIVE_NP);
	pthread_mutex_init(&dpt->lock, &mta);
	pthread_mutex_init(&dpt->slot_lock, &mta);
	pthread_cond_init(&dpt->cond, NULL);
	pthread_mutexattr_destroy(&mta);

	dpt->fp = fp;
	dpt->dmx = dmx;
	dpt->pid = pid;
	dpt->tid = tid;
	dpt->timeout = (unsigned int)timeout;
	dpt->last_part = 0;
	for(i=0; i<MAX_PART_SUPPORT; i++)
		dpt->part_slot[i].stat = 0;
	dpt->part_size_max = 0;
	dpt->dl_size = 0;

	rc = pthread_create(&dpt->thread, NULL, dpt_thread, (void*)dpt);
	if(rc)
	{
		log_error( "dpt thread create fail: %s", strerror(rc));
		pthread_mutex_destroy(&dpt->lock);
		pthread_cond_destroy(&dpt->cond);
		return -1;
	}

	return 0;
}


static int upd_startDownloader(JNIEnv* env, jobject obj, jint dmx_id, jint pid, jint tableid, jstring store, jint timeout)
{
	dpt_t *dpt;

	dpt = (dpt_t*)malloc(sizeof(dpt_t));
	if(!dpt)
		return -1;

	memset(dpt, 0, sizeof(dpt_t));

	const char* str;
	str = env->GetStringUTFChars(store, NULL);
	dpt->store=strdup(str);
	if(str)
		env->ReleaseStringUTFChars(store, str);

	log_info("Start UpdateDownloader dmx[%d] pid[%d] tid[%d] timeout[%d] store[%s]\n",
		dmx_id, pid, tableid, timeout, dpt->store);

	FILE *fp = fopen(dpt->store, "wb");
	if(!fp) {
		log_error("can not create file [%s]\n", dpt->store);
		free(dpt);
		return -1;
	}

	dpt->obj = env->NewGlobalRef(obj);

	env->SetLongField( obj, gDLHandleID, (jlong)dpt);

	download_parts(dpt, dmx_id, pid, tableid, fp, timeout);

	return 0;

}

static int upd_stopDownloader(JNIEnv* env, jobject obj)
{
	AM_ErrorCode_t ret;
	dpt_t *dpt;

	dpt = (dpt_t*)env->GetLongField(obj, gDLHandleID);

	if (!dpt) 
		return -1;

	pthread_t t;

	pthread_mutex_lock(&dpt->lock);
	dpt->quit = 1;
	t = dpt->thread;
	pthread_mutex_unlock(&dpt->lock);

	pthread_cond_signal(&dpt->cond);

	if (t != pthread_self())
		pthread_join(t, NULL);

	env->SetLongField( obj, gDLHandleID, 0);

	if (dpt->obj)
		env->DeleteGlobalRef(dpt->obj);

	if(dpt->store)
		free(dpt->store);
	if(dpt->fp) {
		fclose(dpt->fp);
		dpt->fp=NULL;
	}

	free(dpt);

	return 0;
}


static JNINativeMethod upd_methods[] =
{
	/* name, signature, funcPtr */
	{"native_tvupd_start_monitor", "(ILjava/lang/String;)I", (void*)upd_startMonitor},
	{"native_tvupd_stop_monitor", "()I", (void*)upd_stopMonitor},
	{"native_tvupd_start_downloader", "(IIILjava/lang/String;I)I", (void*)upd_startDownloader},
	{"native_tvupd_stop_downloader", "()I", (void*)upd_stopDownloader},

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

	clazz = env->FindClass( className);

	if (clazz == NULL)
		return -1;

	if ((rc = (env->RegisterNatives(clazz, methods, numMethods))) < 0)
		return -1;

	return 0;
}

JNIEXPORT jint
JNI_OnLoad(JavaVM* vm, void* reserved)
{
	JNIEnv* env = NULL;
	jclass clazz;

	if (vm->GetEnv( (void**) &env, JNI_VERSION_1_4) != JNI_OK)
		return -1;

	if (registerNativeMethods(env, "com/amlogic/tvservice/TVUpdater", upd_methods, NELEM(upd_methods)) < 0)
		return -1;

	gJavaVM = vm;

	clazz = env->FindClass( "com/amlogic/tvservice/TVUpdater");
	if(clazz == NULL){
		log_error("FindClass com/amlogic/tvservice/TVUpdater failed");
		return -1;
	}

	gOnEventID = env->GetMethodID(clazz, "onEvent", "(Lcom/amlogic/tvservice/TVUpdater$Event;)V");
	gMonHandleID = env->GetFieldID(clazz, "native_mon_handle", "J");
	gDLHandleID = env->GetFieldID(clazz, "native_dl_handle", "J");

	gEventClass       = env->FindClass( "com/amlogic/tvservice/TVUpdater$Event");
	gEventClass       = (jclass)env->NewGlobalRef( (jobject)gEventClass);
	gEventInitID      = env->GetMethodID( gEventClass, "<init>", "(Lcom/amlogic/tvservice/TVUpdater;I)V");

	return JNI_VERSION_1_4;
}

} /*extern "C"*/

