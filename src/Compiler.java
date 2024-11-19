import backend.Translator;
import frontend.Lexer;
import frontend.Parser;
import llvmir.Module;
import middle.Visitor;
import middle.optimizer.MidOptimizer;
import utils.FileIO;

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

        // Step 4: go to Semantic Analysis and build LLVM IR
        // print the Symbol Table and errors from visitor
        Visitor visitor = new Visitor(parser.getCompUnit(), parser.getErrors());
        visitor.buildIR();
        FileIO.printSymTableResult(visitor.getSymbolTables());
        FileIO.printError(visitor.getErrors());

        // Step 5: Mid optimize
        MidOptimizer optimizer = new MidOptimizer(visitor.getModule());
        optimizer.optimize();

        // Step 6: print the LLVM IR
        Module module = optimizer.getModule();
        module.setVirtualName();
        FileIO.printLlvmIrResult(module);

        // Step 7: generate Mips code and print the result
        Translator translator = new Translator(module);
        translator.genMipsCode();
        FileIO.printMipsCode(translator.getDataSegment(), translator.getTextSegment());
    }
}
