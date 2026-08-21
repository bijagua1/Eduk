package com.eduk.app.cloud

import org.junit.Assert.assertEquals
import org.junit.Test

class ChildAccountCreationErrorsTest {
    @Test
    fun `explains that an occupied username needs a unique replacement`() {
        val message = childAccountCreationErrorMessage(
            EdukCloudException(409, "USERNAME_TAKEN", "That student username is already in use."),
            "Marlon"
        )

        assertEquals(
            "That student username is already in use. Choose a different username, such as marlon-1.",
            message
        )
    }

    @Test
    fun `preserves the safe child-limit explanation from cloud`() {
        val message = childAccountCreationErrorMessage(
            EdukCloudException(403, "CHILD_LIMIT_REACHED", "Your free plan supports up to 1 child profile."),
            "new.student"
        )

        assertEquals("Your free plan supports up to 1 child profile.", message)
    }
}
