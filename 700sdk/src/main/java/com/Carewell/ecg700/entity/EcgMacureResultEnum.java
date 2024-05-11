package com.Carewell.ecg700.entity;

/**
 * @author wxd
 */

public enum EcgMacureResultEnum {
    TYPE_NONE,//无算法,未分析.也可能是人工下诊断，点击结果。
    TYPE_LOCAL_TRADITIONAL_RESULT,//本地传统算法诊断结果
    TYPE_LOCAL_HRV_RESULT,//HRV，诊断算法结果
    TYPE_LOCAL_AI,//本机 ai 算法结果
    TYPE_NET_AI,//上传云端ai，诊断算法结果
    TYPE_GLASGOW_RESULT,//glasgow算法，诊断算法结果
}
