LOCAL_PATH := $(call my-dir)

include $(CLEAR_VARS)

LOCAL_MODULE    := libjnitvsubtitle
LOCAL_MODULE_TAGS := optional
LOCAL_SRC_FILES := TVSubtitle.cpp
LOCAL_ARM_MODE := arm
LOCAL_C_INCLUDES := external/libzvbi/src \
	external/dvb/include/am_mw \
	external/dvb/include/am_adp \
	bionic/libc/include \
	external/dvb/android/ndk/include

LOCAL_SHARED_LIBRARIES += libzvbi libam_mw libam_adp libskia liblog

LOCAL_PRELINK_MODULE := false

include $(BUILD_SHARED_LIBRARY)

#######################################################################

include $(CLEAR_VARS)

LOCAL_MODULE    := libjnitvdatabase
LOCAL_MODULE_TAGS := optional
LOCAL_SRC_FILES := TVDatabase.c
LOCAL_ARM_MODE := arm
LOCAL_C_INCLUDES := external/libzvbi/src \
	external/dvb/include/am_mw \
	external/dvb/include/am_adp \
	external/sqlite/dist \
	bionic/libc/include \
	external/dvb/android/ndk/include

LOCAL_SHARED_LIBRARIES += libzvbi libam_mw libam_adp libskia liblog

LOCAL_PRELINK_MODULE := false

include $(BUILD_SHARED_LIBRARY)

