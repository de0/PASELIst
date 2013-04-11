package com.de0.paselist;

import java.util.Calendar;


public class GetStat {

	//統計作成し文字列を返す(フルと共有用の配列)
	public static String[] getStr(Db db, String item){
		String[] retStr = {"",""};

		//未取得なら中止
		final String cnt = db.query("SELECT COUNT() FROM PASELI_TABLE;");
		if ( cnt.equals("0") | item.equals("チャージ"))
			return retStr;


		String stat = "統計 - ";

		//アイテムで絞るかどうか
		String filter = "";
		if (item.length() != 0){
			filter = " AND item='" +item.replaceAll("'", "''")+ "' ";
			stat += item;
		}else{
			stat += "全機種";
		}


		stat += "\n\n";


		//計算用--------------------------
		//初使用日
		String day_first = db.query("SELECT MIN(date) FROM (SELECT date,SUM(point) AS point FROM paseli_table WHERE type='支払' "+filter+" GROUP BY date);");
		//初使用からの経過日数
		String day_total = db.query("SELECT JULIANDAY('now')-JULIANDAY(MIN(date)) FROM (SELECT date,SUM(point) AS point FROM paseli_table WHERE type='支払' "+filter+" GROUP BY date);");


		//金額関連--------------------------
		//総額
		String total   = db.query("SELECT SUM(point) FROM paseli_table WHERE type='支払' "+filter+";");

		//平均額（プレイした日の）
		String average = db.query("SELECT AVG(point) FROM (SELECT date,SUM(point) AS point FROM paseli_table WHERE type='支払' "+filter+" GROUP BY date);");

		//一日で使った最高額と日付
		String max     = db.query("SELECT MAX(point) FROM (SELECT date,SUM(point) AS point FROM paseli_table WHERE type='支払' "+filter+" GROUP BY date);");
		String maxdate = db.query("SELECT date FROM (SELECT date,SUM(point) AS point FROM paseli_table WHERE type='支払' "+filter+" GROUP BY date) WHERE point=(SELECT MAX(point) FROM (SELECT date,SUM(point) AS point FROM paseli_table WHERE type='支払' "+filter+" GROUP BY date));");

		//プレイした日数
		String day_played= db.query("SELECT count(date) FROM (SELECT date,SUM(point) AS point FROM paseli_table WHERE type='支払' "+filter+" GROUP BY date);");

		//プレイ頻度
		String day_freq1 = String.valueOf(Float.parseFloat(day_played) / Float.parseFloat(day_total) *30);
		String day_freq2 = String.valueOf(Float.parseFloat(day_total)  / Float.parseFloat(day_played) );

		stat += "　　　　利用総額：" + total + " P\n";
		stat += "　　　平均利用額：" + average + " P/プレイ日\n";
		stat += "　　　最高利用額：" + max + " P/日 (" + maxdate + ")\n";

		stat += "　　　プレイ日数：" + day_played + "日\n";
		stat += "　　　プレイ頻度：" + formatFloat(day_freq1) + " 日/月 (" +formatFloat(day_freq2)+ " 日間隔)\n";

		stat += "\n";


		//チャージ関連(フィルタ時はなし)--------------------------
		if (item.length() == 0){
			//総額
			String charge_total   = db.query("SELECT SUM(point) FROM paseli_table WHERE type='チャージ';");

			//平均額（プレイした日の）
			String charge_average = db.query("SELECT AVG(point) FROM (SELECT date,SUM(point) AS point FROM paseli_table WHERE type='チャージ' GROUP BY date);");

			//一日で使った最高額と日付
			String charge_max     = db.query("SELECT MAX(point) FROM (SELECT date,SUM(point) AS point FROM paseli_table WHERE type='チャージ' GROUP BY date);");
			String charge_maxdate = db.query("SELECT date FROM (SELECT date,SUM(point) AS point FROM paseli_table WHERE type='チャージ' GROUP BY date) WHERE point=(SELECT MAX(point) FROM (SELECT date,SUM(point) AS point FROM paseli_table WHERE type='チャージ' GROUP BY date));");

			//プレイした日数
			String day_charged= db.query("SELECT count(date) FROM (SELECT date,SUM(point) AS point FROM paseli_table WHERE type='チャージ' GROUP BY date);");

			//プレイ頻度
			String charge_day_freq1 = String.valueOf(Float.parseFloat(day_charged) / Float.parseFloat(day_total) *30);
			String charge_day_freq2 = String.valueOf(Float.parseFloat(day_total) /Float.parseFloat(day_charged) );

			stat += "　　総チャージ額：" + charge_total + " P\n";
			stat += "　平均チャージ額：" + charge_average + " P/日\n";
			stat += "　最高チャージ額：" + charge_max + " P/日 (" + charge_maxdate + ")\n";

			stat += "　　チャージ日数：" + day_charged + "日\n";
			stat += "　　チャージ頻度：" + formatFloat(charge_day_freq1) + " 日/月 (" +formatFloat(charge_day_freq2)+ " 日間隔)\n";

			stat += "\n";
		}


		//プレイ関連--------------------------

		//総時間
		String playtime = String.valueOf( Integer.parseInt(total) / 10 / 60 );

		//一番プレイした機種とポイント
		String maxplayitem = db.query("SELECT item,SUM(point) AS point FROM paseli_table WHERE type='支払' "+filter+" GROUP BY item ORDER BY point DESC;");
		String maxplayitem_point = db.query("SELECT SUM(point) AS point,item  FROM paseli_table WHERE type='支払' "+filter+" GROUP BY item ORDER BY point DESC;");

		//プレイする曜日(回数基準)
		String maxweekday = db.query("SELECT STRFTIME('%w',date) AS day, COUNT(point) AS point  FROM (SELECT date, SUM(point) AS point FROM paseli_table WHERE type='支払' "+filter+" GROUP BY date) GROUP BY day ORDER BY point DESC;");
//		String minweekday = db.query("SELECT STRFTIME('%w',date) AS day, COUNT(point) AS point  FROM (SELECT date, SUM(point) AS point FROM paseli_table WHERE type='支払' "+filter+" GROUP BY date) GROUP BY day ORDER BY point;");
		//プレイする曜日(金額基準)
//		String maxweekday = db.query("SELECT STRFTIME('%w',date) AS day, SUM(point) AS point  FROM (SELECT date, SUM(point) AS point FROM paseli_table WHERE type='支払' "+filter+" GROUP BY date) GROUP BY day ORDER BY point DESC;");
//		String minweekday = db.query("SELECT STRFTIME('%w',date) AS day, SUM(point) AS point  FROM (SELECT date, SUM(point) AS point FROM paseli_table WHERE type='支払' "+filter+" GROUP BY date) GROUP BY day ORDER BY point;");

		//最終使用日
		String day_last = db.query("SELECT MAX(date) FROM (SELECT date,SUM(point) AS point FROM paseli_table WHERE type='支払' "+filter+" GROUP BY date);");
		//最終日からの経過日数
		String day_last_count = db.query("SELECT JULIANDAY('now')-JULIANDAY(MAX(date)) FROM (SELECT date,SUM(point) AS point FROM paseli_table WHERE type='支払' "+filter+" GROUP BY date);");

		//プレイ期間
		String[][] daycount    = db.getContinueDate(item);
		String maxcontinue     = daycount[0][0];
		String maxcontinuedate = daycount[0][1];
		String maxinterval     = daycount[1][0];
		String maxintervaldate = daycount[1][1];

		if (! maxcontinuedate.equals("")){
			maxcontinuedate = " (～"+ maxcontinuedate+ ")";
		}
		if (! maxintervaldate.equals("")){
			maxintervaldate = " (～"+ maxintervaldate+ ")";
		}


		stat += "　概算プレイ時間：" + playtime + " 時間\n";
		stat += "　最頻プレイ曜日：" + formatWeekday(maxweekday) + "曜日\n";
		stat += "　最長連続プレイ：" + maxcontinue + "日" +maxcontinuedate+ "\n";
		stat += "最長未プレイ間隔：" + maxinterval + "日" +maxintervaldate+ "\n";
		stat += "　　初回プレイ日：" + day_first + " (" +day_total.replaceFirst("\\.\\d+", "")+ " 日前)\n";
		stat += "　　最終プレイ日：" + day_last +  " (" +day_last_count.replaceFirst("\\.\\d+", "")+ " 日前)\n";

		if (item.length() == 0)
			stat += "　最多プレイ機種：" + maxplayitem + " (" +maxplayitem_point+ " P)\n";


		stat += "\n";


		//金額予想--------------------------
		//今月の金額
		String use_month = db.query("SELECT SUM(point) AS point,STRFTIME('%Y-%m',date) AS date FROM paseli_table WHERE type='支払' "+filter+" GROUP BY date HAVING date>=STRFTIME('%Y-%m','now');");
		String use_year  = db.query("SELECT SUM(point) AS point,STRFTIME('%Y',date) AS date FROM paseli_table WHERE type='支払' "+filter+" GROUP BY date HAVING date>=STRFTIME('%Y','now');");

		if (use_month==null){ use_month="0"; };
		if (use_year==null){ use_year="0"; };

		//金額予測
		Calendar calendar = Calendar.getInstance();
		String yoso_month = String.valueOf( Integer.parseInt(use_month) * 30 / calendar.get(Calendar.DATE) );
		String yoso_year  = String.valueOf( Integer.parseInt(use_year ) *365 / calendar.get(Calendar.DAY_OF_YEAR) );

		use_month="";
		use_year="";

		stat += "利用予想額－今月：" + yoso_month + " P\n";
		stat += "利用予想額－今年：" + yoso_year + " P";



		//共有用文字列
		String sharestr;

		sharestr = "#PASELIst 統計\n";
		if (item.length() != 0)
			sharestr+= "【" + item+ "】\n";
		sharestr+="\n";

		sharestr+= "総額：" + total + "P\n";
		sharestr+= "平均：" + average.replaceFirst("\\.\\d+", "") + "/日\n";
		sharestr+= "最高：" + max + "/日\n";
		sharestr+= "日数：" + day_played + "日\n";
		sharestr+= "頻度：" + formatFloat(day_freq2)+ "日間隔\n";
		sharestr+= "\n";
		sharestr+= "総プレイ：" + playtime + "時間\n";
		sharestr+= "最長連続：" + maxcontinue + "日\n";
		sharestr+= "最長引退：" + maxinterval + "日\n";

		if (item.length() == 0)
			sharestr+= "最多機種：" + maxplayitem + " (" +maxplayitem_point+ "P)\n";


		retStr[0]=stat;
		retStr[1]=sharestr;

		return retStr;
	}

	//文字列整形関連
	//曜日変換
	public static String formatWeekday(String s){
		switch(Integer.parseInt(s)){
			case 0: return "日";
			case 1: return "月";
			case 2: return "火";
			case 3: return "水";
			case 4: return "木";
			case 5: return "金";
			case 6: return "土";
			default:return null;
		}
	}
	//小数2桁目以降削除
	private static String formatFloat(String s){
		return s.replaceFirst("(\\.\\d)\\d+", "$1");
	}

}

