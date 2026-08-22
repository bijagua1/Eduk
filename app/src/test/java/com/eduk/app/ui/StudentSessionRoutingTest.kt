package com.eduk.app.ui

import org.junit.Assert.assertEquals
import org.junit.Test

class StudentSessionRoutingTest {
    @Test
    fun `restores child home when the phone still has an encrypted student session`() {
        assertEquals("child_home", studentStartDestination(hasPendingGate = false, hasStoredStudentSession = true))
    }

    @Test
    fun `opens the protected question before restoring child home`() {
        assertEquals("question", studentStartDestination(hasPendingGate = true, hasStoredStudentSession = true))
    }

    @Test
    fun `uses first-time setup when no child session exists`() {
        assertEquals("localization", studentStartDestination(hasPendingGate = false, hasStoredStudentSession = false))
    }
}
