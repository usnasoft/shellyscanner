package it.usna.shellyscan.view.scheduler.gen2plus.pareditor;

import static it.usna.shellyscan.Main.LABELS;

import java.awt.BorderLayout;
import java.awt.Dialog;
import java.awt.FlowLayout;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.util.ArrayList;

import javax.swing.AbstractAction;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.KeyStroke;

import it.usna.shellyscan.controller.UsnaAction;
import it.usna.swing.VerticalFlowLayout;

public class ParamEditorDialog extends JDialog {
	private static final long serialVersionUID = 1L;

	public ParamEditorDialog(final Window owner, final JTextField paramsTF) {
		super(owner, LABELS.getString("dlgLightsEditorTitle"), Dialog.ModalityType.DOCUMENT_MODAL);
		setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
		setLayout(new BorderLayout());
		
		JPanel editorsPanel = new JPanel(new VerticalFlowLayout(VerticalFlowLayout.CENTER, VerticalFlowLayout.LEFT, 0, 0));
		ArrayList<EditorPanel> editorsList = new ArrayList<>();
		String par = paramsTF.getText();
		if(SwitchEditor.check(par)) {
			var swEditor = new SwitchEditor(par);
			editorsPanel.add(swEditor);
			editorsList.add(swEditor);
		}
		if(SliderPar.Dimmer.check(par)) {
			var dimmer = new SliderPar.Dimmer(par);
			editorsPanel.add(dimmer);
			editorsList.add(dimmer);
		}
		if(RGBPanel.check(par)) {
			var rgb = new RGBPanel(par);
			editorsPanel.add(rgb);
			editorsList.add(rgb);
		} else if(SliderPar.White.check(par)) { // else -> SliderPar.White do not coexists with RGBPanel
			var white = new SliderPar.White(par);
			editorsPanel.add(white);
			editorsList.add(white);
		}
		if(SliderPar.CT.check(par)) {
			var ct = new SliderPar.CT(par);
			editorsPanel.add(ct);
			editorsList.add(ct);
		}
		if(SliderPar.Position.check(par)) {
			var ct = new SliderPar.Position(par);
			editorsPanel.add(ct);
			editorsList.add(ct);
		}
		if(editorsList.isEmpty()) {
			dispose();
			return;
		}
		add(editorsPanel, BorderLayout.CENTER);
		
		JPanel buttonsPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
		JButton btnOKButton = new JButton(new UsnaAction("dlgOK", e -> {
			editorsList.forEach(p -> paramsTF.setText(p.change(paramsTF.getText())));
			dispose();
		}));
		JButton btnClose = new JButton(new UsnaAction("dlgClose", e -> dispose()));
		
		buttonsPanel.add(btnOKButton);
		buttonsPanel.add(btnClose);
		add(buttonsPanel, BorderLayout.SOUTH);

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
	
	//	todo: edit icon on label -> it.usna.shellyscan.view.scheduler.gen2plus.G2JobPanel.addCall(String, String, int).btnSelectCombo
	public static boolean canEdit(String par) {
		return
				SwitchEditor.check(par) ||
				SliderPar.Dimmer.check(par) ||
				RGBPanel.check(par) ||
				SliderPar.White.check(par) ||
				SliderPar.CT.check(par) ||
				SliderPar.Position.check(par);
	}

	interface EditorPanel {
		String change(String par);
	}
	
//	public static void main(String ...strings) {
//		new ParamEditorDialog(null, null);
//	}
}