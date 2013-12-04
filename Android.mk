LOCAL_PATH := $(call my-dir)

include $(CLEAR_VARS)

include $(ANDROID_BUILD_TOP)/device/amlogic/$(TARGET_PRODUCT)/BoardConfig.mk

LOCAL_SRC_FILES := $(call all-java-files-under, src/com/amlogic/tvutil)
LOCAL_SRC_FILES += $(call all-java-files-under, src/com/amlogic/tvclient)
LOCAL_SRC_FILES += $(call all-java-files-under, src/com/amlogic/tvsubtitle)
LOCAL_SRC_FILES += $(call all-java-files-under, src/com/amlogic/tvactivity)
LOCAL_SRC_FILES += $(call all-java-files-under, src/com/amlogic/tvdataprovider)

LOCAL_SRC_FILES += $(call all-Iaidl-files-under, src/com/amlogic/tvutil)
LOCAL_SRC_FILES += $(call all-Iaidl-files-under, src/com/amlogic/tvclient)
LOCAL_SRC_FILES += $(call all-Iaidl-files-under, src/com/amlogic/tvsubtitle)
LOCAL_SRC_FILES += $(call all-Iaidl-files-under, src/com/amlogic/tvactivity)
LOCAL_SRC_FILES += $(call all-Iaidl-files-under, src/com/amlogic/tvservice)
LOCAL_SRC_FILES += src/com/amlogic/tvservice/TVConfig.java

#LOCAL_SDK_VERSION := current

LOCAL_MODULE:= tvmiddleware
#LOCAL_JAVA_LIBRARIES := 

LOCAL_PROGUARD_ENABLED := disabled
LOCAL_PROGUARD_FLAGS := -include $(LOCAL_PATH)/proguard.flags

# Generate a checksum that will be used in the app to determine whether the
# firmware in /system/etc/firmware needs to be updated.

#include $(BUILD_JAVA_LIBRARY)
include $(BUILD_STATIC_JAVA_LIBRARY)

##################################################

include $(CLEAR_VARS)

LOCAL_SRC_FILES := \
	src/com/amlogic/tvservice/TVConfig.java \
	src/com/amlogic/tvservice/TVEpgScanner.java \
	src/com/amlogic/tvservice/TVScanner.java \
	src/com/amlogic/tvservice/TVDevice.java \
	src/com/amlogic/tvservice/TVService.java \
	src/com/amlogic/tvservice/TVTime.java \
	src/com/amlogic/tvservice/TVTime.java \
	src/com/amlogic/tvservice/TVRecorder.java \
	src/com/amlogic/tvservice/TVBookManager.java \
    src/com/amlogic/tvservice/TVServiceReceiver.java

LOCAL_MODULE_CLASS := APPS
LOCAL_MODULE := TVService
intermediates := $(call local-intermediates-dir, COMMON)

GEN_PATH := $(intermediates)/src/src/com/amlogic/tvservice
GEN := $(GEN_PATH)/TVDeviceImpl.java
GEN_SRC_PATH := $(LOCAL_PATH)/src/com/amlogic/tvservice



#BOARD_HAVE_TV := true
ifeq ($(BOARD_HAVE_TV), true)
	GEN_SRC := $(GEN_SRC_PATH)/TVSrvDevice.java
	LOCAL_JAVA_LIBRARIES := tv
else
	GEN_SRC := $(GEN_SRC_PATH)/TVMBoxDevice.java
endif
LOCAL_GENERATED_SOURCES := $(GEN)

$(GEN): $(GEN_SRC)
	mkdir -p $(GEN_PATH)
	cp $(GEN_SRC) $(GEN)

$(info $(GEN))

#LOCAL_SDK_VERSION := current

LOCAL_STATIC_JAVA_LIBRARIES := tvmiddleware

LOCAL_PROGUARD_ENABLED := disabled
LOCAL_CERTIFICATE := platform
LOCAL_PACKAGE_NAME := TVService
LOCAL_MODULE :=
LOCAL_MODULE_CLASS :=
include $(BUILD_PACKAGE)


##################################################

include $(CLEAR_VARS)

LOCAL_MODULE := tv_default.cfg

LOCAL_MODULE_TAGS := optional

LOCAL_MODULE_CLASS := ETC

# This will install the file in /system/etc/
#
LOCAL_MODULE_PATH := $(TARGET_OUT_ETC)

LOCAL_SRC_FILES := $(LOCAL_MODULE)

include $(BUILD_PREBUILT)

##################################################

include $(CLEAR_VARS)

LOCAL_MODULE := tv_default.xml

LOCAL_MODULE_TAGS := optional

LOCAL_MODULE_CLASS := ETC

# This will install the file in /system/etc/
#
LOCAL_MODULE_PATH := $(TARGET_OUT_ETC)

LOCAL_SRC_FILES := $(LOCAL_MODULE)

include $(BUILD_PREBUILT)

##################################################

include $(CLEAR_VARS)

LOCAL_MODULE := tv_default.dtd

LOCAL_MODULE_TAGS := optional

LOCAL_MODULE_CLASS := ETC

# This will install the file in /system/etc/
#
LOCAL_MODULE_PATH := $(TARGET_OUT_ETC)

LOCAL_SRC_FILES := $(LOCAL_MODULE)

include $(BUILD_PREBUILT)

##################################################

include $(call all-makefiles-under,$(LOCAL_PATH))
