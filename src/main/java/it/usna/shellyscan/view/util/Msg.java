package it.usna.shellyscan.view.util;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.HeadlessException;
import java.awt.KeyboardFocusManager;
import java.awt.Window;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JScrollPane;

public class Msg {
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
