LOCAL_PATH := $(call my-dir)

DVB_PATH := $(wildcard external/dvb)

ifeq ($(DVB_PATH), )
	DVB_PATH := $(wildcard vendor/amlogic/dvb)
endif

include $(CLEAR_VARS)

LOCAL_MODULE    := libjnitvsubtitle
LOCAL_MODULE_TAGS := optional
LOCAL_SRC_FILES := TVSubtitle.cpp
LOCAL_ARM_MODE := arm
LOCAL_C_INCLUDES := external/libzvbi/src \
	$(DVB_PATH)/include/am_mw \
	$(DVB_PATH)/include/am_adp \
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
	$(DVB_PATH)/include/am_mw \
	$(DVB_PATH)/include/am_adp \
	external/sqlite/dist \
	bionic/libc/include \
	external/dvb/android/ndk/include

LOCAL_SHARED_LIBRARIES += libzvbi libam_mw libam_adp libsqlite libskia liblog

LOCAL_PRELINK_MODULE := false

include $(BUILD_SHARED_LIBRARY)

#######################################################################

include $(CLEAR_VARS)

LOCAL_MODULE    := libjnitvmboxdevice
LOCAL_MODULE_TAGS := optional
LOCAL_SRC_FILES := TVMBoxDevice.cpp
LOCAL_C_INCLUDES := external/libzvbi/src \
	$(DVB_PATH)/include/am_mw \
	$(DVB_PATH)/include/am_adp \
	external/sqlite/dist \
	bionic/libc/include \
	packages/amlogic/LibPlayer/amadec/include \
	packages/amlogic/LibPlayer/amcodec/include \
	packages/amlogic/LibPlayer/amffmpeg \
	packages/amlogic/LibPlayer/amplayer \
	external/dvb/android/ndk/include

LOCAL_SHARED_LIBRARIES += libzvbi libam_mw libam_adp libskia liblog

LOCAL_PRELINK_MODULE := false

include $(BUILD_SHARED_LIBRARY)

#######################################################################

include $(CLEAR_VARS)

LOCAL_MODULE    := libjnitvscanner
LOCAL_MODULE_TAGS := optional
LOCAL_SRC_FILES := TVScanner.c
LOCAL_ARM_MODE := arm
LOCAL_C_INCLUDES := external/libzvbi/src \
	$(DVB_PATH)/include/am_mw \
	$(DVB_PATH)/include/am_adp \
	external/sqlite/dist \
	bionic/libc/include \
	external/dvb/android/ndk/include

LOCAL_SHARED_LIBRARIES += libzvbi libam_mw libam_adp libskia liblog

LOCAL_PRELINK_MODULE := false

include $(BUILD_SHARED_LIBRARY)

#######################################################################

include $(CLEAR_VARS)

LOCAL_MODULE    := libjnitvepgscanner
LOCAL_MODULE_TAGS := optional
LOCAL_SRC_FILES := TVEpgScanner.c
LOCAL_ARM_MODE := arm
LOCAL_C_INCLUDES := external/libzvbi/src \
	$(DVB_PATH)/include/am_mw \
	$(DVB_PATH)/include/am_adp \
	external/sqlite/dist \
	bionic/libc/include \
	external/dvb/android/ndk/include

LOCAL_SHARED_LIBRARIES += libzvbi libam_mw libam_adp libskia liblog

LOCAL_PRELINK_MODULE := false

include $(BUILD_SHARED_LIBRARY)

