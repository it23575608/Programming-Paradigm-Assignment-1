package testlangcompiler;

import java.io.*;
import java_cup.runtime.*;

public class TestlangCompilerCUP {
    
    public static void main(String[] args) {
        if (args.length != 2) {
            System.err.println("Usage: java TestlangCompilerCUP <input.test> <output.java>");
            System.exit(1);
        }
        
        try {
            // Read input
            String input = new String(java.nio.file.Files.readAllBytes(
                java.nio.file.Paths.get(args[0])));
            
            // Create lexer
            LexerCUP lexer = new LexerCUP(new java.io.StringReader(input));
            
            // Create parser
            ParserCUP parser = new ParserCUP(lexer);
            
            // Parse
            ParserCUP.CompilationUnit unit = parser.parse();
            
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
