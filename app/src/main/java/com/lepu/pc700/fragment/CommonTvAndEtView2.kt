package com.lepu.pc700.fragment

import android.content.Context
import android.text.Editable
import android.text.InputFilter
import android.text.TextUtils
import android.text.TextWatcher
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.inputmethod.EditorInfo
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.res.use
import com.lepu.pc700.R

/**
 *
 * java类作用描述
 * zrj 2021/12/13 19:51
 * 更新者 2021/12/13 19:51
 */
class CommonTvAndEtView2 @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : LinearLayout(context, attrs) {

    private  var tv: TextView?=null
    private  var etValue: EditText?=null
    private  var tvValue: TextView?=null

    init {
        context.obtainStyledAttributes(attrs, R.styleable.CommonTvAndEtView2).use {
            val view =
                LayoutInflater.from(context).inflate(R.layout.view_commont_tv_tv_et2, this, true)
            tv = view.findViewById(R.id.tv)
            setTvText(it.getString(R.styleable.CommonTvAndEtView2_textView_text))
            tvValue = view.findViewById(R.id.tv_value)

            etValue = view.findViewById<EditText>(R.id.et_value)
            val focusable = it.getBoolean(R.styleable.CommonTvAndEtView2_et2_focusable, true)
            etValue?.isCursorVisible = focusable
            etValue?.isFocusable = focusable
            etValue?.isFocusableInTouchMode = focusable
            etValue?.filters = arrayOf<InputFilter>(
                InputFilter.LengthFilter(
                    it.getInt(
                        R.styleable.CommonTvAndEtView2_et2_maxLength,
                        Int.MAX_VALUE
                    )
                )
            )
            etValue?.inputType =
                it.getInt(
                    R.styleable.CommonTvAndEtView2_android_inputType,
                    EditorInfo.TYPE_CLASS_TEXT
                )
        }
    }


    fun setEtFocusable(focusable: Boolean) {
        etValue?.isCursorVisible = focusable
        etValue?.isFocusable = focusable
        etValue?.isFocusableInTouchMode = focusable
    }

    private fun setTvText(title: String?) {
        if (!TextUtils.isEmpty(title)) {
            tv?.text = title
        }
    }

    fun setEtText(title: String?) {
        etValue?.setText(title)
    }

    fun setTvValue(title: String?) {
        if (title != null) {
            tvValue?.text = title
        }
    }
    fun setTvValueColor(color: Int) {
        tvValue?.setTextColor(color)
    }

    fun getEtText() = etValue?.text.toString()
    fun getTvValue() = tvValue?.text.toString()

    fun setTextChangeListener(body: (key: String) -> Unit) {
        etValue?.setTextChangeListener { body(it) }
    }
}

fun EditText.setTextChangeListener(body: (key: String) -> Unit) {

    addTextChangedListener(object : TextWatcher {
        override fun afterTextChanged(s: Editable?) {
        }

        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
        }

        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
            body(s.toString())
        }
    })
}