package it.usna.shellyscan.view.lightsEditor;

import static it.usna.shellyscan.Main.LABELS;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.FlowLayout;
import java.awt.event.ActionListener;
import java.io.IOException;

import javax.swing.ButtonGroup;
import javax.swing.JPanel;
import javax.swing.JRadioButton;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import it.usna.shellyscan.model.device.modules.RGBCCTInterface;

public class RGBCCTPanel extends LightPanel {
	private static final long serialVersionUID = 1L;
	private static final Logger LOG = LoggerFactory.getLogger(RGBCCTPanel.class);
	private final RGBCCTInterface light;
	private JRadioButton colorButton;
	private JRadioButton cctButton;
	private JPanel cards;
	private CCTPanel cctPanel;
	private RGBPanel rgbPanel;
	
	public RGBCCTPanel(RGBCCTInterface light) {
		this.light = light;
		setLayout(new BorderLayout(0, 0));
		add(modePanel(light), BorderLayout.NORTH);
		
		CardLayout cardsLayout = new CardLayout();
		cctPanel = new CCTPanel(light);
		rgbPanel = new RGBPanel(light);
		
		cards = new JPanel(cardsLayout);
		cards.add(cctPanel, "cct");
		cards.add(rgbPanel, "rgb");
		
		add(cards, BorderLayout.CENTER);
		
		cardsLayout.first(cards);
		adjust();
	}
	
	private JPanel modePanel(RGBCCTInterface light) {
		JPanel typePanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
		typePanel.setBackground(Color.LIGHT_GRAY);
	
		ActionListener modeAction = e -> {
			setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
			try {
				if(colorButton.isSelected() && light.isColorMode() == false) {
					light.setColorMode(true);
					adjust();
				} else if(cctButton.isSelected() && light.isColorMode()) {
					light.setColorMode(false);
					adjust();
				}
			} catch (IOException ex) {
				LOG.error("modeAction", ex);
			}
			setCursor(Cursor.getDefaultCursor());
		};

		cctButton = new JRadioButton(LABELS.getString("labelWhite"));
		cctButton.addActionListener(modeAction);
		colorButton = new JRadioButton(LABELS.getString("labelColor"));
		colorButton.addActionListener(modeAction);
		typePanel.add(cctButton);
		typePanel.add(colorButton);

		ButtonGroup btnGroup = new ButtonGroup();
		btnGroup.add(cctButton);
		btnGroup.add(colorButton);
		return typePanel;
	}

	@Override
	public void change(boolean on) throws IOException {
		light.change(on);
		cctPanel.adjust();
		rgbPanel.adjust();
	}

	private void adjust() {
		CardLayout cl = (CardLayout)(cards.getLayout());
		if(light.isColorMode()) {
			colorButton.setSelected(true);
			rgbPanel.adjust();
			cl.show(cards, "rgb");
		} else {
			cctButton.setSelected(true);
			cctPanel.adjust();
			cl.show(cards, "cct");
		}
	}
}