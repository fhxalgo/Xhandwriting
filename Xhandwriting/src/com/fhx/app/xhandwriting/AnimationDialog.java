package com.fhx.app.xhandwriting;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.text.Html;
import android.text.util.Linkify;
import android.graphics.Color;
import android.widget.FrameLayout;
import android.widget.TextView;

import com.fhxapp.cstroke.CStrokeView;

public class AnimationDialog extends Dialog {
	private static Context mContext = null;
	private CStrokeView mStrokeView;
	private String mStrokeData;
	
	public AnimationDialog(Context context) {
		super(context);
		mContext = context;
	}
	
	public AnimationDialog(Context context, CStrokeView dv) {
		super(context);
		mContext = context;
		mStrokeView = dv;
	}

	/**
	 * } Standard Android on create method that gets called when the activity
	 * initialized.
	 */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		setContentView(R.layout.about);
		TextView tv = (TextView) findViewById(R.id.legal_text);
		tv.setText(readRawTextFile(R.raw.legal));
		tv = (TextView) findViewById(R.id.info_text);
		tv.setText(Html.fromHtml(readRawTextFile(R.raw.info)));
		tv.setLinkTextColor(Color.WHITE);
		Linkify.addLinks(tv, Linkify.ALL);
		FrameLayout ll = (FrameLayout) findViewById(R.id.strokesview1);		
		//ll.addView(mStrokeView);
	}

	public static String readRawTextFile(int id) {
		InputStream inputStream = mContext.getResources().openRawResource(id);
		InputStreamReader in = new InputStreamReader(inputStream);
		BufferedReader buf = new BufferedReader(in);
		String line;
		StringBuilder text = new StringBuilder();
		try {
			while ((line = buf.readLine()) != null)
				text.append(line);
		} catch (IOException e) {
			return null;
		}
		return text.toString();
	}
	
	public void setStrokeData(String strokeData) {
		this.mStrokeData = strokeData;
	}
}