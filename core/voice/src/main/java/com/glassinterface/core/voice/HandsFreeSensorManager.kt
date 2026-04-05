package com.glassinterface.core.voice

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.sqrt

/**
 * Monitors accelerometer and proximity sensors to provide Hands-Free trigger events.
 */
@Singleton
class HandsFreeSensorManager @Inject constructor(
    @ApplicationContext private val context: Context
) : SensorEventListener {

    companion object {
        private const val TAG = "HandsFreeSensor"
        private const val SHAKE_THRESHOLD_G = 3.2f // Higher threshold so walking doesn't trigger it
        private const val SHAKE_COOLDOWN_MS = 2000L
        private const val PROXIMITY_COOLDOWN_MS = 2000L
    }

    private var sensorManager: SensorManager? = null
    private var accelerometer: Sensor? = null
    private var proximity: Sensor? = null

    private var onTrigger: (() -> Unit)? = null
    private var isListening = false

    // Shake state
    private var lastShakeTime = 0L

    // Proximity state
    private var lastProximityTime = 0L

    private var shakeEnabled = false
    private var proximityEnabled = false

    init {
        sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
        accelerometer = sensorManager?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        proximity = sensorManager?.getDefaultSensor(Sensor.TYPE_PROXIMITY)
    }

    /**
     * Start listening to sensors if they are enabled via settings.
     */
    fun startListening(
        enableShake: Boolean,
        enableProximity: Boolean,
        onTriggerCallback: () -> Unit
    ) {
        this.shakeEnabled = enableShake
        this.proximityEnabled = enableProximity
        this.onTrigger = onTriggerCallback

        if (!isListening) {
            if (shakeEnabled) {
                accelerometer?.let {
                    sensorManager?.registerListener(this, it, SensorManager.SENSOR_DELAY_UI)
                    Log.i(TAG, "Registered Accelerometer for Shake-to-Wake")
                }
            }
            if (proximityEnabled) {
                proximity?.let {
                    sensorManager?.registerListener(this, it, SensorManager.SENSOR_DELAY_UI)
                    Log.i(TAG, "Registered Proximity Sensor for Wave-to-Wake")
                }
            }
            isListening = true
        } else {
            // Update sensor registrations based on new config
            updateRegistrations()
        }
    }

    fun stopListening() {
        if (isListening) {
            sensorManager?.unregisterListener(this)
            isListening = false
            Log.i(TAG, "Unregistered sensors")
        }
    }

    private fun updateRegistrations() {
        sensorManager?.unregisterListener(this)
        if (shakeEnabled) {
            accelerometer?.let { sensorManager?.registerListener(this, it, SensorManager.SENSOR_DELAY_UI) }
        }
        if (proximityEnabled) {
            proximity?.let { sensorManager?.registerListener(this, it, SensorManager.SENSOR_DELAY_UI) }
        }
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event == null) return
        val now = System.currentTimeMillis()

        if (event.sensor.type == Sensor.TYPE_ACCELEROMETER && shakeEnabled) {
            val x = event.values[0]
            val y = event.values[1]
            val z = event.values[2]
            val gForce = sqrt((x * x + y * y + z * z).toDouble()) / SensorManager.GRAVITY_EARTH

            if (gForce > SHAKE_THRESHOLD_G) {
                if (now - lastShakeTime > SHAKE_COOLDOWN_MS) {
                    lastShakeTime = now
                    Log.i(TAG, "Shake detected (gForce: ${"%.1f".format(gForce)})! Triggering mic.")
                    onTrigger?.invoke()
                }
            }
        } else if (event.sensor.type == Sensor.TYPE_PROXIMITY && proximityEnabled) {
            val distance = event.values[0]
            // Usually 0.0 means near, and maximumRange means far
            if (distance < 2.0f) { // Within 2cm (wave hand over sensor)
                if (now - lastProximityTime > PROXIMITY_COOLDOWN_MS) {
                    lastProximityTime = now
                    Log.i(TAG, "Proximity wave detected! Triggering mic.")
                    onTrigger?.invoke()
                }
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
}
