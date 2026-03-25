package nl.han.ica.icss.checker;

import nl.han.ica.datastructures.HANLinkedList;
import nl.han.ica.datastructures.IHANLinkedList;
import nl.han.ica.icss.ast.ASTNode;
import nl.han.ica.icss.ast.AST;
import nl.han.ica.icss.ast.Declaration;
import nl.han.ica.icss.ast.ElseClause;
import nl.han.ica.icss.ast.Expression;
import nl.han.ica.icss.ast.IfClause;
import nl.han.ica.icss.ast.Literal;
import nl.han.ica.icss.ast.Operation;
import nl.han.ica.icss.ast.Selector;
import nl.han.ica.icss.ast.Stylerule;
import nl.han.ica.icss.ast.Stylesheet;
import nl.han.ica.icss.ast.VariableAssignment;
import nl.han.ica.icss.ast.VariableReference;
import nl.han.ica.icss.ast.literals.BoolLiteral;
import nl.han.ica.icss.ast.literals.ColorLiteral;
import nl.han.ica.icss.ast.literals.PercentageLiteral;
import nl.han.ica.icss.ast.literals.PixelLiteral;
import nl.han.ica.icss.ast.literals.ScalarLiteral;
import nl.han.ica.icss.ast.operations.AddOperation;
import nl.han.ica.icss.ast.operations.MultiplyOperation;
import nl.han.ica.icss.ast.operations.SubtractOperation;
import nl.han.ica.icss.ast.types.ExpressionType;

import java.util.HashMap;

public class Checker {

    private IHANLinkedList<HashMap<String, ExpressionType>> variableTypes;

    public void check(AST ast) {
        variableTypes = new HANLinkedList<>();
        pushScope();
        checkStylesheet(ast.root);
        popScope();
    }

    // Statement checks
    private void checkStylesheet(Stylesheet stylesheet) {
        for (ASTNode node : stylesheet.body) {
            checkStylesheetChild(node);
        }
    }

    private void checkStylesheetChild(ASTNode node) {
        if (node instanceof VariableAssignment) {
            checkVariableAssignment((VariableAssignment) node);
            return;
        }
        if (node instanceof Stylerule) {
            checkStylerule((Stylerule) node);
            return;
        }
        if (node instanceof Declaration) {
            checkDeclaration((Declaration) node);
            return;
        }
        if (node instanceof IfClause) {
            checkIfClause((IfClause) node);
        }
    }

    private void checkStylerule(Stylerule stylerule) {
        pushScope();
        for (ASTNode node : stylerule.getChildren()) {
            if (node instanceof Selector) {
                continue;
            }
            checkRuleBodyNode(node);
        }
        popScope();
    }

    private void checkRuleBodyNode(ASTNode node) {
        if (node instanceof Declaration) {
            checkDeclaration((Declaration) node);
            return;
        }
        if (node instanceof VariableAssignment) {
            checkVariableAssignment((VariableAssignment) node);
            return;
        }
        if (node instanceof IfClause) {
            checkIfClause((IfClause) node);
            return;
        }
        if (node instanceof ElseClause) {
            checkElseClause((ElseClause) node);
        }
    }

    private void checkIfClause(IfClause ifClause) {
        ExpressionType conditionType = inferType(ifClause.conditionalExpression);
        if (conditionType != ExpressionType.BOOL && conditionType != ExpressionType.UNDEFINED) {
            ifClause.setError("Condition in if-statement must be boolean.");
        }

        pushScope();
        for (ASTNode bodyNode : ifClause.body) {
            checkRuleBodyNode(bodyNode);
        }
        popScope();

        if (ifClause.elseClause != null) {
            checkElseClause(ifClause.elseClause);
        }
    }

    private void checkElseClause(ElseClause elseClause) {
        pushScope();
        for (ASTNode bodyNode : elseClause.body) {
            checkRuleBodyNode(bodyNode);
        }
        popScope();
    }

    private void checkVariableAssignment(VariableAssignment assignment) {
        ExpressionType expressionType = inferType(assignment.expression);
        declareVariable(assignment.name.name, expressionType);
    }

    private void checkDeclaration(Declaration declaration) {
        inferType(declaration.expression);
    }

    // Type inference
    private ExpressionType inferType(Expression expression) {
        if (expression == null) {
            return ExpressionType.UNDEFINED;
        }

        if (expression instanceof Literal) {
            return inferLiteralType((Literal) expression);
        }
        if (expression instanceof VariableReference) {
            return inferReferenceType((VariableReference) expression);
        }
        if (expression instanceof Operation) {
            return inferOperationType((Operation) expression);
        }
        return ExpressionType.UNDEFINED;
    }

    private ExpressionType inferLiteralType(Literal literal) {
        return typeOfLiteral(literal);
    }

    private ExpressionType inferReferenceType(VariableReference variableReference) {
        ExpressionType foundType = lookupVariableType(variableReference.name);
        if (foundType == null) {
            variableReference.setError("Variable is not defined in this scope.");
            return ExpressionType.UNDEFINED;
        }
        return foundType;
    }

    private ExpressionType inferOperationType(Operation operation) {
        ExpressionType lhsType = inferType(operation.lhs);
        ExpressionType rhsType = inferType(operation.rhs);

        if (operation instanceof AddOperation || operation instanceof SubtractOperation) {
            return checkAddSubtractType(operation, lhsType, rhsType);
        }
        if (operation instanceof MultiplyOperation) {
            return checkMultiplyType(operation, lhsType, rhsType);
        }
        return ExpressionType.UNDEFINED;
    }

    private ExpressionType checkAddSubtractType(Expression operationNode, ExpressionType lhsType,
            ExpressionType rhsType) {
        if (lhsType == ExpressionType.UNDEFINED || rhsType == ExpressionType.UNDEFINED) {
            return ExpressionType.UNDEFINED;
        }

        if (isIllegalArithmeticType(lhsType) || isIllegalArithmeticType(rhsType)) {
            operationNode.setError("Color and boolean values cannot be used in arithmetic operations.");
            return ExpressionType.UNDEFINED;
        }

        if (lhsType != rhsType) {
            operationNode.setError("Both operands of + or - must have the same type.");
            return ExpressionType.UNDEFINED;
        }

        return lhsType;
    }

    private ExpressionType checkMultiplyType(Expression operationNode, ExpressionType lhsType, ExpressionType rhsType) {
        if (lhsType == ExpressionType.UNDEFINED || rhsType == ExpressionType.UNDEFINED) {
            return ExpressionType.UNDEFINED;
        }

        if (isIllegalArithmeticType(lhsType) || isIllegalArithmeticType(rhsType)) {
            operationNode.setError("Color and boolean values cannot be used in arithmetic operations.");
            return ExpressionType.UNDEFINED;
        }

        if (lhsType == ExpressionType.SCALAR && rhsType == ExpressionType.SCALAR) {
            return ExpressionType.SCALAR;
        }
        if (lhsType == ExpressionType.SCALAR) {
            return rhsType;
        }
        if (rhsType == ExpressionType.SCALAR) {
            return lhsType;
        }

        operationNode.setError("Multiplication requires at least one scalar operand.");
        return ExpressionType.UNDEFINED;
    }

    private boolean isIllegalArithmeticType(ExpressionType type) {
        return type == ExpressionType.COLOR || type == ExpressionType.BOOL;
    }

    private ExpressionType typeOfLiteral(Literal literal) {
        if (literal instanceof PixelLiteral) {
            return ExpressionType.PIXEL;
        }
        if (literal instanceof PercentageLiteral) {
            return ExpressionType.PERCENTAGE;
        }
        if (literal instanceof ColorLiteral) {
            return ExpressionType.COLOR;
        }
        if (literal instanceof ScalarLiteral) {
            return ExpressionType.SCALAR;
        }
        if (literal instanceof BoolLiteral) {
            return ExpressionType.BOOL;
        }
        return ExpressionType.UNDEFINED;
    }

    // Scope management
    private void pushScope() {
        variableTypes.addFirst(new HashMap<>());
    }

    private void popScope() {
        if (variableTypes.getSize() > 0) {
            variableTypes.removeFirst();
        }
    }

    private void declareVariable(String name, ExpressionType type) {
        variableTypes.getFirst().put(name, type);
    }

    private ExpressionType lookupVariableType(String variableName) {
        for (int i = 0; i < variableTypes.getSize(); i++) {
            HashMap<String, ExpressionType> scope = variableTypes.get(i);
            if (scope.containsKey(variableName)) {
                return scope.get(variableName);
            }
        }
        return null;
    }
}
