package it.usna.shellyscan.view.lightsEditor;

import java.util.function.IntConsumer;
import java.util.function.IntSupplier;
import java.util.function.Supplier;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.event.ChangeListener;

import it.usna.swing.VerticalFlowLayout;

public class SliderPanel extends JPanel implements AdjustableControl {
	private static final long serialVersionUID = 1L;
	private final JSlider slider;
	private final JLabel label = new JLabel();
	private final IntSupplier valSupplier;
	private final Supplier<String> descSupplier; // label
	private final ChangeListener sliderListener;
	private final String suffix;

	public SliderPanel(int min, int max, IntSupplier val, String suffix, Supplier<String> desc, IntConsumer setter, IntConsumer adjustingSetter) {
		super(new VerticalFlowLayout(VerticalFlowLayout.VAlign.CENTER, VerticalFlowLayout.HAlign.LEFT, 0, 0));
		setOpaque(false);
		valSupplier = val;
		descSupplier = desc;
		this.suffix = suffix;

		slider = new JSlider(min, max, val.getAsInt());
		sliderListener = e -> {
			if(slider.getValueIsAdjusting() == false) {
				setter.accept(slider.getValue());
			} else {
				label.setText(desc.get() + ": " + slider.getValue() + suffix);
				if(adjustingSetter != null) {
					adjustingSetter.accept(slider.getValue());
				}
			}
		};

		add(label);
		add(slider);
	}
	
	public SliderPanel(int min, int max, IntSupplier val, String suffix, Supplier<String> desc, IntConsumer setter) {
		this(min, max, val, suffix, desc, setter, null);
	}
	
	public SliderPanel(int min, int max, IntSupplier val, String suffix, String desc, IntConsumer setter) {
		this(min, max, val, suffix, () -> desc, setter, null);
	}

	public SliderPanel(int min, int max, IntSupplier val, String suffix, String desc, IntConsumer setter, IntConsumer adjustingSetter) {
		this(min, max, val, suffix, () -> desc, setter, adjustingSetter);
	}

	@Override
	public void adjust() {
		slider.removeChangeListener(sliderListener);

		label.setText(descSupplier.get() + ": " + valSupplier.getAsInt() + suffix);
		slider.setValue(valSupplier.getAsInt());

		slider.addChangeListener(sliderListener);
	}
}