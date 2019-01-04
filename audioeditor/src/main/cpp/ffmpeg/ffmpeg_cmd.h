//
// Created by mo on 2018/12/20.
//

#include <jni.h>

#ifndef _Included_FFmpeg_Cmd
#define _Included_FFmpeg_Cmd
#ifdef __cplusplus
extern "C" {
#endif

JNIEXPORT jint JNICALL Java_fm_qingting_audioeditor_FFmpegCmd_exec(JNIEnv *, jclass, jint, jobjectArray);

JNIEXPORT void JNICALL Java_fm_qingting_audioeditor_FFmpegCmd_exit(JNIEnv *, jclass);

#ifdef __cplusplus
}
#endif
#endif

void ffmpeg_progress(float progress);