package it.usna.shellyscan.view.scheduler.gen2plus.pareditor;

import static it.usna.shellyscan.Main.LABELS;

import java.awt.BorderLayout;
import java.awt.Dialog;
import java.awt.FlowLayout;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;

import javax.swing.AbstractAction;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.KeyStroke;

import it.usna.shellyscan.controller.UsnaAction;

public class ParamEditorDialog extends JDialog {
	private static final long serialVersionUID = 1L;

	public ParamEditorDialog(final Window owner, final JTextField paramsTF) {
		super(owner, LABELS.getString("dlgLightsEditorTitle"), Dialog.ModalityType.DOCUMENT_MODAL);
		setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
		getContentPane().setLayout(new BorderLayout(0, 0));
		
		RGBPanel rgb;
		if(RGBPanel.check(paramsTF.getText())) {
			rgb = new RGBPanel(paramsTF.getText());
			getContentPane().add(rgb);
		} else {
			dispose();
			return;
		}
		
		JPanel buttonsPanel = new JPanel(new FlowLayout());
		JButton btnOKButton = new JButton(new UsnaAction("dlgOK", e -> {
			paramsTF.setText(rgb.change(paramsTF.getText()));
			dispose();
		}));
		JButton btnClose = new JButton(new UsnaAction("dlgClose", e -> dispose()));
		
		buttonsPanel.add(btnOKButton);
		buttonsPanel.add(btnClose);
		getContentPane().add(buttonsPanel, BorderLayout.SOUTH);

		rootPane.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), "escape_close");
		rootPane.getActionMap().put("escape_close", new AbstractAction() {
			private static final long serialVersionUID = 1L;
			@Override
			public void actionPerformed(ActionEvent e) {
				dispose();
			}
		});
		
		pack();
		setLocationRelativeTo(owner);
		setVisible(true);
	}
	
//	public static void main(String ...strings) {
//		new ParamEditorDialog(null, null);
//	}
}