package com.appkitbox.winui4k

import com.appkitbox.winui4k.internal.com.ComPtr
import com.appkitbox.winui4k.internal.winrt.Activation
import com.appkitbox.winui4k.internal.winrt.Hstring
import com.appkitbox.winui4k.internal.winrt.addEventHandler
import com.appkitbox.winui4k.internal.winrt.getString
import com.appkitbox.winui4k.internal.winui.Abi
import com.appkitbox.winui4k.internal.winui.Dispatcher

/**
 * TrayIcon.displayMessage-like: the Windows App SDK's toast notification, AppNotification.
 * Wraps an AppNotificationBuilder; build up the content with [addText] and friends, then
 * send it to the Action Center with [WAppNotificationManager.show].
 *
 * ```
 * WAppNotificationManager.register()
 * WAppNotificationManager.show(
 *     WAppNotification("Meeting reminder")
 *         .addText("Design review at 3:00 PM")
 *         .addButton("Join", "action" to "join"),
 * )
 * ```
 */
class WAppNotification(text: String = "") {
    private val builder: ComPtr =
        Activation.activate(Abi.CLS_AppNotificationBuilder)
            .queryInterface(Abi.IID_IAppNotificationBuilder)

    init {
        if (text.isNotEmpty()) addText(text)
    }

    /** Adds one line of body text (the first line is treated as the title; up to 3 lines). */
    fun addText(text: String): WAppNotification {
        Hstring.use(text) { h ->
            builder.getPtr(Abi.IAppNotificationBuilder_AddText, h).release() // the return value is the builder itself
        }
        return this
    }

    /** Sets the attribution text at the bottom (e.g. a description of the sender). */
    fun setAttributionText(text: String): WAppNotification {
        Hstring.use(text) { h ->
            builder.getPtr(Abi.IAppNotificationBuilder_SetAttributionText, h).release()
        }
        return this
    }

    /** Adds an argument (key=value) passed to the app when the notification body is clicked. */
    fun addArgument(key: String, value: String): WAppNotification {
        Hstring.use(key) { k ->
            Hstring.use(value) { v ->
                builder.getPtr(Abi.IAppNotificationBuilder_AddArgument, k, v).release()
            }
        }
        return this
    }

    /** Adds a button to the notification. On click, [arguments] is passed to NotificationInvoked. */
    fun addButton(content: String, vararg arguments: Pair<String, String>): WAppNotification {
        val factory = Activation.factory(Abi.CLS_AppNotificationButton, Abi.IID_IAppNotificationButtonFactory)
        val button = Hstring.use(content) { h ->
            factory.getPtr(Abi.IAppNotificationButtonFactory_CreateInstance, h)
        }
        factory.release()
        for ((key, value) in arguments) {
            Hstring.use(key) { k ->
                Hstring.use(value) { v ->
                    button.getPtr(Abi.IAppNotificationButton_AddArgument, k, v).release()
                }
            }
        }
        builder.getPtr(Abi.IAppNotificationBuilder_AddButton, button.ptr).release()
        button.release()
        return this
    }

    /** Sets the notification's purpose (reminder / alarm / urgent, etc.). */
    fun setScenario(scenario: NotificationScenario): WAppNotification {
        builder.getPtr(Abi.IAppNotificationBuilder_SetScenario, scenario.native).release()
        return this
    }

    /** Sets the display duration (default / long). */
    fun setDuration(duration: NotificationDuration): WAppNotification {
        builder.getPtr(Abi.IAppNotificationBuilder_SetDuration, duration.native).release()
        return this
    }

    /** Sets a tag used to identify this notification later, e.g. with RemoveByTagAsync. */
    fun setTag(tag: String): WAppNotification {
        Hstring.use(tag) { h ->
            builder.getPtr(Abi.IAppNotificationBuilder_SetTag, h).release()
        }
        return this
    }

    /** Sets a group name used together with the tag. */
    fun setGroup(group: String): WAppNotification {
        Hstring.use(group) { h ->
            builder.getPtr(Abi.IAppNotificationBuilder_SetGroup, h).release()
        }
        return this
    }

    /** Builds the AppNotification (default interface) via BuildNotification. */
    internal fun build(): ComPtr = builder.getPtr(Abi.IAppNotificationBuilder_BuildNotification)
}

/**
 * The notification's purpose (AppNotificationScenario). Values extracted from the winmd.
 * Affects the default display duration, sound, and priority depending on the purpose.
 */
enum class NotificationScenario(internal val native: Int) {
    /** A regular notification. */
    DEFAULT(0),

    /** A reminder (stays shown until responded to). */
    REMINDER(1),

    /** An alarm. */
    ALARM(2),

    /** An incoming call. */
    INCOMING_CALL(3),

    /** Urgent (shown even in do-not-disturb mode). */
    URGENT(4),
}

/** The notification's display duration (AppNotificationDuration). Values extracted from the winmd. */
enum class NotificationDuration(internal val native: Int) {
    /** Default (a few seconds). */
    DEFAULT(0),

    /** Long (about 25 seconds). */
    LONG(1),
}

/**
 * Whether notifications are currently enabled (AppNotificationSetting). Values extracted from the winmd.
 * Returned by [WAppNotificationManager.setting].
 */
enum class NotificationSetting(internal val native: Int) {
    /** Notifications are enabled. */
    ENABLED(0),

    /** This app's notifications are disabled by user setting. */
    DISABLED_FOR_APPLICATION(1),

    /** All of the user's notifications are disabled. */
    DISABLED_FOR_USER(2),

    /** Disabled by group policy. */
    DISABLED_BY_GROUP_POLICY(3),

    /** Disabled by the app manifest. */
    DISABLED_BY_MANIFEST(4),

    /** Notifications themselves aren't supported in this environment. */
    UNSUPPORTED(5),

    ;

    internal companion object {
        fun of(native: Int) = entries.first { it.native == native }
    }
}

/**
 * The window for sending toast notifications: the Windows App SDK's AppNotificationManager.
 * Call [register] once before use (registers this process to receive notification clicks).
 */
object WAppNotificationManager {
    /** AppNotificationManager.Default (the default interface's pointer). */
    private val manager: ComPtr by lazy {
        val statics = Activation.factory(Abi.CLS_AppNotificationManager, Abi.IID_IAppNotificationManagerStatics)
        val m = statics.getPtr(Abi.IAppNotificationManagerStatics_get_Default)
        statics.release()
        m
    }

    /** Listeners registered via addNotificationInvokedListener. */
    private val invokedListeners = mutableListOf<(String) -> Unit>()

    /**
     * Registers exactly one COM NotificationInvoked handler and fans it out to all listeners.
     * add_NotificationInvoked returns E_ILLEGAL_METHOD_CALL after Register, so the COM
     * registration must not be repeated as listeners come and go.
     */
    private var invokedHandlerAdded = false

    /** Whether AppNotification is usable in this environment (AppNotificationManager.IsSupported). */
    val isSupported: Boolean
        get() {
            val statics2 = Activation.factory(
                Abi.CLS_AppNotificationManager, Abi.IID_IAppNotificationManagerStatics2,
            )
            return try {
                statics2.getBool(Abi.IAppNotificationManagerStatics2_IsSupported)
            } finally {
                statics2.release()
            }
        }

    /** Whether notifications are currently enabled (AppNotificationManager.Setting). */
    val setting: NotificationSetting
        get() = NotificationSetting.of(manager.getInt(Abi.IAppNotificationManager_get_Setting))

    /**
     * Registers this process as a notification sender (AppNotificationManager.Register).
     * For an unpackaged app, this registers automatically using the executable's display
     * name and icon.
     */
    fun register() {
        manager.call(Abi.IAppNotificationManager_Register)
    }

    /** Unregisters the receiver for notification clicks (AppNotificationManager.Unregister). */
    fun unregister() {
        manager.call(Abi.IAppNotificationManager_Unregister)
    }

    /** Sends a notification (AppNotificationManager.Show). */
    fun show(notification: WAppNotification) {
        val built = notification.build()
        manager.call(Abi.IAppNotificationManager_Show, built.ptr)
        built.release()
    }

    /**
     * Subscribes to clicks on the notification body or a button. The listener receives the
     * arguments set via addArgument / addButton, formatted as "key=value;key2=value2".
     * The callback is moved to the UI thread before being invoked, so it's safe to use W*
     * APIs inside the listener.
     *
     * Call this for the first time before [register] (subscribing for the first time after
     * Register makes the WinAppSDK side return E_ILLEGAL_METHOD_CALL).
     */
    fun addNotificationInvokedListener(listener: (String) -> Unit) {
        if (!invokedHandlerAdded) {
            manager.addEventHandler(
                "WinUI4K.NotificationInvokedHandler",
                Abi.IID_NotificationInvokedHandler,
                Abi.IAppNotificationManager_add_NotificationInvoked,
            ) { _, args ->
                // This callback arrives off the UI thread, so read the argument first and move it over
                val eventArgs = ComPtr(args).queryInterface(Abi.IID_IAppNotificationActivatedEventArgs)
                val argument = eventArgs.getString(Abi.IAppNotificationActivatedEventArgs_get_Argument)
                eventArgs.release()
                Dispatcher.invokeLater { invokedListeners.toList().forEach { it(argument) } }
            }
            invokedHandlerAdded = true
        }
        invokedListeners.add(listener)
    }

    /** Unsubscribes a listener registered via [addNotificationInvokedListener]. */
    fun removeNotificationInvokedListener(listener: (String) -> Unit) {
        invokedListeners.remove(listener)
    }
}
