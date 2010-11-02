package com.mobilepearls.react;

import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Paint.Align;
import android.os.Vibrator;
import android.text.InputFilter;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.view.HapticFeedbackConstants;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.EditText;

public class ReactView extends View implements OnClickListener {

	private static final int STATE_START = 1;
	private static final int STATE_WAITING = 2;
	private static final int STATE_RED = 3;
	private static final int STATE_AFTER_CHEAT = 4;

	private static final int NUMBER_OF_CLICKS = 10;

	private int state = STATE_START;
	private int clicks;
	private int totalTime;
	private long startTime = -1;
	private long lastTime = -1;
	private final Paint textPaint = new Paint();
	private Timer timer = new Timer(true);
	private final Random random = new Random();
	private long lastClick;

	public void vibrate() {
		Vibrator vibrator = (Vibrator) getContext().getSystemService(Context.VIBRATOR_SERVICE);
		vibrator.vibrate(1000);
	}

	public ReactView(Context context, AttributeSet attrs) {
		super(context, attrs);
		setOnClickListener(this);
		textPaint.setColor(Color.WHITE);
		textPaint.setAntiAlias(true);
		textPaint.setTextAlign(Align.CENTER);

		DisplayMetrics metrics = new DisplayMetrics();
		((Activity) getContext()).getWindowManager().getDefaultDisplay().getMetrics(metrics);
		int smallest = Math.min(metrics.widthPixels, metrics.heightPixels);
		int textSize;
		if (smallest <= 300) {
			textSize = 18;
		} else if (smallest <= 400) {
			textSize = 24;
		} else {
			textSize = 30;
		}

		textPaint.setTextSize(textSize);
		begin();
	}

	private void startTurnRedTimer() {
		long delay = 2000 + random.nextInt(6000);
		timer.schedule(new TimerTask() {
			@Override
			public void run() {
				post(new Runnable() {
					@Override
					public void run() {
						state = STATE_RED;
						startTime = System.currentTimeMillis();
						invalidate();
					}
				});
			}
		}, delay);
	}

	private void begin() {
		clicks = totalTime = 0;
		startTime = -1;
	}

	private void stateChange() {
		performHapticFeedback(HapticFeedbackConstants.FLAG_IGNORE_VIEW_SETTING/* =1=VIRTUAL_KEY */);

		switch (state) {
		case STATE_START:
			lastClick = System.currentTimeMillis();
			begin();
			startTurnRedTimer();
			state = STATE_WAITING;
			break;
		case STATE_WAITING:
			long timeSinceLast = System.currentTimeMillis() - lastClick;
			if (timeSinceLast < 800) {
				// a quick click - ignore
				return;
			}
			timer.cancel();
			timer = new Timer(true);
			vibrate();
			state = STATE_AFTER_CHEAT;
			break;
		case STATE_RED:
			lastClick = System.currentTimeMillis();
			lastTime = lastClick - startTime;
			totalTime += lastTime;
			clicks++;
			startTime = -1;
			if (clicks == NUMBER_OF_CLICKS) {
				vibrate();
				gameOver();
			} else {
				startTurnRedTimer();
				state = STATE_WAITING;
			}
			break;
		case STATE_AFTER_CHEAT:
			((Activity) getContext()).finish();
			return;
		}
		invalidate();
	}

	private void gameOver() {
		final int totalTimeCopy = totalTime;

		state = STATE_START;
		clicks = 0;
		totalTime = 0;
		invalidate();

		final ReactHighScoreDatabase db = ReactHighScoreDatabase.getDatabase(getContext());
		int position = db.getPositionForScore(totalTimeCopy);

		final String LAST_NAME_KEY = "last_name";
		SharedPreferences prefs = ((Activity) getContext()).getPreferences(Context.MODE_PRIVATE);
		String initialName = prefs.getString(LAST_NAME_KEY, "");

		AlertDialog.Builder alert = new AlertDialog.Builder(getContext());
		alert.setCancelable(false);
		if (position >= ReactHighScoreDatabase.MAX_ENTRIES) {
			alert.setMessage("Time: " + totalTimeCopy + " ms.\n\nYou did not attain high score!");
			alert.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
					((Activity) getContext()).finish();
				}
			});
		} else {
			alert.setMessage("Time: " + totalTimeCopy + " ms.\nHigh score position: " + position
					+ "\n\nPlease enter name:");
			final EditText textInput = new EditText(getContext());
			textInput.setText(initialName);
			textInput.setFilters(new InputFilter[] { new InputFilter.LengthFilter(30) });
			alert.setView(textInput);
			alert.setCancelable(true);
			alert.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int whichButton) {
					String value = textInput.getText().toString();

					Editor editor = ((Activity) getContext()).getPreferences(Context.MODE_PRIVATE).edit();
					editor.putString(LAST_NAME_KEY, value);
					editor.commit();

					db.addEntry(value, totalTimeCopy);
					Intent intent = new Intent();
					intent.setClass(getContext(), ReactHighScoresActivity.class);
					intent.putExtra(ReactHighScoresActivity.JUST_STORED, true);
					((Activity) getContext()).finish();
					getContext().startActivity(intent);
				}
			});
		}

		alert.show();
	}

	@Override
	public void onClick(View v) {
		stateChange();
	}

	private void drawCenteredText(Canvas canvas, String... text) {
		int linePadding = 2;
		int totalHeight = text.length * (int) textPaint.getTextSize() + linePadding;
		int startY = getHeight() / 2 - totalHeight / 2;
		int x = getWidth() / 2;
		for (int i = 0; i < text.length; i++) {
			String s = text[i];
			int y = startY + i * ((int) textPaint.getTextSize());
			canvas.drawText(s, x, y, textPaint);
		}

	}

	@Override
	public void draw(Canvas canvas) {
		switch (state) {
		case STATE_START:
			canvas.drawColor(Color.BLACK);
			drawCenteredText(canvas, "Touch the screen", "or press trackball", "as quick as possible",
					"when the screen turns red.", "", "Touch screen to start!");
			break;
		case STATE_WAITING:
			canvas.drawColor(Color.BLACK);
			String lastString = (lastTime == -1) ? "" : "Last: " + lastTime + " ms";
			drawCenteredText(canvas, "Reactions: " + clicks + "/" + NUMBER_OF_CLICKS, "", "Time: " + totalTime + " ms",
					"", lastString);
			break;
		case STATE_RED:
			canvas.drawColor(Color.RED);
			drawCenteredText(canvas, "React!");
			if (startTime == -1) {
				startTime = System.currentTimeMillis();
			}
			break;
		case STATE_AFTER_CHEAT:
			canvas.drawColor(Color.BLACK);
			drawCenteredText(canvas, "Head start!", "", "Touch to continue.");
			break;

		}
	}

}
