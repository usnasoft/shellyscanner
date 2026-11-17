package it.usna.shellyscan.view;

import static it.usna.shellyscan.Main.LABELS;

import java.awt.FlowLayout;
import java.awt.event.KeyEvent;
import java.util.function.Consumer;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.KeyStroke;

import it.usna.shellyscan.controller.UsnaAction;
import it.usna.swing.texteditor.TextDocumentListener;

/**
 * Find: + <textfield> + <erase button>
 * ctrl^F focus on <textfield>
 * ctrl^E clean <textfield> and focus on <textfield>
 */
public class TableFilterPanel extends JPanel {
	private static final long serialVersionUID = 1L;

	public TableFilterPanel(int textFieldSize, boolean findLabel, Consumer<String> filterAction) {
		super(new FlowLayout(FlowLayout.RIGHT, 3, 0));
		setBorder(BorderFactory.createEmptyBorder(1, 0, 3, 0));

		if(findLabel) {
			add(new JLabel(LABELS.getString("lblFilter")));
		}
		
		JTextField textFieldFilter = new JTextField();
		textFieldFilter.setColumns(textFieldSize);
		textFieldFilter.setBorder(BorderFactory.createEmptyBorder(2, 2, 2, 2));
		add(textFieldFilter);
		
		textFieldFilter.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(KeyEvent.VK_F, MainView.SHORTCUT_KEY), "find_focus_sel");
		textFieldFilter.getActionMap().put("find_focus_sel", new UsnaAction(e -> textFieldFilter.requestFocus()));
		
		final UsnaAction eraseFilterAction = new UsnaAction(this, null, "/images/erase-9-16.png", e -> {
			textFieldFilter.setText("");
			textFieldFilter.requestFocusInWindow();
		});
		JButton eraseFilterButton = new JButton(eraseFilterAction);
		eraseFilterButton.setContentAreaFilled(false);
		eraseFilterButton.setBorder(BorderFactory.createEmptyBorder(1, 2, 1, 2));
		add(eraseFilterButton);
		
		eraseFilterButton.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(KeyEvent.VK_E, MainView.SHORTCUT_KEY), "find_erase_sel");
		eraseFilterButton.getActionMap().put("find_erase_sel", eraseFilterAction);

		textFieldFilter.getDocument().addDocumentListener((TextDocumentListener)e -> filterAction.accept(textFieldFilter.getText()) );
	}
}
