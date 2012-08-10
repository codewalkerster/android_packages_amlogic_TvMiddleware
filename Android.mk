LOCAL_PATH := $(call my-dir)
include $(CLEAR_VARS)

LOCAL_SRC_FILES := $(call all-java-files-under, src)

LOCAL_SDK_VERSION := current

LOCAL_MODULE:= amlogic.tvsubtitle
#LOCAL_JAVA_LIBRARIES := 

LOCAL_PROGUARD_ENABLED := disabled
LOCAL_PROGUARD_FLAGS := -include $(LOCAL_PATH)/proguard.flags

# Generate a checksum that will be used in the app to determine whether the
# firmware in /system/etc/firmware needs to be updated.

#include $(BUILD_JAVA_LIBRARY)
include $(BUILD_STATIC_JAVA_LIBRARY)

##################################################

include $(call all-makefiles-under,$(LOCAL_PATH))
