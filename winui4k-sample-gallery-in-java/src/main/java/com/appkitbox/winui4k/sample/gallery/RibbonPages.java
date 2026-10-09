package com.appkitbox.winui4k.sample.gallery;

import com.appkitbox.winui4k.Orientation;
import com.appkitbox.winui4k.TextWrapping;
import com.appkitbox.winui4k.VerticalAlignment;
import com.appkitbox.winui4k.WButton;
import com.appkitbox.winui4k.WCheckBox;
import com.appkitbox.winui4k.WComboBox;
import com.appkitbox.winui4k.WComponent;
import com.appkitbox.winui4k.WLabel;
import com.appkitbox.winui4k.WPanel;
import com.appkitbox.winui4k.WRibbon;
import com.appkitbox.winui4k.WRibbonTheme;
import com.appkitbox.winui4k.ribbon.RibbonBackstageItemModel;
import com.appkitbox.winui4k.ribbon.RibbonBackstagePlacement;
import com.appkitbox.winui4k.ribbon.RibbonButtonGroupModel;
import com.appkitbox.winui4k.ribbon.RibbonButtonModel;
import com.appkitbox.winui4k.ribbon.RibbonCheckBoxModel;
import com.appkitbox.winui4k.ribbon.RibbonChromeStyle;
import com.appkitbox.winui4k.ribbon.RibbonColorPickerModel;
import com.appkitbox.winui4k.ribbon.RibbonContextualActivation;
import com.appkitbox.winui4k.ribbon.RibbonContextualGroupModel;
import com.appkitbox.winui4k.ribbon.RibbonCustomizePage;
import com.appkitbox.winui4k.ribbon.RibbonDensity;
import com.appkitbox.winui4k.ribbon.RibbonDisplayMode;
import com.appkitbox.winui4k.ribbon.RibbonDropDownButtonModel;
import com.appkitbox.winui4k.ribbon.RibbonFontComboBoxModel;
import com.appkitbox.winui4k.ribbon.RibbonFontSizeComboBoxModel;
import com.appkitbox.winui4k.ribbon.RibbonGalleryItemModel;
import com.appkitbox.winui4k.ribbon.RibbonGalleryModel;
import com.appkitbox.winui4k.ribbon.RibbonGridPickerModel;
import com.appkitbox.winui4k.ribbon.RibbonGroupItemsLayout;
import com.appkitbox.winui4k.ribbon.RibbonGroupModel;
import com.appkitbox.winui4k.ribbon.RibbonIcon;
import com.appkitbox.winui4k.ribbon.RibbonIcons;
import com.appkitbox.winui4k.ribbon.RibbonItemModel;
import com.appkitbox.winui4k.ribbon.RibbonItemSize;
import com.appkitbox.winui4k.ribbon.RibbonMenuItemModel;
import com.appkitbox.winui4k.ribbon.RibbonMenuSeparatorModel;
import com.appkitbox.winui4k.ribbon.RibbonMinimizeBehavior;
import com.appkitbox.winui4k.ribbon.RibbonModel;
import com.appkitbox.winui4k.ribbon.RibbonQuickAccessPosition;
import com.appkitbox.winui4k.ribbon.RibbonReductionStrategy;
import com.appkitbox.winui4k.ribbon.RibbonRowModel;
import com.appkitbox.winui4k.ribbon.RibbonScreenTip;
import com.appkitbox.winui4k.ribbon.RibbonSeparatorModel;
import com.appkitbox.winui4k.ribbon.RibbonSpinnerModel;
import com.appkitbox.winui4k.ribbon.RibbonSplitButtonModel;
import com.appkitbox.winui4k.ribbon.RibbonTabModel;
import com.appkitbox.winui4k.ribbon.RibbonThemePalette;
import com.appkitbox.winui4k.ribbon.RibbonThemeStyle;
import com.appkitbox.winui4k.ribbon.RibbonToggleButtonModel;
import com.appkitbox.winui4k.ribbon.RibbonVisibilityMode;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/*
 * Menus & toolbars category: the demo page for Ribbon (a pure Kotlin version of RibbonSpace.WinUI; Office- and AutoCAD-style ribbons).
 */
final class RibbonPages {
    private static final double RIBBON_WIDTH = 820.0;
    private static final double CAD_RIBBON_WIDTH = 760.0;

    private RibbonPages() {
    }

    /** The Ribbon page: lines up Word-style and AutoCAD-style ribbons. */
    static WComponent buildRibbonPage() {
        WPanel page = GalleryScaffold.buildPage(
                "Ribbon",
                "An Office-style ribbon. Changes to the model (RibbonModel) are reflected in the display, and user actions are written back to the model. "
                        + "Resizing the window shrinks the groups, the Alt key shows KeyTips, and right-clicking lets you add to the QAT or customize.");
        page.add(buildOfficeRibbonExample());
        page.add(buildCadRibbonExample());
        return page;
    }

    // region Small building helpers

    private static RibbonButtonModel button(String id, String label, RibbonIcon icon, RibbonItemSize size) {
        RibbonButtonModel button = new RibbonButtonModel(id, label, icon);
        button.setSize(size);
        return button;
    }

    private static RibbonButtonModel button(String id, String label, RibbonIcon icon) {
        return button(id, label, icon, RibbonItemSize.MEDIUM);
    }

    private static RibbonToggleButtonModel toggle(String id, String label, RibbonIcon icon, RibbonItemSize size) {
        RibbonToggleButtonModel toggle = new RibbonToggleButtonModel(id, label, icon);
        toggle.setSize(size);
        return toggle;
    }

    private static RibbonToggleButtonModel toggle(String id, String label, RibbonIcon icon) {
        return toggle(id, label, icon, RibbonItemSize.SMALL);
    }

    /** A toggle where only one can be selected, like a radio button (among those with the same [groupName]). */
    private static RibbonToggleButtonModel radio(String id, String label, RibbonIcon icon, String groupName, boolean checked) {
        RibbonToggleButtonModel toggle = toggle(id, label, icon);
        toggle.setGroupName(groupName);
        toggle.setChecked(checked);
        return toggle;
    }

    private static RibbonButtonModel withShortcut(RibbonButtonModel button, String shortcut) {
        button.setShortcut(shortcut);
        return button;
    }

    private static RibbonToggleButtonModel withShortcut(RibbonToggleButtonModel toggle, String shortcut) {
        toggle.setShortcut(shortcut);
        return toggle;
    }

    private static RibbonGroupModel group(String id, String label, RibbonItemModel... items) {
        RibbonGroupModel group = new RibbonGroupModel(id, label);
        group.getItems().addAll(Arrays.asList(items));
        return group;
    }

    private static RibbonGroupModel rows(RibbonGroupModel group, int rowCount) {
        group.setItemsLayout(RibbonGroupItemsLayout.ROWS);
        group.setRowCount(rowCount);
        return group;
    }

    private static RibbonMenuItemModel menuItem(String id, String label, RibbonIcon icon) {
        return new RibbonMenuItemModel(id, label, icon);
    }

    private static RibbonTabModel tab(String id, String label, String keyTip, RibbonGroupModel... groups) {
        RibbonTabModel tab = new RibbonTabModel(id, label);
        tab.setKeyTip(keyTip);
        tab.getGroups().addAll(Arrays.asList(groups));
        return tab;
    }

    private static <T> List<String> names(T[] values) {
        List<String> names = new ArrayList<>();
        for (T value : values) {
            names.add(value.toString());
        }
        return names;
    }

    // endregion

    // region Word-style ribbon

    /** A model with Word's [Home] / [Insert] / [Design] / [View] tabs, the [Table Tools] contextual tabs, and a Backstage. */
    private static RibbonModel createOfficeModel() {
        RibbonModel model = new RibbonModel();
        model.setApplicationButtonLabel("File");
        model.setTitle("Document1 - Word");
        model.getTabs().add(createHomeTab());
        model.getTabs().add(createInsertTab());

        RibbonColorPickerModel pageColor = new RibbonColorPickerModel("themes.color", "Page Color", RibbonIcons.COLOR);
        pageColor.setSize(RibbonItemSize.LARGE);
        model.getTabs().add(tab("design", "Design", "D",
                group("themes", "Document Formatting", button("themes.theme", "Themes", RibbonIcons.THEME, RibbonItemSize.LARGE), pageColor)));

        RibbonToggleButtonModel printLayout = toggle("view.print", "Print Layout", RibbonIcons.PAGE, RibbonItemSize.LARGE);
        printLayout.setChecked(true);
        model.getTabs().add(tab("view", "View", "W",
                group("views", "Views",
                        toggle("view.reading", "Read Mode", RibbonIcons.READING_MODE, RibbonItemSize.LARGE),
                        printLayout,
                        new RibbonCheckBoxModel("view.ruler", "Ruler"),
                        new RibbonCheckBoxModel("view.grid", "Gridlines"))));

        // Contextual tabs: the two [Table Tools] tabs that appear only when a table is selected
        RibbonContextualGroupModel tableTools = new RibbonContextualGroupModel("tableTools", "Table Tools");
        tableTools.setActivation(RibbonContextualActivation.SELECT_ON_SHOW);
        model.getContextualGroups().add(tableTools);
        RibbonTabModel tableDesign = new RibbonTabModel("tableDesign", "Table Design");
        tableDesign.setContextualGroupId("tableTools");
        tableDesign.getGroups().add(group("tableStyles", "Table Styles", button("table.shading", "Shading", RibbonIcons.SHADING, RibbonItemSize.LARGE)));
        model.getTabs().add(tableDesign);
        RibbonTabModel tableLayout = new RibbonTabModel("tableLayout", "Layout");
        tableLayout.setContextualGroupId("tableTools");
        tableLayout.getGroups().add(group("rowsColumns", "Rows & Columns", button("table.insertRow", "Insert Above", RibbonIcons.INSERT_ROW, RibbonItemSize.LARGE)));
        model.getTabs().add(tableLayout);

        // The QAT, the candidates that can be added to the QAT by right-clicking, and the items at the right end of the tab row
        model.getQuickAccessItems().add(withShortcut(button("qat.save", "Save", RibbonIcons.SAVE), "Ctrl+S"));
        model.getQuickAccessItems().add(withShortcut(button("qat.undo", "Undo", RibbonIcons.UNDO), "Ctrl+Z"));
        model.getQuickAccessItems().add(withShortcut(button("qat.redo", "Redo", RibbonIcons.REDO), "Ctrl+Y"));
        model.getQuickAccessCandidates().addAll(Arrays.asList(model.findItem("paste"), model.findItem("bold"), model.findItem("insert.table")));
        model.getTabStripItems().add(button("comments", "Comments", RibbonIcons.COMMENT));
        model.getTabStripItems().add(button("share", "Share", RibbonIcons.SHARE));

        // The Backstage (the full-window screen opened with [File])
        model.getBackstage().setTitle("Word");
        List<RibbonBackstageItemModel> backstage = model.getBackstage().getItems();
        backstage.add(new RibbonBackstageItemModel("home", "Home", RibbonIcons.HOME, new WLabel("Your recent items appear here.")));
        backstage.add(new RibbonBackstageItemModel("new", "New", RibbonIcons.NEW_DOCUMENT, new WLabel("Create a document from a blank document or a template.")));
        backstage.add(new RibbonBackstageItemModel("open", "Open", RibbonIcons.OPEN, new WLabel("Open a file.")));
        RibbonBackstageItemModel save = new RibbonBackstageItemModel("save", "Save", RibbonIcons.SAVE);
        save.setHasSeparatorBefore(true);
        backstage.add(save);
        backstage.add(new RibbonBackstageItemModel("print", "Print", RibbonIcons.PRINT, new WLabel("Choose a printer and settings.")));
        RibbonBackstageItemModel options = new RibbonBackstageItemModel("options", "Options", RibbonIcons.SETTINGS);
        options.setPlacement(RibbonBackstagePlacement.BOTTOM);
        backstage.add(options);
        return model;
    }

    private static RibbonTabModel createHomeTab() {
        RibbonSplitButtonModel paste = new RibbonSplitButtonModel("paste", "Paste", RibbonIcons.PASTE);
        paste.setSize(RibbonItemSize.LARGE);
        paste.setShortcut("Ctrl+V");
        paste.setScreenTip(new RibbonScreenTip("Paste (Ctrl+V)", "Add content on the Clipboard to your document."));
        paste.getMenuItems().add(menuItem("paste.keep", "Keep Source Formatting", RibbonIcons.PASTE));
        paste.getMenuItems().add(menuItem("paste.text", "Keep Text Only", RibbonIcons.TEXT));
        paste.getMenuItems().add(new RibbonMenuSeparatorModel());
        paste.getMenuItems().add(menuItem("paste.special", "Paste Special...", null));
        RibbonGroupModel clipboard = group("clipboard", "Clipboard",
                paste,
                withShortcut(button("cut", "Cut", RibbonIcons.CUT), "Ctrl+X"),
                withShortcut(button("copy", "Copy", RibbonIcons.COPY), "Ctrl+C"),
                button("formatPainter", "Format Painter", RibbonIcons.FORMAT_PAINTER));
        clipboard.setDialogLauncherVisible(true);

        RibbonFontComboBoxModel fontName = new RibbonFontComboBoxModel("font.name");
        fontName.setSelectedItem("Yu Gothic UI");
        RibbonFontSizeComboBoxModel fontSize = new RibbonFontSizeComboBoxModel("font.size");
        fontSize.setSelectedItem(10.5);
        RibbonColorPickerModel highlight = new RibbonColorPickerModel("font.highlight", "Text Highlight Color", RibbonIcons.HIGHLIGHT);
        highlight.setShowNoColor(true);
        RibbonGroupModel font = rows(group("font", "Font",
                new RibbonRowModel(fontName, fontSize),
                new RibbonRowModel(
                        new RibbonButtonGroupModel(
                                withShortcut(toggle("bold", "Bold", RibbonIcons.BOLD), "Ctrl+B"),
                                withShortcut(toggle("italic", "Italic", RibbonIcons.ITALIC), "Ctrl+I"),
                                withShortcut(toggle("underline", "Underline", RibbonIcons.UNDERLINE), "Ctrl+U")),
                        new RibbonColorPickerModel("font.color", "Font Color", RibbonIcons.FONT_COLOR),
                        highlight)), 2);
        font.setDialogLauncherVisible(true);

        RibbonSpinnerModel indent = new RibbonSpinnerModel("indent", "Indent", 0.0);
        indent.setUnit("ch");
        indent.setMaximum(20.0);
        RibbonGroupModel paragraph = rows(group("paragraph", "Paragraph",
                new RibbonRowModel(
                        new RibbonButtonGroupModel(toggle("bullets", "Bullets", RibbonIcons.BULLETS), toggle("numbering", "Numbering", RibbonIcons.NUMBERING)),
                        indent),
                new RibbonRowModel(
                        new RibbonButtonGroupModel(
                                radio("align.left", "Align Left", RibbonIcons.ALIGN_LEFT, "align", true),
                                radio("align.center", "Center", RibbonIcons.ALIGN_CENTER, "align", false),
                                radio("align.right", "Align Right", RibbonIcons.ALIGN_RIGHT, "align", false),
                                radio("align.justify", "Justify", RibbonIcons.ALIGN_JUSTIFY, "align", false)))), 2);

        RibbonGroupModel styles = group("styles", "Styles", createStyleGallery());
        styles.setReductionOrder(1);

        RibbonDropDownButtonModel select = new RibbonDropDownButtonModel("select", "Select", RibbonIcons.SELECT);
        select.getMenuItems().add(menuItem("select.all", "Select All", RibbonIcons.SELECT_ALL));
        select.getMenuItems().add(menuItem("select.objects", "Select Objects", RibbonIcons.POINTER));
        RibbonGroupModel editing = group("editing", "Editing",
                withShortcut(button("find", "Find", RibbonIcons.FIND), "Ctrl+F"),
                withShortcut(button("replace", "Replace", RibbonIcons.REPLACE), "Ctrl+H"),
                select);
        editing.setReductionOrder(2);

        return tab("home", "Home", "H", clipboard, font, paragraph, styles, editing);
    }

    /** The style gallery (with categories and filtering; 3 to 5 columns depending on the width). */
    private static RibbonGalleryModel createStyleGallery() {
        RibbonGalleryModel gallery = new RibbonGalleryModel("styles.gallery", "Styles");
        gallery.setIcon(RibbonIcons.FONT);
        gallery.setItemWidth(76.0);
        gallery.setItemHeight(54.0);
        gallery.setMaxColumns(5);
        gallery.setMinColumns(3);
        gallery.setFilterEnabled(true);
        String[] names = {"Normal", "Heading 1", "Heading 2", "Title", "Subtitle", "Quote", "Emphasis"};
        double[] sizes = {14.0, 18.0, 16.0, 22.0, 13.0, 13.0, 13.0};
        for (int i = 0; i < names.length; i++) {
            RibbonGalleryItemModel item = new RibbonGalleryItemModel("style" + i, names[i], null, i < 3 ? "Common Styles" : "Other");
            item.setPreviewText("AaBbCc");
            item.setPreviewFontSize(sizes[i]);
            item.setPreviewBold(i >= 1 && i <= 3);
            gallery.getItems().add(item);
        }
        gallery.setSelectedItem(gallery.getItems().get(0));
        gallery.getMenuItems().add(menuItem("styles.clear", "Clear Formatting", RibbonIcons.CLEAR_FORMATTING));
        gallery.getMenuItems().add(menuItem("styles.apply", "Apply Styles...", null));
        return gallery;
    }

    private static RibbonTabModel createInsertTab() {
        RibbonDropDownButtonModel table = new RibbonDropDownButtonModel("insert.table", "Table", RibbonIcons.TABLE);
        table.setSize(RibbonItemSize.LARGE);
        table.getMenuItems().add(new RibbonGridPickerModel("insert.tablePicker", "Insert Table"));
        table.getMenuItems().add(new RibbonMenuSeparatorModel());
        table.getMenuItems().add(menuItem("insert.tableDialog", "Insert Table...", RibbonIcons.TABLE));
        return tab("insert", "Insert", "N",
                group("tables", "Tables", table),
                group("illustrations", "Illustrations",
                        button("insert.picture", "Pictures", RibbonIcons.PICTURE, RibbonItemSize.LARGE),
                        button("insert.shapes", "Shapes", RibbonIcons.SHAPES, RibbonItemSize.LARGE),
                        button("insert.icons", "Icons", RibbonIcons.ICONS, RibbonItemSize.LARGE),
                        button("insert.chart", "Chart", RibbonIcons.CHART, RibbonItemSize.LARGE)),
                group("links", "Links", button("insert.link", "Link", RibbonIcons.LINK, RibbonItemSize.LARGE)),
                group("comments", "Comments", button("insert.comment", "Comment", RibbonIcons.COMMENT, RibbonItemSize.LARGE)));
    }

    /** A Word-style ribbon, with Options for controlling its display, theme, and state. */
    private static WComponent buildOfficeRibbonExample() {
        RibbonModel model = createOfficeModel();
        WRibbon ribbon = new WRibbon(model);
        WLabel log = new WLabel("Invoked: none");
        log.setTextWrapping(TextWrapping.WRAP);
        ribbon.addItemInvokedListener((event) -> {
            String parameter = event.getParameter() != null ? " (parameter: " + event.getParameter() + ")" : "";
            String label = event.getItem().getLabel() != null ? event.getItem().getLabel() : event.getItem().getId();
            log.setText("Invoked: " + label + parameter);
        });
        ribbon.addTabChangeListener((event) -> {
            log.setText("Tab: " + (event.getNewTab() != null ? event.getNewTab().getLabel() : null));
        });
        ribbon.setWidth(RIBBON_WIDTH);

        WPanel options = new WPanel(8.0);
        options.add(GalleryScaffold.optionsLabel("Display mode"));
        WComboBox displayMode = new WComboBox(Arrays.asList("Classic", "Simplified"));
        displayMode.setSelectedIndex(0);
        displayMode.addListSelectionListener(() -> {
            model.setDisplayMode(displayMode.getSelectedIndex() == 1 ? RibbonDisplayMode.SIMPLIFIED : RibbonDisplayMode.CLASSIC);
        });
        options.add(displayMode);

        options.add(GalleryScaffold.optionsLabel("Visibility"));
        RibbonVisibilityMode[] modes = {RibbonVisibilityMode.ALWAYS_SHOW, RibbonVisibilityMode.TABS_ONLY, RibbonVisibilityMode.FULL_SCREEN};
        WComboBox visibility = new WComboBox(Arrays.asList("Always show Ribbon", "Show tabs only", "Full-screen mode"));
        visibility.setSelectedIndex(0);
        visibility.addListSelectionListener(() -> {
            model.setVisibilityMode(modes[Math.max(visibility.getSelectedIndex(), 0)]);
        });
        options.add(visibility);

        options.add(GalleryScaffold.optionsLabel("Density"));
        WComboBox density = new WComboBox(Arrays.asList("Standard", "Compact", "Touch"));
        density.setSelectedIndex(0);
        density.addListSelectionListener(() -> {
            model.setDensity(RibbonDensity.values()[Math.max(density.getSelectedIndex(), 0)]);
        });
        options.add(density);

        options.add(GalleryScaffold.optionsLabel("Theme (app colors)"));
        List<String> paletteNames = new ArrayList<>();
        for (RibbonThemePalette palette : RibbonThemePalette.PRESETS) {
            paletteNames.add(palette.getName());
        }
        WComboBox palette = new WComboBox(paletteNames);
        palette.setSelectedIndex(0);
        palette.addListSelectionListener(() -> {
            WRibbonTheme.applyPalette(RibbonThemePalette.PRESETS.get(Math.max(palette.getSelectedIndex(), 0)));
        });
        options.add(palette);

        options.add(GalleryScaffold.optionsLabel("Tab row color"));
        WComboBox chrome = new WComboBox(names(RibbonChromeStyle.values()));
        chrome.setSelectedIndex(WRibbonTheme.getChromeStyle().ordinal());
        chrome.addListSelectionListener(() -> {
            WRibbonTheme.applyChromeStyle(RibbonChromeStyle.values()[Math.max(chrome.getSelectedIndex(), 0)]);
        });
        options.add(chrome);

        WCheckBox qatBelow = new WCheckBox("Show the QAT below the ribbon");
        qatBelow.addItemListener((checked) -> {
            model.setQuickAccessPosition(Boolean.TRUE.equals(checked) ? RibbonQuickAccessPosition.BELOW_RIBBON : RibbonQuickAccessPosition.ABOVE_RIBBON);
        });
        options.add(qatBelow);
        WCheckBox tableTools = new WCheckBox("Table Tools (contextual tabs)");
        tableTools.addItemListener((checked) -> {
            model.setContextualGroupVisible("tableTools", Boolean.TRUE.equals(checked));
        });
        options.add(tableTools);

        WPanel actions = new WPanel(6.0, Orientation.HORIZONTAL);
        WButton keyTips = new WButton("KeyTip");
        keyTips.addActionListener(ribbon::showKeyTips);
        actions.add(keyTips);
        WButton file = new WButton("File");
        file.addActionListener(() -> {
            ribbon.setBackstageOpen(true);
        });
        actions.add(file);
        WButton customize = new WButton("Customize");
        customize.addActionListener(() -> {
            ribbon.showCustomizeDialog(RibbonCustomizePage.RIBBON);
        });
        actions.add(customize);
        options.add(actions);

        // Save the ribbon's state (QAT, customizations, minimized state, etc.) to JSON and restore it later
        String[] saved = {null};
        WPanel stateRow = new WPanel(6.0, Orientation.HORIZONTAL);
        WButton save = new WButton("Save state");
        save.addActionListener(() -> {
            saved[0] = ribbon.saveStateToJson();
            log.setText("Saved the state (" + saved[0].length() + " characters)");
        });
        stateRow.add(save);
        WButton restore = new WButton("Restore");
        restore.addActionListener(() -> {
            if (saved[0] != null) {
                log.setText("Restored: " + ribbon.loadStateFromJson(saved[0]));
            }
        });
        stateRow.add(restore);
        options.add(stateRow);

        WPanel body = new WPanel(8.0);
        body.add(ribbon);
        body.add(log);
        return GalleryScaffold.buildExample("Word-style ribbon (tabs, groups, galleries, colors, table insertion, QAT, Backstage, contextual tabs)", body, options);
    }

    // endregion

    // region AutoCAD-style ribbon

    /** AutoCAD style: cycling minimize states, floating panels, expanded panels, group-by-group reduction, and the panel visibility menu. */
    private static WComponent buildCadRibbonExample() {
        RibbonGroupModel draw = group("draw", "Draw",
                button("draw.line", "Line", RibbonIcons.LINE, RibbonItemSize.LARGE),
                button("draw.polyline", "Polyline", RibbonIcons.POLYGON, RibbonItemSize.LARGE),
                button("draw.circle", "Circle", RibbonIcons.CIRCLE),
                button("draw.rectangle", "Rectangle", RibbonIcons.RECTANGLE));
        // Items of the expanded panel (slide-out) opened with the arrow in the title
        draw.getSlideOutItems().add(button("draw.spline", "Spline", RibbonIcons.PEN));
        draw.getSlideOutItems().add(button("draw.hatch", "Hatch", RibbonIcons.FILL));
        RibbonGroupModel modify = group("modify", "Modify",
                button("modify.move", "Move", RibbonIcons.MOVE),
                button("modify.rotate", "Rotate", RibbonIcons.ROTATE),
                button("modify.trim", "Trim", RibbonIcons.CROP),
                new RibbonSeparatorModel(),
                button("modify.erase", "Erase", RibbonIcons.ERASER));
        modify.setReductionOrder(1);
        RibbonGroupModel layers = group("layers", "Layers",
                button("layers.properties", "Layer Properties", RibbonIcons.LAYERS, RibbonItemSize.LARGE),
                toggle("layers.lock", "Lock", RibbonIcons.LOCK));
        layers.setReductionOrder(2);
        RibbonGroupModel measure = group("measure", "Measure", button("measure.distance", "Distance", RibbonIcons.MEASURE, RibbonItemSize.LARGE));

        RibbonModel model = new RibbonModel();
        model.setApplicationButtonLabel("A");
        model.setMinimizeBehavior(RibbonMinimizeBehavior.CYCLE_ALL);
        model.getTabs().add(tab("cadHome", "Home", null, draw, modify, layers, measure));
        model.getTabs().add(tab("cadInsert", "Insert", null, group("block", "Block", button("block.insert", "Insert", RibbonIcons.COMPONENT, RibbonItemSize.LARGE))));

        WRibbon ribbon = new WRibbon(model);
        ribbon.setMinimizeButtonVisible(true);
        ribbon.setCanFloatGroups(true);
        ribbon.setVisibilityMenuEnabled(true);
        ribbon.setReductionStrategy(RibbonReductionStrategy.GROUP_BY_GROUP);
        ribbon.setWidth(CAD_RIBBON_WIDTH);
        WLabel log = new WLabel("Drag a panel's title to make it a floating panel. The arrow in the [Draw] title opens the expanded panel.");
        log.setTextWrapping(TextWrapping.WRAP);
        ribbon.addGroupFloatingListener((source, group, floating) -> {
            log.setText(group.getLabel() + ": " + (floating ? "floating panel" : "returned to the ribbon"));
        });

        WPanel options = new WPanel(8.0);
        WCheckBox cadStyle = new WCheckBox("CAD look (RibbonThemeStyle.CAD)");
        cadStyle.addItemListener((checked) -> {
            WRibbonTheme.applyStyle(Boolean.TRUE.equals(checked) ? RibbonThemeStyle.CAD : RibbonThemeStyle.OFFICE);
        });
        options.add(cadStyle);
        WButton minimize = new WButton("Toggle minimize");
        minimize.addActionListener(ribbon::toggleMinimized);
        options.add(minimize);
        WButton floatDraw = new WButton("Float [Draw]");
        floatDraw.addActionListener(() -> {
            ribbon.floatGroup(draw);
        });
        options.add(floatDraw);
        WButton dock = new WButton("Return all to the ribbon");
        dock.addActionListener(ribbon::returnAllPanelsToRibbon);
        options.add(dock);
        WLabel hint = GalleryScaffold.optionsLabel("The minimize behavior can also be chosen with the arrow at the top right.");
        hint.setVerticalAlignment(VerticalAlignment.CENTER);
        options.add(hint);

        WPanel body = new WPanel(8.0);
        body.add(ribbon);
        body.add(log);
        return GalleryScaffold.buildExample("AutoCAD-style ribbon (cycling minimize states, floating panels, expanded panels, group-by-group reduction)", body, options);
    }

    // endregion
}
