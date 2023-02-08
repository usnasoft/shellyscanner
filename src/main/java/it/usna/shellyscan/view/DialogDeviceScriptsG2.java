package it.usna.shellyscan.view;

import static it.usna.shellyscan.Main.LABELS;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.GridLayout;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.stream.Collectors;
import java.util.zip.ZipException;
import java.util.zip.ZipFile;

import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ListSelectionModel;
import javax.swing.UIManager;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableCellEditor;

import com.fasterxml.jackson.databind.JsonNode;

import it.usna.shellyscan.Main;
import it.usna.shellyscan.model.device.g2.AbstractG2Device;
import it.usna.shellyscan.model.device.g2.modules.Script;
import it.usna.shellyscan.view.util.UtilCollecion;
import it.usna.swing.table.ExTooltipTable;
import it.usna.swing.table.UsnaTableModel;
import it.usna.util.IOFile;

public class DialogDeviceScriptsG2 extends JDialog {
	private static final long serialVersionUID = 1L;
	private final ExTooltipTable table;
	private final static String YES = LABELS.getString("true_yn");
	private final static String NO = LABELS.getString("false_yn");

	private final ArrayList<Script> model = new ArrayList<>();
	private final UsnaTableModel tModel = new UsnaTableModel(LABELS.getString("lblScrColName"), LABELS.getString("lblScrColEnabled"), LABELS.getString("lblScrColRunning"));

	public DialogDeviceScriptsG2(final MainView owner, AbstractG2Device device) {
		super(owner, false);
		setTitle(String.format(LABELS.getString("dlgScriptTitle"), UtilCollecion.getExtendedHostName(device)));
		setDefaultCloseOperation(/*DO_NOTHING_ON_CLOSE*/DISPOSE_ON_CLOSE);

		JPanel buttonsPanel = new JPanel();
		getContentPane().add(buttonsPanel, BorderLayout.SOUTH);

		JButton jButtonClose = new JButton(LABELS.getString("dlgClose"));
		jButtonClose.addActionListener(event -> dispose());

		buttonsPanel.add(jButtonClose);

		JPanel panel = new JPanel();
		panel.setLayout(new BorderLayout(0, 0));

		getContentPane().add(panel, BorderLayout.CENTER);

		JScrollPane scrollPane = new JScrollPane();
		panel.add(scrollPane, BorderLayout.CENTER);

		table = new ExTooltipTable(tModel) {
			private static final long serialVersionUID = 1L;
			{
				((JComponent) getDefaultRenderer(Boolean.class)).setOpaque(true);
				setAutoCreateRowSorter(true);
				setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

				DefaultTableCellRenderer centerRenderer = new DefaultTableCellRenderer();
				centerRenderer.setHorizontalAlignment(JLabel.CENTER);
				columnModel.getColumn(2).setCellRenderer(centerRenderer);
			}

			@Override
			public boolean isCellEditable(final int row, final int column) {
				return convertColumnIndexToModel(column) != 2;
			}

			@Override
			public Component prepareEditor(TableCellEditor editor, int row, int column) {
				JComponent comp = (JComponent)super.prepareEditor(editor, row, column);
				comp.setBackground(table.getSelectionBackground());
				return comp;
			}

			public void editingStopped(ChangeEvent e) {
				// System.out.println(getCellEditor().getCellEditorValue());
				final int mCol = convertColumnIndexToModel(getEditingColumn());
				final int mRow = convertRowIndexToModel(getEditingRow());
				final Script sc = model.get(mRow);
				if(mCol == 0) { // name
					sc.setName((String)getCellEditor().getCellEditorValue());
				} else if(mCol == 1) { // enabled
					sc.setEnabled((Boolean)getCellEditor().getCellEditorValue());
				}
				super.editingStopped(e);
			}
		};
		scrollPane.setViewportView(table);

		JPanel operationsPanel = new JPanel();
		operationsPanel.setBackground(Color.WHITE);
		panel.add(operationsPanel, BorderLayout.SOUTH);

		JButton btnDelete = new JButton(LABELS.getString("btnDelete"));
		btnDelete.addActionListener(e -> {
			final String cancel = UIManager.getString("OptionPane.cancelButtonText");
			if(JOptionPane.showOptionDialog(
					DialogDeviceScriptsG2.this, LABELS.getString("msdDeleteConfitm"), LABELS.getString("btnDelete"),
					JOptionPane.OK_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE, null,
					new Object[] {UIManager.getString("OptionPane.yesButtonText"), cancel}, cancel) == 0) {
				try {
					final int mRow = table.convertRowIndexToModel(table.getSelectedRow());
					final Script sc = model.get(mRow);
					sc.delete();
					model.remove(mRow);
					tModel.removeRow(mRow);
				} catch (IOException e1) {
					Main.errorMsg(e1);
				}
			}
		});
		operationsPanel.setLayout(new GridLayout(1, 0, 2, 0));
		operationsPanel.add(btnDelete);

		JButton btnNew = new JButton(LABELS.getString("btnNew"));
		btnNew.addActionListener(e -> {
			try {
				Script sc = Script.create(device, null);
				model.add(sc);
				tModel.addRow(new Object [] {sc.getName(), sc.isEnabled(), sc.isRunning() ? YES : NO});
			} catch (IOException e1) {
				Main.errorMsg(e1);
			}
		});
		operationsPanel.add(btnNew);

		JButton btnRun = new JButton(LABELS.getString("btnRun"));
		operationsPanel.add(btnRun);
		btnRun.addActionListener(e -> {
			final int selRow = table.getSelectedRow();
			final int mRow = table.convertRowIndexToModel(selRow);
			final Script sc = model.get(mRow);
			try {
				sc.run();
				table.setValueAt(sc.isRunning() ? YES : NO, selRow, 2);
			} catch (IOException e1) {
				Main.errorMsg(e1);
			}
		});

		JButton btnStop = new JButton(LABELS.getString("btnStop"));
		operationsPanel.add(btnStop);
		btnStop.addActionListener(e -> {
			final int selRow = table.getSelectedRow();
			final int mRow = table.convertRowIndexToModel(selRow);
			final Script sc = model.get(mRow);
			try {
				sc.stop();
				table.setValueAt(sc.isRunning() ? YES : NO, selRow, 2);
			} catch (IOException e1) {
				Main.errorMsg(e1);
			}
		});

		JButton btnDownload = new JButton(LABELS.getString("btnDownload"));
		operationsPanel.add(btnDownload);
		btnDownload.addActionListener(e -> {
			final int mRow = table.convertRowIndexToModel(table.getSelectedRow());
			final Script sc = model.get(mRow);
			final JFileChooser fc = new JFileChooser();
			fc.setSelectedFile(new File(sc.getName()));
			if(fc.showSaveDialog(DialogDeviceScriptsG2.this) == JFileChooser.APPROVE_OPTION) {
				setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
				try (FileWriter w = new FileWriter(fc.getSelectedFile())) {
					w.write(sc.getCode());
				} catch (IOException e1) {
					Main.errorMsg(LABELS.getString("msgScrNoCode"));
				} finally {
					setCursor(Cursor.getDefaultCursor());
				}
			}
		});

		JButton btnUpload = new JButton(LABELS.getString("btnUpload"));
		operationsPanel.add(btnUpload);
		btnUpload.addActionListener(e -> {
			final int mRow = table.convertRowIndexToModel(table.getSelectedRow());
			final Script sc = model.get(mRow);
			final JFileChooser fc = new JFileChooser();
			fc.addChoosableFileFilter(new FileNameExtensionFilter(LABELS.getString("filetype_sbk_desc"), Main.BACKUP_FILE_EXT));
			fc.setSelectedFile(new File(sc.getName()));
			if(fc.showOpenDialog(DialogDeviceScriptsG2.this) == JFileChooser.APPROVE_OPTION) {
				setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
				loadCodeFromFile(fc.getSelectedFile(), sc);
			}
		});

		try {
			for(JsonNode script: Script.list(device)) {
				Script sc = new Script(device, script);
				model.add(sc);
				tModel.addRow(new Object [] {sc.getName(), sc.isEnabled(), sc.isRunning() ? YES : NO});
			}
		} catch (IOException e) {
			Main.errorMsg(e);
		}

		ListSelectionListener l = e -> {
			final boolean selection = table.getSelectedRowCount() > 0;
			btnDelete.setEnabled(selection);
			btnRun.setEnabled(selection);
			btnStop.setEnabled(selection);
			btnDownload.setEnabled(selection);
			btnUpload.setEnabled(selection);
		};
		table.getSelectionModel().addListSelectionListener(l);
		l.valueChanged(null);

		this.setSize(490, 320);

		setLocationRelativeTo(owner);
		setVisible(true);

		table.columnsWidthAdapt();
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
							String code = new BufferedReader(new InputStreamReader(is)).lines().collect(Collectors.joining("\n"));
							res = sc.putCode(code);
						}
					}
				} else {
					JOptionPane.showMessageDialog(this, LABELS.getString("scrNoneInZipFile"), LABELS.getString("btnUpload"), JOptionPane.INFORMATION_MESSAGE);
				}
			} catch (ZipException e1) { // no zip (backup) -> text file
				String code = IOFile.readFile(in);
				res = sc.putCode(code);
			}
			if(res != null) {
				Main.errorMsg(res);
			}
		} catch (/*IO*/Exception e1) {
			Main.errorMsg(e1);
		} finally {
			setCursor(Cursor.getDefaultCursor());
		}
	}
}