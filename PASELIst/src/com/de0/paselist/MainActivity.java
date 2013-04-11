package com.de0.paselist;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import android.os.Bundle;
import android.preference.PreferenceManager;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.text.InputType;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.view.View.OnClickListener;
import android.webkit.WebView;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.AdapterView.OnItemClickListener;


public class MainActivity extends Activity{

	private final String TAG = "MainActivity";

	private SharedPreferences sp;
	private List<Map<String,String>> listData;
	private ArrayAdapter<String> spadapter;

	private SimpleAdapter adp;
	private Db db;
	private Graph graph;

	private TextView text;
	private Spinner spinner;
	private ListView list;
	private Button btn_get;
	private Button btn_share;
	private LinearLayout chart_area;

	private OnItemClickListener nullListener;
	private OnItemClickListener itemFilterListener;
	private OnItemClickListener dateFilterListener;
	private OnItemClickListener monthFilterListener;
	private OnItemClickListener yearFilterListener;
	private OnItemClickListener statListener;

	private OnClickListener updateButtonListener;

	private String pass;
	private String otp;
	private String shareStr;

	private Boolean backable;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		//タイトルバー消す
		requestWindowFeature(Window.FEATURE_NO_TITLE);

		super.onCreate(savedInstanceState);
		setContentView(R.layout.maintab);

		sp = PreferenceManager.getDefaultSharedPreferences(this);
		db = new Db(this);
		graph = new Graph(this);

		text=(TextView)findViewById(R.id.text_info);

		//リスナ定義
		setListener();


		//ボタン定義
		btn_get = (Button)findViewById(R.id.button_get);
		btn_get.setOnClickListener(updateButtonListener);


		btn_share = (Button)findViewById(R.id.button_share);
		btn_share.setOnClickListener(new OnClickListener(){
			@Override
			public void onClick(View v) {
				Intent intent = new Intent();
				intent.setAction(Intent.ACTION_SEND);
				intent.setType("text/plain");
				intent.putExtra(Intent.EXTRA_TEXT, shareStr);
				startActivity(intent);
			}
		});

		//リストの定義
		list=(ListView)findViewById(R.id.listViewMain);
		listData = new ArrayList<Map<String, String>>();
		adp = new SimpleAdapter(
				this,
				listData,
				R.layout.list,
				new String[]{ "date", "item", "point" },
				new int[]{ R.id.date, R.id.item, R.id.point }
		);
		list.setAdapter(adp);

		//スピナー
		spinner = (Spinner)findViewById(R.id.spinner1);
//		ArrayAdapter<String> spadapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item);
		spadapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item);
		spadapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		spadapter.add("利用履歴");	//0
		spadapter.add("日別集計");	//1
		spadapter.add("月別集計");	//2
		spadapter.add("年別集計");	//3
		spadapter.add("統計情報");	//4

		spinner.setAdapter(spadapter);
		spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
			@Override
			public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
				setDefault();
			}
			@Override
			public void onNothingSelected(AdapterView<?> arg0) {
			}
		});

		/*
		///////// テスト用 /////////////
		Button btn_tst = (Button)findViewById(R.id.tst);
		btn_tst.setVisibility(View.GONE);
		btn_tst.setOnClickListener(new OnClickListener(){
			@Override
			public void onClick(View v) {
				db.open();
				db.deleteNewest();
				db.close();
			}
		});
		/////////////////////////
		 */


		chart_area = (LinearLayout)findViewById(R.id.chart);

        setDefault();
		ifUserNotConfig();

		Log.v(TAG,"oncleate complated");
	}//onCreateここまで-------------------------


	//メニュー作成
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.activity_main, menu);
		return true;
	}
	//メニュー選択時処理
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.menu_settings:
			Intent intent = new Intent(MainActivity.this,ConfActivity.class);
			startActivity(intent);
			return true;
		}
		return false;
	}

	//戻るキー
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		if( (keyCode == KeyEvent.KEYCODE_BACK) & backable ){
			setDefault();
			return false;
		}

		return super.onKeyDown(keyCode, event);
	}

	//表示内容とリスナの決定
	private void setDefault(){
		//戻れるかどうかのState保存
		backable = false;

		//共有ボタン消しておく
		btn_share.setVisibility(View.GONE);


		//未取得対策
		final String cnt = db.query("SELECT COUNT() FROM PASELI_TABLE;");
		if ( cnt.equals("0"))
			return;

		text.setText("残額 : "+ sp.getString(Info.key_totalpaseli, "0") + " P");


		//グラフエリア表示
		chart_area.setVisibility(View.GONE);
        chart_area.removeAllViews();

		switch(spinner.getSelectedItemPosition()){
		//履歴
		case 0:
			list.setOnItemClickListener(itemFilterListener);
			update(db.getAll());
			break;

		//日別
		case 1:
			list.setOnItemClickListener(dateFilterListener);
			List<Map<String,String>> datas_date = db.getByDate();
			update(datas_date);

			chart_area.setVisibility(View.VISIBLE);
			chart_area.addView( graph.makeChartDate(datas_date, Graph.MODE_DATE) );

			break;

		//月別
		case 2:
			list.setOnItemClickListener(monthFilterListener);
			List<Map<String,String>> datas_month = db.getByMonth();
			update(datas_month);

			chart_area.setVisibility(View.VISIBLE);
			chart_area.addView( graph.makeChartDate(datas_month, Graph.MODE_MONTH) );

			break;

		//年別
		case 3:
			list.setOnItemClickListener(yearFilterListener);

			List<Map<String,String>> datas_year = db.getByYear();
			update(datas_year);

			chart_area.setVisibility(View.VISIBLE);
			chart_area.addView( graph.makeChartDate(datas_year, Graph.MODE_YEAR) );
			break;

		//統計
		case 4:
			btn_share.setVisibility(View.VISIBLE);

			list.setOnItemClickListener(statListener);
			update(db.getByitem());

			setStat("");
			break;

		default:
			break;
		}

	}

	//ユーザ名未設定なら設定に移る
	private boolean ifUserNotConfig(){
		String user = sp.getString("user","");
		//ユーザなし
		if ( user.equals("") ){
			Toast.makeText(getApplicationContext(), "アカウント情報を設定して下さい", Toast.LENGTH_LONG).show();
			Intent intent = new Intent(MainActivity.this,ConfActivity.class);
			startActivity(intent);
			return true;
		}
		return false;
	}


	//リスナ定義
	private void setListener(){
		//なにもしない
		nullListener = new AdapterView.OnItemClickListener(){
			@Override
			public void onItemClick(AdapterView<?> parent, View view, int pos, long id) {
			}
		};

		//クリックしたアイテムでフィルタ(全表示用)
		itemFilterListener = new AdapterView.OnItemClickListener(){
			@Override
			public void onItemClick(AdapterView<?> parent, View view, int pos, long id) {
				String selectedItem = listData.get(pos).get("item");

				text.setText("Filter : " + selectedItem);
				update(db.getByFilterItem(selectedItem));

				//戻れるかどうかのState保存
				backable = true;

				list.setOnItemClickListener(nullListener);
			}
		};


		//日表示用
		dateFilterListener = new AdapterView.OnItemClickListener(){
			@Override
			public void onItemClick(AdapterView<?> parent, View view, int pos, long id) {
				String selectedItem = listData.get(pos).get("date");

				String total = db.query("SELECT SUM(point) FROM paseli_table WHERE type='支払' AND STRFTIME('%Y-%m-%d',date)='"+ selectedItem +"';");
				text.setText("利用履歴 : " + selectedItem + "日 (" +total+ " P)");

				list.setOnItemClickListener(nullListener);

				List<Map<String,String>> datas = db.getByFilterDate(selectedItem);
				update(datas);

				//chart_area.setVisibility(View.VISIBLE);
		        chart_area.removeAllViews();
				chart_area.addView( graph.makeChartItem(datas, Integer.parseInt(total)) );

				//戻れるかどうかのState保存
				backable = true;
			}
		};

		//月表示用
		monthFilterListener = new AdapterView.OnItemClickListener(){
			@Override
			public void onItemClick(AdapterView<?> parent, View view, int pos, long id) {
				String selectedItem = listData.get(pos).get("date");

				String total = db.query("SELECT SUM(point) FROM paseli_table WHERE type='支払' AND STRFTIME('%Y-%m',date)='"+ selectedItem +"';");
				text.setText("利用履歴 : " + selectedItem + "月 (" +total+ " P)");

				list.setOnItemClickListener(nullListener);

				List<Map<String,String>> datas = db.getByFilterMonth(selectedItem);
				update(datas);

				//chart_area.setVisibility(View.VISIBLE);
		        chart_area.removeAllViews();
				chart_area.addView( graph.makeChartItem(datas, Integer.parseInt(total)) );

				//戻れるかどうかのState保存
				backable = true;
			}
		};

		//年表示用
		yearFilterListener = new AdapterView.OnItemClickListener(){
			@Override
			public void onItemClick(AdapterView<?> parent, View view, int pos, long id) {
				String selectedItem = listData.get(pos).get("date");

				String total = db.query("SELECT SUM(point) FROM paseli_table WHERE type='支払' AND STRFTIME('%Y',date)='"+ selectedItem +"';");
				text.setText("利用履歴 : " + selectedItem + "年 (" +total+ " P)");

				list.setOnItemClickListener(nullListener);

				List<Map<String,String>> datas = db.getByFilterYear(selectedItem);
				update(datas);

				//chart_area.setVisibility(View.VISIBLE);
		        chart_area.removeAllViews();
				chart_area.addView( graph.makeChartItem(datas, Integer.parseInt(total)) );

				//戻れるかどうかのState保存
				backable = true;
			}
		};

		//統計用
		statListener = new AdapterView.OnItemClickListener(){
			@Override
			public void onItemClick(AdapterView<?> parent, View view, int pos, long id) {
				String selectedItem = listData.get(pos).get("item");

				//チャージリストに出ないので使われない
				if(selectedItem.equals("チャージ"))
					return;

				update(db.getByFilterItemMonthgroup(selectedItem));
				setStat(selectedItem);

				//戻れるかどうかのState保存
				backable = true;

				list.setOnItemClickListener(nullListener);
			}
		};


		//更新ボタン用
		updateButtonListener = new OnClickListener(){
			@Override
			public void onClick(View v) {
				pass = sp.getString("pass","");
				pass = Encryptor.getDecryptStr(pass,genKey());
				otp  = "";

				//ユーザなかったら設定へ
				if (ifUserNotConfig())
					return;

				//パスなしならダイアログ出す
				if ( pass.equals("") ){
					makePassDialog();
					return;
				}else if ( sp.getBoolean("useotp",false) ){   //OTP使う場合
					makeOtpDialog();
					return;
				}

				//通常処理
				GetPage task = new GetPage(MainActivity.this, (WebView)findViewById(R.id.webView1) , spinner,spadapter, pass, otp);
				task.execute();
			}
		};

	}

	private void makePassDialog(){
		final EditText edit = new EditText(MainActivity.this);
		edit.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);

		AlertDialog.Builder alert = new AlertDialog.Builder(MainActivity.this);
		alert.setTitle("パスワード");
		alert.setView(edit);
		alert.setPositiveButton("OK",new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				pass = edit.getText().toString();

				if ( sp.getBoolean("useotp",false) ){
					makeOtpDialog();
				} else {
					GetPage task = new GetPage(MainActivity.this, (WebView)findViewById(R.id.webView1) , spinner,spadapter, pass, otp);
					task.execute();
				}
			}
		});
		alert.show();
	}


	private void makeOtpDialog(){
		final EditText edit = new EditText(MainActivity.this);
		edit.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);

		AlertDialog.Builder alert = new AlertDialog.Builder(MainActivity.this);
		alert.setTitle("ワンタイムパスワード");
		alert.setView(edit);
		alert.setPositiveButton("OK",new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				otp = edit.getText().toString();
				GetPage task = new GetPage(MainActivity.this, (WebView)findViewById(R.id.webView1) , spinner,spadapter, pass, otp);
				task.execute();
			}
		});
		alert.show();
	}



	//キー用文字列生成
	private String genKey(){
		PackageInfo pkg = null;
		try {
			pkg = getPackageManager().getPackageInfo(this.getApplicationInfo().packageName, PackageManager.GET_META_DATA);
		} catch (NameNotFoundException e) {
			e.printStackTrace();
		}

		Long installTime = pkg.firstInstallTime;
		String pkgName = pkg.packageName;

		String key = pkgName + String.valueOf(installTime);

		return key;
	}


	//統計を反映
	private void setStat(String item){
		String[] res = GetStat.getStr(db, item);
		shareStr = res[1];

		text.setText(res[0]);
	}

	//DBを反映
	private void update(List<Map<String,String>> list){
		listData.clear();
		listData.addAll(list);
		adp.notifyDataSetChanged();
	}

}
