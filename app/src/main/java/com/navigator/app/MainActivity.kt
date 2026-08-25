package com.navigator.app

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.navigator.app.agent.PlaceholderLlmClient
import com.navigator.app.databinding.ActivityMainBinding
import com.navigator.app.nav.GoogleNavigationManager
import com.navigator.app.voice.SpeechInput
import com.navigator.app.voice.TtsManager
import com.navigator.app.voice.VoiceSession
import com.navigator.app.watch.NotificationWatchOutput
import com.navigator.core.agent.NavigatorAgent
import com.navigator.core.format.NotificationFormatter
import com.navigator.core.geo.LatLng
import com.navigator.core.nav.ManeuverType
import com.navigator.core.nav.NavigationState
import com.navigator.core.nav.NavigationStateStore
import com.navigator.core.nav.RouteStatus
import com.navigator.core.policy.WatchUpdatePolicy
import com.navigator.core.tools.NavigatorToolset
import com.navigator.core.tools.ToolArgs
import com.navigator.core.tools.ToolContext
import com.navigator.core.trip.Stop
import com.navigator.core.trip.TripManager

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val formatter = NotificationFormatter()
    private val watchOutput by lazy { NotificationWatchOutput(this) }
    private val tts by lazy { TtsManager(this) }
    private val speech by lazy { SpeechInput(this) }
    private val stateStore = NavigationStateStore()
    private val watchPolicy = WatchUpdatePolicy()
    private val navManager by lazy { GoogleNavigationManager(application, stateStore) }
    private val tripManager = TripManager()
    private val toolRegistry by lazy {
        NavigatorToolset.standard(ToolContext(tripManager, navManager, stateStore))
    }
    private val agent by lazy { NavigatorAgent(PlaceholderLlmClient(), toolRegistry) }
    private val voiceSession by lazy {
        VoiceSession(
            speech = speech,
            tts = tts,
            respond = { transcript ->
                val response = agent.handle(transcript, liveOrSampleState())
                showStatus(response.spoken)
                response.spoken
            },
        )
    }

    private val requestNotifications =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (granted) postTestUpdate() else showStatus(getString(R.string.notifications_denied))
        }

    private val requestMic =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (granted) startVoice() else showStatus(getString(R.string.mic_denied))
        }

    private val requestLocation =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (granted) startNavSession() else showStatus(getString(R.string.location_denied))
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Live navigation state (SDK / bridge) flows to the watch, throttled to avoid spam.
        stateStore.setListener { state ->
            formatter.format(state)?.let { update ->
                if (watchPolicy.shouldPost(update)) watchOutput.show(update)
            }
        }

        binding.testWatchButton.setOnClickListener { onTestWatchClicked() }
        binding.enableBridgeButton.setOnClickListener {
            startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
        }
        binding.voiceButton.setOnClickListener { onVoiceClicked() }
        binding.startNavButton.setOnClickListener { onStartNavClicked() }
        binding.setDestButton.setOnClickListener {
            runTool("set_destination", ToolArgs.of("name" to "Office", "lat" to 12.9698, "lng" to 77.7500))
        }
        binding.addStopButton.setOnClickListener {
            runTool("add_stop", ToolArgs.of("name" to "Orion Mall", "lat" to 13.0359, "lng" to 77.5970))
        }
        binding.clearStopsButton.setOnClickListener { runTool("clear_stops", ToolArgs.EMPTY) }
    }

    private fun onTestWatchClicked() {
        val needsPermission = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        if (needsPermission) {
            requestNotifications.launch(Manifest.permission.POST_NOTIFICATIONS)
            return
        }
        postTestUpdate()
    }

    private fun postTestUpdate() {
        val update = formatter.format(sampleState()) ?: return
        watchOutput.show(update)
        showStatus(getString(R.string.test_posted, update.primary))
    }

    private fun onVoiceClicked() {
        if (!speech.isAvailable()) {
            showStatus(getString(R.string.speech_unavailable))
            return
        }
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            requestMic.launch(Manifest.permission.RECORD_AUDIO)
            return
        }
        startVoice()
    }

    private fun startVoice() {
        showStatus(getString(R.string.listening))
        voiceSession.listen(
            onTranscript = { transcript -> showStatus(getString(R.string.heard, transcript)) },
            onError = { code -> showStatus(getString(R.string.voice_error, code)) },
        )
    }

    private fun onStartNavClicked() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            requestLocation.launch(Manifest.permission.ACCESS_FINE_LOCATION)
            return
        }
        startNavSession()
    }

    private fun startNavSession() {
        showStatus(getString(R.string.nav_initializing))
        navManager.initialize { ready ->
            if (!ready) {
                showStatus(getString(R.string.nav_error))
                return@initialize
            }
            val office = Stop(id = "office", name = "Office", location = LatLng(12.9698, 77.7500))
            navManager.setDestinations(office)
            navManager.startGuidance()
            navManager.startSimulation()
            showStatus(getString(R.string.nav_ready))
        }
    }

    private fun liveOrSampleState(): NavigationState =
        stateStore.current.takeIf { it != NavigationState.EMPTY } ?: sampleState()

    private fun sampleState() = NavigationState(
        currentRoad = "Outer Ring Road",
        nextManeuver = ManeuverType.TURN_LEFT,
        nextManeuverDistanceMeters = 200,
        nextRoad = "100 Feet Road",
        destinationName = "Office",
        etaSeconds = 900,
        remainingDistanceMeters = 5400,
        routeStatus = RouteStatus.ON_ROUTE,
    )

    private fun runTool(name: String, args: ToolArgs) {
        showStatus(toolRegistry.execute(name, args).message)
    }

    private fun showStatus(text: String) {
        binding.statusText.text = text
    }

    override fun onDestroy() {
        speech.destroy()
        tts.shutdown()
        super.onDestroy()
    }
}
