package com.learning.diplomski.ui.adapters

import android.app.Activity
import android.content.Context.LAYOUT_INFLATER_SERVICE
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import com.learning.diplomski.databinding.SpinnerLayoutBinding

class SpinnerAdapter(
    context: Activity,
    id:Int,
    private val list:ArrayList<ItemData>
) : ArrayAdapter<ItemData?>(context, id, list as List<ItemData>) {
    private val inflater: LayoutInflater =
        context.getSystemService(LAYOUT_INFLATER_SERVICE) as LayoutInflater

    override fun getView(
        position: Int,
        convertView: View?,
        parent: ViewGroup
    ): View {
        val spinnerLayoutBinding = SpinnerLayoutBinding.inflate(this.inflater)
        val imageView = spinnerLayoutBinding.locationPointImageView
        imageView.setImageResource(list[position].imageId)
        val textView = spinnerLayoutBinding.locationTextView
        textView.text = list[position].text
        return spinnerLayoutBinding.root
    }

    override fun getDropDownView(
        position: Int,
        convertView: View?,
        parent: ViewGroup
    ): View {
        return getView(position, convertView, parent)
    }
}


data class ItemData(val text: String, val imageId: Int)
