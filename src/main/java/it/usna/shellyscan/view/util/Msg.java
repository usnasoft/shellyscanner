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
import it.usna.shellyscan.model.device.ShellyAbstractDevice;
import it.usna.shellyscan.model.device.ShellyAbstractDevice.Status;

public class Msg {
	private final static Logger LOG = LoggerFactory.getLogger(Msg.class);
	private final static int DEF_ROWS_MAX = 35;
	private final static Pattern PATTERN_BR = Pattern.compile("<br>");
	
	public static void showHtmlMessageDialog(Component parentComponent, CharSequence message, String title, int messageType, final int rowsMax) {
		try {
			Matcher m = PATTERN_BR.matcher(message);
			int rows = (int)m.results().count();
//			while(m.find()) rows++;
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
		} catch(RuntimeException e) { // HeadlessException
			LOG.error(title + "-" + message.toString(), e);
		}
	}
	
	public static void showHtmlMessageDialog(Component parentComponent, CharSequence message, String title, int messageType) {
		showHtmlMessageDialog(parentComponent, message, title, messageType, DEF_ROWS_MAX);
	}
	
	public static void showMsg(Component owner, String msg, String title, int type) /*throws HeadlessException*/ {
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
			JOptionPane.showMessageDialog(owner, msg, title, type);
		} catch(RuntimeException e) { // HeadlessException
			LOG.error(title + "-" + msg, e);
		}
	}
	
//	private static void errorMsg(String msg, String title) {
//		final Window win = KeyboardFocusManager.getCurrentKeyboardFocusManager().getFocusedWindow();
//		showMsg(win, msg, title, JOptionPane.ERROR_MESSAGE);
//	}
	
//	public static void errorMsg(String msg) {
//		Msg.errorMsg(msg, Main.LABELS.getString("errorTitle"));
//	}
	
	public static void errorMsg(Component owner, String msg) {
		showMsg(owner, msg, Main.LABELS.getString("errorTitle"), JOptionPane.ERROR_MESSAGE);
	}

	public static void errorMsg(Component owner, final Throwable t) {
		if(t instanceof IOException) {
			LOG.debug("Connection error", t);
		} else {
			LOG.error("Unexpected", t);
		}
		String msg = t.getMessage();
		if(msg == null || msg.isEmpty()) {
			msg = t.toString();
		}
		errorMsg(owner, msg);
	}
	
	public static void errorMsg(final Throwable t) {
		final Component win = KeyboardFocusManager.getCurrentKeyboardFocusManager().getFocusedWindow();
		errorMsg(win, t);
	}
	
	public static void warningMsg(Component owner, String msg) {
		showMsg(owner, msg, Main.LABELS.getString("warningTitle"), JOptionPane.WARNING_MESSAGE);
	}
	
	public static void errorStatusMsg(Component owner, final ShellyAbstractDevice device, IOException e) {
		if(device.getStatus() == Status.OFF_LINE) {
			showMsg(owner, Main.LABELS.getString("Status-OFFLINE") + ".", Main.LABELS.getString("errorTitle"), JOptionPane.ERROR_MESSAGE);
		} else if(device.getStatus() == Status.NOT_LOOGGED) {
			showMsg(owner, Main.LABELS.getString("Status-PROTECTED") + ".", Main.LABELS.getString("errorTitle"), JOptionPane.ERROR_MESSAGE);
		} else {
			errorMsg(owner, e);
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