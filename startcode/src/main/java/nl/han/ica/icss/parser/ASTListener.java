package nl.han.ica.icss.parser;

import java.util.ArrayList;
import java.util.List;

import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.TerminalNode;

import nl.han.ica.datastructures.HANStack;
import nl.han.ica.datastructures.IHANStack;
import nl.han.ica.icss.ast.*;
import nl.han.ica.icss.ast.literals.*;
import nl.han.ica.icss.ast.operations.AddOperation;
import nl.han.ica.icss.ast.operations.MultiplyOperation;
import nl.han.ica.icss.ast.operations.SubtractOperation;
import nl.han.ica.icss.ast.selectors.ClassSelector;
import nl.han.ica.icss.ast.selectors.IdSelector;
import nl.han.ica.icss.ast.selectors.TagSelector;

/**
 * This class extracts the ICSS Abstract Syntax Tree from the Antlr Parse tree.
 */
public class ASTListener extends ICSSBaseListener {

	private final AST ast;
	private final IHANStack<ASTNode> currentContainer;
	private final IHANStack<Expression> expressionStack;

	public ASTListener() {
		ast = new AST();
		currentContainer = new HANStack<>();
		expressionStack = new HANStack<>();
	}

	public AST getAST() {
		return ast;
	}

	private void pushContainer(ASTNode node) {
		currentContainer.push(node);
	}

	private ASTNode currentNode() {
		ASTNode node = currentContainer.peek();
		if (node == null) {
			throw new IllegalStateException("Container stack is empty.");
		}
		return node;
	}

	private void addNodeToCurrentContainer(ASTNode node) {
		currentNode().addChild(node);
	}

	private void popContainerAndAddToParent() {
		ASTNode node = currentContainer.pop();
		if (node == null) {
			throw new IllegalStateException("Cannot pop container from empty stack.");
		}
		addNodeToCurrentContainer(node);
	}

	private Expression popExpression(String context) {
		Expression expression = expressionStack.pop();
		if (expression == null) {
			throw new IllegalStateException("Missing expression for " + context + ".");
		}
		return expression;
	}

	private Expression[] popExpressionsInOrder(int count, String context) {
		Expression[] items = new Expression[count];
		for (int i = count - 1; i >= 0; i--) {
			items[i] = popExpression(context);
		}
		return items;
	}

	private List<Integer> readAdditiveOperators(ICSSParser.AdditiveExpressionContext ctx) {
		List<Integer> operators = new ArrayList<>();
		if (ctx.children == null) {
			return operators;
		}
		for (ParseTree child : ctx.children) {
			if (child instanceof TerminalNode) {
				Token token = ((TerminalNode) child).getSymbol();
				if (token.getType() == ICSSParser.PLUS || token.getType() == ICSSParser.MIN) {
					operators.add(token.getType());
				}
			}
		}
		return operators;
	}

	@Override
	public void enterStylesheet(ICSSParser.StylesheetContext ctx) {
		pushContainer(ast.root);
	}

	@Override
	public void enterStyleRule(ICSSParser.StyleRuleContext ctx) {
		pushContainer(new Stylerule());
	}

	@Override
	public void exitStyleRule(ICSSParser.StyleRuleContext ctx) {
		popContainerAndAddToParent();
	}

	@Override
	public void exitClassSelector(ICSSParser.ClassSelectorContext ctx) {
		addNodeToCurrentContainer(new ClassSelector(ctx.getText()));
	}

	@Override
	public void exitIdSelector(ICSSParser.IdSelectorContext ctx) {
		addNodeToCurrentContainer(new IdSelector(ctx.getText()));
	}

	@Override
	public void exitTagSelector(ICSSParser.TagSelectorContext ctx) {
		addNodeToCurrentContainer(new TagSelector(ctx.getText()));
	}

	@Override
	public void enterDeclaration(ICSSParser.DeclarationContext ctx) {
		pushContainer(new Declaration());
	}

	@Override
	public void exitDeclaration(ICSSParser.DeclarationContext ctx) {
		Declaration declaration = (Declaration) currentContainer.pop();
		if (declaration == null) {
			throw new IllegalStateException("Cannot close declaration; stack is empty.");
		}
		declaration.addChild(popExpression("declaration"));
		addNodeToCurrentContainer(declaration);
	}

	@Override
	public void exitPropertyName(ICSSParser.PropertyNameContext ctx) {
		addNodeToCurrentContainer(new PropertyName(ctx.getText()));
	}

	@Override
	public void enterVariableAssignment(ICSSParser.VariableAssignmentContext ctx) {
		pushContainer(new VariableAssignment());
	}

	@Override
	public void exitVariableAssignment(ICSSParser.VariableAssignmentContext ctx) {
		VariableAssignment assignment = (VariableAssignment) currentContainer.pop();
		if (assignment == null) {
			throw new IllegalStateException("Cannot close variable assignment; stack is empty.");
		}
		assignment.addChild(popExpression("variable assignment"));
		addNodeToCurrentContainer(assignment);
	}

	@Override
	public void exitVariableReference(ICSSParser.VariableReferenceContext ctx) {
		// In "variableAssignment: variableReference := expression", the first
		// variableReference is the assigned name and not part of the expression.
		if (ctx.getParent() instanceof ICSSParser.VariableAssignmentContext) {
			addNodeToCurrentContainer(new VariableReference(ctx.getText()));
			return;
		}
		expressionStack.push(new VariableReference(ctx.getText()));
	}

	@Override
	public void enterIfClause(ICSSParser.IfClauseContext ctx) {
		pushContainer(new IfClause());
	}

	@Override
	public void exitIfClause(ICSSParser.IfClauseContext ctx) {
		IfClause ifClause = (IfClause) currentContainer.pop();
		if (ifClause == null) {
			throw new IllegalStateException("Cannot close if-clause; stack is empty.");
		}
		addNodeToCurrentContainer(ifClause);
	}

	@Override
	public void enterElseClause(ICSSParser.ElseClauseContext ctx) {
		pushContainer(new ElseClause());
	}

	@Override
	public void exitElseClause(ICSSParser.ElseClauseContext ctx) {
		popContainerAndAddToParent();
	}

	@Override
	public void exitIfGuard(ICSSParser.IfGuardContext ctx) {
		ASTNode node = currentNode();
		if (!(node instanceof IfClause)) {
			throw new IllegalStateException("If-guard must attach to an IfClause.");
		}
		node.addChild(popExpression("if guard"));
	}

	@Override
	public void exitLiteral(ICSSParser.LiteralContext ctx) {
		String text = ctx.getText();
		switch (ctx.getStart().getType()) {
		case ICSSParser.TRUE:
			expressionStack.push(new BoolLiteral(true));
			break;
		case ICSSParser.FALSE:
			expressionStack.push(new BoolLiteral(false));
			break;
		case ICSSParser.COLOR:
			expressionStack.push(new ColorLiteral(text));
			break;
		case ICSSParser.PIXELSIZE:
			expressionStack.push(new PixelLiteral(text));
			break;
		case ICSSParser.PERCENTAGE:
			expressionStack.push(new PercentageLiteral(text));
			break;
		case ICSSParser.SCALAR:
			expressionStack.push(new ScalarLiteral(text));
			break;
		default:
			throw new IllegalStateException("Unknown literal token: " + text);
		}
	}

	@Override
	public void exitMultiplicativeExpression(ICSSParser.MultiplicativeExpressionContext ctx) {
		int operandCount = ctx.primaryExpression().size();
		if (operandCount <= 1) {
			return;
		}

		Expression[] operands = popExpressionsInOrder(operandCount, "multiplication");
		Expression result = operands[0];
		for (int i = 1; i < operands.length; i++) {
			MultiplyOperation mul = new MultiplyOperation();
			mul.addChild(result);
			mul.addChild(operands[i]);
			result = mul;
		}
		expressionStack.push(result);
	}

	@Override
	public void exitAdditiveExpression(ICSSParser.AdditiveExpressionContext ctx) {
		int operandCount = ctx.multiplicativeExpression().size();
		if (operandCount <= 1) {
			return;
		}

		Expression[] operands = popExpressionsInOrder(operandCount, "addition/subtraction");
		List<Integer> operators = readAdditiveOperators(ctx);
		if (operators.size() != operandCount - 1) {
			throw new IllegalStateException("Mismatch between additive operands and operators.");
		}

		Expression result = operands[0];
		for (int i = 1; i < operands.length; i++) {
			int op = operators.get(i - 1);
			if (op == ICSSParser.PLUS) {
				AddOperation add = new AddOperation();
				add.addChild(result);
				add.addChild(operands[i]);
				result = add;
			} else {
				SubtractOperation sub = new SubtractOperation();
				sub.addChild(result);
				sub.addChild(operands[i]);
				result = sub;
			}
		}
		expressionStack.push(result);
	}
}