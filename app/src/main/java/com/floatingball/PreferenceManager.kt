package com.floatingball

import android.content.Context
import android.content.SharedPreferences
import androidx.preference.PreferenceManager as AndroidXPreferenceManager

/**
 * Wrapper around SharedPreferences for ball configuration.
 *
 * Uses AndroidX default shared preferences (same as PreferenceFragment),
 * so settings changes are immediately picked up by the service.
 */
object PreferenceManager {

    private const val KEY_BALL_POS_X = "ball_pos_x"
    private const val KEY_BALL_POS_Y = "ball_pos_y"

    private fun prefs(context: Context): SharedPreferences =
        AndroidXPreferenceManager.getDefaultSharedPreferences(context)

    // Ball position
    fun getBallPosX(context: Context): Int = prefs(context).getInt(KEY_BALL_POS_X, -1)
    fun getBallPosY(context: Context): Int = prefs(context).getInt(KEY_BALL_POS_Y, -1)
    fun saveBallPosition(context: Context, x: Int, y: Int) {
        prefs(context).edit().putInt(KEY_BALL_POS_X, x).putInt(KEY_BALL_POS_Y, y).apply()
    }

    // Ball size (dp) — SeekBarPreference stores as Int
    fun getBallSizeDp(context: Context): Int {
        val prefs = prefs(context)
        return try {
            prefs.getInt("ball_size_dp", 38)
        } catch (e: ClassCastException) {
            // Old value may have been stored as String
            prefs.getString("ball_size_dp", "38")?.toIntOrNull() ?: 38
        }
    }

    // Ball opacity (20-100) — SeekBarPreference stores as Int
    fun getBallOpacity(context: Context): Int {
        val prefs = prefs(context)
        return try {
            prefs.getInt("ball_opacity", 55)
        } catch (e: ClassCastException) {
            prefs.getString("ball_opacity", "55")?.toIntOrNull() ?: 55
        }
    }

    // Gesture action mappings
    fun getClickAction(context: Context): ActionType =
        ActionType.fromString(prefs(context).getString("action_click", "BACK") ?: "BACK")

    fun getDoubleClickAction(context: Context): ActionType =
        ActionType.fromString(prefs(context).getString("action_double_click", "ONE_HANDED") ?: "ONE_HANDED")

    fun getSwipeUpAction(context: Context): ActionType =
        ActionType.fromString(prefs(context).getString("action_swipe_up", "HOME") ?: "HOME")

    fun getSwipeDownAction(context: Context): ActionType =
        ActionType.fromString(prefs(context).getString("action_swipe_down", "NOTIFICATIONS") ?: "NOTIFICATIONS")

    fun getSwipeLeftAction(context: Context): ActionType =
        ActionType.fromString(prefs(context).getString("action_swipe_left", "QUICK_SETTINGS") ?: "QUICK_SETTINGS")

    fun getSwipeRightAction(context: Context): ActionType =
        ActionType.fromString(prefs(context).getString("action_swipe_right", "RECENTS") ?: "RECENTS")
}
