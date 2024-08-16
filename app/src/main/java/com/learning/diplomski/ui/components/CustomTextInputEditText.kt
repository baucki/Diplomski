package com.learning.diplomski.ui.components

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Rect
import android.util.AttributeSet
import com.google.android.material.textfield.TextInputEditText
import com.learning.diplomski.ui.presentation.OptionsListActivity


class CustomTextInputEditText : TextInputEditText {

    private lateinit var optionList: ArrayList<String>
    private var activityContext: Context? = null
    private var tag: String? = null

    constructor(context: Context) : super(context)
    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs)
    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr)

    fun setOptions(options: ArrayList<String>) {
        this.optionList = options
    }

    fun setActivityContext(context: Context) {
        this.activityContext = context
    }

    fun setTag(tag: String) {
        this.tag = tag
    }

    override fun getTag(): String? {
        return tag
    }

    override fun onFocusChanged(focused: Boolean, direction: Int, previouslyFocusedRect: Rect?) {
        super.onFocusChanged(focused, direction, previouslyFocusedRect)
        if (focused) {
            val intent = Intent(activityContext, OptionsListActivity::class.java)
            intent.putStringArrayListExtra("optionList", optionList)
            intent.putExtra("tag", getTag())
            (activityContext as? Activity)?.startActivityForResult(intent, OPTIONS_LIST_REQUEST_CODE)
        }
    }

    companion object {
        const val OPTIONS_LIST_REQUEST_CODE = 1001
    }
}