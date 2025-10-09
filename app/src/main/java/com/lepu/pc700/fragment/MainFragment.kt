package com.lepu.pc700.fragment

import android.annotation.SuppressLint
import android.os.Bundle
import android.os.Environment
import android.view.View
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.Carewell.OmniEcg.jni.toJson
import com.Carewell.ecg700.port.BatteryStatusEvent
import com.Carewell.ecg700.port.LogUtil
import com.Carewell.ecg700.port.observeEvent
import com.Carewell.view.ecg12.LeadType
import com.Carewell.view.ecg12.MainEcgManager
import com.lepu.pc700.App
import com.lepu.pc700.R
import com.lepu.pc700.databinding.FragmentMainBinding
import com.lepu.pc700.net.util.Constant
import com.lepu.pc700.singleClick
import com.lepu.pc700.viewBinding
import com.lepu.pc_700.widget.dialog.EcgPlaybackFragemntDialog
import kotlinx.coroutines.InternalCoroutinesApi
import okhttp3.internal.and
import java.io.BufferedInputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileNotFoundException
import java.io.IOException
import java.io.InputStream

/**
 * 主页
 * zrj
 * 2021/8/3 18:40
 */
class MainFragment : Fragment(R.layout.fragment_main) {

    private val binding by viewBinding(FragmentMainBinding::bind)

    @SuppressLint("SetTextI18n")
    @OptIn(InternalCoroutinesApi::class)
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.tv1.singleClick { findNavController().navigate(R.id.routineExaminationFragment) }
        binding.tv2.singleClick { findNavController().navigate(R.id.ecg12Fragment) }
        binding.tv3.singleClick { findNavController().navigate(R.id.eCGSingleFragment) }
//        binding.tv4.singleClick { findNavController().navigate(R.id.idCardInputFragment) }
        binding.tv4.singleClick { findNavController().navigate(R.id.newIdCardInputFragment) }
        binding.tv5.singleClick { findNavController().navigate(R.id.settingFragment) }
        binding.tv6.singleClick {
            MainEcgManager.getInstance().updateMainEcgShowStyle(LeadType.LEAD_12)
            //读文件的二维数组 赋值
          getECGDataFromFile(Environment.getExternalStorageDirectory().absolutePath + "/PC700/ECG12/120107199910274210/20251009144207.txt")?.let {
              EcgPlaybackFragemntDialog.newInstance(it).show(childFragmentManager, "")
          }


        }
        binding.tv7.singleClick { findNavController().navigate(R.id.bodyFatFragment) }
        //是否充电
        observeEvent<BatteryStatusEvent> {  //没有百分比 只有1-4的分割 0 - 25% 25% - 50% 50% - 75% 75% - 100%
            LogUtil.e(it.toJson())
            binding.tv6.text = "电量等级:${it.chargeLevel},是否充电:${it.ac == 1}"
        }

        App.serial.mAPI?.queryBattery() //可以间隔轮询,也可以在需要的时候调用,拔插电源会主动上报
    }



    /**
     * 获取文件心电数据的二维数组
     */
    fun getECGDataFromFile(filePath: String): Array<ShortArray>? {
        val file = File(filePath)
        val leadChannelCount = 12
        if (!file.exists()) return null
        var inputStream: InputStream? = null
        try {
            inputStream = BufferedInputStream(FileInputStream(file))
            var length = 0
            var value: Short
            val valueArray = ByteArray(2)
            length = inputStream.available()
            val buffer = ByteArray(length)
            val pointSize = length / 2 / leadChannelCount
            val ecgDataItem = Array(leadChannelCount) { ShortArray(pointSize) }
            val simpleDataPackageByteArray = ByteArray(leadChannelCount * 2)
            inputStream.read(buffer, 0, length)
            for (i in 0 until pointSize) {
                System.arraycopy(
                    buffer, i * simpleDataPackageByteArray.size,
                    simpleDataPackageByteArray, 0,
                    simpleDataPackageByteArray.size
                )
                for (j in 0 until leadChannelCount) {
                    valueArray[0] = simpleDataPackageByteArray[j * 2]
                    valueArray[1] = simpleDataPackageByteArray[j * 2 + 1]
                    value = lBytesToShort(valueArray)
                    ecgDataItem[j][i] = value
                }
            }
            return ecgDataItem
        } catch (e: FileNotFoundException) {
            e.printStackTrace()
        } catch (e: IOException) {
            e.printStackTrace()
        } finally {
            if (inputStream != null) { //若is还存在就需要释放，否则不需要释放
                try {
                    inputStream.close()
                } catch (e: IOException) {
                    e.printStackTrace()
                }
            }
        }
        return null
    }

    fun lBytesToShort(valueArray: ByteArray): Short {
        var data = 0
        data = valueArray[0] and 0xff
        data += valueArray[1].toInt() shl 8
        return data.toShort()
    }
}

