package com.learning.diplomski.ui.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.learning.diplomski.R

class FeatureAttributesAdapter(private var featureAttributesList: List<Map<String, Any?>>) :
    RecyclerView.Adapter<FeatureAttributesAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val attributeTextView: TextView = view.findViewById(R.id.attributeTextView)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_feature_attribute, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val featureAttributes = featureAttributesList[position]
        val attributeText = StringBuilder()

        for ((key, value) in featureAttributes) {
            val displayValue = value?.toString() ?: "---"
            attributeText.append("$key: $displayValue\n")
        }

        holder.attributeTextView.text = attributeText.toString()
    }

    override fun getItemCount(): Int {
        return featureAttributesList.size
    }

    fun updateData(newFeatureAttributesList: List<Map<String, Any?>>) {
        featureAttributesList = newFeatureAttributesList
        notifyDataSetChanged()
    }
}