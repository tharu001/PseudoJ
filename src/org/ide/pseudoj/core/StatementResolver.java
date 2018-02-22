package org.ide.pseudoj.core;

import java.util.List;

import java.io.InputStream;
import java.nio.file.NoSuchFileException;
import java.util.ArrayList;
import java.util.Scanner;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.eclipse.jdt.core.dom.PrimitiveType;
import org.eclipse.jdt.core.dom.PrimitiveType.Code;
import org.eclipse.jdt.core.dom.Type;

public class StatementResolver {
	private InputStream ruleReader;
	List<String> rules;

	final private int VARIABLE_DEC = 1;
	final private int METHOD_DEC = 2;

	public StatementResolver() {
		initRules();
	}

	public int filter(String stmt) {
		String[] userWords = stmt.split("\\s+");

		int action = getAction(userWords);
		if (action == VARIABLE_DEC) {
			return VARIABLE_DEC;
		}
		return 0;
	}

	public int getAction(String[] userWords) {
		for (String rule : rules) {
			if (rule.contains(userWords[0])) {
				String[] keyWords = rule.split("\\@");
				// determine action
				if (keyWords[1].contains("variable_dec"))
					return VARIABLE_DEC;
			}
		}
		return 0;
	}

	public ArrayList<List> processVariableDeclearation(String stmt) {
		String cleanStmt = stmt;
		for (String rule : rules) {
			String[] keyWords = rule.split("\\@");
			// determine reserved words and get rid of them
			for (String s : keyWords[0].split("\\$")) {
				cleanStmt = cleanStmt.replace(s, " ");
			}
		}
		cleanStmt = cleanStmt.replace("/", " ");
		String[] userWords = cleanStmt.split("\\s+");
		boolean leaveFirst = false;
		boolean proceed = true;
		ArrayList<List> values = new ArrayList<List>();
		List<String> paramList = new ArrayList<String>();
		List<Object> valueList = new ArrayList<Object>();
		List<Code> typeList = new ArrayList<Code>();
		String previousValue = null;
		String previousParam = null;
		Code previousType = null;
		int repeatingValueCount = 0;
		int repeatingParamCount = 0;

		for (Object userWord : userWords) {
			proceed = true;
			if (leaveFirst) {
				for (String rule : rules) {
					String[] keyWords = rule.split("\\@");
					for (String s : keyWords[0].split("$")) {
						if (userWord.toString().equals(s)) {
							proceed = false;
						}
					}
				}
				if (proceed) {
					try {
						if (NumberUtils.isCreatable(userWord.toString())) {
							if (userWord.toString().split("\\.").length < 2
									&& Long.parseLong(userWord.toString()) > 2147483647
									&& !(Long.parseLong(userWord.toString()) < -9223372036854775808L)) {
								if (previousValue == null || previousValue.equals(null)) {
									previousValue = userWord.toString() + "L";
									previousType = PrimitiveType.LONG;
								}
								for (int i = 0; i < repeatingParamCount - 1; i++) {
									valueList.add(previousValue);
									typeList.add(previousType);
								}
								valueList.add(userWord.toString() + "L");
								typeList.add(PrimitiveType.LONG);
								previousValue = userWord.toString() + "L";
								previousType = PrimitiveType.LONG;
								repeatingValueCount++;
								repeatingParamCount = 0;

							} else if (userWord.toString().split("\\.").length < 2
									&& Integer.parseInt(userWord.toString()) > 32768
									&& !(Integer.parseInt(userWord.toString()) < -2147483648)) {
								if (previousValue == null || previousValue.equals(null)) {
									previousValue = userWord.toString();
									previousType = PrimitiveType.INT;
								}
								for (int i = 0; i < repeatingParamCount - 1; i++) {
									valueList.add(previousValue);
									typeList.add(previousType);
								}
								valueList.add(userWord.toString());
								typeList.add(PrimitiveType.INT);
								previousValue = userWord.toString();
								previousType = PrimitiveType.INT;
								repeatingValueCount++;
								repeatingParamCount = 0;

							} else if (userWord.toString().split("\\.").length < 2
									&& Short.parseShort(userWord.toString()) <= 32768
									&& !(Integer.parseInt(userWord.toString()) < -32768)) {
								if (previousValue == null || previousValue.equals(null)) {
									previousValue = userWord.toString();
									previousType = PrimitiveType.SHORT;
								}
								for (int i = 0; i < repeatingParamCount - 1; i++) {
									valueList.add(previousValue);
									typeList.add(previousType);
								}
								valueList.add(userWord.toString());
								typeList.add(PrimitiveType.SHORT);
								previousValue = userWord.toString();
								previousType = PrimitiveType.SHORT;
								repeatingValueCount++;
								repeatingParamCount = 0;

							} else if (Integer.parseInt(userWord.toString().split("\\.")[1]) <= 4028235E38) {
								if (previousValue == null || previousValue.equals(null)) {
									previousValue = userWord.toString();
									previousType = PrimitiveType.FLOAT;
								}
								for (int i = 0; i < repeatingParamCount - 1; i++) {
									valueList.add(previousValue);
									typeList.add(previousType);
								}
								valueList.add(userWord.toString());
								typeList.add(PrimitiveType.FLOAT);
								previousValue = userWord.toString();
								previousType = PrimitiveType.FLOAT;
								repeatingValueCount++;
								repeatingParamCount = 0;

							} else if (userWord.toString().split("\\.")[1].matches("[0-9]+")
									&& userWord.toString().length() > 9) {
								if (previousValue == null || previousValue.equals(null)) {
									previousValue = userWord.toString();
									previousType = PrimitiveType.DOUBLE;
								}
								for (int i = 0; i < repeatingParamCount - 1; i++) {
									valueList.add(previousValue);
									typeList.add(previousType);
								}
								valueList.add(userWord.toString());
								typeList.add(PrimitiveType.DOUBLE);
								previousValue = userWord.toString();
								previousType = PrimitiveType.DOUBLE;
								repeatingValueCount++;
								repeatingParamCount = 0;

							}
						} else {
							if (userWord.toString().charAt(0) == '\'') {
								if (previousValue == null || previousValue.equals(null)) {
									try {
										previousValue = String.valueOf(userWord.toString().charAt(1));
									} catch (StringIndexOutOfBoundsException se) {
										previousValue = "\''";
									}
									previousType = PrimitiveType.CHAR;
								}
								for (int i = 0; i < repeatingParamCount - 1; i++) {
									valueList.add(previousValue);
									typeList.add(previousType);
								}
								try {

									valueList.add(String.valueOf(userWord.toString().charAt(1)));
									typeList.add(PrimitiveType.CHAR);
									previousValue = String.valueOf(userWord.toString().charAt(1));
								} catch (StringIndexOutOfBoundsException se) {
									valueList.add("\''");
									previousValue = "\''";

								}
								previousType = PrimitiveType.CHAR;
								repeatingValueCount++;
								repeatingParamCount = 0;

							} else if (userWord.toString().charAt(0) == '\"') {
								if (previousValue == null || previousValue.equals(null)) {
									previousValue = userWord.toString();
									previousType = PrimitiveType.VOID;
								}
								for (int i = 0; i < repeatingParamCount - 1; i++) {
									valueList.add(previousValue);
									typeList.add(previousType);
								}
								valueList.add(userWord.toString());
								typeList.add(PrimitiveType.VOID);
								previousValue = userWord.toString();
								previousType = PrimitiveType.VOID;
								repeatingValueCount++;
								repeatingParamCount = 0;

							} else if (userWord.toString().equals("true") || userWord.equals("false")) {
								if (previousValue == null || previousValue.equals(null)) {
									previousValue = userWord.toString();
									previousType = PrimitiveType.BOOLEAN;
								}
								for (int i = 0; i < repeatingParamCount - 1; i++) {
									valueList.add(previousValue);
									typeList.add(previousType);
								}
								valueList.add(userWord.toString());
								typeList.add(PrimitiveType.BOOLEAN);
								previousValue = userWord.toString();
								previousType = PrimitiveType.BOOLEAN;
								repeatingValueCount++;
								repeatingParamCount = 0;

							} else {
								if (previousParam == null || previousParam.equals(null))
									previousParam = userWord.toString();
								for (int i = 0; i < repeatingValueCount - 1; i++) {
									paramList.add(previousParam);
								}
								previousParam = userWord.toString();
								repeatingParamCount++;
								paramList.add(userWord.toString());
								repeatingValueCount = 0;
							}
						}
					} catch (NumberFormatException ne) {
						try {
							if (userWord.toString().split("\\.")[1].matches("[0-9]+")
									&& userWord.toString().length() > 9) {
								if (previousValue == null || previousValue.equals(null)) {
									previousValue = userWord.toString();
									previousType = PrimitiveType.DOUBLE;
								}
								for (int i = 0; i < repeatingParamCount - 1; i++) {
									valueList.add(previousValue);
									typeList.add(previousType);
								}
								valueList.add(userWord.toString());
								typeList.add(PrimitiveType.DOUBLE);
								previousValue = userWord.toString();
								previousType = PrimitiveType.DOUBLE;
								repeatingValueCount++;
								repeatingParamCount = 0;

							} else {
								if (previousParam == null || previousParam.equals(null))
									previousParam = userWord.toString();
								for (int i = 0; i < repeatingValueCount - 1; i++) {
									paramList.add(previousParam);
								}
								previousParam = userWord.toString();
								repeatingParamCount++;
								paramList.add(userWord.toString());
								repeatingValueCount = 0;
							}
						} catch (ArrayIndexOutOfBoundsException ae) {
							if (previousParam == null || previousParam.equals(null))
								previousParam = userWord.toString();
							for (int i = 0; i < repeatingValueCount - 1; i++) {
								paramList.add(previousParam);
							}
							previousParam = userWord.toString();
							repeatingParamCount++;
							paramList.add(userWord.toString());
							repeatingValueCount = 0;
						}
					} catch (ArrayIndexOutOfBoundsException ae) {
						if (previousParam == null || previousParam.equals(null))
							previousParam = userWord.toString();
						for (int i = 0; i < repeatingValueCount - 1; i++) {
							paramList.add(previousParam);
						}
						previousParam = userWord.toString();
						repeatingParamCount++;
						paramList.add(userWord.toString());
						repeatingValueCount = 0;
					}
				}
			}
			leaveFirst = true;
		}
		for (int i = 0; i < repeatingParamCount - 1; i++) {
			valueList.add(previousValue);
			typeList.add(previousType);
		}
		for (int i = 0; i < repeatingValueCount - 1; i++) {
			paramList.add(previousParam);
		}
		values.add(paramList);
		values.add(valueList);
		values.add(typeList);
		return values;
	}

	public void initRules() {
		ClassLoader classLoader = this.getClass().getClassLoader();
		ruleReader = classLoader.getResourceAsStream("res/pseudoj.rules");
		// scan and load rules
		Scanner scanner = new Scanner(ruleReader);
		rules = new ArrayList<String>();
		while (scanner.hasNext()) {
			String line = scanner.nextLine();
			if (line != null && !line.isEmpty()) {
				if (line.charAt(0) != '#')
					rules.add(line.toLowerCase());
			}
		}
		scanner.close();
		if (ruleReader == null) {
			try {
				throw new NoSuchFileException("File not found");
			} catch (NoSuchFileException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}
}
