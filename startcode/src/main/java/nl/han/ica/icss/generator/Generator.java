package nl.han.ica.icss.generator;

import nl.han.ica.icss.ast.AST;
import nl.han.ica.icss.ast.ASTNode;
import nl.han.ica.icss.ast.Declaration;
import nl.han.ica.icss.ast.Expression;
import nl.han.ica.icss.ast.Stylerule;
import nl.han.ica.icss.ast.VariableReference;
import nl.han.ica.icss.ast.literals.BoolLiteral;
import nl.han.ica.icss.ast.literals.ColorLiteral;
import nl.han.ica.icss.ast.literals.PercentageLiteral;
import nl.han.ica.icss.ast.literals.PixelLiteral;
import nl.han.ica.icss.ast.literals.ScalarLiteral;

public class Generator {

	public String generate(AST ast) {
		StringBuilder css = new StringBuilder();
		boolean firstRule = true;

		for (ASTNode node : ast.root.body) {
			if (!(node instanceof Stylerule)) {
				continue;
			}
			if (!firstRule) {
				css.append("\n");
			}
			writeStylerule(css, (Stylerule) node);
			firstRule = false;
		}

		return css.toString();
	}

	private void writeStylerule(StringBuilder css, Stylerule rule) {
		if (rule.selectors.isEmpty()) {
			return;
		}

		for (int i = 0; i < rule.selectors.size(); i++) {
			if (i > 0) {
				css.append(", ");
			}
			css.append(rule.selectors.get(i).toString());
		}
		css.append(" {\n");

		for (ASTNode node : rule.body) {
			if (node instanceof Declaration) {
				Declaration declaration = (Declaration) node;
				if (!declaration.hasError() && declaration.expression != null) {
					writeDeclaration(css, declaration);
				}
			}
		}

		css.append("}\n");
	}

	private void writeDeclaration(StringBuilder css, Declaration declaration) {
		css.append("  ");
		css.append(declaration.property.name);
		css.append(": ");
		css.append(expressionToCss(declaration.expression));
		css.append(";\n");
	}

	private String expressionToCss(Expression expression) {
		if (expression == null) {
			return "";
		}
		if (expression instanceof PixelLiteral) {
			return ((PixelLiteral) expression).value + "px";
		}
		if (expression instanceof PercentageLiteral) {
			return ((PercentageLiteral) expression).value + "%";
		}
		if (expression instanceof ColorLiteral) {
			return ((ColorLiteral) expression).value;
		}
		if (expression instanceof BoolLiteral) {
			return ((BoolLiteral) expression).value ? "TRUE" : "FALSE";
		}
		if (expression instanceof ScalarLiteral) {
			return Integer.toString(((ScalarLiteral) expression).value);
		}
		if (expression instanceof VariableReference) {
			return ((VariableReference) expression).name;
		}
		return "";
	}
}
