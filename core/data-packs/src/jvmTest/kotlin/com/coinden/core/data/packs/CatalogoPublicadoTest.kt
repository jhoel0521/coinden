package com.coinden.core.data.packs

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import java.io.File
import java.security.MessageDigest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Lo que se publica en el sitio para que la app pueda descargarlo.
 *
 * Publicar los mismos paquetes que viajan con la app no es redundante: es lo que permite
 * **corregir una palabra mal traducida sin publicar una versión nueva**, porque un paquete
 * descargado reemplaza al que viajaba dentro. Cuando haya un idioma nuevo, se suma aquí.
 *
 * El catálogo se arma de los archivos reales, así que su huella y su tamaño no se pueden
 * desincronizar a mano. La prueba falla si alguien publica un catálogo que no corresponde.
 */
class CatalogoPublicadoTest {
    private val origen = File("src/commonMain/resources/packs")
    private val publicado = File("../../site/packs")
    private val json = Json { prettyPrint = true }

    @BeforeTest
    fun publicar() {
        publicado.mkdirs()

        val archivos =
            origen
                .listFiles()
                .orEmpty()
                .filter { it.extension == "json" && it.name != INDEX }
                .sortedBy { it.name }
        assertTrue(archivos.isNotEmpty(), "no hay nada que publicar en ${origen.path}")

        archivos.forEach { it.copyTo(File(publicado, it.name), overwrite = true) }

        val entradas =
            archivos.map { archivo ->
                val bytes = archivo.readBytes()
                buildJsonObject {
                    put("id", JsonPrimitive(archivo.nameWithoutExtension))
                    put("kind", JsonPrimitive(if (archivo.name.startsWith("template-")) "TEMPLATE" else "LANGUAGE"))
                    put("url", JsonPrimitive("$BASE_URL/${archivo.name}"))
                    put("sha256", JsonPrimitive(sha256(bytes)))
                    put("sizeBytes", JsonPrimitive(bytes.size))
                }
            }

        File(publicado, "catalog.json").writeText(
            json.encodeToString(
                JsonObject.serializer(),
                buildJsonObject {
                    put("formatVersion", JsonPrimitive(1))
                    put("entries", JsonArray(entradas))
                },
            ),
        )
    }

    @Test
    fun `el sitio publica un paquete por cada uno de los que trae la app`() {
        val enLaApp = origen.listFiles().orEmpty().count { it.extension == "json" && it.name != INDEX }

        assertEquals(enLaApp, PackCatalog.parse(File(publicado, "catalog.json").readText()).size)
    }

    @Test
    fun `el catalogo publicado se puede leer y apunta a archivos que existen`() {
        val catalogo = File(publicado, "catalog.json")
        assertTrue(catalogo.isFile, "falta ${catalogo.path}")

        val entradas = PackCatalog.parse(catalogo.readText())
        assertTrue(entradas.isNotEmpty(), "el catálogo publicado no ofrece nada")

        entradas.forEach { entrada ->
            val archivo = File(publicado, entrada.fileName)
            assertTrue(archivo.isFile, "el catálogo ofrece ${entrada.fileName} y no está publicado")
            assertEquals(sha256(archivo.readBytes()), entrada.sha256, "la huella de ${entrada.fileName} no corresponde")
            assertEquals(archivo.length(), entrada.sizeBytes, "el tamaño de ${entrada.fileName} no corresponde")
            assertTrue(entrada.url.startsWith("https://"), "${entrada.fileName} no se ofrece por HTTPS")
        }
    }

    @Test
    fun `lo publicado dice lo mismo que lo que viaja con la app`() {
        origen
            .listFiles()
            .orEmpty()
            .filter { it.extension == "json" && it.name != INDEX }
            .forEach { archivo ->
                val afuera = File(publicado, archivo.name)
                assertTrue(afuera.isFile, "falta publicar ${archivo.name}")
                assertEquals(archivo.readText(), afuera.readText(), "${archivo.name} publicado no coincide con el de la app")
            }
    }

    private fun sha256(bytes: ByteArray): String = MessageDigest.getInstance("SHA-256").digest(bytes).joinToString("") { "%02x".format(it) }

    private companion object {
        const val BASE_URL = "https://jhoel0521.github.io/coinden/packs"
    }
}
