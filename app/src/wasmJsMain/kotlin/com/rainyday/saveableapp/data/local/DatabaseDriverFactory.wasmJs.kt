package com.rainyday.saveableapp.data.local

import app.cash.sqldelight.async.coroutines.awaitCreate
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.worker.WebWorkerDriver
import com.rainyday.saveableapp.db.AppDatabase
import org.w3c.dom.Worker
import kotlin.JsFun

@JsFun(
    "() => new Worker(new URL('@cashapp/sqldelight-sqljs-worker/sqljs.worker.js', import.meta.url), { type: 'module' })"
)
private external fun createSqlJsWorker(): Worker

/** Runs sql.js (SQLite compiled to WASM) inside a Web Worker; storage backed by IndexedDB. */
suspend fun createWasmDatabaseDriver(): SqlDriver {
    val driver = WebWorkerDriver(createSqlJsWorker())
    AppDatabase.Schema.awaitCreate(driver)
    return driver
}
