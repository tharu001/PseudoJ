package org.ide.pseudoj.editors;

import java.util.HashSet;
import java.util.Set;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.QualifiedName;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.StringLiteral;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;

public class Test {
	public static void main(String args[]){
		ASTParser parser = ASTParser.newParser(AST.JLS8);
		parser.setSource("public class A { public static void main(String[] args) {} int i = 9;  \\n int m; String h;\\n ArrayList<Integer> al = new ArrayList<Integer>();m=1000; int calculate(){}}".toCharArray());
		parser.setKind(ASTParser.K_COMPILATION_UNIT);
		//ASTNode node = parser.createAST(null);
 
 
		final CompilationUnit cu = (CompilationUnit) parser.createAST(null);
 
		cu.accept(new ASTVisitor() {
 
			Set<String> names = new HashSet<String>();
 
		    @SuppressWarnings("unchecked")
			public boolean visit(MethodDeclaration node) {
		        AST ast = node.getAST();

		        MethodInvocation methodInvocation = ast.newMethodInvocation();

		        //System.out.println("Hello, World");
		        QualifiedName qName =
		                   ast.newQualifiedName(
		                            ast.newSimpleName("System"),
		                            ast.newSimpleName("out"));
		        methodInvocation.setExpression(qName);
		        methodInvocation.setName(ast.newSimpleName("println"));

		        StringLiteral literal = ast.newStringLiteral();
		        literal.setLiteralValue("Hello, World");
		        methodInvocation.arguments().add(literal);

		        // Append the statement
		        node.getBody().statements().add(ast.newExpressionStatement(methodInvocation));
				SimpleName name = node.getName();
				this.names.add(name.getIdentifier());
				System.out.println("Declaration of '"+name+"' at line"+cu.getLineNumber(name.getStartPosition()));
				
		        return super.visit(node);
		    }
		    
			public boolean visit(VariableDeclarationFragment node) {
				SimpleName name = node.getName();
				this.names.add(name.getIdentifier());
				System.out.println("Declaration of '"+name+"' at line"+cu.getLineNumber(name.getStartPosition()));
				return false; // do not continue to avoid usage info
			}
 
			public boolean visit(SimpleName node) {
				if (this.names.contains(node.getIdentifier())) {
				System.out.println("Usage of '" + node + "' at line " +	cu.getLineNumber(node.getStartPosition()));
				}
				return true;
			}
		});
	}
}