package it.usna.shellyscan.view.appsettings;

import static it.usna.shellyscan.Main.LABELS;

import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.nio.file.Paths;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.event.ChangeListener;
import javax.swing.filechooser.FileNameExtensionFilter;

import it.usna.shellyscan.Main;
import it.usna.util.AppProperties;

public class PanelStore extends JPanel {
	private static final long serialVersionUID = 1L;
	JCheckBox chckbxUseStore = new JCheckBox();
	JTextField textFieldStoreFileName;

	PanelStore(final AppProperties appProp) {
		GridBagLayout gridBagLayout = new GridBagLayout();
		gridBagLayout.rowHeights = new int[] {0, 0, 1};
		gridBagLayout.columnWeights = new double[]{1.0, 1.0, 0.0};
		gridBagLayout.columnWidths = new int[]{0, 10, 0};
		setLayout(gridBagLayout);

		JLabel lblNewLabel = new JLabel(LABELS.getString("dlgAppStoreUseLabel"));
		lblNewLabel.setFont(new Font("Tahoma", Font.BOLD, 11));
		lblNewLabel.setHorizontalAlignment(SwingConstants.LEFT);
		GridBagConstraints gbc_lblNewLabel = new GridBagConstraints();
		gbc_lblNewLabel.fill = GridBagConstraints.VERTICAL;
		gbc_lblNewLabel.anchor = GridBagConstraints.NORTHWEST;
		gbc_lblNewLabel.insets = new Insets(0, 0, 10, 15);
		gbc_lblNewLabel.gridx = 0;
		gbc_lblNewLabel.gridy = 0;
		add(lblNewLabel, gbc_lblNewLabel);
		
		GridBagConstraints gbc_chckbxUseStore = new GridBagConstraints();
		gbc_chckbxUseStore.insets = new Insets(0, 0, 5, 5);
		gbc_chckbxUseStore.weightx = 100.0;
		gbc_chckbxUseStore.anchor = GridBagConstraints.NORTHWEST;
		gbc_chckbxUseStore.gridx = 1;
		gbc_chckbxUseStore.gridy = 0;
		add(chckbxUseStore, gbc_chckbxUseStore);

		chckbxUseStore.setSelected(appProp.getBoolProperty(DialogAppSettings.PROP_USE_ARCHIVE, true));
		
		JLabel lblStoreFile = new JLabel(LABELS.getString("dlgAppStoreFileLabel"));
		lblStoreFile.setFont(new Font("Tahoma", Font.BOLD, 11));
		GridBagConstraints gbc_lblStoreFile = new GridBagConstraints();
		gbc_lblStoreFile.fill = GridBagConstraints.VERTICAL;
		gbc_lblStoreFile.anchor = GridBagConstraints.NORTHWEST;
		gbc_lblStoreFile.insets = new Insets(0, 0, 10, 15);
		gbc_lblStoreFile.gridx = 0;
		gbc_lblStoreFile.gridy = 1;
		add(lblStoreFile, gbc_lblStoreFile);
		
		textFieldStoreFileName = new JTextField(appProp.getProperty(DialogAppSettings.PROP_ARCHIVE_FILE, Paths.get(System.getProperty("user.home"), "ShellyStore.arc").toString()));
		GridBagConstraints gbc_textFieldStoreFileName = new GridBagConstraints();
		gbc_textFieldStoreFileName.insets = new Insets(0, 0, 5, 5);
		gbc_textFieldStoreFileName.fill = GridBagConstraints.HORIZONTAL;
		gbc_textFieldStoreFileName.gridx = 1;
		gbc_textFieldStoreFileName.gridy = 1;
		add(textFieldStoreFileName, gbc_textFieldStoreFileName);
		textFieldStoreFileName.setColumns(10);
		
		JButton btnFile = new JButton(LABELS.getString("dlgAppStoreFileButtonLabel"));
		GridBagConstraints gbc_btnFile = new GridBagConstraints();
		gbc_btnFile.weightx = 1.0;
		gbc_btnFile.insets = new Insets(0, 0, 5, 0);
		gbc_btnFile.gridx = 2;
		gbc_btnFile.gridy = 1;
		add(btnFile, gbc_btnFile);
		
		JLabel lblNewLabel_1 = new JLabel("");
		GridBagConstraints gbc_lblNewLabel_1 = new GridBagConstraints();
		gbc_lblNewLabel_1.weighty = 1.0;
//		gbc_lblNewLabel_1.insets = new Insets(0, 0, 0, 5);
		gbc_lblNewLabel_1.gridx = 0;
		gbc_lblNewLabel_1.gridy = 2;
		add(lblNewLabel_1, gbc_lblNewLabel_1);
		
		btnFile.addActionListener(event -> {
			final JFileChooser fc = new JFileChooser(Paths.get(textFieldStoreFileName.getText()).getParent().toFile());
			fc.setFileFilter(new FileNameExtensionFilter(Main.ARCHIVE_FILE_EXT, Main.ARCHIVE_FILE_EXT));
			if(fc.showSaveDialog(PanelStore.this) == JFileChooser.APPROVE_OPTION) {
				textFieldStoreFileName.setText(fc.getSelectedFile().getPath());
			}
		});
		
		ChangeListener enableListener = e -> {
			textFieldStoreFileName.setEnabled(chckbxUseStore.isSelected());
			btnFile.setEnabled(chckbxUseStore.isSelected());
		};

		chckbxUseStore.addChangeListener(enableListener);
		enableListener.stateChanged(null);
	}
}