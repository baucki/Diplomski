package com.learning.diplomski.viewmodels

import android.app.Activity
import android.content.Context
import android.graphics.Color
import android.text.InputType
import android.util.TypedValue
import android.view.View
import android.widget.LinearLayout
import androidx.lifecycle.ViewModel
import com.arcgismaps.data.CodedValueDomain
import com.arcgismaps.data.Feature
import com.arcgismaps.data.InheritedDomain
import com.arcgismaps.data.RangeDomain
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.learning.diplomski.ui.components.CustomDatePickerEditText
import com.learning.diplomski.ui.components.CustomTextInputEditText
import com.learning.diplomski.R
import com.learning.diplomski.data.Repository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Date
import java.util.Locale
import javax.inject.Inject

@HiltViewModel
class EditViewModel @Inject constructor() : ViewModel() {

    lateinit var customMap: MutableMap<String, CustomTextInputEditText>

    fun initializeData(
        linearLayout: LinearLayout,
        context: Context
    ) {
        customMap = mutableMapOf()
        for (field in Repository.fields) {
            var view: TextInputLayout? = null
            if (field!!.name == Repository.typeObject) {
                val arrayList: ArrayList<String> = arrayListOf()
                for (type in Repository.types) {
                    arrayList.add(type!!.name)
                }
                Repository.codedValuesList = ArrayList(arrayList)
                view = createView(context,
                    Repository.CustomField(field.name, field.alias, "customText")
                )
            }
            else {
                when (Repository.fieldTypeMap[field!!.fieldType]) {
                    "Text", "Short" -> {
                        var codedValues = ""
                        codedValues = getCodedValues(field.name)
                        if (codedValues != "") {
                            Repository.codedValuesList = ArrayList(codedValues.split(","))
                            view = createView(context,
                                Repository.CustomField(field.name, field.alias, "customText")
                            )
                        } else {
                            view = createView(context,
                                Repository.CustomField(field.name, field.alias, "text")
                            )
                        }
                    }
                    "Double" -> {
                        view = createView(context,
                            Repository.CustomField(field!!.name, field.alias, "decimalNumber")
                        )
                    }
                    "Date" -> {
                        view = createView(context,
                            Repository.CustomField(field!!.name, field.alias, "datePicker")
                        )
                    }
                }
            }
            if (view != null) {
                linearLayout.addView(view)
            }
        }
    }

    fun getCodedValues(fieldName: String): String {
        for (type in Repository.types) {
            val typeObjectId = if (Repository.selectedKey == ""){
                Repository.feature?.attributes?.get(Repository.typeObject)
            } else {
                Repository.typeObjectIdMap[Repository.selectedKey]
            }
            if (type!!.id == typeObjectId) {
                val domains = type?.domains
                if (domains != null) {
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
        }
        return ""
    }

    private fun createView(context: Context, customField: Repository.CustomField): TextInputLayout {
        val textInputLayout = TextInputLayout(context).apply {
            boxBackgroundMode = TextInputLayout.BOX_BACKGROUND_OUTLINE
            boxBackgroundColor = Color.WHITE
            boxStrokeColor = Color.argb(255,204,204,204)
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

        lateinit var type: String
        val dynamicInputField = when(customField.type) {
            "text" -> {
                type = "Text"
                createTextInput(textInputLayout.context, customField.name, customField.alias)
            }
            "number" -> {
                type = "Short"
                createNumberInput(textInputLayout.context, customField.name, customField.alias)
            }
            "decimalNumber" -> {
                type = "Double"
                createDecimalNumberInput(textInputLayout.context, customField.name, customField.alias)
            }
            "customText" -> {
                type = "Text"
                createCustomTextInput(context, textInputLayout.context, customField.name, customField.alias)
            }
            "datePicker" -> {
                type = "Date"
                createCustomDateInput(textInputLayout.context, customField.name, customField.alias)
            }
            else -> null
        }

        if (dynamicInputField != null) {
            Repository.fieldInfoList.add(
                Repository.FieldInfo(
                    dynamicInputField.id,
                    customField.name,
                    null,
                    type
                )
            )
            textInputLayout.addView(dynamicInputField)
        }
        return textInputLayout
    }

    private fun createCustomDateInput(context: Context, name: String, hint: String): CustomDatePickerEditText {
        val textInputEditText = CustomDatePickerEditText(context).apply {
            id = View.generateViewId()
            val attributeValue = Repository.feature?.attributes?.get(name)?.toString() ?: ""
            if (attributeValue != "") {
                val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'")
                val date: Date = sdf.parse(attributeValue)
                val outputFormat = SimpleDateFormat("d/M/yyyy")
                val formattedDate = outputFormat.format(date)
                setText(formattedDate)
            }
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

        return textInputEditText
    }

    private fun createCustomTextInput(context: Context, textInputEditTextContext: Context ,name: String, hint: String): CustomTextInputEditText {
        val textInputEditText = CustomTextInputEditText(textInputEditTextContext).apply {
            id = View.generateViewId()
            var attributeValue = Repository.feature?.attributes?.get(name)?.toString() ?: ""
            if (name == Repository.typeObject)
                attributeValue = Repository.typeObjectNamesMap[Repository.feature?.attributes?.get(name)]!!
            else if (name == "ocena_dekorativnosti" || name == "ocena_kondicije") {
                for (option in Repository.numbersCustomInputFieldList) {
                    if (option.key.toString() == attributeValue)
                        attributeValue = option.value
                }
            }
            setText(attributeValue)
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
        textInputEditText.setActivityContext(context)
        customMap[name] = textInputEditText
        return textInputEditText
    }

    private fun createDecimalNumberInput(context: Context, name: String, hint: String): TextInputEditText {
        val textInputEditText = TextInputEditText(context).apply {
            id = View.generateViewId()
            val attributeValue = Repository.feature?.attributes?.get(name)?.toString() ?: ""
            setText(attributeValue)
            this.hint = hint
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
            id = View.generateViewId()
            val attributeValue = Repository.feature?.attributes?.get(name)?: ""
            var displayValue = ""
            for (option in Repository.numbersCustomInputFieldList) {
                if (option.key == attributeValue)
                    displayValue = option.value
            }
            setText(displayValue)
            this.hint = hint
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

    private fun createTextInput(context: Context, name: String, hint: String): TextInputEditText {
        val textInputEditText = TextInputEditText(context).apply {
            this.hint = hint
            val attributeValue = Repository.feature?.attributes?.get(name)?.toString() ?: ""
            setText(attributeValue)
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

    fun save(activity: Activity, scope: CoroutineScope, updateFeature: (feature: Feature) -> Unit) {
        for (fieldInfo in Repository.fieldInfoList) {
            val dynamicInputField = activity.findViewById<TextInputEditText>(fieldInfo.id)
            if (dynamicInputField.text.toString() != "") {
                Repository.typeObjectNamesMap.entries.firstOrNull { it.value == dynamicInputField.text.toString() }?.let { entry ->
                    val newValue = setDynamicValue(entry.key.toString(),
                        Repository.dataTypeObject
                    )
                    Repository.feature?.attributes?.set(fieldInfo.name, newValue)
                } ?: run {
                    Repository.numbersCustomInputFieldList.firstOrNull { option -> option.value == dynamicInputField.text.toString() }?.let { option ->
                        Repository.feature?.attributes?.set(fieldInfo.name, option.key)
                    } ?: run {
                        val value = setDynamicValue(dynamicInputField.text.toString(), fieldInfo.type)
                        Repository.feature?.attributes?.set(fieldInfo.name, value)
                    }
                }
            } else {
                Repository.feature?.attributes?.set(fieldInfo.name, null)
            }
        }
        scope.launch {
            Repository.feature?.let { feature -> updateFeature(feature) }
        }
    }

    private fun setDynamicValue(valueString: String, valueType: String): Any {
        val value = when (valueType) {
            "Text" -> {
                valueString
            }
            "Short" -> {
                var returnValue: Short = 1
                for (option in Repository.numbersCustomInputFieldList) {
                    if (option.value == valueString)
                        returnValue = option.key
                }
                returnValue
            }
            "Double" -> {
                valueString.toDouble()
            }
            "Date" -> {
                val dateFormat = "d/M/yyyy"

                val inputFormatter = DateTimeFormatter.ofPattern(dateFormat, Locale.ENGLISH)

                val localDate = LocalDate.parse(valueString, inputFormatter)

                val localDateTime = localDate.atStartOfDay()

                val zonedDateTime = localDateTime.atZone(ZoneId.of("UTC"))

                zonedDateTime.toInstant()
            }
            else -> { "null" }
        }
        return value
    }

}