package com.android.savingssquad.singleton
import android.content.Context
import android.util.Log
import java.io.File
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteException

import android.database.sqlite.SQLiteOpenHelper

data class BulkOrder(
    val orderId: String,
    val squadId: String
)

class LocalDatabase private constructor(context: Context) :
    SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    companion object {
        private const val DATABASE_NAME = "orders.db"
        private const val DATABASE_VERSION = 1

        private const val TABLE_ORDERS = "orders"
        private const val COL_ID = "id"
        private const val COL_ORDER_ID = "orderId"
        private const val COL_SQUAD_ID = "squadId"

        @Volatile
        private var INSTANCE: LocalDatabase? = null

        fun getInstance(context: Context): LocalDatabase =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: LocalDatabase(context.applicationContext).also { INSTANCE = it }
            }
    }

    override fun onCreate(db: SQLiteDatabase) {
        val createTableSQL = """
            CREATE TABLE IF NOT EXISTS $TABLE_ORDERS(
                $COL_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COL_ORDER_ID TEXT NOT NULL,
                $COL_SQUAD_ID TEXT NOT NULL
            );
        """.trimIndent()

        db.execSQL(createTableSQL)
        Log.d("LocalDatabase", "✅ Orders table ready")
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        // Handle DB upgrade if needed
    }

    // ------------------ Insert Orders ------------------
    fun insertOrders(orders: List<BulkOrder>) {
        writableDatabase.use { db ->
            db.beginTransaction()
            try {
                val sql = "INSERT INTO $TABLE_ORDERS ($COL_ORDER_ID, $COL_SQUAD_ID) VALUES (?, ?);"
                val stmt = db.compileStatement(sql)
                orders.forEach { order ->
                    stmt.bindString(1, order.orderId)
                    stmt.bindString(2, order.squadId)
                    stmt.executeInsert()
                    Log.d("LocalDatabase", "✅ Inserted order: ${order.orderId}")
                    stmt.clearBindings()
                }
                db.setTransactionSuccessful()
            } catch (e: Exception) {
                Log.e("LocalDatabase", "❌ Failed to insert orders: ${e.localizedMessage}")
            } finally {
                db.endTransaction()
            }
        }
    }

    // ------------------ Fetch Orders ------------------
    fun fetchOrders(): List<BulkOrder> {
        val orders = mutableListOf<BulkOrder>()
        readableDatabase.use { db ->
            val cursor = db.rawQuery("SELECT $COL_ORDER_ID, $COL_SQUAD_ID FROM $TABLE_ORDERS;", null)
            cursor.use {
                while (it.moveToNext()) {
                    val orderId = it.getString(it.getColumnIndexOrThrow(COL_ORDER_ID))
                    val squadId = it.getString(it.getColumnIndexOrThrow(COL_SQUAD_ID))
                    orders.add(BulkOrder(orderId, squadId))
                }
            }
        }
        return orders
    }

    // ------------------ Delete Orders ------------------
    fun deleteOrder(orderId: String) {
        writableDatabase.use { db ->
            try {
                val sql = "DELETE FROM $TABLE_ORDERS WHERE $COL_ORDER_ID = ?;"
                val stmt = db.compileStatement(sql)
                stmt.bindString(1, orderId)
                stmt.executeUpdateDelete()
                Log.d("LocalDatabase", "✅ Deleted order: $orderId")
            } catch (e: Exception) {
                Log.e("LocalDatabase", "❌ Failed to delete order: ${e.localizedMessage}")
            }
        }
    }

    fun deleteAllOrders() {
        writableDatabase.use { db ->
            try {
                db.execSQL("DELETE FROM $TABLE_ORDERS;")
                Log.d("LocalDatabase", "✅ All orders deleted")
            } catch (e: Exception) {
                Log.e("LocalDatabase", "❌ Failed to delete all orders: ${e.localizedMessage}")
            }
        }
    }
}