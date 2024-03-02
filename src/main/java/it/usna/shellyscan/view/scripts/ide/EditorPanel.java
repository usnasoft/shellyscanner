package it.usna.shellyscan.view.scripts.ide;

import java.awt.Color;
import java.awt.Font;

import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.Style;
import javax.swing.text.StyleConstants;

import it.usna.swing.SyntaxEditor;

public class EditorPanel extends SyntaxEditor{
	private static final long serialVersionUID = 1L;

	EditorPanel(String initText) {
		super(baseStyle());
		activateUndo();
		setTabSize(4);
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
	
	private static SimpleAttributeSet baseStyle() {
		SimpleAttributeSet style = new SimpleAttributeSet();
		StyleConstants.setFontFamily(style, Font.MONOSPACED);
		return style;
	}
}