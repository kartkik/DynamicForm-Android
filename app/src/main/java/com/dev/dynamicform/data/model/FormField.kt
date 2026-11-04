package com.dev.dynamicform.data.model

import com.google.gson.annotations.SerializedName

data class ApiResponse(
    @SerializedName("status") val status: String? = null,
    @SerializedName("message") val message: String? = null,
    @SerializedName("data") val data: List<FormField>? = null
)

data class FormField(
    @SerializedName("id") val id: Int,
    @SerializedName("type") val type: String,
    @SerializedName("label") val label: String,
    @SerializedName("meta") val meta: Meta? = null,
    @SerializedName("validations") val validations: Validations? = null
) {
    val key: String get() = "${id}_${label}"

    fun isSeparator(): Boolean = type.equals("separator", ignoreCase = true)
    fun isEnrollmentType(): Boolean = type.equals("enrollment_type", ignoreCase = true)
    fun isName(): Boolean = type.equals("name", ignoreCase = true)
    fun isPhoneNumber(): Boolean = type.equals("phone_number", ignoreCase = true)
    fun isEmail(): Boolean = type.equals("email", ignoreCase = true)
}

data class Meta(
    @SerializedName("options") val options: List<OptionItem>? = null
)

data class OptionItem(
    @SerializedName("option") val option: String,
    @SerializedName("email_mandatory") val emailMandatory: Boolean = false
)

data class Validations(
    @SerializedName("required") val required: Boolean = false
)
