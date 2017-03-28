# Copyright (C) 2010 The Android Open Source Project
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#      http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
#
# $Id: Android.mk 165 2012-12-28 19:55:23Z oparviai $

LOCAL_PATH := $(call my-dir)

include $(CLEAR_VARS)

# *** Remember: Change -O0 into -O2 in add-applications.mk ***

LOCAL_MODULE    := soundtouch
LOCAL_SRC_FILES := soundtouch-jni.cpp soundtouch/source/SoundTouch/AAFilter.cpp  soundtouch/source/SoundTouch/FIFOSampleBuffer.cpp \
                soundtouch/source/SoundTouch/FIRFilter.cpp soundtouch/source/SoundTouch/cpu_detect_x86.cpp \
                soundtouch/source/SoundTouch/RateTransposer.cpp soundtouch/source/SoundTouch/SoundTouch.cpp \
                soundtouch/source/SoundTouch/TDStretch.cpp soundtouch/source/SoundTouch/BPMDetect.cpp soundtouch/source/SoundTouch/PeakFinder.cpp \
				soundtouch/source/SoundTouch/InterpolateLinear.cpp soundtouch/source/SoundTouch/InterpolateCubic.cpp soundtouch/source/SoundTouch/InterpolateShannon.cpp
# for native audio
LOCAL_LDLIBS    += -lgcc 
LOCAL_C_INCLUDES += $(LOCAL_PATH)/soundtouch/include
# --whole-archive -lgcc 
# for logging
LOCAL_LDLIBS    += -llog
# for native asset manager
#LOCAL_LDLIBS    += -landroid
# don't export all symbols
# added "-marm" switch to use arm instruction set instead of thumb for improved calculation performance.
LOCAL_CFLAGS += -Wall -fvisibility=hidden -I soundtouch/source/../include -D ST_NO_EXCEPTION_HANDLING -fdata-sections -ffunction-sections -marm

include $(BUILD_SHARED_LIBRARY)
