package it.usna.shellyscan.view.scripts.ide;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JComponent;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import javax.swing.event.MenuKeyEvent;
import javax.swing.event.MenuKeyListener;
import javax.swing.text.BadLocationException;
import javax.swing.text.DefaultHighlighter;
import javax.swing.text.Element;
import javax.swing.text.Highlighter;
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
	private final static Pattern FUNCTION = Pattern.compile("function\\s+(\\S*\\s*\\()");
	private final static Pattern FUNCTION_ARGS = Pattern.compile("\\s*[\\S&&[^\\(]]+\\((.*)\\)");
	private final boolean darkMode = ScannerProperties.get().getBoolProperty(ScannerProperties.PROP_IDE_DARK);
	private final static int MIN_AUTOCOMPLETE = 2;
	private final static Font AUTOCOMPLETE_FONT = new Font(Font.MONOSPACED, Font.PLAIN, 12);
	private final static Logger LOG = LoggerFactory.getLogger(EditorPanel.class);
	
	private final static ImageIcon FUNCTON_ICON = new ImageIcon(EditorPanel.class.getResource("/images/function_12.png"));
	private final static ImageIcon VAR_ICON = new ImageIcon(EditorPanel.class.getResource("/images/var_12.png"));
	
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
	
	private DefaultHighlighter.DefaultHighlightPainter highlighterPainter;
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
		Style styleReserved = addStyle("usna_reserved", null);
		Style styleImplemented = addStyle("usna_implemented", null);
		Style styleShelly = addStyle("usna_shellyReserved", null);
		if(darkMode) {
			setBackground(new Color(ScriptFrame.DARK_BACKGOUND_COLOR.getRed(), ScriptFrame.DARK_BACKGOUND_COLOR.getGreen(), ScriptFrame.DARK_BACKGOUND_COLOR.getBlue(), 0));
			setCaretColor(Color.WHITE);
			
			StyleConstants.setForeground(styleOperators, new Color(255, 153, 51));
			StyleConstants.setForeground(styleStr, new Color(128, 255, 0));
			StyleConstants.setForeground(styleReserved, new Color(77, 166, 255));
			StyleConstants.setForeground(styleImplemented, new Color(255, 153, 255));
			StyleConstants.setForeground(styleShelly, new Color(190, 65, 201));
			
			highlighterPainter = new DefaultHighlighter.DefaultHighlightPainter(Color.GRAY);
		} else {
			StyleConstants.setForeground(styleOperators, new Color(150, 0, 0));
			StyleConstants.setForeground(styleStr, new Color(0, 120, 0));
			StyleConstants.setForeground(styleReserved, Color.BLUE);
			StyleConstants.setForeground(styleImplemented, new Color(153, 0, 153));
			StyleConstants.setForeground(styleShelly, new Color(102, 0, 204));
			
			highlighterPainter = new DefaultHighlighter.DefaultHighlightPainter(Color.ORANGE);
		}

		Style styleComment = addStyle("usna_comment", null);
		StyleConstants.setForeground(styleComment, /*Color.RED*/new Color(255, 100, 36));
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
		
		// Highlighter {}[]()
		addCaretListener(event -> {
			SwingUtilities.invokeLater(() -> {
				if(highlightCouple != null) {
					highlightCouple.remove(getHighlighter());
					highlightCouple = null;
				}
				int pos = event.getDot();
				try {
					char c = doc.getText(pos, 1).charAt(0);
					if(c == '(' && getCharacterStyleName(pos).equals("usna_brachets")) {
						highlightCorrespondingClose(pos, "(", ")");
					} else if(c == ')' && getCharacterStyleName(pos).equals("usna_brachets")) {
						highlightCorrespondingOpen(pos, "(", ")");
					} else if(c == '[' && getCharacterStyleName(pos).equals("usna_brachets")) {
						highlightCorrespondingClose(pos, "[", "]");
					} else if(c == ']' && getCharacterStyleName(pos).equals("usna_brachets")) {
						highlightCorrespondingOpen(pos, "[", "]");
					} else if(c == '{' && getCharacterStyleName(pos).equals("usna_brachets")) {
						highlightCorrespondingClose(pos, "{", "}");
					} else if(c == '}' && getCharacterStyleName(pos).equals("usna_brachets")) {
						highlightCorrespondingOpen(pos, "{", "}");
					} else if(pos > 0) {
						c = doc.getText(pos - 1, 1).charAt(0);
						if(c == '(' && getCharacterStyleName(pos - 1).equals("usna_brachets")) {
							highlightCorrespondingClose(pos - 1, "(", ")");
						} else if(c == ')' && getCharacterStyleName(pos - 1).equals("usna_brachets")) {
							highlightCorrespondingOpen(pos - 1, "(", ")");
						} else if(c == '[' && getCharacterStyleName(pos - 1).equals("usna_brachets")) {
							highlightCorrespondingClose(pos - 1, "[", "]");
						} else if(c == ']' && getCharacterStyleName(pos - 1).equals("usna_brachets")) {
							highlightCorrespondingOpen(pos - 1, "[", "]");
						} else if(c == '{' && getCharacterStyleName(pos - 1).equals("usna_brachets")) {
							highlightCorrespondingClose(pos - 1, "{", "}");
						} else if(c == '}' && getCharacterStyleName(pos - 1).equals("usna_brachets")) {
							highlightCorrespondingOpen(pos - 1, "{", "}");
						}
					}
				} catch (BadLocationException e) {
					LOG.error("CaretListener", e);
				}
			});
		});
	}

	private static SimpleAttributeSet baseStyle() {
		SimpleAttributeSet style = new SimpleAttributeSet();
		StyleConstants.setFontFamily(style, Font.MONOSPACED);
		StyleConstants.setFontSize(style, ScannerProperties.get().getIntProperty(ScannerProperties.PROP_IDE_FONT_SIZE, ScannerProperties.IDE_FONT_SIZE_DEFAULT));
		if(ScannerProperties.get().getBoolProperty(ScannerProperties.PROP_IDE_DARK)) {
			StyleConstants.setForeground(style, Color.WHITE);
		}
		return style;
	}
	
	private void highlightCorrespondingClose(int pos, String start, String end) throws BadLocationException {
		int count = 0;
		int docLength = doc.getLength();
		for(int i = pos + 1; i < docLength; i++) {
			String c = doc.getText(i, 1);
			if(c.equals(end) && getCharacterStyleName(i).equals("usna_brachets")) {
				if(count == 0) {
					highlightCouple = new HighlightCouple(getHighlighter().addHighlight(pos, pos + 1, highlighterPainter), getHighlighter().addHighlight(i, i + 1, highlighterPainter));
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
		int count = 0;
		for(int i = pos - 1; i >= 0; i--) {
			String c = doc.getText(i, 1);
			if(c.equals(start) && getCharacterStyleName(i).equals("usna_brachets")) {
				if(count == 0) {
					highlightCouple = new HighlightCouple(getHighlighter().addHighlight(pos, pos + 1, highlighterPainter), getHighlighter().addHighlight(i, i + 1, highlighterPainter));
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
	
	public void autoIndentSelected() { // ctrl-I
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
		doc.removeDocumentListener(docListener); // syntax analyzer auto call removed
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
		doc.removeDocumentListener(docListener); // syntax analyzer auto call removed
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
	
	public void autocomplete() { // ctrl - SPACE
		final int pos = getCaretPosition();
		if(pos >= MIN_AUTOCOMPLETE) {
			try {
				StringBuilder token = new StringBuilder();
				int i = pos - 1;
				String c;
				String style;
				for(; i >= 0 && (c = doc.getText(i, 1)).matches("[\\s.,;()\\[\\]{}]") == false && ("default".equals(style = getCharacterStyleName(i)) || "usna_reserved".equals(style)); i--) {
					token.append(c);
				}

				if(token.length() >= MIN_AUTOCOMPLETE) {
					String txt = doc.getText(0, doc.getLength());
					String lowercaseToken = token.reverse().toString().toLowerCase();
					ArrayList<String> found = new ArrayList<>();
					addAutocompleteCandidate(reservedWords, lowercaseToken, found);
					addAutocompleteCandidate(shellyWords, lowercaseToken, found);
					addAutocompleteCandidate(implementedWords, lowercaseToken, found);
					addAutocompleteCandidate(othersForAutocomplete, lowercaseToken, found);
					HashSet<String> functions = findFunctions(txt, lowercaseToken);
					found.addAll(functions);
					HashSet<String> variables = findVariables(txt, lowercaseToken, pos, 0, doc.getLength());
					found.addAll(variables);
					if(found.size() == 1) {
						replace(i + 1, pos - i - 1, found.get(0));
					} else if(found.size() > 1) {
						found.sort(String::compareToIgnoreCase);
						final int start = i;
						JPopupMenu popup = new JPopupMenu();
						found.forEach(t -> {
							JMenuItem m = new JMenuItem(t);
							if(darkMode) {
								m.setOpaque(true);
								m.setBackground(new Color(2, 50, 100));
								m.setForeground(Color.WHITE);
							}
							m.setBorder(BorderFactory.createEmptyBorder(0, 2, 0, 2));
							m.setFont(AUTOCOMPLETE_FONT);
							m.addActionListener(e -> {
								try {
									replace(start + 1, pos - start - 1, t);
								} catch (BadLocationException e1) {
									LOG.error("autocomplete", e);
								}
							});
							if(functions.contains(t)) {
								m.setIcon(FUNCTON_ICON);
							} else if(variables.contains(t)) {
								m.setIcon(VAR_ICON);
							}
							popup.add(m);
						});
						popup.setBorder(BorderFactory.createEmptyBorder(2, 2, 2, 2));
						Rectangle2D r = modelToView2D(pos);
						popup.show(this, (int)r.getX() + 2, (int)r.getMaxY() - 4);
						popup.addMenuKeyListener(new MenuKeyListener() {
							@Override
							public void menuKeyTyped(MenuKeyEvent e) {
								popup.setVisible(false);
								char c;
								if(Character.isISOControl(c = e.getKeyChar()) == false) {
									replaceSelection(String.valueOf(c));
								}
							}
							
							@Override
							public void menuKeyReleased(MenuKeyEvent e) {}
							
							@Override
							public void menuKeyPressed(MenuKeyEvent e) {}
						});
					}
				}
			} catch (BadLocationException e) {
				LOG.error("autocomplete", e);
			}
		}
	}
	
	private static void addAutocompleteCandidate(String[] list, String token, ArrayList<String> found) {
		for(String w: list) {
			if(w.toLowerCase().startsWith(token)) {
				found.add(w);
			}
		}
	}

	private HashSet<String> findFunctions(String txt, String token) {
		final HashSet<String> found = new HashSet<>();
		Matcher functionMatcher = FUNCTION.matcher(txt);
		while(functionMatcher.find()) {
			String fName = functionMatcher.group(1);
			if(fName.toLowerCase().startsWith(token) && getCharacterStyleName(functionMatcher.start(1)).equals("default")) {
				found.add(fName);
			}
		}
		return found;
	}
	
	private HashSet<String> findVariables(String txt, String token, int caretPos, int start, int end) {
		final HashSet<String> found = new HashSet<>();
		boolean function = false;
		int bracketCount = 0;
		int length = doc.getLength();
		Matcher functionMatcher = FUNCTION.matcher(txt);
		Matcher functionArgsMatcher = FUNCTION_ARGS.matcher(txt);

		int lastFunctionPos = Integer.MAX_VALUE;
		for(int pos = start; pos < end; pos++) {
			if(function) {
				if(txt.charAt(pos) == '{' && getCharacterStyleName(pos).equals("usna_brachets")) {
					bracketCount++;
				} else if((txt.charAt(pos) == '}' && getCharacterStyleName(pos).equals("usna_brachets") && --bracketCount == 0) || pos == end - 1) { // pos == end - 1 -> last, not yet closed
					function = false;
					if(pos + 1 >= caretPos && lastFunctionPos <= caretPos) {
						found.addAll(findVariables(txt, token, caretPos, lastFunctionPos, pos));

						if(functionArgsMatcher.find(lastFunctionPos)) { // parameters
							int defPos;
							for(String par: functionArgsMatcher.group(1).split(",")) {
								if((defPos = par.indexOf('=')) > 0) {
									par = par.substring(0, defPos).trim(); // function myFunction(x, y = 10)
								} else {
									par = par.replace("...", "").trim(); // function myFunction(x, y) - function sum(...args) {
								}
								if(par.toLowerCase().startsWith(token)) {
									found.add(par);
								}
							}
						}
					}
				}
			} else if(functionMatcher.region(pos, length).lookingAt() && getCharacterStyleName(pos).equals("usna_reserved")) {
				function = true;
				lastFunctionPos = pos + 8; //" function" -> 8
			} else if(txt.charAt(pos) == '=' && getCharacterStyleName(pos).equals("usna_operator")) {
				String w = findPreviousPlainWord(txt, pos);
				//					System.out.println(w);
				if(w.toLowerCase().startsWith(token)) {
					found.add(w); // todo: ignoring "let"
				}
			}
		}
		return found;
	}
	
	private String findPreviousPlainWord(String txt, int pos) {
		StringBuilder w = new StringBuilder();
		while(--pos >= 0 && getCharacterStyleName(pos).equals("default")) {
			char c;
			if(Character.isWhitespace(c = txt.charAt(pos))) {
				if(w.length() > 0) {
					break;
				}
			} else {
				w.append(c);
			}
		}
		return w.reverse().toString();
	}
	
	//// scopes: global, function, block (let)

	@Override
	protected void paintComponent(Graphics g) {
		if (darkMode) {
			g.setColor(/*Color.BLACK*/ScriptFrame.DARK_BACKGOUND_COLOR);
			g.fillRect(0, 0, getWidth(), getHeight());
		}
		super.paintComponent(g);
	}
	
	private record HighlightCouple(Object tag1, Object tag2) {
		public void remove(Highlighter h) {
			h.removeHighlight(tag1);
			h.removeHighlight(tag2);
		}
	}
}