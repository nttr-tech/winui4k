package com.appkitbox.winui4k.sample.gallery;

import com.appkitbox.winui4k.Orientation;
import com.appkitbox.winui4k.TextWrapping;
import com.appkitbox.winui4k.VerticalAlignment;
import com.appkitbox.winui4k.WButton;
import com.appkitbox.winui4k.WCheckBox;
import com.appkitbox.winui4k.WComboBox;
import com.appkitbox.winui4k.WComponent;
import com.appkitbox.winui4k.WItemEvent;
import com.appkitbox.winui4k.WLabel;
import com.appkitbox.winui4k.WPanel;
import com.appkitbox.winui4k.extension.ribbon.WRibbon;
import com.appkitbox.winui4k.extension.ribbon.WRibbonTheme;
import com.appkitbox.winui4k.extension.ribbon.model.RibbonBackstageItemModel;
import com.appkitbox.winui4k.extension.ribbon.model.RibbonBackstagePlacement;
import com.appkitbox.winui4k.extension.ribbon.model.RibbonButtonGroupModel;
import com.appkitbox.winui4k.extension.ribbon.model.RibbonButtonModel;
import com.appkitbox.winui4k.extension.ribbon.model.RibbonCheckBoxModel;
import com.appkitbox.winui4k.extension.ribbon.model.RibbonChromeStyle;
import com.appkitbox.winui4k.extension.ribbon.model.RibbonColorPickerModel;
import com.appkitbox.winui4k.extension.ribbon.model.RibbonContextualActivation;
import com.appkitbox.winui4k.extension.ribbon.model.RibbonContextualGroupModel;
import com.appkitbox.winui4k.extension.ribbon.model.RibbonCustomizePage;
import com.appkitbox.winui4k.extension.ribbon.model.RibbonDensity;
import com.appkitbox.winui4k.extension.ribbon.model.RibbonDisplayMode;
import com.appkitbox.winui4k.extension.ribbon.model.RibbonDropDownButtonModel;
import com.appkitbox.winui4k.extension.ribbon.model.RibbonFontComboBoxModel;
import com.appkitbox.winui4k.extension.ribbon.model.RibbonFontSizeComboBoxModel;
import com.appkitbox.winui4k.extension.ribbon.model.RibbonGalleryItemModel;
import com.appkitbox.winui4k.extension.ribbon.model.RibbonGalleryModel;
import com.appkitbox.winui4k.extension.ribbon.model.RibbonGridPickerModel;
import com.appkitbox.winui4k.extension.ribbon.model.RibbonGroupItemsLayout;
import com.appkitbox.winui4k.extension.ribbon.model.RibbonGroupModel;
import com.appkitbox.winui4k.extension.ribbon.model.RibbonIcon;
import com.appkitbox.winui4k.extension.ribbon.model.RibbonIcons;
import com.appkitbox.winui4k.extension.ribbon.model.RibbonItemModel;
import com.appkitbox.winui4k.extension.ribbon.model.RibbonItemSize;
import com.appkitbox.winui4k.extension.ribbon.model.RibbonMenuItemModel;
import com.appkitbox.winui4k.extension.ribbon.model.RibbonMenuSeparatorModel;
import com.appkitbox.winui4k.extension.ribbon.model.RibbonMinimizeBehavior;
import com.appkitbox.winui4k.extension.ribbon.model.RibbonModel;
import com.appkitbox.winui4k.extension.ribbon.model.RibbonQuickAccessPosition;
import com.appkitbox.winui4k.extension.ribbon.model.RibbonReductionStrategy;
import com.appkitbox.winui4k.extension.ribbon.model.RibbonRowModel;
import com.appkitbox.winui4k.extension.ribbon.model.RibbonScreenTip;
import com.appkitbox.winui4k.extension.ribbon.model.RibbonSeparatorModel;
import com.appkitbox.winui4k.extension.ribbon.model.RibbonSpinnerModel;
import com.appkitbox.winui4k.extension.ribbon.model.RibbonSplitButtonModel;
import com.appkitbox.winui4k.extension.ribbon.model.RibbonStrings;
import com.appkitbox.winui4k.extension.ribbon.model.RibbonTabModel;
import com.appkitbox.winui4k.extension.ribbon.model.RibbonThemePalette;
import com.appkitbox.winui4k.extension.ribbon.model.RibbonThemeStyle;
import com.appkitbox.winui4k.extension.ribbon.model.RibbonToggleButtonModel;
import com.appkitbox.winui4k.extension.ribbon.model.RibbonVisibilityMode;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

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
                Messages.get("ribbon.description"));
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
        model.setApplicationButtonLabel(Messages.get("ribbon.word.file"));
        model.setTitle(Messages.get("ribbon.word.title"));
        model.getTabs().add(createHomeTab());
        model.getTabs().add(createInsertTab());

        RibbonColorPickerModel pageColor = new RibbonColorPickerModel("themes.color", Messages.get("ribbon.word.pagecolor"), RibbonIcons.COLOR);
        pageColor.setSize(RibbonItemSize.LARGE);
        model.getTabs().add(tab("design", Messages.get("ribbon.word.tab.design"), "D",
                group("themes", Messages.get("ribbon.word.group.documentformatting"), button("themes.theme", Messages.get("ribbon.word.theme"), RibbonIcons.THEME, RibbonItemSize.LARGE), pageColor)));

        RibbonToggleButtonModel printLayout = toggle("view.print", Messages.get("ribbon.word.printlayout"), RibbonIcons.PAGE, RibbonItemSize.LARGE);
        printLayout.setChecked(true);
        model.getTabs().add(tab("view", Messages.get("ribbon.word.tab.view"), "W",
                group("views", Messages.get("ribbon.word.group.views"),
                        toggle("view.reading", Messages.get("ribbon.word.readingmode"), RibbonIcons.READING_MODE, RibbonItemSize.LARGE),
                        printLayout,
                        new RibbonCheckBoxModel("view.ruler", Messages.get("ribbon.word.ruler")),
                        new RibbonCheckBoxModel("view.grid", Messages.get("ribbon.word.gridlines")))));

        // Contextual tabs: the two [Table Tools] tabs that appear only when a table is selected
        RibbonContextualGroupModel tableTools = new RibbonContextualGroupModel("tableTools", Messages.get("ribbon.word.tabletools"));
        tableTools.setActivation(RibbonContextualActivation.SELECT_ON_SHOW);
        model.getContextualGroups().add(tableTools);
        RibbonTabModel tableDesign = new RibbonTabModel("tableDesign", Messages.get("ribbon.word.tab.tabledesign"));
        tableDesign.setContextualGroupId("tableTools");
        tableDesign.getGroups().add(group("tableStyles", Messages.get("ribbon.word.group.tablestyles"), button("table.shading", Messages.get("ribbon.word.shading"), RibbonIcons.SHADING, RibbonItemSize.LARGE)));
        model.getTabs().add(tableDesign);
        RibbonTabModel tableLayout = new RibbonTabModel("tableLayout", Messages.get("ribbon.word.tab.layout"));
        tableLayout.setContextualGroupId("tableTools");
        tableLayout.getGroups().add(group("rowsColumns", Messages.get("ribbon.word.group.rowscolumns"), button("table.insertRow", Messages.get("ribbon.word.insertabove"), RibbonIcons.INSERT_ROW, RibbonItemSize.LARGE)));
        model.getTabs().add(tableLayout);

        // The QAT, the candidates that can be added to the QAT by right-clicking, and the items at the right end of the tab row
        model.getQuickAccessItems().add(withShortcut(button("qat.save", Messages.get("ribbon.word.save"), RibbonIcons.SAVE), "Ctrl+S"));
        model.getQuickAccessItems().add(withShortcut(button("qat.undo", Messages.get("ribbon.word.undo"), RibbonIcons.UNDO), "Ctrl+Z"));
        model.getQuickAccessItems().add(withShortcut(button("qat.redo", Messages.get("ribbon.word.redo"), RibbonIcons.REDO), "Ctrl+Y"));
        model.getQuickAccessCandidates().addAll(Arrays.asList(model.findItem("paste"), model.findItem("bold"), model.findItem("insert.table")));
        model.getTabStripItems().add(button("comments", Messages.get("ribbon.word.comments"), RibbonIcons.COMMENT));
        model.getTabStripItems().add(button("share", Messages.get("ribbon.word.share"), RibbonIcons.SHARE));

        // The Backstage (the full-window screen opened with [File])
        model.getBackstage().setTitle("Word");
        List<RibbonBackstageItemModel> backstage = model.getBackstage().getItems();
        backstage.add(new RibbonBackstageItemModel("home", Messages.get("ribbon.word.home"), RibbonIcons.HOME, new WLabel(Messages.get("ribbon.word.backstage.home"))));
        backstage.add(new RibbonBackstageItemModel("new", Messages.get("ribbon.word.new"), RibbonIcons.NEW_DOCUMENT, new WLabel(Messages.get("ribbon.word.backstage.new"))));
        backstage.add(new RibbonBackstageItemModel("open", Messages.get("ribbon.word.open"), RibbonIcons.OPEN, new WLabel(Messages.get("ribbon.word.backstage.open"))));
        RibbonBackstageItemModel save = new RibbonBackstageItemModel("save", Messages.get("ribbon.word.save"), RibbonIcons.SAVE);
        save.setHasSeparatorBefore(true);
        backstage.add(save);
        backstage.add(new RibbonBackstageItemModel("print", Messages.get("ribbon.word.print"), RibbonIcons.PRINT, new WLabel(Messages.get("ribbon.word.backstage.print"))));
        RibbonBackstageItemModel options = new RibbonBackstageItemModel("options", Messages.get("ribbon.word.options"), RibbonIcons.SETTINGS);
        options.setPlacement(RibbonBackstagePlacement.BOTTOM);
        backstage.add(options);
        return model;
    }

    private static RibbonTabModel createHomeTab() {
        RibbonSplitButtonModel paste = new RibbonSplitButtonModel("paste", Messages.get("ribbon.word.paste"), RibbonIcons.PASTE);
        paste.setSize(RibbonItemSize.LARGE);
        paste.setShortcut("Ctrl+V");
        paste.setScreenTip(new RibbonScreenTip(Messages.get("ribbon.word.paste.tiptitle"), Messages.get("ribbon.word.paste.tipbody")));
        paste.getMenuItems().add(menuItem("paste.keep", Messages.get("ribbon.word.paste.keep"), RibbonIcons.PASTE));
        paste.getMenuItems().add(menuItem("paste.text", Messages.get("ribbon.word.paste.text"), RibbonIcons.TEXT));
        paste.getMenuItems().add(new RibbonMenuSeparatorModel());
        paste.getMenuItems().add(menuItem("paste.special", Messages.get("ribbon.word.paste.special"), null));
        RibbonGroupModel clipboard = group("clipboard", Messages.get("ribbon.word.group.clipboard"),
                paste,
                withShortcut(button("cut", Messages.get("ribbon.word.cut"), RibbonIcons.CUT), "Ctrl+X"),
                withShortcut(button("copy", Messages.get("ribbon.word.copy"), RibbonIcons.COPY), "Ctrl+C"),
                button("formatPainter", Messages.get("ribbon.word.formatpainter"), RibbonIcons.FORMAT_PAINTER));
        clipboard.setDialogLauncherVisible(true);

        RibbonFontComboBoxModel fontName = new RibbonFontComboBoxModel("font.name");
        fontName.setSelectedItem("Yu Gothic UI");
        RibbonFontSizeComboBoxModel fontSize = new RibbonFontSizeComboBoxModel("font.size");
        fontSize.setSelectedItem(10.5);
        RibbonColorPickerModel highlight = new RibbonColorPickerModel("font.highlight", Messages.get("ribbon.word.highlight"), RibbonIcons.HIGHLIGHT);
        highlight.setShowNoColor(true);
        RibbonGroupModel font = rows(group("font", Messages.get("ribbon.word.group.font"),
                new RibbonRowModel(fontName, fontSize),
                new RibbonRowModel(
                        new RibbonButtonGroupModel(
                                withShortcut(toggle("bold", Messages.get("ribbon.word.bold"), RibbonIcons.BOLD), "Ctrl+B"),
                                withShortcut(toggle("italic", Messages.get("ribbon.word.italic"), RibbonIcons.ITALIC), "Ctrl+I"),
                                withShortcut(toggle("underline", Messages.get("ribbon.word.underline"), RibbonIcons.UNDERLINE), "Ctrl+U")),
                        new RibbonColorPickerModel("font.color", Messages.get("ribbon.word.fontcolor"), RibbonIcons.FONT_COLOR),
                        highlight)), 2);
        font.setDialogLauncherVisible(true);

        RibbonSpinnerModel indent = new RibbonSpinnerModel("indent", Messages.get("ribbon.word.indent"), 0.0);
        indent.setUnit(Messages.get("ribbon.word.indent.unit"));
        indent.setMaximum(20.0);
        RibbonGroupModel paragraph = rows(group("paragraph", Messages.get("ribbon.word.group.paragraph"),
                new RibbonRowModel(
                        new RibbonButtonGroupModel(toggle("bullets", Messages.get("ribbon.word.bullets"), RibbonIcons.BULLETS), toggle("numbering", Messages.get("ribbon.word.numbering"), RibbonIcons.NUMBERING)),
                        indent),
                new RibbonRowModel(
                        new RibbonButtonGroupModel(
                                radio("align.left", Messages.get("ribbon.word.alignleft"), RibbonIcons.ALIGN_LEFT, "align", true),
                                radio("align.center", Messages.get("ribbon.word.center"), RibbonIcons.ALIGN_CENTER, "align", false),
                                radio("align.right", Messages.get("ribbon.word.alignright"), RibbonIcons.ALIGN_RIGHT, "align", false),
                                radio("align.justify", Messages.get("ribbon.word.justify"), RibbonIcons.ALIGN_JUSTIFY, "align", false)))), 2);

        RibbonGroupModel styles = group("styles", Messages.get("ribbon.word.styles"), createStyleGallery());
        styles.setReductionOrder(1);

        RibbonDropDownButtonModel select = new RibbonDropDownButtonModel("select", Messages.get("ribbon.word.select"), RibbonIcons.SELECT);
        select.getMenuItems().add(menuItem("select.all", Messages.get("ribbon.word.selectall"), RibbonIcons.SELECT_ALL));
        select.getMenuItems().add(menuItem("select.objects", Messages.get("ribbon.word.selectobjects"), RibbonIcons.POINTER));
        RibbonGroupModel editing = group("editing", Messages.get("ribbon.word.group.editing"),
                withShortcut(button("find", Messages.get("ribbon.word.find"), RibbonIcons.FIND), "Ctrl+F"),
                withShortcut(button("replace", Messages.get("ribbon.word.replace"), RibbonIcons.REPLACE), "Ctrl+H"),
                select);
        editing.setReductionOrder(2);

        return tab("home", Messages.get("ribbon.word.home"), "H", clipboard, font, paragraph, styles, editing);
    }

    /** The style gallery (with categories and filtering; 3 to 5 columns depending on the width). */
    private static RibbonGalleryModel createStyleGallery() {
        RibbonGalleryModel gallery = new RibbonGalleryModel("styles.gallery", Messages.get("ribbon.word.styles"));
        gallery.setIcon(RibbonIcons.FONT);
        gallery.setItemWidth(76.0);
        gallery.setItemHeight(54.0);
        gallery.setMaxColumns(5);
        gallery.setMinColumns(3);
        gallery.setFilterEnabled(true);
        String[] names = {
            Messages.get("ribbon.word.style.normal"),
            Messages.get("ribbon.word.style.heading1"),
            Messages.get("ribbon.word.style.heading2"),
            Messages.get("ribbon.word.style.title"),
            Messages.get("ribbon.word.style.subtitle"),
            Messages.get("ribbon.word.style.quote"),
            Messages.get("ribbon.word.style.emphasis"),
        };
        double[] sizes = {14.0, 18.0, 16.0, 22.0, 13.0, 13.0, 13.0};
        for (int i = 0; i < names.length; i++) {
            RibbonGalleryItemModel item = new RibbonGalleryItemModel("style" + i, names[i], null, i < 3 ? Messages.get("ribbon.word.style.category.frequent") : Messages.get("ribbon.word.style.category.other"));
            item.setPreviewText(Messages.get("ribbon.word.style.preview"));
            item.setPreviewFontSize(sizes[i]);
            item.setPreviewBold(i >= 1 && i <= 3);
            gallery.getItems().add(item);
        }
        gallery.setSelectedItem(gallery.getItems().get(0));
        gallery.getMenuItems().add(menuItem("styles.clear", Messages.get("ribbon.word.clearformatting"), RibbonIcons.CLEAR_FORMATTING));
        gallery.getMenuItems().add(menuItem("styles.apply", Messages.get("ribbon.word.applystyles"), null));
        return gallery;
    }

    private static RibbonTabModel createInsertTab() {
        RibbonDropDownButtonModel table = new RibbonDropDownButtonModel("insert.table", Messages.get("ribbon.word.table"), RibbonIcons.TABLE);
        table.setSize(RibbonItemSize.LARGE);
        table.getMenuItems().add(new RibbonGridPickerModel("insert.tablePicker", Messages.get("ribbon.word.inserttable")));
        table.getMenuItems().add(new RibbonMenuSeparatorModel());
        table.getMenuItems().add(menuItem("insert.tableDialog", Messages.get("ribbon.word.inserttable.dialog"), RibbonIcons.TABLE));
        return tab("insert", Messages.get("ribbon.insert"), "N",
                group("tables", Messages.get("ribbon.word.group.tables"), table),
                group("illustrations", Messages.get("ribbon.word.group.illustrations"),
                        button("insert.picture", Messages.get("ribbon.word.pictures"), RibbonIcons.PICTURE, RibbonItemSize.LARGE),
                        button("insert.shapes", Messages.get("ribbon.word.shapes"), RibbonIcons.SHAPES, RibbonItemSize.LARGE),
                        button("insert.icons", Messages.get("ribbon.word.icons"), RibbonIcons.ICONS, RibbonItemSize.LARGE),
                        button("insert.chart", Messages.get("ribbon.word.chart"), RibbonIcons.CHART, RibbonItemSize.LARGE)),
                group("links", Messages.get("ribbon.word.group.links"), button("insert.link", Messages.get("ribbon.word.link"), RibbonIcons.LINK, RibbonItemSize.LARGE)),
                group("comments", Messages.get("ribbon.word.comments"), button("insert.comment", Messages.get("ribbon.word.comment"), RibbonIcons.COMMENT, RibbonItemSize.LARGE)));
    }

    /** A Word-style ribbon, with Options for controlling its display, theme, and state. */
    private static WComponent buildOfficeRibbonExample() {
        RibbonModel model = createOfficeModel();
        WRibbon ribbon = new WRibbon(model);
        WLabel log = new WLabel(Messages.get("ribbon.log.invoked.none"));
        log.setTextWrapping(TextWrapping.WRAP);
        ribbon.addItemInvokedListener((event) -> {
            String label = event.getItem().getLabel() != null ? event.getItem().getLabel() : event.getItem().getId();
            log.setText(event.getParameter() != null
                    ? Messages.get("ribbon.log.invokedwithparameter", label, String.valueOf(event.getParameter()))
                    : Messages.get("ribbon.log.invoked", label));
        });
        ribbon.addTabChangeListener((event) -> {
            log.setText(Messages.get("ribbon.log.tab", event.getNewTab() != null ? event.getNewTab().getLabel() : null));
        });
        ribbon.setWidth(RIBBON_WIDTH);

        WPanel options = new WPanel(8.0);
        options.add(GalleryScaffold.optionsLabel(Messages.get("ribbon.options.displaymode")));
        WComboBox displayMode = new WComboBox(Arrays.asList(Messages.get("ribbon.options.displaymode.classic"), Messages.get("ribbon.options.displaymode.simplified")));
        displayMode.setSelectedIndex(0);
        displayMode.addListSelectionListener(() -> {
            model.setDisplayMode(displayMode.getSelectedIndex() == 1 ? RibbonDisplayMode.SIMPLIFIED : RibbonDisplayMode.CLASSIC);
        });
        options.add(displayMode);

        options.add(GalleryScaffold.optionsLabel(Messages.get("ribbon.options.visibility")));
        RibbonVisibilityMode[] modes = {RibbonVisibilityMode.ALWAYS_SHOW, RibbonVisibilityMode.TABS_ONLY, RibbonVisibilityMode.FULL_SCREEN};
        WComboBox visibility = new WComboBox(Arrays.asList(Messages.get("ribbon.options.visibility.always"), Messages.get("ribbon.options.visibility.tabsonly"), Messages.get("ribbon.options.visibility.fullscreen")));
        visibility.setSelectedIndex(0);
        visibility.addListSelectionListener(() -> {
            model.setVisibilityMode(modes[Math.max(visibility.getSelectedIndex(), 0)]);
        });
        options.add(visibility);

        options.add(GalleryScaffold.optionsLabel(Messages.get("ribbon.options.density")));
        WComboBox density = new WComboBox(Arrays.asList(Messages.get("ribbon.options.density.standard"), Messages.get("ribbon.options.density.compact"), Messages.get("ribbon.options.density.touch")));
        density.setSelectedIndex(0);
        density.addListSelectionListener(() -> {
            model.setDensity(RibbonDensity.values()[Math.max(density.getSelectedIndex(), 0)]);
        });
        options.add(density);

        options.add(GalleryScaffold.optionsLabel(Messages.get("ribbon.options.theme")));
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

        options.add(GalleryScaffold.optionsLabel(Messages.get("ribbon.options.chrome")));
        WComboBox chrome = new WComboBox(names(RibbonChromeStyle.values()));
        chrome.setSelectedIndex(WRibbonTheme.getChromeStyle().ordinal());
        chrome.addListSelectionListener(() -> {
            WRibbonTheme.applyChromeStyle(RibbonChromeStyle.values()[Math.max(chrome.getSelectedIndex(), 0)]);
        });
        options.add(chrome);

        // The language of the strings the ribbon shows itself (ScreenTips, menus, customization, etc.). Shared by all ribbons
        options.add(GalleryScaffold.optionsLabel(Messages.get("ribbon.options.language")));
        List<String> cultures = new ArrayList<>(RibbonStrings.getBuiltInCultures());
        List<String> cultureNames = new ArrayList<>();
        for (String tag : cultures) {
            Locale locale = Locale.forLanguageTag(tag);
            cultureNames.add(locale.getDisplayName(locale) + " (" + tag + ")");
        }
        WComboBox language = new WComboBox(cultureNames);
        language.setSelectedIndex(Math.max(cultures.indexOf(RibbonStrings.getCurrent().getLocale().getLanguage()), 0));
        language.addListSelectionListener(() -> {
            RibbonStrings.setCurrent(RibbonStrings.forCulture(cultures.get(Math.max(language.getSelectedIndex(), 0))));
        });
        options.add(language);

        WCheckBox qatBelow = new WCheckBox(Messages.get("ribbon.options.qatbelow"));
        qatBelow.addItemListener(e -> {
            model.setQuickAccessPosition(e.getStateChange() == WItemEvent.SELECTED ? RibbonQuickAccessPosition.BELOW_RIBBON : RibbonQuickAccessPosition.ABOVE_RIBBON);
        });
        options.add(qatBelow);
        WCheckBox tableTools = new WCheckBox(Messages.get("ribbon.options.tabletools"));
        tableTools.addItemListener(e -> {
            model.setContextualGroupVisible("tableTools", e.getStateChange() == WItemEvent.SELECTED);
        });
        options.add(tableTools);

        WPanel actions = new WPanel(6.0, Orientation.HORIZONTAL);
        WButton keyTips = new WButton("KeyTip");
        keyTips.addActionListener(ribbon::showKeyTips);
        actions.add(keyTips);
        WButton file = new WButton(Messages.get("ribbon.word.file"));
        file.addActionListener(() -> {
            ribbon.setBackstageOpen(true);
        });
        actions.add(file);
        WButton customize = new WButton(Messages.get("ribbon.options.customize"));
        customize.addActionListener(() -> {
            ribbon.showCustomizeDialog(RibbonCustomizePage.RIBBON);
        });
        actions.add(customize);
        options.add(actions);

        // Save the ribbon's state (QAT, customizations, minimized state, etc.) to JSON and restore it later
        String[] saved = {null};
        WPanel stateRow = new WPanel(6.0, Orientation.HORIZONTAL);
        WButton save = new WButton(Messages.get("ribbon.options.savestate"));
        save.addActionListener(() -> {
            saved[0] = ribbon.saveStateToJson();
            log.setText(Messages.get("ribbon.log.saved", String.valueOf(saved[0].length())));
        });
        stateRow.add(save);
        WButton restore = new WButton(Messages.get("ribbon.options.restore"));
        restore.addActionListener(() -> {
            if (saved[0] != null) {
                log.setText(Messages.get("ribbon.log.restored", String.valueOf(ribbon.loadStateFromJson(saved[0]))));
            }
        });
        stateRow.add(restore);
        options.add(stateRow);

        WPanel body = new WPanel(8.0);
        body.add(ribbon);
        body.add(log);
        return GalleryScaffold.buildExample(Messages.get("ribbon.word.example.title"), body, options);
    }

    // endregion

    // region AutoCAD-style ribbon

    /** AutoCAD style: cycling minimize states, floating panels, expanded panels, group-by-group reduction, and the panel visibility menu. */
    private static WComponent buildCadRibbonExample() {
        RibbonGroupModel draw = group("draw", Messages.get("ribbon.cad.group.draw"),
                button("draw.line", Messages.get("ribbon.cad.line"), RibbonIcons.LINE, RibbonItemSize.LARGE),
                button("draw.polyline", Messages.get("ribbon.cad.polyline"), RibbonIcons.POLYGON, RibbonItemSize.LARGE),
                button("draw.circle", Messages.get("ribbon.cad.circle"), RibbonIcons.CIRCLE),
                button("draw.rectangle", Messages.get("ribbon.cad.rectangle"), RibbonIcons.RECTANGLE));
        // Items of the expanded panel (slide-out) opened with the arrow in the title
        draw.getSlideOutItems().add(button("draw.spline", Messages.get("ribbon.cad.spline"), RibbonIcons.PEN));
        draw.getSlideOutItems().add(button("draw.hatch", Messages.get("ribbon.cad.hatch"), RibbonIcons.FILL));
        RibbonGroupModel modify = group("modify", Messages.get("ribbon.cad.group.modify"),
                button("modify.move", Messages.get("ribbon.cad.move"), RibbonIcons.MOVE),
                button("modify.rotate", Messages.get("ribbon.cad.rotate"), RibbonIcons.ROTATE),
                button("modify.trim", Messages.get("ribbon.cad.trim"), RibbonIcons.CROP),
                new RibbonSeparatorModel(),
                button("modify.erase", Messages.get("ribbon.cad.erase"), RibbonIcons.ERASER));
        modify.setReductionOrder(1);
        RibbonGroupModel layers = group("layers", Messages.get("ribbon.cad.group.layers"),
                button("layers.properties", Messages.get("ribbon.cad.layerproperties"), RibbonIcons.LAYERS, RibbonItemSize.LARGE),
                toggle("layers.lock", Messages.get("ribbon.cad.lock"), RibbonIcons.LOCK));
        layers.setReductionOrder(2);
        RibbonGroupModel measure = group("measure", Messages.get("ribbon.cad.group.measure"), button("measure.distance", Messages.get("ribbon.cad.distance"), RibbonIcons.MEASURE, RibbonItemSize.LARGE));

        RibbonModel model = new RibbonModel();
        model.setApplicationButtonLabel("A");
        model.setMinimizeBehavior(RibbonMinimizeBehavior.CYCLE_ALL);
        model.getTabs().add(tab("cadHome", Messages.get("ribbon.word.home"), null, draw, modify, layers, measure));
        model.getTabs().add(tab("cadInsert", Messages.get("ribbon.insert"), null, group("block", Messages.get("ribbon.cad.group.block"), button("block.insert", Messages.get("ribbon.insert"), RibbonIcons.COMPONENT, RibbonItemSize.LARGE))));

        WRibbon ribbon = new WRibbon(model);
        ribbon.setMinimizeButtonVisible(true);
        ribbon.setCanFloatGroups(true);
        ribbon.setVisibilityMenuEnabled(true);
        ribbon.setReductionStrategy(RibbonReductionStrategy.GROUP_BY_GROUP);
        ribbon.setWidth(CAD_RIBBON_WIDTH);
        WLabel log = new WLabel(Messages.get("ribbon.cad.hint"));
        log.setTextWrapping(TextWrapping.WRAP);
        ribbon.addGroupFloatingListener((source, group, floating) -> {
            log.setText(floating ? Messages.get("ribbon.cad.log.floating", group.getLabel()) : Messages.get("ribbon.cad.log.docked", group.getLabel()));
        });

        WPanel options = new WPanel(8.0);
        WCheckBox cadStyle = new WCheckBox(Messages.get("ribbon.cad.options.cadstyle"));
        cadStyle.addItemListener(e -> {
            WRibbonTheme.applyStyle(e.getStateChange() == WItemEvent.SELECTED ? RibbonThemeStyle.CAD : RibbonThemeStyle.OFFICE);
        });
        options.add(cadStyle);
        WButton minimize = new WButton(Messages.get("ribbon.cad.options.minimize"));
        minimize.addActionListener(ribbon::toggleMinimized);
        options.add(minimize);
        WButton floatDraw = new WButton(Messages.get("ribbon.cad.options.floatdraw"));
        floatDraw.addActionListener(() -> {
            ribbon.floatGroup(draw);
        });
        options.add(floatDraw);
        WButton dock = new WButton(Messages.get("ribbon.cad.options.dockall"));
        dock.addActionListener(ribbon::returnAllPanelsToRibbon);
        options.add(dock);
        WLabel hint = GalleryScaffold.optionsLabel(Messages.get("ribbon.cad.options.hint"));
        hint.setVerticalAlignmentInParent(VerticalAlignment.CENTER);
        options.add(hint);

        WPanel body = new WPanel(8.0);
        body.add(ribbon);
        body.add(log);
        return GalleryScaffold.buildExample(Messages.get("ribbon.cad.example.title"), body, options);
    }

    // endregion
}
