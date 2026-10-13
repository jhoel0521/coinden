package com.coinden.core.data.local

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.coinden.core.data.local.db.CoinDenDatabase
import java.util.Properties

/**
 * Base local en la JVM (pruebas hoy; Desktop después). Sin cifrado: el cifrado vive en el
 * adaptador de cada plataforma. [path] null = base en memoria.
 */
object JvmDatabase {
    fun driver(path: String? = null): SqlDriver =
        JdbcSqliteDriver(if (path == null) JdbcSqliteDriver.IN_MEMORY else "jdbc:sqlite:$path", Properties(), CoinDenDatabase.Schema)

    fun open(driver: SqlDriver): CoinDenDatabase = CoinDenDatabase(driver)
}
