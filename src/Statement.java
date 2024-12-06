import java.util.List;

public class Statement {
    static class WhileStatement extends Statement {
        public Expression condition;
        public Statement body;

        public WhileStatement(Expression condition, Statement body) {
            this.condition = condition;
            this.body = body;
        }
    }

    static class IfStatement extends Statement {
        public Expression condition;
        public Statement thenBranch;
        public Statement elseBranch;

        public IfStatement(Expression condition, Statement thenBranch, Statement elseBranch) {
            this.condition = condition;
            this.thenBranch = thenBranch;
            this.elseBranch = elseBranch;
        }
    }

    static class PrintStatement extends Statement {
        public Expression expression;

        public PrintStatement(Expression expression) {
            this.expression = expression;
        }
    }

    static class BlockStatement extends Statement {
        public List<Statement> statements;

        public BlockStatement(List<Statement> statements) {
            this.statements = statements;
        }
    }

    static class ExpressionStatement extends Statement {
        public Expression expression;

        public ExpressionStatement(Expression expression) {
            this.expression = expression;
        }
    }

    static class VariableStatement extends Statement {
        public Token name;
        public Expression initializer;

        public VariableStatement(Token name, Expression initializer) {
            this.name = name;
            this.initializer = initializer;
        }
    }
}
