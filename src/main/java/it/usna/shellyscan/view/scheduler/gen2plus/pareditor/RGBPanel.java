package it.usna.shellyscan.view.scheduler.gen2plus.pareditor;

import static it.usna.shellyscan.Main.LABELS;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.util.regex.Pattern;

import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.event.ChangeListener;

import it.usna.shellyscan.controller.UsnaAction;
import it.usna.shellyscan.view.util.ColorUtil;
import it.usna.swing.VerticalFlowLayout;

/**
 * RGB(W)Panel for the scheduler
 */
public class RGBPanel extends JPanel {
	private static final long serialVersionUID = 1L;

	private JLabel labelRed = new JLabel();
	private JLabel labelGreen = new JLabel();
	private JLabel labelBlue = new JLabel();
	private JLabel labelWhite = new JLabel();
	private final JSlider sliderRed;
	private final JSlider sliderGreen;
	private final JSlider sliderBlue;
	private JSlider sliderWhite;
	private final JPanel previewColorPanel = new JPanel();
	private boolean white = false;
	private static final Pattern RGB_PATTERN = Pattern.compile("\"rgb\"\\s*:\\s*\\[\\s*(\\d+)\\s*,\\s*(\\d+)\\s*,\\s*(\\d+)\\s*\\]");
	private static final Pattern WHITE_PATTERN = Pattern.compile("\"white\"\\s*:\\s*(\\d+)");

	public RGBPanel(final String parameters) {
		setBorder(BorderFactory.createEmptyBorder(6, 8, 12, 8));
		setLayout(new VerticalFlowLayout(VerticalFlowLayout.CENTER, VerticalFlowLayout.CENTER, 0, 0));
		
		var rgbMatcher = RGB_PATTERN.matcher(parameters);
		rgbMatcher.find();
		sliderRed = new JSlider(0, 255, Integer.parseInt(rgbMatcher.group(1)));
		sliderGreen = new JSlider(0, 255, Integer.parseInt(rgbMatcher.group(2)));
		sliderBlue = new JSlider(0, 255, Integer.parseInt(rgbMatcher.group(3)));
		
		var whiteMatcher = WHITE_PATTERN.matcher(parameters);
		white = whiteMatcher.find();

		ChangeListener rgbSliderListener = e -> {
			labelRed.setText(LABELS.getString("labelRed") + ": " + sliderRed.getValue());
			labelGreen.setText(LABELS.getString("labelGreen") + ": " + sliderGreen.getValue());
			labelBlue.setText(LABELS.getString("labelBlue") + ": " + sliderBlue.getValue());
			previewColorPanel.setBackground(ColorUtil.rgbwColor(sliderRed.getValue(), sliderGreen.getValue(), sliderBlue.getValue(), white ? sliderWhite.getValue() : -1));
		};
		JPanel redPanel = new JPanel(new BorderLayout(20, 0));
		redPanel.setOpaque(false);
		redPanel.add(labelRed, BorderLayout.NORTH);
		redPanel.add(sliderRed, BorderLayout.CENTER);
		this.add(redPanel);

		JPanel greenPanel = new JPanel(new BorderLayout(20, 0));
		greenPanel.setOpaque(false);
		greenPanel.add(labelGreen, BorderLayout.NORTH);
		greenPanel.add(sliderGreen, BorderLayout.CENTER);
		this.add(greenPanel);

		JPanel bluePanel = new JPanel(new BorderLayout(20, 0));
		bluePanel.setOpaque(false);
		bluePanel.add(labelBlue, BorderLayout.NORTH);
		bluePanel.add(sliderBlue, BorderLayout.CENTER);
		this.add(bluePanel);
		
		if(white) {
			sliderWhite = new JSlider(0, 255, Integer.parseInt(whiteMatcher.group(1)));
			ChangeListener whiteSliderListener = e -> {
				labelWhite.setText(LABELS.getString("labelWhite") + ": " + sliderWhite.getValue());
				previewColorPanel.setBackground(ColorUtil.rgbwColor(sliderRed.getValue(), sliderGreen.getValue(), sliderBlue.getValue(), sliderWhite.getValue()));
			};
			sliderWhite.addChangeListener(whiteSliderListener);
			JPanel whitePanel = new JPanel(new BorderLayout(20, 0));
			whitePanel.setOpaque(false);
			whitePanel.add(labelWhite, BorderLayout.NORTH);
			whitePanel.add(sliderWhite, BorderLayout.CENTER);
			this.add(whitePanel);
			whiteSliderListener.stateChanged(null);
		}

		JPanel colorsPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
		colorsPanel.setOpaque(false);
		JButton redButton = new JButton(new RGBButtonAction(255, 0, 0));
		redButton.setBorder(BorderFactory.createEmptyBorder(10, 12, 8, 12));
		redButton.setBackground(Color.RED);
		JButton greenButton = new JButton(new RGBButtonAction(0, 255, 0));
		greenButton.setBorder(BorderFactory.createEmptyBorder(10, 12, 8, 12));
		greenButton.setBackground(Color.GREEN);
		JButton yellowButton = new JButton(new RGBButtonAction(255, 255, 0));
		yellowButton.setBorder(BorderFactory.createEmptyBorder(10, 12, 8, 12));
		yellowButton.setBackground(Color.YELLOW);
		JButton blueButton = new JButton(new RGBButtonAction(0, 0, 255));
		blueButton.setBorder(BorderFactory.createEmptyBorder(10, 12, 8, 12));
		blueButton.setBackground(Color.BLUE);
		JButton violetButton = new JButton(new RGBButtonAction(255, 0, 255));
		violetButton.setBorder(BorderFactory.createEmptyBorder(10, 12, 8, 12));
		violetButton.setBackground(new Color(255, 0, 255));
		JButton whiteButton = new JButton(new UsnaAction(e -> {
			if(white) {
				sliderRed.setValue(0);
				sliderGreen.setValue(0);
				sliderBlue.setValue(0);
				sliderWhite.setValue(255);
				previewColorPanel.setBackground(ColorUtil.rgbwColor(0, 0, 0, 255));
			} else {
				sliderRed.setValue(255);
				sliderGreen.setValue(255);
				sliderBlue.setValue(255);
				previewColorPanel.setBackground(ColorUtil.rgbwColor(255, 255, 255, -1));
			}
		}));
		whiteButton.setBorder(BorderFactory.createEmptyBorder(10, 12, 8, 12));
		whiteButton.setBackground(Color.WHITE);

		colorsPanel.add(redButton);
		colorsPanel.add(greenButton);
		colorsPanel.add(yellowButton);
		colorsPanel.add(blueButton);
		colorsPanel.add(violetButton);
		colorsPanel.add(whiteButton);
		this.add(colorsPanel);

		previewColorPanel.setBorder(BorderFactory.createLineBorder(Color.GRAY));
		previewColorPanel.setPreferredSize(new Dimension(200, 10));
		this.add(previewColorPanel);

		sliderRed.addChangeListener(rgbSliderListener);
		sliderGreen.addChangeListener(rgbSliderListener);
		sliderBlue.addChangeListener(rgbSliderListener);
		rgbSliderListener.stateChanged(null);
	}

	private class RGBButtonAction extends AbstractAction {
		private static final long serialVersionUID = 1L;
		private final int red, green, blue;

		private RGBButtonAction(int red, int green, int blue) {
			this.red = red;
			this.green = green;
			this.blue = blue;
		}

		@Override
		public void actionPerformed(ActionEvent e) {
			sliderRed.setValue(red);
			sliderGreen.setValue(green);
			sliderBlue.setValue(blue);
			if(white) {
				sliderWhite.setValue(0);
			}
			previewColorPanel.setBackground(ColorUtil.rgbwColor(sliderRed.getValue(), sliderGreen.getValue(), sliderBlue.getValue(), white ? sliderWhite.getValue() : -1));
		}
	}
	
	public static boolean check(String par) {
		var m = RGB_PATTERN.matcher(par);
		return m.find();
	}
	
	public String change(String par) {
		var m = RGB_PATTERN.matcher(par);
		String ret = m.replaceFirst("\"rgb\":[" + sliderRed.getValue() + "," + sliderGreen.getValue() + "," + sliderBlue.getValue() + "]");
		if(white) {
			m = WHITE_PATTERN.matcher(ret);
			ret = m.replaceFirst("\"white\":" + sliderWhite.getValue());
		}
		return ret;
	}
	
//	public static void main(String ...strings) {
//		String par = "xxx\"rgb\" : [ 1, 20, 3]yyy";
//		var m = RGB_PATTERN.matcher(par);
//		if(m.find()) {
//			System.out.println(m.group(0));
//			System.out.println(m.group(1));
//			System.out.println(m.group(2));
//			System.out.println(m.group(3));
//			
//			m = RGB_PATTERN.matcher(par);
//			System.out.println(m.replaceFirst("\"rgb\":[1,2,3]"));
//		}
//	}
}