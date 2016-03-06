package org.thaliproject.p2p.btconapp;

import android.graphics.Color;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.ForegroundColorSpan;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import org.thaliproject.p2p.btconnectorlib.LogKeeper;

import java.util.ArrayList;

/**
 * @author Stephen A. Gutknecht
 * Copyright 2016 Stephen A Gutknecht, All Rights Reserved.
 * */
public class LogViewActivity extends AppCompatActivity {

	private TextView textViewLogViewMain0;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_log_view);
		Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
		setSupportActionBar(toolbar);

		textViewLogViewMain0 = (TextView) findViewById(R.id.textViewLogViewMain0);

		FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
		fab.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				Snackbar.make(view, "Refreshed!", Snackbar.LENGTH_LONG).setAction("Action", null).show();
				showLogOnActivity();
			}
		});

		showLogOnActivity();
	}


	private int COLOR_BROWN = Color.parseColor("#4E342E");

	public void showLogOnActivity()
	{
		textViewLogViewMain0.setText("");

		ArrayList<String> copyOfItems = new ArrayList<String>();
		copyOfItems.addAll(LogKeeper.getLog());

		/*
		Thoughts on performance is that this Activity is rarely used and we can do some inefficient things.
		 */
		for (int i = 0; i < copyOfItems.size(); i++) {
			String rawLine = copyOfItems.get(i);
			String splitOnBracket[] = rawLine.split("\\[", 2);

			textViewLogViewMain0.append(i + ": ");

			if (splitOnBracket[0].equals("0:0")) {
				textViewLogViewMain0.append(splitOnBracket[1]);
			}
			else {
				Log.i("LVA", "pattern " + splitOnBracket[0]);
				SpannableString outputSpan = new SpannableString(splitOnBracket[1]);
				switch (splitOnBracket[0]) {
					case "3:0":     // HIGH:NORMAL
						outputSpan.setSpan(new ForegroundColorSpan(Color.RED),    0, splitOnBracket[1].length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
						break;
					case "2:0":     // MEDIUM:NORMAL
						outputSpan.setSpan(new ForegroundColorSpan(COLOR_BROWN),  0, splitOnBracket[1].length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
						break;
					default:
						outputSpan.setSpan(new ForegroundColorSpan(Color.BLUE),   0, splitOnBracket[1].length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
						break;
				}
				textViewLogViewMain0.append(outputSpan);
			}

			textViewLogViewMain0.append("\n");
		}
		// toss an extra newline on for easier of reading
		textViewLogViewMain0.append("\n");
	}
}
