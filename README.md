#  demo地址：https://github.com/zrenjun/pc700sdk



## 1.引入依赖

> ```
> Step 1. Add the JitPack repository to your build file
>   Add it in your root build.gradle at the end of repositories:
> 
>      dependencyResolutionManagement {
> 		  repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
> 		  repositories {
> 			  maven { 
>                 url = uri("https://jitpack.io")
>             }
> 		  }
> 	   }
> 
> Step 2. Add the dependency     
> 
>      // https://jitpack.io/com/github/zrenjun/pc700sdk/1.1.4/pc700sdk-1.1.4.aar
>      dependencies {
> 	       implementation 'com.github.zrenjun:pc700sdk:1.1.14'
> 	   }
> 
> Step 3. Add the ndk
>      ndk {
>           abiFilters.add("arm64-v8a")
>      }
> ```


## 2.设备串口通信流程提示，详例参见demo

> 1. 初始化      
>
>    ```
>    //初始化日志
>    LogUtil.isSaveLog(applicationContext)
>    //初始化bus
>    CommonApp.init(this)
>    //初始化串口
>    serial = SerialViewModel(this)
>    serial.start()
>    ```
>
> 2. 发送命令 
>
>    ```
>    App.serial.mAPI?.startNIBPMeasure()
>    ```
>
> 3. 获取数据
>
>    ```
>    observeEvent<NIBPGetMeasureResultEvent> {
>    
>    }
>    ```

## 3.生成Hl7Xml文件并获取本地心电报告

> 具体参见Ecg12Fragment

1. XmlUtil（输出xml）
2. JniTraditionalAnalysis(本地心电传统分析)
3. EcgDataManager（jpg,pdf）


## 4.切换12导波形展示

> ```
> MainEcgManager.getInstance().updateMainEcgShowStyle(LeadType.LEAD_6)
> ```


## 5.固件升级

> ```
> 具体参见：FirmwareUpgradeDialog
> 注意：有主固件需先升级，每次升级到100%需等待一会儿固件重启
> ```
>

## 6.日志发送

> ```
> 串口日志分享：StreamLogFileManager.share()
> ```
>

## 7.OTG开关

> ```
> host/device模式：sendBroadcast(Intent("android.intent.action.${if (flag) "enablehostmode" else "disablehostmode"}"))
> ```
> 
