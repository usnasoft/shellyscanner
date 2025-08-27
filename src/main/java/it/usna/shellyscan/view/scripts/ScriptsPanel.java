package it.usna.shellyscan.view.scripts;

import static it.usna.shellyscan.Main.LABELS;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URI;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.ProviderNotFoundException;
import java.util.ArrayList;
import java.util.Map;
import java.util.stream.Stream;

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

import it.usna.shellyscan.Main;
import it.usna.shellyscan.controller.UsnaAction;
import it.usna.shellyscan.controller.UsnaToggleAction;
import it.usna.shellyscan.model.Devices;
import it.usna.shellyscan.model.device.g2.AbstractG2Device;
import it.usna.shellyscan.model.device.g2.modules.Script;
import it.usna.shellyscan.view.scripts.ide.ScriptFrame;
import it.usna.shellyscan.view.util.Msg;
import it.usna.swing.UsnaPopupMenu;
import it.usna.swing.table.ExTooltipTable;
import it.usna.swing.table.UsnaTableModel;

public class ScriptsPanel extends JPanel {
	private static final long serialVersionUID = 1L;
	private static final Border BUTTON_BORDERS = BorderFactory.createEmptyBorder(0, 12, 0, 12);
	private static final int COL_NAME = 0;
	private static final int COL_ENABLED = 1;
	private static final int COL_RUN = 2;
	private final ExTooltipTable table;
	private final ArrayList<ScriptAndEditor> scripts = new ArrayList<>();

	public ScriptsPanel(JDialog owner, Devices devicesModel, int modelIndex) throws IOException {
		AbstractG2Device device = (AbstractG2Device) devicesModel.get(modelIndex);
		setLayout(new BorderLayout(0, 0));
		final UsnaTableModel tModel = new UsnaTableModel(LABELS.getString("lblScrColName"), LABELS.getString("lblEnabled"), LABELS.getString("lblScrColRunning"));

		table = new ExTooltipTable(tModel) {
			private static final long serialVersionUID = 1L;
			{
				((JComponent) getDefaultRenderer(Boolean.class)).setOpaque(true);
				setAutoCreateRowSorter(true);
				setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

				columnModel.getColumn(COL_RUN).setCellRenderer(new ButtonCellRenderer());
				columnModel.getColumn(COL_RUN).setCellEditor(new ButtonCellEditor());

				activateSingleCellStringCopy();
			}

			@Override
			public boolean isCellEditable(final int row, final int column) {
				final int mRow = table.convertRowIndexToModel(row);
				final int mCol = table.convertColumnIndexToModel(column);
				return mCol == COL_ENABLED || scripts.get(mRow).hasOpenEditor() == false;
			}

			// @Override // very bad when loose focus
			// public Component prepareEditor(TableCellEditor editor, int row,
			// int column) {
			// JComponent comp = (JComponent) super.prepareEditor(editor, row,
			// column);
			// comp.setBackground(table.getSelectionBackground());
			// comp.setForeground(table.getSelectionForeground());
			// return comp;
			// }

			@Override
			public void editingStopped(ChangeEvent e) {
				ScriptsPanel.this.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
				try {
					final int mCol = convertColumnIndexToModel(getEditingColumn());
					final int mRow = convertRowIndexToModel(getEditingRow());
					final Script sc = scripts.get(mRow).script;
					if (mCol == COL_NAME) {
						String ret = sc.setName((String) getCellEditor().getCellEditorValue());
						if (ret != null) {
							Msg.errorMsg(ScriptsPanel.this, ret);
						}
					} else if (mCol == COL_ENABLED) {
						String ret = sc.setEnabled((Boolean) getCellEditor().getCellEditorValue());
						if (ret != null) {
							Msg.errorMsg(ScriptsPanel.this, ret);
						}
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
			if (JOptionPane.showOptionDialog(
					ScriptsPanel.this, LABELS.getString("msgDeleteConfirm"), LABELS.getString("btnDelete"),
					JOptionPane.OK_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE, null,
					new Object[] { UIManager.getString("OptionPane.yesButtonText"), cancel }, cancel) == 0) {
				try {
					final int mRow = table.convertRowIndexToModel(table.getSelectedRow());
					final Script sc = scripts.get(mRow).script;
					sc.delete();
					scripts.remove(mRow);
					tModel.removeRow(mRow);
				} catch (IOException e1) {
					Msg.errorMsg(this, e1);
				}
			}
		}));
		operationsPanel.add(btnDelete);

		final JButton btnNew = new JButton(new UsnaAction(this, "btnNew", e -> {
			try {
				Script sc = Script.create(device, null);
				scripts.add(new ScriptAndEditor(sc));
				int mrow = tModel.addRow(sc.getName(), sc.isEnabled(), sc.isRunning());
				table.setRowSelectionInterval(0, table.convertRowIndexToView(mrow));
			} catch (IOException e1) {
				Msg.errorMsg(this, e1);
			}
		}));
		operationsPanel.add(btnNew);

		final JButton btnDownload = new JButton(new UsnaAction(this, "btnDownload", e -> {
			final int mRow = table.convertRowIndexToModel(table.getSelectedRow());
			final Script sc = scripts.get(mRow).script;
			final JFileChooser fc = new JFileChooser();
			fc.setFileFilter(new FileNameExtensionFilter(LABELS.getString("filetype_js_desc"), DialogDeviceScripts.FILE_EXTENSION));
			fc.setSelectedFile(new java.io.File(sc.getName()));
			if (fc.showSaveDialog(ScriptsPanel.this) == JFileChooser.APPROVE_OPTION) {
				try {
					Files.writeString(fc.getSelectedFile().toPath(), sc.getCode());
					Msg.showMsg(ScriptsPanel.this, "btnDownloadSuccess", LABELS.getString("btnDownload"), JOptionPane.INFORMATION_MESSAGE);
				} catch (IOException e1) {
					Msg.errorMsg(ScriptsPanel.this, LABELS.getString("msgScrNoCode"));
				}
			}
		}));
		operationsPanel.add(btnDownload);

		final JButton btnUpload = new JButton(new UsnaAction(this, "btnUpload", "btnUploadTooltip", null, null, e -> {
			final int mRow = table.convertRowIndexToModel(table.getSelectedRow());
			final Script sc = scripts.get(mRow).script;
			final JFileChooser fc = new JFileChooser();
			fc.setFileFilter(new FileNameExtensionFilter(LABELS.getString("filetype_js_desc"), DialogDeviceScripts.FILE_EXTENSION));
			fc.addChoosableFileFilter(new FileNameExtensionFilter(LABELS.getString("filetype_sbk_desc"), Main.BACKUP_FILE_EXT));
			fc.setSelectedFile(new java.io.File(sc.getName()));
			if (fc.showOpenDialog(ScriptsPanel.this) == JFileChooser.APPROVE_OPTION) {
				loadCodeFromFile(fc.getSelectedFile().toPath(), sc);
			}
		}));
		operationsPanel.add(btnUpload);

		final UsnaAction editAction = new UsnaAction(this, "edit", e -> {
			try {
				final int mRow = table.convertRowIndexToModel(table.getSelectedRow());
				final Script sc = scripts.get(mRow).script;
				final ScriptFrame editor = new ScriptFrame(ScriptsPanel.this, device, sc);
				scripts.get(mRow).editors().add(editor);
				editor.addPropertyChangeListener(ScriptFrame.RUN_EVENT, propertyChangeEvent -> tModel.setValueAt(propertyChangeEvent.getNewValue(), mRow, COL_RUN));
				editor.addPropertyChangeListener(ScriptFrame.CLOSE_EVENT, propertyChangeEvent -> {
					scripts.get(mRow).editors().remove(editor);
					tModel.fireTableCellUpdated(mRow, COL_RUN);
				});
				tModel.fireTableCellUpdated(mRow, COL_RUN);
			} catch (IOException e1) {
				Msg.errorMsg(this, e1);
			}
		});
		final JButton editBtn = new JButton(editAction);
		operationsPanel.add(editBtn);

		UsnaPopupMenu tablePopup = new UsnaPopupMenu(editAction);
		table.addMouseListener(tablePopup.getMouseListener(table));

		// Fill table
		for (Script sc : Script.list(device)) {
			scripts.add(new ScriptAndEditor(sc));
			tModel.addRow(sc.getName(), sc.isEnabled(), sc.isRunning());
		}
		TableColumnModel columnModel = table.getColumnModel();
		columnModel.getColumn(COL_NAME).setPreferredWidth(3000);
		columnModel.getColumn(COL_ENABLED).setPreferredWidth(500);
		columnModel.getColumn(COL_RUN).setPreferredWidth(500);

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
	
	private void loadCodeFromFile(Path in, Script sc) {
		try {
			String res = null;
			try(FileSystem fs = FileSystems.newFileSystem(URI.create("jar:" + in.toUri()), Map.of()); Stream<Path> pathStream = Files.list(fs.getPath("/"))) {
				String[] scriptList = pathStream.filter(p -> p.getFileName().toString().endsWith(".mjs")).map(p -> p.getFileName().toString().substring(0, p.getFileName().toString().length() - 4)).toArray(String[]::new);
				if(scriptList.length > 0) {
					Object sName = JOptionPane.showInputDialog(this, LABELS.getString("scrSelectionMsg"), LABELS.getString("scrSelectionTitle"), JOptionPane.PLAIN_MESSAGE, null, scriptList, null);
					if(sName != null) {
						String code = Files.readString(fs.getPath(sName + ".mjs")).replaceAll("\\r+\\n", "\n");
						res = sc.putCode(code);
					}
				} else {
					JOptionPane.showMessageDialog(this, LABELS.getString("scrNoneInZipFile"), LABELS.getString("btnUpload"), JOptionPane.INFORMATION_MESSAGE);
				}
			} catch (ProviderNotFoundException e) { // no zip (backup) -> text file
				String code = Files.readString(in).replaceAll("\\r+\\n", "\n");
				res = sc.putCode(code);
			}
			if (res == null) {
				Msg.showMsg(ScriptsPanel.this, "btnUploadSuccess", LABELS.getString("btnUpload"), JOptionPane.INFORMATION_MESSAGE);
			} else {
				Msg.errorMsg(this, res);
			}
		} catch (FileNotFoundException | NoSuchFileException e) {
			Msg.errorMsg(this, String.format(LABELS.getString("msgFileNotFound"), in.getFileName().toString()));
		} catch (/*IO*/Exception e) {
			Msg.errorMsg(this, e);
		}
	}

	private class ButtonCellRenderer implements TableCellRenderer {
		private JButton runningB = new JButton();
		private JPanel panel = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 0));
		private static final ImageIcon stopIcon = new ImageIcon(ButtonCellRenderer.class.getResource("/images/Stop16.png"));
		private static final ImageIcon runIcon = new ImageIcon(ButtonCellRenderer.class.getResource("/images/Play16.png"));

		public ButtonCellRenderer() {
			runningB.setBorder(BUTTON_BORDERS);
			panel.add(runningB);
			panel.setOpaque(true);
		}

		@Override
		public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
			if (isSelected) {
				panel.setBackground(table.getSelectionBackground());
			} else if (row % 2 == 1) {
				panel.setBackground(UIManager.getColor("Table.alternateRowColor"));
			} else {
				panel.setBackground(Color.WHITE);
			}
			final boolean editing = scripts.get(table.convertRowIndexToModel(row)).hasOpenEditor();
			runningB.setBackground(editing ? Color.gray : Color.WHITE);
			runningB.setIcon(value == Boolean.TRUE ? stopIcon : runIcon);
			return panel;
		}
	}

	private class ButtonCellEditor extends AbstractCellEditor implements TableCellEditor {
		private static final long serialVersionUID = 1L;
		private JPanel panel = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 0));
		private UsnaToggleAction runStopAction;

		public ButtonCellEditor() {
			runStopAction = new UsnaToggleAction(ScriptsPanel.this, "/images/Stop16.png", "/images/Play16.png",
					e -> {
						final int selRow = table.getSelectedRow();
						final int mRow = table.convertRowIndexToModel(selRow);
						final Script sc = scripts.get(mRow).script;
						try {
							sc.run();
							table.setValueAt(true, selRow, COL_RUN);
							cancelCellEditing();
						} catch (IOException e1) {
							Msg.errorMsg(ScriptsPanel.this, e1);
						}
					},
					e -> {
						final int selRow = table.getSelectedRow();
						final int mRow = table.convertRowIndexToModel(selRow);
						final Script sc = scripts.get(mRow).script;
						try {
							sc.stop();
							table.setValueAt(false, selRow, COL_RUN);
							cancelCellEditing();
						} catch (IOException e1) {
							Msg.errorMsg(ScriptsPanel.this, e1);
						}
					});

			JButton runningB = new JButton(runStopAction);
			runningB.setBorder(BUTTON_BORDERS);
			panel.add(runningB);
			panel.setOpaque(true);
		}

		@Override
		public Object getCellEditorValue() {
			return null;
		}

		@Override
		public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int column) {
			runStopAction.setSelected(value == Boolean.TRUE);
			panel.setBackground(table.getSelectionBackground());
			return panel;
		}
	}

	private record ScriptAndEditor(Script script, ArrayList<ScriptFrame> editors) {
		public ScriptAndEditor(Script script) {
			this(script, new ArrayList<ScriptFrame>());
		}

		public boolean hasOpenEditor() {
			return editors.isEmpty() == false;
		}
	}
}