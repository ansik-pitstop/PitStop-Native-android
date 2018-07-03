package com.pitstop.database

import android.content.ContentValues
import android.database.Cursor
import com.pitstop.models.Dealership
import java.util.*

/**
 * Created by Paul Soladoye on 3/31/2016.
 */
class LocalShopStorage(private val databaseHelper: LocalDatabaseHelper) {

    companion object {

        //SHOP table create statement
        val CREATE_TABLE_DEALERSHIP = ("CREATE TABLE IF NOT EXISTS "
                + TABLES.SHOP.TABLE_NAME + "("
                + TABLES.COMMON.KEY_ID + " INTEGER PRIMARY KEY,"
                + TABLES.SHOP.KEY_NAME + " TEXT, "
                + TABLES.SHOP.KEY_ADDRESS + " TEXT, "
                + TABLES.SHOP.KEY_PHONE + " TEXT, "
                + TABLES.SHOP.KEY_EMAIL + " TEXT, "
                + TABLES.SHOP.KEY_IS_CUSTOM + " INTEGER, "
                + TABLES.COMMON.KEY_OBJECT_ID + " INTEGER, "
                + TABLES.COMMON.KEY_CREATED_AT + " DATETIME" + ")")
    }

    fun getAllDealerships(): List<Dealership> {
        val dealerships = ArrayList<Dealership>()

        val db = databaseHelper.readableDatabase

        val c = db.query(TABLES.SHOP.TABLE_NAME, null, null, null, null, null, null)
        if (c.moveToFirst()) {
            while (!c.isAfterLast) {
                dealerships.add(cursorToDealership(c))
                c.moveToNext()
            }
        }
        c.close()
        return dealerships
    }


    /**
     * Dealership
     */
    fun storeDealership(dealership: Dealership): Long {
        val db = databaseHelper.writableDatabase

        val values = ContentValues()
        values.put(TABLES.COMMON.KEY_OBJECT_ID, dealership.id)
        values.put(TABLES.SHOP.KEY_NAME, dealership.name)
        values.put(TABLES.SHOP.KEY_ADDRESS, dealership.address)
        values.put(TABLES.SHOP.KEY_PHONE, dealership.phone)
        values.put(TABLES.SHOP.KEY_EMAIL, dealership.email)
        values.put(TABLES.SHOP.KEY_IS_CUSTOM, 0)
        return db.insert(TABLES.SHOP.TABLE_NAME, null, values)
    }

    fun storeCustom(dealership: Dealership) {
        val db = databaseHelper.writableDatabase

        val values = ContentValues()
        values.put(TABLES.COMMON.KEY_OBJECT_ID, dealership.id)
        values.put(TABLES.SHOP.KEY_NAME, dealership.name)
        values.put(TABLES.SHOP.KEY_ADDRESS, dealership.address)
        values.put(TABLES.SHOP.KEY_PHONE, dealership.phone)
        values.put(TABLES.SHOP.KEY_EMAIL, dealership.email)
        values.put(TABLES.SHOP.KEY_IS_CUSTOM, 1)
        db.insert(TABLES.SHOP.TABLE_NAME, null, values)

    }

    fun storeDealerships(dealerships: List<Dealership>) {
        for (dealership in dealerships) {
            storeDealership(dealership)
        }
    }

    fun getDealership(shopId: Int): Dealership? {
        val db = databaseHelper.writableDatabase

        val c = db.query(TABLES.SHOP.TABLE_NAME, null,
                TABLES.COMMON.KEY_OBJECT_ID + "=?", arrayOf(shopId.toString()), null, null, null)
        if (c.count == 0) {
            c.close()
            return null
        }

        c.moveToFirst()
        val dealership = cursorToDealership(c)
        c.close()
        return dealership
    }

    /**
     * Delete all dealerships
     */
    fun deleteAllDealerships(): Int {
        val db = databaseHelper.writableDatabase
        return db.delete(TABLES.SHOP.TABLE_NAME, null,null)
    }

    fun removeById(Id: Int) {
        val db = databaseHelper.writableDatabase
        db.delete(TABLES.SHOP.TABLE_NAME, TABLES.COMMON.KEY_OBJECT_ID + "=?",
                arrayOf(Id.toString()))
    }


    private fun cursorToDealership(c: Cursor): Dealership {
        val dealership = Dealership()
        dealership.id = c.getInt(c.getColumnIndex(TABLES.COMMON.KEY_OBJECT_ID))
        dealership.setCustom(c.getInt(c.getColumnIndex(TABLES.SHOP.KEY_IS_CUSTOM)))
        dealership.name = c.getString(c.getColumnIndex(TABLES.SHOP.KEY_NAME))
        dealership.address = c.getString(c.getColumnIndex(TABLES.SHOP.KEY_ADDRESS))
        dealership.setPhoneNumber(c.getString(c.getColumnIndex(TABLES.SHOP.KEY_PHONE)))
        dealership.email = c.getString(c.getColumnIndex(TABLES.SHOP.KEY_EMAIL))

        return dealership
    }

    fun removeAllDealerships() {
        val db = databaseHelper.writableDatabase
        db.delete(TABLES.SHOP.TABLE_NAME, null, null)

    }
}
