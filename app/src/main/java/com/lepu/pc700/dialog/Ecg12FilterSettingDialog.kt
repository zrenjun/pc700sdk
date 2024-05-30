package com.lepu.pc700.dialog

import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.View
import androidx.fragment.app.DialogFragment
import com.Carewell.ecg700.ParseEcg12Data
import com.lepu.pc700.R
import com.lepu.pc700.databinding.DialogEcg12FilterSettingsBinding
import com.lepu.pc700.singleClick
import com.lepu.pc700.viewBinding


class Ecg12FilterSettingDialog : DialogFragment(R.layout.dialog_ecg12_filter_settings) {

    override fun onStart() {
        super.onStart()
        dialog?.setCanceledOnTouchOutside(false)
        dialog?.setCancelable(false)
        dialog?.window?.setLayout(
            (0.5 * resources.displayMetrics.widthPixels).toInt(),
            (0.6 * resources.displayMetrics.heightPixels).toInt()
        )
        dialog?.window?.decorView?.setPadding(0, 0, 0, 0)
        dialog?.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
    }

    private val binding by viewBinding(DialogEcg12FilterSettingsBinding::bind)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        with(binding) {
            rgPacemaker.check(if (ParseEcg12Data.isAddPacemaker) R.id.rb_pm_on else R.id.rb_pm_off)
            btnCancel.singleClick {
                dismiss()
            }
            btnSure.singleClick {
                val hpf = when (rgHpf.checkedRadioButtonId) {
                    R.id.rb_hpf_001 -> 0.01f
                    R.id.rb_hpf_005 -> 0.05f
                    R.id.rb_hpf_032 -> 0.32f
                    R.id.rb_hpf_067 -> 0.67f
                    else -> 0f
                }
                val lpf = when (rgLpf.checkedRadioButtonId) {
                    R.id.rb_lpf_25 -> 25
                    R.id.rb_lpf_35 -> 35
                    R.id.rb_lpf_45 -> 45
                    R.id.rb_lpf_75 -> 75
                    R.id.rb_lpf_100 -> 100
                    R.id.rb_lpf_150 -> 150
                    R.id.rb_lpf_300 -> 300
                    else -> 0
                }
                val pff = when (rgPff.checkedRadioButtonId) {
                    R.id.rb_pff_50 -> 50
                    R.id.rb_pff_60 -> 60
                    else -> 0
                }
                onAdoptListener?.invoke(lpf, hpf, pff, rbPmOn.isChecked)
                dismiss()
            }
        }
    }

    private var onAdoptListener: ((lowPassHz: Int, hpHz: Float, acHz: Int, isAddPaceMaker: Boolean) -> Unit)? = null

    fun setOnAdoptListener(l: ((lowPassHz: Int, hpHz: Float, acHz: Int, isAddPaceMaker: Boolean) -> Unit)): Ecg12FilterSettingDialog {
        this.onAdoptListener = l
        return this
    }
}