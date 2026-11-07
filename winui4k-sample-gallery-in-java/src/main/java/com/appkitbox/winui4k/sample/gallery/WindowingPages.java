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
import com.appkitbox.winui4k.WItemEvent;
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
            Messages.get("appwindow.description"));

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
        frame.setContentPane(stackedContent());
        frame.setVisible(true);
        return frame;
    }

    /** The child window's content: a panel that stacks the added components vertically from the top (the default content is a BorderLayout as in JFrame, so it is replaced). */
    private static WPanel stackedContent() {
        WPanel panel = new WPanel(12.0);
        panel.setMargin(24.0);
        return panel;
    }

    /** The "close" button always placed at the top of a child window's content. */
    private static void addCloseButton(WFrame frame) {
        WButton closeButton = new WButton(Messages.get("appwindow.child.close"));
        closeButton.addActionListener(() -> {
            frame.dispose();
        });
        frame.add(closeButton);
    }

    /** 1) Title / Resize / Move / SetIcon. */
    private static WComponent buildAppWindowBasicExample() {
        WLabel status = new WLabel(Messages.get("appwindow.child.none"));
        WFrame[] child = new WFrame[1];

        WButton openButton = new WButton(Messages.get("appwindow.child.open"));
        openButton.addActionListener(() -> {
            WFrame frame = openChildFrame(Messages.get("appwindow.basic.child.title"));
            addCloseButton(frame);
            frame.add(new WLabel(Messages.get("appwindow.basic.child.label")));
            frame.getAppWindow().resize(480, 320);
            child[0] = frame;
            status.setText(Messages.get("appwindow.basic.opened", frame.getAppWindow().getTitle()));
        });

        WButton titleButton = new WButton(Messages.get("appwindow.basic.changetitle"));
        titleButton.addActionListener(() -> {
            WFrame frame = child[0];
            if (frame == null) {
                return;
            }
            frame.getAppWindow().setTitle(Messages.get("appwindow.basic.newtitle", String.valueOf(System.currentTimeMillis() % 1000)));
            status.setText("Title = \"" + frame.getAppWindow().getTitle() + "\"");
        });

        WButton resizeButton = new WButton("Resize(640, 480)");
        resizeButton.addActionListener(() -> {
            WFrame frame = child[0];
            if (frame == null) {
                return;
            }
            frame.getAppWindow().resize(640, 480);
            status.setText("Size = " + frame.getAppWindow().getSize()
                + " / ClientSize = " + frame.getAppWindow().getClientSize());
        });

        WButton moveButton = new WButton("Move(100, 100)");
        moveButton.addActionListener(() -> {
            WFrame frame = child[0];
            if (frame == null) {
                return;
            }
            frame.getAppWindow().move(100, 100);
            status.setText("Position = " + frame.getAppWindow().getPosition());
        });

        WPanel buttons = new WPanel(8.0, Orientation.HORIZONTAL);
        buttons.add(openButton);
        buttons.add(titleButton);
        buttons.add(resizeButton);
        buttons.add(moveButton);

        WPanel body = new WPanel(8.0);
        body.add(buttons);
        body.add(status);
        return GalleryScaffold.buildExample(
            Messages.get("appwindow.basic.title"), body);
    }

    /** 2) Screen centering: computes the center point from WDisplayArea.nearest(appWindow).workArea. */
    private static WComponent buildAppWindowCenterExample() {
        WLabel status = new WLabel(Messages.get("appwindow.child.none"));
        WFrame[] child = new WFrame[1];

        WButton openButton = new WButton(Messages.get("appwindow.center.open"));
        openButton.addActionListener(() -> {
            WFrame frame = openChildFrame(Messages.get("appwindow.center.child.title"));
            addCloseButton(frame);
            frame.add(new WLabel(Messages.get("appwindow.center.child.label")));
            frame.getAppWindow().resize(360, 240);
            frame.getAppWindow().move(50, 50); // first place it near the edge of the screen
            child[0] = frame;
            status.setText(Messages.get("appwindow.child.opened"));
        });

        WButton centerButton = new WButton(Messages.get("appwindow.center.button"));
        centerButton.addActionListener(() -> {
            WFrame frame = child[0];
            if (frame == null) {
                return;
            }
            WRectangle workArea = WDisplayArea.nearest(frame.getAppWindow()).getWorkArea();
            WDimension size = frame.getAppWindow().getSize();
            int x = workArea.getX() + (workArea.getWidth() - size.getWidth()) / 2;
            int y = workArea.getY() + (workArea.getHeight() - size.getHeight()) / 2;
            frame.getAppWindow().move(x, y);
            status.setText("WorkArea = " + workArea + " → Position = " + frame.getAppWindow().getPosition());
        });

        WPanel buttons = new WPanel(8.0, Orientation.HORIZONTAL);
        buttons.add(openButton);
        buttons.add(centerButton);

        WPanel body = new WPanel(8.0);
        body.add(buttons);
        body.add(status);
        return GalleryScaffold.buildExample(Messages.get("appwindow.center.title"), body);
    }

    /** 3) OverlappedPresenter's 6 toggles + Maximize/Minimize/Restore + the State display. */
    private static WComponent buildAppWindowPresenterExample() {
        WLabel status = new WLabel(Messages.get("appwindow.child.none"));
        WFrame[] child = new WFrame[1];
        WOverlappedPresenter[] presenter = new WOverlappedPresenter[1];

        WButton openButton = new WButton(Messages.get("appwindow.child.open"));
        openButton.addActionListener(() -> {
            WFrame frame = openChildFrame(Messages.get("appwindow.presenter.child.title"));
            addCloseButton(frame);
            frame.add(new WLabel(Messages.get("appwindow.presenter.child.label")));
            frame.getAppWindow().resize(480, 360);
            WOverlappedPresenter newPresenter = WOverlappedPresenter.create();
            frame.getAppWindow().setPresenter(newPresenter);
            child[0] = frame;
            presenter[0] = newPresenter;
            status.setText("state = " + newPresenter.getState());
        });

        Runnable refreshState = () ->
            status.setText("state = " + (presenter[0] == null ? null : presenter[0].getState()));

        WCheckBox alwaysOnTopCheck = new WCheckBox("IsAlwaysOnTop");
        alwaysOnTopCheck.addItemListener(e -> {
            if (presenter[0] != null) {
                presenter[0].setAlwaysOnTop(e.getStateChange() == WItemEvent.SELECTED);
            }
        });
        WCheckBox maximizableCheck = new WCheckBox("IsMaximizable");
        maximizableCheck.setSelected(true);
        maximizableCheck.addItemListener(e -> {
            if (presenter[0] != null) {
                presenter[0].setMaximizable(e.getStateChange() == WItemEvent.SELECTED);
            }
        });
        WCheckBox minimizableCheck = new WCheckBox("IsMinimizable");
        minimizableCheck.setSelected(true);
        minimizableCheck.addItemListener(e -> {
            if (presenter[0] != null) {
                presenter[0].setMinimizable(e.getStateChange() == WItemEvent.SELECTED);
            }
        });
        WCheckBox resizableCheck = new WCheckBox("IsResizable");
        resizableCheck.setSelected(true);
        resizableCheck.addItemListener(e -> {
            if (presenter[0] != null) {
                presenter[0].setResizable(e.getStateChange() == WItemEvent.SELECTED);
            }
        });
        WCheckBox borderCheck = new WCheckBox("HasBorder");
        borderCheck.setSelected(true);
        WCheckBox titleBarCheck = new WCheckBox("HasTitleBar");
        titleBarCheck.setSelected(true);
        WButton applyBorderButton = new WButton(Messages.get("appwindow.presenter.applyborder"));
        applyBorderButton.addActionListener(() -> {
            if (presenter[0] != null) {
                presenter[0].setBorderAndTitleBar(
                    borderCheck.isSelected(),
                    titleBarCheck.isSelected());
            }
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
        });
        WButton minimizeButton = new WButton("Minimize");
        minimizeButton.addActionListener(() -> {
            if (presenter[0] != null) {
                presenter[0].minimize();
            }
            refreshState.run();
        });
        WButton restoreButton = new WButton("Restore");
        restoreButton.addActionListener(() -> {
            if (presenter[0] != null) {
                presenter[0].restore();
            }
            refreshState.run();
        });
        WPanel stateButtons = new WPanel(8.0, Orientation.HORIZONTAL);
        stateButtons.add(maximizeButton);
        stateButtons.add(minimizeButton);
        stateButtons.add(restoreButton);

        WPanel body = new WPanel(8.0);
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
        WLabel status = new WLabel(Messages.get("appwindow.child.none"));
        WOverlappedPresenter[] presenter = new WOverlappedPresenter[1];

        WButton openButton = new WButton(Messages.get("appwindow.minmax.open"));
        openButton.addActionListener(() -> {
            WFrame frame = openChildFrame(Messages.get("appwindow.minmax.child.title"));
            addCloseButton(frame);
            frame.add(new WLabel(Messages.get("appwindow.minmax.child.label")));
            frame.getAppWindow().resize(400, 300);
            WOverlappedPresenter newPresenter = WOverlappedPresenter.create();
            newPresenter.setPreferredMinimumWidth(300);
            newPresenter.setPreferredMinimumHeight(200);
            newPresenter.setPreferredMaximumWidth(800);
            newPresenter.setPreferredMaximumHeight(600);
            frame.getAppWindow().setPresenter(newPresenter);
            presenter[0] = newPresenter;
            status.setText(Messages.get("appwindow.minmax.set"));
        });

        WButton clearButton = new WButton(Messages.get("appwindow.minmax.clear"));
        clearButton.addActionListener(() -> {
            WOverlappedPresenter p = presenter[0];
            if (p == null) {
            }
            p.setPreferredMinimumWidth(null);
            p.setPreferredMinimumHeight(null);
            p.setPreferredMaximumWidth(null);
            p.setPreferredMaximumHeight(null);
            status.setText(Messages.get("appwindow.minmax.cleared"));
        });

        WPanel buttons = new WPanel(8.0, Orientation.HORIZONTAL);
        buttons.add(openButton);
        buttons.add(clearButton);

        WPanel body = new WPanel(8.0);
        body.add(buttons);
        body.add(status);
        return GalleryScaffold.buildExample(Messages.get("appwindow.minmax.title"), body);
    }

    /**
     * 5) Modal (createForDialog + WAppWindow.create(presenter, owner) + isModal).
     * It creates a raw AppWindow with an owner (unrelated to WFrame), so note that the window has no content.
     */
    private static WComponent buildAppWindowModalExample() {
        WLabel status = new WLabel(Messages.get("appwindow.modal.none"));

        WButton openButton = new WButton(Messages.get("appwindow.modal.open"));
        openButton.addActionListener(() -> {
            // Creates a raw AppWindow that requires an owner (the Gallery itself). Even with IsModal=true it is
            // a bare AppWindow with no Content, so a real app would have to layer its own UI on top of it.
            WFrame mainFrame = new WFrame("Gallery");
            WOverlappedPresenter presenter = WOverlappedPresenter.createForDialog();
            presenter.setModal(true);
            WAppWindow modalWindow = WAppWindow.create(presenter, mainFrame);
            modalWindow.setTitle(Messages.get("appwindow.modal.window.title"));
            modalWindow.resize(360, 200);
            status.setText(Messages.get("appwindow.modal.created", String.valueOf(presenter.isModal())));
        });

        WLabel note = new WLabel(
            Messages.get("appwindow.modal.note"));
        note.setForeground(GalleryTheme.TEXT_SECONDARY());
        note.setTextWrapping(TextWrapping.WRAP);

        WPanel body = new WPanel(8.0);
        body.add(note);
        body.add(openButton);
        body.add(status);
        return GalleryScaffold.buildExample(
            Messages.get("appwindow.modal.title"), body);
    }

    /** 6) FullScreen (a "restore" button is required). */
    private static WComponent buildAppWindowFullScreenExample() {
        WFrame[] child = new WFrame[1];
        WLabel status = new WLabel(Messages.get("appwindow.child.none"));

        WButton openButton = new WButton(Messages.get("appwindow.child.open"));
        openButton.addActionListener(() -> {
            WFrame frame = openChildFrame(Messages.get("appwindow.fullscreen.child.title"));
            frame.getAppWindow().resize(480, 320);
            child[0] = frame;
            status.setText(Messages.get("appwindow.child.opened"));
            // addCloseButton is skipped since it can be unreachable while full-screen; provide a separate restore button instead
            frame.add(new WLabel(Messages.get("appwindow.fullscreen.child.label")));
        });

        WButton fullScreenButton = new WButton(Messages.get("appwindow.fullscreen.enter"));
        fullScreenButton.addActionListener(() -> {
            WFrame frame = child[0];
            if (frame == null) {
                return;
            }
            frame.getAppWindow().setPresenter(WFullScreenPresenter.create());
            status.setText(Messages.get("appwindow.fullscreen.entered"));
        });

        WButton restoreButton = new WButton(Messages.get("appwindow.restore"));
        restoreButton.addActionListener(() -> {
            WFrame frame = child[0];
            if (frame == null) {
                return;
            }
            frame.getAppWindow().setPresenter(WAppWindowPresenterKind.DEFAULT);
            status.setText(Messages.get("appwindow.restored"));
        });

        WButton closeButton = new WButton(Messages.get("appwindow.child.close"));
        closeButton.addActionListener(() -> {
            if (child[0] != null) {
                child[0].dispose();
            }
        });

        WPanel buttons = new WPanel(8.0, Orientation.HORIZONTAL);
        buttons.add(openButton);
        buttons.add(fullScreenButton);
        buttons.add(restoreButton);
        buttons.add(closeButton);

        WPanel body = new WPanel(8.0);
        body.add(buttons);
        body.add(status);
        return GalleryScaffold.buildExample(
            Messages.get("appwindow.fullscreen.title"), body);
    }

    /** 7) CompactOverlay (a size ComboBox). */
    private static WComponent buildAppWindowCompactOverlayExample() {
        WFrame[] child = new WFrame[1];
        WLabel status = new WLabel(Messages.get("appwindow.child.none"));

        WButton openButton = new WButton(Messages.get("appwindow.child.open"));
        openButton.addActionListener(() -> {
            WFrame frame = openChildFrame(Messages.get("appwindow.compactoverlay.child.title"));
            addCloseButton(frame);
            frame.add(new WLabel(Messages.get("appwindow.compactoverlay.child.label")));
            frame.getAppWindow().resize(480, 320);
            child[0] = frame;
            status.setText(Messages.get("appwindow.child.opened"));
        });

        List<String> sizeNames = new ArrayList<String>();
        for (CompactOverlaySize size : CompactOverlaySize.values()) {
            sizeNames.add(size.name());
        }
        WComboBox sizeCombo = new WComboBox(sizeNames);
        sizeCombo.setSelectedIndex(1); // MEDIUM

        WButton applyButton = new WButton(Messages.get("appwindow.compactoverlay.apply"));
        applyButton.addActionListener(() -> {
            WFrame frame = child[0];
            if (frame == null) {
                return;
            }
            CompactOverlaySize size = CompactOverlaySize.values()[Math.max(sizeCombo.getSelectedIndex(), 0)];
            WCompactOverlayPresenter presenter = WCompactOverlayPresenter.create();
            presenter.setInitialSize(size);
            frame.getAppWindow().setPresenter(presenter);
            status.setText(Messages.get("appwindow.compactoverlay.applied", size.toString()));
        });

        WButton restoreButton = new WButton(Messages.get("appwindow.restore"));
        restoreButton.addActionListener(() -> {
            WFrame frame = child[0];
            if (frame == null) {
                return;
            }
            frame.getAppWindow().setPresenter(WAppWindowPresenterKind.DEFAULT);
            status.setText(Messages.get("appwindow.restored"));
        });

        WPanel buttons = new WPanel(8.0, Orientation.HORIZONTAL);
        buttons.add(openButton);
        buttons.add(sizeCombo);
        buttons.add(applyButton);
        buttons.add(restoreButton);

        WPanel body = new WPanel(8.0);
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
            Messages.get("appwindowtitlebar.description"));

        page.add(buildTitleBarColorExample());
        page.add(buildTitleBarExtendAndHeightExample());
        page.add(buildTitleBarThemeExample());
        return page;
    }

    /** 1) The 12 colors (choose the target with a WComboBox + WColorPicker + ResetToDefault). */
    private static WComponent buildTitleBarColorExample() {
        WFrame[] child = new WFrame[1];
        WLabel status = new WLabel(Messages.get("appwindowtitlebar.child.none"));

        WButton openButton = new WButton(Messages.get("appwindowtitlebar.child.open"));
        openButton.addActionListener(() -> {
            WFrame frame = openChildFrame(Messages.get("appwindowtitlebar.color.child.title"));
            addCloseButton(frame);
            frame.add(new WLabel(Messages.get("appwindowtitlebar.color.child.label")));
            // BackgroundColor/ForegroundColor and friends only take visual effect on the system-drawn
            // title bar (legacy colors, ExtendsContentIntoTitleBar = false). Setting it to true means the
            // app's content draws the title bar area itself, so the background/text colors stop being
            // used (the real WinUI-Gallery's AppWindowTitleBarWindow.xaml.cs leaves it unset too).
            frame.getAppWindow().resize(480, 320);
            child[0] = frame;
            status.setText(Messages.get("appwindowtitlebar.child.opened"));
        });

        List<String> slotNames = new ArrayList<String>();
        for (TitleBarColorSlot slot : TITLE_BAR_COLOR_SLOTS) {
            slotNames.add(slot.name);
        }
        WComboBox targetCombo = new WComboBox(slotNames);
        targetCombo.setSelectedIndex(0);

        WColorPicker picker = new WColorPicker();
        picker.setColor(GalleryTheme.DEMO_BLUE);

        WButton applyButton = new WButton(Messages.get("appwindowtitlebar.color.apply"));
        applyButton.addActionListener(() -> {
            WFrame frame = child[0];
            if (frame == null) {
                return;
            }
            TitleBarColorSlot slot = TITLE_BAR_COLOR_SLOTS.get(Math.max(targetCombo.getSelectedIndex(), 0));
            slot.set.accept(frame.getAppWindow().getTitleBar(), picker.getColor());
            status.setText(Messages.get("appwindowtitlebar.color.applied", slot.name, String.valueOf(picker.getColor())));
        });

        WButton resetButton = new WButton(Messages.get("appwindowtitlebar.color.reset"));
        resetButton.addActionListener(() -> {
            if (child[0] != null) {
                child[0].getAppWindow().getTitleBar().resetToDefault();
            }
            status.setText(Messages.get("appwindowtitlebar.color.resetdone"));
        });

        WPanel buttons = new WPanel(8.0, Orientation.HORIZONTAL);
        buttons.add(openButton);
        buttons.add(targetCombo);
        buttons.add(applyButton);
        buttons.add(resetButton);

        WPanel body = new WPanel(8.0);
        body.add(buttons);
        body.add(picker);
        body.add(status);
        return GalleryScaffold.buildExample(Messages.get("appwindowtitlebar.color.title"), body);
    }

    /** 2) ExtendsContentIntoTitleBar + PreferredHeightOption (a child window). */
    private static WComponent buildTitleBarExtendAndHeightExample() {
        WFrame[] child = new WFrame[1];
        WLabel status = new WLabel(Messages.get("appwindowtitlebar.child.none"));

        WButton openButton = new WButton(Messages.get("appwindowtitlebar.child.open"));
        openButton.addActionListener(() -> {
            WFrame frame = openChildFrame(Messages.get("appwindowtitlebar.extend.child.title"));
            addCloseButton(frame);
            frame.add(new WLabel(Messages.get("appwindowtitlebar.extend.child.label")));
            frame.getAppWindow().resize(480, 320);
            child[0] = frame;
            status.setText(Messages.get("appwindowtitlebar.child.opened"));
        });

        WCheckBox extendCheck = new WCheckBox("ExtendsContentIntoTitleBar");
        extendCheck.addItemListener(e -> {
            if (child[0] != null) {
                child[0].setExtendsContentIntoTitleBar(e.getStateChange() == WItemEvent.SELECTED);
            }
        });

        WPanel heightButtons = new WPanel(8.0, Orientation.HORIZONTAL);
        for (TitleBarHeightOption option : TitleBarHeightOption.values()) {
            WButton button = new WButton(option.name());
            button.addActionListener(() -> {
                WFrame frame = child[0];
                if (frame == null) {
                    return;
                }
                frame.getAppWindow().getTitleBar().setPreferredHeightOption(option);
                status.setText("PreferredHeightOption = " + option
                    + " (Height = " + frame.getAppWindow().getTitleBar().getHeight() + "px)");
            });
            heightButtons.add(button);
        }

        WPanel body = new WPanel(8.0);
        body.add(openButton);
        body.add(extendCheck);
        body.add(heightButtons);
        body.add(status);
        return GalleryScaffold.buildExample("ExtendsContentIntoTitleBar / PreferredHeightOption", body);
    }

    /** 3) PreferredTheme. */
    private static WComponent buildTitleBarThemeExample() {
        WFrame[] child = new WFrame[1];
        WLabel status = new WLabel(Messages.get("appwindowtitlebar.child.none"));

        WButton openButton = new WButton(Messages.get("appwindowtitlebar.child.open"));
        openButton.addActionListener(() -> {
            WFrame frame = openChildFrame(Messages.get("appwindowtitlebar.theme.child.title"));
            addCloseButton(frame);
            frame.add(new WLabel(Messages.get("appwindowtitlebar.theme.child.label")));
            // PreferredTheme also needs ExtendsContentIntoTitleBar = true, otherwise it stays the legacy colors and the change won't be visible
            frame.setExtendsContentIntoTitleBar(true);
            frame.getAppWindow().resize(480, 320);
            child[0] = frame;
            status.setText(Messages.get("appwindowtitlebar.child.opened"));
        });

        WPanel themeButtons = new WPanel(8.0, Orientation.HORIZONTAL);
        for (TitleBarTheme theme : TitleBarTheme.values()) {
            WButton button = new WButton(theme.name());
            button.addActionListener(() -> {
                WFrame frame = child[0];
                if (frame == null) {
                    return;
                }
                frame.getAppWindow().getTitleBar().setPreferredTheme(theme);
                status.setText("PreferredTheme = " + theme);
            });
            themeButtons.add(button);
        }

        WPanel body = new WPanel(8.0);
        body.add(openButton);
        body.add(themeButtons);
        body.add(status);
        return GalleryScaffold.buildExample(Messages.get("appwindowtitlebar.theme.title"), body);
    }

    // endregion

    // region SystemBackdrop page

    /** The SystemBackdrop page: lines up demos for trying out WFrame.systemBackdrop's various materials. */
    static WComponent buildSystemBackdropPage() {
        WPanel page = GalleryScaffold.buildPage(
            "SystemBackdrop",
            Messages.get("systembackdrop.description"));

        page.add(buildSystemBackdropSwitchExample());
        return page;
    }

    /** Switching backdrops: applies every SystemBackdropType to a child window. */
    private static WComponent buildSystemBackdropSwitchExample() {
        WLabel status = new WLabel(Messages.get("systembackdrop.child.none"));
        WFrame[] child = new WFrame[1];

        WButton openButton = new WButton(Messages.get("systembackdrop.child.open"));
        openButton.addActionListener(() -> {
            WFrame frame = openChildFrame(Messages.get("systembackdrop.child.title"));
            addCloseButton(frame);
            frame.add(new WLabel(Messages.get("systembackdrop.child.label")));
            frame.getAppWindow().resize(480, 320);
            frame.setSystemBackdrop(SystemBackdropType.MICA);
            child[0] = frame;
            status.setText("SystemBackdrop = " + frame.getSystemBackdrop());
        });

        WPanel buttons = new WPanel(8.0, Orientation.HORIZONTAL);
        buttons.add(openButton);
        for (SystemBackdropType type : SystemBackdropType.values()) {
            WButton button = new WButton(type.name());
            button.addActionListener(() -> {
                WFrame frame = child[0];
                if (frame == null) {
                    return;
                }
                frame.setSystemBackdrop(type);
                status.setText("SystemBackdrop = " + frame.getSystemBackdrop());
            });
            buttons.add(button);
        }

        WPanel body = new WPanel(8.0);
        body.add(buttons);
        body.add(status);
        return GalleryScaffold.buildExample(Messages.get("systembackdrop.switch.title"), body);
    }

    // endregion

    // region Multiple windows page

    /** The Multiple windows page: lines up a demo for creating and managing multiple child windows. */
    static WComponent buildMultipleWindowsPage() {
        WPanel page = GalleryScaffold.buildPage(
            "Multiple windows",
            Messages.get("multiplewindows.description"));

        page.add(buildMultipleWindowsExample());
        return page;
    }

    private static WComponent buildMultipleWindowsExample() {
        List<WFrame> children = new ArrayList<WFrame>();
        WLabel count = new WLabel(Messages.get("multiplewindows.count", "0"));

        Runnable updateCount = () -> {
            children.removeIf(frame -> !frame.isDisplayable());
            count.setText(Messages.get("multiplewindows.count", String.valueOf(children.size())));
        };

        WButton openButton = new WButton(Messages.get("multiplewindows.open"));
        openButton.addActionListener(() -> {
            WFrame frame = new WFrame(Messages.get("multiplewindows.child.title", String.valueOf(children.size() + 1)));
            frame.setContentPane(stackedContent());
            // Matching the real Gallery's Multiple windows demo, open it with an extended title bar
            frame.setExtendsContentIntoTitleBar(true);
            WButton closeButton = new WButton(Messages.get("multiplewindows.child.close"));
            closeButton.addActionListener(() -> {
                frame.dispose();
                updateCount.run();
            });
            frame.add(new WLabel(Messages.get("multiplewindows.child.label")));
            frame.add(closeButton);
            frame.setVisible(true);
            frame.getAppWindow().resizeClient(600, 400);
            children.add(frame);
            updateCount.run();
        });

        WButton closeAllButton = new WButton(Messages.get("multiplewindows.closeall"));
        closeAllButton.addActionListener(() -> {
            for (WFrame frame : children) {
                frame.dispose();
            }
            updateCount.run();
        });

        WPanel buttons = new WPanel(8.0, Orientation.HORIZONTAL);
        buttons.add(openButton);
        buttons.add(closeAllButton);

        WPanel body = new WPanel(8.0);
        body.add(buttons);
        body.add(count);
        return GalleryScaffold.buildExample(
            Messages.get("multiplewindows.title"), body);
    }

    // endregion

    // region TitleBar page

    /** The TitleBar page: lines up demos for trying out WTitleBar's various features. */
    static WComponent buildTitleBarPage() {
        WPanel page = GalleryScaffold.buildPage(
            "TitleBar",
            Messages.get("titlebar.description"));

        page.add(buildTitleBarInlineExample());
        page.add(buildTitleBarDragRegionExample());
        page.add(buildTitleBarEndToEndExample());
        return page;
    }

    /** 1) Operating properties inline (Title/Subtitle/back/pane-toggle/content=WAutoSuggestBox). */
    private static WComponent buildTitleBarInlineExample() {
        WTitleBar titleBar = new WTitleBar();
        titleBar.setTitle(Messages.get("titlebar.inline.sampletitle"));
        titleBar.setSubtitle(Messages.get("titlebar.inline.subtitle"));
        titleBar.setBackButtonVisible(true);
        titleBar.setPaneToggleButtonVisible(true);

        WLabel log = new WLabel(Messages.get("titlebar.inline.event.none"));
        log.setTextWrapping(TextWrapping.WRAP);
        titleBar.addBackRequestedListener(() -> {
            log.setText(Messages.get("titlebar.inline.event.back"));
        });
        titleBar.addPaneToggleRequestedListener(() -> {
            log.setText(Messages.get("titlebar.inline.event.panetoggle"));
        });

        WTextField titleField = new WTextField();
        titleField.setPlaceholderText(Messages.get("titlebar.inline.placeholder.title"));
        titleField.setWidth(200.0);
        titleField.setText(titleBar.getTitle());
        WTextField subtitleField = new WTextField();
        subtitleField.setPlaceholderText(Messages.get("titlebar.inline.subtitle"));
        subtitleField.setWidth(200.0);
        subtitleField.setText(titleBar.getSubtitle());
        WButton applyButton = new WButton(Messages.get("titlebar.inline.apply"));
        applyButton.addActionListener(() -> {
            titleBar.setTitle(titleField.getText());
            titleBar.setSubtitle(subtitleField.getText());
        });

        WCheckBox backVisibleCheck = new WCheckBox("IsBackButtonVisible");
        backVisibleCheck.setSelected(true);
        backVisibleCheck.addItemListener(e -> {
            titleBar.setBackButtonVisible(e.getStateChange() == WItemEvent.SELECTED);
        });
        WCheckBox backEnabledCheck = new WCheckBox("IsBackButtonEnabled");
        backEnabledCheck.setSelected(true);
        backEnabledCheck.addItemListener(e -> {
            titleBar.setBackButtonEnabled(e.getStateChange() == WItemEvent.SELECTED);
        });
        WCheckBox paneToggleCheck = new WCheckBox("IsPaneToggleButtonVisible");
        paneToggleCheck.setSelected(true);
        paneToggleCheck.addItemListener(e -> {
            titleBar.setPaneToggleButtonVisible(e.getStateChange() == WItemEvent.SELECTED);
        });

        WCheckBox useSearchBoxCheck = new WCheckBox(Messages.get("titlebar.inline.searchbox"));
        useSearchBoxCheck.addItemListener(e -> {
            titleBar.setContent(e.getStateChange() == WItemEvent.SELECTED ? new WAutoSuggestBox(Messages.get("titlebar.inline.search")) : null);
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

        WPanel body = new WPanel(8.0);
        body.add(titleBar);
        body.add(fields);
        body.add(checks);
        body.add(log);
        return GalleryScaffold.buildExample(
            Messages.get("titlebar.inline.title"),
            body);
    }

    /** 2) Drag region (setIsDragRegion). */
    private static WComponent buildTitleBarDragRegionExample() {
        WTitleBar titleBar = new WTitleBar();
        titleBar.setTitle(Messages.get("titlebar.drag.sampletitle"));

        WButton dragButton = new WButton(Messages.get("titlebar.drag.region"));
        WTitleBar.setIsDragRegion(dragButton, true);

        WButton autoButton = new WButton(Messages.get("titlebar.drag.auto"));
        autoButton.addActionListener(() -> {
            WTitleBar.setIsDragRegion(dragButton, null);
        });

        WButton nonDragButton = new WButton("SetIsDragRegion = false");
        nonDragButton.addActionListener(() -> {
            WTitleBar.setIsDragRegion(dragButton, false);
        });

        WLabel note = new WLabel(
            Messages.get("titlebar.drag.note"));
        note.setForeground(GalleryTheme.TEXT_SECONDARY());
        note.setTextWrapping(TextWrapping.WRAP);

        WPanel buttons = new WPanel(8.0, Orientation.HORIZONTAL);
        buttons.add(dragButton);
        buttons.add(autoButton);
        buttons.add(nonDragButton);

        WPanel body = new WPanel(8.0);
        body.add(note);
        body.add(titleBar);
        body.add(buttons);
        return GalleryScaffold.buildExample(Messages.get("titlebar.drag.title"), body);
    }

    /** 3) An end-to-end child window (a full WTitleBar + WNavigationView setup). */
    private static WComponent buildTitleBarEndToEndExample() {
        WLabel status = new WLabel(Messages.get("titlebar.endtoend.none"));

        WButton openButton = new WButton(Messages.get("titlebar.endtoend.open"));
        openButton.addActionListener(() -> {
            WFrame frame = new WFrame(Messages.get("titlebar.endtoend.window.title"));

            WTitleBar titleBar = new WTitleBar();
            titleBar.setTitle(Messages.get("titlebar.endtoend.window.title"));
            titleBar.setPaneToggleButtonVisible(true);

            WNavigationView navigationView = new WNavigationView();
            navigationView.setPaneToggleButtonVisible(false); // avoid showing it twice - only the TitleBar side is shown
            navigationView.setBackButtonVisible(NavigationViewBackButtonVisible.COLLAPSED);
            WNavigationViewItem item1 = new WNavigationViewItem(Messages.get("titlebar.endtoend.page1"));
            WNavigationViewItem item2 = new WNavigationViewItem(Messages.get("titlebar.endtoend.page2"));
            navigationView.addItem(item1);
            navigationView.addItem(item2);
            WLabel content = new WLabel(Messages.get("titlebar.endtoend.page1"));
            content.setHorizontalAlignmentInParent(HorizontalAlignment.LEFT);
            navigationView.setContent(content);
            navigationView.addSelectionListener(item -> {
                content.setText(item != null ? item.getText() : "");
            });
            navigationView.setSelectedItem(item1);

            titleBar.addPaneToggleRequestedListener(() -> {
                navigationView.setPaneOpen(!navigationView.isPaneOpen());
            });

            WGrid root = new WGrid();
            root.addRow();
            root.addRow(GridLength.star());
            root.add(titleBar, 0, 0, 1, 1);
            root.add(navigationView, 1, 0, 1, 1);

            frame.setContentPane(root);
            frame.setExtendsContentIntoTitleBar(true);
            frame.setTitleBar(titleBar);
            frame.getAppWindow().getTitleBar().setPreferredHeightOption(TitleBarHeightOption.TALL);
            frame.getAppWindow().resize(560, 400);
            frame.setVisible(true);
            status.setText(Messages.get("titlebar.endtoend.opened"));
        });

        WPanel body = new WPanel(8.0);
        body.add(openButton);
        body.add(status);
        return GalleryScaffold.buildExample(Messages.get("titlebar.endtoend.title"), body);
    }

    // endregion
}
