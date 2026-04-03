package nl.han.ica.icss.transforms;

import nl.han.ica.datastructures.HANLinkedList;
import nl.han.ica.datastructures.IHANLinkedList;
import nl.han.ica.icss.ast.AST;
import nl.han.ica.icss.ast.ASTNode;
import nl.han.ica.icss.ast.Declaration;
import nl.han.ica.icss.ast.Expression;
import nl.han.ica.icss.ast.IfClause;
import nl.han.ica.icss.ast.Literal;
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
import nl.han.ica.icss.ast.operations.DivideOperation;
import nl.han.ica.icss.ast.operations.MultiplyOperation;
import nl.han.ica.icss.ast.operations.SubtractOperation;

import java.util.ArrayList;
import java.util.HashMap;

public class Evaluator implements Transform {

    private IHANLinkedList<HashMap<String, Literal>> variableValues;

    public Evaluator() {
    }

    @Override
    public void apply(AST ast) {
        variableValues = new HANLinkedList<>();
        variableValues.addFirst(new HashMap<>());
        evalStylesheet(ast.root);
    }

    private void pushScope() {
        variableValues.addFirst(new HashMap<>());
    }

    private void popScope() {
        if (variableValues.getSize() > 1) {
            variableValues.removeFirst();
        }
    }

    private void evalStylesheet(Stylesheet stylesheet) {
        for (ASTNode node : stylesheet.body) {
            if (node instanceof VariableAssignment) {
                evalVariableAssignment((VariableAssignment) node);
                continue;
            }
            if (node instanceof Stylerule) {
                evalStylerule((Stylerule) node);
            }
        }
    }

    private void evalStylerule(Stylerule stylerule) {
        pushScope();
        int index = 0;
        while (index < stylerule.body.size()) {
            ASTNode node = stylerule.body.get(index);
            if (node instanceof IfClause) {
                IfClause ifClause = (IfClause) node;
                Literal cond = evalExpression(ifClause.conditionalExpression);
                boolean takeIfBranch = false;
                if (cond instanceof BoolLiteral) {
                    takeIfBranch = ((BoolLiteral) cond).value;
                } else {
                    ifClause.setError(cond == null
                            ? "Could not evaluate if condition."
                            : "If condition must be a boolean value.");
                }
                stylerule.body.remove(index);

                ArrayList<ASTNode> chosenBranch = takeIfBranch
                        ? ifClause.body
                        : (ifClause.elseClause != null ? ifClause.elseClause.body : new ArrayList<>());

                pushScope();
                for (int branchIndex = 0; branchIndex < chosenBranch.size(); branchIndex++) {
                    ASTNode branchNode = chosenBranch.get(branchIndex);
                    if (branchNode instanceof VariableAssignment) {
                        evalVariableAssignment((VariableAssignment) branchNode);
                        stylerule.body.add(index + branchIndex, branchNode);
                    } else if (branchNode instanceof Declaration) {
                        Declaration declaration = (Declaration) branchNode;
                        applyEvaluatedExpression(declaration);
                        stylerule.body.add(index + branchIndex, declaration);
                    } else if (branchNode instanceof IfClause) {
                        stylerule.body.add(index + branchIndex, branchNode);
                    } else {
                        stylerule.body.add(index + branchIndex, branchNode);
                    }
                }
                popScope();
                continue;
            }

            if (node instanceof Declaration) {
                applyEvaluatedExpression((Declaration) node);
                index++;
                continue;
            }
            if (node instanceof VariableAssignment) {
                evalVariableAssignment((VariableAssignment) node);
                index++;
                continue;
            }
            index++;
        }
        popScope();
    }

    private void applyEvaluatedExpression(Declaration declaration) {
        Literal result = evalExpression(declaration.expression);
        if (result == null) {
            declaration.setError("Could not evaluate declaration value.");
        } else {
            declaration.expression = result;
        }
    }

    private void evalVariableAssignment(VariableAssignment assignment) {
        Literal value = evalExpression(assignment.expression);
        if (value == null) {
            assignment.setError("Could not evaluate variable value.");
            return;
        }
        assignment.expression = value;
        variableValues.getFirst().put(assignment.name.name, copyLiteral(value));
    }

    private Literal evalExpression(Expression expression) {
        if (expression == null) {
            return null;
        }
        if (expression instanceof Literal) {
            return copyLiteral((Literal) expression);
        }
        if (expression instanceof VariableReference) {
            VariableReference ref = (VariableReference) expression;
            Literal value = lookupVariable(ref.name);
            if (value == null) {
                ref.setError("Variable has no value at evaluation time.");
                return null;
            }
            return copyLiteral(value);
        }
        if (expression instanceof AddOperation) {
            AddOperation operation = (AddOperation) expression;
            Literal left = evalExpression(operation.lhs);
            Literal right = evalExpression(operation.rhs);
            return add(operation, left, right);
        }
        if (expression instanceof SubtractOperation) {
            SubtractOperation operation = (SubtractOperation) expression;
            Literal left = evalExpression(operation.lhs);
            Literal right = evalExpression(operation.rhs);
            return subtract(operation, left, right);
        }
        if (expression instanceof MultiplyOperation) {
            MultiplyOperation operation = (MultiplyOperation) expression;
            Literal left = evalExpression(operation.lhs);
            Literal right = evalExpression(operation.rhs);
            return multiply(operation, left, right);
        }
        if (expression instanceof DivideOperation) {
            DivideOperation operation = (DivideOperation) expression;
            Literal left = evalExpression(operation.lhs);
            Literal right = evalExpression(operation.rhs);
            return divide(operation, left, right);
        }
        expression.setError("Unsupported expression in evaluation.");
        return null;
    }

    private Literal add(AddOperation operation, Literal left, Literal right) {
        if (left == null || right == null) {
            return null;
        }
        if (left instanceof PixelLiteral && right instanceof PixelLiteral) {
            return new PixelLiteral(((PixelLiteral) left).value + ((PixelLiteral) right).value);
        }
        if (left instanceof PercentageLiteral && right instanceof PercentageLiteral) {
            return new PercentageLiteral(((PercentageLiteral) left).value + ((PercentageLiteral) right).value);
        }
        if (left instanceof ScalarLiteral && right instanceof ScalarLiteral) {
            return new ScalarLiteral(((ScalarLiteral) left).value + ((ScalarLiteral) right).value);
        }
        operation.setError("Invalid operands for addition.");
        return null;
    }

    private Literal subtract(SubtractOperation operation, Literal left, Literal right) {
        if (left == null || right == null) {
            return null;
        }
        if (left instanceof PixelLiteral && right instanceof PixelLiteral) {
            return new PixelLiteral(((PixelLiteral) left).value - ((PixelLiteral) right).value);
        }
        if (left instanceof PercentageLiteral && right instanceof PercentageLiteral) {
            return new PercentageLiteral(((PercentageLiteral) left).value - ((PercentageLiteral) right).value);
        }
        if (left instanceof ScalarLiteral && right instanceof ScalarLiteral) {
            return new ScalarLiteral(((ScalarLiteral) left).value - ((ScalarLiteral) right).value);
        }
        operation.setError("Invalid operands for subtraction.");
        return null;
    }

    private Literal multiply(MultiplyOperation operation, Literal left, Literal right) {
        if (left == null || right == null) {
            return null;
        }
        if (left instanceof ScalarLiteral && right instanceof ScalarLiteral) {
            return new ScalarLiteral(((ScalarLiteral) left).value * ((ScalarLiteral) right).value);
        }
        if (left instanceof ScalarLiteral && right instanceof PixelLiteral) {
            return new PixelLiteral(((ScalarLiteral) left).value * ((PixelLiteral) right).value);
        }
        if (left instanceof PixelLiteral && right instanceof ScalarLiteral) {
            return new PixelLiteral(((PixelLiteral) left).value * ((ScalarLiteral) right).value);
        }
        if (left instanceof ScalarLiteral && right instanceof PercentageLiteral) {
            return new PercentageLiteral(((ScalarLiteral) left).value * ((PercentageLiteral) right).value);
        }
        if (left instanceof PercentageLiteral && right instanceof ScalarLiteral) {
            return new PercentageLiteral(((PercentageLiteral) left).value * ((ScalarLiteral) right).value);
        }
        operation.setError("Invalid operands for multiplication.");
        return null;
    }

    private Literal divide(DivideOperation operation, Literal left, Literal right) {
        if (left == null || right == null) {
            return null;
        }
        if (left instanceof ScalarLiteral && right instanceof ScalarLiteral) {
            int divisor = ((ScalarLiteral) right).value;
            if (divisor == 0) {
                operation.setError("Division by zero.");
                return null;
            }
            return new ScalarLiteral(((ScalarLiteral) left).value / divisor);
        }
        if (left instanceof PixelLiteral && right instanceof ScalarLiteral) {
            int divisor = ((ScalarLiteral) right).value;
            if (divisor == 0) {
                operation.setError("Division by zero.");
                return null;
            }
            return new PixelLiteral(((PixelLiteral) left).value / divisor);
        }
        if (left instanceof PercentageLiteral && right instanceof ScalarLiteral) {
            int divisor = ((ScalarLiteral) right).value;
            if (divisor == 0) {
                operation.setError("Division by zero.");
                return null;
            }
            return new PercentageLiteral(((PercentageLiteral) left).value / divisor);
        }
        if (left instanceof PixelLiteral && right instanceof PixelLiteral) {
            int divisor = ((PixelLiteral) right).value;
            if (divisor == 0) {
                operation.setError("Division by zero.");
                return null;
            }
            return new ScalarLiteral(((PixelLiteral) left).value / divisor);
        }
        if (left instanceof PercentageLiteral && right instanceof PercentageLiteral) {
            int divisor = ((PercentageLiteral) right).value;
            if (divisor == 0) {
                operation.setError("Division by zero.");
                return null;
            }
            return new ScalarLiteral(((PercentageLiteral) left).value / divisor);
        }
        operation.setError("Invalid operands for division.");
        return null;
    }

    private Literal copyLiteral(Literal literal) {
        if (literal == null) {
            return null;
        }
        if (literal instanceof PixelLiteral) {
            return new PixelLiteral(((PixelLiteral) literal).value);
        }
        if (literal instanceof PercentageLiteral) {
            return new PercentageLiteral(((PercentageLiteral) literal).value);
        }
        if (literal instanceof ScalarLiteral) {
            return new ScalarLiteral(((ScalarLiteral) literal).value);
        }
        if (literal instanceof BoolLiteral) {
            return new BoolLiteral(((BoolLiteral) literal).value);
        }
        if (literal instanceof ColorLiteral) {
            return new ColorLiteral(((ColorLiteral) literal).value);
        }
        return null;
    }

    private Literal lookupVariable(String name) {
        for (int i = 0; i < variableValues.getSize(); i++) {
            HashMap<String, Literal> scope = variableValues.get(i);
            if (scope.containsKey(name)) {
                return scope.get(name);
            }
        }
        return null;
    }

}
