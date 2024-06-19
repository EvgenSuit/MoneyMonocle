package com.money.monocle.domain

import androidx.annotation.StringRes
import com.money.monocle.ui.presentation.StringValue

sealed class CustomResult(val error: StringValue = StringValue.Empty) {
    data object Idle: CustomResult()
    data object InProgress: CustomResult()
    data object Empty: CustomResult()
    data object Success: CustomResult()
    class DynamicError(error: String): CustomResult(StringValue.DynamicString(error))
    class ResourceError(@StringRes res: Int): CustomResult(StringValue.StringResource(res))
}