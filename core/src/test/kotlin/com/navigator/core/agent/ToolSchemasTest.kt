package com.navigator.core.agent

import com.navigator.core.nav.NavigationController
import com.navigator.core.nav.NavigationStateStore
import com.navigator.core.trip.Stop
import com.navigator.core.trip.TripManager
import com.navigator.core.tools.NavigatorToolset
import com.navigator.core.tools.ToolContext
import com.navigator.core.util.Clock
import org.junit.Assert.assertTrue
import org.junit.Test

class ToolSchemasTest {

    private class NoOpController : NavigationController {
        override fun setDestinations(destination: Stop, stops: List<Stop>) {}
        override fun startGuidance() {}
        override fun stopGuidance() {}
        override fun clearNavigation() {}
        override fun startSimulation() {}
        override fun stopSimulation() {}
    }

    private fun registry() = NavigatorToolset.standard(
        ToolContext(TripManager(Clock { 0L }), NoOpController(), NavigationStateStore()),
    )

    @Test
    fun schemas_cover_the_registered_tools() {
        val schemas = ToolSchemas.forRegistry(registry())
        assertTrue(schemas.any { it.name == "add_stop" })
        assertTrue(schemas.any { it.name == "set_destination" })
    }

    @Test
    fun json_has_function_name_required_and_types() {
        val json = ToolSchemas.toJson(ToolSchemas.forRegistry(registry()))
        assertTrue(json.startsWith("[") && json.endsWith("]"))
        assertTrue(json.contains("\"name\":\"add_stop\""))
        assertTrue(json.contains("\"required\":[\"name\",\"lat\",\"lng\"]"))
        assertTrue(json.contains("\"type\":\"number\""))
    }
}
