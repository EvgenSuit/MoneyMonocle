package com.money.monocle.domain.useCases

import android.util.Patterns

class UsernameValidator {
    operator fun invoke(username: String): UsernameValidationResult {
        return if (username.isBlank()) UsernameValidationResult.IS_EMPTY
        else UsernameValidationResult.CORRECT
    }
}
class EmailValidator {
    operator fun invoke(email: String): EmailValidationResult {
        return if (Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            EmailValidationResult.CORRECT
        }
        else EmailValidationResult.INCORRECT_FORMAT
    }
}

class PasswordValidator {
    operator fun invoke(password: String): PasswordValidationResult {
        return if (password.length < 8) PasswordValidationResult.NOT_LONG_ENOUGH
        else if (password.count(Char::isUpperCase) == 0) PasswordValidationResult.NOT_ENOUGH_UPPERCASE
        else if (!password.contains("[0-9]".toRegex())) PasswordValidationResult.NOT_ENOUGH_DIGITS
        else PasswordValidationResult.CORRECT
    }
}

enum class FieldType {
    USERNAME,
    EMAIL,
    PASSWORD
}
enum class UsernameValidationResult {
    IS_EMPTY,
    CORRECT
}
enum class EmailValidationResult {
    INCORRECT_FORMAT,
    CORRECT
}
enum class PasswordValidationResult {
    NOT_LONG_ENOUGH,
    NOT_ENOUGH_DIGITS,
    NOT_ENOUGH_UPPERCASE,
    CORRECT
}
enum class AuthType {
    SIGN_IN,
    SIGN_UP
}