package com.nikgapps

import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.platform.app.InstrumentationRegistry
import com.nikgapps.app.presentation.ui.component.ProjectCreationGate
import com.nikgapps.app.presentation.ui.component.projectCreationUnavailableMessage
import com.nikgapps.app.registry.RegistryAvailability
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class ProjectCreationGateInstrumentedTest {
    @get:Rule val compose = createComposeRule()

    @Test fun plusShowsRetryTimeWithoutEnteringCreationAndWorksAfterCooldownClears() {
        check(InstrumentationRegistry.getInstrumentation().targetContext.packageName == "com.nikgapps.review.debug")
        RegistryAvailability.clearExpired(Long.MAX_VALUE)
        var creationRequests = 0
        try {
            val retryAt = RegistryAvailability.rateLimited("600", null)
            compose.setContent {
                MaterialTheme {
                    ProjectCreationGate(onCreate = { creationRequests++ }) { create ->
                        Button(onClick = create) { Text("Create project") }
                    }
                }
            }
            compose.onNodeWithText("Create project").performClick()
            compose.onNodeWithText("Project creation is temporarily unavailable").assertIsDisplayed()
            compose.onNodeWithText(projectCreationUnavailableMessage(retryAt)).assertIsDisplayed()
            assertEquals(0, creationRequests)
            compose.onNodeWithText("OK").performClick()
            RegistryAvailability.clearExpired(Long.MAX_VALUE)
            compose.onNodeWithText("Create project").performClick()
            assertEquals(1, creationRequests)
            compose.onNodeWithText("Project creation is temporarily unavailable").assertDoesNotExist()
        } finally { RegistryAvailability.clearExpired(Long.MAX_VALUE) }
    }
}
