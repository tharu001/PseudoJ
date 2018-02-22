package org.ide.pseudoj.editors;

import java.util.HashSet;

import java.util.Set;

import org.eclipse.core.runtime.NullProgressMonitor;
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

public class Test2 {
	public static void main(String args[]){
		String source = String.join("\n",
		        "public class HelloWorld {",
		        "    public static void main(String[] args) {",
		                  // Insert the following statement.
		                  // System.out.println("Hello, World");
		        "    }",
		        "}");

		ASTParser parser = ASTParser.newParser(AST.JLS8);
		parser.setSource(source.toCharArray());
		parser.setKind(ASTParser.K_COMPILATION_UNIT);

		CompilationUnit unit = (CompilationUnit) parser.createAST(new NullProgressMonitor());

		unit.accept(new ASTVisitor() {

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

		        return super.visit(node);
		    }
		});
	}
}