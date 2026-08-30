package it.usna.shellyscan.view.scheduler.gen2plus.pareditor;

import static it.usna.shellyscan.Main.LABELS;

import java.util.regex.Pattern;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.event.ChangeListener;

import it.usna.shellyscan.view.scheduler.gen2plus.pareditor.ParamEditorDialog.EditorPanel;
import it.usna.shellyscan.view.util.UtilMiscellaneous;
import it.usna.swing.VerticalFlowLayout;

//public class DimmerPanel extends JPanel implements EditorPanel {
//	private static final long serialVersionUID = 1L;
//	private JLabel labelWhite = new JLabel();
//	private JSlider sliderBrightness;
//	private static final Pattern BRIGHTNESS_PATTERN = Pattern.compile("\"brightness\"\\s*:\\s*(\\d+)");
//	
//	public DimmerPanel(final String parameters) {
//		setBorder(BorderFactory.createEmptyBorder(6, 8, 12, 8));
//		setLayout(new VerticalFlowLayout(VerticalFlowLayout.CENTER, VerticalFlowLayout.LEFT, 0, 0));
//		
//		var whiteMatcher = BRIGHTNESS_PATTERN.matcher(parameters);
//		whiteMatcher.find();
//		sliderBrightness = new JSlider(0, 100, UtilMiscellaneous.clamp(Integer.parseInt(whiteMatcher.group(1)), 0, 100));
//		this.add(labelWhite);
//		this.add(sliderBrightness);
//		ChangeListener cl = e -> labelWhite.setText(LABELS.getString("labelBrightness") + ": " + sliderBrightness.getValue());
//		sliderBrightness.addChangeListener(cl);
//		cl.stateChanged(null);
//	}
//	
//	public static boolean check(String par) {
//		var m = BRIGHTNESS_PATTERN.matcher(par);
//		return m.find();
//	}
//	
//	@Override
//	public String change(String par) {
//		var m = BRIGHTNESS_PATTERN.matcher(par);
//		return  m.replaceFirst("\"brightness\":" + sliderBrightness.getValue());
//	}
//}

abstract class SliderPar extends JPanel implements EditorPanel {
	private static final long serialVersionUID = 1L;
	protected JSlider slider;
	
	public SliderPar(final String parameters, String parName, Pattern pattern, int min, int max) {
		setBorder(BorderFactory.createEmptyBorder(6, 8, 12, 8));
		setLayout(new VerticalFlowLayout(VerticalFlowLayout.CENTER, VerticalFlowLayout.LEFT, 0, 0));
		
		var matcher = pattern.matcher(parameters);
		matcher.find();
		slider = new JSlider(min, max, UtilMiscellaneous.clamp(Integer.parseInt(matcher.group(1)), min, max));
		JLabel label = new JLabel();
		this.add(label);
		this.add(slider);
		ChangeListener cl = e -> label.setText(parName + ": " + slider.getValue());
		slider.addChangeListener(cl);
		cl.stateChanged(null);
	}
	
	static class Dimmer extends SliderPar {
		private static final long serialVersionUID = 1L;
		private static final Pattern BRIGHTNESS_PATTERN = Pattern.compile("\"brightness\"\\s*:\\s*(\\d+)");

		public Dimmer(final String parameters) {
			super(parameters, LABELS.getString("labelBrightness"), BRIGHTNESS_PATTERN, 0, 100);
		}

		public static boolean check(String par) {
			return BRIGHTNESS_PATTERN.matcher(par).find();
		}
		
		@Override
		public String change(String par) {
			var m = BRIGHTNESS_PATTERN.matcher(par);
			return  m.replaceFirst("\"brightness\":" + slider.getValue());
		}
	}
	
	static class White extends SliderPar {
		private static final long serialVersionUID = 1L;
		private static final Pattern WHITE_PATTERN = Pattern.compile("\"white\"\\s*:\\s*(\\d+)");

		public White(final String parameters) {
			super(parameters, LABELS.getString("labelWhite"), WHITE_PATTERN, 0, 255);
		}

		public static boolean check(String par) {
			return WHITE_PATTERN.matcher(par).find();
		}
		
		@Override
		public String change(String par) {
			var m = WHITE_PATTERN.matcher(par);
			return  m.replaceFirst("\"white\":" + slider.getValue());
		}
	}
	
	static class CT extends SliderPar {
		private static final long serialVersionUID = 1L;
		private static final Pattern CT_PATTERN = Pattern.compile("\"ct\"\\s*:\\s*(\\d+)");

		public CT(final String parameters) {
			super(parameters, LABELS.getString("labelTemperature"), CT_PATTERN, 2700, 6500);
		}

		public static boolean check(String par) {
			return CT_PATTERN.matcher(par).find();
		}
		
		@Override
		public String change(String par) {
			var m = CT_PATTERN.matcher(par);
			return  m.replaceFirst("\"ct\":" + slider.getValue());
		}
	}
}