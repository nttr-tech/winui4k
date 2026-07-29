package com.appkitbox.winui4k.sample.gallery;

import com.appkitbox.winui4k.CompactOverlaySize;
import com.appkitbox.winui4k.GridLength;
import com.appkitbox.winui4k.HorizontalAlignment;
import com.appkitbox.winui4k.NavigationViewBackButtonVisible;
import com.appkitbox.winui4k.Orientation;
import com.appkitbox.winui4k.SystemBackdropType;
import com.appkitbox.winui4k.TextWrapping;
import com.appkitbox.winui4k.TitleBarHeightOption;
import com.appkitbox.winui4k.TitleBarTheme;
import com.appkitbox.winui4k.WAppWindow;
import com.appkitbox.winui4k.WAppWindowPresenterKind;
import com.appkitbox.winui4k.WAppWindowTitleBar;
import com.appkitbox.winui4k.WAutoSuggestBox;
import com.appkitbox.winui4k.WButton;
import com.appkitbox.winui4k.WCheckBox;
import com.appkitbox.winui4k.WColor;
import com.appkitbox.winui4k.WColorPicker;
import com.appkitbox.winui4k.WComboBox;
import com.appkitbox.winui4k.WCompactOverlayPresenter;
import com.appkitbox.winui4k.WComponent;
import com.appkitbox.winui4k.WDimension;
import com.appkitbox.winui4k.WDisplayArea;
import com.appkitbox.winui4k.WFrame;
import com.appkitbox.winui4k.WFullScreenPresenter;
import com.appkitbox.winui4k.WGrid;
import com.appkitbox.winui4k.WLabel;
import com.appkitbox.winui4k.WNavigationView;
import com.appkitbox.winui4k.WNavigationViewItem;
import com.appkitbox.winui4k.WOverlappedPresenter;
import com.appkitbox.winui4k.WPanel;
import com.appkitbox.winui4k.WRectangle;
import com.appkitbox.winui4k.WTextField;
import com.appkitbox.winui4k.WTitleBar;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Function;
import kotlin.Unit;

/*
 * Windowing category: the four pages AppWindow / AppWindowTitleBar / Multiple windows / TitleBar.
 * The demos mostly manipulate the native window itself, so destructive operations (FullScreen /
 * CompactOverlay, switching the Presenter, and so on) are never applied to the main Gallery window - always
 * do them on a child window (a separate {@link WFrame}). Always attach a "close" button to child windows.
 */
final class WindowingPages {
    private WindowingPages() {
    }

    // region AppWindow page

    /** The AppWindow page: lines up demos for trying out WAppWindow's various features. */
    static WComponent buildAppWindowPage() {
        WPanel page = GalleryScaffold.buildPage(
            "AppWindow",
            "WinUI 3's AppWindow, which manages a native window (title, position, size, behavior). "
                + "All operations are performed on a child window and never affect the Gallery itself.");

        page.add(buildAppWindowBasicExample());
        page.add(buildAppWindowCenterExample());
        page.add(buildAppWindowPresenterExample());
        page.add(buildAppWindowMinMaxSizeExample());
        page.add(buildAppWindowModalExample());
        page.add(buildAppWindowFullScreenExample());
        page.add(buildAppWindowCompactOverlayExample());
        return page;
    }

    /** Creates and shows a single child window (with a close button). The basis for each demo below. */
    private static WFrame openChildFrame(String title) {
        WFrame frame = new WFrame(title);
        frame.setVisible(true);
        return frame;
    }

    /** The "close" button always placed at the top of a child window's content. */
    private static void addCloseButton(WFrame frame) {
        WButton closeButton = new WButton("Close this window");
        closeButton.addActionListener(() -> {
            frame.setVisible(false);
            return Unit.INSTANCE;
        });
        frame.add(closeButton);
    }

    /** 1) Title / Resize / Move / SetIcon. */
    private static WComponent buildAppWindowBasicExample() {
        WLabel status = new WLabel("No child window has been created yet");
        WFrame[] child = new WFrame[1];

        WButton openButton = new WButton("Open a child window");
        openButton.addActionListener(() -> {
            WFrame frame = openChildFrame("AppWindow demo (child window)");
            addCloseButton(frame);
            frame.add(new WLabel("You can operate this window's AppWindow."));
            frame.getAppWindow().resize(480, 320);
            child[0] = frame;
            status.setText("Opened a child window (Title = \"" + frame.getAppWindow().getTitle() + "\")");
            return Unit.INSTANCE;
        });

        WButton titleButton = new WButton("Change the title");
        titleButton.addActionListener(() -> {
            WFrame frame = child[0];
            if (frame == null) {
                return Unit.INSTANCE;
            }
            frame.getAppWindow().setTitle("Changed title (" + (System.currentTimeMillis() % 1000) + ")");
            status.setText("Title = \"" + frame.getAppWindow().getTitle() + "\"");
            return Unit.INSTANCE;
        });

        WButton resizeButton = new WButton("Resize(640, 480)");
        resizeButton.addActionListener(() -> {
            WFrame frame = child[0];
            if (frame == null) {
                return Unit.INSTANCE;
            }
            frame.getAppWindow().resize(640, 480);
            status.setText("Size = " + frame.getAppWindow().getSize()
                + " / ClientSize = " + frame.getAppWindow().getClientSize());
            return Unit.INSTANCE;
        });

        WButton moveButton = new WButton("Move(100, 100)");
        moveButton.addActionListener(() -> {
            WFrame frame = child[0];
            if (frame == null) {
                return Unit.INSTANCE;
            }
            frame.getAppWindow().move(100, 100);
            status.setText("Position = " + frame.getAppWindow().getPosition());
            return Unit.INSTANCE;
        });

        WPanel buttons = new WPanel(8.0, Orientation.HORIZONTAL);
        buttons.add(openButton);
        buttons.add(titleButton);
        buttons.add(resizeButton);
        buttons.add(moveButton);

        WPanel body = new WPanel(8.0, Orientation.VERTICAL);
        body.add(buttons);
        body.add(status);
        return GalleryScaffold.buildExample(
            "Title, size, and position (Title / Resize / Move / Size / ClientSize / Position)", body);
    }

    /** 2) Screen centering: computes the center point from WDisplayArea.nearest(appWindow).workArea. */
    private static WComponent buildAppWindowCenterExample() {
        WLabel status = new WLabel("No child window has been created yet");
        WFrame[] child = new WFrame[1];

        WButton openButton = new WButton("Open a child window (placed toward the bottom-right of the screen)");
        openButton.addActionListener(() -> {
            WFrame frame = openChildFrame("Screen centering demo (child window)");
            addCloseButton(frame);
            frame.add(new WLabel("The \"Center it\" button computes the center point from DisplayArea.WorkArea."));
            frame.getAppWindow().resize(360, 240);
            frame.getAppWindow().move(50, 50); // first place it near the edge of the screen
            child[0] = frame;
            status.setText("Opened a child window");
            return Unit.INSTANCE;
        });

        WButton centerButton = new WButton("Center it");
        centerButton.addActionListener(() -> {
            WFrame frame = child[0];
            if (frame == null) {
                return Unit.INSTANCE;
            }
            WRectangle workArea = WDisplayArea.Companion.nearest(frame.getAppWindow()).getWorkArea();
            WDimension size = frame.getAppWindow().getSize();
            int x = workArea.getX() + (workArea.getWidth() - size.getWidth()) / 2;
            int y = workArea.getY() + (workArea.getHeight() - size.getHeight()) / 2;
            frame.getAppWindow().move(x, y);
            status.setText("WorkArea = " + workArea + " → Position = " + frame.getAppWindow().getPosition());
            return Unit.INSTANCE;
        });

        WPanel buttons = new WPanel(8.0, Orientation.HORIZONTAL);
        buttons.add(openButton);
        buttons.add(centerButton);

        WPanel body = new WPanel(8.0, Orientation.VERTICAL);
        body.add(buttons);
        body.add(status);
        return GalleryScaffold.buildExample("Screen centering (WDisplayArea.nearest / workArea)", body);
    }

    /** 3) OverlappedPresenter's 6 toggles + Maximize/Minimize/Restore + the State display. */
    private static WComponent buildAppWindowPresenterExample() {
        WLabel status = new WLabel("No child window has been created yet");
        WFrame[] child = new WFrame[1];
        WOverlappedPresenter[] presenter = new WOverlappedPresenter[1];

        WButton openButton = new WButton("Open a child window");
        openButton.addActionListener(() -> {
            WFrame frame = openChildFrame("OverlappedPresenter demo (child window)");
            addCloseButton(frame);
            frame.add(new WLabel("Use the toggles and buttons to operate OverlappedPresenter's properties."));
            frame.getAppWindow().resize(480, 360);
            WOverlappedPresenter newPresenter = WOverlappedPresenter.Companion.create();
            frame.getAppWindow().setPresenter(newPresenter);
            child[0] = frame;
            presenter[0] = newPresenter;
            status.setText("state = " + newPresenter.getState());
            return Unit.INSTANCE;
        });

        Runnable refreshState = () ->
            status.setText("state = " + (presenter[0] == null ? null : presenter[0].getState()));

        WCheckBox alwaysOnTopCheck = new WCheckBox("IsAlwaysOnTop");
        alwaysOnTopCheck.addItemListener(checked -> {
            if (presenter[0] != null) {
                presenter[0].setAlwaysOnTop(Boolean.TRUE.equals(checked));
            }
            return Unit.INSTANCE;
        });
        WCheckBox maximizableCheck = new WCheckBox("IsMaximizable");
        maximizableCheck.setChecked(true);
        maximizableCheck.addItemListener(checked -> {
            if (presenter[0] != null) {
                presenter[0].setMaximizable(Boolean.TRUE.equals(checked));
            }
            return Unit.INSTANCE;
        });
        WCheckBox minimizableCheck = new WCheckBox("IsMinimizable");
        minimizableCheck.setChecked(true);
        minimizableCheck.addItemListener(checked -> {
            if (presenter[0] != null) {
                presenter[0].setMinimizable(Boolean.TRUE.equals(checked));
            }
            return Unit.INSTANCE;
        });
        WCheckBox resizableCheck = new WCheckBox("IsResizable");
        resizableCheck.setChecked(true);
        resizableCheck.addItemListener(checked -> {
            if (presenter[0] != null) {
                presenter[0].setResizable(Boolean.TRUE.equals(checked));
            }
            return Unit.INSTANCE;
        });
        WCheckBox borderCheck = new WCheckBox("HasBorder");
        borderCheck.setChecked(true);
        WCheckBox titleBarCheck = new WCheckBox("HasTitleBar");
        titleBarCheck.setChecked(true);
        WButton applyBorderButton = new WButton("Apply Border/TitleBar (SetBorderAndTitleBar)");
        applyBorderButton.addActionListener(() -> {
            if (presenter[0] != null) {
                presenter[0].setBorderAndTitleBar(
                    Boolean.TRUE.equals(borderCheck.isChecked()),
                    Boolean.TRUE.equals(titleBarCheck.isChecked()));
            }
            return Unit.INSTANCE;
        });

        WPanel checks = new WPanel(8.0, Orientation.HORIZONTAL);
        checks.add(alwaysOnTopCheck);
        checks.add(maximizableCheck);
        checks.add(minimizableCheck);
        checks.add(resizableCheck);

        WPanel borderRow = new WPanel(8.0, Orientation.HORIZONTAL);
        borderRow.add(borderCheck);
        borderRow.add(titleBarCheck);
        borderRow.add(applyBorderButton);

        WButton maximizeButton = new WButton("Maximize");
        maximizeButton.addActionListener(() -> {
            if (presenter[0] != null) {
                presenter[0].maximize();
            }
            refreshState.run();
            return Unit.INSTANCE;
        });
        WButton minimizeButton = new WButton("Minimize");
        minimizeButton.addActionListener(() -> {
            if (presenter[0] != null) {
                presenter[0].minimize();
            }
            refreshState.run();
            return Unit.INSTANCE;
        });
        WButton restoreButton = new WButton("Restore");
        restoreButton.addActionListener(() -> {
            if (presenter[0] != null) {
                presenter[0].restore();
            }
            refreshState.run();
            return Unit.INSTANCE;
        });
        WPanel stateButtons = new WPanel(8.0, Orientation.HORIZONTAL);
        stateButtons.add(maximizeButton);
        stateButtons.add(minimizeButton);
        stateButtons.add(restoreButton);

        WPanel body = new WPanel(8.0, Orientation.VERTICAL);
        body.add(openButton);
        body.add(checks);
        body.add(borderRow);
        body.add(stateButtons);
        body.add(status);
        return GalleryScaffold.buildExample(
            "OverlappedPresenter (IsAlwaysOnTop / IsMaximizable / IsMinimizable / IsResizable / "
                + "SetBorderAndTitleBar / Maximize / Minimize / Restore / State)",
            body);
    }

    /** 4) Min/max size (PreferredMinimum/MaximumWidth/Height, null clears them). */
    private static WComponent buildAppWindowMinMaxSizeExample() {
        WLabel status = new WLabel("No child window has been created yet");
        WOverlappedPresenter[] presenter = new WOverlappedPresenter[1];

        WButton openButton = new WButton("Open a child window (min 300x200 / max 800x600)");
        openButton.addActionListener(() -> {
            WFrame frame = openChildFrame("Min/max size demo (child window)");
            addCloseButton(frame);
            frame.add(new WLabel("Drag-resize the window to see the constraints in action."));
            frame.getAppWindow().resize(400, 300);
            WOverlappedPresenter newPresenter = WOverlappedPresenter.Companion.create();
            newPresenter.setPreferredMinimumWidth(300);
            newPresenter.setPreferredMinimumHeight(200);
            newPresenter.setPreferredMaximumWidth(800);
            newPresenter.setPreferredMaximumHeight(600);
            frame.getAppWindow().setPresenter(newPresenter);
            presenter[0] = newPresenter;
            status.setText("Set Min = (300, 200) / Max = (800, 600)");
            return Unit.INSTANCE;
        });

        WButton clearButton = new WButton("Clear the constraints (null)");
        clearButton.addActionListener(() -> {
            WOverlappedPresenter p = presenter[0];
            if (p == null) {
                return Unit.INSTANCE;
            }
            p.setPreferredMinimumWidth(null);
            p.setPreferredMinimumHeight(null);
            p.setPreferredMaximumWidth(null);
            p.setPreferredMaximumHeight(null);
            status.setText("Cleared the constraints");
            return Unit.INSTANCE;
        });

        WPanel buttons = new WPanel(8.0, Orientation.HORIZONTAL);
        buttons.add(openButton);
        buttons.add(clearButton);

        WPanel body = new WPanel(8.0, Orientation.VERTICAL);
        body.add(buttons);
        body.add(status);
        return GalleryScaffold.buildExample("Min/max size (PreferredMinimum/MaximumWidth/Height)", body);
    }

    /**
     * 5) Modal (createForDialog + WAppWindow.create(presenter, owner) + isModal).
     * It creates a raw AppWindow with an owner (unrelated to WFrame), so note that the window has no content.
     */
    private static WComponent buildAppWindowModalExample() {
        WLabel status = new WLabel("Not created yet");

        WButton openButton = new WButton("Open a modal-like child window (empty content)");
        openButton.addActionListener(() -> {
            // Creates a raw AppWindow that requires an owner (the Gallery itself). Even with IsModal=true it is
            // a bare AppWindow with no Content, so a real app would have to layer its own UI on top of it.
            WFrame mainFrame = new WFrame("Gallery");
            WOverlappedPresenter presenter = WOverlappedPresenter.Companion.createForDialog();
            presenter.setModal(true);
            WAppWindow modalWindow = WAppWindow.Companion.create(presenter, mainFrame);
            modalWindow.setTitle("Modal AppWindow (no content)");
            modalWindow.resize(360, 200);
            status.setText("Created an AppWindow with IsModal = " + presenter.isModal() + " "
                + "(it looks like an empty window since Content isn't set)");
            return Unit.INSTANCE;
        });

        WLabel note = new WLabel(
            "IsModal requires an owner window. An AppWindow with an owner can only be created via "
                + "AppWindowStatics.Create(presenter, ownerWindowId), and is a separate \"raw AppWindow\" "
                + "unrelated to a WFrame's Content.");
        note.setForeground(GalleryTheme.TEXT_SECONDARY());
        note.setTextWrapping(TextWrapping.WRAP);

        WPanel body = new WPanel(8.0, Orientation.VERTICAL);
        body.add(note);
        body.add(openButton);
        body.add(status);
        return GalleryScaffold.buildExample(
            "Modal (CreateForDialog / IsModal / WAppWindow.create(presenter, owner))", body);
    }

    /** 6) FullScreen (a "restore" button is required). */
    private static WComponent buildAppWindowFullScreenExample() {
        WFrame[] child = new WFrame[1];
        WLabel status = new WLabel("No child window has been created yet");

        WButton openButton = new WButton("Open a child window");
        openButton.addActionListener(() -> {
            WFrame frame = openChildFrame("FullScreen demo (child window)");
            frame.getAppWindow().resize(480, 320);
            child[0] = frame;
            status.setText("Opened a child window");
            // addCloseButton is skipped since it can be unreachable while full-screen; provide a separate restore button instead
            frame.add(new WLabel("Try \"Go full screen\" followed by \"Restore\"."));
            return Unit.INSTANCE;
        });

        WButton fullScreenButton = new WButton("Go full screen");
        fullScreenButton.addActionListener(() -> {
            WFrame frame = child[0];
            if (frame == null) {
                return Unit.INSTANCE;
            }
            frame.getAppWindow().setPresenter(WFullScreenPresenter.Companion.create());
            status.setText("Switched to FullScreenPresenter");
            return Unit.INSTANCE;
        });

        WButton restoreButton = new WButton("Restore (back to the default Presenter)");
        restoreButton.addActionListener(() -> {
            WFrame frame = child[0];
            if (frame == null) {
                return Unit.INSTANCE;
            }
            frame.getAppWindow().setPresenter(WAppWindowPresenterKind.DEFAULT);
            status.setText("Restored to the default Presenter");
            return Unit.INSTANCE;
        });

        WButton closeButton = new WButton("Close this window");
        closeButton.addActionListener(() -> {
            if (child[0] != null) {
                child[0].setVisible(false);
            }
            return Unit.INSTANCE;
        });

        WPanel buttons = new WPanel(8.0, Orientation.HORIZONTAL);
        buttons.add(openButton);
        buttons.add(fullScreenButton);
        buttons.add(restoreButton);
        buttons.add(closeButton);

        WPanel body = new WPanel(8.0, Orientation.VERTICAL);
        body.add(buttons);
        body.add(status);
        return GalleryScaffold.buildExample(
            "Full screen (FullScreenPresenter / restore via the default Presenter)", body);
    }

    /** 7) CompactOverlay (a size ComboBox). */
    private static WComponent buildAppWindowCompactOverlayExample() {
        WFrame[] child = new WFrame[1];
        WLabel status = new WLabel("No child window has been created yet");

        WButton openButton = new WButton("Open a child window");
        openButton.addActionListener(() -> {
            WFrame frame = openChildFrame("CompactOverlay demo (child window)");
            addCloseButton(frame);
            frame.add(new WLabel("Try a compact display that always floats on top (picture-in-picture-like)."));
            frame.getAppWindow().resize(480, 320);
            child[0] = frame;
            status.setText("Opened a child window");
            return Unit.INSTANCE;
        });

        List<String> sizeNames = new ArrayList<String>();
        for (CompactOverlaySize size : CompactOverlaySize.values()) {
            sizeNames.add(size.name());
        }
        WComboBox sizeCombo = new WComboBox(sizeNames);
        sizeCombo.setSelectedIndex(1); // MEDIUM

        WButton applyButton = new WButton("Switch to CompactOverlay");
        applyButton.addActionListener(() -> {
            WFrame frame = child[0];
            if (frame == null) {
                return Unit.INSTANCE;
            }
            CompactOverlaySize size = CompactOverlaySize.values()[Math.max(sizeCombo.getSelectedIndex(), 0)];
            WCompactOverlayPresenter presenter = WCompactOverlayPresenter.Companion.create();
            presenter.setInitialSize(size);
            frame.getAppWindow().setPresenter(presenter);
            status.setText("Switched to CompactOverlayPresenter (InitialSize = " + size + ")");
            return Unit.INSTANCE;
        });

        WButton restoreButton = new WButton("Restore (back to the default Presenter)");
        restoreButton.addActionListener(() -> {
            WFrame frame = child[0];
            if (frame == null) {
                return Unit.INSTANCE;
            }
            frame.getAppWindow().setPresenter(WAppWindowPresenterKind.DEFAULT);
            status.setText("Restored to the default Presenter");
            return Unit.INSTANCE;
        });

        WPanel buttons = new WPanel(8.0, Orientation.HORIZONTAL);
        buttons.add(openButton);
        buttons.add(sizeCombo);
        buttons.add(applyButton);
        buttons.add(restoreButton);

        WPanel body = new WPanel(8.0, Orientation.VERTICAL);
        body.add(buttons);
        body.add(status);
        return GalleryScaffold.buildExample("CompactOverlay (CompactOverlayPresenter.InitialSize)", body);
    }

    // endregion

    // region AppWindowTitleBar page

    /** The name of one of the 12 selectable color properties, plus its getter/setter. */
    private static final class TitleBarColorSlot {
        final String name;
        final Function<WAppWindowTitleBar, WColor> get;
        final BiConsumer<WAppWindowTitleBar, WColor> set;

        TitleBarColorSlot(String name, Function<WAppWindowTitleBar, WColor> get, BiConsumer<WAppWindowTitleBar, WColor> set) {
            this.name = name;
            this.get = get;
            this.set = set;
        }
    }

    private static final List<TitleBarColorSlot> TITLE_BAR_COLOR_SLOTS = Arrays.asList(
        new TitleBarColorSlot("BackgroundColor", WAppWindowTitleBar::getBackgroundColor, WAppWindowTitleBar::setBackgroundColor),
        new TitleBarColorSlot("ForegroundColor", WAppWindowTitleBar::getForegroundColor, WAppWindowTitleBar::setForegroundColor),
        new TitleBarColorSlot("InactiveBackgroundColor", WAppWindowTitleBar::getInactiveBackgroundColor, WAppWindowTitleBar::setInactiveBackgroundColor),
        new TitleBarColorSlot("InactiveForegroundColor", WAppWindowTitleBar::getInactiveForegroundColor, WAppWindowTitleBar::setInactiveForegroundColor),
        new TitleBarColorSlot("ButtonBackgroundColor", WAppWindowTitleBar::getButtonBackgroundColor, WAppWindowTitleBar::setButtonBackgroundColor),
        new TitleBarColorSlot("ButtonForegroundColor", WAppWindowTitleBar::getButtonForegroundColor, WAppWindowTitleBar::setButtonForegroundColor),
        new TitleBarColorSlot("ButtonHoverBackgroundColor", WAppWindowTitleBar::getButtonHoverBackgroundColor, WAppWindowTitleBar::setButtonHoverBackgroundColor),
        new TitleBarColorSlot("ButtonHoverForegroundColor", WAppWindowTitleBar::getButtonHoverForegroundColor, WAppWindowTitleBar::setButtonHoverForegroundColor),
        new TitleBarColorSlot("ButtonPressedBackgroundColor", WAppWindowTitleBar::getButtonPressedBackgroundColor, WAppWindowTitleBar::setButtonPressedBackgroundColor),
        new TitleBarColorSlot("ButtonPressedForegroundColor", WAppWindowTitleBar::getButtonPressedForegroundColor, WAppWindowTitleBar::setButtonPressedForegroundColor),
        new TitleBarColorSlot("ButtonInactiveBackgroundColor", WAppWindowTitleBar::getButtonInactiveBackgroundColor, WAppWindowTitleBar::setButtonInactiveBackgroundColor),
        new TitleBarColorSlot("ButtonInactiveForegroundColor", WAppWindowTitleBar::getButtonInactiveForegroundColor, WAppWindowTitleBar::setButtonInactiveForegroundColor));

    /** The AppWindowTitleBar page: lines up demos for trying out WAppWindowTitleBar's various features. */
    static WComponent buildAppWindowTitleBarPage() {
        WPanel page = GalleryScaffold.buildPage(
            "AppWindowTitleBar",
            "WAppWindowTitleBar, which sets the system title bar's appearance (color, height, theme). "
                + "Colors like BackgroundColor only take visual effect on the system-drawn title bar "
                + "(ExtendsContentIntoTitleBar = false), and the Inactive-prefixed colors are used while inactive.");

        page.add(buildTitleBarColorExample());
        page.add(buildTitleBarExtendAndHeightExample());
        page.add(buildTitleBarThemeExample());
        return page;
    }

    /** 1) The 12 colors (choose the target with a WComboBox + WColorPicker + ResetToDefault). */
    private static WComponent buildTitleBarColorExample() {
        WFrame[] child = new WFrame[1];
        WLabel status = new WLabel("No child window has been created yet");

        WButton openButton = new WButton("Open a child window");
        openButton.addActionListener(() -> {
            WFrame frame = openChildFrame("TitleBar color demo (child window)");
            addCloseButton(frame);
            frame.add(new WLabel("Pick a target color property from the combo below and change it with the ColorPicker."));
            // BackgroundColor/ForegroundColor and friends only take visual effect on the system-drawn
            // title bar (legacy colors, ExtendsContentIntoTitleBar = false). Setting it to true means the
            // app's content draws the title bar area itself, so the background/text colors stop being
            // used (the real WinUI-Gallery's AppWindowTitleBarWindow.xaml.cs leaves it unset too).
            frame.getAppWindow().resize(480, 320);
            child[0] = frame;
            status.setText("Opened a child window");
            return Unit.INSTANCE;
        });

        List<String> slotNames = new ArrayList<String>();
        for (TitleBarColorSlot slot : TITLE_BAR_COLOR_SLOTS) {
            slotNames.add(slot.name);
        }
        WComboBox targetCombo = new WComboBox(slotNames);
        targetCombo.setSelectedIndex(0);

        WColorPicker picker = new WColorPicker();
        picker.setColor(WColor.Companion.getBLUE());

        WButton applyButton = new WButton("Apply this color");
        applyButton.addActionListener(() -> {
            WFrame frame = child[0];
            if (frame == null) {
                return Unit.INSTANCE;
            }
            TitleBarColorSlot slot = TITLE_BAR_COLOR_SLOTS.get(Math.max(targetCombo.getSelectedIndex(), 0));
            slot.set.accept(frame.getAppWindow().getTitleBar(), picker.getColor());
            status.setText("Applied " + slot.name + " = " + picker.getColor());
            return Unit.INSTANCE;
        });

        WButton resetButton = new WButton("ResetToDefault (restore all colors)");
        resetButton.addActionListener(() -> {
            if (child[0] != null) {
                child[0].getAppWindow().getTitleBar().resetToDefault();
            }
            status.setText("Restored all colors to their defaults");
            return Unit.INSTANCE;
        });

        WPanel buttons = new WPanel(8.0, Orientation.HORIZONTAL);
        buttons.add(openButton);
        buttons.add(targetCombo);
        buttons.add(applyButton);
        buttons.add(resetButton);

        WPanel body = new WPanel(8.0, Orientation.VERTICAL);
        body.add(buttons);
        body.add(picker);
        body.add(status);
        return GalleryScaffold.buildExample("The 12 color properties (IReference<Windows.UI.Color> / ResetToDefault)", body);
    }

    /** 2) ExtendsContentIntoTitleBar + PreferredHeightOption (a child window). */
    private static WComponent buildTitleBarExtendAndHeightExample() {
        WFrame[] child = new WFrame[1];
        WLabel status = new WLabel("No child window has been created yet");

        WButton openButton = new WButton("Open a child window");
        openButton.addActionListener(() -> {
            WFrame frame = openChildFrame("ExtendsContentIntoTitleBar demo (child window)");
            addCloseButton(frame);
            frame.add(new WLabel("Extend the content into the title bar area, and switch its height."));
            frame.getAppWindow().resize(480, 320);
            child[0] = frame;
            status.setText("Opened a child window");
            return Unit.INSTANCE;
        });

        WCheckBox extendCheck = new WCheckBox("ExtendsContentIntoTitleBar");
        extendCheck.addItemListener(checked -> {
            if (child[0] != null) {
                child[0].setExtendsContentIntoTitleBar(Boolean.TRUE.equals(checked));
            }
            return Unit.INSTANCE;
        });

        WPanel heightButtons = new WPanel(8.0, Orientation.HORIZONTAL);
        for (TitleBarHeightOption option : TitleBarHeightOption.values()) {
            WButton button = new WButton(option.name());
            button.addActionListener(() -> {
                WFrame frame = child[0];
                if (frame == null) {
                    return Unit.INSTANCE;
                }
                frame.getAppWindow().getTitleBar().setPreferredHeightOption(option);
                status.setText("PreferredHeightOption = " + option
                    + " (Height = " + frame.getAppWindow().getTitleBar().getHeight() + "px)");
                return Unit.INSTANCE;
            });
            heightButtons.add(button);
        }

        WPanel body = new WPanel(8.0, Orientation.VERTICAL);
        body.add(openButton);
        body.add(extendCheck);
        body.add(heightButtons);
        body.add(status);
        return GalleryScaffold.buildExample("ExtendsContentIntoTitleBar / PreferredHeightOption", body);
    }

    /** 3) PreferredTheme. */
    private static WComponent buildTitleBarThemeExample() {
        WFrame[] child = new WFrame[1];
        WLabel status = new WLabel("No child window has been created yet");

        WButton openButton = new WButton("Open a child window");
        openButton.addActionListener(() -> {
            WFrame frame = openChildFrame("PreferredTheme demo (child window)");
            addCloseButton(frame);
            frame.add(new WLabel("Switch the title bar's color theme."));
            // PreferredTheme also needs ExtendsContentIntoTitleBar = true, otherwise it stays the legacy colors and the change won't be visible
            frame.setExtendsContentIntoTitleBar(true);
            frame.getAppWindow().resize(480, 320);
            child[0] = frame;
            status.setText("Opened a child window");
            return Unit.INSTANCE;
        });

        WPanel themeButtons = new WPanel(8.0, Orientation.HORIZONTAL);
        for (TitleBarTheme theme : TitleBarTheme.values()) {
            WButton button = new WButton(theme.name());
            button.addActionListener(() -> {
                WFrame frame = child[0];
                if (frame == null) {
                    return Unit.INSTANCE;
                }
                frame.getAppWindow().getTitleBar().setPreferredTheme(theme);
                status.setText("PreferredTheme = " + theme);
                return Unit.INSTANCE;
            });
            themeButtons.add(button);
        }

        WPanel body = new WPanel(8.0, Orientation.VERTICAL);
        body.add(openButton);
        body.add(themeButtons);
        body.add(status);
        return GalleryScaffold.buildExample("Color theme (PreferredTheme)", body);
    }

    // endregion

    // region SystemBackdrop page

    /** The SystemBackdrop page: lines up demos for trying out WFrame.systemBackdrop's various materials. */
    static WComponent buildSystemBackdropPage() {
        WPanel page = GalleryScaffold.buildPage(
            "SystemBackdrop",
            "The system materials for a window's background (Mica / Mica Alt / Acrylic). "
                + "Mica is already applied to the Gallery itself; try switching materials in the child window.");

        page.add(buildSystemBackdropSwitchExample());
        return page;
    }

    /** Switching backdrops: applies every SystemBackdropType to a child window. */
    private static WComponent buildSystemBackdropSwitchExample() {
        WLabel status = new WLabel("No child window has been created yet");
        WFrame[] child = new WFrame[1];

        WButton openButton = new WButton("Open a child window");
        openButton.addActionListener(() -> {
            WFrame frame = openChildFrame("SystemBackdrop demo (child window)");
            addCloseButton(frame);
            frame.add(new WLabel("The buttons switch this window's backdrop."));
            frame.getAppWindow().resize(480, 320);
            frame.setSystemBackdrop(SystemBackdropType.MICA);
            child[0] = frame;
            status.setText("SystemBackdrop = " + frame.getSystemBackdrop());
            return Unit.INSTANCE;
        });

        WPanel buttons = new WPanel(8.0, Orientation.HORIZONTAL);
        buttons.add(openButton);
        for (SystemBackdropType type : SystemBackdropType.values()) {
            WButton button = new WButton(type.name());
            button.addActionListener(() -> {
                WFrame frame = child[0];
                if (frame == null) {
                    return Unit.INSTANCE;
                }
                frame.setSystemBackdrop(type);
                status.setText("SystemBackdrop = " + frame.getSystemBackdrop());
                return Unit.INSTANCE;
            });
            buttons.add(button);
        }

        WPanel body = new WPanel(8.0, Orientation.VERTICAL);
        body.add(buttons);
        body.add(status);
        return GalleryScaffold.buildExample("Switching backdrops (MICA / MICA_ALT / ACRYLIC / NONE)", body);
    }

    // endregion

    // region Multiple windows page

    /** The Multiple windows page: lines up a demo for creating and managing multiple child windows. */
    static WComponent buildMultipleWindowsPage() {
        WPanel page = GalleryScaffold.buildPage(
            "Multiple windows",
            "A demo of opening and managing several native windows (WFrame) at the same time.");

        page.add(buildMultipleWindowsExample());
        return page;
    }

    private static WComponent buildMultipleWindowsExample() {
        List<WFrame> children = new ArrayList<WFrame>();
        WLabel count = new WLabel("Open windows: 0");

        Runnable updateCount = () -> {
            children.removeIf(frame -> !frame.isVisible());
            count.setText("Open windows: " + children.size());
        };

        WButton openButton = new WButton("Open another child window");
        openButton.addActionListener(() -> {
            WFrame frame = new WFrame("Child window #" + (children.size() + 1));
            // Matching the real Gallery's Multiple windows demo, open it with an extended title bar
            frame.setExtendsContentIntoTitleBar(true);
            WButton closeButton = new WButton("Close this window");
            closeButton.addActionListener(() -> {
                frame.setVisible(false);
                updateCount.run();
                return Unit.INSTANCE;
            });
            frame.add(new WLabel("This window is a WFrame independent of the Gallery itself."));
            frame.add(closeButton);
            frame.setVisible(true);
            frame.getAppWindow().resizeClient(600, 400);
            children.add(frame);
            updateCount.run();
            return Unit.INSTANCE;
        });

        WButton closeAllButton = new WButton("Close all");
        closeAllButton.addActionListener(() -> {
            for (WFrame frame : children) {
                frame.setVisible(false);
            }
            updateCount.run();
            return Unit.INSTANCE;
        });

        WPanel buttons = new WPanel(8.0, Orientation.HORIZONTAL);
        buttons.add(openButton);
        buttons.add(closeAllButton);

        WPanel body = new WPanel(8.0, Orientation.VERTICAL);
        body.add(buttons);
        body.add(count);
        return GalleryScaffold.buildExample(
            "Creating and managing multiple windows (extendsContentIntoTitleBar / resizeClient)", body);
    }

    // endregion

    // region TitleBar page

    /** The TitleBar page: lines up demos for trying out WTitleBar's various features. */
    static WComponent buildTitleBarPage() {
        WPanel page = GalleryScaffold.buildPage(
            "TitleBar",
            "WTitleBar, a custom title bar that can have a back button, a pane-toggle button, and "
                + "arbitrary content. It's also used for the Gallery's own title bar.");

        page.add(buildTitleBarInlineExample());
        page.add(buildTitleBarDragRegionExample());
        page.add(buildTitleBarEndToEndExample());
        return page;
    }

    /** 1) Operating properties inline (Title/Subtitle/back/pane-toggle/content=WAutoSuggestBox). */
    private static WComponent buildTitleBarInlineExample() {
        WTitleBar titleBar = new WTitleBar();
        titleBar.setTitle("Sample title");
        titleBar.setSubtitle("Subtitle");
        titleBar.setBackButtonVisible(true);
        titleBar.setPaneToggleButtonVisible(true);

        WLabel log = new WLabel("Event: none");
        log.setTextWrapping(TextWrapping.WRAP);
        titleBar.addBackRequestedListener(() -> {
            log.setText("Event: BackRequested");
            return Unit.INSTANCE;
        });
        titleBar.addPaneToggleRequestedListener(() -> {
            log.setText("Event: PaneToggleRequested");
            return Unit.INSTANCE;
        });

        WTextField titleField = new WTextField("Title");
        titleField.setWidth(200.0);
        titleField.setText(titleBar.getTitle());
        WTextField subtitleField = new WTextField("Subtitle");
        subtitleField.setWidth(200.0);
        subtitleField.setText(titleBar.getSubtitle());
        WButton applyButton = new WButton("Apply Title/Subtitle");
        applyButton.addActionListener(() -> {
            titleBar.setTitle(titleField.getText());
            titleBar.setSubtitle(subtitleField.getText());
            return Unit.INSTANCE;
        });

        WCheckBox backVisibleCheck = new WCheckBox("IsBackButtonVisible");
        backVisibleCheck.setChecked(true);
        backVisibleCheck.addItemListener(checked -> {
            titleBar.setBackButtonVisible(Boolean.TRUE.equals(checked));
            return Unit.INSTANCE;
        });
        WCheckBox backEnabledCheck = new WCheckBox("IsBackButtonEnabled");
        backEnabledCheck.setChecked(true);
        backEnabledCheck.addItemListener(checked -> {
            titleBar.setBackButtonEnabled(Boolean.TRUE.equals(checked));
            return Unit.INSTANCE;
        });
        WCheckBox paneToggleCheck = new WCheckBox("IsPaneToggleButtonVisible");
        paneToggleCheck.setChecked(true);
        paneToggleCheck.addItemListener(checked -> {
            titleBar.setPaneToggleButtonVisible(Boolean.TRUE.equals(checked));
            return Unit.INSTANCE;
        });

        WCheckBox useSearchBoxCheck = new WCheckBox("Show a search box as Content");
        useSearchBoxCheck.addItemListener(checked -> {
            titleBar.setContent(Boolean.TRUE.equals(checked) ? new WAutoSuggestBox("Search") : null);
            return Unit.INSTANCE;
        });

        WPanel fields = new WPanel(8.0, Orientation.HORIZONTAL);
        fields.add(titleField);
        fields.add(subtitleField);
        fields.add(applyButton);

        WPanel checks = new WPanel(8.0, Orientation.HORIZONTAL);
        checks.add(backVisibleCheck);
        checks.add(backEnabledCheck);
        checks.add(paneToggleCheck);
        checks.add(useSearchBoxCheck);

        WPanel body = new WPanel(8.0, Orientation.VERTICAL);
        body.add(titleBar);
        body.add(fields);
        body.add(checks);
        body.add(log);
        return GalleryScaffold.buildExample(
            "Inline display (Title / Subtitle / IsBackButtonVisible / IsBackButtonEnabled / "
                + "IsPaneToggleButtonVisible / Content / BackRequested / PaneToggleRequested)",
            body);
    }

    /** 2) Drag region (setIsDragRegion). */
    private static WComponent buildTitleBarDragRegionExample() {
        WTitleBar titleBar = new WTitleBar();
        titleBar.setTitle("Drag region demo");

        WButton dragButton = new WButton("This spot is a drag region (SetIsDragRegion = true)");
        WTitleBar.Companion.setIsDragRegion(dragButton, true);

        WButton autoButton = new WButton("Restore default auto-detection (null)");
        autoButton.addActionListener(() -> {
            WTitleBar.Companion.setIsDragRegion(dragButton, null);
            return Unit.INSTANCE;
        });

        WButton nonDragButton = new WButton("SetIsDragRegion = false");
        nonDragButton.addActionListener(() -> {
            WTitleBar.Companion.setIsDragRegion(dragButton, false);
            return Unit.INSTANCE;
        });

        WLabel note = new WLabel(
            "The TitleBar.IsDragRegion attached property lets you make any component a draggable "
                + "region (by default, AutoRefreshDragRegions automatically makes things like buttons non-draggable).");
        note.setForeground(GalleryTheme.TEXT_SECONDARY());
        note.setTextWrapping(TextWrapping.WRAP);

        WPanel buttons = new WPanel(8.0, Orientation.HORIZONTAL);
        buttons.add(dragButton);
        buttons.add(autoButton);
        buttons.add(nonDragButton);

        WPanel body = new WPanel(8.0, Orientation.VERTICAL);
        body.add(note);
        body.add(titleBar);
        body.add(buttons);
        return GalleryScaffold.buildExample("Drag region (TitleBar.SetIsDragRegion)", body);
    }

    /** 3) An end-to-end child window (a full WTitleBar + WNavigationView setup). */
    private static WComponent buildTitleBarEndToEndExample() {
        WLabel status = new WLabel("No child window has been created yet");

        WButton openButton = new WButton("Open a WTitleBar + NavigationView child window");
        openButton.addActionListener(() -> {
            WFrame frame = new WFrame("TitleBar full setup demo");

            WTitleBar titleBar = new WTitleBar();
            titleBar.setTitle("TitleBar full setup demo");
            titleBar.setPaneToggleButtonVisible(true);

            WNavigationView navigationView = new WNavigationView();
            navigationView.setPaneToggleButtonVisible(false); // avoid showing it twice - only the TitleBar side is shown
            navigationView.setBackButtonVisible(NavigationViewBackButtonVisible.COLLAPSED);
            WNavigationViewItem item1 = new WNavigationViewItem("Page 1", null);
            WNavigationViewItem item2 = new WNavigationViewItem("Page 2", null);
            navigationView.addItem(item1);
            navigationView.addItem(item2);
            WLabel content = new WLabel("Page 1");
            content.setHorizontalAlignment(HorizontalAlignment.LEFT);
            navigationView.setContent(content);
            navigationView.addSelectionListener(item -> {
                content.setText(item != null ? item.getText() : "");
                return Unit.INSTANCE;
            });
            navigationView.setSelectedItem(item1);

            titleBar.addPaneToggleRequestedListener(() -> {
                navigationView.setPaneOpen(!navigationView.isPaneOpen());
                return Unit.INSTANCE;
            });

            WGrid root = new WGrid(0.0, 0.0);
            root.addRow(GridLength.Companion.getAUTO());
            root.addRow(GridLength.Companion.star(1.0));
            root.add(titleBar, 0, 0, 1, 1);
            root.add(navigationView, 1, 0, 1, 1);

            frame.setContentPane(root);
            frame.setExtendsContentIntoTitleBar(true);
            frame.setTitleBar(titleBar);
            frame.getAppWindow().getTitleBar().setPreferredHeightOption(TitleBarHeightOption.TALL);
            frame.getAppWindow().resize(560, 400);
            frame.setVisible(true);
            status.setText("Opened a child window");
            return Unit.INSTANCE;
        });

        WPanel body = new WPanel(8.0, Orientation.VERTICAL);
        body.add(openButton);
        body.add(status);
        return GalleryScaffold.buildExample("End-to-end (WTitleBar + WNavigationView + Grid + setTitleBar)", body);
    }

    // endregion
}
