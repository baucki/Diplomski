package com.learning.diplomski.rmv

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.text.InputType
import android.util.TypedValue
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import androidx.appcompat.app.AppCompatActivity
import com.arcgismaps.data.CodedValueDomain
import com.arcgismaps.data.InheritedDomain
import com.arcgismaps.data.RangeDomain
import com.arcgismaps.data.ServiceFeatureTable
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.learning.diplomski.ui.components.CustomDatePickerEditText
import com.learning.diplomski.ui.components.CustomTextInputEditText
import com.learning.diplomski.R
import com.learning.diplomski.data.Repository
import java.time.LocalDate
import java.time.format.DateTimeFormatter

class SearchActivity: AppCompatActivity(){

    private lateinit var searchButton: Button

    private lateinit var customMap: MutableMap<String, CustomTextInputEditText>
    private lateinit var inputFields: MutableList<TextInputLayout>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_search)

        initView()
        initListeners()
    }

    private fun initView() {
        val featureTable = Repository.featureLayer!!.featureTable as ServiceFeatureTable
        Repository.fields = featureTable.fields
        Repository.types = featureTable.featureTypes
        Repository.typeObject = "tip"
        Repository.dataTypeObject = "Short"

        for (type in Repository.types) {
            if (type != null) {
                Repository.typeObjectNamesMap[type.id] = type.name
                Repository.typeObjectIdMap[type.name] = type.id
            }
        }

        customMap = mutableMapOf()
        inputFields = mutableListOf()
        val linearLayout = findViewById<LinearLayout>(R.id.linearLayout)

        for (fieldInForm in Repository.searchFormList) {
            var isCustomText: Boolean
            Repository.aliasCustomFieldMap[fieldInForm.alias] =
                Repository.CustomFieldMap(fieldInForm.name, fieldInForm.type)
            if (fieldInForm.type == "customText" || fieldInForm.type == "number") {
                isCustomText = true
                if (fieldInForm.name == Repository.typeObject) {
                    val arrayList: ArrayList<String> = arrayListOf()
                    for (type in Repository.types) {
                        arrayList.add(type!!.name)
                    }
                    Repository.codedValuesList = ArrayList(arrayList)
                } else {
                    val codedValues: String = getCodedValues(fieldInForm.name)
                    if (codedValues != "") {
                        Repository.codedValuesList = ArrayList(codedValues.split(","))
                    }
                }
            } else {
                isCustomText = false
            }
            val view: TextInputLayout = createView(this,
                Repository.CustomField(fieldInForm.name, fieldInForm.alias, fieldInForm.type)
            )
            inputFields.add(view)
            if (isCustomText)
                view.isEnabled = fieldInForm.name == Repository.typeObject
            linearLayout.addView(view)
        }
        searchButton = findViewById(R.id.searchButton)
    }

    private fun initListeners() {
        searchButton.setOnClickListener {
            var queryString = ""
            for (field in inputFields) {
                val input = field.editText?.text.toString()
                val alias = field.editText?.hint
                if (input != "") {
                    when (Repository.typesMap[Repository.aliasCustomFieldMap[alias]?.type]) {
                        "Text" -> {
                            val name = cleanString("${Repository.aliasCustomFieldMap[alias]?.name}")
                            var query = ""
                            if (name == Repository.typeObject) {
                                for ((key, value) in Repository.typeObjectNamesMap) {
                                    if (value == input) {
                                        query = "$name = $key"
                                    }
                                }
                            } else {
                                query = "$name LIKE '$input'"
                            }
                            queryString = makeQuery(queryString, query)
                        }
                        "Short" -> {
                            val name = cleanString("${Repository.aliasCustomFieldMap[alias]?.name}")
                            var queryInput = ""
                            for (option in Repository.numbersCustomInputFieldList)
                                if (option.value == input)
                                    queryInput = option.key.toString()
                            val query = "$name = $queryInput"
                            queryString = makeQuery(queryString, query)
                        }
                        "Double" -> {
                            var name = cleanString("${Repository.aliasCustomFieldMap[alias]?.name}")
                            var query = ""
                            if (name.endsWith("_od")) {
                                name = name.substring(0, name.length-3)
                                query = "$name >= $input"
                            }
                            if (name.endsWith("_do")) {
                                name = name.substring(0, name.length-3)
                                query = "$name <= $input"
                            }
                            queryString = makeQuery(queryString, query)
                        }
                        "Date" -> {
                            var name = cleanString("${Repository.aliasCustomFieldMap[alias]?.name}")
                            var query = ""
                            if (name.endsWith("_od")) {
                                name = name.substring(0, name.length-3)
                                val formattedInput = formatDate(input)
                                query = "$name >= timestamp '$formattedInput 00:00:00'"
                            }
                            if (name.endsWith("_do")) {
                                name = name.substring(0, name.length-3)
                                val formattedInput = formatDate(input)
                                query = "$name <= timestamp '$formattedInput 00:00:00'"
                            }
                            queryString = makeQuery(queryString, query)
                        }
                    }
                }
            }
            val resultIntent = Intent()
            resultIntent.putExtra("queryString", queryString)
            setResult(Activity.RESULT_OK, resultIntent)
            finish()
        }
    }

    private fun formatDate(date: String): String {
        val originalFormatter = DateTimeFormatter.ofPattern("d/M/yyyy")
        val date = LocalDate.parse(date, originalFormatter)
        val desiredFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
        return date.format(desiredFormatter)
    }

    private fun makeQuery(destination: String, source: String): String {
        return if (destination == "")
            source
        else
            "$destination AND $source"
    }

    private fun cleanString(input: String): String {
        val pattern = Regex("^(.*?)(\\s(od|do))?(\\s*\\(.*\\))?$")
        val matchResult = pattern.find(input)

        return if (matchResult != null) {
            matchResult.groupValues[1].trim()
        } else {
            input
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
    private fun createView(context: Context, customField: Repository.CustomField): TextInputLayout {
        val textInputLayout = TextInputLayout(context).apply {
            boxBackgroundMode = TextInputLayout.BOX_BACKGROUND_OUTLINE
            boxBackgroundColor = Color.WHITE
            boxStrokeColor = Color.argb(255, 204, 204, 204)
            val layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            layoutParams.setMargins(
                0,
                this.resources.getDimensionPixelSize(R.dimen.text_input_margin_top),
                0,
                0
            )
            this.layoutParams = layoutParams
            setBoxCornerRadii(
                context.resources.getDimensionPixelSize(R.dimen.box_corner_radius).toFloat(),
                context.resources.getDimensionPixelSize(R.dimen.box_corner_radius).toFloat(),
                context.resources.getDimensionPixelSize(R.dimen.box_corner_radius).toFloat(),
                context.resources.getDimensionPixelSize(R.dimen.box_corner_radius).toFloat()
            )
        }

        val dynamicInputField = when (customField.type) {
            "text" -> {
                createTextInput(textInputLayout.context, customField.name, customField.alias)
            }
            "number" -> {
//                createNumberInput(textInputLayout.context, customField.name, customField.alias)
                createCustomTextInput(textInputLayout.context, customField.name, customField.alias)
            }
            "decimalNumber" -> {
                createDecimalNumberInput(textInputLayout.context, customField.name, customField.alias)
            }
            "customText" -> {
                createCustomTextInput(textInputLayout.context, customField.name, customField.alias)
            }
            "datePicker" -> {
                createCustomDateInput(textInputLayout.context, customField.name, customField.alias)
            }
            else -> null

        }
        if (dynamicInputField != null) {
            textInputLayout.addView(dynamicInputField)
        }
        return textInputLayout
    }

    private fun createCustomTextInput(context: Context, name: String, hint: String): CustomTextInputEditText {
        val textInputEditText = CustomTextInputEditText(context).apply {
            id = View.generateViewId()
            this.hint = hint
            setPadding(
                context.resources.getDimensionPixelSize(R.dimen.text_input_padding),
                context.resources.getDimensionPixelSize(R.dimen.text_input_padding),
                context.resources.getDimensionPixelSize(R.dimen.text_input_padding),
                context.resources.getDimensionPixelSize(R.dimen.text_input_padding)
            )
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 16f)
            setCompoundDrawablePadding(context.resources.getDimensionPixelSize(R.dimen.drawable_padding))
        }
        textInputEditText.setOptions(Repository.codedValuesList)
        textInputEditText.setTag(name)
        textInputEditText.setActivityContext(this)
        customMap[name] = textInputEditText

        return textInputEditText
    }


    private fun createDecimalNumberInput(context: Context, name: String, hint: String): TextInputEditText {
        val textInputEditText = TextInputEditText(context).apply {
            this.hint = hint
            id = View.generateViewId()
            setPadding(
                context.resources.getDimensionPixelSize(R.dimen.text_input_padding),
                context.resources.getDimensionPixelSize(R.dimen.text_input_padding),
                context.resources.getDimensionPixelSize(R.dimen.text_input_padding),
                context.resources.getDimensionPixelSize(R.dimen.text_input_padding)
            )
            inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 16f)
            setCompoundDrawablePadding(context.resources.getDimensionPixelSize(R.dimen.drawable_padding))
        }

        return textInputEditText
    }

    private fun createNumberInput(context: Context, name: String, hint: String): TextInputEditText {
        val textInputEditText = TextInputEditText(context).apply {
            this.hint = hint
            id = View.generateViewId()
            setPadding(
                context.resources.getDimensionPixelSize(R.dimen.text_input_padding),
                context.resources.getDimensionPixelSize(R.dimen.text_input_padding),
                context.resources.getDimensionPixelSize(R.dimen.text_input_padding),
                context.resources.getDimensionPixelSize(R.dimen.text_input_padding)
            )
            inputType = InputType.TYPE_CLASS_NUMBER
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 16f)
            setCompoundDrawablePadding(context.resources.getDimensionPixelSize(R.dimen.drawable_padding))
        }

        return textInputEditText
    }

    private fun createCustomDateInput(context: Context, name: String, hint: String): TextInputEditText {
        val textInputEditText = CustomDatePickerEditText(context).apply {
            this.hint = hint
            id = View.generateViewId()
            setPadding(
                context.resources.getDimensionPixelSize(R.dimen.text_input_padding),
                context.resources.getDimensionPixelSize(R.dimen.text_input_padding),
                context.resources.getDimensionPixelSize(R.dimen.text_input_padding),
                context.resources.getDimensionPixelSize(R.dimen.text_input_padding)
            )

            setTextSize(TypedValue.COMPLEX_UNIT_SP, 16f)
            setCompoundDrawablePadding(context.resources.getDimensionPixelSize(R.dimen.drawable_padding))
        }

        return textInputEditText
    }

    private fun createTextInput(context: Context, name: String, hint: String): TextInputEditText {
        val textInputEditText = TextInputEditText(context).apply {
            this.hint = hint
            id = View.generateViewId()
            setPadding(
                context.resources.getDimensionPixelSize(R.dimen.text_input_padding),
                context.resources.getDimensionPixelSize(R.dimen.text_input_padding),
                context.resources.getDimensionPixelSize(R.dimen.text_input_padding),
                context.resources.getDimensionPixelSize(R.dimen.text_input_padding)
            )
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 16f)
            setCompoundDrawablePadding(context.resources.getDimensionPixelSize(R.dimen.drawable_padding))
        }

        return textInputEditText
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == CustomTextInputEditText.OPTIONS_LIST_REQUEST_CODE && resultCode == Activity.RESULT_OK) {
            val selectedOption = data?.getStringExtra("selectedOption")
            val tag = data?.getStringExtra("tag")
            if (customMap.containsKey(tag))  {
                val editText = customMap[tag]
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
            val dynamicInputField = customMap[searchForm.name]
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
                val inputField: CustomTextInputEditText? = customMap[field.name]
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