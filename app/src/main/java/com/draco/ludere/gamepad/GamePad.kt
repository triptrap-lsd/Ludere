package com.draco.ludere.gamepad

import android.app.Activity
import android.app.Service
import android.content.Context
import android.content.pm.PackageManager
import android.hardware.display.DisplayManager
import android.os.Build
import android.view.Display
import android.view.InputDevice
import android.view.WindowManager
import androidx.appcompat.app.AppCompatActivity
import com.draco.ludere.R
import com.swordfish.libretrodroid.GLRetroView
import io.github.controlwear.virtual.joystick.android.JoystickView
import io.reactivex.disposables.CompositeDisposable
import kotlin.math.cos
import kotlin.math.sin

class GamePad(
    context: Context,
) {
    val pad = JoystickView(context)

    companion object {
        /**
         * Should the user see the on-screen controls?
         */
        @Suppress("DEPRECATION")
        fun shouldShowGamePads(activity: Activity): Boolean {
            /* Config says we shouldn't use virtual controls */
            if (!activity.resources.getBoolean(R.bool.config_gamepad))
                return false

            /* Devices without a touchscreen don't need a GamePad */
            val hasTouchScreen = activity.packageManager?.hasSystemFeature(PackageManager.FEATURE_TOUCHSCREEN)
            if (hasTouchScreen == null || hasTouchScreen == false)
                return false

            /* Fetch the current display that the game is running on */
            val currentDisplayId = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R)
                activity.display!!.displayId
            else {
                val wm = activity.getSystemService(AppCompatActivity.WINDOW_SERVICE) as WindowManager
                wm.defaultDisplay.displayId
            }

            /* Are we presenting this screen on a TV or display? */
            val dm = activity.getSystemService(Service.DISPLAY_SERVICE) as DisplayManager
            if (dm.getDisplay(currentDisplayId).flags and Display.FLAG_PRESENTATION == Display.FLAG_PRESENTATION)
                return false

            /* If a GamePad is connected, we definitely don't need touch controls */
            for (id in InputDevice.getDeviceIds()) {
                InputDevice.getDevice(id).apply {
                    if (sources and InputDevice.SOURCE_GAMEPAD == InputDevice.SOURCE_GAMEPAD)
                        return false
                }
            }

            return true
        }
    }

    /**
     * Register input events to the RetroView
     */
    fun subscribe(compositeDisposable: CompositeDisposable, retroView: GLRetroView) {
        pad.setOnMoveListener { angle, strength ->
            // Apply dead zone to filter tiny inputs
            val deadZone = 10
            if (strength < deadZone) {
                retroView.sendMotionEvent(GLRetroView.MOTION_SOURCE_DPAD, 0f, 0f)
                return@setOnMoveListener
            }

            // Convert angle and strength to x, y coordinates
            val rad = Math.toRadians(angle.toDouble())
            val x = (cos(rad) * (strength / 100.0)).toFloat()
            val y = (sin(rad) * (strength / 100.0)).toFloat()

            // Send motion event to RetroView (using analog left stick)
            retroView.sendMotionEvent(GLRetroView.MOTION_SOURCE_ANALOG_LEFT, x, y)
        }
    }
}
