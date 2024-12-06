import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SpartieScanner {
    private String source;

    private int start = 0;
    private int current = 0;
    private int line = 1;

    private static final Map<String, TokenType> keywords = new HashMap<>();
    static {
        keywords.put("if", TokenType.IF);
        keywords.put("else", TokenType.ELSE);
        keywords.put("for", TokenType.FOR);
        keywords.put("while", TokenType.WHILE);
        keywords.put("true", TokenType.TRUE);
        keywords.put("false", TokenType.FALSE);
        keywords.put("fun", TokenType.FUN);
        keywords.put("return", TokenType.RETURN);
        keywords.put("var", TokenType.VAR);
        keywords.put("print", TokenType.PRINT);
        keywords.put("null", TokenType.NULL);
    }

    public SpartieScanner(String source) {
        this.source = source;
    }

    public List<Token> scan() {
        List<Token> tokens = new ArrayList<>();

        Token token = null;
        while (!isAtEnd() && (token = getNextToken()) != null) {
            if (token.type != TokenType.IGNORE) tokens.add(token);
        }
        tokens.add(new Token(TokenType.EOF, null, line));

        return tokens;
    }

    private Token getNextToken() {
        Token token = null;

        // Try to get each type of token, starting with single
        token = getSingleCharacterToken();
        if (token == null) token = getComparisonToken();
        if (token == null) token = getDivideOrComment();
        if (token == null) token = getStringToken();
        if (token == null) token = getNumericToken();
        if (token == null) token = getIdentifierOrReservedWord();
        if (token == null) {
            error(line, String.format("Unexpected character '%c' at %d", source.charAt(current), current));
        }

        return token;
    }

    private Token getDivideOrComment() {
        // Hint: Examine the character for a comparison but check the next character (as long as one is available)
        // For example: <
        char nextCharacter = source.charAt(current);

        if (nextCharacter == '/') {
            if (examine('/')) {
                while(!isAtEnd() && source.charAt(current) != '\n') {
                    current++;
                }
                return new Token(TokenType.IGNORE, "", line);
            }
            else {
                current++;
                return new Token(TokenType.DIVIDE, String.valueOf(nextCharacter), line);
            }
        }
        return null;
    }

    private Token getIdentifierOrReservedWord() {
        char nextCharacter = source.charAt(current);
        if (isAlpha(nextCharacter)) {
            start = current;
            while (isAlpha(nextCharacter)) {
                current++;
                if (isAtEnd()) break;

                nextCharacter = source.charAt(current);
            }
            String identifierOrKeyword = source.substring(start, current);

            // It is a keyword
            if (keywords.containsKey(identifierOrKeyword)) {
                return new Token(keywords.get(identifierOrKeyword), identifierOrKeyword, line, identifierOrKeyword);
            }
            else {
                return new Token(TokenType.IDENTIFIER, identifierOrKeyword, line, identifierOrKeyword);
            }
        }
        return null;
    }

    private Token getNumericToken() {
        char nextCharacter = source.charAt(current);
        if (isDigit(nextCharacter)) {
            start = current;
            boolean periodMatched = false;
            while (isDigit(nextCharacter) || nextCharacter == '.') {
                if (nextCharacter == '.' && periodMatched) {
                    // We have two periods
                    error(line, "Invalid number with two periods");
                    return null;
                }
                else if (nextCharacter == '.') {
                    periodMatched = true;
                }

                current++;
                if (isAtEnd()) break;

                nextCharacter = source.charAt(current);
            }
            String numberString = source.substring(start, current);
            Double numberValue = Double.parseDouble(numberString);

            return new Token(TokenType.NUMBER, numberString, line, numberValue);

        }
        return null;
    }

    private Token getStringToken() {
        char nextCharacter = source.charAt(current);

        String string = null;

        if (nextCharacter == '"') {
            int temp_current = current;

            while (!isAtEnd()) {
                temp_current++;
                if (temp_current >= source.length() || source.charAt(temp_current) == '\n') {
                    // We hit EOL, report error
                    current = temp_current;
                    error(line, "String did not terminate before new line.");
                    return null;
                }
                if (source.charAt(temp_current) == '"') {
                    // We have our string
                    string = source.substring(current + 1, temp_current);

                    // Update our current
                    current = temp_current + 1;
                    return new Token(TokenType.STRING, string, line, string);
                }
            }

            error(line, "String did not terminate.");
        }
        return null;
    }

    private Token getComparisonToken() {
        // Hint: Examine the character for a comparison but check the next character (as long as one is available)
        // For example: <
        char nextCharacter = source.charAt(current);

        TokenType type = TokenType.UNDEFINED;

        switch (nextCharacter) {
            // Comparison
            case '!': type = (examine('=') ? TokenType.NOT_EQUAL : TokenType.NOT); break;
            case '=': type = (examine('=') ? TokenType.EQUIVALENT : TokenType.ASSIGN); break;
            case '<': type = (examine('=') ? TokenType.LESS_EQUAL : TokenType.LESS_THAN); break;
            case '>': type = (examine('=') ? TokenType.GREATER_EQUAL : TokenType.GREATER_THAN); break;
        }

        if (type != TokenType.UNDEFINED) {
            // Check for two character token
            if (type == TokenType.NOT_EQUAL || type == TokenType.EQUIVALENT || type == TokenType.LESS_EQUAL || type == TokenType.GREATER_EQUAL) {
                current+=2;
                return new Token(type, source.substring(current - 2, current), line);
            }
            else {
                // Otherwise, we had one character token
                current++;
                return new Token(type, String.valueOf(nextCharacter), line);
            }
        }
        else {
            return null;
        }
    }

    private Token getSingleCharacterToken() {
        // Hint: Examine the character, if you can get a token, return it, otherwise return null
        // Hint: Be careful with the divide, we have ot know if it is a single character

        char nextCharacter = source.charAt(current);

        TokenType type = TokenType.UNDEFINED;

        // Start with tokens that are single characters
        switch (nextCharacter) {
            case '(': type = TokenType.LEFT_PAREN; break;
            case ')': type = TokenType.RIGHT_PAREN; break;
            case '{': type = TokenType.LEFT_BRACE; break;
            case '}': type = TokenType.RIGHT_BRACE; break;
            case ',': type = TokenType.COMMA; break;
            case '-': type = TokenType.SUBTRACT; break;
            case '+': type = TokenType.ADD; break;
            case '*': type = TokenType.MULTIPLY; break;
            case ';': type = TokenType.SEMICOLON; break;
            case '&': type = TokenType.AND; break;
            case '|': type = TokenType.OR; break;
            case '\n':
                type = TokenType.IGNORE;
                line++;
                break;
            case ' ', '\t', '\r':
                type = TokenType.IGNORE;
                break;
            default:
                break;
        }

        if (type != TokenType.UNDEFINED) {
            // We have a valid token, advanced and return token
            current++;
            return new Token(type, String.valueOf(nextCharacter), line);
        }
        else {
            return null;
        }
    }

    // Helper Methods
    private boolean isDigit(char character) {
        return character >= '0' && character <= '9';
    }

    private boolean isAlpha(char character) {
        return character >= 'a' && character <= 'z' ||
                character >= 'A' && character <= 'Z';
    }

    // This will check if a character is what you expect, if so, it will advance
    // Useful for checking <= or //
    private boolean examine(char expected) {
        if (isAtEnd()) return false;
        if (source.charAt(current + 1) != expected) return false;

        // Otherwise, it matches it, so advance
        return true;
    }

    private boolean isAtEnd() {
        return current >= source.length();
    }

    // Error handling
    private void error(int line, String message) {
        System.err.printf("Error occurred on line %d : %s\n", line, message);
        System.exit(ErrorCode.INTERPRET_ERROR);
    }
}
