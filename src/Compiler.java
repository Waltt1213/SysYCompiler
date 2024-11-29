import backend.beforeopt.OldTranslator;
import frontend.Lexer;
import frontend.Parser;
import llvmir.Module;
import middle.irbuilder.Visitor;
import middle.optimizer.Optimizer;
import utils.FileIO;

import java.io.IOException;

public class Compiler {
    private static final boolean Optimize = true;

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
        Module module = visitor.getModule();
        Optimizer optimizer = new Optimizer(module);
        if (Optimize) {
            module.setVirtualName();
            FileIO.printLlvmIrResult(module, FileIO.NoOptimizeIrFilePath);
            optimizer.optimizeSSA();
        }

        // Step 6: print the LLVM IR
        module.setVirtualName();
        FileIO.printLlvmIrResult(module, FileIO.llvmIrFilePath);
        if (Optimize) {
            FileIO.printLlvmIrResult(module, FileIO.OptimizeIrFilePath);
        }

        // Step 7: generate Mips code and print the result
        if (Optimize) {
            optimizer.optimizeBackend();
            FileIO.printLlvmIrResult(module, FileIO.optimizeFilePath);
        }
        OldTranslator oldTranslator = new OldTranslator(module);
        oldTranslator.genMipsCode();
        FileIO.printMipsCode(oldTranslator.getDataSegment(), oldTranslator.getTextSegment());
    }
}
