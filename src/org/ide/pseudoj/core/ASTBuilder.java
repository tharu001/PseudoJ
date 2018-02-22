package org.ide.pseudoj.core;

import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jface.text.IDocument;

public class ASTBuilder {
	
	private static CompilationUnit cu;
	
	public static CompilationUnit getAST(IDocument doc) {
		ASTParser parser = ASTParser.newParser(AST.JLS9);
		parser.setSource(doc.get().toCharArray());
		parser.setKind(ASTParser.K_COMPILATION_UNIT);
		//ASTNode node = parser.createAST(null);
		cu = (CompilationUnit) parser.createAST(null);
		return cu;
	}
	
	public static void resetAST() {
		cu = null;
	}
}
