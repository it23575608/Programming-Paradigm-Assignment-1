package testlangcompiler;

import java_cup.runtime.*;
import java.util.*;
import java.io.*;

public class ParserCUP {
    
    public static class CompilationUnit {
        public ConfigData config;
        public Map<String, Variable> variables = new LinkedHashMap<>();
        public List<TestCase> testMethods = new ArrayList<>();
    }
    
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
    
    private LexerCUP lexer;
    private Symbol currentToken;
    
    public ParserCUP(LexerCUP lexer) {
        this.lexer = lexer;
        try {
            currentToken = lexer.next_token();
        } catch (IOException e) {
            throw new RuntimeException("Error reading input", e);
        }
    }
    
    public CompilationUnit parse() throws Exception {
        CompilationUnit unit = new CompilationUnit();
        
        // Parse config block (optional)
        if (currentToken.sym == sym.CONFIG) {
            unit.config = parseConfig();
        }
        
        // Parse let statements
        unit.variables = parseLetStatements();
        
        // Parse test blocks
        unit.testMethods = parseTestBlocks();
        
        return unit;
    }
    
    private ConfigData parseConfig() throws Exception {
        expect(sym.CONFIG);
        expect(sym.LBRACE);
        
        ConfigData config = new ConfigData();
        
        while (currentToken.sym != sym.RBRACE) {
            if (currentToken.sym == sym.BASE_URL) {
                expect(sym.BASE_URL);
                expect(sym.EQ);
                config.baseUrl = (String)currentToken.value;
                expect(sym.STRING);
                expect(sym.SEMICOLON);
            } else if (currentToken.sym == sym.HEADER) {
                expect(sym.HEADER);
                String key = (String)currentToken.value;
                expect(sym.STRING);
                expect(sym.EQ);
                String value = (String)currentToken.value;
                expect(sym.STRING);
                expect(sym.SEMICOLON);
                config.defaultHeaders.put(key, value);
            } else {
                throw new RuntimeException("Unexpected token in config: " + currentToken);
            }
        }
        
        expect(sym.RBRACE);
        return config;
    }
    
    private Map<String, Variable> parseLetStatements() throws Exception {
        Map<String, Variable> variables = new LinkedHashMap<>();
        
        while (currentToken.sym == sym.LET) {
            expect(sym.LET);
            String name = (String)currentToken.value;
            expect(sym.IDENTIFIER);
            expect(sym.EQ);
            
            String value;
            boolean isString;
            if (currentToken.sym == sym.STRING) {
                value = (String)currentToken.value;
                isString = true;
                expect(sym.STRING);
            } else {
                value = currentToken.value.toString();
                isString = false;
                expect(sym.NUMBER);
            }
            
            expect(sym.SEMICOLON);
            variables.put(name, new Variable(name, value, isString));
        }
        
        return variables;
    }
    
    private List<TestCase> parseTestBlocks() throws Exception {
        List<TestCase> tests = new ArrayList<>();
        
        while (currentToken.sym == sym.TEST) {
            expect(sym.TEST);
            String name = (String)currentToken.value;
            expect(sym.IDENTIFIER);
            expect(sym.LBRACE);
            
            TestCase testCase = new TestCase(name);
            
            while (currentToken.sym != sym.RBRACE) {
                if (currentToken.sym == sym.GET || currentToken.sym == sym.POST || 
                    currentToken.sym == sym.PUT || currentToken.sym == sym.DELETE) {
                    testCase.requests.add(parseHttpRequest());
                } else if (currentToken.sym == sym.EXPECT) {
                    testCase.assertions.add(parseAssertion());
                } else {
                    throw new RuntimeException("Unexpected token in test: " + currentToken);
                }
            }
            
            expect(sym.RBRACE);
            tests.add(testCase);
        }
        
        return tests;
    }
    
    private HttpRequest parseHttpRequest() throws Exception {
        String method;
        if (currentToken.sym == sym.GET) {
            method = "GET";
            expect(sym.GET);
        } else if (currentToken.sym == sym.POST) {
            method = "POST";
            expect(sym.POST);
        } else if (currentToken.sym == sym.PUT) {
            method = "PUT";
            expect(sym.PUT);
        } else if (currentToken.sym == sym.DELETE) {
            method = "DELETE";
            expect(sym.DELETE);
        } else {
            throw new RuntimeException("Expected HTTP method");
        }
        
        String path = (String)currentToken.value;
        expect(sym.STRING);
        
        HttpRequest request = new HttpRequest(method, path);
        
        if (currentToken.sym == sym.LBRACE) {
            expect(sym.LBRACE);
            
            while (currentToken.sym != sym.RBRACE) {
                if (currentToken.sym == sym.HEADER) {
                    expect(sym.HEADER);
                    String key = (String)currentToken.value;
                    expect(sym.STRING);
                    expect(sym.EQ);
                    String value = (String)currentToken.value;
                    expect(sym.STRING);
                    expect(sym.SEMICOLON);
                    request.headers.put(key, value);
                } else if (currentToken.sym == sym.BODY) {
                    expect(sym.BODY);
                    expect(sym.EQ);
                    request.body = (String)currentToken.value;
                    expect(sym.STRING);
                    expect(sym.SEMICOLON);
                } else {
                    throw new RuntimeException("Unexpected token in request block: " + currentToken);
                }
            }
            
            expect(sym.RBRACE);
        }
        
        expect(sym.SEMICOLON);
        return request;
    }
    
    private Assertion parseAssertion() throws Exception {
        expect(sym.EXPECT);
        
        if (currentToken.sym == sym.STATUS) {
            expect(sym.STATUS);
            expect(sym.EQ);
            Object expected = currentToken.value;
            expect(sym.NUMBER);
            expect(sym.SEMICOLON);
            return new Assertion("status", expected, null);
        } else if (currentToken.sym == sym.HEADER) {
            expect(sym.HEADER);
            String headerName = (String)currentToken.value;
            expect(sym.STRING);
            
            if (currentToken.sym == sym.EQ) {
                expect(sym.EQ);
                String value = (String)currentToken.value;
                expect(sym.STRING);
                expect(sym.SEMICOLON);
                return new Assertion("header_equals", headerName, value);
            } else if (currentToken.sym == sym.CONTAINS) {
                expect(sym.CONTAINS);
                String value = (String)currentToken.value;
                expect(sym.STRING);
                expect(sym.SEMICOLON);
                return new Assertion("header_contains", headerName, value);
            } else {
                throw new RuntimeException("Expected = or contains after header");
            }
        } else if (currentToken.sym == sym.BODY) {
            expect(sym.BODY);
            expect(sym.CONTAINS);
            String value = (String)currentToken.value;
            expect(sym.STRING);
            expect(sym.SEMICOLON);
            return new Assertion("body_contains", null, value);
        } else {
            throw new RuntimeException("Unexpected assertion type: " + currentToken);
        }
    }
    
    private void expect(int expectedSym) throws Exception {
        if (currentToken.sym != expectedSym) {
            throw new RuntimeException("Expected " + expectedSym + " but got " + currentToken.sym);
        }
        try {
            currentToken = lexer.next_token();
        } catch (IOException e) {
            throw new RuntimeException("Error reading next token", e);
        }
    }
}
