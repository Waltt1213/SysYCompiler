package utils;

import backend.mips.MipsData;
import backend.mips.MipsFunction;
import backend.mips.MipsInstruction;
import backend.mips.MipsModule;
import frontend.Error;
import frontend.Token;
import frontend.TokenType;
import frontend.ast.CompUnit;
import llvmir.Module;
import middle.irbuilder.SymbolTable;

import java.io.*;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Comparator;

public class FileIO {
    private static final String testFilePath = String.valueOf(Paths.get("testfile.txt"));
    private static final String lexerFilePath = String.valueOf(Paths.get("lexer.txt"));
    private static final String errorFilePath = String.valueOf(Paths.get("error.txt"));
    private static final String parserFilePath = String.valueOf(Paths.get("parser.txt"));
    private static final String symbolFilePath = String.valueOf(Paths.get("symbol.txt"));
    public static final String llvmIrFilePath = String.valueOf(Paths.get("llvm_ir.txt"));
    public static final String optimizeFilePath = String.valueOf(Paths.get("optimize_ir.txt"));
    public static final String NoOptimizeIrFilePath = String.valueOf(Paths.get("testfilei22371103王鹏_优化前中间代码.txt"));
    public static final String OptimizeIrFilePath = String.valueOf(Paths.get("testfilei22371103王鹏_优化后中间代码.txt"));
    public static final String mipsFilePath = String.valueOf(Paths.get("mips.txt"));
    public static final String NoOptMipsFilePath = String.valueOf(Paths.get("testfilei22371103王鹏_优化前目标代码.txt"));

    public static String readTestFile() throws IOException {
        FileReader fr = new FileReader(testFilePath);
        BufferedReader br = new BufferedReader(fr);
        StringBuilder sb = new StringBuilder();
        String line;
        while ((line = br.readLine()) != null) {
            sb.append(line).append("\n");
        }
        br.close();
        fr.close();
        return sb.toString();
    }

    public static void printLexerResult(ArrayList<Token> tokens) throws IOException {
        FileWriter fw = new FileWriter(lexerFilePath);
        BufferedWriter bw = new BufferedWriter(fw);
        for (Token token: tokens) {
            if (token.getType() != TokenType.ANNOTATION) {
                String line = token.toString() + "\n";
                bw.write(line);
            }
        }
        bw.close();
        fw.close();
    }

    public static void printParserResult(CompUnit compUnit) throws IOException {
        FileWriter fw = new FileWriter(parserFilePath);
        BufferedWriter bw = new BufferedWriter(fw);
        compUnit.printToFile(bw);
        bw.close();
        fw.close();
    }

    public static void printSymTableResult(ArrayList<SymbolTable> sts) throws IOException {
        FileWriter fw = new FileWriter(symbolFilePath);
        BufferedWriter bw = new BufferedWriter(fw);
        // sts.sort(Comparator.comparing(SymbolTable::getDepth));
        int id = 1;
        for (SymbolTable st : sts) {
            for (String name : st.getSymItems().keySet()) {
                bw.write(id + " " + st.getSymItems().get(name).toString() + "\n");
            }
            id++;
        }
        bw.close();
        fw.close();
    }

    public static void printError(ArrayList<Error> errors) throws IOException {
        FileWriter fw = new FileWriter(errorFilePath);
        BufferedWriter bw = new BufferedWriter(fw);
        if (errors.isEmpty()) {
            bw.write("");
            return;
        }
        errors.sort(Comparator.comparing(Error::getLineno));
        int lastLineno = 0;
        for (Error error: errors) {
            String line = error.toString() + "\n";
            if (lastLineno != error.getLineno()) {
                bw.write(line);
                lastLineno = error.getLineno();
            }
        }
        bw.close();
        fw.close();
    }

    public static void printLlvmIrResult(Module module, String path) throws IOException {
        FileWriter fw = new FileWriter(path);
        BufferedWriter bw = new BufferedWriter(fw);
        bw.write(module.toString());
        bw.close();
        fw.close();
    }

    public static void printMipsCode(ArrayList<MipsData> dataSegment,
                                     ArrayList<MipsInstruction> textSegment) throws IOException {
        FileWriter fw = new FileWriter(mipsFilePath);
        BufferedWriter bw = new BufferedWriter(fw);
        if (!dataSegment.isEmpty()) {
            bw.write(".data\n");
        }
        for (MipsData dataSeg: dataSegment) {
            bw.write(dataSeg.toString() + "\n");
        }
        if (!textSegment.isEmpty()) {
            bw.write(".text\n");
        }
        for (MipsInstruction instruction: textSegment) {
            bw.write(instruction.toString() + "\n");
        }
        bw.close();
        fw.close();
    }

    public static void printMipsCode(MipsModule module) throws IOException {
        FileWriter fw = new FileWriter(mipsFilePath);
        BufferedWriter bw = new BufferedWriter(fw);
        if (!module.getDataSegment().isEmpty()) {
            bw.write(".data\n");
        }
        for (MipsData dataSeg: module.getDataSegment()) {
            bw.write(dataSeg.toString() + "\n");
        }
        if (!module.getTextSegment().isEmpty()) {
            bw.write(".text\n");
        }
        for (MipsFunction function: module.getTextSegment()) {
            bw.write(function.getName() + ": \n");
            for (MipsInstruction instruction : function.getInstructions()) {
                bw.write(instruction.toString() + "\n");
            }
        }
        bw.close();
        fw.close();
    }

}
