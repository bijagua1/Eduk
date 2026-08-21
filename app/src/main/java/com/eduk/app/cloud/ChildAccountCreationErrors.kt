package com.eduk.app.cloud

fun childAccountCreationErrorMessage(error: Throwable, username: String): String = when ((error as? EdukCloudException)?.errorCode) {
    "USERNAME_TAKEN" -> "That student username is already in use. Choose a different username, such as ${username.trim().lowercase()}-1."
    "CHILD_LIMIT_REACHED" -> error.message
    "UNAUTHENTICATED", "SESSION_EXPIRED" -> "Your parent session has expired. Please sign in again."
    "VALIDATION_ERROR" -> "Check the child name, username, 4–8 digit PIN, grade, and daily limit, then try again."
    else -> "We could not create this child account right now. Check your connection and try again."
}
