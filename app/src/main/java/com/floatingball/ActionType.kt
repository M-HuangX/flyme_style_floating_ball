package com.floatingball

/**
 * Actions that can be triggered by gestures on the floating ball.
 */
enum class ActionType(val displayName: String) {
    BACK("Back"),
    HOME("Home"),
    RECENTS("Recent Apps"),
    NOTIFICATIONS("Notifications"),
    QUICK_SETTINGS("Quick Settings"),
    SCREENSHOT("Screenshot"),
    LOCK_SCREEN("Lock Screen"),
    ONE_HANDED("One-Handed Mode"),
    NONE("None");

    companion object {
        fun fromString(value: String): ActionType {
            return entries.find { it.name == value } ?: BACK
        }
    }
}
