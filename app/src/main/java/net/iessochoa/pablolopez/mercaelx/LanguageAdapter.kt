package net.iessochoa.pablolopez.mercaelx

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.ImageView
import android.widget.TextView

class LanguageAdapter(
    private val context: Context,
    private val languages: List<LanguageItem>
) : ArrayAdapter<LanguageItem>(context, 0, languages) {

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        return createCustomView(position, convertView, parent)
    }

    override fun getDropDownView(position: Int, convertView: View?, parent: ViewGroup): View {
        return createCustomView(position, convertView, parent)
    }

    private fun createCustomView(position: Int, convertView: View?, parent: ViewGroup): View {
        val view = convertView ?: LayoutInflater.from(context)
            .inflate(R.layout.spinner_language_item, parent, false)

        val item = languages[position]
        val imageFlag = view.findViewById<ImageView>(R.id.imageFlag)
        val textLanguage = view.findViewById<TextView>(R.id.textLanguage)

        imageFlag.setImageResource(item.flagResId)
        textLanguage.text = item.name

        return view
    }
}
