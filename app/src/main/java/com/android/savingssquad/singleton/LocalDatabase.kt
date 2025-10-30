package com.android.savingssquad.singleton
import android.content.Context
import android.util.Log
import java.io.File
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteException

data class BulkOrder(
    var orderId: String,
    var groupFundId: String
)

class LocalDatabase private constructor(context: Context) {

    companion object {
        @Volatile
        private var INSTANCE: LocalDatabase? = null

        fun shared(context: Context): LocalDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: LocalDatabase(context.applicationContext).also { INSTANCE = it }
            }
        }
    }

    private var db: SQLiteDatabase? = null

    init {
        try {
            openDatabase(context)
            createTable()
        } catch (e: Exception) {
            Log.e("LocalDatabase", "❌ Database init error: ${e.message}")
        }
    }

    // MARK: - Open Database
    @Throws(Exception::class)
    private fun openDatabase(context: Context) {
        val dir = context.getExternalFilesDir(null) ?: context.filesDir
        val dbFile = File(dir, "orders.sqlite")

        db = SQLiteDatabase.openOrCreateDatabase(dbFile, null)
        Log.d("LocalDatabase", "✅ Database opened at ${dbFile.absolutePath}")
    }

    // MARK: - Create Table
    @Throws(Exception::class)
    private fun createTable() {
        val sql = """
            CREATE TABLE IF NOT EXISTS orders(
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                orderId TEXT NOT NULL,
                groupFundId TEXT NOT NULL
            );
        """.trimIndent()
        execute(sql, "✅ Orders table ready")
    }

    // MARK: - Helper: Execute SQL without results
    @Throws(Exception::class)
    private fun execute(sql: String, successMessage: String? = null) {
        try {
            db?.execSQL(sql)
            successMessage?.let { Log.d("LocalDatabase", it) }
        } catch (e: SQLiteException) {
            throw Exception("SQL execution failed: ${e.message}")
        }
    }

    // MARK: - Insert
    @Throws(Exception::class)
    fun insertOrders(orders: List<BulkOrder>) {
        val sql = "INSERT INTO orders (orderId, groupFundId) VALUES (?, ?);"
        db?.beginTransaction()
        try {
            val stmt = db?.compileStatement(sql)
            orders.forEach { order ->
                stmt?.bindString(1, order.orderId)
                stmt?.bindString(2, order.groupFundId)
                stmt?.executeInsert()
                stmt?.clearBindings()
                Log.d("LocalDatabase", "✅ Inserted order: ${order.orderId}")
            }
            db?.setTransactionSuccessful()
        } catch (e: Exception) {
            throw Exception("Insert failed: ${e.message}")
        } finally {
            db?.endTransaction()
        }
    }

    // MARK: - Fetch
    @Throws(Exception::class)
    fun fetchOrders(): List<BulkOrder> {
        val sql = "SELECT orderId, groupFundId FROM orders;"
        val results = mutableListOf<BulkOrder>()

        try {
            val cursor = db?.rawQuery(sql, null)
            cursor?.use {
                while (it.moveToNext()) {
                    val orderId = it.getString(0)
                    val groupFundId = it.getString(1)
                    results.add(BulkOrder(orderId, groupFundId))
                }
            }
        } catch (e: Exception) {
            throw Exception("Fetch failed: ${e.message}")
        }

        return results
    }

    // MARK: - Delete
    @Throws(Exception::class)
    fun deleteOrder(orderId: String) {
        val sql = "DELETE FROM orders WHERE orderId = ?;"
        try {
            val stmt = db?.compileStatement(sql)
            stmt?.bindString(1, orderId)
            stmt?.executeUpdateDelete()
            Log.d("LocalDatabase", "✅ Deleted order: $orderId")
        } catch (e: Exception) {
            throw Exception("Failed to delete orderId: $orderId (${e.message})")
        }
    }

    @Throws(Exception::class)
    fun deleteAllOrders() {
        execute("DELETE FROM orders;", "✅ All orders deleted")
    }
}