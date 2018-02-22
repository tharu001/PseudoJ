package org.ide.pseudoj.core;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Scanner;
import java.util.Set;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.Path;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.ArrayCreation;
import org.eclipse.jdt.core.dom.ArrayInitializer;
import org.eclipse.jdt.core.dom.Block;
import org.eclipse.jdt.core.dom.CharacterLiteral;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.Name;
import org.eclipse.jdt.core.dom.PrimitiveType;
import org.eclipse.jdt.core.dom.PrimitiveType.Code;
import org.eclipse.jdt.core.dom.QualifiedName;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.StringLiteral;
import org.eclipse.jdt.core.dom.TypeDeclaration;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;
import org.eclipse.jdt.core.dom.VariableDeclarationStatement;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.text.edits.MalformedTreeException;
import org.eclipse.text.edits.TextEdit;
import org.eclipse.ui.editors.text.TextFileDocumentProvider;
import org.eclipse.ui.texteditor.IDocumentProvider;

public class PreProcessor {
	// java AST to be compiled
	CompilationUnit unit;
	final String typeName;
	final IWorkspace workspace;
	final IFile javaFile;
	final IDocumentProvider provider;
	IDocument doc;
	StatementResolver resolver;

	final private int VARIABLE_DEC = 1;
	final private int METHOD_DEC = 2;

	public PreProcessor(String typeName, String projName) {
		String[] arr = typeName.split("\\.");
		this.typeName = arr[0];
		workspace = ResourcesPlugin.getWorkspace();
		javaFile = workspace.getRoot()
				.getFile(new Path("/" + projName + "/src/defaultPackage/" + this.typeName + ".java"));
		provider = new TextFileDocumentProvider();
		resolver = new StatementResolver();
		try {
			provider.connect(javaFile);
			doc = provider.getDocument(javaFile);
		} catch (CoreException e) {
			e.printStackTrace();
		}
	}

	// reinstantiate AST
	public void reInitAST() {
		ASTBuilder.resetAST();
		doc.set("package defaultPackage;\r\n" + "\r\n" + "public class " + this.typeName + "{\r\n"
				+ "public static void Main(String[] args){\r\n" + "	\r\n" + "}\r\n" + "}");
		unit = ASTBuilder.getAST(doc);
		unit.recordModifications();
	}

	// process pseudo-code line by line
	public void processCode(InputStream psInput) {
		reInitAST();
		Scanner scanner = new Scanner(psInput);
		while (scanner.hasNext()) {
			String line = scanner.nextLine();
			if (resolver.filter(line) == VARIABLE_DEC) {
				processVariableDeclearations(line);
			}
		}
		scanner.close();
		doFinishJava(); // finish the job and update the java file

	}

	@SuppressWarnings("unchecked")
	public void processVariableDeclearations(String line) {
		ArrayList<List> varDecList = resolver.processVariableDeclearation(line);
		List<String> paramList = (ArrayList<String>) varDecList.get(0);
		List<Object> valueList = (ArrayList<Object>) varDecList.get(1);
		List<Code> typeList = (ArrayList<Code>) varDecList.get(2);
		String varName = null;
		Code type = null;
		String cat = null;

		// search for array declarations and re-organize variables list with identified
		// arrays
		int length = 1;
		boolean arrayFound = false;
		boolean isEdge = false;
		int lastRemovedPosition = 0;

		for (int i = 0; i < paramList.size(); i++) {
			String paramName = null;
			Code typeName = null;
			if (i != 0) {
				if (paramList.get(i - 1).equals(paramList.get(i)) && !isEdge) {
					arrayFound = true;
					length++;
					if (i == paramList.size() - 2) {
						if (paramList.get(i).equals(paramList.get(i + 1))) {
							isEdge = true;
							length++;
						}
					}
				} else if (arrayFound) {
					List<Object> tempList = new ArrayList<Object>();
					if (isEdge)
						i++;
					for (int j = 1; j < length + 1; j++) {
						tempList.add(valueList.get(i - j));
						paramName = paramList.get(i - j);
						typeName = typeList.get(i - j);
						valueList.remove(i - j);
						paramList.remove(i - j);
						typeList.remove(i - j);
						lastRemovedPosition = i - j;
					}
					List<Object> arrayValues = new ArrayList<Object>();
					for (int k = 0; k < tempList.size(); k++) {
						arrayValues.add(tempList.get(tempList.size() - (k + 1)));
					}
					valueList.add(lastRemovedPosition, arrayValues);
					paramList.add(lastRemovedPosition, paramName);
					typeList.add(lastRemovedPosition, typeName);
					lastRemovedPosition = 0;
					arrayFound = false;
					isEdge = false;
					i = i - (length);
					length = 1;
				} else if (i == paramList.size() - 2) {
					if (paramList.get(i).equals(paramList.get(i + 1))) {
						isEdge = true;
						arrayFound = true;
						length++;
					}
				}
			} else if (i == paramList.size() - 2) {
				if (paramList.get(i).equals(paramList.get(i + 1))) {
					isEdge = true;
					arrayFound = true;
					length++;
				}
			}
		}
		// process variable/array declarations
		for (int i = 0; i < paramList.size(); i++) {
			varName = paramList.get(i);
			List<Object> values = new ArrayList<Object>();
			try {
				type = typeList.get(i);
				if (((ArrayList) valueList.get(i)).size() > 1) {
					cat = "array";
					declareVariable(varName, (ArrayList) valueList.get(i), type, cat);
				}
			} catch (ClassCastException ce) {
				cat = "var";
				values.add(valueList.get(i));
				declareVariable(varName, values, type, cat);
			} catch (IndexOutOfBoundsException ie) {
				type = null;
				cat = "var";
				values.add(null);
				declareVariable(varName, values, type, cat);
			} catch (NullPointerException ne) {
				doc.set("package defaultPackage;\r\n" + "\r\n" + "public class " + this.typeName + "{\r\n"
						+ "public static void Main(String[] args){\r\n"
						+ "\t//***Waiting.... Please complete your Pseudo-code!" + "	\r\n" + "}\r\n" + "}");
			}
		}
	}

	public void declareVariable(String varName, List<Object> varValue, Code type, String category) {
		TypeDeclaration typeDec = (TypeDeclaration) unit.types().get(0);
		MethodDeclaration md = typeDec.getMethods()[0];
		createStatements(unit.getAST(), md, varName, varValue, type, category);
	}

	@SuppressWarnings("unchecked")
	private void createStatements(AST ast, MethodDeclaration md, String varName, List<Object> varValue, Code type,
			String category) {

		VariableDeclarationFragment fragment = ast.newVariableDeclarationFragment();
		try {
		fragment.setName(ast.newSimpleName(varName));
		} catch(IllegalArgumentException ia) {
			//discard input
		}
		VariableDeclarationStatement varDeclStmt = ast.newVariableDeclarationStatement(fragment);

		if (category.equals("var")) {
			try {
				varDeclStmt.setType(ast.newPrimitiveType(type));
			} catch (IllegalArgumentException ia) {
				Name name = ast.newName("Object");
				varDeclStmt.setType(ast.newSimpleType(name));
				doc.set("package defaultPackage;\r\n" + "\r\n" + "public class " + this.typeName + "{\r\n"
						+ "public static void Main(String[] args){\r\n" + "\t//***Variable " + varName
						+ " is not set! Please assign the value!" + "	\r\n" + "}\r\n" + "}");
			}
			if (type == PrimitiveType.FLOAT) {
				fragment.setInitializer(ast.newNumberLiteral(varValue.get(0).toString() + "F"));
			} else if (type == PrimitiveType.INT || type == PrimitiveType.SHORT || type == PrimitiveType.LONG
					|| type == PrimitiveType.DOUBLE) {
				fragment.setInitializer(ast.newNumberLiteral(varValue.get(0).toString()));
			} else if (type == PrimitiveType.CHAR) {
				try {
					CharacterLiteral cl = ast.newCharacterLiteral();
					cl.setCharValue(varValue.get(0).toString().charAt(0));
					fragment.setInitializer(cl);
				} catch (IllegalArgumentException ie) {
					doc.set("package defaultPackage;\r\n" + "\r\n" + "public class " + this.typeName + "{\r\n"
							+ "public static void Main(String[] args){\r\n"
							+ "\t//***Waiting.... Please complete your Pseudo-code!" + "	\r\n" + "}\r\n" + "}");
				}
			} else if (type == PrimitiveType.VOID) {
				Name name = ast.newName("String");
				varDeclStmt.setType(ast.newSimpleType(name));
				StringLiteral sl = ast.newStringLiteral();
				try {
					sl.setEscapedValue(varValue.get(0).toString());
				} catch (IllegalArgumentException ie) {
					doc.set("package defaultPackage;\r\n" + "\r\n" + "public class " + this.typeName + "{\r\n"
							+ "public static void Main(String[] args){\r\n"
							+ "\t//***Waiting.... Please complete your Pseudo-code!" + "	\r\n" + "}\r\n" + "}");
				}
				fragment.setInitializer(sl);
			} else if (type == PrimitiveType.BOOLEAN) {
				fragment.setInitializer(ast.newBooleanLiteral(Boolean.parseBoolean(varValue.get(0).toString())));
			} else {
				fragment.setInitializer(ast.newNullLiteral());
			}

		} else if (category.equals("array")) {
			ArrayCreation ac3 = ast.newArrayCreation();

			// set array type
			if (type == PrimitiveType.VOID) {
				Name name = ast.newName("String");
				varDeclStmt.setType(ast.newArrayType(ast.newSimpleType(name)));
				ac3.setType(ast.newArrayType(ast.newPrimitiveType(type)));
			} else {
				varDeclStmt.setType(ast.newArrayType(ast.newPrimitiveType(type)));
				ac3.setType(ast.newArrayType(ast.newPrimitiveType(type)));
			}
			// set array initializer
			ArrayInitializer ai = ast.newArrayInitializer();
			ac3.setInitializer(ai);

			for (Object el : varValue) {
				if (type == PrimitiveType.FLOAT) {
					ai.expressions().add(ast.newNumberLiteral(el.toString() + "F"));
				} else if (type == PrimitiveType.INT || type == PrimitiveType.SHORT || type == PrimitiveType.LONG
						|| type == PrimitiveType.DOUBLE) {
					ai.expressions().add(ast.newNumberLiteral(el.toString()));
				} else if (type == PrimitiveType.CHAR) {
					CharacterLiteral cl = ast.newCharacterLiteral();
					cl.setCharValue(el.toString().charAt(0));
					ai.expressions().add(cl);
				} else if (type == PrimitiveType.VOID) {
					StringLiteral sl = ast.newStringLiteral();
					ai.expressions().add(sl);
					try {
						sl.setEscapedValue(el.toString());
					} catch (IllegalArgumentException ie) {
						doc.set("package defaultPackage;\r\n" + "\r\n" + "public class " + this.typeName + "{\r\n"
								+ "public static void Main(String[] args){\r\n"
								+ "\t//***Waiting.... Please complete your Pseudo-code!" + "	\r\n" + "}\r\n" + "}");
					}
				} else if (type == PrimitiveType.BOOLEAN) {
					ai.expressions().add(ast.newBooleanLiteral(Boolean.parseBoolean(el.toString())));
				}
			}
			fragment.setInitializer(ac3);

		}
		Block mainBlock = md.getBody();
		mainBlock.statements().add(varDeclStmt);
	}

	@SuppressWarnings("unchecked")
	public void addMethod() {
		MethodDeclaration md = unit.getAST().newMethodDeclaration();
		md.setName(unit.getAST().newSimpleName("newMethod"));
		md.setBody(unit.getAST().newBlock());
		TypeDeclaration typeDec = (TypeDeclaration) unit.types().get(0);
		typeDec.bodyDeclarations().add(md);
	}

	public void doFinishJava() {
		TextEdit edits = unit.rewrite(doc, null);
		try {
			edits.apply(doc);
		} catch (MalformedTreeException | BadLocationException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public void visitAST() {
		unit.accept(new ASTVisitor() {

			Set<String> names = new HashSet<String>();

			@SuppressWarnings("unchecked")
			public boolean visit(MethodDeclaration node) {
				AST ast = node.getAST();

				MethodInvocation methodInvocation = ast.newMethodInvocation();

				QualifiedName qName = ast.newQualifiedName(ast.newSimpleName("System"), ast.newSimpleName("out"));
				methodInvocation.setExpression(qName);
				methodInvocation.setName(ast.newSimpleName("println"));

				StringLiteral literal = ast.newStringLiteral();
				literal.setLiteralValue("Hello, World");
				methodInvocation.arguments().add(literal);

				// Append the statement
				node.getBody().statements().add(ast.newExpressionStatement(methodInvocation));
				SimpleName name = node.getName();
				this.names.add(name.getIdentifier());
				System.out
						.println("Declaration of '" + name + "' at line" + unit.getLineNumber(name.getStartPosition()));

				return super.visit(node);
			}

			public boolean visit(VariableDeclarationFragment node) {
				SimpleName name = node.getName();
				this.names.add(name.getIdentifier());
				System.out
						.println("Declaration of '" + name + "' at line" + unit.getLineNumber(name.getStartPosition()));
				return false; // do not continue to avoid usage info
			}

			public boolean visit(SimpleName node) {
				if (this.names.contains(node.getIdentifier())) {
					System.out
							.println("Usage of '" + node + "' at line " + unit.getLineNumber(node.getStartPosition()));
				}
				return true;
			}
		});
	}
}
