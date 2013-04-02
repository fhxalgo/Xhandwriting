package com.fhxapp.cstroke;

import android.graphics.Rect;

public class SimplePolygon {
	public int npoints;

	public int points[];

	public int xpoints[];

	public int ypoints[];

	protected Rect bounds;
	//protected Rectangle bounds;

	public SimplePolygon() {
		xpoints = new int[4];
		ypoints = new int[4];
	}

	public boolean contains(int x, int y) {
		if (npoints <= 2 || !bounds.contains(x, y)) {
			return false;
		}
		int hits = 0;

		int lastx = xpoints[npoints - 1];
		int lasty = ypoints[npoints - 1];
		int curx, cury;

		// Walk the edges of the polygon
		for (int i = 0; i < npoints; lastx = curx, lasty = cury, i++) {
			curx = xpoints[i];
			cury = ypoints[i];

			if (cury == lasty) {
				continue;
			}

			int leftx;
			if (curx < lastx) {
				if (x >= lastx) {
					continue;
				}
				leftx = curx;
			} else {
				if (x >= curx) {
					continue;
				}
				leftx = lastx;
			}

			double test1, test2;
			if (cury < lasty) {
				if (y < cury || y >= lasty) {
					continue;
				}
				if (x < leftx) {
					hits++;
					continue;
				}
				test1 = x - curx;
				test2 = y - cury;
			} else {
				if (y < lasty || y >= cury) {
					continue;
				}
				if (x < leftx) {
					hits++;
					continue;
				}
				test1 = x - lastx;
				test2 = y - lasty;
			}

			if (test1 < (test2 / (lasty - cury) * (lastx - curx))) {
				hits++;
			}
		}

		return ((hits & 1) != 0);
	}

	public Rect getBounds() {
		if (bounds == null) {
			calculateBounds(xpoints, ypoints, npoints);
		}
		return bounds;
	}

	public void addPoint(int x, int y) {
		if (npoints == xpoints.length) {
			int tmp[];

			tmp = new int[npoints * 2];
			System.arraycopy(xpoints, 0, tmp, 0, npoints);
			xpoints = tmp;

			tmp = new int[npoints * 2];
			System.arraycopy(ypoints, 0, tmp, 0, npoints);
			ypoints = tmp;
		}
		xpoints[npoints] = x;
		ypoints[npoints] = y;
		npoints++;
		
		if (bounds != null) {
			updateBounds(x, y);
		}
	}

	public int[] getPolygonPoints() {
		int[] points = new int[npoints * 2];
		for (int i = 0; i < npoints; i++) {
			points[2 * i] = xpoints[i];
			points[2 * i + 1] = ypoints[i];
		}
		return points;
	}
	
	public float[] getPolygonPointsFloat() {
		float[] points = new float[npoints * 2];
		for (int i = 0; i < npoints; i++) {
			points[2 * i] = xpoints[i];
			points[2 * i + 1] = ypoints[i];
		}
		return points;
	}
	
	void calculateBounds(int xpoints[], int ypoints[], int npoints) {
		int boundsMinX = Integer.MAX_VALUE;
		int boundsMinY = Integer.MAX_VALUE;
		int boundsMaxX = Integer.MIN_VALUE;
		int boundsMaxY = Integer.MIN_VALUE;

		for (int i = 0; i < npoints; i++) {
			int x = xpoints[i];
			boundsMinX = Math.min(boundsMinX, x);
			boundsMaxX = Math.max(boundsMaxX, x);
			int y = ypoints[i];
			boundsMinY = Math.min(boundsMinY, y);
			boundsMaxY = Math.max(boundsMaxY, y);
		}
		
		/*
		public Rect (int left, int top, int right, int bottom) 
		Create a new rectangle with the specified coordinates. 
		Note: no range checking is performed, so the caller must ensure that left <= right and top <= bottom.

		Parameters
		left  The X coordinate of the left side of the rectangle 
		top  The Y coordinate of the top of the rectangle 
		right  The X coordinate of the right side of the rectangle 
		bottom  The Y coordinate of the bottom of the rectangle  
		 */
		
		bounds = new Rect(boundsMinX, boundsMinY, boundsMaxX, boundsMaxY);
	}

	
	void updateBounds(int x, int y) {
		// Note: Rect has left < right, top < bottom 
		// Rectangle has x, x + width, y, y+ height
		
		if (x < bounds.left) {
			//bounds.right = bounds.width() + (bounds.left - x);
			bounds.left = x;
		} else if ( x > bounds.right) {
			bounds.right = x;
		} else {
			
		}
		
		
		if (y < bounds.top) {
			//bounds.bottom = bounds.height() + (bounds.top - y);
			bounds.top = y;
		} else if ( y > bounds.bottom) {
			bounds.bottom = y;
		} else {
			
		}
	}
	
//	void updateBounds(int x, int y) {
//		if (x < bounds.x) {
//			bounds.width = bounds.width + (bounds.x - x);
//			bounds.x = x;
//		} else {
//			bounds.width = Math.max(bounds.width, x - bounds.x);
//			// bounds.x = bounds.x;
//		}
//
//		if (y < bounds.y) {
//			bounds.height = bounds.height + (bounds.y - y);
//			bounds.y = y;
//		} else {
//			bounds.height = Math.max(bounds.height, y - bounds.y);
//			// bounds.y = bounds.y;
//		}
//	}

}
