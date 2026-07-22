package com.appkitbox.winui4k

import com.appkitbox.winui4k.internal.com.ComPtr
import com.appkitbox.winui4k.internal.winrt.Activation
import com.appkitbox.winui4k.internal.winui.NotificationInterop

/**
 * The taskbar icon's badge: the Windows App SDK's BadgeNotificationManager.
 * Overlays an unread count ([setCount]) or a status glyph ([setGlyph]) on the app's taskbar icon.
 */
object WBadgeNotification {
    /** BadgeNotificationManager.Current (the default interface's pointer). */
    private val manager: ComPtr by lazy {
        val statics = Activation.factory(
            NotificationInterop.CLS_BadgeNotificationManager, NotificationInterop.IID_IBadgeNotificationManagerStatics,
        )
        val m = statics.getPtr(NotificationInterop.IBadgeNotificationManagerStatics_get_Current)
        statics.release()
        m
    }

    /** Sets the badge to a number (e.g. an unread count). 100 and above shows as "99+". */
    fun setCount(count: Int) {
        require(count >= 0) { "count must be 0 or greater: $count" }
        manager.call(NotificationInterop.IBadgeNotificationManager_SetBadgeAsCount, count)
    }

    /** Sets the badge to a status glyph (alarm / playing, etc.). */
    fun setGlyph(glyph: BadgeGlyph) {
        manager.call(NotificationInterop.IBadgeNotificationManager_SetBadgeAsGlyph, glyph.native)
    }

    /** Clears the badge. */
    fun clear() {
        manager.call(NotificationInterop.IBadgeNotificationManager_ClearBadge)
    }
}

/** A status glyph shown on the badge (BadgeNotificationGlyph). Values extracted from the winmd. */
enum class BadgeGlyph(internal val native: Int) {
    /** No badge. */
    NONE(0),

    /** There's activity. */
    ACTIVITY(1),

    /** An alarm is set. */
    ALARM(2),

    /** Alert. */
    ALERT(3),

    /** Attention. */
    ATTENTION(4),

    /** Available. */
    AVAILABLE(5),

    /** Away. */
    AWAY(6),

    /** Busy. */
    BUSY(7),

    /** Error. */
    ERROR(8),

    /** New message. */
    NEW_MESSAGE(9),

    /** Paused. */
    PAUSED(10),

    /** Playing. */
    PLAYING(11),

    /** Unavailable. */
    UNAVAILABLE(12),
}
