package com.example.controliot

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import java.security.MessageDigest

class DatabaseHelper(context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    companion object {
        private const val DATABASE_NAME = "IoTControl.db"
        private const val DATABASE_VERSION = 1


        private const val TABLE_USERS = "usuarios"
        private const val COLUMN_USER_ID = "id"
        private const val COLUMN_USERNAME = "username"
        private const val COLUMN_PASSWORD = "password"


        private const val TABLE_LOGS = "historial"
        private const val COLUMN_LOG_ID = "id"
        private const val COLUMN_TEMP = "temperatura"
        private const val COLUMN_STATE = "estado_ventilador"
        private const val COLUMN_DATE = "fecha_registro"
    }

    override fun onCreate(db: SQLiteDatabase?) {
        val createUsersTable = ("CREATE TABLE " + TABLE_USERS + " ("
                + COLUMN_USER_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
                + COLUMN_USERNAME + " TEXT UNIQUE, "
                + COLUMN_PASSWORD + " TEXT)")
        db?.execSQL(createUsersTable)


        val createLogsTable = ("CREATE TABLE " + TABLE_LOGS + " ("
                + COLUMN_LOG_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
                + COLUMN_TEMP + " REAL, "
                + COLUMN_STATE + " TEXT, "
                + COLUMN_DATE + " DATETIME DEFAULT CURRENT_TIMESTAMP)")
        db?.execSQL(createLogsTable)


        val defaultUser = ContentValues().apply {
            put(COLUMN_USERNAME, "admin")
            put(COLUMN_PASSWORD, hashPassword("admin123"))
        }
        db?.insert(TABLE_USERS, null, defaultUser)
    }

    override fun onUpgrade(db: SQLiteDatabase?, oldVersion: Int, newVersion: Int) {
        db?.execSQL("DROP TABLE IF EXISTS $TABLE_USERS")
        db?.execSQL("DROP TABLE IF EXISTS $TABLE_LOGS")
        onCreate(db)
    }

    private fun hashPassword(password: String): String {
        val bytes = password.toByteArray()
        val md = MessageDigest.getInstance("SHA-256")
        val digest = md.digest(bytes)
        return digest.fold("") { str, it -> str + "%02x".format(it) }
    }

    fun checkUser(username: String, pass: String): Boolean {
        val db = readableDatabase
        val hashedPass = hashPassword(pass)
        val query = "SELECT * FROM $TABLE_USERS WHERE $COLUMN_USERNAME = ? AND $COLUMN_PASSWORD = ?"
        val cursor = db.rawQuery(query, arrayOf(username, hashedPass))
        val exists = cursor.count > 0
        cursor.close()
        return exists
    }


    fun insertLog(temp: Float, state: String): Boolean {
        val db = writableDatabase
        val values = ContentValues().apply {
            put(COLUMN_TEMP, temp)
            put(COLUMN_STATE, state)
        }
        val result = db.insert(TABLE_LOGS, null, values)
        return result != -1L
    }
}