import frontend.Lexer;
import frontend.Parser;
import table.Visitor;

import java.io.IOException;

public class Compiler {

    public static void main(String[] args) throws IOException {
        String sourceCode = FileIO.readTestFile();
        Lexer lexer = new Lexer(sourceCode);
        lexer.analyzeCode();
        FileIO.printLexerResult(lexer.getTokens());
        Parser parser = new Parser(lexer.getTokens(), lexer.getErrors());
        parser.analyzeTokens();
        FileIO.printParserResult(parser.getCompUnit());
        Visitor visitor = new Visitor(parser.getCompUnit(), parser.getErrors());
        visitor.buildSymTable();
        FileIO.printSymTableResult(visitor.getSymbolTables());
        FileIO.printError(visitor.getErrors());
    }
}
