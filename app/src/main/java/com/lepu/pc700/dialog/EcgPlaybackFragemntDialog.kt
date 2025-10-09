package com.lepu.pc_700.widget.dialog

import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.View
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.lifecycleScope
import com.Carewell.view.ecg12.EcgScrollDirection
import com.Carewell.view.ecg12.MainEcgManager
import com.Carewell.view.ecg12.PreviewManager
import com.Carewell.view.other.LoadingForView
import com.lepu.pc700.R
import com.lepu.pc700.databinding.DialogEcgUploadBinding
import com.lepu.pc700.singleClick
import com.lepu.pc700.viewBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * 12导上传提示
 * zrj
 * 2021/12/24 14:43
 */
class EcgPlaybackFragemntDialog : DialogFragment(R.layout.dialog_ecg_upload) {
    private val binding by viewBinding(DialogEcgUploadBinding::bind)
    private lateinit var loading: LoadingForView
    private var ecgImageWidth = 0
    private var ecgImageHeight = 0

    companion object {
        private var mDrawEcgDataAll: Array<ShortArray>? = null
        fun newInstance(data: Array<ShortArray>): EcgPlaybackFragemntDialog {
            mDrawEcgDataAll = data
            return EcgPlaybackFragemntDialog()
        }
    }

    override fun onStart() {
        super.onStart()
        dialog?.setCanceledOnTouchOutside(false)
        dialog?.setCancelable(false)
        dialog?.window?.setLayout(
            (0.75 * resources.displayMetrics.widthPixels).toInt(),
            (0.90 * resources.displayMetrics.heightPixels).toInt()
        )
        dialog?.window?.decorView?.setPadding(0, 0, 0, 0)
        dialog?.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        loading = LoadingForView(requireContext(), binding.viewGroup)
        loading.show()
        binding.ecgShowImageView.post {
            ecgImageHeight = binding.ecgShowImageView.height
            ecgImageWidth = binding.ecgShowImageView.width
            resetEcgImage()
        }
        binding.ecgShowImageView.setOnOMoveListener { x ->
            when {
                x < 0 -> PreviewManager.getInstance().baseEcgPreviewTemplate.leadManager
                    .setEcgScrollDirection(EcgScrollDirection.LEFT)
                x > 0 -> PreviewManager.getInstance()
                    .baseEcgPreviewTemplate.leadManager
                    .setEcgScrollDirection(EcgScrollDirection.RIGHT)
                else -> return@setOnOMoveListener
            }
            PreviewManager.getInstance().getCurrentScrrenDrawData(
                mDrawEcgDataAll,
                binding.ecgShowImageView.width,
                -x,
                MainEcgManager.getInstance().leadSpeedType
            )

            val bitmap = updateEcgImage()
            binding.ecgShowImageView.refrshView(bitmap)
            binding.dataBar.updateSlider(PreviewManager.getInstance().dataRatio)
        }

        binding.btnCancel.singleClick {
            dismiss()
        }
    }
    /**
     * 更新心电图数据绘制
     */
    private fun updateEcgImage(): Bitmap {
        //获取心电图模板
        val baseEcgPreviewTemplate = PreviewManager.getInstance().baseEcgPreviewTemplate
        baseEcgPreviewTemplate.drawEcgPathPreview()
        baseEcgPreviewTemplate.drawLeadInfo()
        //绘制其它信息
        return baseEcgPreviewTemplate.bgBitmap
    }


    /**
     * 重置心电图
     */
    private fun resetEcgImage() {
        lifecycleScope.launch {
            withContext(Dispatchers.IO) {
                PreviewManager.getInstance()
                    .resetDrawEcg(ecgImageWidth, ecgImageHeight)
                PreviewManager.getInstance().clearCurrentScreenData()
                val leadSpeedType = MainEcgManager.getInstance().leadSpeedType
                PreviewManager.getInstance().getCurrentScrrenDrawData(
                    mDrawEcgDataAll,
                    ecgImageWidth, 0f, leadSpeedType
                )
                val gridSpace =
                    ecgImageHeight / (BIG_GRID_COUNT *SMALL_GRID_COUNT)
                val displayCount =
                    (ecgImageWidth - gridSpace * 5).toFloat() / PreviewManager.getInstance().baseEcgPreviewTemplate.leadColumes / gridSpace.toFloat()
                val size = mDrawEcgDataAll?.get(0)?.size ?: 0
                val totalCount = (leadSpeedType.value as Float) * size / PreviewManager.SAMPLE_RATE
                val bitmap = updateEcgImage()
                withContext(Dispatchers.Main) {
                    binding.dataBar.initSliderLen(displayCount, totalCount)
                    binding.dataBar.updateSlider(PreviewManager.getInstance().dataRatio)
                    binding.ecgShowImageView.refrshView(bitmap)
                    loading.dismiss()
                }
            }
        }
    }

    val BIG_GRID_COUNT: Int = 30 //纵向大网格数量 30
    val SMALL_GRID_COUNT: Int = 5 //小网格数量 5
}

