package it.usna.shellyscan.view.util;

import javax.swing.JTextPane;
import javax.swing.text.BadLocationException;
import javax.swing.text.Style;
import javax.swing.text.StyledDocument;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class UsnaTextPane extends JTextPane {
	private static final long serialVersionUID = 1L;
	private final static Logger LOG = LoggerFactory.getLogger(UsnaTextPane.class);
	private final StyledDocument doc;
	
	public UsnaTextPane() {
		this.
		doc = getStyledDocument();
	}
	
	public void append(String str) {
		try {
			doc.insertString(doc.getLength(), str, null);
		} catch (BadLocationException e) {
			LOG.error("", e);
		}
	}
	
	public void append(String str, Style style) {
		try {
			doc.insertString(doc.getLength(), str, style);
		} catch (BadLocationException e) {
			LOG.error("", e);
		}
	}

	public void insert(String str, int pos) {
		try {
			doc.insertString(pos, str, null);
		} catch (BadLocationException e) {
			LOG.error("", e);
		}
	}
	
	public void insert(String str, int pos, Style style) {
		try {
			doc.insertString(pos, str, style);
		} catch (BadLocationException e) {
			LOG.error("", e);
		}
	}
}
