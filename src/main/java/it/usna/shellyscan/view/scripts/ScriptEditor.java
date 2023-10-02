package it.usna.shellyscan.view.scripts;

import static it.usna.shellyscan.Main.LABELS;

import java.awt.BorderLayout;
import java.awt.Cursor;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.Toolkit;
import java.awt.Window;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.stream.Collectors;
import java.util.zip.ZipException;
import java.util.zip.ZipFile;

import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JToolBar;
import javax.swing.SwingConstants;
import javax.swing.filechooser.FileNameExtensionFilter;

import it.usna.shellyscan.Main;
import it.usna.shellyscan.controller.UsnaAction;
import it.usna.shellyscan.model.device.g2.modules.Script;
import it.usna.shellyscan.view.BasicEditorPanel;
import it.usna.shellyscan.view.util.Msg;
import it.usna.util.IOFile;

/**
 * A small text editor where "load" and "save" relies on "scipts" notes
 * @author usna
 */
public class ScriptEditor extends JFrame {
	private static final long serialVersionUID = 1L;
	
	public ScriptEditor(Window owner, Script script) throws IOException {
//		super(owner, LABELS.getString("dlgScriptEditorTitle") + " - " + scipt.getName());
		super(LABELS.getString("dlgScriptEditorTitle") + " - " + script.getName());
		setIconImage(Toolkit.getDefaultToolkit().createImage(getClass().getResource(Main.ICON)));
		

		
		BasicEditorPanel editor = new BasicEditorPanel(this, script.getCode()) {
			private static final long serialVersionUID = 1L;

						
			protected JToolBar createToolbar(JToolBar toolBar) {
				UsnaAction openAction = new UsnaAction(ScriptEditor.this, "/images/folder-3-24.png", "btnFind", e -> {
					final JFileChooser fc = new JFileChooser();
					fc.addChoosableFileFilter(new FileNameExtensionFilter(LABELS.getString("filetype_sbk_desc"), Main.BACKUP_FILE_EXT));
					if(fc.showOpenDialog(ScriptEditor.this) == JFileChooser.APPROVE_OPTION) {
						textArea.setText(loadCodeFromFile(fc.getSelectedFile()));
					}
				});
				UsnaAction saveAction = new UsnaAction(null, "/images/save-24.png", "btnFind", e -> {
//					FindReplaceDialog f = new FindReplaceDialog(owner, textArea, true);
//					f.setLocationRelativeTo(ScriptEditor.this);
//					f.setVisible(true);
				});
				UsnaAction saveAsAction = new UsnaAction(null, "/images/save-as-24.png", "btnFind", e -> {
//					FindReplaceDialog f = new FindReplaceDialog(owner, textArea, true);
//					f.setLocationRelativeTo(ScriptEditor.this);
//					f.setVisible(true);
				});
				
				toolBar.add(openAction);
				toolBar.add(saveAction);
				toolBar.add(saveAsAction);
				toolBar.addSeparator();
				return super.createToolbar(toolBar);
			}
		};
		getContentPane().add(editor);

		// bottom buttons
		JPanel southPanel = new JPanel(new GridLayout(1, 3, 0, 0));
		southPanel.add(new JLabel());
		
		JPanel buttonsPanel = new JPanel(new FlowLayout());
		getContentPane().add(southPanel, BorderLayout.SOUTH);
		
		JButton saveButton = new JButton(LABELS.getString("btnUpload"));
		saveButton.addActionListener(e -> {
			try {
				ScriptEditor.this.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
				String res = script.putCode(editor.getText());
				if(res != null) {
					Msg.errorMsg(this, res);
				}
			} finally {
				ScriptEditor.this.setCursor(Cursor.getDefaultCursor());
			}
		});
		buttonsPanel.add(saveButton);
		
		JButton closeButton = new JButton(LABELS.getString("dlgClose"));
		closeButton.addActionListener(e -> dispose());
		buttonsPanel.add(closeButton);
		
		southPanel.add(buttonsPanel);
		
		JLabel caretLabel = new JLabel(editor.getCaretRow() + " : " + editor.getCaretColumn() + " ", SwingConstants.RIGHT);
		southPanel.add(caretLabel);
		
		editor.addCaretListener(e -> {
			caretLabel.setText(editor.getCaretRow() + " : " + editor.getCaretColumn() + " ");
		});
		
		setSize(800, 600);
		setVisible(true);
		editor.requestFocus();
		setLocationRelativeTo(owner);
	}
	
	private String loadCodeFromFile(File in) {
		try {
			try (ZipFile inZip = new ZipFile(in, StandardCharsets.UTF_8)) { // backup
				String[] scriptList = inZip.stream().filter(z -> z.getName().endsWith(".mjs")).map(z -> z.getName().substring(0, z.getName().length() - 4)).toArray(String[]::new);
				if(scriptList.length > 0) {
					Object sName = JOptionPane.showInputDialog(this, LABELS.getString("scrSelectionMsg"), LABELS.getString("scrSelectionTitle"), JOptionPane.PLAIN_MESSAGE, null, scriptList, null);
					if(sName != null) {
						try (InputStream is = inZip.getInputStream(inZip.getEntry(sName + ".mjs"))) {
							return new BufferedReader(new InputStreamReader(is)).lines().collect(Collectors.joining("\n"));
						}
					}
				} else {
					JOptionPane.showMessageDialog(this, LABELS.getString("scrNoneInZipFile"), LABELS.getString("btnUpload"), JOptionPane.INFORMATION_MESSAGE);
				}
			} catch (ZipException e1) { // no zip (backup) -> text file
				return IOFile.readFile(in);
			}
		} catch (/*IO*/Exception e1) {
			Msg.errorMsg(e1);
		} finally {
			setCursor(Cursor.getDefaultCursor());
		}
		return "";
	}
}