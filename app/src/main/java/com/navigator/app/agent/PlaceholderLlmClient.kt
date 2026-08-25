package com.navigator.app.agent

import com.navigator.core.agent.LlmClient
import com.navigator.core.agent.LlmRequest
import com.navigator.core.agent.LlmResponse

/** Stand-in until a real provider + API key is configured on the build machine. */
class PlaceholderLlmClient : LlmClient {
    override fun complete(request: LlmRequest): LlmResponse =
        LlmResponse.Text("The assistant isn't configured yet. Add an API key to enable it.")
}
