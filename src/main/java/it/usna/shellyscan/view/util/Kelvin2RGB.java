package it.usna.shellyscan.view.util;

import java.awt.Color;

public class Kelvin2RGB {
	/**
	 * https://gist.github.com/stasikos/06b02d18f570fc1eaa9f
	 * 
	 * Convert color temperature in Kelvins to RGB color for AWT
	 * @param temperature
	 * @return ready to use color object
	 */
	public static Color getRGBFromK(int temperature) {
		// Used this: https://gist.github.com/paulkaplan/5184275 at the beginning
		// based on http://stackoverflow.com/questions/7229895/display-temperature-as-a-color-with-c
		// this answer: http://stackoverflow.com/a/24856307
		// (so, just interpretation of pseudocode in Java)

		double x = temperature / 1000.0;
		if (x > 40) {
			x = 40;
		}
		double red;
		double green;
		double blue;

		// R
		if (temperature < 6527) {
			red = 1;
		} else {
			final double[] redpoly = {4.93596077e0, -1.29917429e0,
					1.64810386e-01, -1.16449912e-02,
					4.86540872e-04, -1.19453511e-05,
					1.59255189e-07, -8.89357601e-10};
			red = poly(redpoly, x);
		}
		
		// G
		if (temperature < 850) {
			green = 0;
		} else if (temperature <= 6600) {
			double[] greenpoly = {-4.95931720e-01, 1.08442658e0,
					-9.17444217e-01, 4.94501179e-01,
					-1.48487675e-01, 2.49910386e-02,
					-2.21528530e-03, 8.06118266e-05};
			green = poly(greenpoly, x);
		} else {
			final double[] greenpoly = {3.06119745e0, -6.76337896e-01,
					8.28276286e-02, -5.72828699e-03,
					2.35931130e-04, -5.73391101e-06,
					7.58711054e-08, -4.21266737e-10};
			green = poly(greenpoly, x);
		}
		
		// B
		if (temperature < 1900) {
			blue = 0;
		} else if (temperature < 6600) {
			final double[] bluepoly = {4.93997706e-01, -8.59349314e-01,
					5.45514949e-01, -1.81694167e-01,
					4.16704799e-02, -6.01602324e-03,
					4.80731598e-04, -1.61366693e-05};
			blue = poly(bluepoly, x);
		} else {
			blue = 1;
		}

		red = clamp(red, 0, 1);
		blue = clamp(blue, 0, 1);
		green = clamp(green, 0, 1);
		return new Color((float) red, (float) green, (float) blue);
	}

	private static double poly(double[] coefficients, double x) {
		double result = coefficients[0];
		double xn = x;
		for (int i = 1; i < coefficients.length; i++) {
			result += xn * coefficients[i];
			xn *= x;
		}
		return result;
	}

	private static double clamp(double x, double min, double max) {
		if (x < min) {
			return min;
		}
		if (x > max) {
			return max;
		}
		return x;
	}
	
	////////////////////////
	// https://github.com/vendor-vandor/pidome-unofficial/blob/d1b15bf85085452a664c2892ffb26260df441007/pidome-pidome-server-connector/src/main/java/org/pidome/server/connector/tools/ColorImpl.java
		
	public static double[] kelvinToColor(long kelvin, String colorType){
        double tmpCalc;
        double r;
        double g;
        double b;
        float[] hsbvals = null;
        
        if(kelvin>40000) kelvin = 40000;
        if(kelvin<1000) kelvin = 1000;

        kelvin = kelvin / 100;
        
        /// red
        if(kelvin<= 66){
            r = 255;
        } else {
            tmpCalc = kelvin - 60;
            tmpCalc = 329.698727446 * Math.pow(tmpCalc,-0.1332047592);
            r = tmpCalc;
            if(r<0) r = 0;
            if(r>255) r = 255;
        }
        /// green
        if(kelvin <= 66){
            tmpCalc = kelvin;
            tmpCalc = 99.4708025861 * Math.log(tmpCalc) - 161.1195681661;
            g = tmpCalc;
            if(g<0) g = 0;
            if(g>255) g = 255;
        } else {
            tmpCalc = kelvin - 60;
            tmpCalc = 288.1221695283 * Math.pow(tmpCalc,-0.0755148492);
            g = tmpCalc;
            if(g<0) g = 0;
            if(g>255) g = 255;
        }
        /// blue
        if(kelvin >= 66){
            b = 255;
        } else if(kelvin <= 19){
            b = 0;
        } else {
            tmpCalc = kelvin - 10;
            tmpCalc = 138.5177312231 * Math.log(tmpCalc) - 305.0447927307;
            b = tmpCalc;
            if(b < 0) b = 0;
            if(b > 255) b = 255;
        }
        double[] rgb = new double[3];
        switch(colorType){
            case "hsb":
                float[] floatVals = java.awt.Color.RGBtoHSB((int)r, (int)g, (int)b, hsbvals);
                rgb[0] = (double)floatVals[0];
                rgb[1] = (double)floatVals[1];
                rgb[2] = (double)floatVals[2];
                return rgb;
            default:
                rgb[0] = (int)r;
                rgb[1] = (int)g;
                rgb[2] = (int)b;
            return rgb;
        }
    }
}