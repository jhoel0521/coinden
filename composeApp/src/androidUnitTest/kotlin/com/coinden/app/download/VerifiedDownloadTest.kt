package com.coinden.app.download

import com.sun.net.httpserver.HttpServer
import kotlinx.coroutines.test.runTest
import java.io.File
import java.net.InetSocketAddress
import java.security.MessageDigest
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue

/**
 * Lo que la app promete de toda descarga: se verifica la huella y un archivo que no coincide se
 * borra y no se usa. Se prueba contra un servidor local, sin salir a internet.
 */
class VerifiedDownloadTest {
    private lateinit var server: HttpServer
    private lateinit var carpeta: File
    private val contenido = """{ "formatVersion": 1, "code": "fr" }"""

    private val huellaReal: String
        get() =
            MessageDigest
                .getInstance("SHA-256")
                .digest(contenido.toByteArray())
                .joinToString("") { "%02x".format(it) }

    private val base get() = "http://127.0.0.1:${server.address.port}"

    /** Lo que el servidor recibio: sirve para comprobar que no viaja nada de la persona. */
    private val cabeceras = mutableListOf<Map<String, List<String>>>()
    private val metodos = mutableListOf<String>()

    @BeforeTest
    fun levantar() {
        carpeta =
            File.createTempFile("packs", "").let {
                it.delete()
                it.mkdirs()
                it
            }
        server = HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0)
        server.createContext("/fr.json") { intercambio ->
            cabeceras += intercambio.requestHeaders.mapKeys { it.key.lowercase() }
            metodos += intercambio.requestMethod
            val bytes = contenido.toByteArray()
            intercambio.sendResponseHeaders(200, bytes.size.toLong())
            intercambio.responseBody.use { it.write(bytes) }
        }
        server.start()
    }

    @AfterTest
    fun bajar() {
        cabeceras.clear()
        metodos.clear()
        server.stop(0)
        carpeta.deleteRecursively()
    }

    @Test
    fun `con la huella correcta el archivo queda listo`() =
        runTest {
            val destino = File(carpeta, "fr.json")

            val resultado = VerifiedDownload.to(destino, "$base/fr.json", huellaReal)

            val listo = assertIs<DownloadResult.Ready>(resultado)
            assertEquals(contenido, listo.file.readText())
        }

    @Test
    fun `una descarga alterada se borra y no se usa`() =
        runTest {
            val destino = File(carpeta, "fr.json")
            val huellaQueNoEs = "00".repeat(32)

            val resultado = VerifiedDownload.to(destino, "$base/fr.json", huellaQueNoEs)

            assertEquals(DownloadResult.Corrupted, resultado)
            assertFalse(destino.exists(), "el archivo alterado no puede quedar en el disco")
        }

    @Test
    fun `una descarga alterada no pisa lo que ya estaba bien`() =
        runTest {
            val destino = File(carpeta, "fr.json")
            VerifiedDownload.to(destino, "$base/fr.json", huellaReal)

            VerifiedDownload.to(destino, "$base/fr.json", "11".repeat(32))

            assertTrue(destino.exists(), "lo que ya estaba verificado se conserva")
            assertEquals(contenido, destino.readText())
        }

    @Test
    fun `no queda ningun archivo a medias`() =
        runTest {
            VerifiedDownload.to(File(carpeta, "fr.json"), "$base/fr.json", "22".repeat(32))

            val aMedias = carpeta.listFiles().orEmpty().filter { it.name.endsWith(".part") }
            assertEquals(emptyList(), aMedias)
        }

    @Test
    fun `una direccion que no responde no rompe nada`() =
        runTest {
            val destino = File(carpeta, "fr.json")

            val resultado = VerifiedDownload.to(destino, "$base/no-existe.json", huellaReal)

            assertEquals(DownloadResult.Failed, resultado)
            assertFalse(destino.exists())
        }

    @Test
    fun `la peticion no lleva nada de la persona`() =
        runTest {
            VerifiedDownload.to(File(carpeta, "fr.json"), "$base/fr.json", huellaReal)

            val recibidas = cabeceras.single()
            assertEquals("GET", metodos.single(), "descargar es solo pedir un archivo público")
            listOf("cookie", "authorization", "x-user", "x-device-id").forEach { prohibida ->
                assertFalse(recibidas.containsKey(prohibida), "la petición no puede llevar $prohibida")
            }
        }

    @Test
    fun `informa el avance de la descarga`() =
        runTest {
            val avances = mutableListOf<Long>()

            VerifiedDownload.to(File(carpeta, "fr.json"), "$base/fr.json", huellaReal) { bajado, _ -> avances += bajado }

            assertTrue(avances.isNotEmpty(), "se esperaba al menos un aviso de avance")
            assertEquals(contenido.toByteArray().size.toLong(), avances.last())
        }
}
