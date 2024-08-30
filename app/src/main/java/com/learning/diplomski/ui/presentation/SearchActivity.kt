package com.learning.diplomski.ui.presentation

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.arcgismaps.data.CodedValueDomain
import com.arcgismaps.data.InheritedDomain
import com.arcgismaps.data.RangeDomain
import com.learning.diplomski.ui.components.CustomTextInputEditText
import com.learning.diplomski.R
import com.learning.diplomski.data.local.Repository
import com.learning.diplomski.viewmodels.SearchViewModel

class SearchActivity: AppCompatActivity(){

    private val viewModel: SearchViewModel by viewModels()

    private lateinit var searchButton: Button
    private lateinit var linearLayout: LinearLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_search)

        initView()
        initListeners()
    }

    private fun initView() {
        linearLayout = findViewById(R.id.linearLayout)
        viewModel.initializeData(linearLayout, this)
        searchButton = findViewById(R.id.searchButton)
    }

    private fun initListeners() {
        searchButton.setOnClickListener {
            val queryString = viewModel.search()
            val resultIntent = Intent()
            resultIntent.putExtra("queryString", queryString)
            setResult(Activity.RESULT_OK, resultIntent)
            finish()
        }
    }

    private fun getCodedValues(fieldName: String): String {
        for (type in Repository.types) {
            val typeObjectId = if (Repository.selectedKey == ""){
                null
            } else {
                Repository.typeObjectIdMap[Repository.selectedKey]
            }

            if (type!!.id == typeObjectId) {
                val domains = type.domains
                for (domain in domains) {
                    if (domain.key == fieldName) {
                        val domainDetails = when (domain.value) {
                            is CodedValueDomain -> {
                                val codedValues =
                                    (domain.value as CodedValueDomain).codedValues.joinToString { it.name }
                                codedValues
                            }
                            is InheritedDomain -> {
                                val codedValues =
                                    (domain.value as CodedValueDomain).codedValues.joinToString { it.name }
                                codedValues
                            }
                            is RangeDomain -> {
                                "Range: ${(domain.value as RangeDomain).minValue} - ${(domain.value as RangeDomain).maxValue}"
                            }
                            else -> ""
                        }
                        return domainDetails
                    }
                }
            }
        }
        return ""
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

    private fun clearAllFields() {
        for (searchForm in Repository.searchFormList) {
            val dynamicInputField = viewModel.customMap[searchForm.name]
            if (dynamicInputField != null && searchForm.name != Repository.typeObject) {
                dynamicInputField.isEnabled = true
                dynamicInputField.setText("")
            }
        }
    }

    private fun updateOptionsList() {
        for (field in Repository.searchFormList) {
            val newCodedValues = getCodedValues(field.name)
            if (newCodedValues != "") {
                val inputField: CustomTextInputEditText? = viewModel.customMap[field.name]
                inputField?.setOptions(ArrayList(newCodedValues.split(",")))
            }
        }
    }

    private fun clearFocusFromAllFields() {
        val rootView = findViewById<View>(android.R.id.content)
        rootView.clearFocus()
    }

    companion object {
        const val SEARCH_FEATURES_REQUEST_CODE = 1003
    }

}