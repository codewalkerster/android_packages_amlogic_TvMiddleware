LOCAL_PATH := $(call my-dir)

include $(CLEAR_VARS)

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
	src/com/amlogic/tvservice/TVMBoxDevice.java

LOCAL_PACKAGE_NAME := TVService

LOCAL_SDK_VERSION := current

LOCAL_STATIC_JAVA_LIBRARIES := tvmiddleware
#LOCAL_JAVA_LIBRARIES := tv
LOCAL_PROGUARD_ENABLED := disabled

LOCAL_CERTIFICATE := platform

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

include $(call all-makefiles-under,$(LOCAL_PATH))
