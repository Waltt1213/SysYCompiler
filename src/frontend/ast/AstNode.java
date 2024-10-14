package frontend.ast;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.ArrayList;

public interface AstNode {
    String getSymbol();

    ArrayList<AstNode> getAstChild();

    void printToFile(BufferedWriter bw) throws IOException;
}
