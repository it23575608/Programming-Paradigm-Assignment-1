package testlangcompiler;

import java.io.*;
import java_cup.runtime.*;
import java.util.*;

public class LexerCUP implements java_cup.runtime.Scanner {
    private Reader reader;
    private int currentChar;
    private int line = 1;
    private int column = 1;
    
    public LexerCUP(Reader reader) {
        this.reader = reader;
        try {
            currentChar = reader.read();
        } catch (IOException e) {
            currentChar = -1;
        }
    }
    
    public Symbol next_token() throws IOException {
        while (currentChar != -1) {
            if (Character.isWhitespace(currentChar)) {
                if (currentChar == '\n') {
                    line++;
                    column = 1;
                } else {
                    column++;
                }
                currentChar = reader.read();
                continue;
            }
            
            if (currentChar == '/' && peek() == '/') {
                // Skip comment
                while (currentChar != -1 && currentChar != '\n') {
                    currentChar = reader.read();
                }
                continue;
            }
            
            // Keywords
            if (Character.isLetter(currentChar)) {
                StringBuilder word = new StringBuilder();
                while (Character.isLetterOrDigit(currentChar) || currentChar == '_') {
                    word.append((char) currentChar);
                    currentChar = reader.read();
                    column++;
                }
                
                String keyword = word.toString();
                switch (keyword) {
                    case "config": return new Symbol(sym.CONFIG, line, column, keyword);
                    case "base_url": return new Symbol(sym.BASE_URL, line, column, keyword);
                    case "header": return new Symbol(sym.HEADER, line, column, keyword);
                    case "let": return new Symbol(sym.LET, line, column, keyword);
                    case "test": return new Symbol(sym.TEST, line, column, keyword);
                    case "GET": return new Symbol(sym.GET, line, column, keyword);
                    case "POST": return new Symbol(sym.POST, line, column, keyword);
                    case "PUT": return new Symbol(sym.PUT, line, column, keyword);
                    case "DELETE": return new Symbol(sym.DELETE, line, column, keyword);
                    case "expect": return new Symbol(sym.EXPECT, line, column, keyword);
                    case "status": return new Symbol(sym.STATUS, line, column, keyword);
                    case "body": return new Symbol(sym.BODY, line, column, keyword);
                    case "contains": return new Symbol(sym.CONTAINS, line, column, keyword);
                    default: return new Symbol(sym.IDENTIFIER, line, column, keyword);
                }
            }
            
            // Numbers
            if (Character.isDigit(currentChar)) {
                StringBuilder number = new StringBuilder();
                while (Character.isDigit(currentChar)) {
                    number.append((char) currentChar);
                    currentChar = reader.read();
                    column++;
                }
                return new Symbol(sym.NUMBER, line, column, Integer.parseInt(number.toString()));
            }
            
            // Strings
            if (currentChar == '"') {
                StringBuilder str = new StringBuilder();
                currentChar = reader.read();
                column++;
                while (currentChar != -1 && currentChar != '"') {
                    if (currentChar == '\\') {
                        currentChar = reader.read();
                        column++;
                        if (currentChar == 'n') str.append('\n');
                        else if (currentChar == 't') str.append('\t');
                        else if (currentChar == 'r') str.append('\r');
                        else if (currentChar == '\\') str.append('\\');
                        else if (currentChar == '"') str.append('"');
                        else str.append((char) currentChar);
                    } else {
                        str.append((char) currentChar);
                    }
                    currentChar = reader.read();
                    column++;
                }
                if (currentChar == '"') {
                    currentChar = reader.read();
                    column++;
                }
                return new Symbol(sym.STRING, line, column, str.toString());
            }
            
            // Symbols
            switch (currentChar) {
                case '{': 
                    currentChar = reader.read();
                    column++;
                    return new Symbol(sym.LBRACE, line, column-1, "{");
                case '}': 
                    currentChar = reader.read();
                    column++;
                    return new Symbol(sym.RBRACE, line, column-1, "}");
                case '(': 
                    currentChar = reader.read();
                    column++;
                    return new Symbol(sym.LPAREN, line, column-1, "(");
                case ')': 
                    currentChar = reader.read();
                    column++;
                    return new Symbol(sym.RPAREN, line, column-1, ")");
                case '=': 
                    currentChar = reader.read();
                    column++;
                    return new Symbol(sym.EQ, line, column-1, "=");
                case ';': 
                    currentChar = reader.read();
                    column++;
                    return new Symbol(sym.SEMICOLON, line, column-1, ";");
                case '.': 
                    if (peek() == '.') {
                        currentChar = reader.read();
                        currentChar = reader.read();
                        column += 2;
                        return new Symbol(sym.DOTDOT, line, column-2, "..");
                    } else {
                        currentChar = reader.read();
                        column++;
                        return new Symbol(sym.DOT, line, column-1, ".");
                    }
                default:
                    System.err.println("Unknown character: " + (char)currentChar + " at line " + line + ", column " + column);
                    currentChar = reader.read();
                    column++;
            }
        }
        return new Symbol(sym.EOF, line, column);
    }
    
    private int peek() throws IOException {
        reader.mark(1);
        int next = reader.read();
        reader.reset();
        return next;
    }
}
