import java.util.List;

public class SpartieInterpreter {
    private Environment globalEnvironment = new Environment();

    public void run(List<Statement> statements) {
        for(Statement statement : statements) {
            interpret(statement);
        }
    }

    private void interpret(Statement statement) {
        switch(statement) {
            case Statement.PrintStatement printStatement ->  interpretPrintStatement(printStatement);
            case Statement.ExpressionStatement expressionStatement -> interpretExpressionStatement(expressionStatement);
            case Statement.VariableStatement variableStatement -> interpretVariableStatement(variableStatement);
            case Statement.BlockStatement blockStatement -> interpretBlockStatement(blockStatement);
            case Statement.IfStatement ifStatement -> interpretIfStatement(ifStatement);
            case Statement.WhileStatement whileStatement -> interpretWhileStatement(whileStatement);
            case null, default -> {}
        };
    }

    private Object interpret(Expression expression) {
        return switch (expression) {
            case Expression.LogicalExpression logicalExpression -> interpretLogical(logicalExpression);
            case Expression.AssignmentExpression assignmentExpression -> interpretAssign(assignmentExpression);
            case Expression.VariableExpression variableExpression -> interpretVariable(variableExpression);
            case Expression.LiteralExpression literalExpression -> interpretLiteral(literalExpression);
            case Expression.ParenthesesExpression parenthesesExpression -> interpretParenthesis(parenthesesExpression);
            case Expression.UnaryExpression unaryExpression -> interpretUnary(unaryExpression);
            case Expression.BinaryExpression binaryExpression -> interpretBinary(binaryExpression);
            case null, default -> null;
        };
    }

    // Statement Implementation
    private void interpretWhileStatement(Statement.WhileStatement statement) {
        // TODO: Evaluate the while statement based on the condition
        Expression condition = statement.condition;
        while (isTrue(interpret(condition))) {
            interpret(statement.body);
        }
    }

    private void interpretIfStatement(Statement.IfStatement statement) {
        // TODO: Evaluate the condition and then execute the appropriate branch
        Expression condition = statement.condition;
        boolean conditionValue = isTrue(interpret(condition));
        if (conditionValue) interpret(statement.thenBranch);
        else interpret(statement.elseBranch);
    }

    private void interpretBlockStatement(Statement.BlockStatement statement) {
        interpretBlock(statement.statements, new Environment(globalEnvironment));
    }

    private void interpretVariableStatement(Statement.VariableStatement statement) {
        Object value = null;
        if (statement.initializer != null) {
            // Evaluate the variable assignment expression
            value = interpret(statement.initializer);
        }
        globalEnvironment.define(statement.name.text, value);
    }

    private void interpretExpressionStatement(Statement.ExpressionStatement statement) {
        // We can re-use our previous interpret
        interpret(statement.expression);
    }

    private void interpretPrintStatement(Statement.PrintStatement statement) {
        // First evaluate the expression
        Object value = interpret(statement.expression);

        System.out.println(value.toString());
    }

    private void interpretBlock(List<Statement> statements, Environment environment) {
        // Store a reference to the previous environment and swap it out with the new environment
        Environment previous = globalEnvironment;

        globalEnvironment = environment;
        for (Statement statement : statements) {
            interpret(statement);
        }

        // Restore environment
        globalEnvironment = previous;
    }

    private Object interpretLogical(Expression.LogicalExpression logicalExpression) {
        Object left = interpret(logicalExpression.left);

        if (logicalExpression.operator.type == TokenType.OR) {
            // Short-circuit
            if (isTrue(left)) {
                return left;
            }
        }
        else {
            if (!isTrue(left)) {
                return left;
            }
        }

        // If we make it this far, we need to evaluate right
        return interpret(logicalExpression.right);
    }

    private Object interpretAssign(Expression.AssignmentExpression expression) {
        // TODO: Interpret the expression for the assignment and then assign it to our global environment,
        //  then return the value

        Token variable = expression.name;
        Object value = interpret(expression.value);
        globalEnvironment.assign(variable, value);
        return value;
    }

    private Object interpretVariable(Expression.VariableExpression expression) {
        // TODO: Return the value from our global environment
        String variableName = expression.name.text;
        return globalEnvironment.get(variableName);
    }

    private Object interpretLiteral(Expression.LiteralExpression expression) {
        return expression.literalValue;
    }

    private Object interpretParenthesis(Expression.ParenthesesExpression expression) {
        // Take what is inside and send it back
        return this.interpret(expression.expression);
    }

    private Object interpretUnary(Expression.UnaryExpression expression) {
        Object right = interpret(expression.right);

        switch (expression.operator.type) {
            case NOT:
                return !isTrue(right);
            case SUBTRACT:
                validateOperand(expression.operator, right);
                return -(double)right;
        }

        return null;
    }

    private Object interpretBinary(Expression.BinaryExpression expression) {
        Object left = interpret(expression.left);
        Object right = interpret(expression.right);

        // Handle unique case with add operator that can be applied to Strings and Doubles
        if (expression.operator.type == TokenType.ADD) {
            if (left instanceof Double && right instanceof Double) {
                return (double) left + (double) right;
            } else if (left instanceof String && right instanceof String) {
                return (String) left + (String) right;
            }
            else if ((left instanceof String || right instanceof String) && (left instanceof Double || right instanceof Double)) {
                if (left instanceof Double) {
                    return String.format("%.2f%s", (Double)left, (String)right);
                }
                else {
                    return String.format("%s%.2f", (String)left, (Double)right);
                }
            }
        }

        switch(expression.operator.type) {
            case EQUIVALENT:
                return isEquivalent(left, right);
            case NOT_EQUAL:
                return !isEquivalent(left, right);
        }

        // If we ge this far, then validate operands
        validateOperands(expression.operator, left, right);

        switch(expression.operator.type) {
            case SUBTRACT:
                return (double)left + (double)right;
            case MULTIPLY:
                return (double)left * (double)right;
            case DIVIDE:
                return (double)left / (double)right;
            case GREATER_THAN:
                return (double)left > (double)right;
            case GREATER_EQUAL:
                return (double)left >= (double)right;
            case LESS_THAN:
                return (double)left < (double)right;
            case LESS_EQUAL:
                return (double)left <= (double)right;
        }

        return null;
    }

    // Helper Methods

    // Test equivalency
    private boolean isEquivalent(Object left, Object right) {
        // They are equal under the following conditions:
        // 1. They are both null
        // 2. The equals method returns true (String or Double)

        // We have to account a NPE
        if (left == null && right == null) return true;
        if (left == null || right == null) return false;

        return left.equals(right);
    }

    // False is literal false or null
    private boolean isTrue(Object object) {
        if (object == null) return false;
        if (object instanceof Boolean) return (boolean)object;
        return true;
    }

    // Validate the type
    private void validateOperand(Token operator, Object operand) {
        if (operand instanceof Double) return;
        error("Invalid type on line " + operator.line + " : " + operator.text + operand);
    }

    private void validateOperands(Token operator, Object operand1, Object operand2) {
        if (operand1 instanceof Double && operand2 instanceof Double) return;
        error("Invalid type on line " + operator.line + " : " + operand1 + operator.text + operand2);
    }

    private void error(String message) {
        System.err.println(message);
        System.exit(2);
    }
}
