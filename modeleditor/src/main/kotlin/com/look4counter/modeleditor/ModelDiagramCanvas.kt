package com.look4counter.modeleditor

import com.intellij.util.ui.UIUtil
import com.intellij.ui.JBColor
import com.intellij.ide.CopyProvider
import com.intellij.ide.DeleteProvider
import com.intellij.ide.PasteProvider
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonShortcuts
import com.intellij.openapi.actionSystem.DataSink
import com.intellij.openapi.actionSystem.PlatformDataKeys
import com.intellij.openapi.actionSystem.UiDataProvider
import com.intellij.openapi.project.DumbAwareAction
import java.awt.BasicStroke
import java.awt.AlphaComposite
import java.awt.Color
import java.awt.Dimension
import java.awt.Graphics
import java.awt.Graphics2D
import java.awt.RenderingHints
import java.awt.Cursor
import java.awt.Point
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import java.awt.event.KeyAdapter
import java.awt.event.KeyEvent
import java.awt.geom.Area
import java.awt.geom.Ellipse2D
import java.awt.geom.Path2D
import java.awt.geom.RoundRectangle2D
import java.awt.geom.Rectangle2D
import java.awt.geom.Line2D
import javax.swing.JComponent
import javax.swing.JViewport
import javax.swing.event.ChangeListener
import javax.swing.SwingUtilities
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.ceil
import kotlin.math.roundToInt
import java.util.Locale

class ModelDiagramCanvas : JComponent(), UiDataProvider, javax.swing.Scrollable {
    var navigationOnly = false
    var snapEnabled = false
        set(value) {
            if (field == value) return
            field = value
            if (!value) {
                alignmentGuides = emptyList()
                lastChildDrag?.let { previewChildDrag(it, true) }
            }
            repaint()
        }
    override fun getPreferredScrollableViewportSize(): Dimension = preferredSize
    override fun getScrollableUnitIncrement(visible: java.awt.Rectangle, orientation: Int, direction: Int) = 30
    override fun getScrollableBlockIncrement(visible: java.awt.Rectangle, orientation: Int, direction: Int) =
        maxOf(30, (if (orientation == javax.swing.SwingConstants.HORIZONTAL) visible.width else visible.height) - 30)
    override fun getScrollableTracksViewportWidth() = navigationOnly
    override fun getScrollableTracksViewportHeight() = navigationOnly
    var diagram = ModelDiagram(emptyList(), emptyList())
        private set
    private var paths = emptyList<Pair<DiagramWire, List<DiagramPoint>>>()
    private var origin = DiagramPoint(0.0, 0.0)
    private var originInitialized = false
    private val endpointPreview = mutableMapOf<Pair<Int, Boolean>, DiagramPoint>()
    private var wireDrag: Pair<Int, Boolean>? = null
    private var wireDragged = false
    private var originalWire: DiagramWire? = null
    private var bendDrag: WireHandle? = null
    private data class WireHandle(val pointIndex: Int?, val center: DiagramPoint, val size: Int, val segmentIndex: Int? = null)
    var pan = DiagramPoint(0.0, 0.0)
        private set
    private val wireColor = Color(145, 145, 145)
    private var scaleFactor = 1.0
    val zoom: Double get() = scaleFactor
    var onZoomChanged: (() -> Unit)? = null
    private val selection = linkedSetOf<DiagramNode>()
    val selectedNodes: List<DiagramNode> get() = selection.toList()
    val lastSelectedNode: DiagramNode? get() = selection.lastOrNull()
    var selectedWire: DiagramWire? = null
        private set
    var canMoveNodes: () -> Boolean = { true }
    var onMoveStarted: (() -> Unit)? = null
    var onNodesMoved: ((Map<DiagramNode, DiagramPoint>) -> Unit)? = null
    var onNodeResized: ((DiagramNode, DiagramNode) -> Unit)? = null
    var onNodesResized: ((Map<DiagramNode, DiagramNode>) -> Unit)? = null
    var onWireEdited: ((DiagramWire, DiagramWire, Int?) -> Unit)? = null
    var onWireEditRequested: ((DiagramWire) -> Unit)? = null
    var onActivityEditRequested: ((DiagramNode) -> Unit)? = null
    var onChildCreateRequested: ((DiagramNodeKind, DiagramPoint) -> Unit)? = null
    var onCreationTrace: ((String) -> Unit)? = null
    var lastCreationInputId: String? = null
        private set
    var onWireCreateRequested: ((DiagramWire) -> Unit)? = null
    var onCreationModeChanged: ((DiagramNodeKind?) -> Unit)? = null
    var onWireCreationModeChanged: ((Boolean) -> Unit)? = null
    var activityColor: (DiagramNode) -> Color? = { null }
    var activityForeground: (DiagramNode) -> Color? = { null }
    var changeHighlights: DiagramChangeHighlights = DiagramChangeHighlights()
        set(value) { field = value; repaint() }
    var creationPreset: ActivityPreset? = null
        private set
    var wireCreationMode = false
        private set
    private var wireCreationStart: DiagramNode? = null
    private var wireCreationStartPoint: DiagramPoint? = null
    private var wireCreationPoint: DiagramPoint? = null
    private var wireCreationTarget: DiagramNode? = null
    var creationKind: DiagramNodeKind? = null
        private set
    var creationRevision = 0L
        private set
    private var creationPoint: DiagramPoint? = null
    private var creationPressPoint: Point? = null
    private var creationPressRevision: Long = -1L
    private var creationReleaseHandledAt: Long = Long.MIN_VALUE
    private var creationReleaseHandledPoint: Point? = null
    private val creationClickTolerance = 4
    val ghostNode: DiagramNode? get() {
        val kind = creationKind ?: return null
        val point = creationPoint ?: return null
        if (!canMoveNodes() || diagram.message != null) return null
        val width = if (kind == DiagramNodeKind.Activity) 100.0 else 20.0
        val height = if (kind == DiagramNodeKind.Activity) 30.0 else 20.0
        return DiagramNode(kind, "", if (kind == DiagramNodeKind.Activity) creationPreset?.name ?: "Activity" else "",
            DiagramPoint(point.x - width / 2, point.y - height / 2), width, height,
            className = creationPreset?.properties?.firstOrNull { it.key == "class" }?.value?.trim().orEmpty())
    }
    val wireCreationPreview: DiagramWire? get() {
        val start = wireCreationStart ?: return null
        val point = wireCreationPoint ?: return null
        if (!wireCreationMode || !canMoveNodes() || diagram.message != null) return null
        val target = wireCreationTarget
        if (target != null && target.id == start.id && target.kind == start.kind) return null
        val end = target ?: DiagramNode(DiagramNodeKind.InitialState, "wire-preview", "",
            point + DiagramPoint(-10.0, -10.0), 20.0, 20.0)
        return draftWire(start, end, wireCreationStartPoint ?: start.center, point, attachSnap = target != null)
    }
    private var resizeLeft: Boolean? = null
    private var dragBase: ModelDiagram? = null
    private var dragAnchor: Point? = null
    private var draggedNodes = emptyList<DiagramNode>()
    var alignmentGuides = emptyList<AlignmentGuide>()
        private set
    private var alignmentTargets = emptyList<DiagramNode>()
    private var lastChildDrag: Point? = null
    private var previous: Point? = null
    private var boxStart: DiagramPoint? = null
    private var retained = emptyList<DiagramNode>()
    private var gestureSelection = emptyList<DiagramNode>()
    private var gestureWire: DiagramWire? = null
    private var gesturePan = DiagramPoint(0.0, 0.0)
    var copyProvider: CopyProvider? = null
    var pasteProvider: PasteProvider? = null
    var deleteProvider: DeleteProvider? = null
    val isDragging: Boolean get() = wireCreationStart != null || miniMapDrag != null || previous != null || boxStart != null || dragBase != null || wireDrag != null || bendDrag != null
    val screenOffset: DiagramPoint get() = DiagramPoint(origin.x + pan.x - visibleRect.x, origin.y + pan.y - visibleRect.y)
    var selectionBox: Rectangle2D.Double? = null
        private set
    private var observedViewport: JViewport? = null
    private var miniMapDrag: MiniMapProjection? = null
    private val viewportListener = ChangeListener { repaint() }
    val diagramPropertyLines: List<String> get() = diagram.properties.filter { it.key != "description" }
        .flatMap { "${it.key}=${it.value}".lineSequence().toList() }
    val miniMapProjection: MiniMapProjection? get() = DiagramMiniMap.layout(diagram.nodes, paths.map { it.second },
        paths.mapNotNull { (wire, path) -> if (!wire.showsCondition) null else labelBounds(path, wire.condition) },
        visibleRect, origin + pan, zoom)

    init {
        name = "model.diagram"
        isOpaque = true
        background = UIUtil.getPanelBackground()
        foreground = Color.BLACK
        isFocusable = true
        accessibleContext?.accessibleName = "Glue Activity Diagram"
        object : DumbAwareAction("드래그 취소") {
            override fun actionPerformed(event: AnActionEvent) = cancelDrag()
        }.registerCustomShortcutSet(CommonShortcuts.ESCAPE, this)
        object : DumbAwareAction("선택 요소 삭제") {
            override fun getActionUpdateThread() = ActionUpdateThread.EDT
            override fun update(event: AnActionEvent) {
                event.presentation.isEnabled = deleteProvider?.canDeleteElement(event.dataContext) == true
            }
            override fun actionPerformed(event: AnActionEvent) {
                deleteProvider?.let { if (it.canDeleteElement(event.dataContext)) it.deleteElement(event.dataContext) }
            }
        }.registerCustomShortcutSet(CommonShortcuts.DELETE, this)
        val navigation = object : MouseAdapter() {
            override fun mousePressed(event: MouseEvent) {
                traceCreationMouse(event)
                if (SwingUtilities.isLeftMouseButton(event) && miniMapProjection?.frame?.contains(event.point) == true) {
                    val projection = miniMapProjection!!
                    if (projection.contentArea.contains(event.point)) {
                        if (isDragging) cancelDrag()
                        gesturePan = pan
                        gestureSelection = selectedNodes
                        gestureWire = selectedWire
                        miniMapDrag = projection
                        navigateMiniMap(event.point, projection)
                    }
                    requestFocusInWindow()
                    event.consume()
                    return
                }
                if (wireCreationMode && SwingUtilities.isLeftMouseButton(event)) {
                    updateWireCreation(event.point)
                    requestFocusInWindow()
                    repaint()
                    event.consume()
                    return
                }
                if (creationKind != null && SwingUtilities.isLeftMouseButton(event)) {
                    if (creationReleaseHandledAt != Long.MIN_VALUE &&
                        event.`when` - creationReleaseHandledAt <= 750L &&
                        creationReleaseHandledPoint?.distance(event.point) ?: Double.MAX_VALUE <= creationClickTolerance) {
                        creationPressPoint = null
                        creationPressRevision = -1L
                        onCreationTrace?.invoke("IGNORE input=${ChildCreationTrace.inputId(event)} reason=double_click")
                        requestFocusInWindow()
                        event.consume()
                        return
                    }
                    creationPressPoint = event.point
                    creationPressRevision = creationRevision
                    creationPoint = modelPoint(event.point)
                    requestFocusInWindow()
                    repaint()
                    event.consume()
                    return
                }
                gestureSelection = selectedNodes
                gestureWire = selectedWire
                gesturePan = pan
                dragBase = null
                dragAnchor = null
                resizeLeft = null
                alignmentGuides = emptyList()
                lastChildDrag = null
                wireDrag = null
                bendDrag = null
                originalWire = null
                wireDragged = false
                previous = null
                boxStart = null
                selectionBox = null
                if (SwingUtilities.isRightMouseButton(event)) {
                    previous = event.point
                    boxStart = null
                    selectionBox = null
                    cursor = Cursor.getPredefinedCursor(Cursor.MOVE_CURSOR)
                } else if (SwingUtilities.isLeftMouseButton(event)) {
                    val wireHandle = if (canMoveNodes()) wireHandleAt(event.point) else null
                    val handle = if (canMoveNodes()) resizeHandleAt(event.point) else null
                    val frontWire = selectedWire?.takeIf { wireAt(event.point) == it }
                    val node = if (frontWire == null) handle?.first ?: nodeAt(event.point) else null
                    if (wireHandle != null) {
                        val wire = selectedWire!!
                        val lastIndex = wirePath(wire).lastIndex
                        if (wireHandle.pointIndex == 0 || wireHandle.pointIndex == lastIndex)
                            wireDrag = wire.index to (wireHandle.pointIndex == 0)
                        else bendDrag = wireHandle
                        originalWire = wire
                        onMoveStarted?.invoke()
                        cursor = Cursor.getPredefinedCursor(Cursor.MOVE_CURSOR)
                    } else if (handle != null) {
                        selection.remove(handle.first)
                        selection.add(handle.first)
                        resizeLeft = handle.second
                        dragBase = diagram
                        dragAnchor = event.point
                        draggedNodes = selectedNodes.filter { it.isActivity }
                        cursor = Cursor.getPredefinedCursor(if (handle.second) Cursor.W_RESIZE_CURSOR else Cursor.E_RESIZE_CURSOR)
                        onMoveStarted?.invoke()
                    } else if (node != null) {
                        selectedWire = null
                        if (!event.isShiftDown && node !in selection) selection.clear()
                        selection.remove(node)
                        selection.add(node)
                        if (canMoveNodes()) {
                            dragBase = diagram
                            dragAnchor = event.point
                            draggedNodes = selectedNodes
                            onMoveStarted?.invoke()
                        }
                    } else {
                        val wire = wireAt(event.point)
                        if (wire != null) {
                            selection.clear()
                            selectedWire = wire
                        } else {
                        selectedWire = null
                        retained = if (event.isShiftDown) selectedNodes else emptyList()
                        selection.clear()
                        selection.addAll(retained)
                        boxStart = modelPoint(event.point)
                        selectionBox = Rectangle2D.Double(boxStart!!.x, boxStart!!.y, 0.0, 0.0)
                        }
                    }
                } else return
                alignmentTargets = if (dragBase != null) diagram.nodes.filter { it !in selection } else emptyList()
                requestFocusInWindow()
                repaint()
                event.consume()
            }

            override fun mouseClicked(event: MouseEvent) {
                traceCreationMouse(event)
                lastCreationInputId = ChildCreationTrace.inputId(event)
                if (!SwingUtilities.isLeftMouseButton(event) || !canMoveNodes() || diagram.message != null) {
                    onCreationTrace?.invoke("IGNORE input=$lastCreationInputId reason=" +
                        when { !SwingUtilities.isLeftMouseButton(event) -> "not_left_button"; !canMoveNodes() -> "read_only"; else -> "invalid_diagram" })
                    return
                }
                if (miniMapProjection?.frame?.contains(event.point) == true) {
                    onCreationTrace?.invoke("IGNORE input=$lastCreationInputId reason=minimap")
                    event.consume(); return
                }
                if (wireCreationMode) {
                    onCreationTrace?.invoke("ROUTE input=$lastCreationInputId target=wire_creation")
                    if (event.clickCount != 1) return
                    val target = nodeAt(event.point, includeStateBoundary = true)
                    if (target != null) {
                        val start = wireCreationStart
                        if (start == null) {
                            gestureSelection = selectedNodes; gestureWire = selectedWire; gesturePan = pan
                            wireCreationStart = target; wireCreationStartPoint = modelPoint(event.point)
                            selection.clear(); selection.add(target); selectedWire = null
                            onMoveStarted?.invoke()
                        } else if (start.id != target.id || start.kind != target.kind) {
                            val draft = draftWire(start, target, wireCreationStartPoint ?: start.center, modelPoint(event.point))
                            onWireCreateRequested?.invoke(draft)
                            wireCreationStart = null; wireCreationStartPoint = null
                        }
                    }
                    updateWireCreation(event.point)
                    cursor = Cursor.getPredefinedCursor(Cursor.CROSSHAIR_CURSOR)
                    repaint(); event.consume(); return
                }
                val kind = creationKind
                if (kind != null) {
                    if (creationReleaseHandledAt != Long.MIN_VALUE &&
                        event.`when` - creationReleaseHandledAt <= 750L &&
                        creationReleaseHandledPoint?.distance(event.point) ?: Double.MAX_VALUE <= creationClickTolerance) {
                        onCreationTrace?.invoke("IGNORE input=$lastCreationInputId reason=release_already_submitted")
                        event.consume()
                        return
                    }
                    creationPoint = modelPoint(event.point)
                    if (event.clickCount == 1) {
                        val ghost = ghostNode
                        when {
                            ghost == null -> onCreationTrace?.invoke("IGNORE input=$lastCreationInputId reason=no_ghost")
                            onChildCreateRequested == null -> onCreationTrace?.invoke("IGNORE input=$lastCreationInputId reason=no_handler")
                            else -> {
                                onCreationTrace?.invoke("SUBMIT input=$lastCreationInputId modelX=${ghost.location.x} modelY=${ghost.location.y}")
                                onChildCreateRequested?.invoke(kind, ghost.location)
                            }
                        }
                    } else onCreationTrace?.invoke("IGNORE input=$lastCreationInputId reason=click_count count=${event.clickCount}")
                    event.consume()
                    return
                }
                onCreationTrace?.invoke("ROUTE input=$lastCreationInputId reason=child_mode_off")
                if (event.clickCount != 2) return
                val (x, y) = modelPoint(event.point)
                val label = paths.asReversed().firstOrNull { (wire, path) ->
                    wire.showsCondition && labelBounds(path, wire.condition)?.contains(x, y) == true
                }?.first
                val wire = label ?: wireAt(event.point)
                val node = if (label != null || (wire != null && wire == selectedWire)) null else nodeAt(event.point)
                clearGesture()
                if (node?.isActivity == true) {
                    selection.clear()
                    selection.add(node)
                    selectedWire = null
                    onActivityEditRequested?.invoke(node)
                } else if (node == null && wire != null) {
                    selection.clear()
                    selectedWire = wire
                    if (wire.canEditCondition) onWireEditRequested?.invoke(wire)
                }
                repaint()
                event.consume()
            }

            override fun mouseDragged(event: MouseEvent) {
                miniMapDrag?.let { projection ->
                    navigateMiniMap(event.point, projection)
                    event.consume()
                    return
                }
                if (wireCreationMode && previous == null) {
                    updateWireCreation(event.point); repaint(); event.consume(); return
                }
                val last = previous
                val start = boxStart
                val base = dragBase
                val anchor = dragAnchor
                if (last != null) {
                    pan += DiagramPoint((event.x - last.x).toDouble(), (event.y - last.y).toDouble())
                    previous = event.point
                    if (wireCreationMode) updateWireCreation(event.point)
                    if (creationKind != null) creationPoint =
                        if (miniMapProjection?.frame?.contains(event.point) == true) null else modelPoint(event.point)
                } else if (wireDrag != null) {
                    previewEndpoint(event.point)
                    wireDragged = true
                } else if (bendDrag != null) {
                    previewBendpoint(event.point)
                    wireDragged = true
                } else if (base != null && anchor != null) {
                    lastChildDrag = event.point
                    previewChildDrag(event.point, event.isAltDown)
                } else if (start != null) {
                    val end = modelPoint(event.point)
                    val box = Rectangle2D.Double(minOf(start.x, end.x), minOf(start.y, end.y),
                        kotlin.math.abs(end.x - start.x), kotlin.math.abs(end.y - start.y))
                    selectionBox = box
                    selection.clear()
                    selection.addAll(retained)
                    diagram.nodes.filter { node ->
                        node.location.x >= box.minX && node.location.y >= box.minY &&
                            node.location.x + node.width <= box.maxX && node.location.y + node.height <= box.maxY
                    }.forEach { node -> selection.remove(node); selection.add(node) }
                } else return
                repaint()
                event.consume()
            }

            override fun mouseReleased(event: MouseEvent) {
                traceCreationMouse(event)
                if (creationKind != null && creationPressPoint != null && SwingUtilities.isLeftMouseButton(event)) {
                    val press = creationPressPoint
                    val sameRevision = creationPressRevision == creationRevision
                    creationPressPoint = null
                    creationPressRevision = -1L
                    if (press != null && sameRevision) {
                        val distance = press.distance(event.point)
                        if (distance <= creationClickTolerance) {
                            creationPoint = modelPoint(event.point)
                            lastCreationInputId = ChildCreationTrace.inputId(event)
                            val ghost = ghostNode
                            if (ghost != null && onChildCreateRequested != null) {
                                onCreationTrace?.invoke("SUBMIT_RELEASE input=$lastCreationInputId modelX=${ghost.location.x} modelY=${ghost.location.y}")
                                onChildCreateRequested?.invoke(creationKind!!, ghost.location)
                                creationReleaseHandledAt = event.`when`
                                creationReleaseHandledPoint = event.point
                            } else onCreationTrace?.invoke("IGNORE input=$lastCreationInputId reason=no_ghost")
                        } else onCreationTrace?.invoke("IGNORE input=${ChildCreationTrace.inputId(event)} reason=drag distance=${distance.toInt()}")
                    }
                    event.consume()
                    repaint()
                    return
                }
                miniMapDrag?.let { projection ->
                    navigateMiniMap(event.point, projection)
                    miniMapDrag = null
                    repaint()
                    event.consume()
                    return
                }
                if (wireDrag != null || bendDrag != null) {
                    val original = originalWire!!
                    val reconnecting = wireDrag != null
                    val valid = if (reconnecting) validEndpointTarget(event.point) != null else true
                    if (wireDragged && valid) {
                        if (reconnecting) previewEndpoint(event.point) else previewBendpoint(event.point)
                    }
                    val edited = diagram.wires.single { it.index == original.index }
                    val removedBendpoint = if (edited.bendpoints.size == original.bendpoints.size - 1)
                        bendDrag?.pointIndex?.minus(1) else null
                    val commit = wireDragged && valid && edited != original
                    endpointPreview.clear()
                    wireDrag = null
                    bendDrag = null
                    originalWire = null
                    if (commit) {
                        if (onWireEdited != null) onWireEdited?.invoke(original, edited, removedBendpoint)
                        else display(DiagramEdits.applyWire(diagram, edited))
                    } else replaceWire(original)
                    cursor = Cursor.getDefaultCursor()
                    repaint()
                    event.consume()
                    return
                }
                if (previous == null && boxStart == null && dragBase == null) return
                if (dragBase != null && lastChildDrag != null) {
                    previewChildDrag(event.point, event.isAltDown)
                    if (dragBase == null) return
                }
                val base = dragBase
                val resizing = resizeLeft != null
                val original = if (resizing) draggedNodes.firstOrNull() else null
                val resized = if (original != null && base != null) diagram.nodes[base.nodes.indexOf(original)] else null
                val resizedNodes = if (!resizing || base == null) emptyMap() else draggedNodes.associateWith { node ->
                    diagram.nodes[base.nodes.indexOf(node)]
                }.filter { (node, changed) -> node != changed }
                val moves = if (base == null) emptyMap() else draggedNodes.associateWith { node ->
                    diagram.nodes[base.nodes.indexOf(node)].location
                }.filter { (node, location) -> node.location != location }
                dragBase = null
                dragAnchor = null
                resizeLeft = null
                alignmentGuides = emptyList()
                alignmentTargets = emptyList()
                lastChildDrag = null
                previous = null
                boxStart = null
                selectionBox = null
                cursor = Cursor.getDefaultCursor()
                if (resizedNodes.isNotEmpty()) {
                    if (onNodesResized != null) onNodesResized?.invoke(resizedNodes)
                    else if (resizedNodes.size == 1 && onNodeResized != null && original != null && resized != null)
                        onNodeResized?.invoke(original, resized)
                    else display(diagram)
                } else if (!resizing && moves.isNotEmpty()) {
                    if (onNodesMoved != null) onNodesMoved?.invoke(moves) else display(diagram)
                }
                repaint()
                event.consume()
            }

            override fun mouseMoved(event: MouseEvent) {
                if (miniMapProjection?.frame?.contains(event.point) == true) {
                    creationPoint = null
                    wireCreationPoint = null
                    cursor = Cursor.getDefaultCursor()
                    repaint()
                    return
                }
                if (wireCreationMode) {
                    updateWireCreation(event.point)
                    cursor = Cursor.getPredefinedCursor(Cursor.CROSSHAIR_CURSOR)
                    repaint(); return
                }
                if (creationKind != null) {
                    creationPoint = modelPoint(event.point)
                    cursor = Cursor.getPredefinedCursor(Cursor.CROSSHAIR_CURSOR)
                    repaint()
                    return
                }
                if (previous != null || boxStart != null || dragBase != null || wireDrag != null || bendDrag != null) return
                val side = if (canMoveNodes()) resizeHandleAt(event.point) else null
                val type = when {
                    side != null -> if (side.second) Cursor.W_RESIZE_CURSOR else Cursor.E_RESIZE_CURSOR
                    canMoveNodes() && wireHandleAt(event.point) != null -> Cursor.MOVE_CURSOR
                    else -> Cursor.DEFAULT_CURSOR
                }
                cursor = Cursor.getPredefinedCursor(type)
            }

            override fun mouseExited(event: MouseEvent) {
                creationPoint = null
                wireCreationPoint = null
                repaint()
                if (previous == null && dragBase == null && wireDrag == null && bendDrag == null) cursor = Cursor.getDefaultCursor()
            }

            override fun mouseEntered(event: MouseEvent) {
                if (creationKind != null && miniMapProjection?.frame?.contains(event.point) != true) {
                    creationPoint = modelPoint(event.point)
                    cursor = Cursor.getPredefinedCursor(Cursor.CROSSHAIR_CURSOR)
                    repaint()
                }
            }
        }
        addMouseListener(navigation)
        addMouseMotionListener(navigation)
        addMouseWheelListener { event ->
            if (isDragging) cancelDrag()
            val amount = event.preciseWheelRotation * 30
            pan += if (event.isControlDown) DiagramPoint(-amount, 0.0) else DiagramPoint(0.0, -amount)
            creationPoint = null
            repaint()
            event.consume()
        }
        addKeyListener(object : KeyAdapter() {
            override fun keyPressed(event: KeyEvent) {
                if (event.keyCode == KeyEvent.VK_ALT) lastChildDrag?.let { previewChildDrag(it, true) }
            }
            override fun keyReleased(event: KeyEvent) {
                if (event.keyCode == KeyEvent.VK_ALT) lastChildDrag?.let { previewChildDrag(it, false) }
            }
        })
    }

    fun navigateTo(point: DiagramPoint) {
        val visible = visibleRect
        pan = DiagramPoint(visible.centerX - origin.x - point.x * zoom, visible.centerY - origin.y - point.y * zoom)
        creationPoint = null
        repaint()
    }

    fun setZoom(value: Double) {
        if (!value.isFinite()) return
        val next = (value.coerceIn(0.2, 1.5) * 10).roundToInt() / 10.0
        if (next == zoom) return
        if (isDragging) cancelDrag()
        val visible = visibleRect
        val center = modelPoint(Point(visible.centerX.toInt(), visible.centerY.toInt()))
        scaleFactor = next
        pan = DiagramPoint(visible.centerX - origin.x - center.x * zoom, visible.centerY - origin.y - center.y * zoom)
        creationPoint = null
        onZoomChanged?.invoke()
        repaint()
    }

    private fun navigateMiniMap(point: Point, projection: MiniMapProjection) {
        val area = projection.contentArea
        navigateTo(DiagramPoint((point.x.toDouble().coerceIn(area.minX, area.maxX) - projection.offsetX) / projection.scale,
            (point.y.toDouble().coerceIn(area.minY, area.maxY) - projection.offsetY) / projection.scale))
    }

    private fun previewChildDrag(point: Point, bypass: Boolean) {
        val base = dragBase ?: return
        val anchor = dragAnchor ?: return
        if (!canMoveNodes()) { cancelDrag(); return }
        val raw = DiagramPoint((point.x - anchor.x) / zoom, (point.y - anchor.y) / zoom)
        val left = resizeLeft
        val result = if (left == null) {
            if (bypass || !snapEnabled) AlignmentResult(raw, emptyList()) else DiagramAlignment.move(draggedNodes, alignmentTargets, raw)
        } else {
            val widthDelta = maxOf(if (left) -raw.x else raw.x, 16.0 - draggedNodes.minOf { it.width })
            if (bypass || !snapEnabled) AlignmentResult(DiagramPoint(widthDelta, 0.0), emptyList())
            else DiagramAlignment.resize(draggedNodes, alignmentTargets, widthDelta, left)
        }
        alignmentGuides = result.guides
        val moved = base.nodes.associateWith { node ->
            if (node !in draggedNodes) node
            else if (left != null) node.copy(width = node.width + result.delta.x,
                location = if (left) node.location + DiagramPoint(-result.delta.x, 0.0) else node.location)
            else node.copy(location = node.location + result.delta)
        }
        replaceDiagram(base.copy(nodes = base.nodes.map { moved.getValue(it) },
            wires = base.wires.map { DiagramEdits.relocateNodes(it, moved) }))
    }

    override fun updateUI() {
        super.updateUI()
        background = UIUtil.getPanelBackground()
    }

    override fun addNotify() {
        super.addNotify()
        observedViewport = SwingUtilities.getAncestorOfClass(JViewport::class.java, this) as? JViewport
        observedViewport?.addChangeListener(viewportListener)
    }

    override fun removeNotify() {
        observedViewport?.removeChangeListener(viewportListener)
        observedViewport = null
        super.removeNotify()
    }

    override fun uiDataSnapshot(sink: DataSink) {
        copyProvider?.let { sink.set(PlatformDataKeys.COPY_PROVIDER, it) }
        pasteProvider?.let { sink.set(PlatformDataKeys.PASTE_PROVIDER, it) }
        deleteProvider?.let { sink.set(PlatformDataKeys.DELETE_ELEMENT_PROVIDER, it) }
    }

    fun clearSelection() {
        selection.clear()
        selectedWire = null
        repaint()
    }

    fun cancelDrag() {
        onCreationTrace?.invoke("MODE_CANCEL kind=$creationKind revision=$creationRevision")
        if (wireCreationMode) {
            wireCreationMode = false
            wireCreationPoint = null
            cursor = Cursor.getDefaultCursor()
            onWireCreationModeChanged?.invoke(false)
            repaint()
        }
        if (creationKind != null) {
            creationRevision++
            creationKind = null
            creationPoint = null
            creationPressPoint = null
            creationPressRevision = -1L
            creationReleaseHandledAt = Long.MIN_VALUE
            creationReleaseHandledPoint = null
            cursor = Cursor.getDefaultCursor()
            onCreationModeChanged?.invoke(null)
            repaint()
        }
        if (!isDragging) return
        val base = dragBase
        val wire = originalWire
        if (base != null) replaceDiagram(base)
        else if (wire != null) replaceWire(wire)
        pan = gesturePan
        selection.clear()
        selection.addAll(gestureSelection.mapNotNull { old -> diagram.nodes.firstOrNull { it.id == old.id && it.kind == old.kind } })
        selectedWire = gestureWire?.let { old -> diagram.wires.firstOrNull { it.index == old.index } }
        clearGesture()
        rebuildPaths()
        repaint()
    }

    fun setCreationKind(kind: DiagramNodeKind?, preset: ActivityPreset? = null) {
        creationRevision++
        if (isDragging) cancelDrag()
        if (kind != null && wireCreationMode) setWireCreationMode(false)
        creationKind = kind
        creationPreset = if (kind == DiagramNodeKind.Activity) preset?.detached() else null
        creationPoint = null
        creationPressPoint = null
        creationPressRevision = -1L
        creationReleaseHandledAt = Long.MIN_VALUE
        creationReleaseHandledPoint = null
        cursor = Cursor.getPredefinedCursor(if (kind == null) Cursor.DEFAULT_CURSOR else Cursor.CROSSHAIR_CURSOR)
        onCreationModeChanged?.invoke(kind)
        onCreationTrace?.invoke("MODE_SET kind=$kind preset=${preset != null} revision=$creationRevision")
        repaint()
    }

    private fun traceCreationMouse(event: MouseEvent) {
        onCreationTrace?.invoke("CANVAS_${ChildCreationTrace.eventName(event)} ${ChildCreationTrace.mouseDetails(event)} " +
            "kind=$creationKind revision=$creationRevision wire=$wireCreationMode writable=${canMoveNodes()} invalid=${diagram.message != null} focus=$isFocusOwner zoom=$zoom")
    }

    fun setWireCreationMode(enabled: Boolean) {
        if (isDragging) cancelDrag()
        if (enabled && creationKind != null) setCreationKind(null)
        wireCreationMode = enabled
        wireCreationStart = null; wireCreationStartPoint = null; wireCreationPoint = null; wireCreationTarget = null
        cursor = Cursor.getPredefinedCursor(if (enabled) Cursor.CROSSHAIR_CURSOR else Cursor.DEFAULT_CURSOR)
        onWireCreationModeChanged?.invoke(enabled)
        repaint()
    }

    private fun updateWireCreation(point: Point) {
        wireCreationPoint = modelPoint(point)
        wireCreationTarget = nodeAt(point, includeStateBoundary = true)
    }

    private fun draftWire(start: DiagramNode, end: DiagramNode, from: DiagramPoint, to: DiagramPoint,
                          attachSnap: Boolean = true): DiagramWire {
        val first = if (snapEnabled) WireSnap.position(start, from, end) else from
        val last = if (snapEnabled) WireSnap.position(end, to, start) else to
        var wire = DiagramWire(-1, "", "", start, end,
            if (start.isActivity && end.isActivity) last else first, last, emptyList())
        if (snapEnabled && attachSnap) {
            wire = WireSnap.attach(wire, true, first)
            wire = WireSnap.attach(wire, false, last)
        }
        return wire
    }

    fun selectWireByIndex(index: Int) {
        selection.clear()
        selectedWire = diagram.wires.firstOrNull { it.index == index }
        repaint()
    }

    private fun clearGesture() {
        wireCreationStart = null; wireCreationStartPoint = null; wireCreationPoint = null; wireCreationTarget = null
        miniMapDrag = null
        alignmentGuides = emptyList()
        alignmentTargets = emptyList()
        lastChildDrag = null
        endpointPreview.clear()
        wireDrag = null
        bendDrag = null
        originalWire = null
        wireDragged = false
        dragBase = null
        dragAnchor = null
        resizeLeft = null
        draggedNodes = emptyList()
        previous = null
        boxStart = null
        retained = emptyList()
        selectionBox = null
        cursor = Cursor.getDefaultCursor()
    }

    fun selectNodesByIds(ids: Set<String>) {
        selection.clear()
        selection.addAll(diagram.nodes.filter { it.id in ids })
        selectedWire = null
        repaint()
    }

    private fun labelBounds(points: List<DiagramPoint>, text: String): Rectangle2D.Double? {
        val center = DiagramGeometry.labelCenter(points) ?: return null
        val metrics = getFontMetrics(font ?: javax.swing.UIManager.getFont("Label.font"))
        val width = metrics.stringWidth(text).toDouble()
        val height = maxOf(1, metrics.height - 3).toDouble()
        return Rectangle2D.Double(center.x - width / 2, center.y - height / 2, width, height + 1)
    }

    private fun modelPoint(point: Point) = DiagramPoint((point.x - origin.x - pan.x) / zoom, (point.y - origin.y - pan.y) / zoom)

    private fun nodeAt(point: Point, includeStateBoundary: Boolean = false): DiagramNode? {
        val (x, y) = modelPoint(point)
        val ordered = diagram.nodes.filter { it != lastSelectedNode } + listOfNotNull(lastSelectedNode)
        return ordered.asReversed().firstOrNull { node ->
            val shape = if (node.isActivity)
                RoundRectangle2D.Double(node.location.x, node.location.y, node.width, node.height, 16.0, 16.0)
            else Ellipse2D.Double(node.location.x, node.location.y, node.width, node.height)
            if (includeStateBoundary && !node.isActivity)
                kotlin.math.hypot(x - node.center.x, y - node.center.y) <= node.width / 2 + 1e-9
            else shape.contains(x, y)
        }
    }

    private fun resizeHandleAt(point: Point): Pair<DiagramNode, Boolean>? {
        val (x, y) = modelPoint(point)
        for (node in selection.toList().asReversed().filter { it.isActivity }) {
            for (left in listOf(true, false)) {
                val centerX = if (left) node.location.x else node.location.x + node.width
                if (handleBounds(DiagramPoint(centerX, node.center.y), 6).contains(x, y))
                    return node to left
            }
        }
        return null
    }

    private fun handleBounds(center: DiagramPoint, size: Int) = Rectangle2D.Double(
        (center.x.roundToInt() - size / 2).toDouble(), (center.y.roundToInt() - size / 2).toDouble(), size.toDouble(), size.toDouble())

    private fun wireHandleAt(point: Point): WireHandle? {
        val wire = selectedWire ?: return null
        val points = wirePath(wire)
        val (x, y) = modelPoint(point)
        val handles = points.mapIndexed { index, center -> WireHandle(index, center, 6) } +
            points.zipWithNext().mapIndexed { index, (a, b) -> WireHandle(null, DiagramPoint((a.x + b.x) / 2, (a.y + b.y) / 2), 4, index) }
        return handles.firstOrNull { handleBounds(it.center, 6).contains(x, y) }
    }

    private fun validEndpointTarget(point: Point): DiagramNode? {
        val original = originalWire ?: return null
        val target = nodeAt(point, includeStateBoundary = true) ?: return null
        val opposite = if (wireDrag?.second == true) original.end else original.start
        return target.takeUnless { opposite != null && it.id == opposite.id && it.kind == opposite.kind }
    }

    private fun replaceWire(wire: DiagramWire) = replaceDiagram(diagram.copy(
        wires = diagram.wires.map { if (it.index == wire.index) wire else it }))

    private fun previewEndpoint(point: Point) {
        val original = originalWire ?: return
        val key = wireDrag ?: return
        val target = validEndpointTarget(point)
        val rawPosition = modelPoint(point)
        val opposite = if (key.second) original.end else original.start
        val position = if (snapEnabled && target != null) WireSnap.position(target, rawPosition,
            opposite.takeIf { original.bendpoints.isEmpty() }) else rawPosition
        val wire = if (target == null) {
            endpointPreview[key] = position
            original
        } else {
            endpointPreview.remove(key)
            val connected = if (key.second) original.copy(start = target, firstRelativePoint = position, explicitContacts = false)
            else original.copy(end = target, lastRelativePoint = position, explicitContacts = false)
            val adjusted = if (connected.bendpoints.isEmpty() && connected.start?.isActivity == true && connected.end?.isActivity == true)
                connected.copy(firstRelativePoint = position)
            else connected
            if (snapEnabled) WireSnap.attach(adjusted, key.second, position) else adjusted
        }
        replaceWire(wire)
    }

    private fun previewBendpoint(point: Point) {
        val original = originalWire ?: return
        val handle = bendDrag ?: return
        val index = handle.pointIndex?.minus(1) ?: handle.segmentIndex!!
        val rawPosition = modelPoint(point)
        val position = if (snapEnabled) WireSnap.dragPosition(original, rawPosition) else rawPosition
        replaceWire(if (handle.pointIndex == null) DiagramEdits.dragMidpoint(original, index, position)
            else DiagramEdits.dragBendpoint(original, index, position))
    }

    fun wirePath(wire: DiagramWire): List<DiagramPoint> {
        endpointPreview[wire.index to true]?.let { return DiagramGeometry.previewStateEndpoint(wire, true, it) }
        endpointPreview[wire.index to false]?.let { return DiagramGeometry.previewStateEndpoint(wire, false, it) }
        return DiagramGeometry.path(wire)
    }

    private fun rebuildPaths() {
        paths = diagram.wires.map { it to wirePath(it) }.filter { it.second.size >= 2 }
    }

    private fun wireAt(point: Point): DiagramWire? {
        val (x, y) = modelPoint(point)
        selectedWire?.let { wire ->
            val points = wirePath(wire)
            if ((wire.showsCondition && labelBounds(points, wire.condition)?.contains(x, y) == true) ||
                points.zipWithNext().any { (a, b) -> Line2D.ptSegDist(a.x, a.y, b.x, b.y, x, y) <= 4.0 }) return wire
        }
        val reversed = paths.asReversed()
        return reversed.firstOrNull { (wire, points) ->
            wire.showsCondition && labelBounds(points, wire.condition)?.contains(x, y) == true
        }?.first ?: reversed.firstOrNull { (_, points) ->
            points.zipWithNext().any { (a, b) -> Line2D.ptSegDist(a.x, a.y, b.x, b.y, x, y) <= 4.0 }
        }?.first
    }

    fun display(value: ModelDiagram, preserveOrigin: Boolean = false) {
        clearGesture()
        if (preserveOrigin) originInitialized = true
        replaceDiagram(value)
        updateBounds()
    }

    private fun replaceDiagram(value: ModelDiagram) {
        val retained = selection.mapNotNull { old -> value.nodes.firstOrNull { it.id == old.id && it.kind == old.kind } }
        selection.clear()
        selection.addAll(retained)
        selectedWire = selectedWire?.let { old -> value.wires.firstOrNull { it.index == old.index } }
        diagram = value
        rebuildPaths()
        repaint()
    }

    private fun updateBounds() {
        val labelPoints = paths.filter { it.first.showsCondition }.flatMap { (wire, path) ->
            labelBounds(path, wire.condition)?.let { listOf(DiagramPoint(it.minX, it.minY), DiagramPoint(it.maxX, it.maxY)) }.orEmpty()
        }
        val points = diagram.nodes.flatMap { listOf(it.location, it.location + DiagramPoint(it.width, it.height)) } + paths.flatMap { it.second } + labelPoints
        val minX = minOf(0.0, points.minOfOrNull { it.x } ?: 0.0)
        val minY = minOf(0.0, points.minOfOrNull { it.y } ?: 0.0)
        if (!originInitialized && diagram.nodes.isNotEmpty()) {
            origin = DiagramPoint(if (minX < 0) 20.0 - minX else 0.0, if (minY < 0) 20.0 - minY else 0.0)
            originInitialized = true
        }
        // Re-centering or shrinking here moves unrelated nodes or clamps the viewport after an edit.
        preferredSize = Dimension(maxOf(400, preferredSize.width, ceil((points.maxOfOrNull { it.x } ?: 0.0) + origin.x + 100).toInt()),
            maxOf(300, preferredSize.height, ceil((points.maxOfOrNull { it.y } ?: 0.0) + origin.y + 60).toInt()))
        revalidate()
        repaint()
    }

    override fun paintComponent(graphics: Graphics) {
        super.paintComponent(graphics)
        val g = graphics.create() as Graphics2D
        try {
            g.color = background
            g.fillRect(0, 0, width, height)
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
            g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON)
            g.font = font ?: javax.swing.UIManager.getFont("Label.font")
            val screenTransform = g.transform
            g.translate(origin.x + pan.x, origin.y + pan.y)
            g.scale(zoom, zoom)
            g.color = foreground
            g.stroke = BasicStroke(1.2f)
            if (diagram.message != null) g.drawString(diagram.message, 0f, 18f)
            paths.forEach { (wire, points) -> drawWire(g, wire, points, changeColor(changeHighlights.wireChanges[wire.id])) }
            paths.filter { it.first.showsCondition }.forEach { (wire, points) -> drawLabel(g, wire, points, changeColor(changeHighlights.wireChanges[wire.id])) }
            changeHighlights.removedWires.forEach { wire ->
                val path = DiagramGeometry.path(wire)
                if (path.size >= 2) drawWire(g, wire, path, changeColor(DiagramChangeKind.Removed), true)
            }
            diagram.nodes.forEach { drawNode(g, it, changeColor(changeHighlights.nodeChanges[it.id])) }
            changeHighlights.removedNodes.forEach { drawNode(g, it, changeColor(DiagramChangeKind.Removed), true) }
            drawSelection(g)
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
            g.stroke = BasicStroke(1.2f)
            // The primary selection is above every other diagram element and its handles.
            lastSelectedNode?.let { node ->
                drawNode(g, node, changeColor(changeHighlights.nodeChanges[node.id]))
                drawNodeSelection(g, node)
            }
            selectedWire?.let { wire ->
                val points = wirePath(wire)
                if (points.size >= 2) {
                    drawWire(g, wire, points, changeColor(changeHighlights.wireChanges[wire.id]))
                    if (wire.showsCondition) drawLabel(g, wire, points, changeColor(changeHighlights.wireChanges[wire.id]))
                    drawWireSelection(g, wire)
                }
            }
            ghostNode?.let { node ->
                val ghostGraphics = g.create() as Graphics2D
                try {
                    ghostGraphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
                    ghostGraphics.stroke = BasicStroke(1.2f)
                    drawNode(ghostGraphics, node)
                } finally {
                    ghostGraphics.dispose()
                }
            }
            wireCreationPreview?.let { wire ->
                val preview = g.create() as Graphics2D
                try {
                    preview.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
                    preview.stroke = BasicStroke(1.2f)
                    drawWire(preview, wire, DiagramGeometry.path(wire))
                } finally { preview.dispose() }
            }
            if (alignmentGuides.isNotEmpty()) {
                val guides = g.create() as Graphics2D
                try {
                    val clip = Area(guides.clip ?: Rectangle2D.Double(-origin.x - pan.x, -origin.y - pan.y, width.toDouble(), height.toDouble()))
                    miniMapProjection?.frame?.let { frame -> clip.subtract(Area(Rectangle2D.Double(
                        (frame.x - origin.x - pan.x) / zoom, (frame.y - origin.y - pan.y) / zoom,
                        frame.width / zoom, frame.height / zoom))) }
                    guides.clip = clip
                    guides.color = JBColor(Color(215, 65, 135), Color(240, 105, 175))
                    guides.stroke = BasicStroke(1f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10f, floatArrayOf(4f, 3f), 0f)
                    alignmentGuides.forEach { drawAlignmentGuide(guides, it) }
                } finally { guides.dispose() }
            }
            g.transform = screenTransform
            g.color = wireColor
            val visible = visibleRect
            g.drawString("x: ${pan.x.roundToInt()}, y: ${pan.y.roundToInt()}", visible.x + 8, visible.y + g.fontMetrics.ascent + 8)
            var propertyY = visible.y + g.fontMetrics.ascent + 8 + g.fontMetrics.height
            diagramPropertyLines.forEach { line ->
                g.drawString(line, visible.x + 8, propertyY)
                propertyY += g.fontMetrics.height
            }
            drawMiniMap(g)
        } finally { g.dispose() }
    }

    private fun drawAlignmentGuide(g: Graphics2D, guide: AlignmentGuide) {
        val distance = guide.distance
        if (distance == null) {
            g.stroke = BasicStroke(1f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10f, floatArrayOf(4f, 3f), 0f)
            g.draw(Line2D.Double(guide.from.x, guide.from.y, guide.to.x, guide.to.y))
            return
        }
        g.stroke = BasicStroke(1f)
        val horizontal = guide.from.y == guide.to.y
        val direction = if (horizontal) DiagramPoint(1.0, 0.0) else DiagramPoint(0.0, 1.0)
        val cross = if (horizontal) DiagramPoint(0.0, 1.0) else DiagramPoint(1.0, 0.0)
        fun line(a: DiagramPoint, b: DiagramPoint) = g.draw(Line2D.Double(a.x, a.y, b.x, b.y))
        line(guide.from, guide.to)
        val arrow = minOf(4.0, distance / 3)
        for ((point, sign) in listOf(guide.from to 1, guide.to to -1)) {
            line(point + DiagramPoint(cross.x * -4, cross.y * -4), point + DiagramPoint(cross.x * 4, cross.y * 4))
            if (arrow > 0) {
                val tip = point + DiagramPoint(direction.x * arrow * sign, direction.y * arrow * sign)
                line(point, tip + DiagramPoint(cross.x * -3, cross.y * -3))
                line(point, tip + DiagramPoint(cross.x * 3, cross.y * 3))
            }
        }
        val text = String.format(Locale.ROOT, "%.1f", distance).removeSuffix(".0") + " px"
        val metrics = g.fontMetrics
        val middle = DiagramPoint((guide.from.x + guide.to.x) / 2, (guide.from.y + guide.to.y) / 2)
        val x = if (horizontal) middle.x - metrics.stringWidth(text) / 2.0 else middle.x + 7
        val y = if (horizontal) middle.y - 6 else middle.y + metrics.ascent / 2.0
        val color = g.color
        g.color = background
        g.fill(Rectangle2D.Double(x - 2, y - metrics.ascent, metrics.stringWidth(text) + 4.0, metrics.height.toDouble()))
        g.color = color
        g.drawString(text, x.toFloat(), y.toFloat())
    }

    private fun drawMiniMap(graphics: Graphics2D) {
        val projection = miniMapProjection ?: return
        val g = graphics.create() as Graphics2D
        try {
            val frame = projection.frame
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
            g.composite = AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.78f)
            g.color = background
            g.fillRoundRect(frame.x, frame.y, frame.width, frame.height, 8, 8)
            g.color = wireColor
            g.stroke = BasicStroke(1f)
            g.drawRoundRect(frame.x, frame.y, frame.width, frame.height, 8, 8)
            val originalClip = g.clip
            g.clip(projection.contentArea)
            val modelGraphics = g.create() as Graphics2D
            try {
                modelGraphics.translate(projection.offsetX, projection.offsetY)
                modelGraphics.scale(projection.scale, projection.scale)
                modelGraphics.stroke = BasicStroke((1.0 / projection.scale).toFloat())
                paths.forEach { (wire, path) -> drawWire(modelGraphics, wire, path, changeColor(changeHighlights.wireChanges[wire.id])) }
                paths.filter { it.first.showsCondition }.forEach { (wire, path) -> drawLabel(modelGraphics, wire, path, changeColor(changeHighlights.wireChanges[wire.id])) }
                changeHighlights.removedWires.forEach { wire ->
                    val path = DiagramGeometry.path(wire)
                    if (path.size >= 2) drawWire(modelGraphics, wire, path, changeColor(DiagramChangeKind.Removed), true)
                }
                diagram.nodes.forEach { drawNode(modelGraphics, it, changeColor(changeHighlights.nodeChanges[it.id])) }
                changeHighlights.removedNodes.forEach { drawNode(modelGraphics, it, changeColor(DiagramChangeKind.Removed), true) }
            } finally {
                modelGraphics.dispose()
            }
            g.clip = originalClip
            g.color = JBColor.BLUE
            g.stroke = BasicStroke(1.2f)
            g.draw(projection.viewport)
        } finally {
            g.dispose()
        }
    }

    private fun drawWire(g: Graphics2D, wire: DiagramWire, points: List<DiagramPoint>, color: Color? = null, ghost: Boolean = false) {
        val oldStroke = g.stroke
        val path = Path2D.Double().apply {
            moveTo(points.first().x, points.first().y)
            points.drop(1).forEach { lineTo(it.x, it.y) }
        }
        g.color = color ?: wireColor
        if (color != null) g.stroke = if (ghost) BasicStroke(2.8f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_ROUND, 10f, floatArrayOf(6f, 4f), 0f) else BasicStroke(2.8f)
        g.draw(path)
        val end = points.last()
        val previous = points.dropLast(1).lastOrNull { it != end }
        if (previous != null) {
            val angle = atan2(end.y - previous.y, end.x - previous.x)
            val arrow = Path2D.Double().apply {
                moveTo(end.x, end.y)
                lineTo(end.x - 8 * cos(angle - 0.45), end.y - 8 * sin(angle - 0.45))
                lineTo(end.x - 8 * cos(angle + 0.45), end.y - 8 * sin(angle + 0.45))
                closePath()
            }
            g.fill(arrow)
        }
        g.stroke = oldStroke
    }

    private fun drawLabel(g: Graphics2D, wire: DiagramWire, points: List<DiagramPoint>, color: Color? = null) {
        val bounds = labelBounds(points, wire.condition) ?: return
        g.color = background
        g.fill(bounds)
        g.color = color ?: wireColor
        val baseline = bounds.y + (bounds.height - 1) / 2 + (g.fontMetrics.ascent - g.fontMetrics.descent) / 2.0 - 2
        g.drawString(wire.condition, bounds.x.toFloat(), baseline.toFloat())
    }

    private fun drawNode(g: Graphics2D, node: DiagramNode, outlineColor: Color? = null, outlineOnly: Boolean = false) {
        val oldStroke = g.stroke
        val x = node.location.x
        val y = node.location.y
        when (node.kind) {
            DiagramNodeKind.Activity -> {
                val shape = RoundRectangle2D.Double(x, y, node.width, 30.0, 16.0, 16.0)
                if (!outlineOnly) {
                    g.color = activityColor(node) ?: Color.WHITE
                    g.fill(shape)
                }
                g.color = outlineColor ?: foreground
                if (outlineColor != null) g.stroke = BasicStroke(2.8f)
                g.draw(shape)
                val oldClip = g.clip
                g.clip(shape)
                val textX = x + (node.width - g.fontMetrics.stringWidth(node.name)) / 2
                g.color = activityForeground(node) ?: wireColor
                g.drawString(node.name, textX.toFloat(), (y + (30 - g.fontMetrics.height) / 2 + g.fontMetrics.ascent).toFloat())
                g.clip = oldClip
            }
            DiagramNodeKind.InitialState -> {
                val shape = Ellipse2D.Double(x, y, 20.0, 20.0)
                if (!outlineOnly) { g.color = Color.BLACK; g.fill(shape) }
                g.color = outlineColor ?: Color.BLACK
                if (outlineColor != null) g.stroke = BasicStroke(2.8f)
                g.draw(shape)
            }
            DiagramNodeKind.FinalState -> {
                g.color = outlineColor ?: Color.BLACK
                if (outlineColor != null) g.stroke = BasicStroke(2.8f)
                g.draw(Ellipse2D.Double(x, y, 20.0, 20.0))
                if (!outlineOnly) { g.color = Color.BLACK; g.fill(Ellipse2D.Double(x + 3, y + 3, 15.0, 15.0)) }
            }
        }
        g.stroke = oldStroke
    }

    private fun changeColor(kind: DiagramChangeKind?): Color? = when (kind) {
        DiagramChangeKind.Added -> Color(35, 145, 65)
        DiagramChangeKind.Removed -> Color(205, 45, 45)
        DiagramChangeKind.Modified -> Color(225, 130, 20)
        null -> null
    }

    private fun drawSelection(g: Graphics2D) {
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF)
        g.color = wireColor
        g.stroke = BasicStroke(1f)
        selection.forEach { node -> drawNodeSelection(g, node) }
        selectedWire?.let { drawWireSelection(g, it) }
        selectionBox?.let { box ->
            g.color = Color(90, 180, 255, 55)
            g.fill(box)
            g.color = wireColor
            g.stroke = BasicStroke(1f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10f, floatArrayOf(4f, 3f), 0f)
            g.draw(box)
        }
    }

    private fun drawNodeSelection(g: Graphics2D, node: DiagramNode) {
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF)
            g.stroke = BasicStroke(1f)
            val x = node.location.x
            val y = node.location.y
            val right = x + node.width
            val bottom = y + node.height
            g.color = wireColor
            g.draw(Rectangle2D.Double(x, y, node.width, node.height))
            val handles = listOf(DiagramPoint(x, y), DiagramPoint(node.center.x, y), DiagramPoint(right, y),
                DiagramPoint(right, node.center.y), DiagramPoint(right, bottom), DiagramPoint(node.center.x, bottom),
                DiagramPoint(x, bottom), DiagramPoint(x, node.center.y))
            handles.forEach { center ->
                drawHandle(g, center, 6, if (node == lastSelectedNode) 4 else 0, node != lastSelectedNode)
            }
    }

    private fun drawWireSelection(g: Graphics2D, wire: DiagramWire) {
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF)
            val points = wirePath(wire)
            if (points.size >= 2) {
                points.forEach { drawHandle(g, it, 6, 4) }
                points.zipWithNext().forEach { (a, b) ->
                    drawHandle(g, DiagramPoint((a.x + b.x) / 2, (a.y + b.y) / 2), 4, 2)
                }
            }
    }

    private fun drawHandle(g: Graphics2D, center: DiagramPoint, size: Int, inner: Int, border: Boolean = false) {
        val left = center.x.roundToInt() - size / 2
        val top = center.y.roundToInt() - size / 2
        g.color = Color.WHITE
        g.fillRect(left, top, size, size)
        g.color = Color.BLACK
        if (border) g.drawRect(left, top, size - 1, size - 1)
        if (inner > 0) g.fillRect(left + (size - inner) / 2, top + (size - inner) / 2, inner, inner)
    }
}
