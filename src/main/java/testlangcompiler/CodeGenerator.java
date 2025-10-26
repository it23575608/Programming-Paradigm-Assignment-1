package testlangcompiler;

import java.util.*;

public class CodeGenerator {
    
    public static String generate(TestlangCompiler.CompilationUnit unit) {
        return generateCode(unit);
    }
    
    public static String generate(ParserCUP.CompilationUnit unit) {
        return generateCodeCUP(unit);
    }
    
    // Substitute variables in a string (e.g., "$user" -> "admin")
    private static String substituteVariables(String text, Map<String, TestlangCompiler.Variable> vars) {
        return substituteVariablesInternal(text, vars);
    }
    
    private static String substituteVariablesCUP(String text, Map<String, ParserCUP.Variable> vars) {
        return substituteVariablesInternal(text, vars);
    }
    
    private static String substituteVariablesInternal(String text, Object vars) {
        if (text == null) return text;
        StringBuilder result = new StringBuilder();
        int i = 0;
        while (i < text.length()) {
            if (text.charAt(i) == '$' && i + 1 < text.length()) {
                int start = i + 1;
                int end = start;
                while (end < text.length() && (Character.isLetterOrDigit(text.charAt(end)) || text.charAt(end) == '_')) {
                    end++;
                }
                if (end > start) {
                    String varName = text.substring(start, end);
                    Object var = ((Map<?,?>)vars).get(varName);
                    if (var != null) {
                        Object value = getField(var, "value");
                        result.append(value != null ? value.toString() : "");
                    } else {
                        result.append(text.substring(i, end));
                    }
                    i = end;
                } else {
                    result.append('$');
                    i++;
                }
            } else {
                result.append(text.charAt(i));
                i++;
            }
        }
        return result.toString();
    }
    
    // Resolve URL - add base_url if path starts with "/"
    private static String resolveUrl(String path, TestlangCompiler.CompilationUnit unit) {
        if (path == null) return path;
        if (unit.config != null && unit.config.baseUrl != null && path.startsWith("/")) {
            return unit.config.baseUrl + path;
        }
        return path;
    }
    
    private static String resolveUrl(String path, Object unit) {
        if (path == null) return path;
        Object config = getField(unit, "config");
        if (config != null) {
            Object baseUrl = getField(config, "baseUrl");
            if (baseUrl != null && path.startsWith("/")) {
                return (String)baseUrl + path;
            }
        }
        return path;
    }
    
    // Generate the complete JUnit test class
    private static String generateCode(TestlangCompiler.CompilationUnit unit) {
        return generateCodeInternal(unit);
    }
    
    private static String generateCodeCUP(ParserCUP.CompilationUnit unit) {
        return generateCodeInternal(unit);
    }
    
    private static String generateCodeInternal(Object unit) {
        StringBuilder code = new StringBuilder();
        
        // Imports
        code.append("import org.junit.jupiter.api.*;\n");
        code.append("import static org.junit.jupiter.api.Assertions.*;\n");
        code.append("import java.net.http.*;\n");
        code.append("import java.net.*;\n");
        code.append("import java.time.Duration;\n");
        code.append("import java.nio.charset.StandardCharsets;\n");
        code.append("import java.util.*;\n\n");
        
        // Class declaration
        code.append("public class GeneratedTests {\n");
        
        // Static fields
        String baseUrl = "http://localhost:8080";
        Object config = getField(unit, "config");
        if (config != null) {
            Object baseUrlObj = getField(config, "baseUrl");
            if (baseUrlObj != null) {
                baseUrl = (String)baseUrlObj;
            }
        }
        code.append("  static String BASE = \"").append(escapeJava(baseUrl)).append("\";\n");
        code.append("  static Map<String,String> DEFAULT_HEADERS = new HashMap<>();\n");
        code.append("  static HttpClient client;\n\n");
        
        // Setup method
        code.append("  @BeforeAll\n");
        code.append("  static void setup() {\n");
        code.append("    client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();\n");
        if (config != null) {
            Object headers = getField(config, "defaultHeaders");
            if (headers instanceof Map) {
                for (Map.Entry<String, String> entry : ((Map<String, String>)headers).entrySet()) {
                    code.append("    DEFAULT_HEADERS.put(\"").append(escapeJava(entry.getKey()))
                         .append("\", \"").append(escapeJava(entry.getValue())).append("\");\n");
                }
            }
        }
        code.append("  }\n\n");
        
        // Generate test methods
        Object testMethods = getField(unit, "testMethods");
        if (testMethods instanceof List) {
            for (Object testMethod : (List<?>)testMethods) {
                generateTestMethod(code, testMethod, unit);
            }
        }
        
        code.append("}\n");
        
        return code.toString();
    }
    
    private static void generateTestMethod(StringBuilder code, Object tm, Object unit) {
        String name = (String)getField(tm, "name");
        code.append("  @Test\n");
        code.append("  void test_").append(name).append("() throws Exception {\n");
        
        // Generate each request
        Object requests = getField(tm, "requests");
        if (requests instanceof List) {
            for (Object req : (List<?>)requests) {
                generateHttpRequest(code, req, unit);
            }
        }
        
        // Generate assertions
        Object assertions = getField(tm, "assertions");
        if (assertions instanceof List) {
            int assertIndex = 0;
            for (Object ass : (List<?>)assertions) {
                generateAssertion(code, ass, assertIndex++);
            }
        }
        
        code.append("  }\n\n");
    }
    
    private static void generateHttpRequest(StringBuilder code, Object req, Object unit) {
        String path = (String)getField(req, "path");
        String method = (String)getField(req, "method");
        String url = resolveUrl(path, unit);
        String resolvedUrl = substituteVariablesInternal(url, getField(unit, "variables"));
        
        code.append("    HttpRequest.Builder b = HttpRequest.newBuilder(URI.create(\"")
             .append(escapeJava(resolvedUrl)).append("\"))\n");
        code.append("      .timeout(Duration.ofSeconds(10))");
        
        // Add method-specific code
        if (method.equals("POST") || method.equals("PUT")) {
            Object body = getField(req, "body");
            if (body != null) {
                String resolvedBody = substituteVariablesInternal((String)body, getField(unit, "variables"));
                code.append("\n      .").append(method).append("(HttpRequest.BodyPublishers.ofString(\"")
                     .append(escapeJava(resolvedBody)).append("\"))");
            } else {
                code.append("\n      .").append(method).append("(HttpRequest.BodyPublishers.ofString(\"\"))");
            }
        } else {
            code.append("\n      .").append(method).append("()");
        }
        code.append(";\n");
        
        // Add default headers
        code.append("    for (var e: DEFAULT_HEADERS.entrySet()) b.header(e.getKey(), e.getValue());\n");
        
        // Add request-specific headers
        Object headers = getField(req, "headers");
        if (headers instanceof Map && !((Map<?,?>)headers).isEmpty()) {
            for (Map.Entry<String, String> header : ((Map<String, String>)headers).entrySet()) {
                code.append("    b.header(\"").append(escapeJava(header.getKey()))
                     .append("\", \"").append(escapeJava(header.getValue())).append("\");\n");
            }
        }
        
        code.append("    HttpResponse<String> resp = client.send(b.build(), HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));\n\n");
    }
    
    private static void generateAssertion(StringBuilder code, Object ass, int index) {
        String type = (String)getField(ass, "type");
        Object expected = getField(ass, "expected");
        String expectedStr = (String)getField(ass, "expectedStr");
        
        if (type.equals("status")) {
            code.append("    assertEquals(").append(expected).append(", resp.statusCode());\n");
        } else if (type.equals("header_equals")) {
            String headerName = (String)expected;
            code.append("    assertEquals(\"").append(escapeJava(expectedStr))
                 .append("\", resp.headers().firstValue(\"").append(escapeJava(headerName))
                 .append("\").orElse(\"\"));\n");
        } else if (type.equals("header_contains")) {
            String headerName = (String)expected;
            code.append("    assertTrue(resp.headers().firstValue(\"").append(escapeJava(headerName))
                 .append("\").orElse(\"\").contains(\"").append(escapeJava(expectedStr))
                 .append("\"));\n");
        } else if (type.equals("body_contains")) {
            code.append("    assertTrue(resp.body().contains(\"")
                 .append(escapeJava(expectedStr))
                 .append("\"));\n");
        }
    }
    
    private static Object getField(Object obj, String fieldName) {
        try {
            java.lang.reflect.Field field = obj.getClass().getField(fieldName);
            return field.get(obj);
        } catch (Exception e) {
            return null;
        }
    }
    
    private static String escapeJava(String str) {
        if (str == null) return "";
        return str.replace("\\", "\\\\")
                  .replace("\"", "\\\"")
                  .replace("\n", "\\n")
                  .replace("\r", "\\r")
                  .replace("\t", "\\t");
    }
}

