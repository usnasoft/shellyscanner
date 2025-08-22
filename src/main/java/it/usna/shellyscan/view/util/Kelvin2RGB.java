package it.usna.shellyscan.view.util;

import java.awt.Color;

public class Kelvin2RGB {
	private Kelvin2RGB() {}
	
	// https://github.com/vendor-vandor/pidome-unofficial/blob/d1b15bf85085452a664c2892ffb26260df441007/pidome-pidome-server-connector/src/main/java/org/pidome/server/connector/tools/ColorImpl.java
	public static Color kelvinToColor(long kelvin/*, String colorType*/){
        double tmpCalc;
        double r;
        double g;
        double b;
//        float[] hsbvals = null;
        
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
//        double[] rgb = new double[3];
//        switch(colorType){
//            case "hsb":
//                float[] floatVals = java.awt.Color.RGBtoHSB((int)r, (int)g, (int)b, hsbvals);
//                rgb[0] = (double)floatVals[0];
//                rgb[1] = (double)floatVals[1];
//                rgb[2] = (double)floatVals[2];
//                return null;
//            default:
//                rgb[0] = (int)r;
//                rgb[1] = (int)g;
//                rgb[2] = (int)b;
//        }
        return new Color((int)r, (int)g, (int)b);
    }
}