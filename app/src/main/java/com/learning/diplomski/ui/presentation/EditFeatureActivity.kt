package com.learning.diplomski.ui.presentation

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.arcgismaps.data.Feature
import com.arcgismaps.data.ServiceFeatureTable
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.textfield.TextInputEditText
import com.learning.diplomski.ui.components.CustomTextInputEditText
import com.learning.diplomski.R
import com.learning.diplomski.data.local.Repository
import com.learning.diplomski.viewmodels.EditViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@AndroidEntryPoint
class EditFeatureActivity : AppCompatActivity() {

    private val viewModel: EditViewModel by viewModels()

    private lateinit var linearLayout: LinearLayout
    private lateinit var saveButton: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_edit_feature)

        initView()
        initListeners()
        initObservers()

    }

    private fun initView() {
        linearLayout = findViewById(R.id.linearLayout)
        Repository.fieldInfoList.clear()
        viewModel.initializeData(linearLayout, this)
        saveButton = findViewById(R.id.button_save)

    }

    private fun initListeners() {
        val coroutineScope = CoroutineScope(Dispatchers.IO)
        saveButton.setOnClickListener {
            viewModel.save(this, coroutineScope ) {
                coroutineScope.launch {
                    updateFeature(it)
                }
            }
        }
    }

    private fun initObservers() {
        viewModel.updateResult.observe(this) { success ->
            if (success) {
                val resultIntent = Intent()
                resultIntent.putExtra("updateSuccess", true)
                setResult(Activity.RESULT_OK, resultIntent)
                finish()
            }
        }
    }

    private suspend fun updateFeature(feature: Feature) {
//        val serviceFeatureTable = feature.featureTable as? ServiceFeatureTable
//        if (serviceFeatureTable != null) {
//            try {
//                serviceFeatureTable.updateFeature(feature).apply {
//                    onSuccess {
//                        serviceFeatureTable.applyEdits()
//                        val resultIntent = Intent()
//                        resultIntent.putExtra("updateSuccess", true)
//                        setResult(Activity.RESULT_OK, resultIntent)
//                        finish()
//                    }
//                    onFailure {
//                        val rootView = findViewById<View>(android.R.id.content)
//                        Snackbar.make(rootView, "Failed to update feature", Snackbar.LENGTH_SHORT).show()
//                    }
//                }
//            } catch (e: Exception) {
//                val rootView = findViewById<View>(android.R.id.content)
//                Snackbar.make(rootView, "An error occurred", Snackbar.LENGTH_SHORT).show()
//            }
//        } else {
//            val rootView = findViewById<View>(android.R.id.content)
//            Snackbar.make(rootView, "Invalid feature table", Snackbar.LENGTH_SHORT).show()
//        }
        viewModel.updateFeature(feature)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == CustomTextInputEditText.OPTIONS_LIST_REQUEST_CODE && resultCode == Activity.RESULT_OK) {
            val selectedOption = data?.getStringExtra("selectedOption")
            val tag = data?.getStringExtra("tag")
            if (viewModel.customMap.containsKey(tag))  {
                val editText = viewModel.customMap[tag]
                editText?.setText(selectedOption)
            }
            if (tag == Repository.typeObject) {
                Repository.selectedKey = selectedOption!!
                clearAllFields()
                updateOptionsList()
            }
        }
        clearFocusFromAllFields()
    }

    private fun updateOptionsList() {
        for (field in Repository.fields) {
            val newCodedValues = viewModel.getCodedValues(field!!.name)
            if (newCodedValues != "") {
                val inputField: CustomTextInputEditText? = viewModel.customMap[field.name]
                if (inputField != null) {
                    viewModel.customMap[field.name]!!.setOptions(ArrayList(newCodedValues.split(",")))
                }
            }
        }
    }

    private fun clearAllFields() {
        for (fieldInfo in Repository.fieldInfoList) {
            val dynamicInputField = findViewById<TextInputEditText>(fieldInfo.id)
            if (fieldInfo.name != Repository.typeObject)
                dynamicInputField.setText("")
        }
    }

    private fun clearFocusFromAllFields() {
        val rootView = findViewById<View>(android.R.id.content)
        rootView.clearFocus()
    }

    companion object {
        const val EDIT_FEATURE_REQUEST_CODE = 1002
    }
}