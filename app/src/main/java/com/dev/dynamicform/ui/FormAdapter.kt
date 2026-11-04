package com.dev.dynamicform.ui

import android.graphics.Color
import android.text.InputType
import android.text.Spannable
import android.text.SpannableString
import android.text.TextWatcher
import android.text.style.ForegroundColorSpan
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.core.content.ContextCompat
import androidx.core.widget.doAfterTextChanged
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.dev.dynamicform.R
import com.dev.dynamicform.data.model.FormField
import com.dev.dynamicform.data.model.OptionItem
import com.dev.dynamicform.databinding.ItemFieldDropdownBinding
import com.dev.dynamicform.databinding.ItemFieldTextBinding

class FormAdapter(
    private val getValue: (key: String) -> String,
    private val isRequired: (field: FormField) -> Boolean,
    private val onValueChanged: (key: String, value: String) -> Unit,
    private val onOptionSelected: (key: String, option: OptionItem) -> Unit
) : ListAdapter<FormField, RecyclerView.ViewHolder>(FieldDiffCallback) {

    init {
        setHasStableIds(true)
    }

    private var errors: Map<String, String> = emptyMap()

    override fun getItemId(position: Int): Long {
        return getItem(position).key.hashCode().toLong()
    }

    fun updateErrors(newErrors: Map<String, String>) {
        val oldErrors = errors
        errors = newErrors
        for (i in 0 until currentList.size) {
            val key = currentList[i].key
            if (oldErrors[key] != newErrors[key]) {
                notifyItemChanged(i, PAYLOAD_ERROR)
            }
        }
    }

    override fun getItemViewType(position: Int): Int {
        return if (getItem(position).isEnrollmentType()) TYPE_DROPDOWN else TYPE_TEXT
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return if (viewType == TYPE_DROPDOWN) {
            DropdownViewHolder(ItemFieldDropdownBinding.inflate(inflater, parent, false))
        } else {
            TextViewHolder(ItemFieldTextBinding.inflate(inflater, parent, false))
        }
    }

    override fun onBindViewHolder(
        holder: RecyclerView.ViewHolder,
        position: Int,
        payloads: MutableList<Any>
    ) {
        if (payloads.contains(PAYLOAD_ERROR)) {
            val field = getItem(position)
            val error = errors[field.key]
            if (holder is TextViewHolder) {
                holder.updateError(error)
            } else if (holder is DropdownViewHolder) {
                holder.updateError(error)
            }
        } else {
            super.onBindViewHolder(holder, position, payloads)
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val field = getItem(position)
        val error = errors[field.key]

        if (holder is DropdownViewHolder) {
            holder.bind(field, error)
        } else if (holder is TextViewHolder) {
            holder.bind(field, error)
        }
    }

    inner class TextViewHolder(private val binding: ItemFieldTextBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(field: FormField, error: String?) {
            val required = isRequired(field)

            val label = if (required) "${field.label} *" else field.label
            val spannable = SpannableString(label)

            if (required) {
                spannable.setSpan(
                    ForegroundColorSpan(Color.RED),
                    field.label.length,
                    label.length,
                    Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                )
            }

            binding.tvFieldLabel.text = spannable

            binding.editText.inputType = when {
                field.isPhoneNumber() -> InputType.TYPE_CLASS_PHONE
                field.isEmail() -> InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS or InputType.TYPE_CLASS_TEXT
                else -> InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_CAP_WORDS
            }

            if (binding.editText.tag is TextWatcher) {
                binding.editText.removeTextChangedListener(binding.editText.tag as TextWatcher)
            }

            val currentValue = getValue(field.key)
            if (binding.editText.text.toString() != currentValue) {
                binding.editText.setText(currentValue)
            }

            val watcher = binding.editText.doAfterTextChanged {
                if (binding.editText.hasFocus()) {
                    if (binding.tvError.visibility == View.VISIBLE) {
                        binding.tvError.visibility = View.GONE
                        binding.underline.setBackgroundResource(R.drawable.bg_underline_selector)
                    }
                    onValueChanged(field.key, it?.toString().orEmpty())
                }
            }
            binding.editText.tag = watcher

            updateError(error)
        }

        fun updateError(error: String?) {
            val context = itemView.context
            if (error.isNullOrEmpty()) {
                binding.tvError.visibility = View.GONE
                binding.underline.setBackgroundResource(R.drawable.bg_underline_selector)
            } else {
                binding.tvError.text = error
                binding.tvError.visibility = View.VISIBLE
                binding.underline.setBackgroundColor(ContextCompat.getColor(context, R.color.error))
            }
        }
    }

    inner class DropdownViewHolder(private val binding: ItemFieldDropdownBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(field: FormField, error: String?) {
            val context = itemView.context
            val required = isRequired(field)

            val label = if (required) "${field.label} *" else field.label
            val spannable = SpannableString(label)

            if (required) {
                spannable.setSpan(
                    ForegroundColorSpan(Color.RED),
                    field.label.length,
                    label.length,
                    Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                )
            }

            binding.tvDropdownLabel.text = spannable

            val options = field.meta?.options.orEmpty()
            val optionLabels = options.map { it.option }

            binding.autoCompleteTextView.setAdapter(
                ArrayAdapter(context, R.layout.item_dropdown_item, optionLabels)
            )

            val currentValue = getValue(field.key)
            binding.autoCompleteTextView.setText(currentValue, false)

            val open = { binding.autoCompleteTextView.showDropDown() }
            binding.autoCompleteTextView.setOnClickListener { open() }
            binding.dropdownContainer.setOnClickListener { open() }

            binding.autoCompleteTextView.setOnItemClickListener { _, _, pos, _ ->
                if (pos in options.indices) {
                    onOptionSelected(field.key, options[pos])
                }
            }

            updateError(error)
        }

        fun updateError(error: String?) {
            val context = itemView.context
            if (error.isNullOrEmpty()) {
                binding.tvDropdownError.visibility = View.GONE
                binding.dropdownUnderline.setBackgroundColor(
                    ContextCompat.getColor(context, R.color.line_inactive)
                )
            } else {
                binding.tvDropdownError.text = error
                binding.tvDropdownError.visibility = View.VISIBLE
                binding.dropdownUnderline.setBackgroundColor(
                    ContextCompat.getColor(context, R.color.error)
                )
            }
        }
    }

    companion object {
        private const val TYPE_TEXT = 1
        private const val TYPE_DROPDOWN = 2
        private const val PAYLOAD_ERROR = "PAYLOAD_ERROR"

        private val FieldDiffCallback = object : DiffUtil.ItemCallback<FormField>() {
            override fun areItemsTheSame(oldItem: FormField, newItem: FormField) =
                oldItem.key == newItem.key

            override fun areContentsTheSame(oldItem: FormField, newItem: FormField) =
                oldItem == newItem
        }
    }
}
