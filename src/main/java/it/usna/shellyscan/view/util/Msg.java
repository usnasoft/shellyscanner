package it.usna.shellyscan.view.util;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.KeyboardFocusManager;
import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JScrollPane;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import it.usna.shellyscan.Main;
import it.usna.shellyscan.model.device.DeviceAPIException;
import it.usna.shellyscan.model.device.ShellyAbstractDevice;
import it.usna.shellyscan.model.device.ShellyAbstractDevice.Status;

public class Msg {
	private static final Logger LOG = LoggerFactory.getLogger(Msg.class);
	private static final int DEF_ROWS_MAX = 35;
	private static final Pattern PATTERN_BR = Pattern.compile("<br>");
	
	private Msg() {}
	
	/**
	 * @param owner determines the Frame in which the dialog is displayed
	 * @param message
	 * @param title
	 * @param messageType JOptionPane.ERROR_MESSAGE, JOptionPane.INFORMATION_MESSAGE, ...
	 * @param rowsMax
	 */
	public static void showHtmlMessageDialog(Component owner, CharSequence message, String title, int messageType, final int rowsMax) {
		try {
			Matcher m = PATTERN_BR.matcher(message);
			int rows = (int)m.results().count();
			if(rows <= rowsMax) {
				JOptionPane.showMessageDialog(owner, message, title, messageType);
			} else {
				JScrollPane scrollPane = new JScrollPane(new JLabel(message.toString()));
				Dimension d = scrollPane.getPreferredSize();
				d.height = owner.getGraphics().getFontMetrics().getHeight() * rowsMax;
				d.width += scrollPane.getVerticalScrollBar().getPreferredSize().width;
				scrollPane.setPreferredSize(d);
				JOptionPane.showMessageDialog(owner, scrollPane, title, messageType);
			}
		} catch(RuntimeException e) { // HeadlessException
			LOG.error(title + "-" + message.toString(), e);
		}
	}
	
	public static void showHtmlMessageDialog(Component owner, CharSequence message, String title, int messageType) {
		showHtmlMessageDialog(owner, message, title, messageType, DEF_ROWS_MAX);
	}
	
	public static void showMsg(Component owner, String msg, String title, int type) {
		try {
			if((msg == null || msg.isEmpty())) {
				if(title == null || title.isEmpty()) {
					msg = Main.LABELS.getString("errorTitle");
				} else {
					msg = title;
				}
			} else if(Main.LABELS.containsKey(msg)) {
				msg = Main.LABELS.getString(msg);
			} else if(msg.startsWith("<html>") == false) {
				msg = splitLine(msg, 128);
			}
			if(owner == null) {
				owner = KeyboardFocusManager.getCurrentKeyboardFocusManager().getFocusedWindow();
			}
			JOptionPane.showMessageDialog(owner, msg, title, type);
		} catch(RuntimeException e) { // HeadlessException
			LOG.error(title + "-" + msg, e);
		}
	}

	public static void errorMsg(Component owner, String msg) {
		showMsg(owner, msg, Main.LABELS.getString("errorTitle"), JOptionPane.ERROR_MESSAGE);
	}

	public static void errorMsg(Component owner, final Throwable t) {
		if(t instanceof IOException) {
			LOG.debug("Connection error", t);
		} else {
			LOG.error("Unexpected", t);
		}
		String msg;
		if(t instanceof DeviceAPIException api && api.getErrorMessage() != null && api.getErrorMessage().isBlank() == false) {
			msg = api.getErrorMessage();
		} else {
			msg = t.getMessage();
			if(msg == null || msg.isBlank()) {
				msg = t.toString();
			}
		}
		errorMsg(owner, msg);
	}

	public static void warningMsg(Component owner, String msg) {
		showMsg(owner, msg, Main.LABELS.getString("warningTitle"), JOptionPane.WARNING_MESSAGE);
	}
	
	public static boolean errorStatusMsg(Component owner, final ShellyAbstractDevice device, IOException e) {
		if(device.getStatus() == Status.OFF_LINE) {
			showMsg(owner, Main.LABELS.getString("Status-OFFLINE") + ".", Main.LABELS.getString("errorTitle"), JOptionPane.ERROR_MESSAGE);
			return true;
		} else if(device.getStatus() == Status.NOT_LOOGGED) {
			showMsg(owner, Main.LABELS.getString("Status-PROTECTED") + ".", Main.LABELS.getString("errorTitle"), JOptionPane.ERROR_MESSAGE);
			return true;
		} else {
			errorMsg(owner, e);
			return false;
		}
	}
	
	private static String splitLine(String str, int maxLine) {
		final String lines[] = str.split("\\R");
		String newStr = "";
		for(String line: lines) {
			while(line.length() > maxLine) {
				newStr += line.substring(0, maxLine) + "\n";
				line = line.substring(maxLine);
			}
			newStr += line + '\n';
		}
		return newStr;
	}
}