package com.shaua.mocklocation.data;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import com.elvishew.xlog.XLog;

import java.util.ArrayList;
import java.util.List;

/**
 * 收藏位置数据库访问对象
 */
public class FavoriteLocationDao extends SQLiteOpenHelper {

    public static final String TABLE_NAME = "FavoriteLocation";
    public static final String DB_COLUMN_ID = "DB_COLUMN_ID";
    public static final String DB_COLUMN_LOCATION = "DB_COLUMN_LOCATION";
    public static final String DB_COLUMN_LONGITUDE_WGS84 = "DB_COLUMN_LONGITUDE_WGS84";
    public static final String DB_COLUMN_LATITUDE_WGS84 = "DB_COLUMN_LATITUDE_WGS84";
    public static final String DB_COLUMN_TIMESTAMP = "DB_COLUMN_TIMESTAMP";
    public static final String DB_COLUMN_LONGITUDE_BD09 = "DB_COLUMN_LONGITUDE_BD09";
    public static final String DB_COLUMN_LATITUDE_BD09 = "DB_COLUMN_LATITUDE_BD09";

    private static final int DB_VERSION = 1;
    private static final String DB_NAME = "FavoriteLocation.db";
    private static final String CREATE_TABLE = "create table if not exists " + TABLE_NAME +
            " (" + DB_COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
            + DB_COLUMN_LOCATION + " TEXT, "
            + DB_COLUMN_LONGITUDE_WGS84 + " TEXT NOT NULL, "
            + DB_COLUMN_LATITUDE_WGS84 + " TEXT NOT NULL, "
            + DB_COLUMN_TIMESTAMP + " BIGINT NOT NULL, "
            + DB_COLUMN_LONGITUDE_BD09 + " TEXT NOT NULL, "
            + DB_COLUMN_LATITUDE_BD09 + " TEXT NOT NULL)";

    public FavoriteLocationDao(Context context) {
        super(context, DB_NAME, null, DB_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase sqLiteDatabase) {
        sqLiteDatabase.execSQL(CREATE_TABLE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase sqLiteDatabase, int oldVersion, int newVersion) {
        sqLiteDatabase.execSQL("DROP TABLE IF EXISTS " + TABLE_NAME);
        onCreate(sqLiteDatabase);
    }

    /**
     * 保存收藏位置（若 WGS84 经纬度已存在则先删除旧记录）
     */
    public void save(FavoriteLocation location) {
        try (SQLiteDatabase db = getWritableDatabase()) {
            ContentValues values = toContentValues(location);
            db.delete(TABLE_NAME,
                    DB_COLUMN_LONGITUDE_WGS84 + " = ? AND " + DB_COLUMN_LATITUDE_WGS84 + " = ?",
                    new String[]{location.getLongitudeWgs84(), location.getLatitudeWgs84()});
            db.insert(TABLE_NAME, null, values);
        } catch (Exception e) {
            XLog.e("FavoriteLocationDao: save error");
        }
    }

    /**
     * 查询所有收藏位置，按时间倒序
     */
    public List<FavoriteLocation> findAll() {
        List<FavoriteLocation> list = new ArrayList<>();
        try (SQLiteDatabase db = getReadableDatabase();
             Cursor cursor = db.query(TABLE_NAME, null,
                     DB_COLUMN_ID + " > ?", new String[]{"0"},
                     null, null, DB_COLUMN_TIMESTAMP + " DESC", null)) {
            while (cursor.moveToNext()) {
                list.add(fromCursor(cursor));
            }
        } catch (Exception e) {
            XLog.e("FavoriteLocationDao: findAll error");
        }
        return list;
    }

    /**
     * 按 ID 删除
     */
    public void deleteById(int id) {
        try (SQLiteDatabase db = getWritableDatabase()) {
            db.delete(TABLE_NAME, DB_COLUMN_ID + " = ?", new String[]{String.valueOf(id)});
        } catch (Exception e) {
            XLog.e("FavoriteLocationDao: deleteById error");
        }
    }

    /**
     * 删除全部
     */
    public void deleteAll() {
        try (SQLiteDatabase db = getWritableDatabase()) {
            db.delete(TABLE_NAME, null, null);
        } catch (Exception e) {
            XLog.e("FavoriteLocationDao: deleteAll error");
        }
    }

    /**
     * 更新名称
     */
    public void updateName(int id, String name) {
        try (SQLiteDatabase db = getWritableDatabase()) {
            ContentValues values = new ContentValues();
            values.put(DB_COLUMN_LOCATION, name);
            db.update(TABLE_NAME, values, DB_COLUMN_ID + " = ?", new String[]{String.valueOf(id)});
        } catch (Exception e) {
            XLog.e("FavoriteLocationDao: updateName error");
        }
    }

    /**
     * 清理超过指定天数的记录
     */
    public void archiveExpired(long expirationDays) {
        long expirationSeconds = expirationDays * 24 * 60 * 60;
        try (SQLiteDatabase db = getWritableDatabase()) {
            db.delete(TABLE_NAME,
                    DB_COLUMN_TIMESTAMP + " < ?",
                    new String[]{String.valueOf(System.currentTimeMillis() / 1000 - expirationSeconds)});
        } catch (Exception e) {
            XLog.e("FavoriteLocationDao: archiveExpired error");
        }
    }

    private ContentValues toContentValues(FavoriteLocation location) {
        ContentValues values = new ContentValues();
        values.put(DB_COLUMN_LOCATION, location.getName());
        values.put(DB_COLUMN_LONGITUDE_WGS84, location.getLongitudeWgs84());
        values.put(DB_COLUMN_LATITUDE_WGS84, location.getLatitudeWgs84());
        values.put(DB_COLUMN_TIMESTAMP, location.getTimestamp());
        values.put(DB_COLUMN_LONGITUDE_BD09, location.getLongitudeBd09());
        values.put(DB_COLUMN_LATITUDE_BD09, location.getLatitudeBd09());
        return values;
    }

    private FavoriteLocation fromCursor(Cursor cursor) {
        FavoriteLocation location = new FavoriteLocation();
        location.setId(cursor.getInt(0));
        location.setName(cursor.getString(1));
        location.setLongitudeWgs84(cursor.getString(2));
        location.setLatitudeWgs84(cursor.getString(3));
        location.setTimestamp(cursor.getLong(4));
        location.setLongitudeBd09(cursor.getString(5));
        location.setLatitudeBd09(cursor.getString(6));
        return location;
    }
}