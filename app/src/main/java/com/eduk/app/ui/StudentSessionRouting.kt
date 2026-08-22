package com.eduk.app.ui

internal fun studentStartDestination(hasPendingGate: Boolean, hasStoredStudentSession: Boolean): String = when {
    hasPendingGate -> "question"
    hasStoredStudentSession -> "child_home"
    else -> "localization"
}
