package com.learning.diplomski.data.local

import com.arcgismaps.data.Feature
import com.arcgismaps.data.FeatureType
import com.arcgismaps.data.Field
import com.arcgismaps.data.FieldType
import com.arcgismaps.mapping.layers.FeatureLayer

object Repository {
    var featureLayer: FeatureLayer? = null
    var feature: Feature? = null

    var featureLayerList: MutableList<FeatureLayer?> = mutableListOf()
    var codedValuesList: ArrayList<String> = ArrayList()
    var fieldInfoList: MutableList<FieldInfo> = mutableListOf()
    var fields: List<Field?> = emptyList()
    var types: List<FeatureType?> = emptyList()
    val fieldTypeMap = mapOf(
        FieldType.Text to "Text",
        FieldType.Int16 to "Short",
        FieldType.Int32 to "Integer",
        FieldType.Int64 to "Long",
        FieldType.Float32 to "Float",
        FieldType.Float64 to "Double",
        FieldType.Date to "Date",
        FieldType.DateOnly to "DateOnly",
        FieldType.Oid to "OID",
        FieldType.Geometry to "Geometry",
        FieldType.GlobalId to "GlobalId",
        FieldType.Blob to "Blob",
        FieldType.Raster to "Raster",
        FieldType.Guid to "GUID",
        FieldType.Xml to "XML"
    )

    val typesMap = mapOf(
        "text" to "Text",
        "customText" to "Text",
        "number" to "Short",
        "datePicker" to "Date",
        "decimalNumber" to "Double"
    )

    var typeObject: String = ""
    var dataTypeObject: String = ""
    var selectedKey: String = ""
    var typeObjectNamesMap: MutableMap<Any, String> = mutableMapOf()
    var typeObjectIdMap: MutableMap<String, Any> = mutableMapOf()

    val searchFormList: List<CustomField> = listOf(
        CustomField( "tip", "Tip objekta",  "customText"),
        CustomField( "vrsta", "Vrsta drveta", "customText"),
        CustomField( "fitopatoloske_promene", "Fitopataloške promene" , "customText"),
        CustomField( "entomoloske_promene", "Entomološke promene", "customText"),
        CustomField( "slomljene_grane", "Slomljene grane",  "customText"),
        CustomField( "ocena_dekorativnosti", "Ocena dekorativnosti", "number"),
        CustomField( "procena_starosti",  "Procena starosti", "customText"),
        CustomField( "vreme_sadnje_od", "Vreme sadnje od", "datePicker"),
        CustomField( "vreme_sadnje_do", "Vreme sadnje do", "datePicker"),
        CustomField( "rasadnik", "Rasadnik ",  "text"),
        CustomField( "visina_stabla_od", "Visina stabla od (m)", "decimalNumber"),
        CustomField( "visina_stabla_do", "Visina stabla do (m)",  "decimalNumber"),
        CustomField( "visina_debla_od", "Visina debla od (m)",  "decimalNumber"),
        CustomField( "visina_debla_do", "Visina debla do (m)",  "decimalNumber"),
        CustomField( "napomena", "Napomena", "text"),
    )
    val numbersCustomInputFieldList: List<NumberCustomField> = listOf(
        NumberCustomField(1, "1- Loše"),
        NumberCustomField(2, "2- Dovoljno"),
        NumberCustomField(3, "3- Dobro"),
        NumberCustomField(4, "4- Vrlo dobro"),
        NumberCustomField(5, "5- Odlično")
    )

    val aliasCustomFieldMap: MutableMap<String, CustomFieldMap> = mutableMapOf()

    data class FieldInfo(
        val id: Int,
        val name: String,
        val value: Any?,
        val type: String
    )
    data class CustomField(
        val name: String,
        val alias: String,
        val type: String
    )
    data class CustomFieldMap(
        val name: String,
        val type: String
    )
    data class NumberCustomField(
        val key: Short,
        val value: String
    )
}