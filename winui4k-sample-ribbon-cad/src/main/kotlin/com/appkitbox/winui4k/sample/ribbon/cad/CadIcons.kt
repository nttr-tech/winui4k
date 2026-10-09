package com.appkitbox.winui4k.sample.ribbon.cad

import com.appkitbox.winui4k.ribbon.RibbonIcon

/**
 * Line-art icons for the CAD commands (ported from CadIcons in the RibbonSpace demo. MIT License, THIRD-PARTY-NOTICES.md).
 *
 * Layered paths on a 32 x 32 design grid (`[viewbox=32;stroke=1.8;color=#E8A33D]M...` separated by `|`).
 * Layers without a color follow the theme's icon color; fixed colors are used for small accents (orange = result,
 * blue = grip, green = add / on, red = delete). Use [icon] to turn one into a [RibbonIcon].
 */
@Suppress("LargeClass") // A table that gathers the demo CAD icon definitions in one place
object CadIcons {
    /** Turns a layered path string into an icon whose path is in 32 units. */
    @JvmStatic
    fun icon(value: String): RibbonIcon = RibbonIcon.path(value, 32.0)

    // ---- Home › Draw

    const val LINE =
        "[viewbox=32;stroke=1.8]M6,26 L26,6" +
            "|[color=#3DA9F5]M4.5,24.5 L7.5,24.5 L7.5,27.5 L4.5,27.5 Z M24.5,4.5 L27.5,4.5 L27.5,7.5 L24.5,7.5 Z"

    const val POLYLINE =
        "[viewbox=32;stroke=1.8]M4,24 L11,9 L20,21 L28,8" +
            "|[color=#3DA9F5]M2.5,22.5 L5.5,22.5 L5.5,25.5 L2.5,25.5 Z M9.5,7.5 L12.5,7.5 L12.5,10.5 L9.5,10.5 Z M18.5,19.5 L21.5,19.5 L21.5,22.5 L18.5,22.5 Z M26.5,6.5 L29.5,6.5 L29.5,9.5 L26.5,9.5 Z"

    const val CIRCLE_CENTER_RADIUS =
        "[viewbox=32;stroke=1.8]M5,16 A11,11 0 1 1 27,16 A11,11 0 1 1 5,16 Z" +
            "|[stroke=1.3;color=#E8A33D]M16,16 L23.78,8.22" +
            "|[color=#3DA9F5]M14.5,14.5 L17.5,14.5 L17.5,17.5 L14.5,17.5 Z M22.28,6.72 L25.28,6.72 L25.28,9.72 L22.28,9.72 Z"

    const val CIRCLE_TWO_POINT =
        "[viewbox=32;stroke=1.8]M5,16 A11,11 0 1 1 27,16 A11,11 0 1 1 5,16 Z" +
            "|[stroke=1.3;color=#E8A33D]M5,16 H27" +
            "|[color=#3DA9F5]M3.5,14.5 L6.5,14.5 L6.5,17.5 L3.5,17.5 Z M25.5,14.5 L28.5,14.5 L28.5,17.5 L25.5,17.5 Z"

    const val CIRCLE_THREE_POINT =
        "[viewbox=32;stroke=1.8]M5,16 A11,11 0 1 1 27,16 A11,11 0 1 1 5,16 Z" +
            "|[color=#3DA9F5]M4.97,9 L7.97,9 L7.97,12 L4.97,12 Z M24.03,9 L27.03,9 L27.03,12 L24.03,12 Z M14.5,25.5 L17.5,25.5 L17.5,28.5 L14.5,28.5 Z"

    const val ARC =
        "[viewbox=32;stroke=1.8]M4.25,23.28 A12.5,12.5 0 1 1 27.75,23.28" +
            "|[color=#3DA9F5]M2.75,21.78 L5.75,21.78 L5.75,24.78 L2.75,24.78 Z M14.5,5 L17.5,5 L17.5,8 L14.5,8 Z M26.25,21.78 L29.25,21.78 L29.25,24.78 L26.25,24.78 Z"

    const val ARC_START_CENTER_END =
        "[viewbox=32;stroke=1.3;color=#E8A33D]M26,25 L7,25 L7,6" +
            "|[stroke=1.8]M26,25 A19,19 0 0 0 7,6" +
            "|[color=#3DA9F5]M24.5,23.5 L27.5,23.5 L27.5,26.5 L24.5,26.5 Z M5.5,23.5 L8.5,23.5 L8.5,26.5 L5.5,26.5 Z M5.5,4.5 L8.5,4.5 L8.5,7.5 L5.5,7.5 Z"

    const val RECTANGLE =
        "[viewbox=32;stroke=1.8]M5,8 L27,8 L27,24 L5,24 Z" +
            "|[color=#3DA9F5]M3.5,22.5 L6.5,22.5 L6.5,25.5 L3.5,25.5 Z M25.5,6.5 L28.5,6.5 L28.5,9.5 L25.5,9.5 Z"

    const val POLYGON =
        "[viewbox=32;stroke=1.8]M16,3.5 L5.17,9.75 L5.17,22.25 L16,28.5 L26.83,22.25 L26.83,9.75 Z" +
            "|[stroke=1.3;color=#E8A33D]M16,16 L26.83,9.75" +
            "|[color=#3DA9F5]M14.5,14.5 L17.5,14.5 L17.5,17.5 L14.5,17.5 Z"

    const val ELLIPSE =
        "[viewbox=32;stroke=1.3;color=#E8A33D]M3.5,16 H28.5" +
            "|[stroke=1.8]M3.5,16 A12.5,7.5 0 1 1 28.5,16 A12.5,7.5 0 1 1 3.5,16 Z" +
            "|[color=#3DA9F5]M14.5,14.5 L17.5,14.5 L17.5,17.5 L14.5,17.5 Z M27,14.5 L30,14.5 L30,17.5 L27,17.5 Z M14.5,7 L17.5,7 L17.5,10 L14.5,10 Z"

    const val SPLINE =
        "[viewbox=32;stroke=1.8]M3,23 C9,5 14,6 16.5,16 C19,26 24,27 29,9" +
            "|[color=#3DA9F5]M1.5,21.5 L4.5,21.5 L4.5,24.5 L1.5,24.5 Z M15,14.5 L18,14.5 L18,17.5 L15,17.5 Z M27.5,7.5 L30.5,7.5 L30.5,10.5 L27.5,10.5 Z"

    const val HATCH =
        "[viewbox=32;stroke=1.3;color=#E8A33D]M5,9.4 L9.4,5 M5,13.8 L13.8,5 M5,18.2 L18.2,5 M5,22.6 L22.6,5 M5,27 L27,5 M9.4,27 L27,9.4 M13.8,27 L27,13.8 M18.2,27 L27,18.2 M22.6,27 L27,22.6" +
            "|[stroke=1.8]M5,5 L27,5 L27,27 L5,27 Z"

    const val GRADIENT =
        "[viewbox=32;color=#E8A33D]M5,5 L10.5,5 L10.5,27 L5,27 Z" +
            "|[color=#E8A33D;opacity=0.7]M10.5,5 L16,5 L16,27 L10.5,27 Z" +
            "|[color=#E8A33D;opacity=0.42]M16,5 L21.5,5 L21.5,27 L16,27 Z" +
            "|[color=#E8A33D;opacity=0.18]M21.5,5 L27,5 L27,27 L21.5,27 Z" +
            "|[stroke=1.8]M5,5 L27,5 L27,27 L5,27 Z"

    const val BOUNDARY =
        "[viewbox=32;stroke=1.3;opacity=0.55]M10,3 V29 M22,3 V29 M3,10 H29 M3,22 H29" +
            "|[color=#E8A33D;opacity=0.3]M10,10 L22,10 L22,22 L10,22 Z" +
            "|[stroke=1.8;color=#E8A33D]M10,10 L22,10 L22,22 L10,22 Z"

    const val POINT =
        "[viewbox=32;stroke=1.3]M16,3 V29 M3,16 H29" +
            "|[stroke=1.8]M9,16 A7,7 0 1 1 23,16 A7,7 0 1 1 9,16 Z" +
            "|[color=#E8A33D]M13.6,16 A2.4,2.4 0 1 1 18.4,16 A2.4,2.4 0 1 1 13.6,16 Z"

    const val REGION =
        "[viewbox=32;opacity=0.25]M6,24 V12 Q6,6 12,6 H21 L27,13 L23,26 Z" +
            "|[stroke=1.8]M6,24 V12 Q6,6 12,6 H21 L27,13 L23,26 Z" +
            "|[color=#3DA9F5]M4.5,22.5 L7.5,22.5 L7.5,25.5 L4.5,25.5 Z M19.5,4.5 L22.5,4.5 L22.5,7.5 L19.5,7.5 Z M25.5,11.5 L28.5,11.5 L28.5,14.5 L25.5,14.5 Z M21.5,24.5 L24.5,24.5 L24.5,27.5 L21.5,27.5 Z"

    const val REVISION_CLOUD =
        "[viewbox=32;stroke=1.6]M5.5,9 A2.94,2.94 0 0 1 10.75,9 A2.94,2.94 0 0 1 16,9 A2.94,2.94 0 0 1 21.25,9 A2.94,2.94 0 0 1 26.5,9 A2.8,2.8 0 0 1 26.5,14 A2.8,2.8 0 0 1 26.5,19 A2.8,2.8 0 0 1 26.5,24 A2.94,2.94 0 0 1 21.25,24 A2.94,2.94 0 0 1 16,24 A2.94,2.94 0 0 1 10.75,24 A2.94,2.94 0 0 1 5.5,24 A2.8,2.8 0 0 1 5.5,19 A2.8,2.8 0 0 1 5.5,14 A2.8,2.8 0 0 1 5.5,9 Z" +
            "|[stroke=1.3;color=#E05A5A]M10,21 L14,13 L18,21 Z"

    const val DONUT = "[viewbox=32]M4.5,16 A11.5,11.5 0 1 1 27.5,16 A11.5,11.5 0 1 1 4.5,16 Z M10.5,16 A5.5,5.5 0 1 1 21.5,16 A5.5,5.5 0 1 1 10.5,16 Z"

    const val CONSTRUCTION_LINE =
        "[viewbox=32;stroke=1.8]M3,27 L29,5" +
            "|[stroke=1.3;color=#E8A33D]M2,13 L30,19" +
            "|[color=#3DA9F5]M14.5,14.5 L17.5,14.5 L17.5,17.5 L14.5,17.5 Z"

    const val RAY =
        "[viewbox=32;stroke=1.8]M6,25 L28,8" +
            "|[stroke=1.8]M24.41,7.73 L28,8 L27.36,11.54" +
            "|[stroke=1.3;color=#E8A33D]M6,25 L28,21" +
            "|[stroke=1.3;color=#E8A33D]M25.45,19.42 L28,21 L26.17,23.37" +
            "|[color=#3DA9F5]M4.5,23.5 L7.5,23.5 L7.5,26.5 L4.5,26.5 Z"

    const val MULTI_LINE =
        "[viewbox=32;stroke=1.8]M4,9 L23,9 L23,28" +
            "|[stroke=1.8]M4,16 L16,16 L16,28" +
            "|[stroke=1.1;color=#E8A33D]M4,12.5 L6.2,12.5 M8,12.5 L10.2,12.5 M12,12.5 L14.2,12.5 M16,12.5 L18.2,12.5 M19.5,12.5 L19.5,14.7 M19.5,16.5 L19.5,18.7 M19.5,20.5 L19.5,22.7 M19.5,24.5 L19.5,26.7"

    // ---- Home › Modify

    const val MOVE =
        "[viewbox=32;stroke=1.8]M16,3 V29 M3,16 H29" +
            "|[stroke=1.8]M13.46,5.82 L16,3 L18.54,5.82 M18.54,26.18 L16,29 L13.46,26.18 M5.82,18.54 L3,16 L5.82,13.46 M26.18,13.46 L29,16 L26.18,18.54" +
            "|[color=#E8A33D]M13.5,13.5 L18.5,13.5 L18.5,18.5 L13.5,18.5 Z"

    const val COPY =
        "[viewbox=32;stroke=1.8]M4,4 L16,4 L16,16 L4,16 Z" +
            "|[stroke=1.8;color=#E8A33D]M16,16 L28,16 L28,28 L16,28 Z" +
            "|[color=#3DA9F5]M14.5,14.5 L17.5,14.5 L17.5,17.5 L14.5,17.5 Z"

    const val STRETCH =
        "[viewbox=32;stroke=1.8]M20,8 H4 V24 H20" +
            "|[stroke=1.3;opacity=0.45]M20,8 V24" +
            "|[stroke=1.8;color=#E8A33D]M20,8 H28 V24 H20" +
            "|[color=#3DA9F5]M26.5,6.5 L29.5,6.5 L29.5,9.5 L26.5,9.5 Z M26.5,22.5 L29.5,22.5 L29.5,25.5 L26.5,25.5 Z"

    const val ROTATE =
        "[viewbox=32;stroke=1.8;opacity=0.5]M8,24 H27" +
            "|[stroke=1.8;color=#E8A33D]M8,24 L13.87,5.93" +
            "|[stroke=1.8]M20.43,22.69 A12.5,12.5 0 0 0 13.87,12.96" +
            "|[stroke=1.8]M15.1,16.35 L13.87,12.96 L17.36,12.09" +
            "|[color=#3DA9F5]M6.5,22.5 L9.5,22.5 L9.5,25.5 L6.5,25.5 Z"

    const val MIRROR =
        "[viewbox=32;stroke=1.3;color=#3DA9F5]M16,3 L16,6.2 M16,8.4 L16,11.6 M16,13.8 L16,17 M16,19.2 L16,22.4 M16,24.6 L16,27.8" +
            "|[stroke=1.8]M3,26 H12.5 V8 Z" +
            "|[stroke=1.8;color=#E8A33D]M29,26 H19.5 V8 Z"

    const val SCALE =
        "[viewbox=32;stroke=1.8;color=#E8A33D]M5,5 L27,5 L27,27 L5,27 Z" +
            "|[stroke=1.8]M5,17 L15,17 L15,27 L5,27 Z" +
            "|[stroke=1.8]M13,19 L23,9" +
            "|[stroke=1.8]M19.4,9.19 L23,9 L22.81,12.6" +
            "|[color=#3DA9F5]M3.5,25.5 L6.5,25.5 L6.5,28.5 L3.5,28.5 Z"

    const val TRIM =
        "[viewbox=32;stroke=1.8]M11,4 V28 M21,4 V28" +
            "|[stroke=1.8]M3,16 H11 M21,16 H29" +
            "|[stroke=1.8;color=#E05A5A]M12.6,16 L15,16 M17.2,16 L19.6,16"

    const val EXTEND =
        "[viewbox=32;stroke=1.8]M27,4 V28" +
            "|[stroke=1.8]M4,11 H14 M4,21 H14" +
            "|[stroke=1.8;color=#E8A33D]M14,11 H26 M14,21 H26" +
            "|[stroke=1.8;color=#E8A33D]M23.77,8.99 L26,11 L23.77,13.01 M23.77,18.99 L26,21 L23.77,23.01"

    const val FILLET =
        "[viewbox=32;stroke=1.3;opacity=0.4]M6,16 V6 H16" +
            "|[stroke=1.8]M6,28 V16 M16,6 H28" +
            "|[stroke=1.8;color=#E8A33D]M6,16 A10,10 0 0 1 16,6" +
            "|[color=#3DA9F5]M4.5,14.5 L7.5,14.5 L7.5,17.5 L4.5,17.5 Z M14.5,4.5 L17.5,4.5 L17.5,7.5 L14.5,7.5 Z"

    const val CHAMFER =
        "[viewbox=32;stroke=1.3;opacity=0.4]M6,16 V6 H16" +
            "|[stroke=1.8]M6,28 V16 M16,6 H28" +
            "|[stroke=1.8;color=#E8A33D]M6,16 L16,6" +
            "|[color=#3DA9F5]M4.5,14.5 L7.5,14.5 L7.5,17.5 L4.5,17.5 Z M14.5,4.5 L17.5,4.5 L17.5,7.5 L14.5,7.5 Z"

    const val BLEND =
        "[viewbox=32;stroke=1.8]M3,25 H12 M20,7 H29" +
            "|[stroke=1.8;color=#E8A33D]M12,25 C19,25 13,7 20,7" +
            "|[color=#3DA9F5]M10.5,23.5 L13.5,23.5 L13.5,26.5 L10.5,26.5 Z M18.5,5.5 L21.5,5.5 L21.5,8.5 L18.5,8.5 Z"

    const val ARRAY_RECTANGULAR =
        "[viewbox=32;color=#E8A33D]M4,4 L10,4 L10,10 L4,10 Z" +
            "|[stroke=1.6]M4,4 L10,4 L10,10 L4,10 Z M13,4 L19,4 L19,10 L13,10 Z M22,4 L28,4 L28,10 L22,10 Z M4,13 L10,13 L10,19 L4,19 Z M13,13 L19,13 L19,19 L13,19 Z M22,13 L28,13 L28,19 L22,19 Z M4,22 L10,22 L10,28 L4,28 Z M13,22 L19,22 L19,28 L13,28 Z M22,22 L28,22 L28,28 L22,28 Z"

    const val ARRAY_POLAR =
        "[viewbox=32;color=#E8A33D]M13,5.5 A3,3 0 1 1 19,5.5 A3,3 0 1 1 13,5.5 Z" +
            "|[stroke=1.6]M13,5.5 A3,3 0 1 1 19,5.5 A3,3 0 1 1 13,5.5 Z M3.91,10.75 A3,3 0 1 1 9.91,10.75 A3,3 0 1 1 3.91,10.75 Z M3.91,21.25 A3,3 0 1 1 9.91,21.25 A3,3 0 1 1 3.91,21.25 Z M13,26.5 A3,3 0 1 1 19,26.5 A3,3 0 1 1 13,26.5 Z M22.09,21.25 A3,3 0 1 1 28.09,21.25 A3,3 0 1 1 22.09,21.25 Z M22.09,10.75 A3,3 0 1 1 28.09,10.75 A3,3 0 1 1 22.09,10.75 Z" +
            "|[color=#3DA9F5]M14.7,14.7 L17.3,14.7 L17.3,17.3 L14.7,17.3 Z"

    const val ARRAY_PATH =
        "[viewbox=32;stroke=1.3;opacity=0.6]M4,28 C8,12 18,5 29,5" +
            "|[color=#E8A33D]M2.47,21.73 L7.67,21.73 L7.67,26.93 L2.47,26.93 Z" +
            "|[stroke=1.5]M2.47,21.73 L7.67,21.73 L7.67,26.93 L2.47,26.93 Z M8.28,10.95 L13.48,10.95 L13.48,16.15 L8.28,16.15 Z M15.72,4.91 L20.92,4.91 L20.92,10.11 L15.72,10.11 Z M24.11,2.5 L29.31,2.5 L29.31,7.7 L24.11,7.7 Z"

    const val OFFSET =
        "[viewbox=32;stroke=1.8]M5,28 V14 A9,9 0 0 1 14,5 H28" +
            "|[stroke=1.8;color=#E8A33D]M11.5,28 V14 A2.5,2.5 0 0 1 14,11.5 H28"

    const val ERASE =
        "[viewbox=32;color=#E05A5A]M4,19 L8.4,14.6 L18.4,24.6 L14,29 Z" +
            "|[stroke=1.8]M4,19 L15,8 L25,18 L14,29 Z M8.4,14.6 L18.4,24.6" +
            "|[stroke=1.3]M18,29 H29"

    const val EXPLODE =
        "[viewbox=32;stroke=1.8]M11,5 H21 M11,27 H21 M5,11 V21 M27,11 V21" +
            "|[stroke=1.3;color=#E8A33D]M13.5,13.5 L10.5,10.5 M18.5,13.5 L21.5,10.5 M13.5,18.5 L10.5,21.5 M18.5,18.5 L21.5,21.5" +
            "|[color=#E8A33D]M14.5,14.5 L17.5,14.5 L17.5,17.5 L14.5,17.5 Z"

    const val BREAK =
        "[viewbox=32;stroke=1.8]M4,26 L12.64,18.8 M19.36,13.2 L28,6" +
            "|[stroke=1.3;color=#E05A5A]M14.08,17.6 L15.31,16.58 M16.54,15.55 L17.77,14.53" +
            "|[color=#3DA9F5]M11.14,17.3 L14.14,17.3 L14.14,20.3 L11.14,20.3 Z M17.86,11.7 L20.86,11.7 L20.86,14.7 L17.86,14.7 Z"

    const val BREAK_AT_POINT =
        "[viewbox=32;stroke=1.8]M4,26 L28,6" +
            "|[stroke=1.3;color=#E8A33D]M12.5,10.5 L19.5,21.5" +
            "|[color=#3DA9F5]M14.5,14.5 L17.5,14.5 L17.5,17.5 L14.5,17.5 Z"

    const val JOIN =
        "[viewbox=32;stroke=1.8]M3,22 H12 M20,22 H29" +
            "|[stroke=1.8;color=#E8A33D]M12,22 H20" +
            "|[stroke=1.3]M5,11 H12 M27,11 H20" +
            "|[stroke=1.3]M10.27,8.99 L12.5,11 L10.27,13.01 M21.73,13.01 L19.5,11 L21.73,8.99" +
            "|[color=#3DA9F5]M10.5,20.5 L13.5,20.5 L13.5,23.5 L10.5,23.5 Z M18.5,20.5 L21.5,20.5 L21.5,23.5 L18.5,23.5 Z"

    const val LENGTHEN =
        "[viewbox=32;stroke=1.8]M4,13 H18" +
            "|[stroke=1.8;color=#E8A33D]M18,13 H28" +
            "|[stroke=1.3]M4,20 V28 M28,20 V28" +
            "|[stroke=1.3]M4,24 H28" +
            "|[stroke=1.3]M5.93,25.74 L4,24 L5.93,22.26 M26.07,22.26 L28,24 L26.07,25.74" +
            "|[color=#3DA9F5]M26.5,11.5 L29.5,11.5 L29.5,14.5 L26.5,14.5 Z"

    const val ALIGN =
        "[viewbox=32;stroke=1.8;color=#E8A33D]M5.56,7.87 L24.79,2.36 L26.44,8.13 L7.21,13.64 Z" +
            "|[stroke=1.8]M4,21 L28,21 L28,27 L4,27 Z" +
            "|[stroke=1.6]M16,13.5 V19" +
            "|[stroke=1.6]M18.01,16.77 L16,19 L13.99,16.77" +
            "|[color=#3DA9F5]M2.5,19.5 L5.5,19.5 L5.5,22.5 L2.5,22.5 Z M26.5,19.5 L29.5,19.5 L29.5,22.5 L26.5,22.5 Z"

    const val REVERSE =
        "[viewbox=32;stroke=1.8]M4,10 H27" +
            "|[stroke=1.8]M24.03,7.32 L27,10 L24.03,12.68" +
            "|[stroke=1.8;color=#E8A33D]M28,22 H5" +
            "|[stroke=1.8;color=#E8A33D]M7.97,24.68 L5,22 L7.97,19.32"

    const val EDIT_POLYLINE =
        "[viewbox=32;stroke=1.8]M3,22 L8,6 L16,16 L24,4" +
            "|[color=#3DA9F5]M1.5,20.5 L4.5,20.5 L4.5,23.5 L1.5,23.5 Z M6.5,4.5 L9.5,4.5 L9.5,7.5 L6.5,7.5 Z M14.5,14.5 L17.5,14.5 L17.5,17.5 L14.5,17.5 Z M22.5,2.5 L25.5,2.5 L25.5,5.5 L22.5,5.5 Z" +
            "|[stroke=1.5;color=#E8A33D]M15,29 L20.13,27.41 L27.37,20.16 L23.84,16.63 L16.59,23.87 Z M20.13,27.41 L16.59,23.87"

    // ---- Home › Annotation

    const val MULTILINE_TEXT =
        "[viewbox=32;stroke=2]M3,21 L9.5,4 L16,21 M5.47,14.54 L13.53,14.54" +
            "|[stroke=1.6]M19,6 H29 M19,11 H29 M19,16 H29 M19,21 H26 M3,27 H21" +
            "|[stroke=1.6;color=#E8A33D]M24.5,23.5 V30"

    const val SINGLE_LINE_TEXT =
        "[viewbox=32;stroke=2.2]M4,26 L12.5,5 L21,26 M7.23,18.02 L17.77,18.02" +
            "|[stroke=1.6;color=#E8A33D]M26,7 V25 M23.5,6 H28.5 M23.5,26 H28.5"

    const val DIMENSION_LINEAR =
        "[viewbox=32;stroke=1.8]M5,27 H27" +
            "|[stroke=1.3]M5,24 V8 M27,24 V8" +
            "|[stroke=1.3]M5,14 H27" +
            "|[color=#E8A33D]M5,14 L9.2,12.1 L9.2,15.9 Z M27,14 L22.8,15.9 L22.8,12.1 Z" +
            "|[stroke=1.6]M12,9 H20"

    const val DIMENSION_ALIGNED =
        "[viewbox=32;stroke=1.3]M11.82,27.47 L3.51,15.59 M28.21,16 L19.89,4.12" +
            "|[stroke=1.3]M6.37,19.69 L22.76,8.22" +
            "|[color=#E8A33D]M6.37,19.69 L8.73,15.72 L10.9,18.84 Z M22.76,8.22 L20.41,12.18 L18.23,9.07 Z" +
            "|[stroke=1.8]M13.54,29.93 L29.93,18.46" +
            "|[stroke=1.6]M8.83,11.86 L14.57,7.85"

    const val DIMENSION_ANGULAR =
        "[viewbox=32;stroke=1.8]M19.16,8.88 L5,27 L28,27" +
            "|[stroke=1.3;color=#E8A33D]M19.96,25.95 A15,15 0 0 0 15.04,15.85" +
            "|[color=#E8A33D]M20,26.87 L18.2,22.87 L21.8,22.87 Z M14.34,15.26 L18.6,16.31 L16.38,19.14 Z"

    const val DIMENSION_RADIUS =
        "[viewbox=32;stroke=1.8]M4,18 A10,10 0 1 1 24,18 A10,10 0 1 1 4,18 Z" +
            "|[stroke=1.3;color=#E8A33D]M14,18 L24.96,7.04" +
            "|[color=#E8A33D]M21.07,10.93 L19.44,15.24 L16.76,12.56 Z" +
            "|[stroke=1.3;color=#E8A33D]M24.96,7.04 H29" +
            "|[color=#3DA9F5]M12.7,16.7 L15.3,16.7 L15.3,19.3 L12.7,19.3 Z"

    const val DIMENSION_DIAMETER =
        "[viewbox=32;stroke=1.8]M5,16 A11,11 0 1 1 27,16 A11,11 0 1 1 5,16 Z" +
            "|[stroke=1.3;color=#E8A33D]M8.22,23.78 L23.78,8.22" +
            "|[color=#E8A33D]M8.22,23.78 L9.85,19.46 L12.54,22.15 Z M23.78,8.22 L22.15,12.54 L19.46,9.85 Z"

    const val MULTILEADER =
        "[viewbox=32;stroke=1.3]M6,26 L14,13 H17" +
            "|[color=#E8A33D]M5,27.5 L5.71,22.53 L9.11,24.63 Z" +
            "|[stroke=1.6]M20,8 H29 M20,13 H29 M20,18 H26"

    const val TABLE =
        "[viewbox=32;color=#E8A33D;opacity=0.85]M4,6 L28,6 L28,12 L4,12 Z" +
            "|[stroke=1.8]M4,6 L28,6 L28,26 L4,26 Z" +
            "|[stroke=1.3]M4,12 H28 M4,19 H28 M12,12 V26 M20,12 V26"

    const val TEXT_STYLE =
        "[viewbox=32;stroke=2]M3,22 L10,4 L17,22 M5.66,15.16 L14.34,15.16" +
            "|[stroke=1.4;color=#E8A33D]M24.51,18.34 L24.24,16.52 L21.76,16.52 L21.49,18.34 L19.72,19.36 L18,18.69 L16.77,20.83 L18.21,21.98 L18.21,24.02 L16.77,25.17 L18,27.31 L19.72,26.64 L21.49,27.66 L21.76,29.48 L24.24,29.48 L24.51,27.66 L26.28,26.64 L28,27.31 L29.23,25.17 L27.79,24.02 L27.79,21.98 L29.23,20.83 L28,18.69 L26.28,19.36 Z M21,23 A2,2 0 1 1 25,23 A2,2 0 1 1 21,23 Z"

    const val DIMENSION_STYLE =
        "[viewbox=32;stroke=1.3]M4,4 V14 M26,4 V14" +
            "|[stroke=1.3]M4,9 H26" +
            "|[color=#E8A33D]M4,9 L7.6,7.3 L7.6,10.7 Z M26,9 L22.4,10.7 L22.4,7.3 Z" +
            "|[stroke=1.4;color=#E8A33D]M24.51,18.34 L24.24,16.52 L21.76,16.52 L21.49,18.34 L19.72,19.36 L18,18.69 L16.77,20.83 L18.21,21.98 L18.21,24.02 L16.77,25.17 L18,27.31 L19.72,26.64 L21.49,27.66 L21.76,29.48 L24.24,29.48 L24.51,27.66 L26.28,26.64 L28,27.31 L29.23,25.17 L27.79,24.02 L27.79,21.98 L29.23,20.83 L28,18.69 L26.28,19.36 Z M21,23 A2,2 0 1 1 25,23 A2,2 0 1 1 21,23 Z" +
            "|[stroke=1.6]M4,19 H14 M4,24 H12 M4,29 H14"

    // ---- Home › Layers

    const val LAYER_PROPERTIES =
        "[viewbox=32;color=#E8A33D;opacity=0.55]M4,10 L16,5 L28,10 L16,15 Z" +
            "|[stroke=1.8]M4,10 L16,5 L28,10 L16,15 Z" +
            "|[stroke=1.8]M4,16 L16,21 L28,16 M4,22 L16,27 L28,22"

    const val LAYER_ON =
        "[viewbox=32;color=#E8A33D]M12.75,22.5 V19.63 A6.5,6.5 0 1 1 19.25,19.63 V22.5 Z" +
            "|[stroke=1.8]M12.75,22.5 V19.63 A6.5,6.5 0 1 1 19.25,19.63 V22.5 Z" +
            "|[stroke=1.6]M13,25.5 H19 M14.5,28.5 H17.5" +
            "|[stroke=1.6;color=#E8A33D]M24.6,14 L27.2,14 M22.08,7.92 L23.92,6.08 M16,5.4 L16,2.8 M9.92,7.92 L8.08,6.08 M7.4,14 L4.8,14"

    const val LAYER_OFF =
        "[viewbox=32;opacity=0.15]M12.75,22.5 V19.63 A6.5,6.5 0 1 1 19.25,19.63 V22.5 Z" +
            "|[stroke=1.8]M12.75,22.5 V19.63 A6.5,6.5 0 1 1 19.25,19.63 V22.5 Z" +
            "|[stroke=1.6]M13,25.5 H19 M14.5,28.5 H17.5"

    const val LAYER_THAW =
        "[viewbox=32;color=#E8A33D]M10.5,16 A5.5,5.5 0 1 1 21.5,16 A5.5,5.5 0 1 1 10.5,16 Z" +
            "|[stroke=1.6]M10.5,16 A5.5,5.5 0 1 1 21.5,16 A5.5,5.5 0 1 1 10.5,16 Z" +
            "|[stroke=1.8;color=#E8A33D]M24.5,16 L28,16 M22.01,9.99 L24.49,7.51 M16,7.5 L16,4 M9.99,9.99 L7.51,7.51 M7.5,16 L4,16 M9.99,22.01 L7.51,24.49 M16,24.5 L16,28 M22.01,22.01 L24.49,24.49"

    const val LAYER_FREEZE =
        "[viewbox=32;stroke=1.8]M16,3.5 L16,28.5 M26.83,9.75 L5.17,22.25 M5.17,9.75 L26.83,22.25" +
            "|[stroke=1.5;color=#3DA9F5]M23.43,8.77 L22.5,12.25 L25.97,13.18 M13.45,5.95 L16,8.5 L18.55,5.95 M6.03,13.18 L9.5,12.25 L8.57,8.77 M8.57,23.23 L9.5,19.75 L6.03,18.82 M18.55,26.05 L16,23.5 L13.45,26.05 M25.97,18.82 L22.5,19.75 L23.43,23.23"

    const val LAYER_UNLOCK =
        "[viewbox=32;color=#5CB85C]M9,14 H23 A2,2 0 0 1 25,16 V26 A2,2 0 0 1 23,28 H9 A2,2 0 0 1 7,26 V16 A2,2 0 0 1 9,14 Z" +
            "|[stroke=1.8]M9,14 H23 A2,2 0 0 1 25,16 V26 A2,2 0 0 1 23,28 H9 A2,2 0 0 1 7,26 V16 A2,2 0 0 1 9,14 Z" +
            "|[stroke=2]M21,14 V8.5 A5,5 0 0 0 11,8.5 V10" +
            "|[stroke=2]M16,19 V23"

    const val LAYER_LOCK =
        "[viewbox=32;color=#E8A33D]M9,14 H23 A2,2 0 0 1 25,16 V26 A2,2 0 0 1 23,28 H9 A2,2 0 0 1 7,26 V16 A2,2 0 0 1 9,14 Z" +
            "|[stroke=1.8]M9,14 H23 A2,2 0 0 1 25,16 V26 A2,2 0 0 1 23,28 H9 A2,2 0 0 1 7,26 V16 A2,2 0 0 1 9,14 Z" +
            "|[stroke=2]M11,14 V10 A5,5 0 0 1 21,10 V14" +
            "|[stroke=2]M16,19 V23"

    const val MAKE_CURRENT =
        "[viewbox=32;color=#E8A33D;opacity=0.55]M3,8 L13,3.8 L23,8 L13,12.2 Z" +
            "|[stroke=1.8]M3,8 L13,3.8 L23,8 L13,12.2 Z" +
            "|[stroke=1.8]M3,12.8 L13,17 L23,12.8 M3,17.6 L13,21.8 L23,17.6" +
            "|[stroke=2.4;color=#5CB85C]M16.5,22.5 L20.5,26.5 L28.5,16.5"

    const val MATCH_LAYER =
        "[viewbox=32;color=#E8A33D;opacity=0.55]M2,21 L8,18.2 L14,21 L8,23.8 Z" +
            "|[stroke=1.6]M2,21 L8,18.2 L14,21 L8,23.8 Z" +
            "|[stroke=1.6]M2,24.6 L8,27.4 L14,24.6" +
            "|[stroke=1.6]M18,21 L24,18.2 L30,21 L24,23.8 Z" +
            "|[stroke=1.6]M18,24.6 L24,27.4 L30,24.6" +
            "|[stroke=1.6;color=#E8A33D]M8,14 Q16,3 24,13" +
            "|[stroke=1.6;color=#E8A33D]M24.19,9.81 L24,13 L20.84,12.48"

    const val LAYER_ISOLATE =
        "[viewbox=32;color=#E8A33D;opacity=0.7]M4,10 L16,5 L28,10 L16,15 Z" +
            "|[stroke=1.8]M4,10 L16,5 L28,10 L16,15 Z" +
            "|[stroke=1.8;opacity=0.3]M4,16 L16,21 L28,16 M4,22 L16,27 L28,22"

    const val LAYER_UNISOLATE =
        "[viewbox=32;stroke=1.8]M3,9 L13,4.8 L23,9 L13,13.2 Z" +
            "|[stroke=1.8]M3,15 L13,19.2 L23,15 M3,21 L13,25.2 L23,21" +
            "|[stroke=1.6;color=#E8A33D]M28,4 V28" +
            "|[stroke=1.6;color=#E8A33D]M25.86,6.38 L28,4 L30.14,6.38 M30.14,25.62 L28,28 L25.86,25.62"

    const val LAYER_PREVIOUS =
        "[viewbox=32;stroke=1.8]M10,8 L19.5,4 L29,8 L19.5,12 Z" +
            "|[stroke=1.8]M10,12.8 L19.5,16.8 L29,12.8 M10,17.6 L19.5,21.6 L29,17.6" +
            "|[stroke=1.8;color=#E8A33D]M4,23 H12 A3,3 0 0 1 12,29 H8" +
            "|[stroke=1.8;color=#E8A33D]M6.23,25.01 L4,23 L6.23,20.99"

    // ---- Insert › Block

    const val INSERT_BLOCK =
        "[viewbox=32;stroke=1.8]M13,3 L29,3 L29,19 L13,19 Z M16.68,11 A4.32,4.32 0 1 1 25.32,11 A4.32,4.32 0 1 1 16.68,11 Z" +
            "|[stroke=1.8;color=#E8A33D]M4,28 L11,21" +
            "|[stroke=1.8;color=#E8A33D]M7.4,21.19 L11,21 L10.81,24.6" +
            "|[color=#3DA9F5]M11.5,17.5 L14.5,17.5 L14.5,20.5 L11.5,20.5 Z"

    const val CREATE_BLOCK =
        "[viewbox=32;stroke=1.8]M3,3 L20,3 L20,20 L3,20 Z M6.91,11.5 A4.59,4.59 0 1 1 16.09,11.5 A4.59,4.59 0 1 1 6.91,11.5 Z" +
            "|[stroke=2.4;color=#5CB85C]M21,25 H29 M25,21 V29"

    const val EDIT_BLOCK =
        "[viewbox=32;stroke=1.8]M3,3 L20,3 L20,20 L3,20 Z M6.91,11.5 A4.59,4.59 0 1 1 16.09,11.5 A4.59,4.59 0 1 1 6.91,11.5 Z" +
            "|[stroke=1.5;color=#E8A33D]M16,29 L21.13,27.41 L29.08,19.45 L25.55,15.92 L17.59,23.87 Z M21.13,27.41 L17.59,23.87"

    const val EDIT_ATTRIBUTE =
        "[viewbox=32;stroke=1.8]M3,10.5 L9.5,4 L23,4 L23,17 L9.5,17 Z M8.1,10.5 A1.4,1.4 0 1 1 10.9,10.5 A1.4,1.4 0 1 1 8.1,10.5 Z" +
            "|[stroke=1.4]M12,10.5 H19" +
            "|[stroke=1.5;color=#E8A33D]M16,29 L21.13,27.41 L29.08,19.45 L25.55,15.92 L17.59,23.87 Z M21.13,27.41 L17.59,23.87"

    const val DEFINE_ATTRIBUTES =
        "[viewbox=32;stroke=1.8]M3,10.5 L9.5,4 L23,4 L23,17 L9.5,17 Z M8.1,10.5 A1.4,1.4 0 1 1 10.9,10.5 A1.4,1.4 0 1 1 8.1,10.5 Z" +
            "|[stroke=1.4]M12,10.5 H19" +
            "|[stroke=2.4;color=#5CB85C]M21,25 H29 M25,21 V29"

    // ---- Home › Properties

    const val MATCH_PROPERTIES =
        "[viewbox=32;stroke=2.6]M27.5,4.5 L20,12" +
            "|[stroke=1.6]M21.1,15.1 L16.9,10.9 L12.9,14.9 L17.1,19.1 Z" +
            "|[color=#E8A33D]M12.9,14.9 Q7,17 4.5,27.5 Q15,25 17.1,19.1 Z" +
            "|[stroke=1.2;color=#E8A33D]M12.9,14.9 Q7,17 4.5,27.5 Q15,25 17.1,19.1"

    const val COLOR =
        "[viewbox=32;color=#E8A33D]M16,16 V4.5 A11.5,11.5 0 0 1 27.5,16 Z" +
            "|[color=#5CB85C]M16,16 H27.5 A11.5,11.5 0 0 1 16,27.5 Z" +
            "|[color=#3DA9F5]M16,16 V27.5 A11.5,11.5 0 0 1 4.5,16 Z" +
            "|[color=#E05A5A]M16,16 H4.5 A11.5,11.5 0 0 1 16,4.5 Z" +
            "|[stroke=1.6]M4.5,16 A11.5,11.5 0 1 1 27.5,16 A11.5,11.5 0 1 1 4.5,16 Z"

    const val LINETYPE =
        "[viewbox=32;stroke=1.8]M4,8 H28" +
            "|[stroke=1.8]M4,16 L8.2,16 M10.6,16 L14.8,16 M17.2,16 L21.4,16 M23.8,16 L28,16" +
            "|[stroke=1.8]M4,24 H11 M14.5,24 H15.5 M19,24 H28"

    const val LINEWEIGHT =
        "[viewbox=32;stroke=1.2]M5,7 H27" +
            "|[stroke=2.4]M5,15 H27" +
            "|[stroke=4.2]M5,24.5 H27"

    const val TRANSPARENCY =
        "[viewbox=32;opacity=0.35]M4,4 L8.8,4 L8.8,8.8 L4,8.8 Z M13.6,4 L18.4,4 L18.4,8.8 L13.6,8.8 Z M23.2,4 L28,4 L28,8.8 L23.2,8.8 Z M8.8,8.8 L13.6,8.8 L13.6,13.6 L8.8,13.6 Z M18.4,8.8 L23.2,8.8 L23.2,13.6 L18.4,13.6 Z M4,13.6 L8.8,13.6 L8.8,18.4 L4,18.4 Z M13.6,13.6 L18.4,13.6 L18.4,18.4 L13.6,18.4 Z M23.2,13.6 L28,13.6 L28,18.4 L23.2,18.4 Z M8.8,18.4 L13.6,18.4 L13.6,23.2 L8.8,23.2 Z M18.4,18.4 L23.2,18.4 L23.2,23.2 L18.4,23.2 Z M4,23.2 L8.8,23.2 L8.8,28 L4,28 Z M13.6,23.2 L18.4,23.2 L18.4,28 L13.6,28 Z M23.2,23.2 L28,23.2 L28,28 L23.2,28 Z" +
            "|[color=#E8A33D;opacity=0.7]M4,28 L28,4 V28 Z" +
            "|[stroke=1.6]M4,4 L28,4 L28,28 L4,28 Z"

    const val PROPERTIES_PALETTE =
        "[viewbox=32;stroke=1.8]M7,3 H25 A2,2 0 0 1 27,5 V27 A2,2 0 0 1 25,29 H7 A2,2 0 0 1 5,27 V5 A2,2 0 0 1 7,3 Z" +
            "|[stroke=1.3]M5,9 H27" +
            "|[stroke=1.6]M9,14 H14 M9,19 H14 M9,24 H14" +
            "|[stroke=1.6;color=#E8A33D]M17,14 H23 M17,19 H21 M17,24 H23"

    // ---- Home › Groups

    const val GROUP =
        "[viewbox=32;stroke=1.3;color=#E8A33D]M3.5,4 L28.5,4 L28.5,28.5 L3.5,28.5 Z" +
            "|[stroke=1.6]M7,8 L14,8 L14,15 L7,15 Z M17.7,11.5 A3.8,3.8 0 1 1 25.3,11.5 A3.8,3.8 0 1 1 17.7,11.5 Z M10,25 L14.5,18 L19,25 Z" +
            "|[color=#3DA9F5]M2.2,2.7 L4.8,2.7 L4.8,5.3 L2.2,5.3 Z M27.2,2.7 L29.8,2.7 L29.8,5.3 L27.2,5.3 Z M2.2,27.2 L4.8,27.2 L4.8,29.8 L2.2,29.8 Z M27.2,27.2 L29.8,27.2 L29.8,29.8 L27.2,29.8 Z"

    const val UNGROUP =
        "[viewbox=32;stroke=1.3;color=#E05A5A;opacity=0.85]M3.5,10 V4 H9.5 M22.5,4 H28.5 V10 M28.5,22.5 V28.5 H22.5 M9.5,28.5 H3.5 V22.5" +
            "|[stroke=1.6]M4,4 L11,4 L11,11 L4,11 Z M20.2,8 A3.8,3.8 0 1 1 27.8,8 A3.8,3.8 0 1 1 20.2,8 Z M11,28 L15.5,21 L20,28 Z"

    const val GROUP_EDIT =
        "[viewbox=32;stroke=1.3;color=#E8A33D]M3,3 L22,3 L22,22 L3,22 Z" +
            "|[stroke=1.5]M6.5,6.5 L12,6.5 L12,12 L6.5,12 Z M14.5,9.5 A3,3 0 1 1 20.5,9.5 A3,3 0 1 1 14.5,9.5 Z M8,19 L11.5,14 L15,19 Z" +
            "|[stroke=1.5;color=#E8A33D]M16,29 L21.13,27.41 L29.08,19.45 L25.55,15.92 L17.59,23.87 Z M21.13,27.41 L17.59,23.87"

    // ---- Home › Utilities

    const val MEASURE_DISTANCE =
        "[viewbox=32;stroke=1.8]M3,19 L29,19 L29,27 L3,27 Z" +
            "|[stroke=1.3]M7,19 V23 M11,19 V22 M15,19 V23 M19,19 V22 M23,19 V23" +
            "|[stroke=1.5;color=#E8A33D]M6,10 H26" +
            "|[color=#E8A33D]M6,10 L9.6,8.3 L9.6,11.7 Z M26,10 L22.4,11.7 L22.4,8.3 Z" +
            "|[stroke=1.3]M4,6 V14 M28,6 V14"

    const val MEASURE_AREA =
        "[viewbox=32;color=#E8A33D;opacity=0.35]M5,24 L8,7 L22,5 L27,18 L16,27 Z" +
            "|[stroke=1.8]M5,24 L8,7 L22,5 L27,18 L16,27 Z" +
            "|[color=#3DA9F5]M3.5,22.5 L6.5,22.5 L6.5,25.5 L3.5,25.5 Z M6.5,5.5 L9.5,5.5 L9.5,8.5 L6.5,8.5 Z M20.5,3.5 L23.5,3.5 L23.5,6.5 L20.5,6.5 Z M25.5,16.5 L28.5,16.5 L28.5,19.5 L25.5,19.5 Z M14.5,25.5 L17.5,25.5 L17.5,28.5 L14.5,28.5 Z"

    const val MEASURE_ANGLE =
        "[viewbox=32;stroke=1.8]M3,25 H29 M3,25 A13,13 0 0 1 29,25" +
            "|[stroke=1.3]M27.26,18.5 L25.18,19.7 M22.5,13.74 L21.3,15.82 M16,12 L16,14.4 M9.5,13.74 L10.7,15.82 M4.74,18.5 L6.82,19.7" +
            "|[stroke=1.8;color=#E8A33D]M16,25 L24.6,12.71" +
            "|[color=#3DA9F5]M14.7,23.7 L17.3,23.7 L17.3,26.3 L14.7,26.3 Z"

    const val QUICK_SELECT =
        "[viewbox=32;color=#E8A33D;opacity=0.5]M5,6 H27 L19,16 H13 Z" +
            "|[stroke=1.8]M5,6 H27 L19,16 V25 L13,28 V16 Z"

    const val SELECT_ALL =
        "[viewbox=32;stroke=1.3;color=#3DA9F5]M3,3 L6,3 M8.2,3 L11.2,3 M13.4,3 L16.4,3 M18.6,3 L21.6,3 M23.8,3 L26.8,3 M29,3 L29,6 M29,8.2 L29,11.2 M29,13.4 L29,16.4 M29,18.6 L29,21.6 M29,23.8 L29,26.8 M29,29 L26,29 M23.8,29 L20.8,29 M18.6,29 L15.6,29 M13.4,29 L10.4,29 M8.2,29 L5.2,29 M3,29 L3,26 M3,23.8 L3,20.8 M3,18.6 L3,15.6 M3,13.4 L3,10.4 M3,8.2 L3,5.2" +
            "|[stroke=1.8]M7,12 A5,5 0 1 1 17,12 A5,5 0 1 1 7,12 Z M8,25 L25,16" +
            "|[color=#3DA9F5]M10.5,10.5 L13.5,10.5 L13.5,13.5 L10.5,13.5 Z M6.5,23.5 L9.5,23.5 L9.5,26.5 L6.5,26.5 Z M23.5,14.5 L26.5,14.5 L26.5,17.5 L23.5,17.5 Z M15,19 L18,19 L18,22 L15,22 Z"

    const val ID_POINT =
        "[viewbox=32;stroke=1.3]M11,8 V30 M2,19 H22" +
            "|[stroke=1.6]M7.5,15.5 L14.5,15.5 L14.5,22.5 L7.5,22.5 Z" +
            "|[color=#E8A33D]M9.2,19 A1.8,1.8 0 1 1 12.8,19 A1.8,1.8 0 1 1 9.2,19 Z" +
            "|[stroke=1.3;color=#E8A33D]M17,3 L30,3 L30,11 L17,11 Z" +
            "|[stroke=1.4;color=#E8A33D]M20,7 H27"

    const val CALCULATOR =
        "[viewbox=32;stroke=1.8]M9.5,3 H22.5 A2.5,2.5 0 0 1 25,5.5 V26.5 A2.5,2.5 0 0 1 22.5,29 H9.5 A2.5,2.5 0 0 1 7,26.5 V5.5 A2.5,2.5 0 0 1 9.5,3 Z" +
            "|[color=#E8A33D;opacity=0.85]M10,6 L22,6 L22,11 L10,11 Z" +
            "|M10.1,14.6 L12.9,14.6 L12.9,17.4 L10.1,17.4 Z M14.6,14.6 L17.4,14.6 L17.4,17.4 L14.6,17.4 Z M19.1,14.6 L21.9,14.6 L21.9,17.4 L19.1,17.4 Z M10.1,19.1 L12.9,19.1 L12.9,21.9 L10.1,21.9 Z M14.6,19.1 L17.4,19.1 L17.4,21.9 L14.6,21.9 Z M19.1,19.1 L21.9,19.1 L21.9,21.9 L19.1,21.9 Z M10.1,23.6 L12.9,23.6 L12.9,26.4 L10.1,26.4 Z M14.6,23.6 L17.4,23.6 L17.4,26.4 L14.6,26.4 Z M19.1,23.6 L21.9,23.6 L21.9,26.4 L19.1,26.4 Z"

    // ---- Home › Clipboard

    const val PASTE =
        "[viewbox=32;stroke=1.8]M13,28 H5 V6 H21 V13" +
            "|[color=#E8A33D]M9.5,3.5 L16.5,3.5 L16.5,8.5 L9.5,8.5 Z" +
            "|[stroke=1.8]M13,13 L27,13 L27,29 L13,29 Z" +
            "|[stroke=1.4]M16,18 H24 M16,22 H24 M16,26 H21"

    const val COPY_CLIP =
        "[viewbox=32;stroke=1.8]M11,22 H5 V4 H19 V8" +
            "|[stroke=1.8]M11,8 L27,8 L27,28 L11,28 Z" +
            "|[stroke=1.4;color=#E8A33D]M14,13 H24 M14,18 H24 M14,23 H20"

    const val CUT =
        "[viewbox=32;stroke=1.8]M11.5,21 L21,3.5 M20.5,21 L11,3.5" +
            "|[stroke=1.8;color=#E8A33D]M5.5,24.5 A4,4 0 1 1 13.5,24.5 A4,4 0 1 1 5.5,24.5 Z M18.5,24.5 A4,4 0 1 1 26.5,24.5 A4,4 0 1 1 18.5,24.5 Z" +
            "|M14.7,12.3 A1.3,1.3 0 1 1 17.3,12.3 A1.3,1.3 0 1 1 14.7,12.3 Z"

    // ---- View

    const val ZOOM_EXTENTS =
        "[viewbox=32;stroke=1.8]M4.5,13 A8.5,8.5 0 1 1 21.5,13 A8.5,8.5 0 1 1 4.5,13 Z" +
            "|[stroke=2.8]M19.65,19.65 L27.5,27.5" +
            "|[stroke=1.5;color=#E8A33D]M8.5,11 V8.5 H11 M15,8.5 H17.5 V11 M17.5,15 V17.5 H15 M11,17.5 H8.5 V15"

    const val ZOOM_WINDOW =
        "[viewbox=32;stroke=1.8]M4.5,13 A8.5,8.5 0 1 1 21.5,13 A8.5,8.5 0 1 1 4.5,13 Z" +
            "|[stroke=2.8]M19.65,19.65 L27.5,27.5" +
            "|[stroke=1.5;color=#E8A33D]M9,10 L17,10 L17,16 L9,16 Z"

    const val PAN =
        "[viewbox=32;color=#E8A33D;opacity=0.25]M10,27 C6.5,24 4.5,20.5 4.5,18 C4.5,16.5 6.5,16 7.8,17.4 L9.5,19.5 V8.5 A1.6,1.6 0 0 1 12.7,8.5 V15 V5.6 A1.6,1.6 0 0 1 15.9,5.6 V15 V6.6 A1.6,1.6 0 0 1 19.1,6.6 V15.5 V9 A1.6,1.6 0 0 1 22.3,9 V20 C22.3,24 20.5,26 19.5,27 Z" +
            "|[stroke=1.6]M10,27 C6.5,24 4.5,20.5 4.5,18 C4.5,16.5 6.5,16 7.8,17.4 L9.5,19.5 V8.5 A1.6,1.6 0 0 1 12.7,8.5 V15 V5.6 A1.6,1.6 0 0 1 15.9,5.6 V15 V6.6 A1.6,1.6 0 0 1 19.1,6.6 V15.5 V9 A1.6,1.6 0 0 1 22.3,9 V20 C22.3,24 20.5,26 19.5,27 Z"

    const val ORBIT =
        "[viewbox=32;stroke=1.3;opacity=0.4]M29.5,16 A13.5,5 0 0 0 2.5,16" +
            "|[stroke=1.8]M9,16 A7,7 0 1 1 23,16 A7,7 0 1 1 9,16 Z" +
            "|[stroke=1.8;color=#E8A33D]M2.5,16 A13.5,5 0 0 0 28.84,17.55" +
            "|[stroke=1.8;color=#E8A33D]M25.45,19.87 L28.84,20.17 L28.18,23.51"

    const val VIEW_MANAGER =
        "[viewbox=32;stroke=1.8]M5,9 H19 A2,2 0 0 1 21,11 V23 A2,2 0 0 1 19,25 H5 A2,2 0 0 1 3,23 V11 A2,2 0 0 1 5,9 Z" +
            "|[stroke=1.8]M21,14 L29,9.5 V24.5 L21,20" +
            "|[color=#E8A33D]M7,14 A2,2 0 1 1 11,14 A2,2 0 1 1 7,14 Z" +
            "|[stroke=1.4;color=#E8A33D]M7,20.5 H17"

    const val NAMED_VIEWS =
        "[viewbox=32;stroke=1.3;opacity=0.6]M8,9 V5 H28 V21 H24" +
            "|[stroke=1.8]M4,9 L24,9 L24,25 L4,25 Z" +
            "|[stroke=1.4;color=#E8A33D]M8,16 V13 H11 M17,13 H20 V16 M20,18 V21 H17 M11,21 H8 V18"

    const val VIEWPORTS =
        "[viewbox=32;color=#E8A33D;opacity=0.4]M4,5 L16,5 L16,16 L4,16 Z" +
            "|[stroke=1.6;color=#E8A33D]M4,5 L16,5 L16,16 L4,16 Z" +
            "|[stroke=1.8]M4,5 L28,5 L28,27 L4,27 Z" +
            "|[stroke=1.3]M16,5 V27 M4,16 H28"

    // ---- Insert

    const val ATTACH =
        "[viewbox=32;stroke=1.8]M21,10 V22 A5,5 0 0 1 11,22 V8.5 A3.5,3.5 0 0 1 18,8.5 V21 A1.5,1.5 0 0 1 15,21 V11" +
            "|[stroke=1.5;color=#E8A33D]M24,4 V9 M21.5,6.5 H26.5"

    const val CLIP =
        "[viewbox=32;stroke=1.3;opacity=0.4]M4,5 L28,5 L28,25 L4,25 Z M6,22 L12,13 L17,19 L21,15 L26,22" +
            "|[stroke=1.8;color=#E8A33D]M8,9 L24,9 L24,21 L14,21 L8,15 Z" +
            "|[color=#3DA9F5]M6.7,7.7 L9.3,7.7 L9.3,10.3 L6.7,10.3 Z M22.7,7.7 L25.3,7.7 L25.3,10.3 L22.7,10.3 Z M22.7,19.7 L25.3,19.7 L25.3,22.3 L22.7,22.3 Z M12.7,19.7 L15.3,19.7 L15.3,22.3 L12.7,22.3 Z M6.7,13.7 L9.3,13.7 L9.3,16.3 L6.7,16.3 Z"

    const val IMPORT_PDF =
        "[viewbox=32;stroke=1.8]M12,4 H22 L28,10 V28 H12 V25 M12,16 V4 M22,4 V10 H28" +
            "|[color=#E05A5A]M4.2,16 H20.8 A1.2,1.2 0 0 1 22,17.2 V23.8 A1.2,1.2 0 0 1 20.8,25 H4.2 A1.2,1.2 0 0 1 3,23.8 V17.2 A1.2,1.2 0 0 1 4.2,16 Z" +
            "|[stroke=1.1;color=#FFFFFF]M5.2,23.2 V17.8 H6.6 A1.35,1.35 0 0 1 6.6,20.5 H5.2 M9.25,17.8 H9.85 A2.7,2.7 0 0 1 9.85,23.2 H9.25 Z M16.65,17.8 H13.85 V23.2 M13.85,20.5 H16.05" +
            "|[stroke=1.5;color=#E8A33D]M17,7 V11.5" +
            "|[stroke=1.5;color=#E8A33D]M18.74,10.07 L17,12 L15.26,10.07"

    const val DATA_EXTRACTION =
        "[viewbox=32;color=#E8A33D;opacity=0.85]M11,4 L28,4 L28,9 L11,9 Z" +
            "|[stroke=1.6]M11,4 L28,4 L28,20 L11,20 Z M11,9 H28 M11,14.5 H28 M19.5,9 V20" +
            "|[stroke=1.5]M3,21 L11,21 L11,29 L3,29 Z M4.84,25 A2.16,2.16 0 1 1 9.16,25 A2.16,2.16 0 1 1 4.84,25 Z" +
            "|[stroke=1.6;color=#E8A33D]M13,25 H21 V22.5" +
            "|[stroke=1.6;color=#E8A33D]M19.13,23.58 L21,21.5 L22.87,23.58"

    const val FIELD =
        "[viewbox=32;opacity=0.15]M3,9 L29,9 L29,23 L3,23 Z" +
            "|[stroke=1.6]M3,9 L29,9 L29,23 L3,23 Z" +
            "|[stroke=1.4;color=#E8A33D]M11,12 L9.5,20 M15,12 L13.5,20 M8,14.3 H16.5 M7.5,17.7 H16" +
            "|[stroke=1.6]M20,16 H25"

    // ---- Output

    const val PLOT =
        "[viewbox=32;stroke=1.8]M9,22 H4 V12 H28 V22 H23" +
            "|[stroke=1.8]M9,12 V4 H23 V12" +
            "|[stroke=1.8]M9,18 V28 H23 V18 M9,18 H23" +
            "|[stroke=1.3;color=#E8A33D]M12,22 H20 M12,25 H18" +
            "|[color=#E8A33D]M23.3,15 A1.2,1.2 0 1 1 25.7,15 A1.2,1.2 0 1 1 23.3,15 Z"

    const val EXPORT_PDF =
        "[viewbox=32;stroke=1.8]M4,4 H14 L20,10 V28 H4 V25 M4,16 V4 M14,4 V10 H20" +
            "|[color=#E05A5A]M10.2,16 H26.8 A1.2,1.2 0 0 1 28,17.2 V23.8 A1.2,1.2 0 0 1 26.8,25 H10.2 A1.2,1.2 0 0 1 9,23.8 V17.2 A1.2,1.2 0 0 1 10.2,16 Z" +
            "|[stroke=1.1;color=#FFFFFF]M11.2,23.2 V17.8 H12.6 A1.35,1.35 0 0 1 12.6,20.5 H11.2 M15.25,17.8 H15.85 A2.7,2.7 0 0 1 15.85,23.2 H15.25 Z M22.65,17.8 H19.85 V23.2 M19.85,20.5 H22.05" +
            "|[stroke=1.5;color=#E8A33D]M23,9 H29" +
            "|[stroke=1.5;color=#E8A33D]M27.57,7.26 L29.5,9 L27.57,10.74"

    const val PUBLISH =
        "[viewbox=32;stroke=1.3;opacity=0.55]M4,9 V27 H20" +
            "|[stroke=1.3;opacity=0.8]M7,6 V24 H23 V22" +
            "|[stroke=1.8]M10,3 L25,3 L25,21 L10,21 Z" +
            "|[stroke=1.8;color=#E8A33D]M19,26 L28,17" +
            "|[stroke=1.8;color=#E8A33D]M24.6,17.18 L28,17 L27.82,20.4"

    const val PAGE_SETUP =
        "[viewbox=32;stroke=1.8]M4,6 L28,6 L28,26 L4,26 Z" +
            "|[stroke=1.2;color=#E8A33D]M8,10 L10.2,10 M12,10 L14.2,10 M16,10 L18.2,10 M20,10 L22.2,10 M24,10 L24,12.2 M24,14 L24,16.2 M24,18 L24,20.2 M24,22 L21.8,22 M20,22 L17.8,22 M16,22 L13.8,22 M12,22 L9.8,22 M8,22 L8,19.8 M8,18 L8,15.8 M8,14 L8,11.8" +
            "|[stroke=1.3]M17,22 V18 H24"

    const val BATCH_PLOT =
        "[viewbox=32;stroke=1.8]M9,19 H4 V9 H28 V19 H23" +
            "|[stroke=1.8]M9,9 V3 H23 V9" +
            "|[stroke=1.8]M9,15 V24 H23 V15 M9,15 H23" +
            "|[stroke=1.5;color=#E8A33D]M12,26 V29 H26 V17 H23" +
            "|[color=#E8A33D]M23.3,12 A1.2,1.2 0 1 1 25.7,12 A1.2,1.2 0 1 1 23.3,12 Z"

    // ---- Manage

    const val CUSTOMIZE =
        "[viewbox=32;stroke=1.8]M4.5,4 H23.5 A1.5,1.5 0 0 1 25,5.5 V20.5 A1.5,1.5 0 0 1 23.5,22 H4.5 A1.5,1.5 0 0 1 3,20.5 V5.5 A1.5,1.5 0 0 1 4.5,4 Z" +
            "|[stroke=1.3]M3,9 H25" +
            "|M6,12 L9,12 L9,15 L6,15 Z M11,12 L14,12 L14,15 L11,15 Z M6,17 L9,17 L9,20 L6,20 Z" +
            "|[stroke=1.5;color=#E8A33D]M16,29 L21.13,27.41 L29.08,19.45 L25.55,15.92 L17.59,23.87 Z M21.13,27.41 L17.59,23.87"

    const val ACTION_RECORDER =
        "[viewbox=32;stroke=1.8]M4.5,16 A11.5,11.5 0 1 1 27.5,16 A11.5,11.5 0 1 1 4.5,16 Z" +
            "|[color=#E05A5A]M10,16 A6,6 0 1 1 22,16 A6,6 0 1 1 10,16 Z"

    const val PURGE =
        "[viewbox=32;stroke=1.8]M5,8 H27 M12,8 V4.5 H20 V8" +
            "|[stroke=1.8]M7.5,8 L9.5,28 H22.5 L24.5,8" +
            "|[stroke=1.4;color=#E05A5A]M13,12.5 V24 M16,12.5 V24 M19,12.5 V24"

    const val UNITS =
        "[viewbox=32;stroke=1.8]M3,22 L22,3 L29,10 L10,29 Z" +
            "|[stroke=1.3;color=#E8A33D]M5.38,19.62 L6.93,21.18 M7.75,17.25 L10.15,19.65 M10.12,14.88 L11.68,16.43 M12.5,12.5 L14.9,14.9 M14.88,10.12 L16.43,11.68 M17.25,7.75 L19.65,10.15 M19.62,5.38 L21.18,6.93"

    const val OPTIONS =
        "[viewbox=32;stroke=1.8]M18.24,6.67 L17.76,3.62 L14.24,3.62 L13.76,6.67 L10.98,7.81 L8.49,6 L6,8.49 L7.81,10.98 L6.67,13.76 L3.62,14.24 L3.62,17.76 L6.67,18.24 L7.81,21.02 L6,23.51 L8.49,26 L10.98,24.19 L13.76,25.33 L14.24,28.38 L17.76,28.38 L18.24,25.33 L21.02,24.19 L23.51,26 L26,23.51 L24.19,21.02 L25.33,18.24 L28.38,17.76 L28.38,14.24 L25.33,13.76 L24.19,10.98 L26,8.49 L23.51,6 L21.02,7.81 Z" +
            "|[stroke=1.8;color=#E8A33D]M11.8,16 A4.2,4.2 0 1 1 20.2,16 A4.2,4.2 0 1 1 11.8,16 Z"

    const val LOAD_APPLICATION =
        "[viewbox=32;color=#E8A33D;opacity=0.35]M6,10 H12.5 A3,3 0 1 1 18.5,10 H24 V15.5 A3,3 0 1 1 24,21.5 V28 H6 Z" +
            "|[stroke=1.8]M6,10 H12.5 A3,3 0 1 1 18.5,10 H24 V15.5 A3,3 0 1 1 24,21.5 V28 H6 Z"

    // ---- 3D

    const val BOX =
        "[viewbox=32;color=#E8A33D;opacity=0.55]M16,3 L27.5,9.5 L16,16 L4.5,9.5 Z" +
            "|[stroke=1.8]M16,3 L27.5,9.5 L27.5,22.5 L16,29 L4.5,22.5 L4.5,9.5 Z" +
            "|[stroke=1.8]M4.5,9.5 L16,16 L27.5,9.5 M16,16 V29"

    const val CYLINDER =
        "[viewbox=32;color=#E8A33D;opacity=0.55]M6,8 A10,4 0 1 1 26,8 A10,4 0 1 1 6,8 Z" +
            "|[stroke=1.8]M6,8 A10,4 0 1 1 26,8 A10,4 0 1 1 6,8 Z" +
            "|[stroke=1.8]M6,8 V24 M26,8 V24 M6,24 A10,4 0 0 0 26,24" +
            "|[stroke=1.3;opacity=0.35]M6,24 A10,4 0 0 1 26,24"

    const val SPHERE =
        "[viewbox=32;stroke=1.8]M3.5,16 A12.5,12.5 0 1 1 28.5,16 A12.5,12.5 0 1 1 3.5,16 Z" +
            "|[stroke=1.8;color=#E8A33D]M3.5,16 A12.5,4.5 0 0 0 28.5,16" +
            "|[stroke=1.3;opacity=0.35]M3.5,16 A12.5,4.5 0 0 1 28.5,16" +
            "|[stroke=1.3;opacity=0.55]M16,3.5 A5,12.5 0 0 0 16,28.5"

    const val CONE =
        "[viewbox=32;color=#E8A33D;opacity=0.3]M16,4 L6,24 A10,4 0 0 0 26,24 Z" +
            "|[stroke=1.8]M16,4 L6,24 A10,4 0 0 0 26,24 Z" +
            "|[stroke=1.3;opacity=0.35]M6,24 A10,4 0 0 1 26,24"

    const val EXTRUDE =
        "[viewbox=32;color=#E8A33D;opacity=0.55]M4,22 L13,27 L22,22 L13,17 Z" +
            "|[stroke=1.8]M4,22 L13,27 L22,22 M4,11 L13,16 L22,11 L13,6 Z M4,11 V22 M13,16 V27 M22,11 V22" +
            "|[stroke=1.3;opacity=0.35]M13,17 L4,22 M13,17 L22,22 M13,17 V6" +
            "|[stroke=1.8;color=#E8A33D]M28,24 V5" +
            "|[stroke=1.8;color=#E8A33D]M25.86,6.88 L28,4.5 L30.14,6.88"

    const val REVOLVE =
        "[viewbox=32;stroke=1.2;color=#3DA9F5]M16,2.5 L16,5.1 M16,6.9 L16,9.5 M16,11.3 L16,13.9 M16,15.7 L16,18.3 M16,20.1 L16,22.7 M16,24.5 L16,27.1 M16,28.9 L16,29.5" +
            "|[stroke=1.8;color=#E8A33D]M16,5 H21 V10 H19 V16 H25 V22 H16" +
            "|[stroke=1.3;opacity=0.3]M16,5 H11 V10 H13 V16 H7 V22 H16" +
            "|[stroke=1.8]M7,25.5 A9,3 0 0 0 25,25.5" +
            "|[stroke=1.8]M22.13,26.38 L25,25.5 L25.57,28.44"

    const val SWEEP =
        "[viewbox=32;stroke=1.8]M7,24 C7,10 26,22 26,7" +
            "|[stroke=1.8]M23.72,7.53 L26,5 L28.28,7.53" +
            "|[color=#E8A33D;opacity=0.45]M2.8,25 A4.2,1.9 0 1 1 11.2,25 A4.2,1.9 0 1 1 2.8,25 Z" +
            "|[stroke=1.6;color=#E8A33D]M2.8,25 A4.2,1.9 0 1 1 11.2,25 A4.2,1.9 0 1 1 2.8,25 Z"

    const val LOFT =
        "[viewbox=32;stroke=1.8]M6,25 Q13,16 7.5,7 M26,25 Q19,16 24.5,7" +
            "|[stroke=1.5;color=#E8A33D]M6,25 A10,3.5 0 1 1 26,25 A10,3.5 0 1 1 6,25 Z M9.7,16 A6.3,2.3 0 1 1 22.3,16 A6.3,2.3 0 1 1 9.7,16 Z M7.5,7 A8.5,3 0 1 1 24.5,7 A8.5,3 0 1 1 7.5,7 Z"

    const val UNION =
        "[viewbox=32;color=#E8A33D;opacity=0.5]M16,9.07 A8,8 0 1 0 16,22.93 A8,8 0 1 0 16,9.07 Z" +
            "|[stroke=1.8]M16,9.07 A8,8 0 1 0 16,22.93 A8,8 0 1 0 16,9.07 Z" +
            "|[stroke=1.3;opacity=0.35]M16,9.07 A8,8 0 0 0 16,22.93 A8,8 0 0 0 16,9.07"

    const val SUBTRACT =
        "[viewbox=32;color=#E8A33D;opacity=0.5]M16,9.07 A8,8 0 1 0 16,22.93 A8,8 0 0 1 16,9.07 Z" +
            "|[stroke=1.8]M16,9.07 A8,8 0 1 0 16,22.93 A8,8 0 0 1 16,9.07 Z" +
            "|[stroke=1.3;opacity=0.4]M16,9.07 A8,8 0 1 1 16,22.93"

    const val INTERSECT =
        "[viewbox=32;color=#E8A33D;opacity=0.6]M16,9.07 A8,8 0 0 1 16,22.93 A8,8 0 0 1 16,9.07 Z" +
            "|[stroke=1.3;opacity=0.6]M4,16 A8,8 0 1 1 20,16 A8,8 0 1 1 4,16 Z M12,16 A8,8 0 1 1 28,16 A8,8 0 1 1 12,16 Z" +
            "|[stroke=1.8;color=#E8A33D]M16,9.07 A8,8 0 0 1 16,22.93 A8,8 0 0 1 16,9.07 Z"

    const val PRESSPULL =
        "[viewbox=32;color=#E8A33D;opacity=0.55]M4,17 L16,11 L28,17 L16,23 Z" +
            "|[stroke=1.8]M4,17 L16,11 L28,17 L16,23 Z" +
            "|[stroke=1.8]M4,17 V22 L16,28 L28,22 V17 M16,23 V28" +
            "|[stroke=1.8;color=#E8A33D]M16,17 V5" +
            "|[stroke=1.8;color=#E8A33D]M13.86,6.38 L16,4 L18.14,6.38"

    // ---- Status bar

    const val GRID = "[viewbox=32;stroke=1.5]M4,4 H28 M4,12 H28 M4,20 H28 M4,28 H28 M4,4 V28 M12,4 V28 M20,4 V28 M28,4 V28"

    const val SNAP =
        "[viewbox=32]M4.5,4.5 L7.5,4.5 L7.5,7.5 L4.5,7.5 Z M14.5,4.5 L17.5,4.5 L17.5,7.5 L14.5,7.5 Z M24.5,4.5 L27.5,4.5 L27.5,7.5 L24.5,7.5 Z M4.5,14.5 L7.5,14.5 L7.5,17.5 L4.5,17.5 Z M24.5,14.5 L27.5,14.5 L27.5,17.5 L24.5,17.5 Z M4.5,24.5 L7.5,24.5 L7.5,27.5 L4.5,27.5 Z M14.5,24.5 L17.5,24.5 L17.5,27.5 L14.5,27.5 Z M24.5,24.5 L27.5,24.5 L27.5,27.5 L24.5,27.5 Z" +
            "|[stroke=2;color=#E8A33D]M12,16 A4,4 0 1 1 20,16 A4,4 0 1 1 12,16 Z" +
            "|[color=#E8A33D]M14.7,16 A1.3,1.3 0 1 1 17.3,16 A1.3,1.3 0 1 1 14.7,16 Z"

    const val ORTHO =
        "[viewbox=32;stroke=2]M6,4 V26 H28" +
            "|[stroke=1.6;color=#E8A33D]M6,19 H13 V26"

    const val POLAR =
        "[viewbox=32;stroke=2]M5,27 H29 M5,27 V3" +
            "|[stroke=1.6;color=#E8A33D]M5,27 L24.92,15.5 M5,27 L16.5,7.08" +
            "|[stroke=1.3]M18,27 A13,13 0 0 0 5,14"

    const val OBJECT_SNAP =
        "[viewbox=32;stroke=2]M4,28 L22,10" +
            "|[stroke=2;color=#E8A33D]M16,4 L28,4 L28,16 L16,16 Z"

    const val OBJECT_SNAP_TRACKING =
        "[viewbox=32;stroke=1.3]M9,10 L11.4,10 M13.4,10 L15.8,10 M17.8,10 L20.2,10 M22.2,10 L24.6,10 M26.6,10 L29,10 M22,22 L22,19.6 M22,17.6 L22,15.2 M22,13.2 L22,10.8 M22,8.8 L22,6.4 M22,4.4 L22,3" +
            "|[stroke=1.8;color=#E8A33D]M2.9,10 H8.1 M5.5,7.4 V12.6 M19.4,26.5 H24.6 M22,23.9 V29.1" +
            "|[stroke=2;color=#E8A33D]M19.4,7.4 L24.6,12.6 M24.6,7.4 L19.4,12.6"

    const val SHOW_LINEWEIGHT =
        "[viewbox=32;stroke=1.2]M4,13 L13,4" +
            "|[stroke=2.2]M4,21 L21,4" +
            "|[stroke=3.6]M5,28.5 L28.5,5"

    const val SHOW_TRANSPARENCY =
        "[viewbox=32;opacity=0.9]M4,4 L20,4 L20,20 L4,20 Z" +
            "|[color=#E8A33D;opacity=0.65]M12,12 L28,12 L28,28 L12,28 Z" +
            "|[stroke=1.4;color=#E8A33D]M12,12 L28,12 L28,28 L12,28 Z"

    const val SELECTION_CYCLING =
        "[viewbox=32;stroke=1.6]M4,4 L18,4 L18,18 L4,18 Z" +
            "|[stroke=1.6]M10,10 L24,10 L24,24 L10,24 Z" +
            "|[stroke=2;color=#E8A33D]M27,13 A12,12 0 0 1 13,27" +
            "|[stroke=2;color=#E8A33D]M15,28.65 L12.5,27 L14.41,24.68"

    const val ANNOTATION_SCALE =
        "[viewbox=32;stroke=2]M7,20 L16,3 L25,20 M10.42,13.54 L21.58,13.54" +
            "|[stroke=1.6;color=#E8A33D]M3,26 H29" +
            "|[stroke=1.4;color=#E8A33D]M3,23 V29 M9.5,24.5 V27.5 M16,23 V29 M22.5,24.5 V27.5 M29,23 V29"

    const val WORKSPACE =
        "[viewbox=32;stroke=1.8]M5,5 H27 A2,2 0 0 1 29,7 V25 A2,2 0 0 1 27,27 H5 A2,2 0 0 1 3,25 V7 A2,2 0 0 1 5,5 Z" +
            "|[stroke=1.3]M3,10 H29" +
            "|[color=#E8A33D;opacity=0.75]M4.5,11.5 L11,11.5 L11,25.5 L4.5,25.5 Z" +
            "|[stroke=1.3]M11,10 V27"

    const val CLEAN_SCREEN =
        "[viewbox=32;stroke=2]M4,11 V4 H11 M21,4 H28 V11 M28,21 V28 H21 M11,28 H4 V21" +
            "|[stroke=1.6;color=#E8A33D]M10,10 L22,10 L22,22 L10,22 Z"

    const val MODEL =
        "[viewbox=32;stroke=2;color=#5CB85C]M6,26 V6" +
            "|[stroke=2;color=#5CB85C]M3.72,7.03 L6,4.5 L8.28,7.03" +
            "|[stroke=2;color=#E05A5A]M6,26 H26" +
            "|[stroke=2;color=#E05A5A]M24.97,23.72 L27.5,26 L24.97,28.28" +
            "|[stroke=1.6]M3.5,23.5 L8.5,23.5 L8.5,28.5 L3.5,28.5 Z"

    const val PAPER =
        "[viewbox=32;stroke=2]M3,6 L29,6 L29,26 L3,26 Z" +
            "|[stroke=1.4;color=#E8A33D]M7,10 L18,10 L18,22 L7,22 Z" +
            "|[stroke=1.4]M21,22 H25 M21,18 H25"

    // ---- Application

    const val APP_LOGO =
        "[viewbox=32;color=#C8302C]M9,3 H23 A6,6 0 0 1 29,9 V23 A6,6 0 0 1 23,29 H9 A6,6 0 0 1 3,23 V9 A6,6 0 0 1 9,3 Z" +
            "|[stroke=3;color=#FFFFFF]M22.4,10.6 A8,8 0 1 0 22.4,21.4" +
            "|[color=#FFFFFF]M20.6,8.8 L24.2,8.8 L24.2,12.4 L20.6,12.4 Z M20.6,19.6 L24.2,19.6 L24.2,23.2 L20.6,23.2 Z"

    const val NEW =
        "[viewbox=32;stroke=1.8]M18,29 H7 V3 H19 L25,9 V18 M19,3 V9 H25" +
            "|[stroke=2.4;color=#5CB85C]M19.8,25 H28.2 M24,20.8 V29.2"

    const val OPEN =
        "[viewbox=32;stroke=1.8]M4,26 V6 H12 L14.5,9 H24 V14" +
            "|[color=#E8A33D;opacity=0.6]M4,26 L9,14 H29.5 L24.5,26 Z" +
            "|[stroke=1.8]M4,26 L9,14 H29.5 L24.5,26 Z"

    const val SAVE =
        "[viewbox=32;stroke=1.8]M5,5 H23 L27,9 V27 H5 Z" +
            "|[color=#E8A33D;opacity=0.85]M10,5 L21,5 L21,11.5 L10,11.5 Z" +
            "|[stroke=1.6]M9,17 L23,17 L23,27 L9,27 Z"

    const val SAVE_AS =
        "[viewbox=32;stroke=1.8]M15,27 H5 V5 H23 L27,9 V16" +
            "|[color=#E8A33D;opacity=0.85]M10,5 L21,5 L21,11.5 L10,11.5 Z" +
            "|[stroke=1.6]M9,27 V17 H19" +
            "|[stroke=1.5;color=#E8A33D]M17,29 L22.13,27.41 L28.67,20.87 L25.13,17.33 L18.59,23.87 Z M22.13,27.41 L18.59,23.87"

    const val UNDO =
        "[viewbox=32;stroke=1.8]M7,12 H19 A7,7 0 0 1 19,26 H11" +
            "|[stroke=1.8]M10.12,14.81 L7,12 L10.12,9.19"

    const val REDO =
        "[viewbox=32;stroke=1.8]M25,12 H13 A7,7 0 0 0 13,26 H21" +
            "|[stroke=1.8]M21.88,9.19 L25,12 L21.88,14.81"

    const val CLOSE =
        "[viewbox=32;stroke=1.8]M18,29 H7 V3 H19 L25,9 V18 M19,3 V9 H25" +
            "|[stroke=2.4;color=#E05A5A]M20.4,21.4 L27.6,28.6 M27.6,21.4 L20.4,28.6"

    const val EXIT =
        "[viewbox=32;stroke=1.8]M19,10 V4 H5 V28 H19 V22" +
            "|[stroke=1.8;color=#E8A33D]M12,16 H28" +
            "|[stroke=1.8;color=#E8A33D]M25.03,13.32 L28,16 L25.03,18.68"

    const val RECENT =
        "[viewbox=32;stroke=1.8]M4,16 A12,12 0 1 1 28,16 A12,12 0 1 1 4,16 Z" +
            "|[stroke=2;color=#E8A33D]M16,8 V16 L21.5,19.5"

    const val SEARCH =
        "[viewbox=32;stroke=1.8]M4.5,13.5 A9,9 0 1 1 22.5,13.5 A9,9 0 1 1 4.5,13.5 Z" +
            "|[stroke=3]M20.5,20.5 L28,28"

    const val HELP =
        "[viewbox=32;stroke=1.8]M3.5,16 A12.5,12.5 0 1 1 28.5,16 A12.5,12.5 0 1 1 3.5,16 Z" +
            "|[stroke=2.2;color=#3DA9F5]M11.8,12.4 A4.3,4.3 0 1 1 18.6,15.6 C17,16.8 16,17.6 16,20" +
            "|[color=#3DA9F5]M14.5,23.8 A1.5,1.5 0 1 1 17.5,23.8 A1.5,1.5 0 1 1 14.5,23.8 Z"

    const val SHARE =
        "[viewbox=32;stroke=1.6]M11.5,14.5 L20.5,9 M11.5,17.5 L20.5,23" +
            "|[stroke=1.8]M20,7.5 A3.5,3.5 0 1 1 27,7.5 A3.5,3.5 0 1 1 20,7.5 Z M20,24.5 A3.5,3.5 0 1 1 27,24.5 A3.5,3.5 0 1 1 20,24.5 Z" +
            "|[color=#E8A33D]M4.7,16 A3.8,3.8 0 1 1 12.3,16 A3.8,3.8 0 1 1 4.7,16 Z" +
            "|[stroke=1.8]M4.7,16 A3.8,3.8 0 1 1 12.3,16 A3.8,3.8 0 1 1 4.7,16 Z"

    // ---- Others

    const val CIRCLE_TAN_TAN_RADIUS =
        "[viewbox=32;stroke=1.3;opacity=0.6]M2,28 L30,28 M3,4 L3,30" +
            "|[stroke=1.8]M3,17 A11,11 0 1 1 25,17 A11,11 0 1 1 3,17 Z" +
            "|[stroke=1.3;color=#E8A33D]M14,17 L21.78,9.22" +
            "|[color=#3DA9F5]M1.5,15.5 L4.5,15.5 L4.5,18.5 L1.5,18.5 Z M12.5,26.5 L15.5,26.5 L15.5,29.5 L12.5,29.5 Z"

    const val CIRCLE_TAN_TAN_TAN =
        "[viewbox=32;stroke=1.3;opacity=0.6]M2,29 L30,29 M16,1 L1,27 M16,1 L31,27" +
            "|[stroke=1.8]M8.5,19.5 A7.5,7.5 0 1 1 23.5,19.5 A7.5,7.5 0 1 1 8.5,19.5 Z" +
            "|[color=#3DA9F5]M14.5,25.5 L17.5,25.5 L17.5,28.5 L14.5,28.5 Z M8,14 L11,14 L11,17 L8,17 Z M21,14 L24,14 L24,17 L21,17 Z"

    const val ELLIPTICAL_ARC =
        "[viewbox=32;stroke=1.3;opacity=0.35]M3.5,16 A12.5,7.5 0 0 0 28.5,16" +
            "|[stroke=1.8]M3.5,16 A12.5,7.5 0 0 1 28.5,16" +
            "|[color=#3DA9F5]M2,14.5 L5,14.5 L5,17.5 L2,17.5 Z M27,14.5 L30,14.5 L30,17.5 L27,17.5 Z"

    const val SURFACE_NETWORK =
        "[viewbox=32;color=#E8A33D;opacity=0.35]M4,22 C10,14 22,26 28,18 L24,8 C18,14 10,4 6,10 Z" +
            "|[stroke=1.6]M4,22 C10,14 22,26 28,18 M6,10 C10,4 18,14 24,8 M4,22 L6,10 M28,18 L24,8" +
            "|[stroke=1.2;color=#E8A33D]M5,16 C11,10 20,21 26,13 M12,19 L13,9 M20,21 L19,11"

    const val SURFACE_PLANAR =
        "[viewbox=32;color=#E8A33D;opacity=0.45]M3,22 L13,8 L29,10 L19,24 Z" +
            "|[stroke=1.8]M3,22 L13,8 L29,10 L19,24 Z" +
            "|[color=#3DA9F5]M1.5,20.5 L4.5,20.5 L4.5,23.5 L1.5,23.5 Z M27.5,8.5 L30.5,8.5 L30.5,11.5 L27.5,11.5 Z"

    const val MESH_BOX =
        "[viewbox=32;color=#E8A33D;opacity=0.4]M16,3 L27.5,9.5 L16,16 L4.5,9.5 Z" +
            "|[stroke=1.6]M16,3 L27.5,9.5 L27.5,22.5 L16,29 L4.5,22.5 L4.5,9.5 Z M4.5,9.5 L16,16 L27.5,9.5 M16,16 V29" +
            "|[stroke=1]M10.25,6.25 L21.75,12.75 M21.75,6.25 L10.25,12.75 M4.5,16 L16,22.5 L27.5,16 M10.25,12.75 V25.75 M21.75,12.75 V25.75"

    const val MESH_SMOOTH =
        "[viewbox=32;color=#E8A33D;opacity=0.4]M16,3.5 C25,3.5 28.5,8 28.5,16 C28.5,24 25,28.5 16,28.5 C7,28.5 3.5,24 3.5,16 C3.5,8 7,3.5 16,3.5 Z" +
            "|[stroke=1.8]M16,3.5 C25,3.5 28.5,8 28.5,16 C28.5,24 25,28.5 16,28.5 C7,28.5 3.5,24 3.5,16 C3.5,8 7,3.5 16,3.5 Z" +
            "|[stroke=1]M3.5,16 C10,20 22,20 28.5,16 M16,3.5 C12,10 12,22 16,28.5"

    const val LIGHT =
        "[viewbox=32;color=#F2C94C]M10,14 A6,6 0 1 1 22,14 C22,17 20,18.5 19.5,21 H12.5 C12,18.5 10,17 10,14 Z" +
            "|[stroke=1.8]M10,14 A6,6 0 1 1 22,14 C22,17 20,18.5 19.5,21 H12.5 C12,18.5 10,17 10,14 Z" +
            "|[stroke=1.6]M12.5,24.5 H19.5 M14,28 H18" +
            "|[stroke=1.4;color=#F2C94C]M16,3 V1 M24.5,6 L26,4.5 M7.5,6 L6,4.5 M27.5,14 H29.5 M4.5,14 H2.5"

    const val SUN =
        "[viewbox=32;color=#F2C94C]M10,16 A6,6 0 1 1 22,16 A6,6 0 1 1 10,16 Z" +
            "|[stroke=1.6]M10,16 A6,6 0 1 1 22,16 A6,6 0 1 1 10,16 Z" +
            "|[stroke=1.8;color=#F2C94C]M16,2.5 V6 M16,26 V29.5 M2.5,16 H6 M26,16 H29.5 M6.45,6.45 L8.93,8.93 M23.07,23.07 L25.55,25.55 M6.45,25.55 L8.93,23.07 M23.07,8.93 L25.55,6.45"

    const val MATERIAL =
        "[viewbox=32;color=#E8A33D;opacity=0.65]M4,16 A12,12 0 1 1 28,16 A12,12 0 1 1 4,16 Z" +
            "|[color=#FFFFFF;opacity=0.55]M9,11 A4,3 0 1 1 17,11 A4,3 0 1 1 9,11 Z" +
            "|[stroke=1.8]M4,16 A12,12 0 1 1 28,16 A12,12 0 1 1 4,16 Z" +
            "|[stroke=1.2;opacity=0.5]M6,22 C12,18 20,26 27,20"

    const val RENDER =
        "[viewbox=32;stroke=1.8]M3,6 H29 V24 H3 Z" +
            "|[color=#3DA9F5;opacity=0.55]M5,8 H27 V17 H5 Z" +
            "|[color=#5CB85C;opacity=0.75]M5,22 L12,14 L17,19 L21,15 L27,22 Z" +
            "|[color=#F2C94C]M20,10.5 A2.5,2.5 0 1 1 25,10.5 A2.5,2.5 0 1 1 20,10.5 Z" +
            "|[stroke=1.8]M11,28 H21 M16,24 V28"

    const val CAMERA =
        "[viewbox=32;stroke=1.8]M3,10 H20 V24 H3 Z" +
            "|[stroke=1.8;color=#E8A33D]M20,14 L29,9 V25 L20,20" +
            "|[color=#E8A33D;opacity=0.5]M20,14 L29,9 V25 L20,20 Z" +
            "|[stroke=1.6]M6,6 A3,3 0 1 1 12,6 A3,3 0 1 1 6,6 Z M13,6 A3,3 0 1 1 19,6 A3,3 0 1 1 13,6 Z"

    const val COMPARE =
        "[viewbox=32;stroke=1.8]M4,4 H16 V24 H4 Z" +
            "|[stroke=1.8;color=#E8A33D]M16,8 H28 V28 H16" +
            "|[color=#E05A5A;opacity=0.7]M7,8 H13 V12 H7 Z" +
            "|[color=#5CB85C;opacity=0.8]M19,18 H25 V22 H19 Z"

    const val CONSTRAINT_AUTO =
        "[viewbox=32;stroke=1.8]M4,26 L4,10 L18,10" +
            "|[stroke=1.6;color=#E8A33D]M4,15 H9 V10" +
            "|[stroke=1.6;color=#3DA9F5]M20,26 L28,18 M23,18 H28 V23" +
            "|[color=#E8A33D]M14,20 L17,25 L22,15 L23.5,16 L17,28 L12.5,21 Z"

    const val CONSTRAINT_COINCIDENT =
        "[viewbox=32;stroke=1.8]M4,26 L16,16 L28,22 M8,6 L16,16" +
            "|[color=#E8A33D]M13,16 A3,3 0 1 1 19,16 A3,3 0 1 1 13,16 Z"

    const val CONSTRAINT_COLLINEAR =
        "[viewbox=32;stroke=1.8]M3,26 L13,17 M19,11.6 L29,3" +
            "|[stroke=1.3;color=#E8A33D]M13,17 L19,11.6" +
            "|[color=#3DA9F5]M11.5,15.5 L14.5,15.5 L14.5,18.5 L11.5,18.5 Z M17.5,10 L20.5,10 L20.5,13 L17.5,13 Z"

    const val CONSTRAINT_CONCENTRIC =
        "[viewbox=32;stroke=1.8]M4,16 A12,12 0 1 1 28,16 A12,12 0 1 1 4,16 Z" +
            "|[stroke=1.8;color=#E8A33D]M10,16 A6,6 0 1 1 22,16 A6,6 0 1 1 10,16 Z" +
            "|[color=#3DA9F5]M14.5,14.5 L17.5,14.5 L17.5,17.5 L14.5,17.5 Z"

    const val CONSTRAINT_PARALLEL =
        "[viewbox=32;stroke=1.8]M4,22 L22,4" +
            "|[stroke=1.8;color=#E8A33D]M10,28 L28,10"

    const val CONSTRAINT_PERPENDICULAR =
        "[viewbox=32;stroke=1.8]M4,27 H28" +
            "|[stroke=1.8;color=#E8A33D]M16,27 V4" +
            "|[stroke=1.3]M16,21 H22 V27"

    const val CONSTRAINT_HORIZONTAL =
        "[viewbox=32;stroke=1.8;color=#E8A33D]M4,16 H28" +
            "|[color=#3DA9F5]M2.5,14.5 L5.5,14.5 L5.5,17.5 L2.5,17.5 Z M26.5,14.5 L29.5,14.5 L29.5,17.5 L26.5,17.5 Z" +
            "|[stroke=1.3;opacity=0.6]M8,10 H24 M8,22 H24"

    const val CONSTRAINT_VERTICAL =
        "[viewbox=32;stroke=1.8;color=#E8A33D]M16,4 V28" +
            "|[color=#3DA9F5]M14.5,2.5 L17.5,2.5 L17.5,5.5 L14.5,5.5 Z M14.5,26.5 L17.5,26.5 L17.5,29.5 L14.5,29.5 Z" +
            "|[stroke=1.3;opacity=0.6]M10,8 V24 M22,8 V24"

    const val CONSTRAINT_TANGENT =
        "[viewbox=32;stroke=1.8]M6,18 A9,9 0 1 1 24,18 A9,9 0 1 1 6,18 Z" +
            "|[stroke=1.8;color=#E8A33D]M2,27 H30"

    const val CONSTRAINT_EQUAL =
        "[viewbox=32;stroke=1.8]M5,8 L27,8" +
            "|[stroke=1.8;color=#E8A33D]M5,24 L27,24" +
            "|[stroke=1.6]M12,14 H20 M12,18 H20"

    const val COMMAND_LINE =
        "[viewbox=32;stroke=1.8]M3,6 H29 V26 H3 Z" +
            "|[stroke=1.8;color=#E8A33D]M8,12 L13,16 L8,20 M15,21 H23"

    const val FILE_TABS =
        "[viewbox=32;stroke=1.8]M3,10 H29 V27 H3 Z" +
            "|[color=#E8A33D;opacity=0.8]M4,5 H13 L14,10 H4 Z" +
            "|[stroke=1.4]M15,6 H23 L24,10"

    const val CLOSE_EDITOR =
        "[viewbox=32;stroke=1.8]M4,6 H24 V20 M4,6 V26 H16" +
            "|[color=#5CB85C]M22,16.5 A7,7 0 1 1 22,30.5 A7,7 0 1 1 22,16.5 Z" +
            "|[stroke=2;color=#FFFFFF]M18.5,23.5 L21,26 L25.5,21"
}
