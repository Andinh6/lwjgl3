/*
 * Copyright LWJGL. All rights reserved.
 * License terms: https://www.lwjgl.org/license
 * MACHINE GENERATED FILE, DO NOT EDIT
 */
#include "common_tools.h"
#include "lwjgl_malloc.h"
#define NVG_MALLOC(sz)     org_lwjgl_malloc(sz)
#define NVG_REALLOC(p,sz)  org_lwjgl_realloc(p,sz)
#define NVG_FREE(p)        org_lwjgl_free(p)
DISABLE_WARNINGS()
#define NANOVG_GL3_IMPLEMENTATION
#include "nanovg.h"
#include "nanovg_gl.h"
#include "nanovg_gl_utils.h"
ENABLE_WARNINGS()

EXTERN_C_ENTER

JNIEXPORT jint JNICALL JavaCritical_org_lwjgl_nanovg_NanoVGGL3_nnvglCreateImageFromHandle(jlong ctxAddress, jint textureId, jint w, jint h, jint flags) {
    NVGcontext *ctx = (NVGcontext *)(intptr_t)ctxAddress;
    return (jint)nvglCreateImageFromHandleGL3(ctx, (GLuint)textureId, w, h, flags);
}
JNIEXPORT jint JNICALL Java_org_lwjgl_nanovg_NanoVGGL3_nnvglCreateImageFromHandle(JNIEnv *__env, jclass clazz, jlong ctxAddress, jint textureId, jint w, jint h, jint flags) {
    UNUSED_PARAMS(__env, clazz)
    return JavaCritical_org_lwjgl_nanovg_NanoVGGL3_nnvglCreateImageFromHandle(ctxAddress, textureId, w, h, flags);
}

JNIEXPORT jint JNICALL JavaCritical_org_lwjgl_nanovg_NanoVGGL3_nnvglImageHandle(jlong ctxAddress, jint image) {
    NVGcontext *ctx = (NVGcontext *)(intptr_t)ctxAddress;
    return (jint)nvglImageHandleGL3(ctx, image);
}
JNIEXPORT jint JNICALL Java_org_lwjgl_nanovg_NanoVGGL3_nnvglImageHandle(JNIEnv *__env, jclass clazz, jlong ctxAddress, jint image) {
    UNUSED_PARAMS(__env, clazz)
    return JavaCritical_org_lwjgl_nanovg_NanoVGGL3_nnvglImageHandle(ctxAddress, image);
}

JNIEXPORT jlong JNICALL Java_org_lwjgl_nanovg_NanoVGGL3_nnvgCreate(JNIEnv *__env, jclass clazz, jint flags) {
    UNUSED_PARAM(clazz)
    return (jlong)(intptr_t)nvgCreateGL3(__env, flags);
}

JNIEXPORT void JNICALL JavaCritical_org_lwjgl_nanovg_NanoVGGL3_nnvgDelete(jlong ctxAddress) {
    NVGcontext *ctx = (NVGcontext *)(intptr_t)ctxAddress;
    nvgDeleteGL3(ctx);
}
JNIEXPORT void JNICALL Java_org_lwjgl_nanovg_NanoVGGL3_nnvgDelete(JNIEnv *__env, jclass clazz, jlong ctxAddress) {
    UNUSED_PARAMS(__env, clazz)
    JavaCritical_org_lwjgl_nanovg_NanoVGGL3_nnvgDelete(ctxAddress);
}

JNIEXPORT jlong JNICALL JavaCritical_org_lwjgl_nanovg_NanoVGGL3_nnvgluCreateFramebuffer(jlong ctxAddress, jint w, jint h, jint imageFlags) {
    NVGcontext *ctx = (NVGcontext *)(intptr_t)ctxAddress;
    return (jlong)(intptr_t)nvgluCreateFramebufferGL3(ctx, w, h, imageFlags);
}
JNIEXPORT jlong JNICALL Java_org_lwjgl_nanovg_NanoVGGL3_nnvgluCreateFramebuffer(JNIEnv *__env, jclass clazz, jlong ctxAddress, jint w, jint h, jint imageFlags) {
    UNUSED_PARAMS(__env, clazz)
    return JavaCritical_org_lwjgl_nanovg_NanoVGGL3_nnvgluCreateFramebuffer(ctxAddress, w, h, imageFlags);
}

JNIEXPORT void JNICALL JavaCritical_org_lwjgl_nanovg_NanoVGGL3_nnvgluBindFramebuffer(jlong ctxAddress, jlong fbAddress) {
    NVGcontext *ctx = (NVGcontext *)(intptr_t)ctxAddress;
    NVGLUframebuffer *fb = (NVGLUframebuffer *)(intptr_t)fbAddress;
    nvgluBindFramebufferGL3(ctx, fb);
}
JNIEXPORT void JNICALL Java_org_lwjgl_nanovg_NanoVGGL3_nnvgluBindFramebuffer(JNIEnv *__env, jclass clazz, jlong ctxAddress, jlong fbAddress) {
    UNUSED_PARAMS(__env, clazz)
    JavaCritical_org_lwjgl_nanovg_NanoVGGL3_nnvgluBindFramebuffer(ctxAddress, fbAddress);
}

JNIEXPORT void JNICALL JavaCritical_org_lwjgl_nanovg_NanoVGGL3_nnvgluDeleteFramebuffer(jlong ctxAddress, jlong fbAddress) {
    NVGcontext *ctx = (NVGcontext *)(intptr_t)ctxAddress;
    NVGLUframebuffer *fb = (NVGLUframebuffer *)(intptr_t)fbAddress;
    nvgluDeleteFramebufferGL3(ctx, fb);
}
JNIEXPORT void JNICALL Java_org_lwjgl_nanovg_NanoVGGL3_nnvgluDeleteFramebuffer(JNIEnv *__env, jclass clazz, jlong ctxAddress, jlong fbAddress) {
    UNUSED_PARAMS(__env, clazz)
    JavaCritical_org_lwjgl_nanovg_NanoVGGL3_nnvgluDeleteFramebuffer(ctxAddress, fbAddress);
}

EXTERN_C_EXIT
