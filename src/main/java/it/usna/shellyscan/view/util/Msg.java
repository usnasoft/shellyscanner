package it.usna.shellyscan.view.util;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.HeadlessException;
import java.awt.KeyboardFocusManager;
import java.awt.Window;
import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JScrollPane;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import it.usna.shellyscan.Main;
import it.usna.shellyscan.model.device.ShellyAbstractDevice;
import it.usna.shellyscan.model.device.ShellyAbstractDevice.Status;

public class Msg {
	private final static Logger LOG = LoggerFactory.getLogger(Msg.class);
	private final static int ROWS_MAX = 35;
	private final static Pattern PATTERN_BR = Pattern.compile("<br>");
	
	public static void showHtmlMessageDialog(Component parentComponent, CharSequence message, String title, int messageType, final int rowsMax) throws HeadlessException {
		int rows = 1;
		Matcher m = PATTERN_BR.matcher(message);
		while(m.find()) rows++;
		if(rows <= rowsMax) {
			JOptionPane.showMessageDialog(parentComponent, message, title, messageType);
		} else {
			JScrollPane scrollPane = new JScrollPane(new JLabel(message.toString()));
			Dimension d = scrollPane.getPreferredSize();
			d.height = parentComponent.getGraphics().getFontMetrics().getHeight() * rowsMax;
			d.width += scrollPane.getVerticalScrollBar().getPreferredSize().width;
			scrollPane.setPreferredSize(d);
			JOptionPane.showMessageDialog(parentComponent, scrollPane, title, messageType);
		}
	}
	
	public static void showHtmlMessageDialog(Component parentComponent, CharSequence message, String title, int messageType) throws HeadlessException {
		showHtmlMessageDialog(parentComponent, message, title, messageType, ROWS_MAX);
	}
	
	// Errors
	public static void errorMsg(Window owner, String msg, String title) {
		if((msg == null || msg.isEmpty())) {
			msg = "Error";
		} else if(msg.startsWith("<html>") == false) {
			msg = splitLine(msg, 128);
		}
		JOptionPane.showMessageDialog(owner, msg, title, JOptionPane.ERROR_MESSAGE);
	}
	
	public static void errorMsg(String msg, String title) {
		final Window win = KeyboardFocusManager.getCurrentKeyboardFocusManager().getFocusedWindow();
		errorMsg(win, msg, title);
	}
	
	public static void errorMsg(String msg) {
		Msg.errorMsg(msg, Main.LABELS.getString("errorTitle"));
	}
	
	public static void errorMsg(final Throwable t) {
		if(t instanceof IOException) {
			LOG.debug("Connection error", t);
		} else {
			LOG.error("Unexpected", t);
		}
		String message = t.getMessage();
		if(message == null || message.isEmpty()) {
			message = t.toString();
		}
		errorMsg(message);
	}
	
	public static void errorStatusMsg(Window owner, final ShellyAbstractDevice device, IOException e) {
		if(device.getStatus() == Status.OFF_LINE) {
			errorMsg(owner, Main.LABELS.getString("Status-OFFLINE") + ".", Main.LABELS.getString("errorTitle"));
		} else if(device.getStatus() == Status.NOT_LOOGGED) {
			errorMsg(owner, Main.LABELS.getString("Status-PROTECTED") + ".", Main.LABELS.getString("errorTitle"));
		} else {
			errorMsg(e);
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
