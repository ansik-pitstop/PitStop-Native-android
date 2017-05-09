LOCAL_PATH := $(call my-dir)

include $(CLEAR_VARS)

LOCAL_MODULE    := secret
LOCAL_SRC_FILES := secret.c

include $(BUILD_SHARED_LIBRARY)