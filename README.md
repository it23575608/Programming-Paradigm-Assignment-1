# Test Language Compiler

A simple compiler that converts test files into Java JUnit tests. This was built as a school project to learn about compilers.

## What it does

This compiler takes a custom test language and turns it into Java code that can run HTTP API tests. You write tests in a simple language, and it generates proper Java JUnit test classes.

## Requirements

- Java 17 or higher
- Maven (or use the included wrapper)

## How to run

1. **Build the project:**
   ```bash
   ./mvnw clean compile
   ```

2. **Compile a test file:**
   ```bash
   java -cp target/classes testlangcompiler.TestlangCompiler example.test GeneratedTests.java
   ```

3. **Look at the output:**
   ```bash
   type GeneratedTests.java
   ```

## Test language syntax

Write your tests like this:

```test
config {
  base_url = "http://localhost:8080";
  header "Content-Type" = "application/json";
}

let user = "admin";
let id = 42;

test Login {
  POST "/api/login" {
    body = "{ \"username\": \"$user\", \"password\": \"1234\" }";
  }
  expect status = 200;
  expect header "Content-Type" contains "json";
  expect body contains "\"token\":";
}
```

## What you can do

- Set up base URLs and headers
- Create variables
- Make HTTP requests (GET, POST, PUT, DELETE)
- Check status codes, headers, and response body

## Example files

- `example.test` - Working example
- `invalid1.test` - Shows error for bad variable name
- `invalid2.test` - Shows error for wrong body type
- `invalid3.test` - Shows error for wrong status type
- `invalid4.test` - Shows error for missing semicolon

## How to test errors

```bash
java -cp target/classes testlangcompiler.TestlangCompiler invalid1.test test1.java
java -cp target/classes testlangcompiler.TestlangCompiler invalid2.test test2.java
java -cp target/classes testlangcompiler.TestlangCompiler invalid3.test test3.java
java -cp target/classes testlangcompiler.TestlangCompiler invalid4.test test4.java
```

## If something goes wrong

- Make sure you ran `./mvnw clean compile` first
- Check that Java 17+ is installed
- Look at the error messages - they usually tell you what's wrong

## Project structure

- `src/main/java/testlangcompiler/` - Main compiler code
- `src/main/flex/lexer.flex` - Lexer rules
- `src/main/cup/parser.cup` - Parser rules
- `example.test` - Example input file
- `invalid*.test` - Files that show errors
