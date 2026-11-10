package com.nikgapps

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.platform.app.InstrumentationRegistry
import com.nikgapps.app.presentation.ui.component.ServerRequestLimitsPreference
import com.nikgapps.app.registry.ServerRequestLimits
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.Response
import org.junit.Rule
import org.junit.Test

class ServerRequestLimitsInstrumentedTest {
    @get:Rule val compose = createComposeRule()

    @Test fun developerOptionsShowSeparateServerCountsAndUpdateAfterRateLimiting() {
        check(InstrumentationRegistry.getInstrumentation().targetContext.packageName == "com.nikgapps.review.debug")
        fun observe(host: String, remaining: String, limit: String, code: Int = 200) {
            ServerRequestLimits.observe(Response.Builder().request(Request.Builder().url("https://$host/").build())
                .protocol(Protocol.HTTP_1_1).code(code).message("fixture")
                .header("RateLimit-Remaining", remaining).header("RateLimit-Limit", limit).build())
        }
        observe("gitlab.com", "499", "500")
        observe("api.github.com", "4980", "5000")
        compose.setContent { MaterialTheme { Column { ServerRequestLimitsPreference() } } }
        compose.onNodeWithText("GitLab requests").assertIsDisplayed()
        compose.onNodeWithText("499 / 500 remaining", substring = true).assertIsDisplayed()
        compose.onNodeWithText("4980 / 5000 remaining", substring = true).assertIsDisplayed()
        observe("gitlab.com", "0", "500", 429)
        compose.onNodeWithText("0 / 500 remaining · HTTP 429", substring = true).assertIsDisplayed()
        compose.onNodeWithText("4980 / 5000 remaining", substring = true).assertIsDisplayed()
    }
}
