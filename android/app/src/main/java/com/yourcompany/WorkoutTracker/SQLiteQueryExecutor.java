package com.yourcompany.WorkoutTracker;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Base64;

import com.readystatesoftware.sqliteasset.SQLiteAssetHelper;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Arrays;

import io.flutter.plugin.common.FlutterMethodChannel;

/**
 * Created by silver_android on 05/03/17.
 */

public class SQLiteQueryExecutor extends SQLiteAssetHelper {

    private static final String DB_NAME = "workout-tracker.db";
    private static final int DB_VERSION = 1;

    public SQLiteQueryExecutor(Context context) {
        super(context, DB_NAME, null, DB_VERSION);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {

    }

    void rawQuery(String queryJSON, FlutterMethodChannel.Response response) {
        try {
            JSONObject queryObj = new JSONObject(queryJSON);
            String query = queryObj.getString("query");
            JSONArray paramsArray = queryObj.getJSONArray("params");
            String[] params = new String[paramsArray.length()];
            for (int i = 0, length = paramsArray.length(); i < length; i++) {
                params[i] = paramsArray.getString(i);
            }
            boolean write = queryObj.getBoolean("write");
            boolean executable = queryObj.getBoolean("executable");

            SQLiteDatabase database = write ? getWritableDatabase() : getReadableDatabase();
            if (executable) {
                database.execSQL(query, params);
                response.success("");
            } else {
                Cursor cursor = database.rawQuery(query, params);
                response.success(cursorToJSON(cursor).toString());
            }
        } catch (JSONException e) {
            e.printStackTrace();
            response.error(e.getMessage(), "", "");
        }
    }

    void runTransaction(String queryJSON, FlutterMethodChannel.Response response) {
        try {
            JSONObject queriesObj = new JSONObject(queryJSON);
            JSONArray queries = queriesObj.getJSONArray("queries");
            String[] results = new String[queries.length()];

            boolean write = queriesObj.getBoolean("write");
            SQLiteDatabase database = write ? getWritableDatabase() : getReadableDatabase();
            database.beginTransaction();
            for (int i = 0, length = queries.length(); i < length; i++) {
                JSONObject queryObj = queries.getJSONObject(i);
                String query = queryObj.getString("query");
                JSONArray paramsArray = queryObj.getJSONArray("params");
                String[] params = new String[paramsArray.length()];
                for (int j = 0; j < paramsArray.length(); j++) {
                    params[j] = paramsArray.getString(j);
                }
                Cursor cursor = database.rawQuery(query, params);
                results[i] = cursorToJSON(cursor).toString();
            }
            database.setTransactionSuccessful();
            database.endTransaction();
            response.success(Arrays.toString(results));
        } catch (JSONException e) {
            e.printStackTrace();
            response.error(e.getMessage(), "", "");
        }
    }

    private JSONArray cursorToJSON(Cursor cursor) throws JSONException {
        JSONArray cursorJSON = new JSONArray();
        if (cursor.moveToFirst()) {
            do {
                int numColumns = cursor.getColumnCount();
                JSONObject row = new JSONObject();
                for (int i = 0; i < numColumns; i++) {
                    String columnName = cursor.getColumnName(i);
                    if (columnName != null) {
                        Object val = null;
                        switch (cursor.getType(i)) {
                            case Cursor.FIELD_TYPE_INTEGER:
                                val = cursor.getInt(i);
                                break;
                            case Cursor.FIELD_TYPE_FLOAT:
                                val = cursor.getFloat(i);
                                break;
                            case Cursor.FIELD_TYPE_STRING:
                                val = cursor.getString(i);
                                break;
                            case Cursor.FIELD_TYPE_BLOB:
                                val = Base64.encodeToString(cursor.getBlob(i), Base64.DEFAULT);
                                break;
                        }

                        row.put(columnName, val);
                    }
                }
                cursorJSON.put(row);
            } while (cursor.moveToNext());
        }
        cursor.close();
        return cursorJSON;
    }
}
