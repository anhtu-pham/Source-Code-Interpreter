import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

// From Crafting Interpreters
public class SpartieParser {
    private static class ParseError extends RuntimeException {}
    private List<Token> tokens;
    private int current = 0;

    public SpartieParser(List<Token> tokens) {
        this.tokens = tokens;
    }

    public List<Statement> parse() {
        List<Statement> statements = new ArrayList<>();
        while (! isAtEnd()) {
            statements.add(declaration());
        }

        return statements;
    }

    private Statement declaration() {
        if (match(TokenType.VAR)) {
            return variableDeclaration();
        }
        else {
            return statement();
        }
    }

    private Statement statement() {
        if (match(TokenType.PRINT)) {
            return printStatement();
        }
        else if (match(TokenType.LEFT_BRACE)) {
            return new Statement.BlockStatement(block());
        }
        else if (match(TokenType.IF)) {
            return ifStatement();
        }
        else if (match(TokenType.WHILE)) {
            return whileStatement();
        }
        else if (match(TokenType.FOR)) {
            return forStatement();
        }

        return expressionStatement();
    }

    private Statement forStatement() {
        consume(TokenType.LEFT_PAREN, "Missing '(' after 'for'.");

        Statement initializer;
        if (match(TokenType.SEMICOLON)) {
            initializer = null;
        }
        else if (match(TokenType.VAR)) {
            initializer = variableDeclaration();
        }
        else {
            initializer = expressionStatement();
        }

        Expression condition = null;
        if (!check(TokenType.SEMICOLON)) {
            condition = expression();
        }
        consume(TokenType.SEMICOLON, "Expect ';' after for condition.");

        Expression increment = null;
        if (!check(TokenType.RIGHT_PAREN)) {
            increment = expression();
        }
        consume(TokenType.RIGHT_PAREN, "Expect ')' after for condition.");
        Statement body = statement();

        // TODO: We have the initializer, we have the condition, we have the increment. Take those components
        //  and convert into while loop. Hint: Build a block statement and then a while statement using the condition.

        List<Statement> statements = new ArrayList<>();
        if (body instanceof Statement.BlockStatement) {
            statements = ((Statement.BlockStatement) body).statements;
        }
        statements.add(new Statement.ExpressionStatement(increment));
        Statement whileBlock = new Statement.BlockStatement(statements);
        Statement whileStatement = new Statement.WhileStatement(condition, whileBlock);
        return new Statement.BlockStatement(List.of(initializer, whileStatement));
    }

    private Statement whileStatement() {
        consume(TokenType.LEFT_PAREN, "Missing '(' after 'while'.");
        Expression condition = expression();
        consume(TokenType.RIGHT_PAREN, "Expect ')' after while condition.");

        Statement body = statement();

        return new Statement.WhileStatement(condition, body);
    }
    private Statement ifStatement() {
        consume(TokenType.LEFT_PAREN, "Missing '(' after 'if'.");
        Expression condition = expression();
        consume(TokenType.RIGHT_PAREN, "Expect ')' after if condition.");

        Statement thenBranch = statement();
        Statement elseBranch = null;

        if (match(TokenType.ELSE)) {
            elseBranch = statement();
        }

        return new Statement.IfStatement(condition, thenBranch, elseBranch);
    }

    private List<Statement> block() {
        List<Statement> statements = new ArrayList<>();

        while (!check(TokenType.RIGHT_BRACE) && ! isAtEnd()) {
            statements.add(declaration());
        }

        consume(TokenType.RIGHT_BRACE, "Expected '}'.");
        return statements;
    }

    private Expression assignment() {
        Expression expression = or();

        if (match(TokenType.ASSIGN)) {
            Token equals = previous();
            Expression value = assignment();

            if (expression instanceof Expression.VariableExpression) {
                Token name = ((Expression.VariableExpression) expression).name;
                return new Expression.AssignmentExpression(name, value);
            }

            throw error(equals, "Invalid assignment");
        }

        return expression;
    }

    private Expression or() {
        Expression expression = and();

        while (match(TokenType.OR)) {
            Token operator = previous();
            Expression right = and();
            expression = new Expression.LogicalExpression(expression, operator, right);
        }

        return expression;
    }

    private Expression and() {
        Expression expression = equality();

        while (match(TokenType.AND)) {
            Token operator = previous();
            Expression right = equality();
            expression = new Expression.LogicalExpression(expression, operator, right);
        }

        return expression;
    }

    private Statement variableDeclaration() {
        // Check first to make sure we have a name for the variable
        Token variableName = consume(TokenType.IDENTIFIER, "Expected variable name.");

        // By default, no expression exists
        Expression initializer = null;

        // A variable is being initialized
        if (match(TokenType.ASSIGN)) {
            // Get expression to right
            initializer = expression();
        }

        consume(TokenType.SEMICOLON, "Expected ';' at end of variable declaration");

        return new Statement.VariableStatement(variableName, initializer);
    }

    private Statement printStatement() {
        Expression expression = expression();
        consume(TokenType.SEMICOLON, "Expected ';' at end of print.");
        return new Statement.PrintStatement(expression);
    }

    private Statement expressionStatement() {
        Expression expression = expression();
        consume(TokenType.SEMICOLON, "Expected ';' at end of expression.");
        return new Statement.ExpressionStatement(expression);
    }

    private Expression expression() {
        return assignment();
    }

    private Expression equality() {
        Expression expression = comparison();

        while (match(TokenType.NOT_EQUAL, TokenType.EQUIVALENT)) {
            Token operator = previous();
            Expression right = comparison();
            expression = new Expression.BinaryExpression(expression, operator, right);
        }

        return expression;
    }

    private Expression comparison() {
        Expression expression = term();

        while (match(TokenType.GREATER_THAN, TokenType.GREATER_EQUAL, TokenType.LESS_THAN, TokenType.LESS_EQUAL)) {
            Token operator = previous();
            Expression right = term();
            expression = new Expression.BinaryExpression(expression, operator, right);
        }

        return expression;
    }

    private Expression term() {
        Expression expression = factor();

        while (match(TokenType.SUBTRACT, TokenType.ADD)) {
            Token operator = previous();
            Expression right = factor();
            expression = new Expression.BinaryExpression(expression, operator, right);
        }

        return expression;
    }

    private Expression factor() {
        Expression expression = unary();

        while (match(TokenType.DIVIDE, TokenType.MULTIPLY)) {
            Token operator = previous();
            Expression right = unary();
            expression = new Expression.BinaryExpression(expression, operator, right);
        }

        return expression;
    }

    private Expression unary() {
        if (match(TokenType.NOT, TokenType.SUBTRACT)) {
            Token operator = previous();
            Expression right = unary();
            return new Expression.UnaryExpression(operator, right);
        }

        return primary();
    }

    private Expression primary() {
        if (match(TokenType.IDENTIFIER)) return new Expression.VariableExpression(previous());
        if (match(TokenType.FALSE)) return new Expression.LiteralExpression(false);
        if (match(TokenType.TRUE)) return new Expression.LiteralExpression(true);
        if (match(TokenType.NULL)) return new Expression.LiteralExpression(null);

        if (match(TokenType.NUMBER, TokenType.STRING)) {
            return new Expression.LiteralExpression(previous().literal);
        }

        if (match(TokenType.LEFT_PAREN)) {
            Expression expression = expression();
            consume(TokenType.RIGHT_PAREN, "Expect ')' after expression.");
            return new Expression.ParenthesesExpression(expression);
        }

        throw error(peek(), "Expected expression");
    }

    // Error reporting
    private Token consume(TokenType type, String message) {
        if (check(type)) return advance();

        throw error(peek(), message);
    }

    static ParseError error(Token token, String message) {
        if (token.type == TokenType.EOF) {
            System.err.println("Error occurred on line: " + token.line + " at end " + message);
        } else {
            System.err.println(token.line + " at '" + token.text + "'" + message);
        }
        return new ParseError();
    }

    // Utility functions
    private boolean match(TokenType... types) {
        for (TokenType type : types) {
            if (check(type)) {
                advance();
                return true;
            }
        }
        return false;
    }

    private boolean check(TokenType type) {
        if (isAtEnd()) return false;
        return peek().type == type;
    }

    private Token advance() {
        if (!isAtEnd()) current++;
        return previous();
    }

    private boolean isAtEnd() {
        return peek().type == TokenType.EOF;
    }

    private Token peek() {
        return tokens.get(current);
    }

    private Token previous() {
        return tokens.get(current - 1);
    }
}
