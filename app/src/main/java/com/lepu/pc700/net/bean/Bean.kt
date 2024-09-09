package com.lepu.pc700.net.bean

import com.lepu.pc700.net.util.Constant


/**
 *
 *  说明: model
 *
 */
class AnalysisId {
    var analysis_id: String? = null
}

class EcgResult {
    var report_url: String? = null
    var analysis_result: AnalysisResult? = null
}
class AnalysisResult {
    var measurements: Measurements? = null
    var diagnosticList = listOf<Diagnostic>()
}
//  "hr": 81,
//                "pd": 145,
//                "pr": 178,
//                "qt": 392,
//                "rr": 749,
//                "td": 192,
//                "qrs": 93,
//                "qtc": 452,
//                "rv1": 337,
//                "rv5": 337,
//                "rv6": 337,
//                "sv1": 0,
//                "sv2": 0,
//                "sv5": 0,
//                "maxHR": 110,
//                "minHR": 63,
//                "axis_P": 47,
//                "axis_T": -129,
//                "axis_QRS": 47
class Measurements {
    var hr = 0
    var pd = 0
    var pr = 0
    var qt = 0
    var rr = 0
    var td = 0
    var qrs = 0
    var qtc = 0
    var rv1 = 0
    var rv5 = 0
    var rv6 = 0
    var sv1 = 0
    var sv2 = 0
    var sv5 = 0
    var maxHR = 0
    var minHR = 0
    var axis_P = 0
    var axis_T = 0
    var axis_QRS = 0
}
//  {
//                    "code": "821",
//                    "content": "窦性心律不齐",
//                    "leadInvolved": ""
//                }
class Diagnostic {
    var code = ""
    var content = ""
}


//{
//    "ecg": {
//        "duration": 370, //分析时长，单位s
//        "sample_rate": 125, //采样率，如125
//        "measure_time": "2022-02-03 13:29:55" //测量时间(YYYY-MM-DD HH:mm:ss格式，如2022-02-04 13:29:55),
//    },
//    "user": {
//        "name": "test1", //姓名，必填
//        "phone": "", //手机号，必填
//        "gender": "1", //性别（1：男，2：女）,对于签字报告必填
//        "birthday": "1991-08-13",//出生日期（yyyy-mm-dd）,对于签字报告必填
//        "id_number": "" //身份证号，对于签字报告必填
//    },
//    "device": {
//        "sn": "236xxxx", //设备sn
//        "band": "Lepu", //品牌
//        "model": "PC-700" //型号，从下列设备列表中查找对应正确的model(区分大小写)
//    },
//    "access_token": "", //对应服务能力的AccessToken
//    "analysis_type": "1-12", //分析类型，“-”前表示1-短程，“-”后表示导联类型,12-十二导
//    "application_id": "xxx.xxx.xx", //应用id(应用包名,注意格式)
//    "service_ability": 1 // 1 AI分析， 2 医生签字报告
//   }


class EcgInfo {
    var ecg: Ecg? = null
    var user: User? = null
    var device: Device? = null
    var access_token = Constant.token
    var analysis_type = "1-12"
    var application_id = "com.viatom.test"
    var service_ability = 1
}

class Ecg {
    var duration = 30
    var sample_rate = 1000
    var measure_time = ""
}

class User {
    var name = ""
    var phone = ""
    var gender = ""
    var birthday = ""
    var id_number = ""
}

class Device {
    var sn = ""
    var band = "Lepu"
    var model = "PC-700"
}