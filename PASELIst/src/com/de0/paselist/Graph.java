package com.de0.paselist;

import java.io.UnsupportedEncodingException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.achartengine.ChartFactory;
import org.achartengine.GraphicalView;
import org.achartengine.chart.BarChart.Type;
import org.achartengine.model.TimeSeries;
import org.achartengine.model.XYMultipleSeriesDataset;
import org.achartengine.renderer.XYMultipleSeriesRenderer;
import org.achartengine.renderer.XYSeriesRenderer;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Paint.Align;
import android.util.Log;

public class Graph {
	private final String TAG = "Graph";

	//日毎グラフで何日分表示するか
	private final int DATELIMIT = 100;

	private Context context;


	//モード定義
	public static final int MODE_DATE =1;
	public static final int MODE_MONTH=2;
	public static final int MODE_YEAR =3;



	public Graph(Context c){
		context = c;
	}

	public GraphicalView makeChartItem(List<Map<String,String>> datas, Integer total){
		//データ変換
		TimeSeries series = new TimeSeries("item");

		//グラフ表示設定
		XYMultipleSeriesRenderer renderer = new XYMultipleSeriesRenderer();

		//各系列の最大最小値
		Double pointMax = 0d;
		Double pointMin = 100000000d;

		Integer itemNum = 1;

		//値セットとmaxmin取得
		for( Map<String,String> row : datas){
			String itemStr    = row.get("item");
			Double point = Double.parseDouble(row.get("point"));

			//ポイントminmax更新
			if (pointMax < point)
				pointMax = point;
			if (pointMin > point)
				pointMin = point;

			//項目追加
			series.add( itemNum ,point );



			//軸のラベル作成
			String label = itemStr.substring(0,4);

			//全角なら文字数半分にする
			try {
				if ( label.getBytes("SJIS").length > 4 )
					label = itemStr.substring(0,2);
			} catch (UnsupportedEncodingException e) {
				e.printStackTrace();
			}

			Double percent = 100 * point / total;
			label += "\n" + String.valueOf(percent).substring(0,3).replaceAll("\\.$", "");

			if (itemNum == 1)
				label += "%";

			renderer.addXTextLabel( itemNum ,label);

			itemNum++;
		}

		//軸の設定
		renderer.setXAxisMin(0);
		renderer.setXAxisMax(itemNum);
		renderer.setYAxisMin(0);
		renderer.setYAxisMax(pointMax);

		renderer.setXLabels(0);

		setRenderer(renderer);

		//データ設定
		XYMultipleSeriesDataset dataset = new XYMultipleSeriesDataset();
		dataset.addSeries(series);

		//作成
		return ChartFactory.getBarChartView(context, dataset,  renderer, Type.DEFAULT);
	}


	public GraphicalView makeChartDate(List<Map<String,String>> datas, Integer mode){
		String dateFormat;
		Integer offsetUnit;

		Calendar cal = Calendar.getInstance();

		switch (mode){
		case MODE_DATE:
			dateFormat = "yyyy-MM-dd";
			offsetUnit = Calendar.DATE;
			break;
		case MODE_MONTH:
			dateFormat = "yyyy-MM";
			offsetUnit = Calendar.MONDAY;
			break;
		case MODE_YEAR:
			dateFormat = "yyyy";
			offsetUnit = Calendar.YEAR;
			break;
		default:
			Log.v(TAG,"undefined mode");
			return null;
		}


		//データ変換
		TimeSeries series = new TimeSeries("time");

		//グラフ表示設定
		XYMultipleSeriesRenderer renderer = new XYMultipleSeriesRenderer();

		//各系列の最大最小値
		Date dateMax = null;
		Date dateMin = null;
		Double pointMax = 0d;
		Double pointMin = 100000000d;

		Integer count = 0;

		//値セットとmaxmin取得
		for( Map<String,String> row : datas){
			String datestr    = row.get("date");
			Date   dateparam  = formatDate(datestr,dateFormat);
			Double pointparam = Double.parseDouble(row.get("point"));

			//日付初期化
			if (dateMax == null)
				dateMax = dateparam;
			if (dateMin == null)
				dateMin = dateparam;

			//日付minmax更新
			if (dateMax.compareTo(dateparam) < 0)
				dateMax = dateparam;
			if (dateMin.compareTo(dateparam) > 0)
				dateMin = dateparam;

			//ポイントminmax更新
			if (pointMax < pointparam)
				pointMax = pointparam;
			if (pointMin > pointparam)
				pointMin = pointparam;

			//項目追加
			series.add( dateparam ,pointparam );


			//軸作成
			cal.setTime(dateparam);
			Integer date  = cal.get(Calendar.DATE);
			Integer month = cal.get(Calendar.MONTH) + 1;
			Integer year  = cal.get(Calendar.YEAR);
			Integer week  = cal.get(Calendar.DAY_OF_WEEK) - 1;
			String label = "";


			if(mode == MODE_DATE){
				label = date.toString();

				//1日だけ月表示
				if ( date == 1 ){
					label = month.toString() + "/" + label;
				}

				//曜日を付加
				label += "\n" + GetStat.formatWeekday(week.toString());

				renderer.addXTextLabel( dateparam.getTime() ,label);

				if (count > DATELIMIT)
					break;

			}else if (mode == MODE_MONTH){
				label = month.toString();

				//1月だけ年を付加
				if ( month == 1 ){
					label += "\n" + year.toString();
				}

				//奇数月だけ出す
				if (month % 2 == 1)
					renderer.addXTextLabel( dateparam.getTime() ,label);

			}else if (mode == MODE_YEAR){
				label = datestr;
				renderer.addXTextLabel( dateparam.getTime() ,label);
			}

			count++;
		}


		//初期位置補正
		cal.setTime(dateMin);
		cal.add(offsetUnit, -1);
		Long xMin = cal.getTime().getTime();

		//日表示だけは一ヶ月分に絞る
		if (mode == MODE_DATE){
			Calendar today = Calendar.getInstance();
			today.add(Calendar.DATE, -30);
			xMin = today.getTime().getTime();
		}


		cal.setTime(dateMax);
		cal.add(offsetUnit, +1);
		Long xMax = cal.getTime().getTime();


		//軸の設定
		renderer.setXAxisMin(xMin);
		renderer.setXAxisMax(xMax);
		renderer.setYAxisMin(0);
		renderer.setYAxisMax(pointMax);

		renderer.setXLabels(0);

		setRenderer(renderer);

		//データ設定
		XYMultipleSeriesDataset dataset = new XYMultipleSeriesDataset();
		dataset.addSeries(series);

		//作成
		return ChartFactory.getBarChartView(context, dataset,  renderer, Type.DEFAULT);
	}


	//デフォルトの表示設定
	private void setRenderer(XYMultipleSeriesRenderer renderer){
		//renderer.setChartTitle("年別集計");     // グラフタイトル
		//renderer.setChartTitleTextSize(20);
		//renderer.setPointSize(5f);                      // ポイントマーカーサイズ

		//xy軸設定
		//renderer.setAxisTitleTextSize(16);
		renderer.setShowAxes(false);
		renderer.setAxesColor(Color.LTGRAY);            // X軸、Y軸カラー
		//renderer.setXLabels(5);                         // X軸ラベルのおおよその数
		//renderer.setYLabels(5);                         // Y軸ラベルのおおよその数

		//軸ラベル設定
		renderer.setXLabelsAlign(Align.CENTER);
		renderer.setXLabelsColor(Color.BLACK);
		renderer.setYLabelsAlign(Align.LEFT);
		renderer.setYLabelsColor(0,Color.BLACK);
		renderer.setLabelsTextSize(20);                 // ラベルサイズ

		//凡例設定
		//renderer.setLegendTextSize(15);
		renderer.setShowLegend(false);

		// スクロール許可(X,Y)
		renderer.setPanEnabled(true, false);
		// ズーム許可
		renderer.setZoomEnabled(true, false);

		renderer.setBarSpacing(0.5); //間隔

		//データ値表示
		//renderer.setDisplayChartValues(true);
		//renderer.setChartValuesTextSize(18);

		//グリッド設定
		renderer.setShowGrid(true);
		renderer.setGridColor(Color.LTGRAY);
		renderer.setShowCustomTextGrid(false); //カスタム分のグリッド出すか


		// マージン top, left, bottom, right
		renderer.setMargins(new int[] { 10, 5, 15, 5 });
		renderer.setMarginsColor(Color.WHITE);

		//系列の色/マーク
		XYSeriesRenderer r = new XYSeriesRenderer();
		//r.setColor(Color.GREEN);
		r.setColor(context.getResources().getColor(R.color.paseli));
		//r.setPointStyle(PointStyle.CIRCLE);
		//r.setFillPoints(false);
		renderer.addSeriesRenderer(r);
	}


	//文字列をdateに変換
	private Date formatDate(String dateStr,String Format){
		//SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
		SimpleDateFormat sdf = new SimpleDateFormat(Format, Locale.US);

		Date date = null;

		try {
			date = sdf.parse( dateStr );
		} catch (ParseException e) {
			Log.v(TAG,"dateParseError:"+dateStr);
			e.printStackTrace();
		}

		return date;
	}

}
