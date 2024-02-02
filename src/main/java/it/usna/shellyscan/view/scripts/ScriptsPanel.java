package it.usna.shellyscan.view.scripts;

import static it.usna.shellyscan.Main.LABELS;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.zip.ZipException;
import java.util.zip.ZipFile;

import javax.swing.AbstractCellEditor;
import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.UIManager;
import javax.swing.border.Border;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumnModel;

import com.fasterxml.jackson.databind.JsonNode;

import it.usna.shellyscan.Main;
import it.usna.shellyscan.controller.UsnaAction;
import it.usna.shellyscan.model.Devices;
import it.usna.shellyscan.model.device.g2.AbstractG2Device;
import it.usna.shellyscan.model.device.g2.modules.Script;
import it.usna.shellyscan.view.DialogDeviceLogsG2;
import it.usna.shellyscan.view.util.Msg;
import it.usna.swing.table.ExTooltipTable;
import it.usna.swing.table.UsnaTableModel;

public class ScriptsPanel extends JPanel {
	private static final long serialVersionUID = 1L;
	private final static Border BUTTON_BORDERS = BorderFactory.createEmptyBorder(0, 12, 0, 12);
	private final ExTooltipTable table;
	private final ArrayList<Script> scripts = new ArrayList<>();

	public ScriptsPanel(JDialog owner, Devices devicesModel, int modelIndex) throws IOException {
		AbstractG2Device device = (AbstractG2Device) devicesModel.get(modelIndex);
		setLayout(new BorderLayout(0, 0));
		final UsnaTableModel tModel = new UsnaTableModel(LABELS.getString("lblScrColName"), LABELS.getString("lblScrColEnabled"), LABELS.getString("lblScrColRunning"));

		table = new ExTooltipTable(tModel) {
			private static final long serialVersionUID = 1L;
			{
				((JComponent) getDefaultRenderer(Boolean.class)).setOpaque(true);
				setAutoCreateRowSorter(true);
				setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

				columnModel.getColumn(2).setCellRenderer(new ButtonCellRenderer());
				columnModel.getColumn(2).setCellEditor(new ButtonCellEditor());
				
				activateSingleCellStringCopy();
			}

			@Override
			public boolean isCellEditable(final int row, final int column) {
				return true;
			}

			@Override
			public Component prepareEditor(TableCellEditor editor, int row, int column) {
				JComponent comp = (JComponent)super.prepareEditor(editor, row, column);
				comp.setBackground(table.getSelectionBackground());
				comp.setForeground(table.getSelectionForeground());
				return comp;
			}

			@Override
			public void editingStopped(ChangeEvent e) {
				ScriptsPanel.this.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
				try {
					final int mCol = convertColumnIndexToModel(getEditingColumn());
					final int mRow = convertRowIndexToModel(getEditingRow());
					final Script sc = scripts.get(mRow);
					if(mCol == 0) { // name
						sc.setName((String)getCellEditor().getCellEditorValue());
					} else if(mCol == 1) { // enabled
						sc.setEnabled((Boolean)getCellEditor().getCellEditorValue());
					}
					super.editingStopped(e);
				} finally {
					ScriptsPanel.this.setCursor(Cursor.getDefaultCursor());
				}
			}
		};
		JScrollPane scrollPane = new JScrollPane(table, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
		add(scrollPane, BorderLayout.CENTER);

		JPanel operationsPanel = new JPanel();
		operationsPanel.setLayout(new GridLayout(1, 0, 2, 0));
		operationsPanel.setBackground(Color.WHITE);
		add(operationsPanel, BorderLayout.SOUTH);

		final JButton btnDelete = new JButton(new UsnaAction(this, "btnDelete", e -> {
			final String cancel = UIManager.getString("OptionPane.cancelButtonText");
			if(JOptionPane.showOptionDialog(
					ScriptsPanel.this, LABELS.getString("msgDeleteConfirm"), LABELS.getString("btnDelete"),
					JOptionPane.OK_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE, null,
					new Object[] {UIManager.getString("OptionPane.yesButtonText"), cancel}, cancel) == 0) {
				try {
					final int mRow = table.convertRowIndexToModel(table.getSelectedRow());
					final Script sc = scripts.get(mRow);
					sc.delete();
					scripts.remove(mRow);
					tModel.removeRow(mRow);
				} catch (IOException e1) {
					Msg.errorMsg(e1);
				}
			}
		}));
		operationsPanel.add(btnDelete);

		final JButton btnNew = new JButton(new UsnaAction(this, "btnNew", e -> {
			try {
				Script sc = Script.create(device, null);
				scripts.add(sc);
				tModel.addRow(sc.getName(), sc.isEnabled(), sc.isRunning());
			} catch (IOException e1) {
				Msg.errorMsg(e1);
			}
		}));
		operationsPanel.add(btnNew);

		final JButton btnDownload = new JButton(new UsnaAction(this, "btnDownload", e -> {
			final int mRow = table.convertRowIndexToModel(table.getSelectedRow());
			final Script sc = scripts.get(mRow);
			final JFileChooser fc = new JFileChooser();
			fc.setFileFilter(new FileNameExtensionFilter(LABELS.getString("filetype_js_desc"), DialogDeviceScriptsG2.FILE_EXTENSION));
			fc.setSelectedFile(new File(sc.getName()));
			if(fc.showSaveDialog(ScriptsPanel.this) == JFileChooser.APPROVE_OPTION) {
//				setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
				try (FileWriter w = new FileWriter(fc.getSelectedFile())) {
					w.write(sc.getCode());
				} catch (IOException e1) {
					Msg.errorMsg(ScriptsPanel.this, LABELS.getString("msgScrNoCode"));
				} /*finally {
					setCursor(Cursor.getDefaultCursor());
				}*/
			}
		}));
		operationsPanel.add(btnDownload);

		final JButton btnUpload = new JButton(new UsnaAction(this, "btnUpload", e -> {
			final int mRow = table.convertRowIndexToModel(table.getSelectedRow());
			final Script sc = scripts.get(mRow);
			final JFileChooser fc = new JFileChooser();
			fc.setFileFilter(new FileNameExtensionFilter(LABELS.getString("filetype_js_desc"), DialogDeviceScriptsG2.FILE_EXTENSION));
			fc.addChoosableFileFilter(new FileNameExtensionFilter(LABELS.getString("filetype_sbk_desc"), Main.BACKUP_FILE_EXT));
			fc.setSelectedFile(new File(sc.getName()));
			if(fc.showOpenDialog(ScriptsPanel.this) == JFileChooser.APPROVE_OPTION) {
//				setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
				loadCodeFromFile(fc.getSelectedFile(), sc);
//				setCursor(Cursor.getDefaultCursor());
			}
		}));
		operationsPanel.add(btnUpload);
		
		final JButton logsBtn = new JButton(new UsnaAction(this, "btnLogs", e -> {
			new DialogDeviceLogsG2(owner, devicesModel, modelIndex, AbstractG2Device.LOG_WARN);
		}));
		operationsPanel.add(logsBtn);

		final JButton editBtn = new JButton(new UsnaAction(this, "edit2", e -> {
			try {
				final int mRow = table.convertRowIndexToModel(table.getSelectedRow());
				final Script sc = scripts.get(mRow);
				new ScriptEditor(ScriptsPanel.this, sc);
			} catch (IOException e1) {
				Msg.errorMsg(e1);
			}
		}));
		operationsPanel.add(editBtn);

		for(JsonNode script: Script.list(device)) {
			Script sc = new Script(device, script);
			scripts.add(sc);
			tModel.addRow(sc.getName(), sc.isEnabled(), sc.isRunning());
		}
		TableColumnModel columnModel = table.getColumnModel();
		columnModel.getColumn(0).setPreferredWidth(3000);
		columnModel.getColumn(1).setPreferredWidth(500);
		columnModel.getColumn(2).setPreferredWidth(500);

		ListSelectionListener l = e -> {
			final boolean selection = table.getSelectedRowCount() > 0;
			btnDelete.setEnabled(selection);
			btnDownload.setEnabled(selection);
			btnUpload.setEnabled(selection);
			editBtn.setEnabled(selection);
		};
		table.getSelectionModel().addListSelectionListener(l);
		l.valueChanged(null);
	}
	
	private void loadCodeFromFile(File in, Script sc) {
		try {
			String res = null;
			try (ZipFile inZip = new ZipFile(in, StandardCharsets.UTF_8)) { // backup
				String[] scriptList = inZip.stream().filter(z -> z.getName().endsWith(".mjs")).map(z -> z.getName().substring(0, z.getName().length() - 4)).toArray(String[]::new);
				if(scriptList.length > 0) {
					Object sName = JOptionPane.showInputDialog(this, LABELS.getString("scrSelectionMsg"), LABELS.getString("scrSelectionTitle"), JOptionPane.PLAIN_MESSAGE, null, scriptList, null);
					if(sName != null) {
						try (InputStream is = inZip.getInputStream(inZip.getEntry(sName + ".mjs"))) {
//							String code = new BufferedReader(new InputStreamReader(is)).lines().collect(Collectors.joining("\n"));
//							res = sc.putCode(code);
							res = sc.putCode(new InputStreamReader(is));
						}
					}
				} else {
					JOptionPane.showMessageDialog(this, LABELS.getString("scrNoneInZipFile"), LABELS.getString("btnUpload"), JOptionPane.INFORMATION_MESSAGE);
				}
			} catch (ZipException e1) { // no zip (backup) -> text file
//				String code = IOFile.readFile(in);
//				res = sc.putCode(code);
				try (Reader r = Files.newBufferedReader(in.toPath())) {
					res = sc.putCode(r);
				}
			}
			if(res != null) {
				Msg.errorMsg(this, res);
			}
		} catch (/*IO*/Exception e1) {
			Msg.errorMsg(e1);
		}
	}
	
	private class ButtonCellRenderer implements TableCellRenderer {
		private JButton runningB = new JButton(new ImageIcon(getClass().getResource("/images/Pause16.png")));
		private JButton idleB = new JButton(new ImageIcon(getClass().getResource("/images/Play16.png")));
		private JPanel running = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 0));
		private JPanel idle = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 0));
		
		public ButtonCellRenderer() {
			runningB.setBorder(BUTTON_BORDERS);
			running.add(runningB);
			idleB.setBorder(BUTTON_BORDERS);
			idle.add(idleB);
			running.setOpaque(true);
			idle.setOpaque(true);
		}
		
		@Override
		public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
			JComponent b = (value == Boolean.TRUE) ? running : idle;
			if(isSelected) {
				b.setBackground(table.getSelectionBackground());
			} else if (row % 2 == 1) {
                b.setBackground(UIManager.getColor("Table.alternateRowColor"));
            } else {
                b.setBackground(Color.WHITE);
            }
			return b;
		}
	}
	
	private class ButtonCellEditor extends AbstractCellEditor implements TableCellEditor {
		private static final long serialVersionUID = 1L;
		private JButton runningB = new JButton(new ImageIcon(getClass().getResource("/images/Pause16.png")));
		private JButton idleB = new JButton(new ImageIcon(getClass().getResource("/images/Play16.png")));
		private JPanel running = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 0));
		private JPanel idle = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 0));
		
		public ButtonCellEditor() {
			runningB.setBorder(BUTTON_BORDERS);
			running.add(runningB);
			idleB.setBorder(BUTTON_BORDERS);
			idle.add(idleB);
			running.setOpaque(true);
			idle.setOpaque(true);
			
			runningB.addActionListener(e -> {
				final int selRow = table.getSelectedRow();
				final int mRow = table.convertRowIndexToModel(selRow);
				final Script sc = scripts.get(mRow);
				try {
					ScriptsPanel.this.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
					sc.stop();
					table.setValueAt(false, selRow, 2);
					cancelCellEditing();
				} catch (IOException e1) {
					Msg.errorMsg(e1);
				} finally {
					ScriptsPanel.this.setCursor(Cursor.getDefaultCursor());
				}
			});
			
			idleB.addActionListener(e -> {
				final int selRow = table.getSelectedRow();
				final int mRow = table.convertRowIndexToModel(selRow);
				final Script sc = scripts.get(mRow);
				try {
					ScriptsPanel.this.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
					sc.run();
					table.setValueAt(true, selRow, 2);
					cancelCellEditing();
				} catch (IOException e1) {
					Msg.errorMsg(e1);
				} finally {
					ScriptsPanel.this.setCursor(Cursor.getDefaultCursor());
				}
			});
		}
		
		@Override
		public Object getCellEditorValue() {
			return null;
		}

		@Override
		public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int column) {
			JComponent b = (value == Boolean.TRUE) ? running : idle;
			b.setBackground(table.getSelectionBackground());
			return b;
		}
	}
}