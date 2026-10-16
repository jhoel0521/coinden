package com.coinden.app.capture

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaDataSource
import android.media.MediaPlayer
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coinden.composeapp.generated.resources.Res
import coinden.composeapp.generated.resources.evidence_next
import coinden.composeapp.generated.resources.evidence_page
import coinden.composeapp.generated.resources.evidence_pause
import coinden.composeapp.generated.resources.evidence_play
import coinden.composeapp.generated.resources.evidence_previous
import coinden.composeapp.generated.resources.evidence_unavailable
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.rendering.PDFRenderer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jetbrains.compose.resources.stringResource

/**
 * Visor de evidencia en Android, todo en memoria: la imagen se decodifica de sus bytes, el PDF se
 * dibuja con pdfbox página por página y el audio suena desde un [MediaDataSource]. Nada descifrado
 * se escribe en el almacenamiento del teléfono.
 */
class AndroidEvidenceViewer(
    context: Context,
) : EvidenceViewerPort {
    private val appContext = context.applicationContext

    @Composable
    override fun Show(
        attachment: Attachment,
        modifier: Modifier,
    ) {
        when (EvidenceKind.of(attachment.mimeType)) {
            EvidenceKind.IMAGE -> ImageEvidence(attachment.bytes, modifier)
            EvidenceKind.PDF -> PdfEvidence(attachment.bytes, modifier)
            EvidenceKind.AUDIO -> AudioEvidence(attachment.bytes, modifier)
            EvidenceKind.OTHER -> Text(stringResource(Res.string.evidence_unavailable), modifier = modifier)
        }
    }

    @Composable
    private fun ImageEvidence(
        bytes: ByteArray,
        modifier: Modifier,
    ) {
        val bitmap = remember(bytes) { EvidenceRendering.image(bytes) }
        if (bitmap == null) {
            Text(stringResource(Res.string.evidence_unavailable), modifier = modifier)
        } else {
            Box(modifier.verticalScroll(rememberScrollState())) {
                Image(
                    bitmap.asImageBitmap(),
                    contentDescription = null,
                    contentScale = ContentScale.FillWidth,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }

    @Composable
    private fun PdfEvidence(
        bytes: ByteArray,
        modifier: Modifier,
    ) {
        val document = remember(bytes) { EvidenceRendering.pdf(appContext, bytes) }
        DisposableEffect(document) { onDispose { document?.close() } }
        if (document == null) {
            Text(stringResource(Res.string.evidence_unavailable), modifier = modifier)
            return
        }
        var page by remember(document) { mutableIntStateOf(0) }
        var image by remember(document) { mutableStateOf<Bitmap?>(null) }
        LaunchedEffect(document, page) {
            image = null
            image = withContext(Dispatchers.Default) { document.render(page) }
        }
        Column(modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
            val pages = document.pageCount
            if (pages > 1) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = { page-- }, enabled = page > 0, modifier = Modifier.heightIn(min = 48.dp)) {
                        Text(stringResource(Res.string.evidence_previous))
                    }
                    Text(stringResource(Res.string.evidence_page, page + 1, pages), style = MaterialTheme.typography.labelLarge)
                    OutlinedButton(onClick = { page++ }, enabled = page < pages - 1, modifier = Modifier.heightIn(min = 48.dp)) {
                        Text(stringResource(Res.string.evidence_next))
                    }
                }
            }
            Box(Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()), contentAlignment = Alignment.TopCenter) {
                val shown = image
                if (shown == null) {
                    CircularProgressIndicator()
                } else {
                    Image(
                        shown.asImageBitmap(),
                        contentDescription = null,
                        contentScale = ContentScale.FillWidth,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        }
    }

    @Composable
    private fun AudioEvidence(
        bytes: ByteArray,
        modifier: Modifier,
    ) {
        val player = remember(bytes) { EvidenceRendering.player(bytes) }
        var playing by remember(player) { mutableStateOf(false) }
        DisposableEffect(player) {
            player?.setOnCompletionListener { playing = false }
            onDispose { player?.release() }
        }
        if (player == null) {
            Text(stringResource(Res.string.evidence_unavailable), modifier = modifier)
            return
        }
        // El visor ocupa la pantalla; el botón es de tamaño normal, arriba.
        Box(modifier, contentAlignment = Alignment.TopCenter) {
            Button(
                onClick = {
                    if (playing) player.pause() else player.start()
                    playing = !playing
                },
                modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp),
            ) { Text(stringResource(if (playing) Res.string.evidence_pause else Res.string.evidence_play)) }
        }
    }
}

/**
 * El dibujo de la evidencia, sin Compose y todo en memoria (se prueba en el emulador): ningún paso
 * escribe el archivo descifrado en el almacenamiento.
 */
object EvidenceRendering {
    /** Escala de dibujo del PDF: legible a pantalla completa sin usar demasiada memoria. */
    private const val PDF_SCALE = 2f

    fun image(bytes: ByteArray): Bitmap? = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)

    /** El PDF abierto desde sus bytes, o null si no es un PDF. Hay que cerrarlo al terminar. */
    fun pdf(
        context: Context,
        bytes: ByteArray,
    ): PdfPages? {
        PDFBoxResourceLoader.init(context.applicationContext)
        return runCatching { PdfPages(PDDocument.load(bytes)) }.getOrNull()
    }

    /** Un reproductor preparado con el audio en memoria, o null si no se puede reproducir. */
    fun player(bytes: ByteArray): MediaPlayer? =
        runCatching {
            MediaPlayer().apply {
                setDataSource(BytesDataSource(bytes))
                prepare()
            }
        }.getOrNull()

    class PdfPages internal constructor(
        private val document: PDDocument,
    ) : AutoCloseable {
        val pageCount: Int get() = document.numberOfPages

        fun render(page: Int): Bitmap? = runCatching { PDFRenderer(document).renderImage(page, PDF_SCALE) }.getOrNull()

        override fun close() = document.close()
    }

    /** El audio descifrado, leído desde memoria. */
    private class BytesDataSource(
        private val bytes: ByteArray,
    ) : MediaDataSource() {
        override fun readAt(
            position: Long,
            buffer: ByteArray,
            offset: Int,
            size: Int,
        ): Int {
            if (position >= bytes.size) return -1
            val count = minOf(size.toLong(), bytes.size - position).toInt()
            System.arraycopy(bytes, position.toInt(), buffer, offset, count)
            return count
        }

        override fun getSize(): Long = bytes.size.toLong()

        override fun close() = Unit
    }
}
