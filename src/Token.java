public class Token {
    public final TokenType type;
    public final String text;
    public final int line;
    public final Object literal;

    public Token(TokenType type, String text, int line) {
        this.type = type;
        this.text = text;
        this.line = line;
        this.literal = null;
    }

    public Token(TokenType type, String text, int line, Object literal) {
        this.type = type;
        this.text = text;
        this.line = line;
        this.literal = literal;
    }

    @Override
    public String toString() {
        return String.format("Line: %d Token: %s Text: %s", line, type, text);
    }
}
