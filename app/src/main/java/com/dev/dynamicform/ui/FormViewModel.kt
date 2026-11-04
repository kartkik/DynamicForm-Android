package com.dev.dynamicform.ui

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dev.dynamicform.data.model.FormField
import com.dev.dynamicform.data.model.OptionItem
import com.dev.dynamicform.data.repository.FormRepository
import com.dev.dynamicform.util.ValidationUtils
import kotlinx.coroutines.launch

class FormViewModel(
    private val repository: FormRepository = FormRepository()
) : ViewModel() {

    private val tag = "DynamicForm:VM"

    private val _pages = MutableLiveData<List<List<FormField>>>(emptyList())
    val pages: LiveData<List<List<FormField>>> = _pages

    private val _currentPageIndex = MutableLiveData(0)
    val currentPageIndex: LiveData<Int> = _currentPageIndex

    private val _isLoading = MutableLiveData(false)
    val isLoading: LiveData<Boolean> = _isLoading

    private val _errorMessage = MutableLiveData<String?>()
    val errorMessage: LiveData<String?> = _errorMessage

    private val _fieldErrors = MutableLiveData<Map<String, String>>(emptyMap())
    val fieldErrors: LiveData<Map<String, String>> = _fieldErrors

    private val _isEmailMandatory = MutableLiveData(false)
    val isEmailMandatory: LiveData<Boolean> = _isEmailMandatory

    private val _submissionSuccess = MutableLiveData<Map<String, String>?>()
    val submissionSuccess: LiveData<Map<String, String>?> = _submissionSuccess

    val fieldValues = mutableMapOf<String, String>()
    private var selectedEnrollmentOption: OptionItem? = null

    init {
        loadForm()
    }

    fun loadForm() {
        Log.d(tag, "Loading form configuration...")
        _isLoading.value = true
        _errorMessage.value = null

        viewModelScope.launch {
            val result = repository.fetchFormFields()
            _isLoading.value = false

            result.onSuccess { allFields ->
                val partitioned = partitionIntoPages(allFields)
                if (partitioned.isEmpty()) {
                    _errorMessage.value = "No form fields found"
                    return@onSuccess
                }
                Log.d(tag, "Form partitioned into ${partitioned.size} pages via separator")
                _pages.value = partitioned
                _currentPageIndex.value = 0
            }.onFailure { error ->
                Log.e(tag, "Failed to load form: ${error.message}", error)
                _errorMessage.value = "Something went wrong. Please try again."    
            }
        }
    }

    private fun partitionIntoPages(fields: List<FormField>): List<List<FormField>> {
        val result = mutableListOf<List<FormField>>()
        var current = mutableListOf<FormField>()

        for (field in fields) {
            if (field.isSeparator()) {
                if (current.isNotEmpty()) {
                    result.add(current)
                    current = mutableListOf()
                }
            } else {
                current.add(field)
            }
        }
        if (current.isNotEmpty()) {
            result.add(current)
        }
        return result
    }

    fun onValueChanged(fieldKey: String, value: String) {
        fieldValues[fieldKey] = value
        Log.d(tag, "Field [$fieldKey] updated: '$value'")
    }

    fun onOptionSelected(fieldKey: String, option: OptionItem) {
        selectedEnrollmentOption = option
        fieldValues.clear()
        fieldValues[fieldKey] = option.option
        _isEmailMandatory.value = option.emailMandatory

        Log.d(tag, "Option selected: ${option.option} (email_mandatory=${option.emailMandatory})")

        _fieldErrors.value = emptyMap()
    }

    fun isFieldRequired(field: FormField): Boolean {
        return if (field.isEmail()) {
            isEmailRequired()
        } else {
            field.validations?.required == true
        }
    }

    fun isEmailRequired(): Boolean {
        return selectedEnrollmentOption?.emailMandatory == true
    }

    fun onNextClicked() {
        val pageList = _pages.value.orEmpty()
        val index = _currentPageIndex.value ?: 0
        if (index !in pageList.indices) return

        val currentPageFields = pageList[index]
        val validationErrors = validateFields(currentPageFields)

        if (validationErrors.isNotEmpty()) {
            Log.w(tag, "Validation failed on page ${index + 1}: $validationErrors")
            _fieldErrors.value = validationErrors
            return
        }

        _fieldErrors.value = emptyMap()

        if (index < pageList.size - 1) {
            val nextIndex = index + 1
            Log.d(tag, "Navigating to page $nextIndex")
            _currentPageIndex.value = nextIndex
        } else {
            Log.d(tag, "Submitting form...")
            val summary = mutableMapOf<String, String>()
            for (page in pageList) {
                for (field in page) {
                    summary[field.label] = fieldValues[field.key].orEmpty()
                }
            }
            _submissionSuccess.value = summary
        }
    }

    fun onPreviousClicked() {
        val index = _currentPageIndex.value ?: 0
        if (index > 0) {
            val prevIndex = index - 1
            Log.d(tag, "Navigating back to page $prevIndex")
            _fieldErrors.value = emptyMap()
            _currentPageIndex.value = prevIndex
        }
    }

    private fun validateFields(fields: List<FormField>): Map<String, String> {
        return ValidationUtils.validateFields(fields, fieldValues, ::isFieldRequired)
    }

    fun resetForm() {
        Log.d(tag, "Resetting form")
        fieldValues.clear()
        selectedEnrollmentOption = null
        _fieldErrors.value = emptyMap()
        _isEmailMandatory.value = false
        _submissionSuccess.value = null
        _currentPageIndex.value = 0
    }
}
