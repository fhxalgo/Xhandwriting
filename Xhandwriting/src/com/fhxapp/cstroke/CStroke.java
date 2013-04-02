package com.fhxapp.cstroke;

import java.util.*;

public class CStroke {
	public SimplePolygon polygon; // the polygon bounding this stroke
	public boolean radical; // true for radical, false otherwise
	public boolean pause; // true if pause after writing stroke
	public boolean draw; // draw if true
	public int direction; // direction to draw stroke with
	public int border;

	public CStroke(String strokedata) {
		polygon = new SimplePolygon();
		int i, x, y;
		draw = false;

		// System.out.println(strokedata);
		for (i = 0; i < strokedata.length(); i++) {
			if (strokedata.charAt(i) == '#') {
				direction = Integer
						.parseInt(strokedata.substring(i + 1, i + 2));
				if (strokedata.charAt(i + 2) == 'P') {
					pause = true;
				} else {
					pause = false;
				}
				if (strokedata.charAt(i + 3) == 'R') {
					radical = true;
				} else {
					radical = false;
				}
				i += 5;
				break;
			}
		}
		StringTokenizer st = new StringTokenizer(strokedata.substring(i,
				strokedata.length()), " \n\r\t;,"); 
		while (st.hasMoreTokens()) {
			x = Integer.parseInt(st.nextToken());
			y = Integer.parseInt(st.nextToken());
			// System.out.println("Point " + x + " " + y);
			polygon.addPoint(x, y);
		}
	}

	public String toString() {
		return String.format("SimplePolygon(points:%s), direction=%d", Arrays.toString(polygon.getPolygonPoints()), direction);
	}
}