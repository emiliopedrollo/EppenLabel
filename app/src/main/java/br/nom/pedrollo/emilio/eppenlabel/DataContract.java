package br.nom.pedrollo.emilio.eppenlabel;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.provider.BaseColumns;

public final class DataContract {
    private static final String SQL_CREATE_ENTRIES =
            "CREATE TABLE " + Data.TABLE_NAME + " (" +
                    Data._ID + " INTEGER PRIMARY KEY," +
                    Data.COLUMN_NAME_CODE + " TEXT UNIQUE," +
                    Data.COLUMN_NAME_TEXT + " TEXT)";
    private static final String SQL_DELETE_ENTRIES =
            "DROP TABLE IF EXISTS " + Data.TABLE_NAME;

    private DataContract() {
    }

    public static class Data implements BaseColumns {
        public static final String TABLE_NAME = "entry";
        public static final String COLUMN_NAME_CODE = "code";
        public static final String COLUMN_NAME_TEXT = "text";
        private String code;
        private String text = null;

        public Data(String code, String text) {
            this.code = code;
            this.text = text;
        }

        public Data(String code) {
            this.code = code;
        }

        public String getCode() {
            return code;
        }

        public void setCode(String code) {
            this.code = code;
        }

        public String getText() {
            return text;
        }

        public void setText(String text) {
            this.text = text;
        }
    }

    public static class DataDbHelper extends SQLiteOpenHelper {
        public static final int DATABASE_VERSION = 1;
        public static final String DATABASE_NAME = "Data.db";

        public DataDbHelper(Context context) {
            super(context, DATABASE_NAME, null, DATABASE_VERSION);
        }

        @Override
        public void onCreate(SQLiteDatabase db) {
            db.execSQL(SQL_CREATE_ENTRIES);
        }

        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
            db.execSQL(SQL_DELETE_ENTRIES);
            onCreate(db);
        }

        @Override
        public void onDowngrade(SQLiteDatabase db, int oldVersion, int newVersion) {
            super.onDowngrade(db, oldVersion, newVersion);
        }
    }
}
