package com.appkitbox.winui4k

import com.appkitbox.winui4k.internal.com.ComPtr
import com.appkitbox.winui4k.internal.ffi.api.Ffi
import com.appkitbox.winui4k.internal.ffi.api.allocate
import com.appkitbox.winui4k.internal.ffi.api.withScope
import com.appkitbox.winui4k.internal.winrt.Activation
import com.appkitbox.winui4k.internal.winrt.addEventHandler
import com.appkitbox.winui4k.internal.winui.FoundationInterop
import com.appkitbox.winui4k.internal.winui.ModalLoop
import com.appkitbox.winui4k.internal.winui.WindowingInterop
import com.appkitbox.winui4k.internal.winui.XamlInterop
import com.appkitbox.winui4k.internal.winui.XamlStructs
import java.lang.ref.WeakReference
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.max

/**
 * JSplitPane-like: a panel that divides two components with a divider, side by side ([HORIZONTAL_SPLIT]) or top and bottom ([VERTICAL_SPLIT]),
 * whose size distribution can be changed by dragging the divider with the mouse. WinUI and the Windows App SDK have no panel with a movable divider
 * (GridSplitter is in the Community Toolkit), so the sizes of the three cells of a WinUI Grid (left, divider, right) are decided from Kotlin.
 * It is unrelated to [WSplitView], which has an opening and closing pane.
 *
 * The size distribution follows the same rules as Swing's BasicSplitPaneUI. Positions and sizes are integers in DIP (corresponding to Swing's pixels).
 * - On the first layout, if [dividerLocation] is negative (the default -1), puts the preferred sizes ([WComponent.preferredSize]) on the left and right,
 *   and gives the remainder (or the shortfall) to the left by the [resizeWeight] ratio and the rest to the right (same for [resetToPreferredSizes])
 * - When the panel size changes, the increase (decrease) is distributed by [resizeWeight] as well. When it would cut into a child's minimum size ([WComponent.minimumSize]),
 *   the other side yields as far as it can keep its own minimum size
 * - Layout corrects [dividerLocation] into the range 0 to (panel width - [dividerSize]). When there is only one child (the other null or hidden),
 *   that child uses everything except the divider
 * - Dragging the divider stays within the minimum sizes of the left and right children ([minimumDividerLocation] to [maximumDividerLocation]).
 *   If [isContinuousLayout] is false, only the divider's shadow moves while dragging, and the layout changes on release
 *
 * Divider position changes are received with [addChangeListener] instead of Swing's PropertyChangeListener (DIVIDER_LOCATION_PROPERTY)
 * (to avoid java.beans). As in Swing, it is called every time the value changes, whether by setting, dragging, or distribution due to panel size changes.
 *
 * When a child's visibility ([WComponent.isVisible]) or preferred size changes, calling [revalidate] redoes the distribution, as in Swing.
 * Over the divider, the mouse pointer becomes a left-right (up-down) arrow. There is no Swing oneTouchExpandable (collapse buttons) or keyboard (F8) operation.
 * Swing's JSplitPane() puts placeholder buttons on the left and right, but this class's no-arg constructor adds no children.
 */
class WSplitPane @JvmOverloads constructor(
    orientation: Int = HORIZONTAL_SPLIT,
    continuousLayout: Boolean = true,
    leftComponent: WComponent? = null,
    rightComponent: WComponent? = null,
) : WComponent(
    Activation.composeDefault(XamlInterop.CLS_Grid, XamlInterop.IID_IGridFactory),
) {
    /** JSplitPane(int, Component, Component)-like: lays out [leftComponent] and [rightComponent] in the [orientation] direction (continuous layout). */
    constructor(orientation: Int, leftComponent: WComponent?, rightComponent: WComponent?) :
        this(orientation, true, leftComponent, rightComponent)

    private val children: ComPtr by lazy {
        val panel = inspectable.queryInterface(XamlInterop.IID_IPanel)
        try {
            own(panel.getPtr(XamlInterop.IPanel_get_Children)) // IVector<UIElement>
        } finally {
            panel.release()
        }
    }
    private val rowDefinitions: ComPtr by lazy { own(inspectable.getPtr(XamlInterop.IGrid_get_RowDefinitions)) }
    private val columnDefinitions: ComPtr by lazy { own(inspectable.getPtr(XamlInterop.IGrid_get_ColumnDefinitions)) }

    /** The divider (has hit testing with a transparent background and draws a thin line in the center). Tests pass mouse input here. */
    internal val divider = WBorder()
    private val dividerLine = WBorder()

    /** The shadow showing the divider position while dragging when [isContinuousLayout] is false. */
    internal val dragIndicator = WBorder()

    private val changeListeners = ChangeListeners(this) { }

    /** The sizes of the left (top), right (bottom) and divider along the main axis (Swing's BasicHorizontalLayoutManager.sizes). */
    private val sizes = IntArray(SIZE_COUNT)
    private var dividerAt = -1
    private var lastLocation = 0

    /** The panel size along the main axis at the previous layout (0 if not laid out yet). */
    private var lastSplitPaneSize = 0
    private var doReset = false
    private var dividerLocationIsSet = false

    /** The ratio specified with [setDividerLocation] (ratio) before being shown. Converted to a position on the first layout. */
    private var pendingProportion: Double? = null

    /** Whether the left and right cells are Auto so that the children's natural sizes can be measured (before the first layout and [resetToPreferredSizes]). */
    private var autoLengths = true
    private var appliedLeading = 0

    /**
     * The amount by which the previous layout widened (narrowed) the left cell and divider. In a parent whose size is determined by its content (such as a top-bottom split inside a vertical [WPanel]),
     * the panel itself grows by that amount, so if the next size change equals this, it is not redistributed (redistributing would grow it without limit).
     */
    private var selfResize = 0
    private var inLayout = false
    private var relayoutPending = false

    private var dragging = false
    private var dragOffset = 0
    private var dragMin = 0
    private var dragMax = 0
    private var beginDragLocation = -1
    private var lastDragLocation = -1
    private var dragHandlersInstalled = false

    /**
     * JSplitPane.getOrientation / setOrientation-like: [HORIZONTAL_SPLIT] (default) to lay out left and right, or [VERTICAL_SPLIT] to lay out top and bottom.
     * Other values throw IllegalArgumentException (as in Swing). Changing the orientation keeps [dividerLocation] and fits it to the size in the new orientation.
     */
    var orientation: Int = HORIZONTAL_SPLIT
        set(value) {
            require(value == HORIZONTAL_SPLIT || value == VERTICAL_SPLIT) {
                "orientation must be HORIZONTAL_SPLIT or VERTICAL_SPLIT: $value"
            }
            if (field == value) return
            field = value
            arrangeCells()
            lastSplitPaneSize = 0
            relayout()
        }

    private val horizontal: Boolean get() = orientation == HORIZONTAL_SPLIT

    /** JSplitPane.getLeftComponent / setLeftComponent-like: the left (top in a vertical split) child. null removes it. */
    var leftComponent: WComponent? = null
        set(value) {
            if (field === value) return
            replaceChild(field, value)
            field?.let { removeNative(it) }
            field = value
            value?.let { insertNative(it, LEFT) }
            relayout()
        }

    /** JSplitPane.getRightComponent / setRightComponent-like: the right (bottom in a vertical split) child. null removes it. */
    var rightComponent: WComponent? = null
        set(value) {
            if (field === value) return
            replaceChild(field, value)
            field?.let { removeNative(it) }
            field = value
            value?.let { insertNative(it, RIGHT) }
            relayout()
        }

    /** JSplitPane.getTopComponent / setTopComponent-like: same as [leftComponent] (the top child of a vertical split). */
    var topComponent: WComponent?
        get() = leftComponent
        set(value) {
            leftComponent = value
        }

    /** JSplitPane.getBottomComponent / setBottomComponent-like: same as [rightComponent] (the bottom child of a vertical split). */
    var bottomComponent: WComponent?
        get() = rightComponent
        set(value) {
            rightComponent = value
        }

    /**
     * JSplitPane.getDividerSize / setDividerSize-like: the divider thickness (DIP). The default is 10, the same as Swing's Metal look
     * (the visible line is the center 1 DIP, and the rest is the area for grabbing the drag).
     */
    var dividerSize: Int = DEFAULT_DIVIDER_SIZE
        set(value) {
            if (field == value) return
            field = value
            relayout()
        }

    /**
     * JSplitPane.getDividerLocation / setDividerLocation(int)-like: the position of the divider's left (top) edge (DIP). The default before being shown is -1,
     * and it is decided from the left child's preferred size on the first layout. Setting it makes [lastDividerLocation] the value before setting, and if it changed,
     * the [addChangeListener] listeners are called. Values beyond the range are corrected by layout to 0 to (panel width - [dividerSize])
     * (and the listeners are called again with the corrected value). Setting a negative value redecides it from the preferred sizes, like [resetToPreferredSizes].
     */
    var dividerLocation: Int
        get() = dividerAt
        set(value) {
            pendingProportion = null
            updateLocation(value)
            if (value < 0) {
                resetToPreferredSizes()
            } else {
                dividerLocationIsSet = true
                relayout()
            }
        }

    /**
     * JSplitPane.setDividerLocation(double)-like: puts the divider at the [proportionalLocation] position of the panel width (height in a vertical split)
     * minus [dividerSize] (fractions truncated). Values outside 0.0 to 1.0 throw IllegalArgumentException (as in Swing).
     *
     * Swing has no effect when called before being shown (width 0), but this class remembers the ratio before being shown and decides the position from the width on the first layout.
     */
    fun setDividerLocation(proportionalLocation: Double) {
        require(proportionalLocation in 0.0..1.0) { "The ratio must be 0.0 to 1.0: $proportionalLocation" }
        val available = availableSize()
        if (available <= 0) {
            pendingProportion = proportionalLocation
            return
        }
        dividerLocation = ((available - dividerSize) * proportionalLocation).toInt()
    }

    /**
     * JSplitPane.getLastDividerLocation / setLastDividerLocation-like: the previous divider position. Setting [dividerLocation] makes it
     * the value before setting, and after a drag it is the value before the drag started. Layout corrections of the position (distribution of size changes, etc.) do not change it.
     */
    var lastDividerLocation: Int
        get() = lastLocation
        set(value) {
            lastLocation = value
        }

    /**
     * JSplitPane.getResizeWeight / setResizeWeight-like: when the panel size changes, the ratio of the increase (decrease) given to the left (top) child.
     * Values outside 0.0 (default; only the right side grows and shrinks) to 1.0 (only the left) throw IllegalArgumentException (as in Swing).
     */
    var resizeWeight: Double = 0.0
        set(value) {
            require(value in 0.0..1.0) { "resizeWeight must be 0.0 to 1.0: $value" }
            field = value
        }

    /**
     * JSplitPane.isContinuousLayout / setContinuousLayout-like: whether the children's layout keeps changing while the divider is dragged (true by default).
     * If false, only the divider's shadow moves while dragging, and [dividerLocation] changes on release.
     */
    var isContinuousLayout: Boolean = true

    /**
     * JSplitPane.isEnabled / setEnabled-like: whether the user can drag the divider (true by default). While false the divider cannot be grabbed
     * (the same as Swing's BasicSplitPaneDivider; setting false in the middle of a drag lets that drag continue until released).
     * It does not stop programmatic changes such as setting [dividerLocation]. It does not disable the children (as in Swing, it is not propagated to them).
     */
    var isEnabled: Boolean = true

    /**
     * JSplitPane.getMinimumDividerLocation-like: the leftmost (topmost) position the divider can be dragged to. If there is a shown left child,
     * its minimum size (the width of [WComponent.minimumSize], the height in a vertical split), otherwise 0.
     */
    val minimumDividerLocation: Int
        get() {
            val left = leftComponent ?: return 0
            return if (left.isVisible) minimumSizeOf(left) else 0
        }

    /**
     * JSplitPane.getMaximumDividerLocation-like: the rightmost (bottommost) position the divider can be dragged to. The panel width minus the right child's minimum size and
     * [dividerSize], or [minimumDividerLocation] if smaller than [minimumDividerLocation]. If there is no right child,
     * [minimumDividerLocation] (it cannot be moved, as in Swing).
     */
    val maximumDividerLocation: Int
        get() {
            val right = rightComponent
            var maxLocation = 0
            if (right != null) {
                val rightMinimum = if (right.isVisible) minimumSizeOf(right) else 0
                maxLocation = availableSize() - rightMinimum - dividerSize
            }
            return max(minimumDividerLocation, maxLocation)
        }

    /**
     * JSplitPane.resetToPreferredSizes-like: redoes the distribution from the preferred sizes of the left and right children (the remainder is distributed by [resizeWeight]).
     * If called before being shown, it does so on the first layout.
     */
    fun resetToPreferredSizes() {
        doReset = true
        dividerLocationIsSet = false
        autoLengths = true
        applyLengths()
        relayout()
    }

    /** JSplitPane.remove(Component)-like: removes [component] if it is the left or right child. */
    fun remove(component: WComponent) {
        if (leftComponent === component) leftComponent = null
        if (rightComponent === component) rightComponent = null
    }

    /** JSplitPane.removeAll-like: removes the left and right children. */
    fun removeAll() {
        leftComponent = null
        rightComponent = null
    }

    /** Redoes the distribution to reflect changes of the children's visibility or preferred sizes (same as Swing's JSplitPane.revalidate). */
    override fun revalidate() {
        super.revalidate()
        relayout()
    }

    /**
     * Receives a [WChangeEvent] (the source is this WSplitPane) every time the divider position ([dividerLocation]) changes.
     * Corresponds to Swing's addPropertyChangeListener(JSplitPane.DIVIDER_LOCATION_PROPERTY, ...). When called, [dividerLocation] is
     * the value after the change. Distribution due to panel size changes is called during XAML layout, so a dialog that waits until closed
     * cannot be opened from inside it ([IllegalStateException]; open it later with WinUiUtilities.invokeLater).
     */
    fun addChangeListener(listener: WChangeListener) {
        changeListeners.add(listener)
    }

    /** Removes one [WChangeListener] added with [addChangeListener]. */
    fun removeChangeListener(listener: WChangeListener) {
        changeListeners.remove(listener)
    }

    /** The ChangeListeners currently added (in the order added). */
    fun getChangeListeners(): Array<WChangeListener> = changeListeners.toArray()

    override fun detachChild(child: WComponent) {
        remove(child)
    }

    // ---- Layout (a port of Swing's BasicSplitPaneUI.BasicHorizontalLayoutManager) ----

    /** The panel size along the main axis (an integer in DIP; 0 before being shown). */
    private fun availableSize(): Int = floor(if (horizontal) actualWidth else actualHeight).toInt()

    private fun isValid(component: WComponent?): Boolean = component != null && component.isVisible

    private fun preferredSizeOf(component: WComponent): Int {
        val size = component.preferredSize()
        return ceil(if (horizontal) size.width else size.height).toInt()
    }

    private fun minimumSizeOf(component: WComponent): Int {
        val size = component.minimumSize
        return ceil(if (horizontal) size.width else size.height).toInt()
    }

    /**
     * Redoes the distribution. If called during layout (setting from a listener), does it again after the current layout.
     * When deciding from the preferred sizes ([autoLengths]) outside XAML layout ([fromLayoutPass] is false), first measures the children's natural sizes
     * with a synchronous layout (newly added children have not been measured yet). The distribution may complete from SizeChanged during that.
     */
    private fun relayout(fromLayoutPass: Boolean = false) {
        if (inLayout) {
            relayoutPending = true
            return
        }
        if (!fromLayoutPass && autoLengths && availableSize() > 0) uiElement.call(XamlInterop.IUIElement_UpdateLayout)
        inLayout = true
        try {
            var rounds = 0
            do {
                relayoutPending = false
                layoutOnce()
            } while (relayoutPending && ++rounds < MAX_RELAYOUT_ROUNDS)
        } finally {
            inLayout = false
        }
    }

    private fun layoutOnce() {
        val available = availableSize()
        if (available <= 0) {
            lastSplitPaneSize = 0
            return
        }
        if (!isValid(leftComponent) && !isValid(rightComponent)) {
            applyLengths() // Do not decide the position while there are no children (decide from the preferred sizes when children are added later)
            return
        }
        pendingProportion?.let { proportion ->
            pendingProportion = null
            updateLocation(((available - dividerSize) * proportion).toInt())
            dividerLocationIsSet = true
        }
        computeSizes(available)
        doReset = false
        dividerLocationIsSet = false
        lastSplitPaneSize = available
        val wasAuto = autoLengths
        autoLengths = false
        applyLengths()
        if (wasAuto) selfResize = 0 // Switching from Auto to fixed sizes is not a change of distribution
        if (sizes[LEFT] != dividerAt) {
            // As in Swing, layout corrections of the position do not change lastDividerLocation
            dividerAt = sizes[LEFT]
            changeListeners.fire()
        }
    }

    /** The branch of Swing's layoutContainer: whether to redecide from the preferred sizes, place from the position, or distribute the size change. */
    @Suppress("ComplexCondition") // A direct copy of Swing's condition
    private fun computeSizes(available: Int) {
        val selfResized = selfResize != 0 && available - lastSplitPaneSize == selfResize
        selfResize = 0
        if ((doReset && !dividerLocationIsSet) || dividerAt < 0) {
            resetSizes(available)
        } else if (lastSplitPaneSize <= 0 || available == lastSplitPaneSize || selfResized || dividerSize != sizes[DIVIDER]) {
            sizes[DIVIDER] = dividerSize
            setSizesFromLocation(dividerAt, available)
        } else {
            distributeSpace(available - lastSplitPaneSize)
        }
    }

    /** Sets the position to [value] and calls the listeners if it changed ([lastDividerLocation] becomes the value before setting). */
    private fun updateLocation(value: Int) {
        val old = dividerAt
        dividerAt = value
        lastLocation = old
        if (old != value) changeListeners.fire()
    }

    /** Swing's resetToPreferredSizes(int): places the preferred sizes and distributes the remainder by [resizeWeight]. */
    private fun resetSizes(available: Int) {
        sizes[LEFT] = leftComponent?.takeIf { isValid(it) }?.let { preferredSizeOf(it) } ?: 0
        sizes[RIGHT] = rightComponent?.takeIf { isValid(it) }?.let { preferredSizeOf(it) } ?: 0
        sizes[DIVIDER] = dividerSize
        distributeSpace(available - (sizes[LEFT] + sizes[RIGHT] + sizes[DIVIDER]))
    }

    /** Swing's setDividerLocation(int, int): puts [leftSize] on the left (fitted within 0 to the whole minus the divider) and the rest on the right. */
    private fun setSizesFromLocation(leftSize: Int, available: Int) {
        val maxSize = available - sizes[DIVIDER]
        val clamped = leftSize.coerceIn(0, max(0, maxSize))
        val leftValid = isValid(leftComponent)
        val rightValid = isValid(rightComponent)
        if (leftValid && rightValid) {
            sizes[LEFT] = clamped
            sizes[RIGHT] = maxSize - clamped
        } else if (leftValid) {
            sizes[LEFT] = maxSize
            sizes[RIGHT] = 0
        } else if (rightValid) {
            sizes[RIGHT] = maxSize
            sizes[LEFT] = 0
        }
    }

    /** Swing's distributeSpace(int, false): distributes [space] (a shortfall if negative) to the left and right by [resizeWeight], yielding to keep the minimum sizes. */
    @Suppress("CyclomaticComplexMethod", "NestedBlockDepth") // A direct copy of Swing's branches
    private fun distributeSpace(space: Int) {
        val left = leftComponent?.takeIf { it.isVisible }
        val right = rightComponent?.takeIf { it.isVisible }
        if (left != null && right != null) {
            val leftExtra = (resizeWeight * space).toInt()
            sizes[LEFT] += leftExtra
            sizes[RIGHT] += space - leftExtra
            val leftMinimum = minimumSizeOf(left)
            val rightMinimum = minimumSizeOf(right)
            val leftMinimumValid = sizes[LEFT] >= leftMinimum
            val rightMinimumValid = sizes[RIGHT] >= rightMinimum
            if (!leftMinimumValid && !rightMinimumValid) {
                moveNegativeToOther()
            } else if (!leftMinimumValid) {
                if (sizes[RIGHT] - (leftMinimum - sizes[LEFT]) < rightMinimum) {
                    if (sizes[LEFT] < 0) moveNegativeToOther()
                } else {
                    sizes[RIGHT] -= leftMinimum - sizes[LEFT]
                    sizes[LEFT] = leftMinimum
                }
            } else if (!rightMinimumValid) {
                if (sizes[LEFT] - (rightMinimum - sizes[RIGHT]) < leftMinimum) {
                    if (sizes[RIGHT] < 0) moveNegativeToOther()
                } else {
                    sizes[LEFT] -= rightMinimum - sizes[RIGHT]
                    sizes[RIGHT] = rightMinimum
                }
            }
            sizes[LEFT] = max(0, sizes[LEFT])
            sizes[RIGHT] = max(0, sizes[RIGHT])
        } else if (left != null) {
            sizes[LEFT] = max(0, sizes[LEFT] + space)
        } else if (right != null) {
            sizes[RIGHT] = max(0, sizes[RIGHT] + space)
        }
    }

    /** Takes the shortfall of the side that went negative from the other side and sets the negative side to 0. */
    private fun moveNegativeToOther() {
        if (sizes[LEFT] < 0) {
            sizes[RIGHT] += sizes[LEFT]
            sizes[LEFT] = 0
        } else if (sizes[RIGHT] < 0) {
            sizes[LEFT] += sizes[RIGHT]
            sizes[RIGHT] = 0
        }
    }

    // ---- Grid cells ----

    /** The definitions along the main axis (columns for a horizontal split, rows for a vertical split). */
    private val primaryDefinitions: ComPtr get() = if (horizontal) columnDefinitions else rowDefinitions

    /** Rebuilds the row and column definitions for the orientation and places the children and divider again. */
    private fun arrangeCells() {
        rowDefinitions.call(FoundationInterop.IVector_Clear)
        columnDefinitions.call(FoundationInterop.IVector_Clear)
        repeat(SIZE_COUNT) {
            val definition = if (horizontal) {
                Activation.activate(XamlInterop.CLS_ColumnDefinition, XamlInterop.IID_IColumnDefinition)
            } else {
                Activation.activate(XamlInterop.CLS_RowDefinition, XamlInterop.IID_IRowDefinition)
            }
            try {
                primaryDefinitions.call(FoundationInterop.IVector_Append, definition.ptr)
            } finally {
                definition.release()
            }
        }
        leftComponent?.let { placeInCell(it, LEFT_CELL) }
        rightComponent?.let { placeInCell(it, RIGHT_CELL) }
        placeInCell(divider, DIVIDER_CELL)
        placeInCell(dragIndicator, 0)
        val span = if (horizontal) XamlInterop.IGridStatics_SetColumnSpan else XamlInterop.IGridStatics_SetRowSpan
        val otherSpan = if (horizontal) XamlInterop.IGridStatics_SetRowSpan else XamlInterop.IGridStatics_SetColumnSpan
        gridStatics.call(span, dragIndicator.frameworkElement.ptr, SIZE_COUNT)
        gridStatics.call(otherSpan, dragIndicator.frameworkElement.ptr, 1)
        styleDivider()
        applyCursor()
        autoLengths = true
        applyLengths()
    }

    private fun placeInCell(component: WComponent, cell: Int) {
        val element = component.frameworkElement.ptr
        gridStatics.call(XamlInterop.IGridStatics_SetRow, element, if (horizontal) 0 else cell)
        gridStatics.call(XamlInterop.IGridStatics_SetColumn, element, if (horizontal) cell else 0)
    }

    /** Sets the cell sizes along the main axis to [sizes] (Auto before being shown and before [resetToPreferredSizes]). */
    private fun applyLengths() {
        val divider = max(0, dividerSize)
        if (autoLengths) {
            setLength(LEFT_CELL, 0.0, GRID_UNIT_AUTO)
            setLength(DIVIDER_CELL, divider.toDouble(), GRID_UNIT_PIXEL)
            setLength(RIGHT_CELL, 0.0, GRID_UNIT_AUTO)
            return
        }
        val leading = sizes[LEFT] + divider
        selfResize = leading - appliedLeading
        appliedLeading = leading
        setLength(LEFT_CELL, sizes[LEFT].toDouble(), GRID_UNIT_PIXEL)
        setLength(DIVIDER_CELL, divider.toDouble(), GRID_UNIT_PIXEL)
        setLength(RIGHT_CELL, 1.0, GRID_UNIT_STAR)
    }

    private fun setLength(cell: Int, value: Double, unit: Int) {
        val definition = primaryDefinitions.getPtr(FoundationInterop.IVector_GetAt, cell)
        try {
            val slot = if (horizontal) XamlInterop.IColumnDefinition_put_Width else XamlInterop.IRowDefinition_put_Height
            XamlStructs.putGridLength(definition, slot, value, unit)
        } finally {
            definition.release()
        }
    }

    private fun insertNative(component: WComponent, side: Int) {
        placeInCell(component, if (side == LEFT) LEFT_CELL else RIGHT_CELL)
        // Insert below (before) the divider's shadow
        children.call(FoundationInterop.IVector_InsertAt, 0, component.uiElement.ptr)
    }

    private fun removeNative(component: WComponent) {
        val index = Ffi.backend.withScope { scope ->
            val memory = Ffi.backend.memory
            val index = scope.allocate(4)
            val found = scope.allocate(1)
            children.call(FoundationInterop.IVector_IndexOf, component.uiElement.ptr, index, found)
            if (memory.getByte(found, 0) != 0.toByte()) memory.getInt(index, 0) else null
        }
        index?.let { children.call(FoundationInterop.IVector_RemoveAt, it) }
    }

    /** Matches the divider line and shadow to the orientation (a vertical line for a horizontal split). */
    private fun styleDivider() {
        if (horizontal) {
            dividerLine.width = 1.0
            dividerLine.height = Double.NaN
            dividerLine.horizontalAlignment = HorizontalAlignment.CENTER
            dividerLine.verticalAlignment = VerticalAlignment.STRETCH
            dragIndicator.horizontalAlignment = HorizontalAlignment.LEFT
            dragIndicator.verticalAlignment = VerticalAlignment.STRETCH
            dragIndicator.height = Double.NaN
        } else {
            dividerLine.height = 1.0
            dividerLine.width = Double.NaN
            dividerLine.horizontalAlignment = HorizontalAlignment.STRETCH
            dividerLine.verticalAlignment = VerticalAlignment.CENTER
            dragIndicator.horizontalAlignment = HorizontalAlignment.STRETCH
            dragIndicator.verticalAlignment = VerticalAlignment.TOP
            dragIndicator.width = Double.NaN
        }
    }

    /** Makes the mouse pointer over the divider a left-right (up-down) arrow (UIElement.ProtectedCursor; obtained by QI because it is protected). */
    private fun applyCursor() {
        val protectedElement = divider.uiElement.queryInterfaceOrNull(XamlInterop.IID_IUIElementProtected) ?: return
        try {
            val shape = if (horizontal) {
                WindowingInterop.InputSystemCursorShape_SizeWestEast
            } else {
                WindowingInterop.InputSystemCursorShape_SizeNorthSouth
            }
            val systemCursor = cursorStatics.getPtr(WindowingInterop.IInputSystemCursorStatics_Create, shape)
            try {
                val cursor = systemCursor.queryInterface(WindowingInterop.IID_IInputCursor)
                try {
                    protectedElement.call(XamlInterop.IUIElementProtected_put_ProtectedCursor, cursor.ptr)
                } finally {
                    cursor.release()
                }
            } finally {
                systemCursor.release()
            }
        } finally {
            protectedElement.release()
        }
    }

    // ---- Dragging the divider (Swing's BasicSplitPaneDivider.DragController) ----

    /** When first loaded: accepts divider drags, and keeps this wrapper while loaded ([ListenerAnchor]). */
    private fun onLoaded() {
        if (dragHandlersInstalled) return
        dragHandlersInstalled = true
        // Add the listeners when loaded so that a WSplitPane never loaded can be collected (adding them keeps the wrapper until it is loaded)
        listenerAnchor.listenerAdded()
        val handler = DividerMouseHandler(WeakReference(this))
        divider.addMouseListener(handler)
        divider.addMouseMotionListener(handler)
    }

    /** Computes the divider position during a drag (fitted into the range) from the mouse position ([event]'s position seen from the divider). */
    private fun dragLocationOf(event: WMouseEvent): Int {
        val position = if (horizontal) event.x + divider.x else event.y + divider.y
        return (position - dragOffset).coerceIn(dragMin, dragMax)
    }

    internal fun startDrag(event: WMouseEvent) {
        if (!isEnabled || lastSplitPaneSize <= 0) return
        dragOffset = if (horizontal) event.x else event.y
        dragMin = minimumDividerLocation
        dragMax = maximumDividerLocation
        beginDragLocation = dividerAt
        lastDragLocation = -1
        dragging = true
        if (!isContinuousLayout) moveIndicator(dividerAt)
    }

    internal fun continueDrag(event: WMouseEvent) {
        if (dragging) dragTo(dragLocationOf(event))
    }

    internal fun finishDrag(event: WMouseEvent) {
        if (!dragging) return
        val newLocation = dragLocationOf(event)
        dragTo(newLocation)
        lastDragLocation = -1
        dragging = false
        if (!isContinuousLayout) {
            dragIndicator.isVisible = false
            dividerLocation = newLocation
        }
        lastLocation = beginDragLocation
    }

    private fun dragTo(newLocation: Int) {
        if (newLocation == lastDragLocation) return
        lastDragLocation = newLocation
        if (isContinuousLayout) dividerLocation = newLocation else moveIndicator(newLocation)
    }

    private fun moveIndicator(newLocation: Int) {
        val size = max(0, dividerSize).toDouble()
        if (horizontal) {
            dragIndicator.width = size
            dragIndicator.setMargin(newLocation.toDouble(), 0.0, 0.0, 0.0)
        } else {
            dragIndicator.height = size
            dragIndicator.setMargin(0.0, newLocation.toDouble(), 0.0, 0.0)
        }
        dragIndicator.isVisible = true
    }

    /** The divider's mouse listener. It is reached from native handlers, so it references the WSplitPane weakly. */
    private class DividerMouseHandler(private val pane: WeakReference<WSplitPane>) : WMouseAdapter() {
        override fun mousePressed(e: WMouseEvent) {
            if (e.button == WMouseEvent.BUTTON1) pane.get()?.startDrag(e)
        }

        override fun mouseDragged(e: WMouseEvent) {
            pane.get()?.continueDrag(e)
        }

        override fun mouseReleased(e: WMouseEvent) {
            if (e.button == WMouseEvent.BUTTON1) pane.get()?.finishDrag(e)
        }
    }

    init {
        divider.background = WColor(0, 0, 0, 0) // With a background, even a transparent one, the whole divider receives the mouse
        dividerLine.background = WColor(DIVIDER_GRAY, DIVIDER_GRAY, DIVIDER_GRAY, DIVIDER_LINE_ALPHA)
        divider.child = dividerLine
        dragIndicator.background = WColor(DIVIDER_GRAY, DIVIDER_GRAY, DIVIDER_GRAY, DRAG_INDICATOR_ALPHA)
        dragIndicator.isVisible = false
        children.call(FoundationInterop.IVector_Append, divider.uiElement.ptr)
        children.call(FoundationInterop.IVector_Append, dragIndicator.uiElement.ptr)
        this.orientation = orientation
        arrangeCells()
        this.isContinuousLayout = continuousLayout
        this.leftComponent = leftComponent
        this.rightComponent = rightComponent
        // The handler stays on the native side, so it does not capture the WSplitPane strongly but looks it up from the anchor (see ListenerAnchor)
        val anchor = listenerAnchor
        frameworkElement.addEventHandler(
            "WinUI4K.SizeChangedHandler",
            XamlInterop.IID_SizeChangedEventHandler,
            XamlInterop.IFrameworkElement_add_SizeChanged,
        ) { _, _ -> (anchor.get() as? WSplitPane)?.let { pane -> ModalLoop.duringLayout { pane.relayout(fromLayoutPass = true) } } }
        frameworkElement.addEventHandler(
            "WinUI4K.LoadedHandler",
            XamlInterop.IID_RoutedEventHandler,
            XamlInterop.IFrameworkElement_add_Loaded,
        ) { _, _ -> (anchor.get() as? WSplitPane)?.onLoaded() }
    }

    companion object {
        /** JSplitPane.VERTICAL_SPLIT-like: lays out top and bottom. */
        const val VERTICAL_SPLIT = 0

        /** JSplitPane.HORIZONTAL_SPLIT-like: lays out left and right (default). */
        const val HORIZONTAL_SPLIT = 1

        private const val DEFAULT_DIVIDER_SIZE = 10
        private const val LEFT = 0
        private const val RIGHT = 1
        private const val DIVIDER = 2
        private const val SIZE_COUNT = 3
        private const val LEFT_CELL = 0
        private const val DIVIDER_CELL = 1
        private const val RIGHT_CELL = 2
        private const val GRID_UNIT_AUTO = 0
        private const val GRID_UNIT_PIXEL = 1
        private const val GRID_UNIT_STAR = 2
        private const val MAX_RELAYOUT_ROUNDS = 4
        private const val DIVIDER_GRAY = 128
        private const val DIVIDER_LINE_ALPHA = 160
        private const val DRAG_INDICATOR_ALPHA = 96

        private val gridStatics: ComPtr by lazy { Activation.factory(XamlInterop.CLS_Grid, XamlInterop.IID_IGridStatics) }
        private val cursorStatics: ComPtr by lazy {
            Activation.factory(WindowingInterop.CLS_InputSystemCursor, WindowingInterop.IID_IInputSystemCursorStatics)
        }
    }
}
