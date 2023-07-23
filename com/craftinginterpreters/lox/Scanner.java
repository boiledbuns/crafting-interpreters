package com.craftinginterpreters.lox;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.craftinginterpreters.lox.TokenType.*; 

class Scanner {
    private static final char NEW_LINE = '\n';

    private final String source;
    private final List<Token> tokens = new ArrayList<>();
    private int start = 0;
    // position of the cursor (on top of next unread item)
    private int current = 0;
    private int line = 1;
    private static final Map<String, TokenType> reservedKeywords; 

    static {
        reservedKeywords = new HashMap<>();
        reservedKeywords.put("and",    AND);
        reservedKeywords.put("class",  CLASS);
        reservedKeywords.put("else",   ELSE);
        reservedKeywords.put("false",  FALSE);
        reservedKeywords.put("for",    FOR);
        reservedKeywords.put("fun",    FUN);
        reservedKeywords.put("if",     IF);
        reservedKeywords.put("nil",    NIL);
        reservedKeywords.put("or",     OR);
        reservedKeywords.put("print",  PRINT);
        reservedKeywords.put("return", RETURN);
        reservedKeywords.put("super",  SUPER);
        reservedKeywords.put("this",   THIS);
        reservedKeywords.put("true",   TRUE);
        reservedKeywords.put("var",    VAR);
        reservedKeywords.put("while",  WHILE);
    }

    Scanner(String source) {
        this.source = source;
    }

    private boolean isAtEnd(int position) {
        return position >= source.length();
    }

    List<Token> scanTokens() { 
        while (!isAtEnd(current)) {
            // We are at the beginning of the next lexeme.
            start = current;
            scanToken();
        }

        tokens.add(new Token(EOF, "", null, line));
        return tokens;
    }

    private void scanToken() { 
        char c = advance();
        switch (c) {
            case '(': addToken(LEFT_PAREN); break;
            case ')': addToken(RIGHT_PAREN); break;
            case '{': addToken(LEFT_BRACE); break;
            case '}': addToken(RIGHT_BRACE); break;
            case ',': addToken(COMMA); break;
            case '.': addToken(DOT); break;
            case '-': addToken(MINUS); break;
            case '+': addToken(PLUS); break;
            case ';': addToken(SEMICOLON); break;
            case '*': addToken(STAR); break; 
            case '!':
                      addToken(match('=') ? BANG_EQUAL : BANG);
                      break;
            case '=':
                      addToken(match('=') ? EQUAL_EQUAL : EQUAL);
                      break;
            case '<': 
                      addToken(match('=') ? LESS_EQUAL : LESS);
                      break;
            case '>':
                      addToken(match('=') ? GREATER_EQUAL : GREATER);
                      break;
            case '/':
                      // comments are meaningless, so do not create token for them
                      if (match('/')) { 
                          while (peek() != '\n' && !isAtEnd(current)) {
                              advance();
                          }
                          advance();
                      } else { 
                          addToken(SLASH);
                      }
                      break;
            // java switch moment
            case ' ':
            case '\r':
            case '\t':
                      // Ignore whitespace.
                      break;
            case '\n':
                      line++;
                      break;
            case '"':
                      handleString();
                      break;
            default:
                      if (isDigit(c)) { 
                          handleNumber();
                      } else if (isAlphaNumeric(c)) { 
                          handleIdentifier();
                      } else { 
                          Lox.error(line, String.format("Unexpected character %s:", c));
                      }
                      break;
        }
    }


    private char peek() { 
        return peek(0);
    }

    // peeks `i` chars ahead of current (cursor)
    private char peek(int i) { 
        int peekPosition = current + i;
        if (isAtEnd(peekPosition)) return '\0';
        return source.charAt(peekPosition);
    }

    private char advance() {
        // note: i don't like using this bc behaviour is a bit non-obvious
        return source.charAt(current++);
    }
    
    private void addToken(TokenType type) { 
        addToken(type, null);
    }

    private void addToken(TokenType type, Object literal) { 
        String text = source.substring(start, current);
        tokens.add(new Token(type, text, literal, line));
    }

    // advances char iff it matches expected
    private boolean match(char expected) { 
        if (isAtEnd(current)) return false;
        if (source.charAt(current) != expected) return false;

        current++;
        return true;
    }

    private boolean isDigit(char c) {
        return c >= '0' && c <= '9';
    }

    private boolean isAlpha(char c) {
        return (c >= 'a' && c <= 'z') ||
            (c >= 'A' && c <= 'Z') || 
            (c == '_');
    }
    
    private boolean isAlphaNumeric(char c) {
        return isAlpha(c) || isDigit(c);
    }

    // REGION handlers

    private void handleString() { 
        // TODO handle escape sequences
        while (peek() != '"') { 
            if (isAtEnd(current)) { 
                Lox.error(line, "Unterminated String.");
                return;
            }

            char currChar = peek();
            if (currChar == NEW_LINE) { 
                line++;
            }

            advance();
        }

        advance(); // advance for the closing quote

        String stringLiteral = source.substring(start + 1, current - 1);
        addToken(STRING, stringLiteral);
    }

    // currenty supports integer and float representation 
    // ie. 9 or 9.0
    private void handleNumber() { 
        while(isDigit(peek())) advance();

        if (peek() == '.' && isDigit(peek(1)))  {
            // advance past the period before chewing thru the rest of the digits
            advance();
            while(isDigit(peek())) advance();
        }
    
        Double literalVal = Double.parseDouble(source.substring(start, current));
        addToken(NUMBER, literalVal);
    }

    private void handleIdentifier() { 
        while(isAlphaNumeric(peek())) advance();

        String lexeme = source.substring(start, current);
        TokenType token = reservedKeywords.get(lexeme);

        
        if (token == null) { 
            // not a reserved keyword
            addToken(IDENTIFIER);
        } else { 
            // reserved keyword
            addToken(token);
        }
        
    }

    // helper
    private void debug() {
        Lox.debug(String.format("Current: %s, peek; %s", current, peek()));
    }
}

