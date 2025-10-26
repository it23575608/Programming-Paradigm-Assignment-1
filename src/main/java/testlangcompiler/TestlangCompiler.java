package testlangcompiler;

import java.io.*;
import java.util.*;

public class TestlangCompiler {
    
    // Data structures
    public static class ConfigData {
        public String baseUrl;
        public Map<String, String> defaultHeaders = new LinkedHashMap<>();
    }
    
    public static class Variable {
        public String name;
        public String value;
        public boolean isString;
        
        public Variable(String n, String v, boolean isStr) {
            name = n;
            value = v;
            isString = isStr;
        }
    }
    
    public static class HttpRequest {
        public String method;
        public String path;
        public String body;
        public Map<String, String> headers = new LinkedHashMap<>();
        
        public HttpRequest(String m, String p) {
            method = m;
            path = p;
        }
    }
    
    public static class Assertion {
        public String type;
        public Object expected;
        public String expectedStr;
        
        public Assertion(String t, Object exp, String expStr) {
            type = t;
            expected = exp;
            expectedStr = expStr;
        }
    }
    
    public static class TestCase {
        public String name;
        public List<HttpRequest> requests = new ArrayList<>();
        public List<Assertion> assertions = new ArrayList<>();
        
        public TestCase(String n) {
            name = n;
        }
    }
    
    public static class CompilationUnit {
        public ConfigData config;
        public Map<String, Variable> variables = new LinkedHashMap<>();
        public List<TestCase> testMethods = new ArrayList<>();
    }
    
    // Simple tokenizer
    public static class Token {
        public String type;
        public String value;
        public int line;
        
        public Token(String t, String v, int l) {
            type = t;
            value = v;
            line = l;
        }
    }
    
    public static List<Token> tokenize(String input) throws Exception {
        List<Token> tokens = new ArrayList<>();
        String[] lines = input.split("\n");
        
        for (int i = 0; i < lines.length; i++) {
            String line = lines[i].trim();
            if (line.isEmpty() || line.startsWith("//")) continue;
            
            // Simple tokenizer
            int pos = 0;
            while (pos < line.length()) {
                char c = line.charAt(pos);
                
                if (Character.isWhitespace(c)) {
                    pos++;
                    continue;
                }
                
                if (c == '"') {
                    // Read string
                    int start = pos;
                    pos++;
                    while (pos < line.length() && (line.charAt(pos) != '"' || (pos > 0 && line.charAt(pos-1) == '\\'))) {
                        pos++;
                    }
                    if (pos < line.length()) pos++;
                    tokens.add(new Token("STRING", line.substring(start, pos), i + 1));
                } else if (isSymbol(c)) {
                    tokens.add(new Token("" + c, "" + c, i + 1));
                    pos++;
                } else {
                    // Read word
                    int start = pos;
                    while (pos < line.length() && !Character.isWhitespace(line.charAt(pos)) && !isSymbol(line.charAt(pos)) && line.charAt(pos) != '"') {
                        pos++;
                    }
                    String word = line.substring(start, pos);
                    if (!word.isEmpty()) {
                        if (isKeyword(word)) {
                            tokens.add(new Token(word.toUpperCase(), word, i + 1));
                        } else if (Character.isDigit(word.charAt(0))) {
                            tokens.add(new Token("NUMBER", word, i + 1));
                        } else {
                            tokens.add(new Token("IDENTIFIER", word, i + 1));
                        }
                    }
                }
            }
        }
        
        return tokens;
    }
    
    private static boolean isSymbol(char c) {
        return c == '{' || c == '}' || c == '(' || c == ')' || c == ';' || c == '=' || c == '.';
    }
    
    private static boolean isKeyword(String text) {
        String[] keywords = {"config", "base_url", "header", "let", "test", "GET", "POST", "PUT", "DELETE",
                             "expect", "status", "body", "contains"};
        for (String kw : keywords) {
            if (kw.equalsIgnoreCase(text)) return true;
        }
        return false;
    }
    
    // Parser
    private List<Token> tokens;
    private int pos = 0;
    
    public CompilationUnit parse(String input) throws Exception {
        tokens = tokenize(input);
        pos = 0;
        
        CompilationUnit unit = new CompilationUnit();
        
        // Parse config (optional)
        if (peek("CONFIG")) {
            unit.config = parseConfig();
        }
        
        // Parse let statements
        while (peek("LET")) {
            parseLet(unit);
        }
        
        // Parse test methods
        while (pos < tokens.size()) {
            if (peek("TEST")) {
                unit.testMethods.add(parseTest());
            } else {
                next();
            }
        }
        
        return unit;
    }
    
    private Token current() {
        return pos < tokens.size() ? tokens.get(pos) : null;
    }
    
    private Token next() {
        if (pos < tokens.size()) {
            return tokens.get(pos++);
        }
        return null;
    }
    
    private Token peek() {
        if (pos < tokens.size()) {
            return tokens.get(pos);
        }
        return null;
    }
    
    private boolean peek(String type) {
        Token t = peek();
        return t != null && t.type.equals(type);
    }
    
    private void expect(String type) throws Exception {
        Token t = next();
        if (t == null || !t.type.equals(type)) {
            throw new Exception("Expected " + type + " but got " + (t == null ? "EOF" : t.type));
        }
    }
    
    private ConfigData parseConfig() throws Exception {
        expect("CONFIG");
        expect("{");
        
        ConfigData config = new ConfigData();
        
        while (!peek("}")) {
            if (peek("BASE_URL")) {
                expect("BASE_URL");
                expect("=");
                Token t = next();
                if (t == null || !t.type.equals("STRING")) {
                    throw new Exception("Expected STRING after base_url =");
                }
                config.baseUrl = unquote(t.value);
                expect(";");
            } else if (peek("HEADER")) {
                expect("HEADER");
                Token key = next();
                if (key == null || !key.type.equals("STRING")) {
                    throw new Exception("Expected STRING after header");
                }
                expect("=");
                Token value = next();
                if (value == null || !value.type.equals("STRING")) {
                    throw new Exception("Expected STRING after =");
                }
                config.defaultHeaders.put(unquote(key.value), unquote(value.value));
                expect(";");
            } else {
                next();
            }
        }
        
        expect("}");
        return config;
    }
    
    private void parseLet(CompilationUnit unit) throws Exception {
        expect("LET");
        Token name = next();
        if (name == null || !name.type.equals("IDENTIFIER")) {
            throw new Exception("Expected IDENTIFIER after let");
        }
        expect("=");
        Token value = next();
        if (value == null || (!value.type.equals("STRING") && !value.type.equals("NUMBER"))) {
            throw new Exception("Expected STRING or NUMBER after =");
        }
        unit.variables.put(name.value, new Variable(name.value, 
            value.type.equals("STRING") ? unquote(value.value) : value.value,
            value.type.equals("STRING")));
        expect(";");
    }
    
    private TestCase parseTest() throws Exception {
        expect("TEST");
        Token name = next();
        if (name == null || !name.type.equals("IDENTIFIER")) {
            throw new Exception("Expected IDENTIFIER after test");
        }
        expect("{");
        
        TestCase tm = new TestCase(name.value);
        
        while (!peek("}")) {
            if (peek("GET") || peek("POST") || peek("PUT") || peek("DELETE")) {
                tm.requests.add(parseHttpRequest());
            } else if (peek("EXPECT")) {
                tm.assertions.add(parseAssertion());
            } else {
                next();
            }
        }
        
        expect("}");
        return tm;
    }
    
    private HttpRequest parseHttpRequest() throws Exception {
        Token methodTok = next();
        String method = methodTok.type;
        
        Token path = next();
        if (path == null || !path.type.equals("STRING")) {
            throw new Exception("Expected STRING for URL path");
        }
        
        HttpRequest req = new HttpRequest(method, unquote(path.value));
        
        if (peek("{")) {
            expect("{");
            while (!peek("}")) {
                if (peek("HEADER")) {
                    expect("HEADER");
                    Token key = next();
                    if (key.type.equals("STRING")) {
                        expect("=");
                        Token value = next();
                        if (value.type.equals("STRING")) {
                            req.headers.put(unquote(key.value), unquote(value.value));
                        }
                    }
                    expect(";");
                } else if (peek("BODY")) {
                    expect("BODY");
                    expect("=");
                    Token value = next();
                    if (!value.type.equals("STRING")) {
                        throw new Exception("Expected STRING for body, got " + value.type);
                    }
                    req.body = unquote(value.value);
                    expect(";");
                } else {
                    next();
                }
            }
            expect("}");
            
            // Only expect semicolon if there's one (for GET/DELETE without block, semicolon is mandatory)
            if (!peek("EXPECT") && !peek("}") && peek() != null && !peek().value.equals("}")) {
                expect(";");
            }
        } else {
            // GET/DELETE must have semicolon
            expect(";");
        }
        
        return req;
    }
    
    private Assertion parseAssertion() throws Exception {
        expect("EXPECT");
        Token type = next();
        
        if (type.type.equals("STATUS")) {
            expect("=");
            Token num = next();
            if (!num.type.equals("NUMBER")) {
                throw new Exception("Expected NUMBER for status, got " + num.type);
            }
            expect(";");
            return new Assertion("status", Integer.parseInt(num.value), null);
        } else if (type.type.equals("HEADER")) {
            Token headerName = next();
            if (headerName.type.equals("STRING")) {
                if (peek("=")) {
                    expect("=");
                    Token value = next();
                    if (value.type.equals("STRING")) {
                        expect(";");
                        return new Assertion("header_equals", unquote(headerName.value), unquote(value.value));
                    }
                    throw new Exception("Expected STRING after =");
                } else if (peek("CONTAINS")) {
                    expect("CONTAINS");
                    Token value = next();
                    if (value.type.equals("STRING")) {
                        expect(";");
                        return new Assertion("header_contains", unquote(headerName.value), unquote(value.value));
                    }
                    throw new Exception("Expected STRING after contains");
                }
            }
            throw new Exception("Expected STRING after header");
        } else if (type.type.equals("BODY")) {
            if (peek("CONTAINS")) {
                expect("CONTAINS");
                Token value = next();
                if (value.type.equals("STRING")) {
                    expect(";");
                    return new Assertion("body_contains", null, unquote(value.value));
                }
                throw new Exception("Expected STRING after contains");
            }
            throw new Exception("Expected contains after body");
        }
        
        throw new Exception("Expected status, header, or body");
    }
    
    private String unquote(String s) {
        if (s.startsWith("\"") && s.endsWith("\"")) {
            s = s.substring(1, s.length() - 1);
        }
        return s.replace("\\\"", "\"").replace("\\\\", "\\");
    }
    
    public static void main(String[] args) {
        if (args.length != 2) {
            System.err.println("Usage: java TestlangCompiler <input.test> <output.java>");
            System.exit(1);
        }
        
        try {
            // Read input
            String input = new String(java.nio.file.Files.readAllBytes(
                java.nio.file.Paths.get(args[0])));
            
            // Parse
            TestlangCompiler compiler = new TestlangCompiler();
            CompilationUnit unit = compiler.parse(input);
            
            // Generate code
            String code = CodeGenerator.generate(unit);
            
            // Write output
            java.nio.file.Files.write(java.nio.file.Paths.get(args[1]), code.getBytes());
            
            System.out.println("Generated test code: " + args[1]);
            
        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }
}
