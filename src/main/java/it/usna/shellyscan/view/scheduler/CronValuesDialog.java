package it.usna.shellyscan.view.scheduler;

import java.awt.BorderLayout;
import java.awt.Dialog;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.Collections;
import java.util.List;
import java.util.stream.IntStream;

import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import javax.swing.SwingConstants;

import it.usna.shellyscan.Main;
import it.usna.shellyscan.controller.UsnaAction;
import it.usna.swing.UsnaSwingUtils;

/**
 * Display a  multi-selection list and fill "origin" JTextField on "OK" according to CronUtils.listAsCronString rule
 */
public class CronValuesDialog extends JDialog {
	private static final long serialVersionUID = 1L;
	
	public CronValuesDialog(JDialog owner, JTextField origin, int min, int max) {
		super(owner, Main.LABELS.getString("schTitle"), Dialog.ModalityType.APPLICATION_MODAL);
		setIconImage(Main.ICON);
		List<Integer> selected = origin != null ? CronUtils.fragmentToInt(origin.getText()) : Collections.emptyList();
		
		JScrollPane scrollPane = new JScrollPane();
		scrollPane.setBorder(BorderFactory.createEmptyBorder());
		getContentPane().add(scrollPane, BorderLayout.CENTER);
		JList<Integer> list = new JList<>(IntStream.range(min, max).boxed().toArray(Integer[]::new));
		for(int sel: selected) {
			list.addSelectionInterval(sel - min, sel - min);
		}
		
		scrollPane.setViewportView(list);
		
		JPanel buttonsPanel = new JPanel();
		getContentPane().add(buttonsPanel, BorderLayout.SOUTH);
		
		JButton okBtn = new JButton(new UsnaAction("dlgOK", e -> {
			List<Integer> selection = list.getSelectedValuesList();
			String res = (selection.size() == 0 || selection.size() == max - min) ? "*" : CronUtils.listAsCronString(selection);
			origin.setText(res);
			origin.requestFocus();
			dispose();
		}));
		buttonsPanel.add(okBtn);
		
		JButton closeBtn = new JButton(new UsnaAction("dlgClose", e -> dispose()));
		buttonsPanel.add(closeBtn);
		
		list.addMouseListener(new MouseAdapter() {
			@Override
		    public void mouseClicked(MouseEvent evt) {
		        if (evt.getClickCount() == 2) {
		        	okBtn.doClick();
		        }
		    }
		});
		
		scrollPane.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), "escape_close");
		scrollPane.getActionMap().put("escape_close", new AbstractAction() {
			private static final long serialVersionUID = 1L;
			@Override
			public void actionPerformed(ActionEvent e) {
				dispose();
			}
		});
		
		pack();
		setSize(getWidth(), 520);
		UsnaSwingUtils.setLocationRelativeTo(this, origin, SwingConstants.RIGHT, -4, -16);
		setVisible(true);
	}
}