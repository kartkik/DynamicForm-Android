package com.dev.dynamicform.util

import android.util.Patterns
import com.dev.dynamicform.data.model.FormField

object ValidationUtils {

    fun isValidPhoneNumber(phone: String): Boolean {
        val clean = phone.replace(" ", "").replace("-", "")
        return clean.matches(Regex("""^\+?[0-9]{7,15}$"""))
    }

    fun isValidEmail(email: String): Boolean {
        return Patterns.EMAIL_ADDRESS.matcher(email).matches()
    }

    fun validateField(
        field: FormField,
        value: String,
        isRequired: Boolean
    ): String? {
        val trimmed = value.trim()

        if (isRequired && trimmed.isEmpty()) {
            return if (field.isEnrollmentType()) {
                "Please select ${field.label}"
            } else {
                "${field.label} is required"
            }
        }

        if (trimmed.isNotEmpty()) {
            if (field.isPhoneNumber() && !isValidPhoneNumber(trimmed)) {
                return "Please enter a valid phone number"
            }
            if (field.isEmail() && !isValidEmail(trimmed)) {
                return "Please enter a valid email address"
            }
        }

        return null
    }

    fun validateFields(
        fields: List<FormField>,
        fieldValues: Map<String, String>,
        isFieldRequired: (FormField) -> Boolean
    ): Map<String, String> {
        val errors = mutableMapOf<String, String>()
        for (field in fields) {
            val value = fieldValues[field.key].orEmpty()
            val error = validateField(field, value, isFieldRequired(field))
            if (error != null) {
                errors[field.key] = error
            }
        }
        return errors
    }
}

