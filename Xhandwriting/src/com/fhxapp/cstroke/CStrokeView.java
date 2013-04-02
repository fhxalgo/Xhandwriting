package com.fhxapp.cstroke;

import java.util.ArrayList;
import java.util.List;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.util.Log;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

public class CStrokeView extends SurfaceView implements SurfaceHolder.Callback {
	private static final String TAG = "CStrokeView";

	Paint paint = new Paint();
	
	// private int scanInterval = 5;
	// private int strokeInterval = 200;
	private int scanInterval = 0;
	private int strokeInterval = 0;
	private List<CStroke> cstrokes;

	DrawThread thread;
	int screenW; // Device's screen width.
	int screenH; // Devices's screen height.
	int ballX; // Ball x position.
	int ballY; // Ball y position.
	int initialY;
	float dY; // Ball vertical speed.
	int ballW;
	int ballH;
	int bgrW;
	int bgrH;
	int angle;
	int bgrScroll;
	int dBgrY; // Background scroll speed.
	float acc;
	//Bitmap ball, bgr, bgrReverse;
	boolean reverseBackroundFirst;
	boolean ballFingerMove;

	// Measure frames per second.
	long now;
	int framesCount = 0;
	int framesCountAvg = 0;
	long framesTimer = 0;
	Paint fpsPaint = new Paint();

	// Frame speed
	long timeNow;
	long timePrev = 0;
	long timePrevFrame = 0;
	long timeDelta;

	public CStrokeView(Context context) {
		super(context);

		// Set thread
		getHolder().addCallback(this);

		setFocusable(true);

		cstrokes = new ArrayList<CStroke>();
	}

	// add stroke data to the view
	public synchronized void setStrokeData(String strokes) {
		cstrokes.clear();

		int i, start;
		StringBuffer tmpstroke = new StringBuffer();
		for (i = 0; i < strokes.length() && strokes.charAt(i) == ' '; i++) {
		}
		start = i;
		for (; i < strokes.length(); i++) {
			if (strokes.charAt(i) == '#' && i != start) {
				cstrokes.add(new CStroke(tmpstroke.toString()));
				tmpstroke.setLength(0);
			}
			tmpstroke.append(strokes.charAt(i));
		}
		cstrokes.add(new CStroke(tmpstroke.toString()));

		Log.i(TAG, "# of cstrokes added: " + cstrokes.size());

		drawStrokesOnCanvas(0);
	}

	private void drawStrokesOnCanvas(int id) {
		Log.i(TAG, "drawStrokesOnCanvas: " + cstrokes.size());
		ln.clear();

		Bitmap b = Bitmap.createBitmap(100, 100, Bitmap.Config.ARGB_8888);
		Canvas c = new Canvas(b);

		for (int i = 0; i < cstrokes.size(); i++) {

			Log.i(TAG, "drawing stroke " + i);
			drawCurrentStroke(c, i);
			thread.drawStroke();
			Log.i(TAG, "finished drawing Strokes " + i + " on Canvas: ");

			// pause here between strokes
			pause(1 * 1000);
		}

	}

	@Override
	public void onSizeChanged(int w, int h, int oldw, int oldh) {
		super.onSizeChanged(w, h, oldw, oldh);
		// This event-method provides the real dimensions of this custom view.
		screenW = w;
		screenH = h;

	}

	// ***************************************
	// ************* TOUCH *****************
	// ***************************************
	@Override
	public synchronized boolean onTouchEvent(MotionEvent ev) {

		switch (ev.getAction()) {
		case MotionEvent.ACTION_DOWN: {
			ballX = (int) ev.getX() - ballW / 2;
			ballY = (int) ev.getY() - ballH / 2;

			ballFingerMove = true;
			break;
		}

		case MotionEvent.ACTION_MOVE: {
			ballX = (int) ev.getX() - ballW / 2;
			ballY = (int) ev.getY() - ballH / 2;

			break;
		}

		case MotionEvent.ACTION_UP:
			ballFingerMove = false;
			dY = 0;
			break;
		}
		return true;
	}

	@Override
	public void onDraw(Canvas canvas) {
		super.onDraw(canvas);

		paint.setStyle(Paint.Style.FILL);

		paint.setColor(Color.WHITE);
		canvas.drawPaint(paint);

		paint.setColor(Color.BLUE);
		canvas.drawCircle(20, 20, 15, paint);

		// int i = 0;
		// for (CStroke currstroke : cstrokes) {
		// //paint.setColor(Color.BLACK);
		// paint.setStrokeWidth(5);
		// //canvas.drawPath(currstroke.polygon, paint);
		// canvas.drawPoints((float[])currstroke.polygon.getPolygonPointsFloat(),
		// paint);
		// //Log.i(TAG, "drawing stroke #: " + ++i);
		// }

		// canvas.save();

		Log.i(TAG, "drawing LineData (begin): " + ln.size());
		synchronized (ln) {
			for (LineData ld : ln) {
				paint.setColor(Color.RED);
				paint.setStrokeWidth(5);
				// canvas.drawLine(ld.x1, ld.y1, ld.x2, ld.y2, paint);
				// canvas.drawRect(ld.x1, ld.y1, ld.x2, ld.y2, paint);
				// canvas.drawCircle(ld.x1, ld.y1, 1, paint);
				canvas.drawPoint(ld.x1, ld.y1, paint);
				// canvas.drawPoint(ld.x1, ld.y1, paint);
				// use drawPath to see if better performance
			}
			// ln.clear(); // this clears current stroke data
		}
		Log.i(TAG, "drawing LineData (end): " + ln.size());

		// canvas.restore();
	}

	public void onDrawSaved(Canvas canvas) {}

	// @Override
	public void surfaceChanged(SurfaceHolder holder, int format, int width,
			int height) {
	}

	// @Override
	public void surfaceCreated(SurfaceHolder holder) {
		thread = new DrawThread(getHolder(), this);
		thread.setRunning(true);
		// thread.start();
	}

	// @Override
	public void surfaceDestroyed(SurfaceHolder holder) {
		boolean retry = true;
		thread.setRunning(false);
		// while (retry) {
		// try {
		// thread.join();
		// retry = false;
		// } catch (InterruptedException e) {
		//
		// }
		// }
	}

	// draw one stroke at a time
	private void drawCurrentStroke(Canvas gc, int i) {

		CStroke currstroke = (CStroke) cstrokes.get(i);
		Rect bounds = currstroke.polygon.getBounds();

		// setCurrentStrokeColor(currstroke, gc);
		Log.i(TAG, "drawCurrentStroke: " + i + ", direction="
				+ currstroke.direction + ", draw=" + currstroke.draw);

		if (currstroke.direction == 1) {
			leftToRightStroke(currstroke, bounds, gc);
		} else if (currstroke.direction == 2) {
			// \ (down)
			downRightStroke(currstroke, bounds, gc);
		} else if (currstroke.direction == 3) {
			// | (down)
			downStroke(currstroke, bounds, gc);
		} else if (currstroke.direction == 4) {
			// 4 / (down)
			downLeftStroke(currstroke, bounds, gc);
		} else if (currstroke.direction == 5) {
			// 5 -- (right to left)
			rightToLeftStroke(currstroke, bounds, gc);

		} else if (currstroke.direction == 6) {
			// 6 \ (up)
			upLeftStroke(currstroke, bounds, gc);
		} else if (currstroke.direction == 7) {
			// 7 | (up)
			upStroke(currstroke, bounds, gc);
		} else if (currstroke.direction == 8) {
			// 8 / (up)
			upRightStroke(currstroke, bounds, gc);
		}

		if (currstroke.pause) {
			pause(strokeInterval);
		}

		currstroke.draw = true;
	}

	/**
	 * @param currstroke
	 * @param bounding
	 * @param gc
	 */
	private void leftToRightStroke(CStroke currstroke, Rect bounding, Canvas gc) {
		int bounding_x = bounding.left;
		int bounding_y = bounding.top;

		Log.i(TAG, " leftToRightStroke(begin): " + bounding);
		Log.i("TAG", String.format(
				"left=%d,tom=%d,right=%d,bottom=%d,width=%d,height=%d",
				bounding.left, bounding.top, bounding.right, bounding.bottom,
				bounding.width(), bounding.height()));

		for (int x = bounding_x; x < bounding_x + bounding.width(); x++) {
			for (int y = bounding_y; y < bounding_y + bounding.height(); y++) {
				if (currstroke.polygon.contains(x, y)) {
					drawLineInUIThread(gc, x, y, x, y);
				}
			}
			// pause(scanInterval);
		}

		Log.i(TAG, " leftToRightStroke(end): " + ln.size());
	}

	/**
	 * @param currstroke
	 * @param bounding
	 * @param gc
	 */
	private void upRightStroke(CStroke currstroke, Rect bounding, Canvas gc) {
		int bounding_height = bounding.height();
		int bounding_width = bounding.width();
		int bounding_x = bounding.left;
		int bounding_y = bounding.top;

		double slope = (double) bounding_height / (double) bounding_width;
		for (int j = bounding_height; j > -bounding_height; j--) {
			for (int k = bounding_width; (k * slope) + j > 0; k--) {
				int l = (int) ((k * slope) + j);
				if (currstroke.polygon.contains(k + bounding_x, l + bounding_y)) {
					drawLineInUIThread(gc, k + bounding_x, l + bounding_y, k
							+ bounding_x, l + bounding_y);
				}
			}
			pause(scanInterval);
		}
	}

	/**
	 * @param currstroke
	 * @param bounds
	 * @param gc
	 */
	private void upStroke(CStroke currstroke, Rect bounds, Canvas gc) {
		// System.out.println("up stroke: " + bounds.x + ", " + bounds.y+
		// " width: " + bounds.width + " height: " + bounds.height);

		int bounds_x = bounds.left;
		int bounds_y = bounds.top;

		for (int y = bounds_y + bounds.height(); y > bounds_y; y--) {
			for (int x = bounds_x; x < bounds_x + bounds.width(); x++) {
				if (currstroke.polygon.contains(x, y)) {
					// System.out.println("drawing up: (" + x + ", " + y + ")");
					drawLineInUIThread(gc, x, y, x, y);
				}
			}
			pause(scanInterval);

		}
	}

	/**
	 * @param currstroke
	 * @param bounds
	 * @param gc
	 */
	private void upLeftStroke(CStroke currstroke, Rect bounds, Canvas gc) {
		double slope = -((double) bounds.height() / (double) bounds.width());

		int bounds_x = bounds.left;
		int bounds_y = bounds.right;

		for (int y = (int) bounds.height() * 2; y > 0; y--) {
			for (int x = 0; (x * slope) + y > 0; x++) {
				int l = (int) ((x * slope) + y);
				if (currstroke.polygon.contains(x + bounds_x, l + bounds_y)) {
					drawLineInUIThread(gc, x + bounds_x, l + bounds_y, x
							+ bounds_x, l + bounds_y);
				}
			}
			pause(scanInterval);
		}
	}

	/**
	 * @param currstroke
	 * @param bounds
	 * @param gc
	 */
	private void rightToLeftStroke(CStroke currstroke, Rect bounds, Canvas gc) {
		int bounds_x = bounds.left;
		int bounds_y = bounds.top;

		for (int x = bounds_x + (int) bounds.width(); x > bounds_x; x--) {
			for (int y = bounds_y; y < bounds_y + bounds.height(); y++) {
				if (currstroke.polygon.contains(x, y)) {
					drawLineInUIThread(gc, x, y, x, y);
				}
			}
			pause(scanInterval);
		}
	}

	/**
	 * @param currstroke
	 * @param bounds
	 * @param gc
	 */
	private void downLeftStroke(CStroke currstroke, Rect bounds, Canvas gc) {
		double slope = (double) bounds.height() / (double) bounds.width();

		int bounds_height = bounds.height();
		int bounds_width = bounds.width();
		int bounds_x = bounds.left;
		int bounds_y = bounds.top;

		for (int y = -bounds_height; y < bounds_height; y++) {
			for (int x = bounds_width; (x * slope) + y > 0; x--) {
				int l = (int) ((x * slope) + y);
				if (currstroke.polygon.contains(x + bounds_x, l + bounds_y)) {
					drawLineInUIThread(gc, x + bounds_x, l + bounds_y, x
							+ bounds_x, l + bounds_y);
				}
			}
			pause(scanInterval);
		}
	}

	/**
	 * @param currstroke
	 * @param bounding
	 * @param gc
	 */
	private void downStroke(CStroke currstroke, Rect bounding, Canvas gc) {
		int bounding_x = (int) bounding.left;
		int bounding_y = (int) bounding.top;

		for (int y = bounding_y; y < bounding_y + bounding.height(); y++) {
			for (int x = bounding_x; x < bounding_x + bounding.width(); x++) {
				if (currstroke.polygon.contains(x, y)) {
					drawLineInUIThread(gc, x, y, x, y);
				}
			}
			pause(scanInterval);
		}
	}

	/**
	 * @param currstroke
	 * @param bounds
	 * @param gc
	 */
	private void downRightStroke(CStroke currstroke, Rect bounds, Canvas gc) {
		int bounds_x = bounds.left;
		int bounds_y = bounds.top;

		Log.i(TAG, " downRightStroke(): " + bounds);
		Log.i("TAG", String.format(
				"left=%d,top=%d,right=%d,bottom=%d,width=%d,height=%d",
				bounds.left, bounds.top, bounds.right, bounds.bottom,
				bounds.width(), bounds.height()));

		double slope = -((double) bounds.height() / (double) bounds.width());
		for (int y = 0; y < bounds.height() * 2; y++) {
			for (int x = 0; (x * slope) + y > 0; x++) {
				int l = (int) ((x * slope) + y);
				if (currstroke.polygon.contains(x + bounds_x, l + bounds_y)) {
					drawLineInUIThread(gc, x + bounds_x, l + bounds_y, x
							+ bounds_x, l + bounds_y);
				}
			}
			pause(scanInterval);
		}
	}

	private void pause(int i) {
		try {
			Thread.sleep(i);
		} catch (Exception exception) {
			exception.printStackTrace();
		}
	}

	private void drawLineInUIThread(final Canvas gc, final int x1,
			final int y1, final int x2, final int y2) {
		synchronized (ln) {
			ln.add(new LineData(x1, y1, x2, y2));
		}

	}

	private void drawLineInUIThread2(final Canvas gc, final int x1,
			final int y1, final int x2, final int y2) {
		new Thread(new Runnable() {
			public void run() {
				// Log.i(TAG,
				// String.format("drawLineInUIThread: (%d, %d) -> (%d, %d)", x1,
				// y1,x2,y2));
				// gc.drawCircle(x1, y1, 5, mPaint);
				// drawPath
				// add all the points to cache instead
				synchronized (ln) {
					ln.add(new LineData(x1, y1, x2, y2));
				}
			}
		}).start();

	}

	private List<LineData> ln = new ArrayList<LineData>();

	class LineData {
		public int x1, y1, x2, y2;

		public LineData(int x1, int y1, int x2, int y2) {
			this.x1 = x1;
			this.y1 = y1;
			this.x2 = x2;
			this.y2 = y2;
		}
	}

	// call onDraw(canvas)

	class DrawThread {
		private SurfaceHolder surfaceHolder;
		private CStrokeView drawView;
		private boolean run = false;

		public DrawThread(SurfaceHolder surfaceHolder, CStrokeView drawView) {
			this.surfaceHolder = surfaceHolder;
			this.drawView = drawView;
		}

		public void setRunning(boolean run) {
			this.run = run;
		}

		public SurfaceHolder getSurfaceHolder() {
			return surfaceHolder;
		}

		@SuppressLint("WrongCall")
		public void drawStroke() {
			Canvas c = null;

			timePrevFrame = System.currentTimeMillis();

			try {
				c = surfaceHolder.lockCanvas(null);
				synchronized (surfaceHolder) {
					// call methods to draw and process next stroke
					drawView.onDraw(c);
				}
			} finally {
				if (c != null) {
					surfaceHolder.unlockCanvasAndPost(c);
				}
			}
		}
	}
}
