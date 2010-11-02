package com.mobilepearls.react;

import android.app.Activity;
import android.os.Bundle;

public class ReactGameActivity extends Activity {

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(R.layout.main);

		ReactView view = (ReactView) findViewById(R.id.reactionview);
		view.setFocusable(true);
		view.setFocusableInTouchMode(true);
		view.requestFocus();
	}

	@Override
	protected void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		//outState.putSerializable(GAME_KEY, game);
	}

}