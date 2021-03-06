package kr.jung.keyboard_hangle.keyboard;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;

public class DictionaryDBHelper extends SQLiteOpenHelper {

    private static final String TAG = "DictionaryDBHelper";

    public DictionaryDBHelper(Context context, String name, SQLiteDatabase.CursorFactory factory, int version) {
        super(context, name, factory, version);
    }

    // DB를 새로 생성할 때 호출되는 함수
    @Override
    public void onCreate(SQLiteDatabase db) {
        // 새로운 테이블 생성
        /* 이름은 DICT 이고, 자동으로 값이 증가하는 _id 정수형 기본키 컬럼과
        item 문자열 컬럼, price 정수형 컬럼, create_at 문자열 컬럼으로 구성된 테이블을 생성. */
        db.execSQL("CREATE TABLE IF NOT EXISTS DICT (_id INTEGER PRIMARY KEY AUTOINCREMENT, word TEXT, input TEXT, lang TEXT, count INTEGER);");
    }

    // DB 업그레이드를 위해 버전이 변경될 때 호출되는 함수
    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {

    }

    public void insertWord(String word, String input, String lang) {
        // 읽고 쓰기가 가능하게 DB 열기
        SQLiteDatabase db = getWritableDatabase();
        // DB에 입력한 값으로 행 추가
        db.execSQL("INSERT INTO DICT VALUES(null, '" + word + "', '" + input + "', '"+ lang+"', 0);");
        db.close();
        Log.i(TAG,"Add Word '"+word+"'"+" ["+input+", "+lang+"]");
    }

    public void clearList() {
        SQLiteDatabase db = getWritableDatabase();
        // 입력한 항목과 일치하는 행 삭제
        db.execSQL("DROP TABLE DICT");
        onCreate(db);
        db.close();
    }

    public List<CandidateInfo> searchWord(String input) {
        Log.i(TAG, "Input Word: "+input);
        ArrayList<CandidateInfo> list = new ArrayList<>();
        if (input.equals("")) {
            return list;
            //When the keyword is empty, return empty list
        }
        SQLiteDatabase db = getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT * FROM DICT WHERE input LIKE '%"+input+"%' ORDER BY count DESC",null);
        CandidateInfo c = new CandidateInfo();
        while(cursor.moveToNext()) {
            Log.i(TAG,"DICT word Load: "+cursor.getString(1));
            c.setWord(cursor.getString(1));
            c.setCount(cursor.getInt(4));
            list.add(c);
        }

        db.close();
        cursor.close();

        return list;
    }

}