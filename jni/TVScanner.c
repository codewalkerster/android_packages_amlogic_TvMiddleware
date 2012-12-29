#include <am_debug.h>
#include <am_scan.h>
#include <am_epg.h>
#include <am_mem.h>
#include <jni.h>
#include <android/log.h>

#define LOG_TAG "TVScannerJNI"
#define log_info(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)
#define log_error(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

typedef struct {
    AM_Bool_t search_nit;
    int dmx_id;
    int fend_id;
    int mode;
    int step;
    int progress;
    int standard;
    int tp_count;
    int cur_srv_type;
    AM_Bool_t locked;
    jobject obj;
    AM_SCAN_TSProgress_t cur_tp;
    char msg[128];
    char cur_name[AM_DB_MAX_SRV_NAME_LEN + 1];
} ProgressData;

enum {
    ONEVENT,
};

enum {
    EVENT_SCAN_PROGRESS = 0,
    EVENT_STORE_BEGIN   = 1,
    EVENT_STORE_END     = 2,
    EVENT_SCAN_END      = 3,
};


static JavaVM* gJavaVm = NULL;
static jclass gScannerClass;
static jclass gChanParamClass;
static jclass gEventClass;
static jmethodID gOnEventID;
static jmethodID gNewEventID;
static jmethodID gNewChanParamID;
static jfieldID gHandleID;

static ProgressData* get_progress_data(JNIEnv *env, jobject obj)
{
    ProgressData *pd = NULL;

    int hScan = (*env)->GetIntField(env, obj, gHandleID);
    AM_SCAN_GetUserData(hScan, (void**)&pd);
    return pd;
}

static jint tv_scan_get_frontend_status(JNIEnv *env, jobject obj)
{
    fe_status_t status;
    ProgressData *pd = get_progress_data(env, obj);
    if(!pd)
        return 0;

    AM_FEND_GetStatus(pd->fend_id, &status);

    return (jint)status;
}

static jint tv_scan_get_frontend_signal_strength(JNIEnv *env, jobject obj)
{
    int strength;
    ProgressData *pd = get_progress_data(env, obj);
    if(!pd)
        return 0;

    AM_FEND_GetStrength(pd->fend_id, &strength);
    return strength;
}

static jint tv_scan_get_frontend_snr(JNIEnv *env, jobject obj)
{
    int snr;
    ProgressData *pd = get_progress_data(env, obj);
    if(!pd)
        return 0;

    AM_FEND_GetSNR(pd->fend_id, &snr);
    return snr;
}

static jint tv_scan_get_frontend_ber(JNIEnv *env, jobject obj)
{
    int ber;
    ProgressData *pd = get_progress_data(env, obj);
    if(!pd)
        return 0;

    AM_FEND_GetBER(pd->fend_id, &ber);
    return ber;
}

static jstring get_java_string(const char *str)
{
    JNIEnv *env;
    int attached = 0;
    int ret = -1;
    jstring jstr;

    ret = (*gJavaVm)->GetEnv(gJavaVm, (void**) &env, JNI_VERSION_1_4);
    if(ret <0) {
        ret = (*gJavaVm)->AttachCurrentThread(gJavaVm,&env,NULL);
        if(ret <0) {
            log_error("callback handler:failed to attach current thread");
            return NULL;
        }
        attached = 1;
    }

    jstr = (*env)->NewStringUTF(env, str);
    if(attached) {
        log_info("callback handler:detach current thread");
        (*gJavaVm)->DetachCurrentThread(gJavaVm);
    }

    return jstr;
}

/**\brief java event callback*/
static void tv_scan_onevent(int evt_type, ProgressData *pd)
{
    JNIEnv *env;
    int attached = 0;
    int ret;

    ret = (*gJavaVm)->GetEnv(gJavaVm, (void**) &env, JNI_VERSION_1_4);
    if(ret <0) {
        ret = (*gJavaVm)->AttachCurrentThread(gJavaVm,&env,NULL);
        if(ret <0) {
            log_error("callback handler:failed to attach current thread");
            return;
        }
        attached = 1;
    }
    jobject event = (*env)->NewObject(env, gEventClass, gNewEventID, pd->obj, evt_type);
    if (event == NULL) return;

    (*env)->SetIntField(env,event,\
                        (*env)->GetFieldID(env, gEventClass, "percent", "I"), (int)(pd->progress));
    (*env)->SetIntField(env,event,\
                        (*env)->GetFieldID(env, gEventClass, "totalChannelCount", "I"), (int)(pd->tp_count));
    (*env)->SetIntField(env,event,\
                        (*env)->GetFieldID(env, gEventClass, "lockedStatus", "I"), (int)(pd->locked ? 1 : 0));
    (*env)->SetIntField(env,event,\
                        (*env)->GetFieldID(env, gEventClass, "channelNumber", "I"), (int)(pd->cur_tp.index));
    (*env)->SetObjectField(env,event,\
                           (*env)->GetFieldID(env, gEventClass, "programName", "Ljava/lang/String;"), pd->cur_name[0] ? get_java_string(pd->cur_name) : NULL);
    /* Clear the name */
    pd->cur_name[0] = 0;
    (*env)->SetIntField(env,event,\
                        (*env)->GetFieldID(env, gEventClass, "programType", "I"), (int)(pd->cur_srv_type));

    jobject cp = (*env)->NewObject(env, gChanParamClass, gNewChanParamID, pd->cur_tp.fend_para.m_type);
    (*env)->SetIntField(env,cp,\
                        (*env)->GetFieldID(env, gChanParamClass, "frequency", "I"), ((struct dvb_frontend_parameters*)&pd->cur_tp.fend_para)->frequency);
    (*env)->SetIntField(env,cp,\
                        (*env)->GetFieldID(env, gChanParamClass, "symbolRate", "I"), pd->cur_tp.fend_para.cable.para.u.qam.symbol_rate);
    (*env)->SetIntField(env,cp,\
                        (*env)->GetFieldID(env, gChanParamClass, "modulation", "I"), pd->cur_tp.fend_para.cable.para.u.qam.modulation);
    (*env)->SetIntField(env,cp,\
                        (*env)->GetFieldID(env, gChanParamClass, "bandwidth", "I"), pd->cur_tp.fend_para.terrestrial.para.u.ofdm.bandwidth);
    (*env)->SetIntField(env,cp,\
                        (*env)->GetFieldID(env, gChanParamClass, "audio", "I"), pd->cur_tp.fend_para.analog.para.u.analog.audmode);
    (*env)->SetIntField(env,cp,\
                        (*env)->GetFieldID(env, gChanParamClass, "standard", "I"), pd->cur_tp.fend_para.analog.para.u.analog.std);
    (*env)->SetObjectField(env,event,\
                           (*env)->GetFieldID(env, gEventClass, "channelParams", "Lcom/amlogic/tvutil/TVChannelParams;"), cp);


    log_info("Call tvscanner java onevent, event type %d", evt_type);
    /*Call the java method*/
    (*env)->CallVoidMethod(env, pd->obj, gOnEventID, event);
    log_info("call java  method in JNI env end");
    if(attached) {
        (*gJavaVm)->DetachCurrentThread(gJavaVm);
    }
}


/**\brief 查找并通知No Name节目*/
static void tv_scan_notify_program(ProgressData *pd, AM_SCAN_TS_t * ts)
{
    dvbpsi_pmt_t *pmt;
    dvbpsi_sdt_t *sdt;
    dvbpsi_pmt_es_t *es;
    dvbpsi_sdt_service_t *srv;
    dvbpsi_descriptor_t *descr;
    uint16_t srv_id;
    char name[AM_DB_MAX_SRV_NAME_LEN + 1];
    AM_Bool_t name_found;
    AM_Bool_t has_audio, has_video;
    jobject obj = pd->obj;

    /*遍历PMT表*/
    AM_SI_LIST_BEGIN(ts->digital.pmts, pmt)
    srv_id = pmt->i_program_number;
    name[0] = '\0';
    name_found = AM_FALSE;
    has_audio = AM_FALSE;
    has_video = AM_FALSE;
    /*取ES流信息*/
    AM_SI_LIST_BEGIN(pmt->p_first_es, es)
    if (has_audio && has_video)
        break;

    switch (es->i_type) {
        /*override by parse descriptor*/
    case 0x6:
        AM_SI_LIST_BEGIN(es->p_first_descriptor, descr)
        if (!has_audio && (descr->i_tag == AM_SI_DESCR_AC3 ||
                           descr->i_tag == AM_SI_DESCR_ENHANCED_AC3 ||
                           descr->i_tag == AM_SI_DESCR_AAC ||
                           descr->i_tag == AM_SI_DESCR_DTS) && es->i_pid < 0x1fff) {
            has_audio = AM_TRUE;
        }
        AM_SI_LIST_END()
        break;
        /*video*/
    case 0x1:
    case 0x2:
    case 0x10:
    case 0x1b:
    case 0xea:
        if (!has_video && es->i_pid < 0x1fff)
            has_video = AM_TRUE;
        break;
        /*audio*/
    case 0x3:
    case 0x4:
    case 0x0f:
    case 0x11:
    case 0x81:
    case 0x8A:
    case 0x82:
    case 0x85:
    case 0x86:
        if (!has_audio && es->i_pid < 0x1fff)
            has_audio = AM_TRUE;
        break;
    default:
        break;
    }
    AM_SI_LIST_END()

    AM_SI_LIST_BEGIN(ts->digital.sdts, sdt)
    AM_SI_LIST_BEGIN(sdt->p_first_service, srv)
    /*从SDT表中查找该service并获取信息*/
    if (srv->i_service_id == srv_id) {
        AM_SI_LIST_BEGIN(srv->p_first_descriptor, descr)
        if (descr->p_decoded && descr->i_tag == AM_SI_DESCR_SERVICE) {
            dvbpsi_service_dr_t *psd = (dvbpsi_service_dr_t*)descr->p_decoded;

            if (psd->i_service_name_length <= 0 && (has_video || has_audio)) {
                name_found = AM_FALSE;
            } else {
                name_found = AM_TRUE;

            }

            /*无有效音视频PID的电视广播节目不通知*/
            if (name_found && ((psd->i_service_type != 0x1 && psd->i_service_type != 0x2) ||
                               ((psd->i_service_type == 0x1 || psd->i_service_type == 0x2) && (has_video || has_audio)))) {
                /*取节目名称*/
                if (psd->i_service_name_length > 0) {
                    AM_SI_ConvertDVBTextCode((char*)psd->i_service_name, psd->i_service_name_length,\
                                       name, AM_DB_MAX_SRV_NAME_LEN);
                    name[AM_DB_MAX_SRV_NAME_LEN] = 0;

                    /*service type 0x16 and 0x19 is user defined, as digital television service*/
                    /*0xc0 is the type of partial reception service in ISDBT*/
                    if((psd->i_service_type == 0x16) || (psd->i_service_type == 0x19) || (psd->i_service_type == 0xc0)) {
                        pd->cur_srv_type = AM_SCAN_SRV_DTV;
                    } else {
                        pd->cur_srv_type = psd->i_service_type==1 ? AM_SCAN_SRV_DTV : AM_SCAN_SRV_DRADIO;
                    }
                    log_info("Native scan notify: %s, prog_type %d, sid=%d\n", name, psd->i_service_type, srv->i_service_id);
                    strcpy(pd->cur_name, name);
                    tv_scan_onevent(EVENT_SCAN_PROGRESS, pd);
                }
            }
            /*跳出多层循环*/
            goto SDT_END;
        }
        AM_SI_LIST_END()
    }
    AM_SI_LIST_END()
    AM_SI_LIST_END()
SDT_END:
    if (! name_found && (has_video || has_audio)) {
        strcpy(name, "No Name");
        strcpy(pd->cur_name, name);
        pd->cur_srv_type = has_video ? AM_SCAN_SRV_DTV : AM_SCAN_SRV_DRADIO;
        tv_scan_onevent(EVENT_SCAN_PROGRESS, pd);
    }

    AM_SI_LIST_END()
}

/**\brief 搜索事件回调*/
static void tv_scan_evt_callback(int dev_no, int event_type, void *param, void *data)
{
    ProgressData *prog = NULL;
    jobject obj;

    AM_SCAN_GetUserData(dev_no, (void**)&prog);
    if (! prog) return;

    obj = prog->obj;

    if (event_type == AM_SCAN_EVT_PROGRESS) {
        AM_SCAN_Progress_t *evt = (AM_SCAN_Progress_t*)param;

        switch (evt->evt) {
        case AM_SCAN_PROGRESS_SCAN_BEGIN:
            prog->progress = 0;
            prog->step = 0;
            prog->search_nit = AM_FALSE;
            break;
        case AM_SCAN_PROGRESS_NIT_BEGIN:
            prog->search_nit = AM_TRUE;
            break;
        case AM_SCAN_PROGRESS_NIT_END:
            prog->search_nit = AM_FALSE;
            break;
        case AM_SCAN_PROGRESS_TS_BEGIN: {
            AM_SCAN_TSProgress_t *tp = (AM_SCAN_TSProgress_t*)evt->data;

            if (tp) {
                prog->cur_tp = *tp;

                prog->progress = (tp->index*100)/tp->total;
                if (prog->tp_count == 0)
                    prog->tp_count = tp->total;
                if (prog->progress >= 100)
                    prog->progress = 99;

                prog->locked = AM_FALSE;
                tv_scan_onevent(EVENT_SCAN_PROGRESS, prog);
            }
        }
        break;
        case AM_SCAN_PROGRESS_TS_END: {
            AM_SCAN_TS_t *ts = (AM_SCAN_TS_t*)evt->data;

            if (ts != NULL) {
                /*查找SDT表里没有描述但PMT有描述的电视广播节目，并通知界面*/
                tv_scan_notify_program(prog, ts);

            }
        }
        break;
        case AM_SCAN_PROGRESS_PAT_DONE:
            if (prog->mode & AM_SCAN_DTVMODE_MANUAL && prog->tp_count == 1) {
                tv_scan_onevent(EVENT_SCAN_PROGRESS, prog);
            }

            break;
        case AM_SCAN_PROGRESS_SDT_DONE: {
            dvbpsi_sdt_t *sdts = (dvbpsi_sdt_t *)evt->data;
            dvbpsi_sdt_t *sdt;

            if (prog->mode & AM_SCAN_DTVMODE_MANUAL && prog->tp_count == 1) {
                prog->progress += 25;
                if (prog->progress >= 100)
                    prog->progress = 99;
                tv_scan_onevent(EVENT_SCAN_PROGRESS, prog);
            }
        }
        break;
        case AM_SCAN_PROGRESS_CAT_DONE: {
            dvbpsi_cat_t *cat = (dvbpsi_cat_t *)evt->data;

            if (prog->mode & AM_SCAN_DTVMODE_MANUAL && prog->tp_count == 1) {
                prog->progress += 25;
                if (prog->progress >= 100)
                    prog->progress = 99;

                tv_scan_onevent(EVENT_SCAN_PROGRESS, prog);
            }
        }
        break;
        case AM_SCAN_PROGRESS_PMT_DONE: {
            dvbpsi_pmt_t *pmt = (dvbpsi_pmt_t *)evt->data;

            if (prog->mode & AM_SCAN_DTVMODE_MANUAL && prog->tp_count == 1) {
                prog->progress += 25;
                if (prog->progress >= 100)
                    prog->progress = 99;

                tv_scan_onevent(EVENT_SCAN_PROGRESS, prog);
            }
        }
        break;
        case AM_SCAN_PROGRESS_MGT_DONE: {
            mgt_section_info_t *mgt = (mgt_section_info_t*)evt->data;

            if (prog->mode & AM_SCAN_DTVMODE_MANUAL && prog->tp_count == 1) {
                prog->progress += 10;
                if (prog->progress >= 100)
                    prog->progress = 99;

                tv_scan_onevent(EVENT_SCAN_PROGRESS, prog);
            }
        }
        break;
        case AM_SCAN_PROGRESS_TVCT_DONE: {
            /*ATSC TVCT*/
            if (prog->mode & AM_SCAN_DTVMODE_MANUAL && prog->tp_count == 1) {
                prog->progress += 15;
                if (prog->progress >= 100)
                    prog->progress = 99;

                tv_scan_onevent(EVENT_SCAN_PROGRESS, prog);
            }
        }
        break;
        case AM_SCAN_PROGRESS_CVCT_DONE: {
            if (prog->mode & AM_SCAN_DTVMODE_MANUAL && prog->tp_count == 1) {
                prog->progress += 15;
                if (prog->progress >= 100)
                    prog->progress = 99;

                tv_scan_onevent(EVENT_SCAN_PROGRESS, prog);
            }
        }
        break;
        case AM_SCAN_PROGRESS_BLIND_SCAN: {

        }
        break;
        case AM_SCAN_PROGRESS_STORE_BEGIN:
            tv_scan_onevent(EVENT_STORE_BEGIN, prog);
            break;
        case AM_SCAN_PROGRESS_STORE_END:
            tv_scan_onevent(EVENT_STORE_END, prog);
            break;
        case AM_SCAN_PROGRESS_SCAN_END:
            prog->progress = 100;
            tv_scan_onevent(EVENT_SCAN_END, prog);
            break;
        case AM_SCAN_PROGRESS_ATV_TUNING:
            prog->cur_tp.fend_para.analog.para.frequency = (int)evt->data;
            prog->locked = AM_FALSE;
            tv_scan_onevent(EVENT_SCAN_PROGRESS, prog);
            break;

        default:
            break;
        }
    } else if (event_type == AM_SCAN_EVT_SIGNAL) {
        AM_SCAN_DTVSignalInfo_t *evt = (AM_SCAN_DTVSignalInfo_t*)param;
        prog->locked = evt->locked;
        tv_scan_onevent(EVENT_SCAN_PROGRESS, prog);
    }
}

static int tv_scan_get_channel_para(JNIEnv *env, jobject obj, jobject para, AM_FENDCTRL_DVBFrontendParameters_t **ppfp)
{
    jfieldID symbol_rate = 0;
    jfieldID modulation = 0;
    jfieldID freq = 0;
    jfieldID bandwidth = 0;
    jfieldID polar = 0;
    jfieldID mode = 0;
    AM_FENDCTRL_DVBFrontendParameters_t *fparam;
    int i = 0;

    if(!ppfp) return -1;

    jclass objclass=(*env)->FindClass(env,"com/amlogic/tvutil/TVChannelParams");
    mode = (*env)->GetFieldID(env,objclass, "mode", "I");
    freq = (*env)->GetFieldID(env,objclass, "frequency", "I");
    symbol_rate = (*env)->GetFieldID(env,objclass, "symbolRate", "I");
    modulation =(*env)->GetFieldID(env,objclass, "modulation", "I");
    bandwidth=(*env)->GetFieldID(env,objclass, "bandwidth", "I");

    if(!(*ppfp = malloc(sizeof(AM_FENDCTRL_DVBFrontendParameters_t))))
        return -1;

    fparam = *ppfp;
    memset(fparam, 0, sizeof(AM_FENDCTRL_DVBFrontendParameters_t));
    fparam->m_type = (*env)->GetIntField(env, para, mode);
    switch(fparam->m_type) {
    case FE_QAM:
        fparam[i].cable.para.frequency = (*env)->GetIntField(env, para, freq);
        fparam[i].cable.para.inversion = 0;
        fparam[i].cable.para.u.qam.symbol_rate = (*env)->GetIntField(env, para, symbol_rate);
        fparam[i].cable.para.u.qam.modulation = (*env)->GetIntField(env, para, modulation);
        break;
    case FE_OFDM:
        fparam[i].terrestrial.para.frequency = (*env)->GetIntField(env, para, freq);
        fparam[i].terrestrial.para.inversion = 0;
        fparam[i].terrestrial.para.u.ofdm.bandwidth = (*env)->GetIntField(env, para, bandwidth);
        break;
    case FE_QPSK:

        break;
    case FE_ATSC:
        break;
    default:
        break;
    }

    return 0;
}

static int tv_scan_get_fe_paras(JNIEnv *env, jobject obj, jint src, jobjectArray freqs, AM_FENDCTRL_DVBFrontendParameters_t **ppfp)
{
    jfieldID freq = 0, bandwidth = 0; 	
    int i;
    AM_FENDCTRL_DVBFrontendParameters_t *pfp;

    if(!ppfp) return -1;

    if (freqs == NULL)
        return 0;

    int size = (*env)->GetArrayLength(env, freqs);
    if(size<=0) return size;

	jclass objclass=(*env)->FindClass(env, "com/amlogic/tvutil/TVChannelParams");

	if(freq == 0) {
		freq = (*env)->GetFieldID(env, objclass, "frequency", "I"); 
		if (freq == 0) 
			return -1;	
	}

    if(!(*ppfp = calloc(size, sizeof(AM_FENDCTRL_DVBFrontendParameters_t))))
        return -1;

    pfp = *ppfp;
    memset(pfp, 0, size * sizeof(AM_FENDCTRL_DVBFrontendParameters_t));
    for (i = 0; i < size; i++,pfp++) {
		jobject fend_para = ((*env)->GetObjectArrayElement(env, freqs, i));
		
        pfp->m_type = src;
        ((struct dvb_frontend_parameters*)pfp)->frequency = (*env)->GetIntField(env, fend_para, freq);
        switch (pfp->m_type) {
        case FE_QAM:
            ((struct dvb_frontend_parameters*)pfp)->u.qam.symbol_rate = 6875000;
            ((struct dvb_frontend_parameters*)pfp)->u.qam.modulation = QAM_64;
            break;
        case FE_OFDM:
		if(bandwidth == 0) {
			bandwidth=(*env)->GetFieldID(env, objclass, "bandwidth", "I"); 
			if (bandwidth == 0) 
				return -1; 
		}
			
            ((struct dvb_frontend_parameters*)pfp)->u.ofdm.bandwidth = (*env)->GetIntField(env, fend_para, bandwidth);
            break;
        case FE_QPSK:
            break;
        case FE_ATSC:
            break;
        default:
            break;
        }

    }

    return size;
}

static int get_sat_para(JNIEnv *env, jobject thiz, jobject para, AM_SCAN_DTVSatellitePara_t *sat_para)
{
    jfieldID name,lnb_num,lof_hi,lof_lo,lof_threshold,signal_22khz,voltage_mode;
    jfieldID motor_num, pos_num, lo_dir, la_dir, lo, la, diseqc_mode, toneburst;
    jfieldID cdc, ucdc, repeats, cmd_order, fast_diseqc, seq_repeat, ub, ub_freq, sat_lo;
    double cof;

    jclass objclass =(*env)->FindClass(env,"com/amlogic/dvbmw/SatellitePara");

    name = (*env)->GetFieldID(env,objclass, "name", "Ljava/lang/String;");
    lnb_num = (*env)->GetFieldID(env,objclass, "lnb_num", "I");
    lof_hi = (*env)->GetFieldID(env,objclass, "lof_hi", "I");
    lof_lo = (*env)->GetFieldID(env,objclass, "lof_lo", "I");
    lof_threshold = (*env)->GetFieldID(env,objclass, "lof_threshold", "I");
    signal_22khz = (*env)->GetFieldID(env,objclass, "signal_22khz", "I");
    voltage_mode = (*env)->GetFieldID(env,objclass, "voltage_mode", "I");
    motor_num = (*env)->GetFieldID(env,objclass, "motor_num", "I");
    pos_num = (*env)->GetFieldID(env,objclass, "position_number", "I");
    lo_dir = (*env)->GetFieldID(env,objclass, "lo_direction", "I");
    la_dir = (*env)->GetFieldID(env,objclass, "la_direction", "I");
    lo = (*env)->GetFieldID(env,objclass, "longitude", "D");
    la = (*env)->GetFieldID(env,objclass, "latitude", "D");
    diseqc_mode = (*env)->GetFieldID(env,objclass, "diseqc_mode", "I");
    toneburst = (*env)->GetFieldID(env,objclass, "toneburst", "I");
    cdc = (*env)->GetFieldID(env,objclass, "committed_cmd", "I");
    ucdc = (*env)->GetFieldID(env,objclass, "uncommitted_cmd", "I");
    repeats = (*env)->GetFieldID(env,objclass, "repeats", "I");
    cmd_order = (*env)->GetFieldID(env,objclass, "cmd_order", "I");
    fast_diseqc = (*env)->GetFieldID(env,objclass, "fast_diseqc", "I");
    seq_repeat = (*env)->GetFieldID(env,objclass, "seq_repeat", "I");
    ub = (*env)->GetFieldID(env,objclass, "user_band", "I");
    ub_freq = (*env)->GetFieldID(env,objclass, "ub_freq", "I");
    sat_lo = (*env)->GetFieldID(env,objclass, "sat_longitude", "D");

    jstring sname = (*env)->GetObjectField(env, para, name);
    const char *str = (*env)->GetStringUTFChars(env, sname, 0);
    memset(sat_para->sat_name, 0, sizeof(sat_para->sat_name));
    if (str != NULL) {
        strncpy(sat_para->sat_name, str, sizeof(sat_para->sat_name) - 1);
        (*env)->ReleaseStringUTFChars(env ,sname, str);
    }

    sat_para->lnb_num = (*env)->GetIntField(env, para, lnb_num);
    sat_para->sec.m_lnbs.m_lof_hi = (*env)->GetIntField(env, para, lof_hi);
    sat_para->sec.m_lnbs.m_lof_lo = (*env)->GetIntField(env, para, lof_lo);
    sat_para->sec.m_lnbs.m_lof_threshold = (*env)->GetIntField(env, para, lof_threshold);
    sat_para->sec.m_lnbs.m_cursat_parameters.m_22khz_signal = (*env)->GetIntField(env, para, signal_22khz);
    sat_para->sec.m_lnbs.m_cursat_parameters.m_voltage_mode = (*env)->GetIntField(env, para, voltage_mode);
    sat_para->motor_num = (*env)->GetIntField(env, para, motor_num);
    sat_para->sec.m_lnbs.m_cursat_parameters.m_rotorPosNum = (*env)->GetIntField(env, para, pos_num);
    if ((*env)->GetIntField(env, para, lo_dir) == 0)
        cof = 1.0;
    else
        cof = -1.0;
    sat_para->sec.m_lnbs.m_rotor_parameters.m_gotoxx_parameters.m_longitude = (*env)->GetDoubleField(env, para, lo) * cof;
    if ((*env)->GetIntField(env, para, la_dir) == 0)
        cof = 1.0;
    else
        cof = -1.0;
    sat_para->sec.m_lnbs.m_rotor_parameters.m_gotoxx_parameters.m_latitude = (*env)->GetDoubleField(env, para, la) * cof;
    sat_para->sec.m_lnbs.m_rotor_parameters.m_gotoxx_parameters.m_sat_longitude = (*env)->GetDoubleField(env, para, sat_lo);
    sat_para->sec.m_lnbs.m_diseqc_parameters.m_diseqc_mode = (*env)->GetIntField(env, para, diseqc_mode);
    sat_para->sec.m_lnbs.m_diseqc_parameters.m_toneburst_param = (*env)->GetIntField(env, para, toneburst);
    sat_para->sec.m_lnbs.m_diseqc_parameters.m_committed_cmd = (unsigned char)(*env)->GetIntField(env, para, cdc);
    sat_para->sec.m_lnbs.m_diseqc_parameters.m_uncommitted_cmd = (unsigned char)(*env)->GetIntField(env, para, ucdc);
    sat_para->sec.m_lnbs.m_diseqc_parameters.m_repeats = (*env)->GetIntField(env, para, repeats);
    sat_para->sec.m_lnbs.m_diseqc_parameters.m_command_order = (*env)->GetIntField(env, para, cmd_order);
    sat_para->sec.m_lnbs.m_diseqc_parameters.m_use_fast = (*env)->GetIntField(env, para, fast_diseqc);
    sat_para->sec.m_lnbs.m_diseqc_parameters.m_seq_repeat = (*env)->GetIntField(env, para, seq_repeat);
    sat_para->sec.m_lnbs.SatCR_idx = (*env)->GetIntField(env, para, ub);
    sat_para->sec.m_lnbs.SatCRvco = (*env)->GetIntField(env, para, ub_freq);
    sat_para->sec.m_lnbs.LNBNum = sat_para->sec.m_lnbs.m_diseqc_parameters.m_toneburst_param==B ? 2 : 1;

    return 0;
}

/**\brief Get the start scan params*/
static jint tv_scan_get_start_para(JNIEnv *env, jobject thiz, jobject para, AM_SCAN_CreatePara_t *start_para)
{
	jfieldID amode, min_freq, max_freq, start_freq, direction, std;
	jfieldID dmode, source, chan_para, freqs, mode, fend_id, dmx_id;
	int java_mode;
	
	jclass objclass =(*env)->FindClass(env,"com/amlogic/tvservice/TVScanner$TVScannerParams"); 
	
	amode = (*env)->GetFieldID(env,objclass, "atvMode", "I"); 
	min_freq = (*env)->GetFieldID(env,objclass, "minFreq", "I"); 
	max_freq = (*env)->GetFieldID(env,objclass, "maxFreq", "I"); 
	start_freq = (*env)->GetFieldID(env,objclass, "startFreq", "I"); 
	direction = (*env)->GetFieldID(env,objclass, "direction", "I"); 
	std = (*env)->GetFieldID(env,objclass, "tunerStd", "I"); 
	mode = (*env)->GetFieldID(env,objclass, "mode", "I"); 
	dmode = (*env)->GetFieldID(env,objclass, "dtvMode", "I"); 
	fend_id = (*env)->GetFieldID(env,objclass, "fendID", "I"); 
	dmx_id = (*env)->GetFieldID(env,objclass, "demuxID", "I"); 
	source = (*env)->GetFieldID(env,objclass, "tsSourceID", "I"); 
	chan_para = (*env)->GetFieldID(env,objclass, "startParams", "Lcom/amlogic/tvutil/TVChannelParams;"); 
	freqs = (*env)->GetFieldID(env,objclass, "ChannelParamsList", "[Lcom/amlogic/tvutil/TVChannelParams;"); 
	
	start_para->fend_dev_id = (*env)->GetIntField(env, para, fend_id); 
	java_mode = (*env)->GetIntField(env, para, mode); 
	if (java_mode == 0) {
		start_para->mode = AM_SCAN_MODE_ATV_DTV;
		start_para->dtv_para.mode = AM_SCAN_DTVMODE_NONE;
		/* Only search ATV */
		start_para->atv_para.mode = (*env)->GetIntField(env, para, amode); 
		start_para->atv_para.fe_cnt= 3;
		start_para->atv_para.fe_paras = calloc(3, sizeof(AM_FENDCTRL_DVBFrontendParameters_t));
		if (start_para->atv_para.fe_paras != NULL) 
		{
			memset(start_para->atv_para.fe_paras, 0, 3 * sizeof(AM_FENDCTRL_DVBFrontendParameters_t));		
			start_para->atv_para.fe_paras[0].m_type = FE_ANALOG;
			start_para->atv_para.fe_paras[0].analog.para.frequency = (*env)->GetIntField(env, para, min_freq); 
			start_para->atv_para.fe_paras[1].m_type = FE_ANALOG;
			start_para->atv_para.fe_paras[1].analog.para.frequency = (*env)->GetIntField(env, para, max_freq); 
			start_para->atv_para.fe_paras[2].m_type = FE_ANALOG;
			start_para->atv_para.fe_paras[2].analog.para.frequency = (*env)->GetIntField(env, para, start_freq);
			log_info("min %u, max %u, start %u", start_para->atv_para.fe_paras[0].analog.para.frequency,
				start_para->atv_para.fe_paras[1].analog.para.frequency,
				start_para->atv_para.fe_paras[2].analog.para.frequency);
		}
		
		start_para->atv_para.direction = (*env)->GetIntField(env, para, direction);
		start_para->atv_para.default_std= (*env)->GetIntField(env, para, std);
		start_para->atv_para.afc_unlocked_step = 3000000;
		start_para->atv_para.cvbs_unlocked_step = 1500000;
		start_para->atv_para.cvbs_locked_step = 6000000;
		start_para->atv_para.afc_range = 2000000;
    } else {
        start_para->mode = AM_SCAN_MODE_DTV_ATV;
        start_para->atv_para.mode = AM_SCAN_ATVMODE_NONE;
        /* Only search DTV */
        start_para->dtv_para.mode = (*env)->GetIntField(env, para, dmode);
        start_para->dtv_para.source = (*env)->GetIntField(env, para, source);
        start_para->dtv_para.dmx_dev_id = (*env)->GetIntField(env, para, dmx_id);
        if ((start_para->dtv_para.mode&0x07) == AM_SCAN_DTVMODE_AUTO ||
            (start_para->dtv_para.mode&0x07) == AM_SCAN_DTVMODE_MANUAL) {
            jobject cp = (*env)->GetObjectField(env, para, chan_para);
            start_para->dtv_para.fe_cnt = 1;
            if (tv_scan_get_channel_para(env, thiz, cp, &start_para->dtv_para.fe_paras) < 0) {
                log_info("Cannot get channel param !");
                return -1;
            }
            /* use the fe_type  specified by user */
            start_para->dtv_para.source = start_para->dtv_para.fe_paras[0].m_type;
        } else {
            jobjectArray freq_list = (*env)->GetObjectField(env, para, freqs);
            start_para->dtv_para.fe_cnt = tv_scan_get_fe_paras(env, thiz, start_para->dtv_para.source,
                                          freq_list, &start_para->dtv_para.fe_paras);
        }
        if (start_para->dtv_para.source == FE_ATSC) {
            start_para->dtv_para.standard = AM_SCAN_DTV_STD_ATSC;
        }
        //Fix me: How can we start for ISDB ?
        else {
            start_para->dtv_para.standard = AM_SCAN_DTV_STD_DVB;
        }
    }

    return 0;
}

/**\brief 开始搜索*/
static jint tv_scan_start(JNIEnv *env, jobject obj, jobject scan_para)
{
    AM_SCAN_CreatePara_t para;
    AM_FEND_OpenPara_t fend_para;
    AM_DMX_OpenPara_t dmx_para;
    int handle = 0,i;
    ProgressData *prog = NULL;

    /**Alloc progress data*/
    prog = (ProgressData*)malloc(sizeof(ProgressData));
    if (! prog) {
        log_error("No memory");
        goto create_end;
    }
    memset(prog, 0, sizeof(ProgressData));
    prog->obj = (*env)->NewGlobalRef(env,obj);
    if (prog->obj == NULL) {
        goto create_end;
    }

    /** Create the scan*/
    memset(&para, 0, sizeof(para));
    if (tv_scan_get_start_para(env, obj, scan_para, &para) < 0) {
        log_error("get scan start param failed");
        goto create_end;
    }

    para.dtv_para.resort_all = AM_FALSE;
    para.dtv_para.sort_method = AM_SCAN_SORT_BY_FREQ_SRV_ID;
    para.store_cb = NULL;
    /* Open frontend & demux */
    log_info("native start scan");
    log_info("Opening frontend%d ...", para.fend_dev_id);
    fend_para.mode = -1;
    AM_FEND_Open(para.fend_dev_id, &fend_para);
    log_info("Opening demux%d ...", para.dtv_para.dmx_dev_id);
    memset(&dmx_para, 0, sizeof(dmx_para));
    AM_DMX_Open(para.dtv_para.dmx_dev_id, &dmx_para);
    AM_DMX_SetSource(para.dtv_para.dmx_dev_id, AM_DMX_SRC_TS2);
    prog->dmx_id = para.dtv_para.dmx_dev_id;
    prog->fend_id = para.fend_dev_id;

    /* Start Scan */
    if (AM_SCAN_Create(&para, &handle) != AM_SUCCESS) {
        handle = 0;
    } else {
        AM_SCAN_SetUserData(handle, (void*)prog);
        /*注册搜索事件*/
        AM_EVT_Subscribe(handle, AM_SCAN_EVT_PROGRESS, tv_scan_evt_callback, NULL);
        /*注册信号质量通知事件*/
        AM_EVT_Subscribe(handle, AM_SCAN_EVT_SIGNAL, tv_scan_evt_callback, NULL);
        if (AM_SCAN_Start(handle) != AM_SUCCESS) {
            AM_SCAN_Destroy(handle, AM_FALSE);
            AM_EVT_Unsubscribe(handle, AM_SCAN_EVT_PROGRESS, tv_scan_evt_callback, NULL);
            AM_EVT_Unsubscribe(handle, AM_SCAN_EVT_SIGNAL, tv_scan_evt_callback, NULL);
            handle = 0;
        }
    }

    if (para.atv_para.fe_paras != NULL)
        free(para.atv_para.fe_paras);
    if (para.dtv_para.fe_paras != NULL)
        free(para.dtv_para.fe_paras);

create_end:
    if (handle == 0) {
        if (prog) {
            if (prog->obj)
                (*env)->DeleteGlobalRef(env, prog->obj);
            free(prog);
        }
    }
    log_info("return create handle %d", handle);
    return handle;
}

/**\brief 销毁搜索*/
static jint tv_scan_destroy(JNIEnv *env, jobject obj, jint hscan, jboolean store)
{
    ProgressData *prog = NULL;
    AM_Bool_t bstore = store ? AM_TRUE : AM_FALSE;
    int ret = -1;

    if(hscan != 0) {
        AM_SCAN_GetUserData(hscan, (void**)&prog);

        ret = AM_SCAN_Destroy(hscan, bstore);
        log_info("Scan destroyed, ret=%d", ret);
        AM_EVT_Unsubscribe(hscan, AM_SCAN_EVT_PROGRESS, tv_scan_evt_callback, NULL);
        AM_EVT_Unsubscribe(hscan, AM_SCAN_EVT_SIGNAL, tv_scan_evt_callback, NULL);
        if (prog) {
            log_info("Closing demux%d ...", prog->dmx_id);
            AM_DMX_Close(prog->dmx_id);
            log_info("Closing frontend%d ...", prog->fend_id);
            AM_FEND_Close(prog->fend_id);
            if (prog->obj)
                (*env)->DeleteGlobalRef(env, prog->obj);
            free(prog);
        }
    }

    return ret;
}

static JNINativeMethod tv_scan_methods[] = {
    /* name, signature, funcPtr */
    {"native_tv_scan_start", "(Lcom/amlogic/tvservice/TVScanner$TVScannerParams;)I", (void*)tv_scan_start},
    {"native_tv_scan_destroy", "(IZ)I", (void*)tv_scan_destroy},
    {"native_get_frontend_status", "()I", (void*)tv_scan_get_frontend_status},
    {"native_get_frontend_signal_strength", "()I", (void*)tv_scan_get_frontend_signal_strength},
    {"native_get_frontend_snr", "()I", (void*)tv_scan_get_frontend_snr},
    {"native_get_frontend_ber", "()I", (void*)tv_scan_get_frontend_ber},
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

    gScannerClass     = clazz;
    gScannerClass     = (jclass)(*env)->NewGlobalRef(env, (jobject)gScannerClass);
    gHandleID         = (*env)->GetFieldID(env, clazz, "hScan", "I");
    gEventClass       = (*env)->FindClass(env, "com/amlogic/tvservice/TVScanner$Event");
    gEventClass       = (jclass)(*env)->NewGlobalRef(env, (jobject)gEventClass);
    gNewEventID       = (*env)->GetMethodID(env, gEventClass, "<init>", "(Lcom/amlogic/tvservice/TVScanner;I)V");
    gOnEventID        = (*env)->GetMethodID(env, clazz, "onEvent", "(Lcom/amlogic/tvservice/TVScanner$Event;)V");
    gChanParamClass   = (*env)->FindClass(env, "com/amlogic/tvutil/TVChannelParams");
    gChanParamClass   = (jclass)(*env)->NewGlobalRef(env, (jobject)gChanParamClass);
    gNewChanParamID      = (*env)->GetMethodID(env, gChanParamClass, "<init>", "(I)V");

    return 0;
}

JNIEXPORT jint
JNI_OnLoad(JavaVM* vm, void* reserved)
{
    JNIEnv* env = NULL;

    if ((*vm)->GetEnv(vm, (void**) &env, JNI_VERSION_1_4) != JNI_OK)
        return -1;

    if (registerNativeMethods(env, "com/amlogic/tvservice/TVScanner", tv_scan_methods, NELEM(tv_scan_methods)) < 0)
        return -1;

    gJavaVm = vm;

    return JNI_VERSION_1_4;
}

