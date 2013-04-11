package com.de0.paselist;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;

import android.content.Context;
import android.util.Log;

public class JsInterface {
	private static final String TAG = "JsInterface";

	private Context cont;

	public JsInterface(Context context) {
		this.cont = context;
	}

	public void getSource(String source){
		String filename = Info.tempfile;

		FileOutputStream out;
		try {
			out = cont.openFileOutput(filename,0);
			OutputStreamWriter writer = new OutputStreamWriter(out);

			writer.write(source);
			writer.flush();
			writer.close();

			Log.v(TAG, "file out : "+filename);

		} catch (IOException e) {
			Log.v(TAG, "getsource ioex");
			e.printStackTrace();
		}

	}
}
