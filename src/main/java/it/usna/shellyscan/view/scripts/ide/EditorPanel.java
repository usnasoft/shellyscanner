package it.usna.shellyscan.view.scripts.ide;

import java.awt.Color;
import java.awt.Font;
import java.util.regex.Pattern;

import javax.swing.Action;
import javax.swing.JComponent;
import javax.swing.KeyStroke;
import javax.swing.text.BadLocationException;
import javax.swing.text.Element;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.Style;
import javax.swing.text.StyleConstants;

import it.usna.swing.texteditor.SyntaxEditor;

public class EditorPanel extends SyntaxEditor {
	private static final long serialVersionUID = 1L;
	private final static Pattern LINE_START = Pattern.compile("(^)(.*)", Pattern.MULTILINE);
	private final static Pattern COMMENTED_LINE = Pattern.compile("(^)(\\s*)//", Pattern.MULTILINE);
	private final static Pattern LINE_INIT = Pattern.compile("^", Pattern.MULTILINE);
	private final static Pattern LINE_TAB = Pattern.compile("^\\t", Pattern.MULTILINE);

	EditorPanel(String initText) {
		super(baseStyle());
		activateUndo();
		setText(initText);
		resetUndo();
		
		Style styleComment = addStyle("usna_red", null);
		StyleConstants.setForeground(styleComment, Color.RED);
		addSyntaxRule(new SyntaxEditor.BlockSimpleSyntax("//", "\n", styleComment));
		addSyntaxRule(new SyntaxEditor.BlockSimpleSyntax("/*", "*/", styleComment));
		
		Style styleStr = addStyle("usna_green", null);
		StyleConstants.setForeground(styleStr, new Color(0, 120, 0));
		addSyntaxRule(new SyntaxEditor.BlockSimpleSyntax("\"", "\"", "\\", styleStr));
		addSyntaxRule(new SyntaxEditor.BlockSimpleSyntax("'", "'", "\\", styleStr));
		
		Style styleBrachets = addStyle("usna_brachets", null);
		StyleConstants.setBold(styleBrachets, true);
		addSyntaxRule(new SyntaxEditor.Keywords(new String[] {"{", "}", "[", "]"}, styleBrachets));
		
		Style styleOperators = addStyle("usna_brachets", null);
		StyleConstants.setForeground(styleOperators, new Color(150, 0, 0));
		addSyntaxRule(new SyntaxEditor.Keywords(new String[] {"=", "+", "-", "*", "/", "%", "<", ">", "&", "|", "!"}, styleOperators));
		
		Style styleReserved = addStyle("usna_styleReserved", null);
		StyleConstants.setBold(styleReserved, true);
		StyleConstants.setForeground(styleReserved, Color.blue);
		addSyntaxRule(new SyntaxEditor.DelimitedKeywords(new String[] {
				"abstract", "arguments", "await*", "boolean", "break", "byte", "case", "catch",
				"char", "class", "const*", "continue", "debugger", "default", "delete", "do",
				"double", "else", "enum", "eval", "export", "extends", "false", "final",
				"finally", "float", "for", "function", "goto", "if", "implements", "import",
				"in", "instanceof", "int", "interface", "let", "long", "native", "new",
				"null", "package", "private", "protected", "public", "return", "short", "static",
				"super", "switch", "synchronized", "this", "throw", "throws", "transient", "true",
				"try", "typeof", "var", "void", "volatile", "while", "with", "yield"}, styleReserved));
		
		Style styleImplemented = addStyle("usna_styleReserved", null);
		StyleConstants.setBold(styleImplemented, true);
		StyleConstants.setForeground(styleImplemented, new Color(153, 0, 153));
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
			final int end = Math.min(root.getElement(endElIndex).getEndOffset(), doc.getLength()); // javadoc: AbstractDocument models an implied break at the end of the document

			String txt = doc.getText(start, end - start);

			int selectedLines = (int)COMMENTED_LINE.matcher(txt).results().count();
			if(selectedLines == endElIndex - startElIndex + 1) { // all lines commented > remove
				txt = COMMENTED_LINE.matcher(txt).replaceAll("$1$2");
			} else {
				txt = LINE_START.matcher(txt).replaceAll("$1//$2"); // LINE_INIT.matcher(txt).replaceAll("//") ???
			}
			replace(start, end - start, txt);
			setSelectionStart(start);
			setSelectionEnd(Math.min(root.getElement(endElIndex).getEndOffset(), doc.getLength()));
//			setCaretPosition(start);
		} catch (BadLocationException e) { }
	}
	
	public boolean indentSelection(boolean add) { // or remove
		final Element root = doc.getDefaultRootElement();
		int startElIndex = root.getElementIndex(getSelectionStart());
		int endElIndex = root.getElementIndex(getSelectionEnd());
		if(startElIndex != endElIndex) {
			final int start = root.getElement(startElIndex).getStartOffset();
			final int end = Math.min(root.getElement(endElIndex).getEndOffset(), doc.getLength()); // javadoc: AbstractDocument models an implied break at the end of the document
			try {
				String txt = doc.getText(start, end - start);
				if(add) {
					txt = LINE_TAB.matcher(txt).replaceAll("");
				} else {
					txt = LINE_INIT.matcher(txt).replaceAll("\\\t");
				}
				replace(start, end - start, txt);
				setSelectionStart(start);
				setSelectionEnd(Math.min(root.getElement(endElIndex).getEndOffset(), doc.getLength()));
				return true;
			} catch (BadLocationException e1) { }
		}
		return false;
	}
	
	private static SimpleAttributeSet baseStyle() {
		SimpleAttributeSet style = new SimpleAttributeSet();
		StyleConstants.setFontFamily(style, Font.MONOSPACED);
		return style;
	}

//	public static void main(String... strings) {
//		String test = "uno\ndue\ntre\nquattro\n // text";
//		System.out.println(test);
//		System.out.println("**************");
//		Pattern p = Pattern.compile("(^)(.*)" , Pattern.MULTILINE );
//		Matcher m = p.matcher(test);
//		String res = m.replaceAll("$1//$2");
//		System.out.println(res);
//		System.out.println("**************");
//		Pattern pc = Pattern.compile("^|\\R");
//		m = pc.matcher(test);
//		System.out.println(m.results().count());
//		Pattern pcc = Pattern.compile("(^|\\R)\\s*//");
//		m = pcc.matcher(test);
//		System.out.println(m.results().count());
//		System.out.println("**************");
//		Pattern rc = Pattern.compile("(^|\\R)(\\s*)//");
//		m = rc.matcher(res);
//		String resr = m.replaceAll("$1$2");
//		System.out.println(resr);
//	}
}