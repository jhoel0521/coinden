package com.coinden.app.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.unit.dp

/**
 * Íconos de trazo del diseño "CoinDen" (24×24), como datos de ruta SVG. Sin emojis ni
 * librerías de íconos: los mismos trazos del lienzo.
 */
enum class CoinDenIcon(
    val path: String,
) {
    CHECK_CIRCLE("M12 3a9 9 0 1 0 0 18 9 9 0 0 0 0-18zM8 12.5l3 3 5-6"),
    GAUGE("M4 17a8 8 0 0 1 16 0M12 17l3.5-4.5M4 17h16"),
    ALERT("M12 4l9 16H3zM12 10v4.5M12 17.5v.01"),
    CASH("M3 7h18v10H3zM12 9.5a2.5 2.5 0 1 0 0 5 2.5 2.5 0 0 0 0-5zM6.5 10v4M17.5 10v4"),
    QR("M4 4h6v6H4zM14 4h6v6h-6zM4 14h6v6H4zM14 14h2v2h-2zM18 18h2v2h-2zM14 19h2M19 14h1"),
    CARD("M3 6h18v12H3zM3 10h18M7 15h4"),
    WALLET("M4 6h14a2 2 0 0 1 2 2v10a2 2 0 0 1-2 2H4zM4 6V5a1 1 0 0 1 1-1h11v2M15 13h2"),
    BANK("M3 9l9-5 9 5M5.5 10v8M10 10v8M14 10v8M18.5 10v8M3 20h18"),
    FOOD("M7 3v18M5 3v5a2 2 0 0 0 4 0V3M17 21V3c-2 1.5-3 4.5-3 8h3"),
    SPARKLE("M12 4l1.8 4.7 4.7 1.8-4.7 1.8L12 17l-1.8-4.7-4.7-1.8 4.7-1.8zM19 16l.7 1.6 1.6.7-1.6.7L19 20.6l-.7-1.6-1.6-.7 1.6-.7z"),
    LOCK("M6 11h12v10H6zM8.5 11V8a3.5 3.5 0 0 1 7 0v3"),
    SHIELD("M12 3l8 3v6c0 4.5-3.5 8-8 9-4.5-1-8-4.5-8-9V6z"),
    PLUS("M12 5v14M5 12h14"),
    CLOSE("M6 6l12 12M18 6L6 18"),
    MIC("M12 3a3 3 0 0 1 3 3v6a3 3 0 0 1-6 0V6a3 3 0 0 1 3-3zM5.5 11a6.5 6.5 0 0 0 13 0M12 17.5V21"),
    CAMERA("M4 8h3l2-3h6l2 3h3v11H4zM12 10.5a3.5 3.5 0 1 0 0 7 3.5 3.5 0 0 0 0-7z"),
    IMAGE("M4 5h16v14H4zM4 16l5-5 4 4 2-2 5 5M15.5 9.5v.01"),
    PENCIL("M4 20l1-4L16 5l3 3L8 19zM14 7l3 3"),
    STOP("M7 7h10v10H7z"),
    HELP("M12 3a9 9 0 1 0 0 18 9 9 0 0 0 0-18zM9.5 9.5a2.5 2.5 0 1 1 3.4 2.3c-.6.3-.9.8-.9 1.4v.6M12 16.5v.01"),
    GROCERIES("M3 4h2l2.4 11h11L21 7H6.1M8 19.5a1.5 1.5 0 1 0 3 0 1.5 1.5 0 1 0-3 0zM15.5 19.5a1.5 1.5 0 1 0 3 0 1.5 1.5 0 1 0-3 0z"),
    BUS("M5 4h14v12H5zM5 11h14M7.5 16v3M16.5 16v3M8 13.5v.01M16 13.5v.01"),
    HEALTH("M9.5 4h5v5.5H20v5h-5.5V20h-5v-5.5H4v-5h5.5z"),
    REPEAT("M4 12a8 8 0 0 1 13.7-5.6L20 9M20 4v5h-5M20 12a8 8 0 0 1-13.7 5.6L4 15M4 20v-5h5"),
    BOLT("M13 3L5 14h6l-1 7 8-11h-6z"),
    HOUSE("M3 11l9-7 9 7M5 9.5V20h14V9.5M10 20v-6h4v6"),
    STAR("M12 4l2.5 5 5.5.8-4 3.9.9 5.5L12 16.6l-4.9 2.6.9-5.5-4-3.9 5.5-.8z"),
    BAG("M5 8h14l-1 12H6zM9 8V6.5a3 3 0 0 1 6 0V8"),
    BOOK("M4 5h6a2 2 0 0 1 2 2v13a2 2 0 0 0-2-2H4zM20 5h-6a2 2 0 0 0-2 2v13a2 2 0 0 1 2-2h6z"),
    PEOPLE("M9 11a3 3 0 1 0 0-6 3 3 0 0 0 0 6zM3 20a6 6 0 0 1 12 0M16 11a2.5 2.5 0 1 0 0-5M17.5 14.5A5 5 0 0 1 21 19.5"),
    PERCENT("M5 19L19 5M7 5a2 2 0 1 0 0 4 2 2 0 0 0 0-4zM17 15a2 2 0 1 0 0 4 2 2 0 0 0 0-4z"),
    TAG("M3 12V4h8l10 10-8 8zM7.5 8v.01"),
    MORE("M4 4h6v6H4zM14 4h6v6h-6zM4 14h6v6H4zM14 14h6v6h-6z"),
    HASH("M10 4L8 20M16 4l-2 16M5 9h15M4 15h15"),
    ;

    /** Vector de trazo; el color se aplica al dibujarlo. */
    val vector: ImageVector by lazy {
        ImageVector
            .Builder(name = name, defaultWidth = 24.dp, defaultHeight = 24.dp, viewportWidth = 24f, viewportHeight = 24f)
            .addPath(
                pathData = addPathNodes(path),
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.8f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
            ).build()
    }
}
