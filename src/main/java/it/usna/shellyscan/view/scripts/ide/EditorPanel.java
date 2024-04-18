package it.usna.shellyscan.view.scripts.ide;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.Action;
import javax.swing.JComponent;
import javax.swing.KeyStroke;
import javax.swing.text.AbstractDocument;
import javax.swing.text.BadLocationException;
import javax.swing.text.Element;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.Style;
import javax.swing.text.StyleConstants;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import it.usna.shellyscan.view.util.ScannerProperties;
import it.usna.swing.texteditor.SyntaxEditor;

public class EditorPanel extends SyntaxEditor {
	private static final long serialVersionUID = 1L;
//	private final static Pattern LINE_START_NOT_EMPTY = Pattern.compile("(^)(.+)", Pattern.MULTILINE);
	private final static Pattern LINE_START = Pattern.compile("(^)(.*)", Pattern.MULTILINE);
	private final static Pattern COMMENTED_LINE = Pattern.compile("(^)(\\s*)//", Pattern.MULTILINE);
	private final static Pattern LINE_TAB = Pattern.compile("(^)\\t", Pattern.MULTILINE);
	private final static Pattern LINE_SPACES = Pattern.compile("[ \t]*");
//	private final static Pattern START_BLOCK = Pattern.compile("\\{[ \t]*$");
	private final static Pattern START_BLOCK = Pattern.compile("\\{");
//	private final static Pattern END_BLOCK = Pattern.compile("[ \t]*}");
//	private final static Pattern END_BLOCK = Pattern.compile("\\}");
	private final static Pattern END_BLOCK_FIND_TAB = Pattern.compile("\\s*(\t)+\\s*$");
	private final boolean darkMode = ScannerProperties.get().getBoolProperty(ScannerProperties.PROP_IDE_DARK);
	private final static Logger LOG = LoggerFactory.getLogger(EditorPanel.class);
	
	EditorPanel(String initText) {
		super(baseStyle());
		setTabSize(ScannerProperties.get().getIntProperty(ScannerProperties.PROP_IDE_TAB_SIZE, ScannerProperties.IDE_TAB_SIZE_DEFAULT));
		activateUndo();
		setText(initText);
		setCaretPosition(0);
		resetUndo();
		
		Style styleOperators = addStyle("usna_operator", null);
		Style styleStr = addStyle("usna_string", null);
		Style styleReserved = addStyle("usna_reserved", null);
		Style styleImplemented = addStyle("usna_implemented", null);
		if(darkMode) {
			setBackground(new Color(0,0,0,0));
			setCaretColor(Color.WHITE);
			
			StyleConstants.setForeground(styleOperators, new Color(255, 153, 51));
			StyleConstants.setForeground(styleStr, new Color(128, 255, 0));
			StyleConstants.setForeground(styleReserved, Color.CYAN);
			StyleConstants.setForeground(styleImplemented, new Color(255, 153, 255));
		} else {
			StyleConstants.setForeground(styleOperators, new Color(150, 0, 0));
			StyleConstants.setForeground(styleStr, new Color(0, 120, 0));
			StyleConstants.setForeground(styleReserved, Color.BLUE);
			StyleConstants.setForeground(styleImplemented, new Color(153, 0, 153));
		}

		Style styleComment = addStyle("usna_comment", null);
		StyleConstants.setForeground(styleComment, Color.RED);
		addSyntaxRule(new SyntaxEditor.BlockSimpleSyntax("//", "\n", styleComment));
		addSyntaxRule(new SyntaxEditor.BlockSimpleSyntax("/*", "*/", styleComment));

		addSyntaxRule(new SyntaxEditor.BlockSimpleSyntax("\"", "\"", "\\", styleStr));
		addSyntaxRule(new SyntaxEditor.BlockSimpleSyntax("'", "'", "\\", styleStr));
		
		Style styleBrachets = addStyle("usna_brachets", null);
		StyleConstants.setBold(styleBrachets, true);
//		StyleConstants.setForeground(styleBrachets, Color.RED);
		addSyntaxRule(new SyntaxEditor.Keywords(new String[] {"{", "}", "[", "]"}, styleBrachets));
		
		addSyntaxRule(new SyntaxEditor.Keywords(new String[] {"=", "+", "-", "*", "/", "%", "<", ">", "&", "|", "!"}, styleOperators));
		
		StyleConstants.setBold(styleReserved, true);
		addSyntaxRule(new SyntaxEditor.DelimitedKeywords(new String[] {
				"abstract", "arguments", "await*", "boolean", "break", "byte", "case", "catch",
				"char", "class", "const*", "continue", "debugger", "default", "delete", "do",
				"double", "else", "enum", "eval", "export", "extends", "false", "final",
				"finally", "float", "for", "function", "goto", "if", "implements", "import",
				"in", "instanceof", "int", "interface", "let", "long", "native", "new",
				"null", "package", "private", "protected", "public", "return", "short", "static",
				"super", "switch", "synchronized", "this", "throw", "throws", "transient", "true",
				"try", "typeof", "var", "void", "volatile", "while", "with", "yield"}, styleReserved));
		
		StyleConstants.setBold(styleImplemented, true);
		addSyntaxRule(new SyntaxEditor.DelimitedKeywords(new String[] {
				"String", "Number", "Function", "Array", "Math", "Date", "Object", "Exceptions"}, styleImplemented));
		
		Style styleShelly = addStyle("usna_shellyReserved", null);
		StyleConstants.setBold(styleShelly, true);
		StyleConstants.setItalic(styleShelly, true);
		StyleConstants.setForeground(styleShelly, new Color(102, 0, 204));
		addSyntaxRule(new SyntaxEditor.DelimitedKeywords(new String[] {
				"Shelly", "JSON", "Timer", "MQTT", "BLE", "HTTPServer"}, styleShelly));
	}
	
	public void mapAction(KeyStroke k, Action action, String name) {
		getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(k, name);
		getActionMap().put(name, action);
	}
	
	public void gotoLine(int line) {
		Element el = getDocument().getDefaultRootElement().getElement(line - 1);
		if(el != null) {
			setCaretPosition(el.getStartOffset());
		}
	}
	
	public void commentSelected() {
		try {
			final Element root = doc.getDefaultRootElement();
			int startElIndex = root.getElementIndex(getSelectionStart());
			final int start = root.getElement(startElIndex).getStartOffset();
			int endElIndex = root.getElementIndex(getSelectionEnd());
			final int end = root.getElement(endElIndex).getEndOffset() - 0;
			final boolean docEnd = end > doc.getLength(); // javadoc: AbstractDocument models an implied break at the end of the document

			String txt = doc.getText(start, end - start);

			int selectedLines = (int)COMMENTED_LINE.matcher(txt).results().count();
			if(selectedLines == endElIndex - startElIndex + 1) { // all lines commented > remove
				txt = COMMENTED_LINE.matcher(txt).replaceAll("$1$2");
			} else {
				txt = LINE_START.matcher(txt).replaceAll("$1//$2");
			}
			if(docEnd) {
				replace(start, end - start - 1, txt.substring(0, txt.length() - 1));
			} else {
				replace(start, end - start, txt);
			}
			setSelectionStart(start);
			setSelectionEnd(root.getElement(endElIndex).getEndOffset() - 1);
		} catch (BadLocationException e) { LOG.error("commentSelected", e); }
	}
	
	public boolean indentSelected(boolean remove) { // or add (shift-tab / tab)
		final Element root = doc.getDefaultRootElement();
		int startElIndex = root.getElementIndex(getSelectionStart());
		int endElIndex = root.getElementIndex(getSelectionEnd());
		if(startElIndex != endElIndex) {
			final int start = root.getElement(startElIndex).getStartOffset();
			final int end = root.getElement(endElIndex).getEndOffset() - 1; // javadoc: AbstractDocument models an implied break at the end of the document
			try {
				String txt = doc.getText(start, end - start);
				if(remove) {
					txt = LINE_TAB.matcher(txt).replaceAll("$1");
				} else {
					txt = LINE_START.matcher(txt).replaceAll("$1\\\t$2");
				}
				replace(start, end - start, txt);
				setSelectionStart(start);
				setSelectionEnd(root.getElement(endElIndex).getEndOffset() - 1);
				return true; // consume event
			} catch (BadLocationException e) { LOG.error("indentSelected", e); }
		}
		return false;
	}
	
	public void autoIndentSelected() {
		final Element root = doc.getDefaultRootElement();
		final int startElIndex = root.getElementIndex(getSelectionStart());
		final int endElIndex = root.getElementIndex(getSelectionEnd());
		StringBuilder txt = new StringBuilder();
		try {
			String indent;
//			String thisIndent;
			String thisLine = null;
			for(int lineIndex = startElIndex; lineIndex <= endElIndex; lineIndex++) {
				if(lineIndex > 0) {
					Element refEl = root.getElement(lineIndex - 1);
					int refLineStart = refEl.getStartOffset();
					int refLineEnd = refEl.getEndOffset();
					String refLine = doc.getText(refLineStart, refLineEnd - refLineStart);
					indent = indentPrev2(refLine, refLineStart, refLineEnd);
					if(thisLine == null) {
						final Matcher findIndentMatcher = LINE_SPACES.matcher(refLine);
						int end = findIndentMatcher.lookingAt() ? findIndentMatcher.end() : 0;
						indent += refLine.substring(0, end);
					} else {
						final Matcher findIndentMatcher = LINE_SPACES.matcher(thisLine);
						int end = findIndentMatcher.lookingAt() ? findIndentMatcher.end() : 0;
						indent += thisLine.substring(0, end);
					}
				} else {
					indent = "";
				}
				Element thisEl = root.getElement(lineIndex);
				int lineStart = thisEl.getStartOffset();
				int lineEnd = thisEl.getEndOffset();
				int lineLength = (lineEnd > doc.getLength()) ? lineEnd - lineStart - 1 : lineEnd - lineStart;
				thisLine = doc.getText(lineStart, lineLength);
				thisLine = indentThis2(thisLine, indent, lineStart, lineEnd);
				
//				Matcher thisIndentMatcher = LINE_SPACES.matcher(thisLine);
//				int spacesEnd = thisIndentMatcher.lookingAt() ? thisIndentMatcher.end() : 0;
//				thisLine = indent + thisLine.replaceAll("^\\s*", "");
				
				txt.append(thisLine);
			}
			int start = root.getElement(startElIndex).getStartOffset();
			int end = root.getElement(endElIndex).getEndOffset();
			int lineLength = (end > doc.getLength()) ? end - start - 1 : end - start;
			replace(start, lineLength, txt.toString());
			Element thisEl = root.getElement(endElIndex);
			setCaretPosition(thisEl.getStartOffset());
		} catch (BadLocationException e) {
			LOG.error("autoIndentSelected", e);
		}
	}
	
	private String indentPrev2(String line, int lineStart, int lineEnd) {
		int plus = 0;
		int lineLength = line.length();
		for(int i = 0; i < lineLength; i++) {
			if(line.charAt(i) == '{') {
				if(doc.getCharacterElement(lineStart + i).getAttributes().getAttribute(StyleConstants.NameAttribute).toString().equals("usna_brachets")) {
					plus++;
				}
			} else if(line.charAt(i) == '}') {
				if(doc.getCharacterElement(lineStart + i).getAttributes().getAttribute(StyleConstants.NameAttribute).toString().equals("usna_brachets")) {
					if(plus > 0) {
						plus--;
					}
				}
			}
		}
		return (plus > 0) ? "\t".repeat(plus) : "";
	}
	
	private String indentThis2(String line, String prevIndent, int lineStart, int lineEnd) {
		// remove opening spaces
		int plus = 0;
		int minus = 0;
		Matcher thisIndentMatcher = LINE_SPACES.matcher(line);
		int spacesEnd = thisIndentMatcher.lookingAt() ? thisIndentMatcher.end() : 0;
		int lineLength = line.length();
		for(int i = spacesEnd; i < lineLength; i++) {
			if(line.charAt(i) == '{') {
				if(doc.getCharacterElement(lineStart + i).getAttributes().getAttribute(StyleConstants.NameAttribute).toString().equals("usna_brachets")) {
					plus++;
				}
			} else if(line.charAt(i) == '}') {
				if(doc.getCharacterElement(lineStart + i).getAttributes().getAttribute(StyleConstants.NameAttribute).toString().equals("usna_brachets")) {
					if(plus > 0) {
						plus--;
					} else {
						minus++;
					}
				}
			}
		}
		while(minus-- > 0) {
			prevIndent = prevIndent.replaceFirst("\\t", "");
		}
		return prevIndent + line.substring(spacesEnd);
	}
	
	// "Enter" typed
	public void newIndentedLine(boolean autoIndent, boolean autoCloseBlock) {
		try {
			int start = doc.getParagraphElement(getSelectionStart()).getStartOffset();
			String currentLine = doc.getText(start, getSelectionStart() - start);
			
			final Matcher startBlockMatcher = START_BLOCK.matcher(currentLine);
			boolean startBlock = startBlockMatcher.find(); //&& doc.getCharacterElement(start + startBlockMatcher.start()).getAttributes().getAttribute(StyleConstants.NameAttribute).toString().equals("usna_brachets");
			final String newLineStart = (autoIndent && startBlock) ? "\n\t" : "\n";
			
			final Matcher findIndentMatcher = LINE_SPACES.matcher(currentLine);
			final String prevIndent = findIndentMatcher.lookingAt() ? currentLine.substring(findIndentMatcher.start(), findIndentMatcher.end()) : "";
			replaceSelection(newLineStart + prevIndent);

			if(startBlock && autoCloseBlock) {
				int pos = getCaretPosition();
				insert("\n" + prevIndent /*+ "}"*/, pos);
				setCaretPosition(pos);
			}
		} catch (BadLocationException e) {
			LOG.error("newIndentedLine", e);
		}
	}
	
	// "}" typed
	public boolean removeIndentlevel() {
		int pos = getCaretPosition();
		Element line = doc.getParagraphElement(pos);
		int start = line.getStartOffset();
		try {
			String currentLine = doc.getText(start, pos - start);
			Matcher m = END_BLOCK_FIND_TAB.matcher(currentLine);
			if(m.find()) {
				doc.removeDocumentListener(docListener);
				undoManager.startCompound();
				insert("}", pos);
				analizeDocument(0, doc.getLength());
				String styleName = doc.getCharacterElement(pos - 1).getAttributes().getAttribute(StyleConstants.NameAttribute).toString();
				if("usna_string".equals(styleName) == false && "usna_comment".equals(styleName) == false) {
					doc.remove(m.start(1) + start, 1);
				}
				undoManager.endCompound();
				doc.addDocumentListener(docListener);
				return true;
			}
		} catch (BadLocationException e) {
			LOG.error("removeIndentlevel", e);
		}
		return false;
	}
	
	public void blockLimitsInsert(String start, String end) {
		doc.removeDocumentListener(docListener);
		replaceSelection(start);
		analizeDocument(0, doc.getLength());
		final int pos = getCaretPosition();
		
		String styleName = doc.getCharacterElement(pos - 1).getAttributes().getAttribute(StyleConstants.NameAttribute).toString();
		if("usna_string".equals(styleName) == false && "usna_comment".equals(styleName) == false) {
			try {
				insert(end, pos); // separate undo
				analizeDocument(0, doc.getLength());
			} catch (BadLocationException e) { LOG.error("blockLimitsInsert", e); }
			setCaretPosition(pos);
		}
		doc.addDocumentListener(docListener);
	}
	
	public void stringBlockLimitsInsert() {
		doc.removeDocumentListener(docListener);
		replaceSelection("\"");
		analizeDocument(0, doc.getLength());
		final int pos = getCaretPosition();
		if("usna_string".equals(doc.getCharacterElement(pos - 1).getAttributes().getAttribute(StyleConstants.NameAttribute).toString()) &&
				pos > 1 && "usna_string".equals(doc.getCharacterElement(pos - 2).getAttributes().getAttribute(StyleConstants.NameAttribute).toString()) == false) {
			try {
				insert("\"", pos); // separate undo
				analizeDocument(0, doc.getLength());
			} catch (BadLocationException e) { LOG.error("stringBlockLimitsInsert", e); }
			setCaretPosition(pos);
		}
		doc.addDocumentListener(docListener);
	}

	@Override
	// StyleConstants.setBackground(style, Color.BLACK);
	protected void paintComponent(Graphics g) {
		if (darkMode) {
			g.setColor(Color.BLACK);
			g.fillRect(0, 0, getWidth(), getHeight());
		}
		super.paintComponent(g);
	}
	
	private static SimpleAttributeSet baseStyle() {
		SimpleAttributeSet style = new SimpleAttributeSet();
		StyleConstants.setFontFamily(style, Font.MONOSPACED);
		boolean darkMode = ScannerProperties.get().getBoolProperty(ScannerProperties.PROP_IDE_DARK);
		if(darkMode) {
			StyleConstants.setForeground(style, Color.WHITE);
		}
		return style;
	}
}