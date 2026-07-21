package jp.hisano.winui4k.swing

import jp.hisano.winui4k.ffi.ComPtr
import jp.hisano.winui4k.winrt.WinRt
import jp.hisano.winui4k.winui.Abi

/**
 * The taskbar icon's badge: the Windows App SDK's BadgeNotificationManager.
 * Overlays an unread count ([setCount]) or a status glyph ([setGlyph]) on the app's taskbar icon.
 */
object WBadgeNotification {
    /** BadgeNotificationManager.Current (the default interface's pointer). */
    private val manager: ComPtr by lazy {
        val statics = WinRt.factory(
            Abi.CLS_BadgeNotificationManager, Abi.IID_IBadgeNotificationManagerStatics,
        )
        val m = statics.getPtr(Abi.IBadgeNotificationManagerStatics_get_Current)
        statics.release()
        m
    }

    /** Sets the badge to a number (e.g. an unread count). 100 and above shows as "99+". */
    fun setCount(count: Int) {
        require(count >= 0) { "count must be 0 or greater: $count" }
        manager.call(Abi.IBadgeNotificationManager_SetBadgeAsCount, count)
    }

    /** Sets the badge to a status glyph (alarm / playing, etc.). */
    fun setGlyph(glyph: BadgeGlyph) {
        manager.call(Abi.IBadgeNotificationManager_SetBadgeAsGlyph, glyph.native)
    }

    /** Clears the badge. */
    fun clear() {
        manager.call(Abi.IBadgeNotificationManager_ClearBadge)
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
