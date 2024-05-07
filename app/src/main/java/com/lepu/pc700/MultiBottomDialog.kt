package com.lepu.pc700

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.View
import androidx.fragment.app.DialogFragment
import com.lepu.pc700.databinding.DialogMeasureBottomSettingBinding
import com.lepu.pc700.utils.singleClick
import com.lepu.pc700.utils.viewBinding


class MultiBottomDialog : DialogFragment(R.layout.dialog_measure_bottom_setting) {

    private val binding by viewBinding(DialogMeasureBottomSettingBinding::bind)
    @SuppressLint("SetTextI18n")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        //点击事件
        binding.rbYicheng.singleClick {
            mListener?.onClickCallback(CheckType.YICHENG)
            App.serial.mAPI?.setGluType(1)
        }
        binding.rbBaijie.singleClick {
            mListener?.onClickCallback(CheckType.BAIJIE)
            App.serial.mAPI?.setGluType(2)
        }
        binding.rbAiaole.singleClick {
            mListener?.onClickCallback(CheckType.AOAILE)
            App.serial.mAPI?.setGluType(3)
        }

        binding.rbLepu.singleClick {
            mListener?.onClickCallback(CheckType.LEPU)
            App.serial.mAPI?.setGluType(4)
        }

        binding.rbGluUnitMmol.singleClick {
            mListener?.onClickCallback(CheckType.MMOL)
        }
        binding.rbGluUnitMgdl.singleClick {
            mListener?.onClickCallback(CheckType.MGDL)
        }

        binding.rbNibpUnitMmhg.singleClick {
            mListener?.onClickCallback(CheckType.MMHG)
        }
        binding.rbNibpUnitKpa.singleClick {
            mListener?.onClickCallback(CheckType.KPA)
        }
        val tempUnit = 1 // 1.摄氏度 , 2:华氏度
        binding.rbTempEar.singleClick {
            mListener?.onClickCallback(CheckType.ER_TEMP)
            App.serial.mAPI?.switchTemperature(1, tempUnit)
        }
        binding.rbTempForeheadAdult.singleClick {
            mListener?.onClickCallback(CheckType.E_TEMP)
            App.serial.mAPI?.switchTemperature(2, tempUnit)
        }
        binding.rbTempForeheadChild.singleClick {
            mListener?.onClickCallback(CheckType.CHILD_TEMP)
            App.serial.mAPI?.switchTemperature(3, tempUnit)
        }
        binding.rbTempObject.singleClick {
            mListener?.onClickCallback(CheckType.OGJECT_TEMP)
            App.serial.mAPI?.switchTemperature(4, tempUnit)
        }
        binding.btnClose.singleClick {
            dismiss()
        }

        this.isCancelable = false
        dialog?.setCanceledOnTouchOutside(false)//点击屏幕不消失
    }

    @Suppress("unused")
    enum class CheckType {
        YICHENG,
        BAIJIE,
        AOAILE,
        LEPU,
        MMOL,
        MGDL,
        MMHG,
        KPA,
        KRKNIBP,
        JXHNIBP,
        ER_TEMP,
        E_TEMP,
        CHILD_TEMP,
        OGJECT_TEMP,
    }

    private var mListener: IOKClickListener? = null

    interface IOKClickListener {
        fun onClickCallback(checkType: CheckType?) //测量值回调
    }

    fun setOKClickListener(okClickListener: IOKClickListener?) {
        mListener = okClickListener

    }
}