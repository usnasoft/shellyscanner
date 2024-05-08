package it.usna.shellyscan.view.scripts.ide;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import javax.swing.text.BadLocationException;
import javax.swing.text.DefaultHighlighter;
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
	private final static Pattern LINE_START = Pattern.compile("(^)(.*)", Pattern.MULTILINE);
	private final static Pattern COMMENTED_LINE = Pattern.compile("(^)(\\s*)//", Pattern.MULTILINE);
	private final static Pattern LINE_TAB = Pattern.compile("(^)\\t", Pattern.MULTILINE);
	private final static Pattern SPACES = Pattern.compile("[ \t]*");
	private final static Pattern FUNCTIONS = Pattern.compile("function\\s*(\\S*\\s*\\()");
	private final boolean darkMode = ScannerProperties.get().getBoolProperty(ScannerProperties.PROP_IDE_DARK);
	private final static int MIN_AUTOCOMPLETE = 2;
	private final static Font AUTOCOMPLETE_FONT = new Font(Font.MONOSPACED, Font.PLAIN, 12);
	private final static Logger LOG = LoggerFactory.getLogger(EditorPanel.class);
	
	private final static String RESERVED_ST_NAME = "usna_reserved";
	
	private final String[] reservedWords = new String[] {
			"abstract", "arguments", "await*", "boolean", "break", "byte", "case", "catch",
			"char", "class", "const*", "continue", "debugger", "default", "delete", "do",
			"double", "else", "enum", "eval", "export", "extends", "false", "final",
			"finally", "float", "for", "function", "goto", "if", "implements", "import",
			"in", "instanceof", "int", "interface", "let", "long", "native", "new",
			"null", "package", "private", "protected", "public", "return", "short", "static",
			"super", "switch", "synchronized", "this", "throw", "throws", "transient", "true",
			"try", "typeof", "var", "void", "volatile", "while", "with", "yield"};
	
	private final String[] implementedWords = new String[] {"String", "Number", "Function", "Array", "Math", "Date", "Object", "Exceptions"};
	
	private final String[] shellyWords = new String[] {"Shelly", "JSON", "Timer", "MQTT", "BLE", "HTTPServer"};
	
	private final String[] othersForAutocomplete = new String[] {"print(", "console.log("};
	
	private DefaultHighlighter.DefaultHighlightPainter hilighter;
	private HighlightCouple highlightCouple;
	
	EditorPanel(String initText) {
		super(baseStyle());
		setTabSize(ScannerProperties.get().getIntProperty(ScannerProperties.PROP_IDE_TAB_SIZE, ScannerProperties.IDE_TAB_SIZE_DEFAULT));
		activateUndo();
		setText(initText);
		setCaretPosition(0);
		resetUndo();
		
		Style styleOperators = addStyle("usna_operator", null);
		Style styleStr = addStyle("usna_string", null);
		Style styleReserved = addStyle(RESERVED_ST_NAME, null);
		Style styleImplemented = addStyle("usna_implemented", null);
		Style styleShelly = addStyle("usna_shellyReserved", null);
		if(darkMode) {
			setBackground(new Color(0, 0, 0, 0));
			setCaretColor(Color.WHITE);
			
			StyleConstants.setForeground(styleOperators, new Color(255, 153, 51));
			StyleConstants.setForeground(styleStr, new Color(128, 255, 0));
			StyleConstants.setForeground(styleReserved, Color.CYAN);
			StyleConstants.setForeground(styleImplemented, new Color(255, 153, 255));
			StyleConstants.setForeground(styleShelly, new Color(190, 65, 201));
			
			hilighter = new DefaultHighlighter.DefaultHighlightPainter(Color.GRAY);
		} else {
			StyleConstants.setForeground(styleOperators, new Color(150, 0, 0));
			StyleConstants.setForeground(styleStr, new Color(0, 120, 0));
			StyleConstants.setForeground(styleReserved, Color.BLUE);
			StyleConstants.setForeground(styleImplemented, new Color(153, 0, 153));
			StyleConstants.setForeground(styleShelly, new Color(102, 0, 204));
			
			hilighter = new DefaultHighlighter.DefaultHighlightPainter(Color.ORANGE);
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
		addSyntaxRule(new SyntaxEditor.Keywords(new String[] {"{", "}", "[", "]", "(", ")"}, styleBrachets));
		
		addSyntaxRule(new SyntaxEditor.Keywords(new String[] {"=", "+", "-", "*", "/", "%", "<", ">", "&", "|", "!"}, styleOperators));
		
		StyleConstants.setBold(styleReserved, true);
		addSyntaxRule(new SyntaxEditor.DelimitedKeywords(reservedWords, styleReserved));
		
		StyleConstants.setBold(styleImplemented, true);
		addSyntaxRule(new SyntaxEditor.DelimitedKeywords(implementedWords, styleImplemented));

		StyleConstants.setBold(styleShelly, true);
		StyleConstants.setItalic(styleShelly, true);
		addSyntaxRule(new SyntaxEditor.DelimitedKeywords(shellyWords, styleShelly));
		
		// hilighter
		addCaretListener(e -> {
			getHighlighter().removeAllHighlights(); // todo
			SwingUtilities.invokeLater(() -> {
				try {
					int pos = e.getDot() - 1;
					String c = doc.getText(pos, 1);
					if(c.equals("(") && getCharacterStyleName(pos).equals("usna_brachets")) {
						highlightCorrespondingClose(pos, "(", ")");
					} else if(c.equals(")") && getCharacterStyleName(pos).equals("usna_brachets")) {
						highlightCorrespondingOpen(pos, "(", ")");
					} else if(c.equals("[") && getCharacterStyleName(pos).equals("usna_brachets")) {
						highlightCorrespondingClose(pos, "[", "]");
					} else if(c.equals("]") && getCharacterStyleName(pos).equals("usna_brachets")) {
						highlightCorrespondingOpen(pos, "[", "]");
					} else if(c.equals("{") && getCharacterStyleName(pos).equals("usna_brachets")) {
						highlightCorrespondingClose(pos, "{", "}");
					} else if(c.equals("}") && getCharacterStyleName(pos).equals("usna_brachets")) {
						highlightCorrespondingOpen(pos, "{", "}");
					}
				} catch (BadLocationException e1) {
					LOG.error("CaretListener", e);
				}
			});
		});
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
	
	private void highlightCorrespondingClose(int pos, String start, String end) throws BadLocationException {
		getHighlighter().addHighlight(pos, pos + 1, hilighter);
		int count = 0;
		int docLength = doc.getLength();
		for(int i = pos + 1; i < docLength; i++) {
			String c = doc.getText(i, 1);
			if(c.equals(end) && getCharacterStyleName(i).equals("usna_brachets")) {
				if(count == 0) {
					getHighlighter().addHighlight(i, i + 1, hilighter);
					break;
				} else {
					count--;
				}
			} else if(c.equals(start) && getCharacterStyleName(i).equals("usna_brachets")) {
				count++;
			}
		}
	}
	
	private void highlightCorrespondingOpen(int pos, String start, String end) throws BadLocationException {
		getHighlighter().addHighlight(pos, pos + 1, hilighter);
		int count = 0;
		for(int i = pos - 1; i >= 0; i--) {
			String c = doc.getText(i, 1);
			if(c.equals(start) && getCharacterStyleName(i).equals("usna_brachets")) {
				if(count == 0) {
					getHighlighter().addHighlight(i, i + 1, hilighter);
					break;
				} else {
					count--;
				}
			} else if(c.equals(end) && getCharacterStyleName(i).equals("usna_brachets")) {
				count++;
			}
		}
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
		} catch (BadLocationException e) {
			LOG.error("commentSelected", e);
		}
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
			} catch (BadLocationException e) {
				LOG.error("indentSelected", e);
			}
		}
		return false;
	}
	
	public void autoIndentSelected() {
		try {
			final Element root = doc.getDefaultRootElement();
			final int startElIndex = root.getElementIndex(getSelectionStart());
			final int endElIndex = root.getElementIndex(getSelectionEnd());
			final StringBuilder txt = new StringBuilder();
			String thisIndent = null;
			Element thisEl = null;
			for(int lineIndex = startElIndex; lineIndex <= endElIndex; lineIndex++) {
				final String indent;
				if(lineIndex > 0) {
					if(thisIndent == null) { // first line
						String refLine;
						int refIndex = lineIndex;
						Element refEl;
						int refLineStart;
						do { // if previous line is empty look back
							refEl = root.getElement(--refIndex);
							refLineStart = refEl.getStartOffset();
							refLine = doc.getText(refLineStart, refEl.getEndOffset() - refLineStart);
						} while(refLine.trim().isEmpty() && refIndex > 0);
						
						final Matcher findIndentMatcher = SPACES.matcher(refLine);
						int end = findIndentMatcher.lookingAt() ? findIndentMatcher.end() : 0;
						indent = indentPrev(refLine, refLineStart) + refLine.substring(0, end);
					} else {
						Element refEl = root.getElement(lineIndex - 1);
						int refLineStart = refEl.getStartOffset();
						String refLine = doc.getText(refLineStart, refEl.getEndOffset() - refLineStart);
						indent = indentPrev(refLine, refLineStart) + thisIndent;
					}
				} else {
					indent = "";
				}
				thisEl = root.getElement(lineIndex);
				int lineStart = thisEl.getStartOffset();
				int lineEnd = thisEl.getEndOffset();
				int lineLength = (lineEnd > doc.getLength()) ? lineEnd - lineStart - 1 : lineEnd - lineStart;
				String thisLine = doc.getText(lineStart, lineLength);
				thisIndent = indentThis(thisLine, indent, lineStart);
				
				Matcher currentIndentMatcher = SPACES.matcher(thisLine);
				int spacesEnd = currentIndentMatcher.lookingAt() ? currentIndentMatcher.end() : 0;
				
				txt.append(thisIndent).append(thisLine.substring(spacesEnd));
			}
			int start = root.getElement(startElIndex).getStartOffset();
			int end = thisEl.getEndOffset();
			int length = (end > doc.getLength()) ? end - start - 1 : end - start;
			String txtStr = txt.toString();
			if(doc.getText(start, length).equals(txtStr) == false) { // avoid false undo
				replace(start, length, txtStr);
			}

			setCaretPosition(Math.min(end - 1, doc.getLength()));
		} catch (BadLocationException e) {
			LOG.error("autoIndentSelected", e);
		}
	}
	
	// add one '\t' for every '{'
	private String indentPrev(String line, int lineStart/*, int lineEnd*/) {
		int plus = 0;
		int lineLength = line.length();
		for(int i = 0; i < lineLength; i++) {
			if(line.charAt(i) == '{') {
				if(getCharacterStyleName(lineStart + i).equals("usna_brachets")) {
					plus++;
				}
			} else if(line.charAt(i) == '}') {
				if(plus > 0 && getCharacterStyleName(lineStart + i).equals("usna_brachets")) {
					plus--;
				}
			}
		}
		return (plus > 0) ? "\t".repeat(plus) : "";
	}
	
	// remove one '\t' for every '}'
	private String indentThis(String line, String indent, int lineStart/*, int lineEnd*/) {
		int plus = 0;
		int minus = 0;
		int lineLength = line.length();
		for(int i = 0; i < lineLength; i++) {
			if(line.charAt(i) == '{') {
				if(getCharacterStyleName(lineStart + i).equals("usna_brachets")) {
					plus++;
				}
			} else if(line.charAt(i) == '}') {
				if(getCharacterStyleName(lineStart + i).equals("usna_brachets")) {
					if(plus > 0) {
						plus--;
					} else {
						minus++;
					}
				}
			}
		}
		while(minus-- > 0) {
			indent = indent.replaceFirst("\\t", "");
		}
		return indent;
	}
	
	// "Enter" typed
	public void newIndentedLine(boolean smartAutoIndent, boolean autoCloseBlock) {
		// called -> Indent enabled
		try {
			int selectionStart = getSelectionStart();
			int start = doc.getParagraphElement(selectionStart).getStartOffset();
			String currentLine = doc.getText(start, getSelectionStart() - start);
			final Matcher findIndentMatcher = SPACES.matcher(currentLine);
			final String prevIndent = findIndentMatcher.lookingAt() ? currentLine.substring(0, findIndentMatcher.end()) : "";
			
			boolean closeBlock = autoCloseBlock && selectionStart > 0 &&
					getSelectionEnd() == selectionStart &&
					doc.getText(getSelectionStart() - 1, 2).equals("{}") &&
					getCharacterStyleName(selectionStart - 1).equals("usna_brachets");
			
			replaceSelection("\n" + prevIndent);
			if(smartAutoIndent) {
				final Element root = doc.getDefaultRootElement();
				int pos = getCaretPosition();
				final int startElIndex = root.getElementIndex(pos);
				int endPar = root.getElement(startElIndex).getEndOffset();
				analizeDocument(0, doc.getLength());
				autoIndentSelected();
				int newEndPar = root.getElement(startElIndex ).getEndOffset();
				setCaretPosition(pos + newEndPar - endPar);
			}
			if(closeBlock) {
				int pos = getCaretPosition();
				final String newLineStart = (smartAutoIndent) ? "\t\n" : "\n";
				insert(newLineStart + prevIndent, pos);
				setCaretPosition(pos + 1);
			}
		} catch (BadLocationException e) {
			LOG.error("newIndentedLine", e);
		}
	}
	
	// "}" typed
	public void removeIndentlevel() {
		final Element root = doc.getDefaultRootElement();
		int pos = getCaretPosition();
		final int startElIndex = root.getElementIndex(pos);
		int endPar = root.getElement(startElIndex).getEndOffset();
		replaceSelection("}");
		analizeDocument(0, doc.getLength());
		autoIndentSelected();
		int newEndPar = root.getElement(startElIndex).getEndOffset();
		setCaretPosition(pos + newEndPar - endPar);
	}
	
	public void blockLimitsInsert(String start, String end) {
		doc.removeDocumentListener(docListener);
		replaceSelection(start);
		analizeDocument(0, doc.getLength());
		final int pos = getCaretPosition();
		
		String styleName = getCharacterStyleName(pos - 1);
		if("usna_string".equals(styleName) == false && "usna_comment".equals(styleName) == false) {
			try {
				insert(end, pos); // separate undo
				analizeDocument(0, doc.getLength());
			} catch (BadLocationException e) {
				LOG.error("blockLimitsInsert", e);
			}
			setCaretPosition(pos);
		}
		doc.addDocumentListener(docListener);
	}
	
	public void stringBlockLimitsInsert() {
		doc.removeDocumentListener(docListener);
		replaceSelection("\"");
		analizeDocument(0, doc.getLength());
		final int pos = getCaretPosition();
		if("usna_string".equals(getCharacterStyleName(pos - 1)) && (pos < 2 || "usna_string".equals(getCharacterStyleName(pos - 2)) == false)) {
			try {
				insert("\"", pos); // separate undo
				analizeDocument(0, doc.getLength());
			} catch (BadLocationException e) {
				LOG.error("stringBlockLimitsInsert", e);
			}
			setCaretPosition(pos);
		}
		doc.addDocumentListener(docListener);
	}
	
	public void autocomplete() {
		final int pos = getCaretPosition();
		if(pos >= MIN_AUTOCOMPLETE) {
			try {
				StringBuilder token = new StringBuilder();
				int i = pos - 1;
				String c;
				for(; i >= 0 && (c = doc.getText(i, 1)).matches("[\\s.,;()\\[\\]{}]") == false && "default".equals(getCharacterStyleName(i)); i--) {
					token.insert(0, c);
				}

				if(token.length() >= MIN_AUTOCOMPLETE) {
					String tokenStr = token.toString();
					ArrayList<String> found = new ArrayList<>();
					addAutocompleteCandidate(reservedWords, tokenStr, found);
					addAutocompleteCandidate(shellyWords, tokenStr, found);
					addAutocompleteCandidate(implementedWords, tokenStr, found);
					addAutocompleteCandidate(othersForAutocomplete, tokenStr, found);
					findFunction(tokenStr, found);
					findVariables(tokenStr, found);
//					System.out.println(found);
					if(found.size() == 1) {
						replace(i + 1, pos - i - 1, found.get(0));
					} else {
						found.sort(String::compareToIgnoreCase);
						final int start = i;
						JPopupMenu popup = new JPopupMenu();
						found.stream().map(t -> new JMenuItem(t)).forEach(m -> {
							if(darkMode) {
								m.setOpaque(true);
								m.setBackground(new Color(2, 50, 100));
								m.setForeground(Color.WHITE);
							}
							m.setBorder(BorderFactory.createEmptyBorder(0, 2, 0, 2));
							m.setFont(AUTOCOMPLETE_FONT);
							m.addActionListener(e -> {
								try {
									replace(start + 1, pos - start - 1, m.getText());
								} catch (BadLocationException e1) {
									LOG.error("autocomplete", e);
								}
							});
							popup.add(m);
						});
						popup.setBorder(BorderFactory.createEmptyBorder(2, 2, 2, 2));
						Rectangle2D r = modelToView2D(pos);
						popup.show(this, (int)r.getX() + 2, (int)r.getMaxY() - 4);
					}
				}
			} catch (BadLocationException e) {
				LOG.error("autocomplete", e);
			}
		}
	}
	
	private static void addAutocompleteCandidate(String[] list, String token, ArrayList<String> found) {
		for(String w: list) {
			if(w.toLowerCase().startsWith(token.toString().toLowerCase())) {
				found.add(w);
			}
		}
	}

	private void findFunction(String token, ArrayList<String> found) {
		try {
			String txt = doc.getText(0, doc.getLength());
			Matcher functionMatcher = FUNCTIONS.matcher(txt);
			while(functionMatcher.find()) {
				String fName = functionMatcher.group(1);
				if(fName.toLowerCase().startsWith(token.toLowerCase()) && getCharacterStyleName(functionMatcher.start(1)).equals("default")) {
					found.add(fName);
				}
			}
		} catch (BadLocationException e) {
			LOG.error("findFunction", e);
		}
	}
	
	private void findVariables(String token, ArrayList<String> found) {
		try {
			boolean function = false;
			int bracketCount = 0;
			int length = doc.getLength();
			String txt = doc.getText(0, length);
			Matcher functionMatcher = FUNCTIONS.matcher(txt);
			for(int pos = 0; pos < length; pos++) {
				if(function) {
					if(txt.charAt(pos) == '{' && getCharacterStyleName(pos).equals("usna_brachets")) {
						bracketCount++;
					} else if(txt.charAt(pos) == '}' && getCharacterStyleName(pos).equals("usna_brachets")) {
						bracketCount--;
						if(bracketCount == 0) {
							function = false;
						}
					}
				} else if(functionMatcher.region(pos, length).lookingAt() && getCharacterStyleName(pos).equals(RESERVED_ST_NAME)) {
					function = true;
				} else if(txt.charAt(pos) == '=' && getCharacterStyleName(pos).equals("usna_operator")) {
					String w = findPreviousWord(txt, pos);
					System.out.println(w);
				}
			}
		} catch (BadLocationException e) {
			LOG.error("findFunction", e);
		}
	}
	
	private String findPreviousWord(String txt, int pos) {
		String w = "";
		while(--pos >= 0 && getCharacterStyleName(pos).equals("default")) {
			if(Character.isWhitespace(txt.charAt(pos))) {
				if(w.length() > 0) {
					break;
				}
			} else {
				w = txt.charAt(pos) + w;
			}
		}
		return w;
	}
	
	// scapes: global, function, let

	@Override
	// StyleConstants.setBackground(style, Color.BLACK);
	protected void paintComponent(Graphics g) {
		if (darkMode) {
			g.setColor(Color.BLACK);
			g.fillRect(0, 0, getWidth(), getHeight());
		}
		super.paintComponent(g);
	}
	
	record HighlightCouple(Object a1, Object a2) {}
}