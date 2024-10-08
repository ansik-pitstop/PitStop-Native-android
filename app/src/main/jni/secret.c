#include <jni.h>

// Pitstop endpoints

JNIEXPORT jstring JNICALL
Java_com_pitstop_utils_SecretUtils_getPitstopEndpointStaging(JNIEnv *env, jobject instance) {
    return (*env) ->  NewStringUTF(env, "http://staging.api.getpitstop.io:10010/");
}

JNIEXPORT jstring JNICALL
Java_com_pitstop_utils_SecretUtils_getPitstopEndpointSnapshot(JNIEnv *env, jobject instance) {
    return (*env) ->  NewStringUTF(env, "http://snapshot.api.getpitstop.io:10011/");
}

JNIEXPORT jstring JNICALL
Java_com_pitstop_utils_SecretUtils_getPitstopEndpointRelease(JNIEnv *env, jobject instance) {
    return (*env) ->  NewStringUTF(env, "https://api.getpitstop.io/");
}

// Pitstop client ids

JNIEXPORT jstring JNICALL
Java_com_pitstop_utils_SecretUtils_getPitstopClientIdDebug(JNIEnv *env, jobject instance) {
    return (*env) ->  NewStringUTF(env, "DINCPNWtqjjG69xfMWuF8BIJ8QjwjyLwCq36C19CkTIMkFnE6zSxz7Xoow0aeq8M6Tlkybu8gd4sDIKD");
}

JNIEXPORT jstring JNICALL
Java_com_pitstop_utils_SecretUtils_getPitstopClientIdRelease(JNIEnv *env, jobject instance) {
    return (*env) ->  NewStringUTF(env, "8pLSeDGJdlW89dzfbfI4uNr0x2Blo4FG8TEkCB5LXKgtfVf8");
}

// Mixpanel

JNIEXPORT jstring JNICALL
Java_com_pitstop_utils_SecretUtils_getMixpanelTokenDev(JNIEnv *env, jobject instance) {
    return (*env) ->  NewStringUTF(env, "d3461bce822d1c8d94a9d71d37010333");
}

JNIEXPORT jstring JNICALL
Java_com_pitstop_utils_SecretUtils_getMixpanelTokenProd(JNIEnv *env, jobject instance) {
    return (*env) ->  NewStringUTF(env, "02b8b23d15c94812afac29c9513f0008");
}

// Smooch

JNIEXPORT jstring JNICALL
Java_com_pitstop_utils_SecretUtils_getSmoochTokenDev(JNIEnv *env, jobject instance) {
    return (*env) ->  NewStringUTF(env, "59a8225230a0873400f8615f");
}

JNIEXPORT jstring JNICALL
Java_com_pitstop_utils_SecretUtils_getSmoochTokenProd(JNIEnv *env, jobject instance) {
    return (*env) ->  NewStringUTF(env, "59a8188b9a42833a005d0bf8");
}

// Parse

JNIEXPORT jstring JNICALL
Java_com_pitstop_utils_SecretUtils_getParseAppIdDev(JNIEnv *env, jobject instance) {
    return (*env) ->  NewStringUTF(env, "ZYOmoKuYsa2LtOKPnBW7dGLNpYbYhZGHb6EBfl3S");
}

JNIEXPORT jstring JNICALL
Java_com_pitstop_utils_SecretUtils_getParseAppIdProd(JNIEnv *env, jobject instance) {
    return (*env) ->  NewStringUTF(env, "uURx2iGflDgd5SUydxUdCUDjL6jfj4qHIPeNcEeb");
}

JNIEXPORT jstring JNICALL
Java_com_pitstop_utils_SecretUtils_getParseClientIdDev(JNIEnv *env, jobject instance) {
    return (*env) ->  NewStringUTF(env, "xrxqVtdLZP9QBa7eXx7ZVizrlFRHqmU5UGzcNfVB");
}

JNIEXPORT jstring JNICALL
Java_com_pitstop_utils_SecretUtils_getParseClientIdProd(JNIEnv *env, jobject instance) {
    return (*env) ->  NewStringUTF(env, "QAXIaHDJ6xX82ZX62zL3C2dQTEikd4PZMSjlGAdb");
}

// Mashape

JNIEXPORT jstring JNICALL
Java_com_pitstop_utils_SecretUtils_getMashapeKey(JNIEnv *env, jobject instance) {
    return (*env) ->  NewStringUTF(env, "Rg3YqzHEJmmshZIoHi4smSVbZvudp1ywHEEjsn0dR4DyKVyF5z");
}

//Google sender id
JNIEXPORT jstring JNICALL
Java_com_pitstop_utils_SecretUtils_getGoogleSenderId(JNIEnv *env, jobject instance) {
    return (*env) ->  NewStringUTF(env, "309712221750");
}

// SnapToRoad endpoints

JNIEXPORT jstring JNICALL
Java_com_pitstop_utils_SecretUtils_getSnapToRoadEndpointStaging(JNIEnv *env, jobject instance) {
    return (*env) ->  NewStringUTF(env, "https://roads.googleapis.com/");
}

JNIEXPORT jstring JNICALL
Java_com_pitstop_utils_SecretUtils_getSnapToRoadEndpointSnapshot(JNIEnv *env, jobject instance) {
    return (*env) ->  NewStringUTF(env, "https://roads.googleapis.com/");
}

JNIEXPORT jstring JNICALL
Java_com_pitstop_utils_SecretUtils_getSnapToRoadEndpointRelease(JNIEnv *env, jobject instance) {
    return (*env) ->  NewStringUTF(env, "https://roads.googleapis.com/");
}

// Maps API Key

JNIEXPORT jstring JNICALL
Java_com_pitstop_utils_SecretUtils_getMapsApiKeyStaging(JNIEnv *env, jobject instance) {
    return (*env) ->  NewStringUTF(env, "AIzaSyDW84AecyYE0rvSFHregjW-a0tRE0-nzFU");
}

JNIEXPORT jstring JNICALL
Java_com_pitstop_utils_SecretUtils_getMapsApiKeySnapshot(JNIEnv *env, jobject instance) {
    return (*env) ->  NewStringUTF(env, "AIzaSyDW84AecyYE0rvSFHregjW-a0tRE0-nzFU");
}

JNIEXPORT jstring JNICALL
Java_com_pitstop_utils_SecretUtils_getMapsApiKeyRelease(JNIEnv *env, jobject instance) {
    return (*env) ->  NewStringUTF(env, "AIzaSyDW84AecyYE0rvSFHregjW-a0tRE0-nzFU");
}
