#include <jni.h>
#include <string>
#include "sigleEcg.c"
#include <android/log.h>
#define  TAG    "sigleEcg"


#include <unistd.h>
#include <sys/stat.h>
#include <ctime>
#include <cstdlib>
#include <fcntl.h>


#include <deque>
#include <cstdlib>
#include <cstring>

#include "streamswtqua.h"
#include "streamswtqua.cpp"
#include "commalgorithm.h"
#include "commalgorithm.cpp"
#include "swt.h"
#include "swt.cpp"
#include <cassert>




extern "C"
JNIEXPORT jint JNICALL
Java_com_Carewell_ecg700_ParseData_hpFilter(JNIEnv *env, jobject thiz, jint data_in, jint init) {
	return HPFilter_05Hz(data_in,init);
}


extern "C"
JNIEXPORT jshortArray JNICALL
Java_com_Carewell_ecg700_ParseData_shortFilter(JNIEnv *env, jobject thiz, jshortArray inShorts) {

	short *shortArray;
	jsize arraySize;
	arraySize = (*env).GetArrayLength(inShorts);

	deque <double > inputt;
	deque <double > realInput;		// Panjie: 实际进行分析的数据

	inputt.clear();

	jboolean *isCopy = (jboolean *)malloc(sizeof(jboolean));
	shortArray =(*env).GetShortArrayElements(inShorts, isCopy);
	for(int j = 0; j < arraySize; j++ )
	{
		inputt.push_back((jdouble) shortArray[j]);
	}
	int inputLength = (int) inputt.size();
//    __android_log_print(ANDROID_LOG_VERBOSE, APPNAME, "doubleArraySize == %d", inputLength);

	int i = 0;
	int j = 0;
	int flag = 0;

	StreamSwtQua streamSwtQua;
	deque <double> outputPoints;
	deque <double> allSig;
	deque <double> outputsize;

	int lenthOfData		= 0;
	int ReduntLength	= 0;
	int MultipleSize		= 0;
	int padDataLen		= 0;		// Panjie: 需要补充的数据点数

	lenthOfData = inputt.size();
	MultipleSize = lenthOfData / 256;
	ReduntLength = lenthOfData - 256 * MultipleSize;

	padDataLen = (MultipleSize + 1) * 256 - lenthOfData;

	for (j = 0; j < lenthOfData; ++j)
	{
		realInput.push_back(inputt[j]);
	}

	// 数据补零
	if (0 != ReduntLength)
	{
		if (padDataLen < 64)
		{
			flag = 1;

			for (j = lenthOfData - 1; j >= lenthOfData - padDataLen; j--)
			{
				realInput.push_back(inputt[j]);
			}

			for (j = 256 * (MultipleSize + 1) - 128; j < 256 * (MultipleSize + 1); j++)
			{
				realInput.push_back(realInput[j]);
			}
		}
		else
		{
			for (j = lenthOfData - 1; j >= lenthOfData - padDataLen; j--)
			{
				realInput.push_back(inputt[j]);
			}
		}
	}

	if (0 == ReduntLength)
	{
		for (i = 0; i < 256 * MultipleSize; ++i)
		{
			streamSwtQua.GetEcgData(realInput[i], outputPoints);

			for (j = 0; j < outputPoints.size(); ++j)
			{
				allSig.push_back(outputPoints[j]);
			}
		}

		for (i = 256 * MultipleSize - 128; i < 256 * MultipleSize; ++i)
		{
			streamSwtQua.GetEcgData(inputt[i], outputPoints);
		}

		for (j = 0; j < 64; j++)
		{
			allSig.push_back(outputPoints[j]);
		}
	}
	else
	{
		for (i = 0; i < realInput.size(); i++)
		{
			streamSwtQua.GetEcgData(realInput[i], outputPoints);

			for (j = 0; j < outputPoints.size(); ++j)
			{
				allSig.push_back(outputPoints[j]);
			}
		}

		if (ReduntLength < 192)
		{
			for (i = 0; i < 192 - ReduntLength; i++)
			{
				allSig.pop_back();
			}
		}

		if (1 == flag)
		{
			for (i = 0; i < 64 + padDataLen; i++)
			{
				allSig.pop_back();
			}
		}
	}


	long length = allSig.size();
//    __android_log_print(ANDROID_LOG_VERBOSE, APPNAME, "length == %ld", length);
	short array[length];
	for(i = 0; i < length; i++)
	{
		array[i] = (short) allSig[i];
	}

	jsize size = (jsize) allSig.size();
	jshortArray result = (*env).NewShortArray(size);
	(*env).SetShortArrayRegion(result, 0, size, array);

	return result;
}

static StreamSwtQua streamSwtQua;

extern "C"
JNIEXPORT jdoubleArray JNICALL
Java_com_Carewell_ecg700_ParseData_offlineFilter(JNIEnv *env, jclass clazz, jdouble f, jboolean reset) {

	if (reset) {
		streamSwtQua.ResetMe();
		return (*env).NewDoubleArray(0);
	}


	deque <double> outputPoints;
	streamSwtQua.GetEcgData(f, outputPoints);
	double *arrays = 0;
	if(outputPoints.empty())
	{
		arrays = (double *)malloc(sizeof(double)*7);
		memset(arrays,'\0',sizeof(arrays));
	} else
	{
		arrays = (double *)malloc(sizeof(double)*outputPoints.size());
		for(int i = 0; i < outputPoints.size(); i++ )
		{
			arrays[i] = outputPoints[i];
		}
	}

	long length = outputPoints.size();
//    __android_log_print(ANDROID_LOG_VERBOSE, APPNAME, "length == %ld", length);

	jsize size = (jsize) outputPoints.size();
	jdoubleArray result = (*env).NewDoubleArray(size);
	(*env).SetDoubleArrayRegion(result, 0, size, arrays);

	return result;
}

JNIEXPORT jint JNI_OnLoad(JavaVM* vm, void* reserved) {
	JNIEnv* env;
	if (vm->GetEnv(reinterpret_cast<void**>(&env), JNI_VERSION_1_6) != JNI_OK) {
		return -1;
	}
	streamSwtQua = StreamSwtQua();
	// Get jclass with env->FindClass.
	// Register methods with env->RegisterNatives.

	return JNI_VERSION_1_6;
}