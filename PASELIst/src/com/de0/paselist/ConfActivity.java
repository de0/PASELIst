package com.de0.paselist;

import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.Bundle;
import android.preference.EditTextPreference;
import android.preference.Preference;
import android.preference.PreferenceManager;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.PreferenceActivity;

public class ConfActivity extends PreferenceActivity {
	private SharedPreferences sp;


	@SuppressWarnings("deprecation")
	public void onCreate(Bundle savedInstanceState){
		sp = PreferenceManager.getDefaultSharedPreferences(this);

		super.onCreate(savedInstanceState);
		addPreferencesFromResource( R.xml.activity_conf );

		//パスが変更されたら暗号化して置き換える
		EditTextPreference passPref = (EditTextPreference)findPreference("pass");
		passPref.setOnPreferenceChangeListener(new OnPreferenceChangeListener(){
			@Override
			public boolean onPreferenceChange(Preference preference,Object newValue) {
				sp.edit().putString("pass", Encryptor.getEncryptedStr(newValue.toString(),genKey())).commit();

				return false;
			}

		});
	}

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

}
