public enum TokenType {
    // Undefined
    UNDEFINED,

    // Variables and identifiers
    VAR, IDENTIFIER,

    // Value types
    STRING, NUMBER,

    // Comparison
    EQUIVALENT, NOT_EQUAL,
    LESS_THAN, LESS_EQUAL,
    GREATER_THAN, GREATER_EQUAL,

    // Simple tokens
    SEMICOLON,
    COMMA,
    ASSIGN, // =
    LEFT_BRACE, RIGHT_BRACE, // { }
    LEFT_PAREN, RIGHT_PAREN, // ( )
    DIVIDE, MULTIPLY, // / *
    ADD, SUBTRACT, // + -
    NOT, // !

    // Control flow
    FOR, WHILE, IF, ELSE,
    FUN, // fun someFunction()
    RETURN,

    // Logical
    TRUE, FALSE, AND, OR,

    // Built in
    PRINT,

    // Markers
    EOF, EOL,

    // No Value
    NULL,

    // Ignore - Comments, spaces
    IGNORE
}
