LOCAL_PATH := $(call my-dir)

include $(CLEAR_VARS)
LOCAL_MODULE := g
LOCAL_SRC_FILES := /home/hossein/AndroidStudioProjects/juggluco/Common/build/intermediates/cxx/Debug/122t3f1d/obj/x86_64/libg.so
LOCAL_CPP_FEATURES := rtti exceptions
LOCAL_EXPORT_LDLIBS := -lc++_static -lc -llog -lGLESv3 -landroid -lEGL -lm -latomic -lz -ldl
include $(PREBUILT_SHARED_LIBRARY)

