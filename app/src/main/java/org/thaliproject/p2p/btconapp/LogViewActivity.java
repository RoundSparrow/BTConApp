package org.thaliproject.p2p.btconapp;

import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
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

	public void showLogOnActivity()
	{
		textViewLogViewMain0.setText("");

		ArrayList<String> copyOfItems = new ArrayList<String>();
		copyOfItems.addAll(LogKeeper.getLog());

		for (int i = 0; i < copyOfItems.size(); i++) {
			textViewLogViewMain0.append(i + ": ");
			textViewLogViewMain0.append(copyOfItems.get(i));
			textViewLogViewMain0.append("\n");
		}
		// toss an extra newline on for easier of reading
		textViewLogViewMain0.append("\n");
	}
}
