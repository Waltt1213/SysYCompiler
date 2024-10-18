import frontend.Lexer;
import frontend.Parser;
import table.Visitor;

import java.io.IOException;

public class Compiler {

    public static void main(String[] args) throws IOException {
        // Step 1: read source code from test file
        String sourceCode = FileIO.readTestFile();

        // Step 2: go to Lexical Analysis and print the result from lexer
        Lexer lexer = new Lexer(sourceCode);
        lexer.analyzeCode();
        FileIO.printLexerResult(lexer.getTokens());

        // Step 3: go to Syntactic Analysis and print the result from parser
        Parser parser = new Parser(lexer.getTokens(), lexer.getErrors());
        parser.analyzeTokens();
        FileIO.printParserResult(parser.getCompUnit());

        // Step 4: go to Semantic Analysis and build Symbol Table
        // then print the result and errors from visitor
        Visitor visitor = new Visitor(parser.getCompUnit(), parser.getErrors());
        visitor.buildSymTable();
        FileIO.printSymTableResult(visitor.getSymbolTables());
        FileIO.printError(visitor.getErrors());
    }
}
