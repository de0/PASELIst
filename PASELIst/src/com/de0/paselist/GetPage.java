package com.de0.paselist;

import java.io.File;
import java.io.IOException;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.preference.PreferenceManager;
import android.util.Log;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.Toast;


@SuppressLint("SetJavaScriptEnabled")
public class GetPage extends AsyncTask<String, Integer, String>{
	private static final int TIMEOUT = 45;
	private static final String TAG = "GetPage";

	//スピナーに一時的に出す文字
	private static final String loadStr = "読込中...";

	//myKONAMIとpaseliのページタイトル
	private static final String titleMykonami = "my KONAMI";
	private static final String titlePaseli = "PASELI";

	private WebView web;
	private Context context;
	private SharedPreferences sp;
	private Db db;

	private Spinner spinner;
	private ArrayAdapter<String> spadapter;
	private Integer spselected;

	private ProgressDialog progress = null;

	private String message;

	private String user;
	private String pass;
	private String otp;

	public GetPage(Context context, WebView webView, Spinner spi,ArrayAdapter<String> spa, String passwd, String otpstr){
		this.web = webView;
		this.context = context;
		this.sp = PreferenceManager.getDefaultSharedPreferences(context);

		this.db = new Db(context);

		this.spinner = spi;
		this.spadapter = spa;

		this.pass = passwd;
		this.otp = otpstr;
	}

	@Override
	protected void onPreExecute(){
		progress = new ProgressDialog(context);
		progress.setTitle("ログイン処理中");
		progress.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
		progress.setMax(5);
		progress.setProgress(0);
		progress.setCancelable(false);
		progress.show();

//		new WebView;
		web.setWebViewClient(new WebViewClient());
		web.getSettings().setJavaScriptEnabled(true);
		web.getSettings().setLoadsImagesAutomatically(false);
		web.addJavascriptInterface(new JsInterface(context),"Android");
		web.getSettings().setSavePassword(false);
		web.getSettings().setSaveFormData(false);

		user = sp.getString("user","");

		spselected = spinner.getSelectedItemPosition();
		spadapter.add(loadStr);
		spinner.setSelection(spinner.getCount()-1);

		message = "";
	}

	@Override
	protected String doInBackground(String... arg0) {
		db.open();

		//MyKONAMI
		publishProgress(1);
		pageTransition("https://my.konami.net/login.do", titleMykonami);
		if (isCancelled()){
//			message += "https://my.konami.net/login.do への接続に失敗\n";
			return "loginfail";
		}


		//ログイン
		publishProgress(2);
		web.loadUrl("javascript:(function(){document.mainForm.strPassword.value='" + pass + "';})();");
		web.loadUrl("javascript:(function(){document.mainForm.strKonamiid.value='" + user + "';})();");
		if (sp.getBoolean("useotp",false))
			web.loadUrl("javascript:(function(){document.mainForm.strOtpPassword.value='" + otp + "';})();");

		pageTransition("javascript:document.mainForm.submit();", titleMykonami);
		if (isCancelled()){
//			message += "my.konami.net 接続失敗\n";
			return "loginfail";
		}

		//PASELI
		publishProgress(3);
		pageTransition("javascript:goRedirect('/paseli/login.kc');", titlePaseli);
		if (isCancelled()){
			message += "\nPASELIチャージサイト接続失敗\n"
					+"・ID/パスが正しいか確認して下さい\n";
			return "loginfail";
		}

		//購入履歴
		publishProgress(4);
		pageTransition("javascript:location.href='payinfo.kc';", titlePaseli);
		if (isCancelled()){
//			message += "PASELI履歴ページ 接続失敗\n";
			return "loginfail";
		}

		//1ページ目
		publishProgress(5);
		pageTransition("javascript:Android.getSource(document.documentElement.outerHTML);", titlePaseli);
		Document doc = getDoc();

		Integer maxPages = 0;
		for (Element links : doc.select("table.buy_table a") ){
			if ( links.outerHtml().indexOf("goPage") > -1 ){
				maxPages = Integer.parseInt(links.text() );
			}
		}

		//残額保存
		String paseli = doc.select("p.money").text().replaceAll("[^0-9]", "");
		sp.edit().putString(Info.key_totalpaseli, paseli).commit();


		//progressをソース取得用に
		publishProgress(1,maxPages);

		//購入履歴取得
		db.beginTransaction();
		try{
			// 最新の一日消す
			db.deleteNewest();

			//2番目に新しい日
			String newest = db.getNewest();
			Log.v(TAG,"newest:"+newest);

			for(Integer p=1; p<=maxPages; p++){
				publishProgress(p);

				pageTransition("javascript:Android.getSource(document.documentElement.outerHTML);", titlePaseli);
				pageTransition("javascript:goNext()", titlePaseli);
				Log.v(TAG, "page:"+p.toString()+ "/"+ maxPages.toString());

				if (isCancelled()){
					Log.v(TAG,"cancelled:データ取得失敗");
					return "pagegetfail";
				}

				//既存のデータまで行ったら終わり
				if ( dbadd(newest) == false)
					break;
			}

			publishProgress(maxPages);
			db.setTransactionSuccessful();
		} finally {
			db.endTransaction();
		}

		return "取得成功";
	}


	protected void onPostExecute(String result){
		db.close();

		Log.v(TAG, "onpostres:"+result);
		if(progress != null){
			progress.dismiss();
			progress = null;
		}

		spadapter.remove(loadStr);
		spinner.setSelection(spselected);


		Toast.makeText(context, result, Toast.LENGTH_LONG).show();

	}

	protected void onProgressUpdate(Integer... i){
		progress.setProgress(i[0]);

		if(i.length == 2){
			progress.setMax(i[1]);
			progress.setTitle("データ取得中");
		}
}

	@Override
	public void onCancelled() {
		db.close();

		Log.v(TAG,"cancelled");

		//プログレス消して
		if(progress != null){
			progress.dismiss();
			progress = null;
		}

		spadapter.remove(loadStr);
		spinner.setSelection(spselected);


		message += "\nその他主な要因\n"
					+"・ネットワークが利用できない\n"
					+"・途中で回線が切替った(3G/Wifi)\n"
					+"・サーバが落ちてる\n"
					+"・端末のRAM不足";

		//ダイアログ出す
		AlertDialog.Builder alert = new AlertDialog.Builder(context);
		alert.setTitle("ページ取得失敗 \n(" + web.getTitle()+ ")");
		alert.setMessage(message);
		alert.setPositiveButton("OK",null);
		alert.show();
	}

	//jsoupで開く
	protected Document getDoc(){
		File in = new File(context.getFilesDir()+ "/" + Info.tempfile);

		Document doc = null;
		try {
			doc = Jsoup.parse(in,"UTF-8");
		} catch (IOException e) {
			Log.v(TAG,"jsoup error");
			e.printStackTrace();
		}

		return doc;
	}

	protected Boolean dbadd(String newestDate){
		Document doc = getDoc();

		//テーブル解析
		for( Element table : doc.select("table.buy_table") ){
			for( Element row : table.select("tr") ){
				Elements col = row.select("td");

				//日付, 内容, ポイント, 詳細
				if (col.size() != 4 ) continue;

				String date  = col.get(0).text();
				String item  = col.get(1).text();
				String point = col.get(2).text();

				if (db.formatDate(date).equals(newestDate) )
					return false;

				db.add(date, item, point);
			}
		}
		return true;
	}



	//ページ移動させる処理
	//遷移先URL, 遷移後のtitle
	private void pageTransition(String url, String title){
		Log.v(TAG,"pageTransstart : "+title );

		web.loadUrl(url);

		//タイムアウトまで待つ
		int t = 0;
		while (t < TIMEOUT){
			try { Thread.sleep(1000);} catch (InterruptedException e) {e.printStackTrace();}
			if (web.getProgress() == 100) break;
			t++;
		}




		//タイムアウトなら終わり
		if (t >= TIMEOUT){
			Log.v(TAG,"pageTrans timeout" + title + " / " + web.getTitle() );
			message = "接続がタイムアウトしました\n"
					+ "・ネットワーク状態を確認して下さい\n";
			cancel(true);
			return;
		}

		//webview死んだら終わり
		if ( web.getTitle() == null ){
			Log.v(TAG,"webview null : " + title);
			message = "ページがリセットされました\n"
					+ "・メモリ不足の可能性があります\n";
			cancel(true);
			return;
		}

		//タイトル合わなかったら終わり
		if ( web.getTitle().equals(title) == false ){
			Log.v(TAG,"pageTrans fail : " + title + " / " + web.getTitle() );
			message = "ページ遷移に失敗\n"
					+ "・ネットワーク状態を確認して下さい\n";
			cancel(true);
			return;
		}

		Log.v(TAG,"pageTrans : " + url + "(wait:" + t + ")" );
	}
}



