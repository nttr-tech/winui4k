package com.appkitbox.winui4k.sample.ribbon.cad

/**
 * Artwork for the CAD demo (strings obtained by running CadArt from the RibbonSpace demo. MIT License, THIRD-PARTY-NOTICES.md).
 *
 * Represents the pictures for the progressive ScreenTips (`*_HELP`, on a 160 grid), block samples (`BLOCK_*`), hatch
 * pattern samples (`HATCH_*`) and visual style samples (`STYLE_*`) as layered paths. Orange marks the result of the
 * operation, blue marks grips.
 */
@Suppress("LargeClass") // A table that gathers the demo artwork definitions in one place
object CadArt {
    const val LINE_HELP =
        "[viewbox=160;stroke=1.2;opacity=0.25]M6,6 L154,6 L154,154 L6,154 Z|[stroke=0.8;opacity=0.12]M6,38 H154 M6,70 H" +
            "154 M6,102 H154 M6,134 H154 M38,6 V154 M70,6 V154 M102,6 V154 M134,6 V154|[stroke=2.6]M28,124 L74,48 L118,104|" +
            "[stroke=2.2;color=#E8A33D]M118,104 L120.69,97.54 M122.62,92.92 L125.31,86.46 M127.23,81.85 L129.92,75.38 M131." +
            "85,70.77 L134.54,64.31 M136.46,59.69 L138,56|[color=#3DA9F5]M24.5,120.5 L31.5,120.5 L31.5,127.5 L24.5,127.5 Z " +
            "M70.5,44.5 L77.5,44.5 L77.5,51.5 L70.5,51.5 Z M114.5,100.5 L121.5,100.5 L121.5,107.5 L114.5,107.5 Z|[stroke=1." +
            "4]M122,56 L134,56 M142,56 L154,56 M138,40 L138,52 M138,60 L138,72 M135,53 L141,53 L141,59 L135,59 Z"

    const val CIRCLE_HELP =
        "[viewbox=160;stroke=1.2;opacity=0.25]M6,6 L154,6 L154,154 L6,154 Z|[stroke=0.8;opacity=0.12]M6,38 H154 M6,70 H" +
            "154 M6,102 H154 M6,134 H154 M38,6 V154 M70,6 V154 M102,6 V154 M134,6 V154|[stroke=2.6]M30,84 A46,46 0 1 1 122," +
            "84 A46,46 0 1 1 30,84 Z|[stroke=2.2;color=#E8A33D]M76,84 L118,60|[color=#3DA9F5]M72.5,80.5 L79.5,80.5 L79.5,87" +
            ".5 L72.5,87.5 Z M118.5,80.5 L125.5,80.5 L125.5,87.5 L118.5,87.5 Z M26.5,80.5 L33.5,80.5 L33.5,87.5 L26.5,87.5 " +
            "Z M72.5,34.5 L79.5,34.5 L79.5,41.5 L72.5,41.5 Z M72.5,126.5 L79.5,126.5 L79.5,133.5 L72.5,133.5 Z|[stroke=1.4]" +
            "M102,60 L114,60 M122,60 L134,60 M118,44 L118,56 M118,64 L118,76 M115,57 L121,57 L121,63 L115,63 Z"

    const val TRIM_HELP =
        "[viewbox=160;stroke=1.2;opacity=0.25]M6,6 L154,6 L154,154 L6,154 Z|[stroke=0.8;opacity=0.12]M6,38 H154 M6,70 H" +
            "154 M6,102 H154 M6,134 H154 M38,6 V154 M70,6 V154 M102,6 V154 M134,6 V154|[stroke=2.2;color=#3DA9F5]M56,30 L56" +
            ",38 M56,43 L56,51 M56,56 L56,64 M56,69 L56,77 M56,82 L56,90 M56,95 L56,103 M56,108 L56,116 M56,121 L56,129 M10" +
            "8,30 L108,38 M108,43 L108,51 M108,56 L108,64 M108,69 L108,77 M108,82 L108,90 M108,95 L108,103 M108,108 L108,11" +
            "6 M108,121 L108,129|[stroke=2.6]M20,80 L56,80 M108,80 L142,80|[stroke=2.2;color=#E8A33D;opacity=0.75]M56,80 L6" +
            "2,80 M67,80 L73,80 M78,80 L84,80 M89,80 L95,80 M100,80 L106,80|[stroke=2.4;color=#E05A5A]M74,62 L90,78 M90,62 " +
            "L74,78|[stroke=1.6]M76,74 L88,74 L88,86 L76,86 Z"

    const val FILLET_HELP =
        "[viewbox=160;stroke=1.2;opacity=0.25]M6,6 L154,6 L154,154 L6,154 Z|[stroke=0.8;opacity=0.12]M6,38 H154 M6,70 H" +
            "154 M6,102 H154 M6,134 H154 M38,6 V154 M70,6 V154 M102,6 V154 M134,6 V154|[stroke=1.6;opacity=0.3]M42,80 L42,4" +
            "0 L82,40|[stroke=2.6]M42,138 L42,80 M82,40 L140,40|[stroke=2.8;color=#E8A33D]M42,80 A40,40 0 0 1 82,40|[stroke" +
            "=1.4;color=#3DA9F5;opacity=0.8]M82,80 L78.46,76.46 M75.64,73.64 L72.1,70.1 M69.27,67.27 L65.74,63.74 M62.91,60" +
            ".91 L59.37,57.37 M56.54,54.54 L53.7,51.7|[color=#3DA9F5]M38.5,76.5 L45.5,76.5 L45.5,83.5 L38.5,83.5 Z M78.5,36" +
            ".5 L85.5,36.5 L85.5,43.5 L78.5,43.5 Z|[color=#3DA9F5]M79.5,80 A2.5,2.5 0 1 1 84.5,80 A2.5,2.5 0 1 1 79.5,80 Z"

    const val HATCH_HELP =
        "[viewbox=160;stroke=1.2;opacity=0.25]M6,6 L154,6 L154,154 L6,154 Z|[stroke=0.8;opacity=0.12]M6,38 H154 M6,70 H" +
            "154 M6,102 H154 M6,134 H154 M38,6 V154 M70,6 V154 M102,6 V154 M134,6 V154|[stroke=1.5;color=#E8A33D]M32,52 L44" +
            ",40 M32,64 L56,40 M32,76 L68,40 M32,88 L80,40 M32,100 L92,40 M32,112 L104,40 M32,124 L116,40 M44,124 L128,40 M" +
            "56,124 L128,52 M68,124 L128,64 M80,124 L128,76 M92,124 L128,88 M104,124 L128,100 M116,124 L128,112|[stroke=2.6" +
            "]M32,40 L128,40 L128,124 L32,124 Z|[stroke=1.4]M64,82 L76,82 M84,82 L96,82 M80,66 L80,78 M80,86 L80,98 M77,79 " +
            "L83,79 L83,85 L77,85 Z"

    const val ARRAY_HELP =
        "[viewbox=160;stroke=1.2;opacity=0.25]M6,6 L154,6 L154,154 L6,154 Z|[stroke=0.8;opacity=0.12]M6,38 H154 M6,70 H" +
            "154 M6,102 H154 M6,134 H154 M38,6 V154 M70,6 V154 M102,6 V154 M134,6 V154|[color=#E8A33D;opacity=0.5]M26,96 L4" +
            "8,96 L48,118 L26,118 Z|[stroke=2.4;color=#E8A33D]M26,96 L48,96 L48,118 L26,118 Z|[stroke=2.2]M64,96 L86,96 L86" +
            ",118 L64,118 Z M102,96 L124,96 L124,118 L102,118 Z M140,96 L162,96 L162,118 L140,118 Z M26,63 L48,63 L48,85 L2" +
            "6,85 Z M64,63 L86,63 L86,85 L64,85 Z M102,63 L124,63 L124,85 L102,85 Z M140,63 L162,63 L162,85 L140,85 Z M26,3" +
            "0 L48,30 L48,52 L26,52 Z M64,30 L86,30 L86,52 L64,52 Z M102,30 L124,30 L124,52 L102,52 Z M140,30 L162,30 L162," +
            "52 L140,52 Z|[color=#3DA9F5]M126,103 L136,107 L126,111 Z M33,26 L37,16 L41,26 Z|[color=#3DA9F5]M33.5,103.5 L40" +
            ".5,103.5 L40.5,110.5 L33.5,110.5 Z"

    const val OFFSET_HELP =
        "[viewbox=160;stroke=1.2;opacity=0.25]M6,6 L154,6 L154,154 L6,154 Z|[stroke=0.8;opacity=0.12]M6,38 H154 M6,70 H" +
            "154 M6,102 H154 M6,134 H154 M38,6 V154 M70,6 V154 M102,6 V154 M134,6 V154|[stroke=2.6]M24,120 L24,60 A24,24 0 " +
            "0 1 48,36 L136,36|[stroke=2.6;color=#E8A33D]M46,120 L46,60 A2,2 0 0 1 48,58 L136,58|[stroke=1.4;color=#3DA9F5]" +
            "M100,36 L100,58 M96.5,41 L100,36 L103.5,41 M96.5,53 L100,58 L103.5,53|[stroke=1.4]M102,84 L114,84 M122,84 L134" +
            ",84 M118,68 L118,80 M118,88 L118,100 M115,81 L121,81 L121,87 L115,87 Z"

    const val POLYLINE_HELP =
        "[viewbox=160;stroke=1.2;opacity=0.25]M6,6 L154,6 L154,154 L6,154 Z|[stroke=0.8;opacity=0.12]M6,38 H154 M6,70 H" +
            "154 M6,102 H154 M6,134 H154 M38,6 V154 M70,6 V154 M102,6 V154 M134,6 V154|[stroke=2.6]M24,120 L56,52 L94,98 A2" +
            "6,26 0 0 1 120,72|[stroke=2.2;color=#E8A33D]M120,72 L123.79,66.11 M126.49,61.91 L130.27,56.02 M132.98,51.81 L1" +
            "36.76,45.92|[color=#3DA9F5]M20.5,116.5 L27.5,116.5 L27.5,123.5 L20.5,123.5 Z M52.5,48.5 L59.5,48.5 L59.5,55.5 " +
            "L52.5,55.5 Z M90.5,94.5 L97.5,94.5 L97.5,101.5 L90.5,101.5 Z M116.5,68.5 L123.5,68.5 L123.5,75.5 L116.5,75.5 Z" +
            "|[stroke=1.4]M122,44 L134,44 M142,44 L154,44 M138,28 L138,40 M138,48 L138,60 M135,41 L141,41 L141,47 L135,47 Z"

    const val MOVE_HELP =
        "[viewbox=160;stroke=1.2;opacity=0.25]M6,6 L154,6 L154,154 L6,154 Z|[stroke=0.8;opacity=0.12]M6,38 H154 M6,70 H" +
            "154 M6,102 H154 M6,134 H154 M38,6 V154 M70,6 V154 M102,6 V154 M134,6 V154|[stroke=2.2;opacity=0.35]M24,76 L64," +
            "76 L64,116 L24,116 Z|[stroke=2.6;color=#E8A33D]M88,36 L128,36 L128,76 L88,76 Z|[stroke=1.4;color=#3DA9F5]M44,9" +
            "6 L49.09,92.82 M52.48,90.7 L57.57,87.52 M60.96,85.4 L66.05,82.22 M69.44,80.1 L74.53,76.92 M77.92,74.8 L83.01,7" +
            "1.62 M86.4,69.5 L91.49,66.32 M94.88,64.2 L99.97,61.02 M103.36,58.9 L108,56|[color=#3DA9F5]M40.5,92.5 L47.5,92." +
            "5 L47.5,99.5 L40.5,99.5 Z|[stroke=1.4]M92,56 L104,56 M112,56 L124,56 M108,40 L108,52 M108,60 L108,72 M105,53 L" +
            "111,53 L111,59 L105,59 Z"

    const val BLOCK_DOOR =
        "[viewbox=32;stroke=1.8]M6,27 L6,7|[stroke=1.4;color=#E8A33D]M6,7 A20,20 0 0 1 26,27|[stroke=1.8]M3,27 H9 M23,2" +
            "7 H29"

    const val BLOCK_WINDOW =
        "[viewbox=32;stroke=1.8]M3,11 H29 M3,21 H29 M3,11 V21 M29,11 V21|[stroke=1.3;color=#3DA9F5]M3,14.5 H29 M3,17.5 " +
            "H29"

    const val BLOCK_CHAIR =
        "[viewbox=32;stroke=1.8]M9,11 H23 V26 A2,2 0 0 1 21,28 H11 A2,2 0 0 1 9,26 Z|[stroke=1.8]M7,5 H25 V9 H7 Z|[colo" +
            "r=#E8A33D;opacity=0.45]M11,13 H21 V26 H11 Z"

    const val BLOCK_TABLE =
        "[viewbox=32;stroke=1.8]M8,16 A8,8 0 1 1 24,16 A8,8 0 1 1 8,16 Z|[stroke=1.5;color=#E8A33D]M13,2.5 H19 V5.5 H13" +
            " Z M13,26.5 H19 V29.5 H13 Z M2.5,13 H5.5 V19 H2.5 Z M26.5,13 H29.5 V19 H26.5 Z"

    const val BLOCK_BED =
        "[viewbox=32;stroke=1.8]M6,4 H26 V28 H6 Z|[stroke=1.5;color=#E8A33D]M8.5,6.5 H14.5 V11 H8.5 Z M17.5,6.5 H23.5 V" +
            "11 H17.5 Z|[stroke=1.4]M6,14 H26 M6,14 L26,20"

    const val BLOCK_TOILET =
        "[viewbox=32;stroke=1.8]M8,4 H24 V10 H8 Z|[stroke=1.8]M10,10 V18 A6,9 0 0 0 22,18 V10|[stroke=1.4;color=#3DA9F5" +
            "]M12.5,17 A3.5,5 0 0 0 19.5,17"

    const val BLOCK_BOLT =
        "[viewbox=32;stroke=1.8]M10,4 H22 L28,16 L22,28 H10 L4,16 Z|[stroke=1.6;color=#E8A33D]M10.5,16 A5.5,5.5 0 1 1 2" +
            "1.5,16 A5.5,5.5 0 1 1 10.5,16 Z|[stroke=1.2;opacity=0.6]M16,2 V30 M2,16 H30"

    const val BLOCK_NUT =
        "[viewbox=32;stroke=1.8]M3,9 H29 V23 H3 Z|[stroke=1.4]M3,13 H29 M3,19 H29|[stroke=1.4;color=#E8A33D]M10,9 V23 M" +
            "22,9 V23"

    const val BLOCK_NORTH_ARROW = "[viewbox=32;stroke=1.8]M16,3 L24,28 L16,22 L8,28 Z|[color=#E8A33D]M16,3 L16,22 L8,28 Z"

    const val BLOCK_TITLE =
        "[viewbox=32;stroke=1.8]M3,5 H29 V27 H3 Z|[stroke=1.3]M16,20 H29 M16,20 V27 M16,23.5 H29 M22,20 V27|[color=#E8A" +
            "33D;opacity=0.7]M18,8 H27 V10 H18 Z"

    const val BLOCK_TREE =
        "[viewbox=32;stroke=1.5]M16,3.5 C22,3.5 27,7.5 27.5,12 C29,15.5 27.5,22 22.5,25 C19.5,28.5 12.5,28.5 9.5,25 C4." +
            "5,22 3,15.5 4.5,12 C5,7.5 10,3.5 16,3.5 Z|[stroke=1.3;color=#5CB85C]M16,16 L16,6 M16,16 L25,14 M16,16 L21,25 M" +
            "16,16 L9,24 M16,16 L7,12|[color=#5CB85C]M14.5,16 A1.5,1.5 0 1 1 17.5,16 A1.5,1.5 0 1 1 14.5,16 Z"

    const val BLOCK_SECTION =
        "[viewbox=32;stroke=1.8]M4,16 A12,12 0 1 1 28,16 A12,12 0 1 1 4,16 Z|[stroke=1.6]M4,16 H28|[color=#E8A33D]M16,4" +
            " L22,10 L10,10 Z"

    const val HATCH_SOLID = "[viewbox=32;color=#E8A33D;opacity=0.9]M3,3 H29 V29 H3 Z|[stroke=1.2]M3,3 H29 V29 H3 Z"

    const val HATCH_ANSI31 =
        "[stroke=1.2;color=#E8A33D]M3,7 L7,3 M3,11 L11,3 M3,15 L15,3 M3,19 L19,3 M3,23 L23,3 M3,27 L27,3 M5,29 L29,5 M9" +
            ",29 L29,9 M13,29 L29,13 M17,29 L29,17 M21,29 L29,21 M25,29 L29,25|[stroke=1.2]M3,3 H29 V29 H3 Z"

    const val HATCH_ANSI37 =
        "[stroke=1.1;color=#E8A33D]M3,8 L8,3 M3,13 L13,3 M3,18 L18,3 M3,23 L23,3 M3,28 L28,3 M7,29 L29,7 M12,29 L29,12 " +
            "M17,29 L29,17 M22,29 L29,22 M27,29 L29,27 M29,8 L24,3 M29,13 L19,3 M29,18 L14,3 M29,23 L9,3 M29,28 L4,3 M25,29" +
            " L3,7 M20,29 L3,12 M15,29 L3,17 M10,29 L3,22 M5,29 L3,27|[stroke=1.2]M3,3 H29 V29 H3 Z"

    const val HATCH_BRICK =
        "[viewbox=32;stroke=1.1;color=#E8A33D]M3,9.5 H29 M3,16 H29 M3,22.5 H29 M10,3 V9.5 M22,3 V9.5 M16,9.5 V16 M4,9.5" +
            " V16 M28,9.5 V16 M10,16 V22.5 M22,16 V22.5 M16,22.5 V29 M4,22.5 V29 M28,22.5 V29|[stroke=1.2]M3,3 H29 V29 H3 Z"

    const val HATCH_CONCRETE =
        "[viewbox=32;color=#E8A33D]M7,7 A1.1,1.1 0 1 1 9.2,7 A1.1,1.1 0 1 1 7,7 Z M17,10 A0.8,0.8 0 1 1 18.6,10 A0.8,0." +
            "8 0 1 1 17,10 Z M24,6 A1,1 0 1 1 26,6 A1,1 0 1 1 24,6 Z M11,17 A0.9,0.9 0 1 1 12.8,17 A0.9,0.9 0 1 1 11,17 Z M" +
            "21,19 A1.2,1.2 0 1 1 23.4,19 A1.2,1.2 0 1 1 21,19 Z M6,25 A0.8,0.8 0 1 1 7.6,25 A0.8,0.8 0 1 1 6,25 Z M15,25 A" +
            "1.1,1.1 0 1 1 17.2,25 A1.1,1.1 0 1 1 15,25 Z M25,26 A0.8,0.8 0 1 1 26.6,26 A0.8,0.8 0 1 1 25,26 Z|[stroke=1;co" +
            "lor=#E8A33D]M5,13 L8,11 L9,14 Z M19,14 L23,13 L21,16 Z M9,21 L12,22 L9.5,24 Z M24,22 L27,21 L26,24 Z|[stroke=1" +
            ".2]M3,3 H29 V29 H3 Z"

    const val HATCH_HONEY =
        "[viewbox=32;stroke=1.1;color=#E8A33D]M3,9 L7,6 L12,9 L12,15 L7,18 L3,15 M12,9 L17,6 L22,9 L22,15 L17,18 L12,15" +
            " M22,9 L27,6 L29,7.5 M22,15 L27,18 L29,16.5 M7,18 L7,24 L12,27 L17,24 L17,18 M17,24 L22,27 L27,24 L27,18 M3,27" +
            " L7,24 M7,6 V3 M17,6 V3 M27,6 V3 M12,27 V29 M22,27 V29|[stroke=1.2]M3,3 H29 V29 H3 Z"

    const val HATCH_GRAVEL =
        "[viewbox=32;stroke=1.1;color=#E8A33D]M5,6 L10,5 L11,9 L6,10 Z M15,4 L20,6 L18,10 L14,8 Z M23,8 L27,6 L28,11 L2" +
            "4,12 Z M6,15 L11,13 L12,18 L7,19 Z M16,14 L21,15 L20,20 L15,18 Z M24,17 L28,18 L26,22 L23,21 Z M5,24 L9,22 L11" +
            ",27 L6,27 Z M14,23 L19,24 L18,28 L13,27 Z M22,25 L27,24 L27,28 L23,28 Z|[stroke=1.2]M3,3 H29 V29 H3 Z"

    const val HATCH_NET =
        "[stroke=1.1;color=#E8A33D]M3,8.2 H29 M3,13.4 H29 M3,18.6 H29 M3,23.8 H29 M8.2,3 V29 M13.4,3 V29 M18.6,3 V29 M2" +
            "3.8,3 V29|[stroke=1.2]M3,3 H29 V29 H3 Z"

    const val HATCH_GRADIENT =
        "[viewbox=32;color=#E8A33D]M5,5 L10.5,5 L10.5,27 L5,27 Z|[color=#E8A33D;opacity=0.7]M10.5,5 L16,5 L16,27 L10.5," +
            "27 Z|[color=#E8A33D;opacity=0.42]M16,5 L21.5,5 L21.5,27 L16,27 Z|[color=#E8A33D;opacity=0.18]M21.5,5 L27,5 L27" +
            ",27 L21.5,27 Z|[stroke=1.8]M5,5 L27,5 L27,27 L5,27 Z"

    const val STYLE2_D_WIREFRAME =
        "[viewbox=32;stroke=1.6]M5,9 L19,9 L19,23 L5,23 Z M13,5 L27,5 L27,19 L13,19 Z M5,9 L13,5 M19,9 L27,5 M19,23 L27" +
            ",19 M5,23 L13,19"

    const val STYLE_HIDDEN = "[viewbox=32;stroke=1.6]M5,11 L19,11 L19,27 L5,27 Z M5,11 L13,5 L27,5 L19,11 M19,27 L27,21 L27,5"

    const val STYLE_SHADED =
        "[viewbox=32;color=#8FA7C4]M5,11 L19,11 L19,27 L5,27 Z|[color=#6E87A6]M19,11 L27,5 L27,21 L19,27 Z|[color=#B5C7" +
            "DC]M5,11 L13,5 L27,5 L19,11 Z|[stroke=1.4]M5,11 L19,11 L19,27 L5,27 Z M5,11 L13,5 L27,5 L19,11 M19,27 L27,21 L" +
            "27,5"

    const val STYLE_REALISTIC =
        "[viewbox=32;color=#C9935A]M5,11 L19,11 L19,27 L5,27 Z|[color=#A0703F]M19,11 L27,5 L27,21 L19,27 Z|[color=#E2B5" +
            "7F]M5,11 L13,5 L27,5 L19,11 Z|[stroke=0.9;color=#7A5230;opacity=0.6]M5,15 H19 M5,19 H19 M5,23 H19"

    const val STYLE_CONCEPTUAL =
        "[viewbox=32;color=#E8A33D;opacity=0.85]M5,11 L19,11 L19,27 L5,27 Z|[color=#3DA9F5;opacity=0.8]M19,11 L27,5 L27" +
            ",21 L19,27 Z|[color=#F2D27A]M5,11 L13,5 L27,5 L19,11 Z|[stroke=1.4]M5,11 L19,11 L19,27 L5,27 Z M5,11 L13,5 L27" +
            ",5 L19,11 M19,27 L27,21 L27,5"

    const val STYLE_X_RAY =
        "[viewbox=32;color=#8FA7C4;opacity=0.35]M5,11 L19,11 L19,27 L5,27 Z M19,11 L27,5 L27,21 L19,27 Z|[stroke=1.4]M5" +
            ",11 L19,11 L19,27 L5,27 Z M5,11 L13,5 L27,5 L19,11 M19,27 L27,21 L27,5|[stroke=1.2;opacity=0.5]M5,27 L13,21 L2" +
            "7,21 M13,21 V5"
}
