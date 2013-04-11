package com.de0.paselist;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import android.annotation.SuppressLint;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteConstraintException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

public class Db {
	private static final String TAG = "Db";

	private static final String sumName = "PASELI 利用総額";

	private static final String DBNAME = "paseli.db";
	private static final String TABLE = "paseli_table";

	private static final String DATE  = "date";
	private static final String ITEM  = "item";
	private static final String POINT = "point";
	private static final String TYPE  = "TYPE";

	private final Context context;
	private DbHelper dbHelper;
	private SQLiteDatabase db;

	// yyyy/mm/dd → yyyy-mm-dd
	private SimpleDateFormat sdf;

	@SuppressLint("SimpleDateFormat")

	public Db(Context context) {
		this.context = context;
		dbHelper = new DbHelper(this.context);
		sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
	}

	private class DbHelper extends SQLiteOpenHelper {

		public DbHelper(Context context) {
			super(context, DBNAME, null, 1);
		}

		@Override
		public void onCreate(SQLiteDatabase db) {

			db.execSQL(
				"CREATE TABLE " + TABLE + " ("
				+ "_id   INTEGER PRIMARY KEY NOT NULL,"
				+ DATE + " TEXT NOT NULL,"
				+ ITEM + " TEXT NOT NULL,"
				+ POINT+ " INTEGER NOT NULL,"
				+ TYPE + " TEXT NOT NULL,"
				+ "UNIQUE("+DATE+","+ITEM+") );"
			);
		}

		@Override
		public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
			// TODO 自動生成されたメソッド・スタブ
		}
	}

	//adapter method
	public Db open(){
		db = dbHelper.getWritableDatabase();
		return this;
	}
	public void close(){
		dbHelper.close();
	}
	public void setTransactionSuccessful(){
		db.setTransactionSuccessful();
	}
	public void beginTransaction(){
		db.beginTransaction();
		Log.v(TAG,"transaction started");
	}
	public void endTransaction(){
		db.endTransaction();
		Log.v(TAG,"transaction ended");
	}


	//get処理用
	public void add(String dateStr, String itemStr, String pointStr) {
		// yyyy/mm/dd → yyyy-mm-dd
		String date = formatDate(dateStr);

		//タイプ判別
		String type = null;
		if ( itemStr.indexOf("チャージ") > -1 ){
			type = "チャージ";
		} else if( itemStr.indexOf("支払") > -1 ) {
			type = "支払";
		}

		//支払(beatmania IIDX 19) → beatmania IIDX 19
		String item = itemStr.replaceAll( "支払\\(", "" ).replaceAll( "\\)", "" );

		// 1,000P → 1000
		Integer point = Integer.valueOf( pointStr.replaceAll("[^0-9]", "") );

		ContentValues val = new ContentValues();
		val.put(DATE, date);
		val.put(ITEM, item);
		val.put(POINT,point);
		val.put(TYPE, type);

		try {
			db.insertOrThrow(TABLE, null, val);
		} catch (SQLiteConstraintException e) {
			Log.v(TAG,"SQLiteConstraintException catched");
		}
	}

	public String formatDate(String dateStr){
		String date = null;
		try {
			date = sdf.format(DateFormat.getDateInstance().parse( dateStr ) );
		} catch (ParseException e) {
			e.printStackTrace();
			Log.v(TAG,"date parse failed : " + dateStr);
		}
		return date;
	}

	//2番目に新しい日付
	public String getNewest(){
//		String query = "SELECT MAX(date) FROM paseli_table where date<(SELECT MAX(date) from paseli_table);";
		String query = "SELECT MAX(date) FROM paseli_table;";
		Cursor cursor = db.rawQuery(query, null);

		cursor.moveToFirst();
		return cursor.getString(0);
	}

	//一番日付新しいのを消す
	public void deleteNewest(){
		//String sql = "DELETE FROM "+TABLE+ " WHERE "+DATE+ "=" +getNewest() ;
		String sql = "DELETE FROM paseli_table WHERE date=(SELECT max(date) FROM paseli_table);";

		db.execSQL(sql);
	}




	//メイン登録用-------------------------------------

	//通常表示
	public List<Map<String,String>> getAll() {
		open();
		//Cursor cursor = db.query(TABLE, null,null,null,null,null,null);
		Cursor cursor = db.rawQuery("select * from paseli_table order by date desc;",null);

		List<Map<String,String>> listData = new ArrayList<Map<String, String>>();

		//context.startManagingCursor(cursor);
		if(cursor.moveToFirst()){
			do{
				Map<String,String> map = new HashMap<String, String>();
				map.put("date" , cursor.getString(cursor.getColumnIndex("date")) );
				map.put("item" , cursor.getString(cursor.getColumnIndex("item")) );
				map.put("point", String.valueOf(cursor.getInt(cursor.getColumnIndex("point")) ) );

				listData.add(map);
			} while(cursor.moveToNext());
		}
		close();

		return listData;
	}

	//日でまとめる(日表示)
	public List<Map<String,String>> getByDate() {
		String sql = "SELECT STRFTIME('%Y-%m-%d',date) AS date,'"+ sumName +"' AS item ,SUM(point) AS point FROM paseli_table WHERE type='支払' GROUP BY date ORDER BY date DESC;";
		return getByQuery(sql);
	}
	//月でまとめる(月表示)
	public List<Map<String,String>> getByMonth() {
		String sql = "SELECT STRFTIME('%Y-%m',date) AS date,'"+ sumName +"' AS item ,SUM(point) AS point FROM paseli_table WHERE type='支払' GROUP BY date ORDER BY date DESC;";
		return getByQuery(sql);
	}
	//年でまとめる(年表示)
	public List<Map<String,String>> getByYear() {
		String sql = "SELECT STRFTIME('%Y',date) AS date,'"+ sumName +"' AS item ,SUM(point) AS point FROM paseli_table WHERE type='支払' GROUP BY date ORDER BY date DESC;";
		return getByQuery(sql);
	}

	//アイテム毎集計（統計ページ）
	public List<Map<String,String>> getByitem() {
		open();
		String query = "SELECT date,item,SUM(point) AS point FROM paseli_table WHERE type='支払' GROUP BY item ORDER BY point DESC;";
		Cursor cursor = db.rawQuery(query, null);

		List<Map<String,String>> listData = new ArrayList<Map<String, String>>();

		//context.startManagingCursor(cursor);
		if(cursor.moveToFirst()){
			do{
				Map<String,String> map = new HashMap<String, String>();
//				map.put("date" , cursor.getString(cursor.getColumnIndex("date")) );
				map.put("item" , cursor.getString(cursor.getColumnIndex("item")) );
				map.put("point", String.valueOf(cursor.getInt(cursor.getColumnIndex("point")) ) );

				listData.add(map);
			} while(cursor.moveToNext());
		}
		close();

		return listData;
	}




	//リスナ登録用-------------------------------------

	//機種でフィルタ
	public List<Map<String,String>> getByFilterItem(String item) {
		String sql = "SELECT date,item,point FROM paseli_table WHERE item='" +item.replaceAll("'", "''")+ "' ORDER BY date DESC;";
		return getByQuery(sql);
	}

	//指定日の内訳
	public List<Map<String,String>> getByFilterDate(String date) {
		String sql = "SELECT STRFTIME('%Y-%m-%d',date) AS date,item ,SUM(point) AS point FROM paseli_table WHERE type='支払' AND STRFTIME('%Y-%m-%d',date)='" + date+ "' GROUP BY item ORDER BY point DESC;";
		return getByQuery(sql);
	}

	//指定月の内訳
	public List<Map<String,String>> getByFilterMonth(String month) {
		String sql = "SELECT STRFTIME('%Y-%m',date) AS date,item ,SUM(point) AS point FROM paseli_table WHERE type='支払' AND STRFTIME('%Y-%m',date)='" + month+ "' GROUP BY item ORDER BY point DESC;";
		return getByQuery(sql);
	}

	//指定年の内訳
	public List<Map<String,String>> getByFilterYear(String year) {
		String sql = "SELECT STRFTIME('%Y',date) AS date,item ,SUM(point) AS point FROM paseli_table WHERE type='支払' AND STRFTIME('%Y',date)='" + year+ "' GROUP BY item ORDER BY point DESC;";
		return getByQuery(sql);
	}

	//月でグループ化、機種でフィルタ
	public List<Map<String,String>> getByFilterItemMonthgroup(String item) {
		String sql = "SELECT STRFTIME('%Y-%m',date) AS date,item,SUM(point) AS point FROM paseli_table WHERE item='" +item.replaceAll("'", "''")+ "' GROUP BY date ORDER BY date DESC;";
		return getByQuery(sql);
	}




	//生SQL実行しリスト返す
	public List<Map<String,String>> getByQuery(String sql) {
		open();
		Cursor cursor = db.rawQuery(sql,null);

		List<Map<String,String>> listData = new ArrayList<Map<String, String>>();

		//context.startManagingCursor(cursor);
		if(cursor.moveToFirst()){
			do{
				Map<String,String> map = new HashMap<String, String>();
				map.put("date" , cursor.getString(cursor.getColumnIndex("date")) );
				map.put("item" , cursor.getString(cursor.getColumnIndex("item")) );
				map.put("point", String.valueOf(cursor.getInt(cursor.getColumnIndex("point")) ) );

				listData.add(map);
			} while(cursor.moveToNext());
		}
		close();

		return listData;
	}


	//クエリそのまま文字列で返す
	public String query(String sql){
		open();
//		Log.v(TAG,"query: "+ sql);

		Cursor cursor = db.rawQuery(sql, null);
		if (cursor.getCount()==0)
			return null;

		cursor.moveToFirst();
		close();

		return cursor.getString(0).replaceFirst("(\\.\\d)\\d+", "$1");
	}



	//連続プレイ日計算
	public String[][] getContinueDate(String item){
		open();

		String filter ="";
		if(item.length() != 0)
			filter = " AND item='" +item.replaceAll("'", "''")+ "' ";

		String sql = "SELECT date FROM paseli_table WHERE type='支払' " +filter+ " GROUP BY date ORDER BY date";
		Cursor cursor = db.rawQuery(sql, null);

		Integer maxContinue = 1;
		Integer maxContinueTmp = 1;
		String  maxContinueDate="";

		Integer maxInterval = 0;
		Integer maxIntervalTmp = 0;
		String  maxIntervalDate="";

		Date prevDate = new Date(0);
		Calendar calendar = Calendar.getInstance();

		if(cursor.moveToFirst()){
			do{
				String dateStr = cursor.getString(cursor.getColumnIndex("date"));
				Date date = null;

				try {
					date = sdf.parse( dateStr );
				} catch (ParseException e) {
					Log.v(TAG,"dateParseError");
					e.printStackTrace();
				}

				//初回だけ
				if (prevDate.getTime() == 0)
					prevDate = date;

				calendar.setTime(prevDate);
				calendar.add(Calendar.DATE, 1);

				//1ループ前の日付+1 が今の日付と一致するか
				if (date.compareTo(calendar.getTime()) == 0){
					//継続日数更新
					maxContinueTmp++;
					if (maxContinue <= maxContinueTmp){
						maxContinue = maxContinueTmp;
						maxContinueDate = dateStr;
					}

					//引退日数リセット
					maxIntervalTmp=0;

				} else {
					//継続日数リセット
					maxContinueTmp=1;

					//引退日数更新
					maxIntervalTmp= (int)( ( date.getTime() - prevDate.getTime() ) / ( 1000 * 60 * 60 * 24) );
					if (maxInterval <= maxIntervalTmp){
						maxInterval = maxIntervalTmp;
						maxIntervalDate = dateStr;
					}
				}

				prevDate=date;
			} while(cursor.moveToNext());
		}
		close();

		return new String[][]{ {maxContinue.toString(),maxContinueDate}, {maxInterval.toString(), maxIntervalDate} };
	}



}
