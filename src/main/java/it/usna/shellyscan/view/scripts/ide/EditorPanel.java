package it.usna.shellyscan.view.scripts.ide;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.Action;
import javax.swing.JComponent;
import javax.swing.KeyStroke;
import javax.swing.text.BadLocationException;
import javax.swing.text.Element;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.Style;
import javax.swing.text.StyleConstants;

import it.usna.shellyscan.view.util.ScannerProperties;
import it.usna.swing.texteditor.SyntaxEditor;

public class EditorPanel extends SyntaxEditor {
	private static final long serialVersionUID = 1L;
	private final static Pattern LINE_START = Pattern.compile("(^)(.*)", Pattern.MULTILINE);
	private final static Pattern COMMENTED_LINE = Pattern.compile("(^)(\\s*)//", Pattern.MULTILINE);
	private final static Pattern LINE_TAB = Pattern.compile("(^)\\t", Pattern.MULTILINE);
	private final static Pattern LINE_SPACES = Pattern.compile("[ \t]*");
	private final static Pattern START_BLOCK = Pattern.compile("\\{[ \t]*$");
	private final static Pattern END_BLOCK_FIND_TAB = Pattern.compile("\\s*(\t)+\\s*$");
	private final boolean darkMode = ScannerProperties.get().getBoolProperty(ScannerProperties.PROP_IDE_DARK);
	
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
	
	public void commentSelection() {
		try {
			final Element root = doc.getDefaultRootElement();
			int startElIndex = root.getElementIndex(getSelectionStart());
			final int start = root.getElement(startElIndex).getStartOffset();
			int endElIndex = root.getElementIndex(getSelectionEnd());
			final int end = root.getElement(endElIndex).getEndOffset() - 1; // javadoc: AbstractDocument models an implied break at the end of the document

			String txt = doc.getText(start, end - start);

			int selectedLines = (int)COMMENTED_LINE.matcher(txt).results().count();
			if(selectedLines == endElIndex - startElIndex + 1) { // all lines commented > remove
				txt = COMMENTED_LINE.matcher(txt).replaceAll("$1$2");
			} else {
				txt = LINE_START.matcher(txt).replaceAll("$1//$2");
			}
			replace(start, end - start, txt);
			setSelectionStart(start);
			setSelectionEnd(root.getElement(endElIndex).getEndOffset() - 1);
//			setCaretPosition(root.getElement(endElIndex).getEndOffset() - 1);
		} catch (BadLocationException e) { /*e.printStackTrace();*/ }
	}
	
	public boolean indentSelection(boolean remove) { // or add
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
			} catch (BadLocationException e1) { }
		}
		return false;
	}
	
	public void newIndentedLine(boolean autoIndent, boolean autoCloseBlock) {
		try {
			final Element root = doc.getDefaultRootElement();
			int startElIndex = root.getElementIndex(getSelectionStart());
			int start = root.getElement(startElIndex).getStartOffset();
			String currentLine = doc.getText(start, getSelectionStart() - start);
			final Matcher findIndent = LINE_SPACES.matcher(currentLine);
			boolean startBlock = START_BLOCK.matcher(currentLine).find();
			final String newLineStart = (autoIndent && startBlock) ? "\n\t" : "\n";
			final String prevIndent = findIndent.lookingAt() ? currentLine.substring(findIndent.start(), findIndent.end()) : "";
			replaceSelection(newLineStart + prevIndent);

			if(startBlock && autoCloseBlock) {
				int pos = getCaretPosition();
				insert("\n" + prevIndent + "}", pos);
				setCaretPosition(pos);
			}
		} catch (BadLocationException e) { /*e.printStackTrace();*/ }
	}
	
	public boolean removeIndentlevel() {
		int pos = getCaretPosition();
		Element line = doc.getParagraphElement(pos);
		int start = line.getStartOffset();
		try {
			String currentLine = doc.getText(start, pos - start);
			Matcher m = END_BLOCK_FIND_TAB.matcher(currentLine);
			if(m.find()) {
				undoManager.startCompound();
				doc.remove(m.start(1) + line.getStartOffset(), 1);
				insert("}", /*getCaretPosition()*/pos - 1);
				undoManager.endCompound();
				return true;
			}
		} catch (BadLocationException e) { /*e.printStackTrace();*/ }
		return false;
	}

	@Override
	protected void paintComponent(Graphics g) {
		if (darkMode) {
			g.setColor(Color.BLACK);
			g.fillRect(0, 0, getWidth(), getHeight());
		}
		super.paintComponent(g);
	}
	
	public boolean insideInnerBlock() {
		String name = getCharacterAttributes().getAttribute(StyleConstants.NameAttribute).toString();
		
		int pos = getCaretPosition();
//		doc.getCharacterElement(pos+1).getAttributes().getAttribute(StyleConstants.NameAttribute).toString();
//		if(/*doc.getParagraphElement(pos).getEndOffset()*/ doc.getLength() == pos) {
////			name = doc.getParagraphElement(getSelectionStart() - 1).getAttributes().getAttribute(StyleConstants.NameAttribute).toString();
////			doc.getParagraphElement(pos).getStartOffset();;
////			doc.getParagraphElement(pos).getEndOffset();
////			
////			final Element root = doc.getDefaultRootElement();
////			int startElIndex = root.getElementIndex(getSelectionStart());
////			root.getElement(1).getAttributes().getAttribute(StyleConstants.NameAttribute).toString();
////			doc.getParagraphElement(pos).getAttributes().getAttribute(StyleConstants.NameAttribute).toString();
//			name = doc.getCharacterElement(pos-1).getAttributes().getAttribute(StyleConstants.NameAttribute).toString();
//		}
		
//		final Element root = doc.getDefaultRootElement();
//		int startElIndex = root.getElementIndex(getSelectionStart());
//		root.getElement(1);
		//todo doc.getParagraphElement(pos).getStartOffset() != pos NO - SI RIFERISCE ALLA RIGA!!!
		return ("usna_string".equals(name) || "usna_comment".equals(name)) &&
				(doc.getParagraphElement(pos).getStartOffset() != pos /*|| pos == doc.getLength()*//*|| doc.getParagraphElement(pos).getStartOffset() == doc.getParagraphElement(pos).getEndOffset()*/);
	}
	
	private static SimpleAttributeSet baseStyle() {
		SimpleAttributeSet style = new SimpleAttributeSet();
		StyleConstants.setFontFamily(style, Font.MONOSPACED);
		boolean darkMode = ScannerProperties.get().getBoolProperty(ScannerProperties.PROP_IDE_DARK);
		if(darkMode) {
			StyleConstants.setForeground(style, Color.WHITE);
//			StyleConstants.setBackground(style, Color.BLACK);
		}
		return style;
	}
}