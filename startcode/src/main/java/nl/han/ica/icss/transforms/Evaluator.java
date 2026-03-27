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
                boolean takeIfBranch = asBoolean(evalExpression(ifClause.conditionalExpression));
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
                        declaration.expression = evalExpression(declaration.expression);
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
                Declaration declaration = (Declaration) node;
                declaration.expression = evalExpression(declaration.expression);
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

    private void evalVariableAssignment(VariableAssignment assignment) {
        Literal value = evalExpression(assignment.expression);
        assignment.expression = copyLiteral(value);
        variableValues.getFirst().put(assignment.name.name, copyLiteral(value));
    }

    private Literal evalExpression(Expression expression) {
        if (expression instanceof Literal) {
            return copyLiteral((Literal) expression);
        }
        if (expression instanceof VariableReference) {
            return lookup(((VariableReference) expression).name);
        }
        if (expression instanceof AddOperation) {
            AddOperation operation = (AddOperation) expression;
            return add(evalExpression(operation.lhs), evalExpression(operation.rhs));
        }
        if (expression instanceof SubtractOperation) {
            SubtractOperation operation = (SubtractOperation) expression;
            return subtract(evalExpression(operation.lhs), evalExpression(operation.rhs));
        }
        if (expression instanceof MultiplyOperation) {
            MultiplyOperation operation = (MultiplyOperation) expression;
            return multiply(evalExpression(operation.lhs), evalExpression(operation.rhs));
        }
        return new ScalarLiteral(0);
    }

    private boolean asBoolean(Literal literal) {
        return literal instanceof BoolLiteral && ((BoolLiteral) literal).value;
    }

    private Literal lookup(String name) {
        Literal value = lookupVariable(name);
        if (value != null) {
            return copyLiteral(value);
        }
        return new ScalarLiteral(0);
    }

    private Literal add(Literal left, Literal right) {
        if (left instanceof PixelLiteral && right instanceof PixelLiteral) {
            return new PixelLiteral(((PixelLiteral) left).value + ((PixelLiteral) right).value);
        }
        if (left instanceof PercentageLiteral && right instanceof PercentageLiteral) {
            return new PercentageLiteral(((PercentageLiteral) left).value + ((PercentageLiteral) right).value);
        }
        if (left instanceof ScalarLiteral && right instanceof ScalarLiteral) {
            return new ScalarLiteral(((ScalarLiteral) left).value + ((ScalarLiteral) right).value);
        }
        return new ScalarLiteral(0);
    }

    private Literal subtract(Literal left, Literal right) {
        if (left instanceof PixelLiteral && right instanceof PixelLiteral) {
            return new PixelLiteral(((PixelLiteral) left).value - ((PixelLiteral) right).value);
        }
        if (left instanceof PercentageLiteral && right instanceof PercentageLiteral) {
            return new PercentageLiteral(((PercentageLiteral) left).value - ((PercentageLiteral) right).value);
        }
        if (left instanceof ScalarLiteral && right instanceof ScalarLiteral) {
            return new ScalarLiteral(((ScalarLiteral) left).value - ((ScalarLiteral) right).value);
        }
        return new ScalarLiteral(0);
    }

    private Literal multiply(Literal left, Literal right) {
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
        return new ScalarLiteral(0);
    }

    private Literal copyLiteral(Literal literal) {
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
        return new ScalarLiteral(0);
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
