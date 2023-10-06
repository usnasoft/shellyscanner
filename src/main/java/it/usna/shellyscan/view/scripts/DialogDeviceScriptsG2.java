package it.usna.shellyscan.view.scripts;

import static it.usna.shellyscan.Main.LABELS;

import java.awt.BorderLayout;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JPanel;

import it.usna.shellyscan.model.device.g2.AbstractG2Device;
import it.usna.shellyscan.view.MainView;
import it.usna.shellyscan.view.util.UtilMiscellaneous;

public class DialogDeviceScriptsG2 extends JDialog {
	private static final long serialVersionUID = 1L;
	public final static String FILE_EXTENSION = "js";
//	private final static Border BUTTON_BORDERS = BorderFactory.createEmptyBorder(0, 12, 0, 12);
//	private final ExTooltipTable table;
//
//	private final ArrayList<Script> scripts = new ArrayList<>();
//	private final UsnaTableModel tModel = new UsnaTableModel(LABELS.getString("lblScrColName"), LABELS.getString("lblScrColEnabled"), LABELS.getString("lblScrColRunning"));

	public DialogDeviceScriptsG2(final MainView owner, AbstractG2Device device) {
		super(owner, false);
		setTitle(String.format(LABELS.getString("dlgScriptTitle"), UtilMiscellaneous.getExtendedHostName(device)));
		setDefaultCloseOperation(/*DO_NOTHING_ON_CLOSE*/DISPOSE_ON_CLOSE);

		JPanel buttonsPanel = new JPanel();
		getContentPane().add(buttonsPanel, BorderLayout.SOUTH);

		JButton jButtonClose = new JButton(LABELS.getString("dlgClose"));
		jButtonClose.addActionListener(event -> dispose());

		buttonsPanel.add(jButtonClose);

		JPanel panel = new ScriptsPanel(device);
//		panel.setLayout(new BorderLayout(0, 0));

		getContentPane().add(panel, BorderLayout.CENTER);

//		JScrollPane scrollPane = new JScrollPane();
//		panel.add(scrollPane, BorderLayout.CENTER);
//
//		table = new ExTooltipTable(tModel) {
//			private static final long serialVersionUID = 1L;
//			{
//				((JComponent) getDefaultRenderer(Boolean.class)).setOpaque(true);
//				setAutoCreateRowSorter(true);
//				setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
//
//				columnModel.getColumn(2).setCellRenderer(new ButtonCellRenderer());
//				columnModel.getColumn(2).setCellEditor(new ButtonCellEditor());
//			}
//
//			@Override
//			public boolean isCellEditable(final int row, final int column) {
//				return true;
//			}
//
//			@Override
//			public Component prepareEditor(TableCellEditor editor, int row, int column) {
//				JComponent comp = (JComponent)super.prepareEditor(editor, row, column);
//				comp.setBackground(table.getSelectionBackground());
//				return comp;
//			}
//
//			public void editingStopped(ChangeEvent e) {
//				DialogDeviceScriptsG2.this.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
//				try {
//					final int mCol = convertColumnIndexToModel(getEditingColumn());
//					final int mRow = convertRowIndexToModel(getEditingRow());
//					final Script sc = scripts.get(mRow);
//					if(mCol == 0) { // name
//						sc.setName((String)getCellEditor().getCellEditorValue());
//					} else if(mCol == 1) { // enabled
//						sc.setEnabled((Boolean)getCellEditor().getCellEditorValue());
//					}
//					super.editingStopped(e);
//				} finally {
//					DialogDeviceScriptsG2.this.setCursor(Cursor.getDefaultCursor());
//				}
//			}
//		};
//		scrollPane.setViewportView(table);
//
//		JPanel operationsPanel = new JPanel();
//		operationsPanel.setLayout(new GridLayout(1, 0, 2, 0));
//		operationsPanel.setBackground(Color.WHITE);
//		panel.add(operationsPanel, BorderLayout.SOUTH);
//
//		JButton btnDelete = new JButton(LABELS.getString("btnDelete"));
//		btnDelete.addActionListener(e -> {
//			final String cancel = UIManager.getString("OptionPane.cancelButtonText");
//			if(JOptionPane.showOptionDialog(
//					DialogDeviceScriptsG2.this, LABELS.getString("msdDeleteConfitm"), LABELS.getString("btnDelete"),
//					JOptionPane.OK_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE, null,
//					new Object[] {UIManager.getString("OptionPane.yesButtonText"), cancel}, cancel) == 0) {
//				try {
//					final int mRow = table.convertRowIndexToModel(table.getSelectedRow());
//					final Script sc = scripts.get(mRow);
//					sc.delete();
//					scripts.remove(mRow);
//					tModel.removeRow(mRow);
//				} catch (IOException e1) {
//					Msg.errorMsg(e1);
//				}
//			}
//		});
//		operationsPanel.add(btnDelete);
//
//		JButton btnNew = new JButton(LABELS.getString("btnNew"));
//		btnNew.addActionListener(e -> {
//			try {
//				Script sc = Script.create(device, null);
//				scripts.add(sc);
//				tModel.addRow(new Object [] {sc.getName(), sc.isEnabled(), sc.isRunning()});
//			} catch (IOException e1) {
//				Msg.errorMsg(e1);
//			}
//		});
//		operationsPanel.add(btnNew);
//
//		JButton btnDownload = new JButton(LABELS.getString("btnDownload"));
//		operationsPanel.add(btnDownload);
//		btnDownload.addActionListener(e -> {
//			final int mRow = table.convertRowIndexToModel(table.getSelectedRow());
//			final Script sc = scripts.get(mRow);
//			final JFileChooser fc = new JFileChooser();
//			fc.addChoosableFileFilter(new FileNameExtensionFilter(LABELS.getString("filetype_js_desc"), DialogDeviceScriptsG2.FILE_EXTENSION));
//			fc.setSelectedFile(new File(sc.getName()));
//			if(fc.showSaveDialog(DialogDeviceScriptsG2.this) == JFileChooser.APPROVE_OPTION) {
//				setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
//				try (FileWriter w = new FileWriter(fc.getSelectedFile())) {
//					w.write(sc.getCode());
//				} catch (IOException e1) {
//					Msg.errorMsg(DialogDeviceScriptsG2.this, LABELS.getString("msgScrNoCode"));
//				} finally {
//					setCursor(Cursor.getDefaultCursor());
//				}
//			}
//		});
//
//		JButton btnUpload = new JButton(LABELS.getString("btnUpload"));
//		operationsPanel.add(btnUpload);
//		btnUpload.addActionListener(e -> {
//			final int mRow = table.convertRowIndexToModel(table.getSelectedRow());
//			final Script sc = scripts.get(mRow);
//			final JFileChooser fc = new JFileChooser();
//			fc.addChoosableFileFilter(new FileNameExtensionFilter(LABELS.getString("filetype_js_desc"), DialogDeviceScriptsG2.FILE_EXTENSION));
//			fc.addChoosableFileFilter(new FileNameExtensionFilter(LABELS.getString("filetype_sbk_desc"), Main.BACKUP_FILE_EXT));
//			fc.setSelectedFile(new File(sc.getName()));
//			if(fc.showOpenDialog(DialogDeviceScriptsG2.this) == JFileChooser.APPROVE_OPTION) {
//				setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
//				loadCodeFromFile(fc.getSelectedFile(), sc);
//				setCursor(Cursor.getDefaultCursor());
//			}
//		});
//		
//		JButton editBtn = new JButton("Edit");
//		operationsPanel.add(editBtn);
//		editBtn.addActionListener(e -> {
//			final int mRow = table.convertRowIndexToModel(table.getSelectedRow());
//			final Script sc = scripts.get(mRow);
//			try {
//				new ScriptEditor(DialogDeviceScriptsG2.this, sc);
//			} catch (IOException e1) {
//				Msg.errorMsg(e1);
//			}
//		});
//
//		try {
//			for(JsonNode script: Script.list(device)) {
//				Script sc = new Script(device, script);
//				scripts.add(sc);
//				tModel.addRow(new Object [] {sc.getName(), sc.isEnabled(), sc.isRunning()});
//			}
//			TableColumnModel columnModel = table.getColumnModel();
//			columnModel.getColumn(0).setPreferredWidth(3000);
//			columnModel.getColumn(1).setPreferredWidth(500);
//			columnModel.getColumn(2).setPreferredWidth(500);
//		} catch (IOException e) {
//			Msg.errorMsg(e);
//		}
//
//		ListSelectionListener l = e -> {
//			final boolean selection = table.getSelectedRowCount() > 0;
//			btnDelete.setEnabled(selection);
//			btnDownload.setEnabled(selection);
//			btnUpload.setEnabled(selection);
//			editBtn.setEnabled(selection);
//		};
//		table.getSelectionModel().addListSelectionListener(l);
//		l.valueChanged(null);

		setSize(490, 320);
		setLocationRelativeTo(owner);
		setVisible(true);
	}

//	private void loadCodeFromFile(File in, Script sc) {
//		try {
//			String res = null;
//			try (ZipFile inZip = new ZipFile(in, StandardCharsets.UTF_8)) { // backup
//				String[] scriptList = inZip.stream().filter(z -> z.getName().endsWith(".mjs")).map(z -> z.getName().substring(0, z.getName().length() - 4)).toArray(String[]::new);
//				if(scriptList.length > 0) {
//					Object sName = JOptionPane.showInputDialog(this, LABELS.getString("scrSelectionMsg"), LABELS.getString("scrSelectionTitle"), JOptionPane.PLAIN_MESSAGE, null, scriptList, null);
//					if(sName != null) {
//						try (InputStream is = inZip.getInputStream(inZip.getEntry(sName + ".mjs"))) {
//							String code = new BufferedReader(new InputStreamReader(is)).lines().collect(Collectors.joining("\n"));
//							res = sc.putCode(code);
//						}
//					}
//				} else {
//					JOptionPane.showMessageDialog(this, LABELS.getString("scrNoneInZipFile"), LABELS.getString("btnUpload"), JOptionPane.INFORMATION_MESSAGE);
//				}
//			} catch (ZipException e1) { // no zip (backup) -> text file
//				String code = IOFile.readFile(in);
//				res = sc.putCode(code);
//			}
//			if(res != null) {
//				Msg.errorMsg(this, res);
//			}
//		} catch (/*IO*/Exception e1) {
//			Msg.errorMsg(e1);
//		} finally {
//			setCursor(Cursor.getDefaultCursor());
//		}
//	}
	
//	private class ButtonCellRenderer implements TableCellRenderer {
//		private JButton runningB = new JButton(new ImageIcon(getClass().getResource("/images/Pause16.png")));
//		private JButton idleB = new JButton(new ImageIcon(getClass().getResource("/images/Play16.png")));
//		private JPanel running = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 0));
//		private JPanel idle = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 0));
//		
//		public ButtonCellRenderer() {
//			runningB.setBorder(BUTTON_BORDERS);
//			running.add(runningB);
//			idleB.setBorder(BUTTON_BORDERS);
//			idle.add(idleB);
//			running.setOpaque(true);
//			idle.setOpaque(true);
//		}
//		
//		@Override
//		public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
//			JComponent b = (value == Boolean.TRUE) ? running : idle;
//			if(isSelected) {
//				b.setBackground(table.getSelectionBackground());
//			} else if (row % 2 == 1) {
//                b.setBackground(UIManager.getColor("Table.alternateRowColor"));
//            } else {
//                b.setBackground(Color.WHITE);
//            }
//			return b;
//		}
//	}
//	
//	public class ButtonCellEditor extends AbstractCellEditor implements TableCellEditor {
//		private static final long serialVersionUID = 1L;
//		private JButton runningB = new JButton(new ImageIcon(getClass().getResource("/images/Pause16.png")));
//		private JButton idleB = new JButton(new ImageIcon(getClass().getResource("/images/Play16.png")));
//		private JPanel running = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 0));
//		private JPanel idle = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 0));
//		public ButtonCellEditor() {
//			runningB.setBorder(BUTTON_BORDERS);
//			running.add(runningB);
//			idleB.setBorder(BUTTON_BORDERS);
//			idle.add(idleB);
//			running.setOpaque(true);
//			idle.setOpaque(true);
//			
//			runningB.addActionListener(e -> {
//				final int selRow = table.getSelectedRow();
//				final int mRow = table.convertRowIndexToModel(selRow);
//				final Script sc = scripts.get(mRow);
//				try {
//					DialogDeviceScriptsG2.this.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
//					sc.stop();
//					table.setValueAt(false, selRow, 2);
//					cancelCellEditing();
//				} catch (IOException e1) {
//					Msg.errorMsg(e1);
//				} finally {
//					DialogDeviceScriptsG2.this.setCursor(Cursor.getDefaultCursor());
//				}
//			});
//			
//			idleB.addActionListener(e -> {
//				final int selRow = table.getSelectedRow();
//				final int mRow = table.convertRowIndexToModel(selRow);
//				final Script sc = scripts.get(mRow);
//				try {
//					DialogDeviceScriptsG2.this.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
//					sc.run();
//					table.setValueAt(true, selRow, 2);
//					cancelCellEditing();
//				} catch (IOException e1) {
//					Msg.errorMsg(e1);
//				} finally {
//					DialogDeviceScriptsG2.this.setCursor(Cursor.getDefaultCursor());
//				}
//			});
//		}
//		
//		@Override
//		public Object getCellEditorValue() {
//			return null;
//		}
//
//		@Override
//		public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int column) {
//			JComponent b = (value == Boolean.TRUE) ? running : idle;
//			b.setBackground(table.getSelectionBackground());
//			return b;
//		}
//	}
} // 274 - 360