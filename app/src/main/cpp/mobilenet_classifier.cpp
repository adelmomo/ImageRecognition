// Tencent is pleased to support the open source community by making ncnn available.
//
// Copyright (C) 2020 THL A29 Limited, a Tencent company. All rights reserved.
//
// Licensed under the BSD 3-Clause License (the "License"); you may not use this file except
// in compliance with the License. You may obtain a copy of the License at
//
// https://opensource.org/licenses/BSD-3-Clause
//
// Unless required by applicable law or agreed to in writing, software distributed
// under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR
// CONDITIONS OF ANY KIND, either express or implied. See the License for the
// specific language governing permissions and limitations under the License.

#include <jni.h>
#include "net.h"
#include <android/log.h>
#include <android/asset_manager_jni.h>
#include <jni.h>

ncnn::Net *classifier=nullptr;
int image_size=224;
int classes=1000;

JNIEXPORT jint JNI_OnLoad(JavaVM* vm, void* reserved)
{

    return JNI_VERSION_1_6;
}
JNIEXPORT void JNI_OnUnload(JavaVM* vm, void* reserved)
{
    delete classifier;
}

extern "C"
JNIEXPORT void JNICALL
Java_com_example_ImageRecognition_ImageRecognizer_00024Companion_classifierInit(
        JNIEnv *env, jobject thiz, jobject manager) {
    if(classifier== nullptr) {
        AAssetManager *mgr = AAssetManager_fromJava(env, manager);
        classifier = new ncnn::Net();
        ncnn::Option opt;
        opt.lightmode = true;
        opt.num_threads = 4;
        classifier->opt = opt;
        classifier->load_param(mgr, "classifier.param");
        classifier->load_model(mgr, "classifier.bin");
    }
}
extern "C"
JNIEXPORT jfloatArray JNICALL
Java_com_example_ImageRecognition_ImageRecognizer_00024Companion_classify(JNIEnv *env,
                                                                                 jobject thiz,
                                                                                 jobject image) {
    __android_log_print(ANDROID_LOG_ERROR,"JNI","classify");
    ncnn::Mat input_image=ncnn::Mat::from_android_bitmap_resize(env,image,ncnn::Mat::PIXEL_RGB,image_size,image_size);
    auto ex=classifier->create_extractor();
    ex.set_light_mode(true);
    ex.set_num_threads(4);
    ncnn::Mat out;
    ex.input("input_1",input_image);
    ex.extract("mobilenet_1.00_224",out);

    float *scores=out.row(0);
    jfloatArray res=env->NewFloatArray(classes);
    env->SetFloatArrayRegion(res,0,classes,scores);
    return res;
}
