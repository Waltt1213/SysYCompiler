# SysYCompiler设计

## 00参考编译器介绍(PL/0 Compiler)

参考的编译器为课程组提供的`PL/0`编译器，采用`Pascal`语言书写，其编译过程主要分为词法分析、语法分析和代码生成三个步骤，此外还包含出错处理程序。

### 总体结构

* 词法分析：通过`getSym`函数从源代码中逐个字符读取，识别保留字、标识符、常量、运算符等词法单元。
* 语法分析：使用递归下降的方法，由相应的函数`block`、`statement`等实现。
* 代码生成：通过`gen`函数生成目标语言。

### 接口设计

| 名称   | 定义                           | 名称       | 定义                   | 名称      | 定义                       |
| ------ | ------------------------------ | ---------- | ---------------------- | --------- | -------------------------- |
| sym    | 存储当前读取的词法单元         | block      | 分析程序处理过程       | factor    | 因子处理                   |
| num    | 存储数值单词的值               | enter      | 新识别符号加入符号表   | condition | 条件处理                   |
| id     | 存储标识符名称                 | position   | 查找标识符在符号表位置 | interpret | 对目标代码的解释执行程序   |
| getsym | 词法分析的入口函数             | listcode   | 列出目标代码清单       | base      | 通过静态链求出数据区基地址 |
| getch  | 漏掉空格，读取一个字符         | statement  | 语句部分处理           | error     | 错误处理程序               |
| gen    | 生成目标代码，并送入目标程序区 | expression | 表达式处理             | middle     | 符号表                     |
| test   | 测试当前单词是否合法           | term       | 项处理                 | code      | 生成的指令序列             |

### 文件组织

PL/0编译器文件组织相对简单，主要包含以下几个部分：

* 主程序：编译器的入口，从源文件中读取源代码。
* 词法分析模块：`getsym`函数负责读取字符并转化为相应符号。
* 语法分析模块：通过`block`、`statement`等函数实现对`PL/0`程序的递归下降解析。
* 虚拟机代码生成：`gen`函数用于生产`PL/0`虚拟机指令，并存储在`code`数组中。
* 解释器模块：`interpret`函数负责解释和执行相应的目标代码。

## 01编译器总体设计(SysY Compiler)

本编译器为SysY语言编译器，采用`Java`语言书写，主要包括词法分析、语法分析、语义分析和中间代码生成、代码优化、生成目标代码等五个部分。

> 当前进度：语法分析

### 总体结构

* 词法分析阶段：从`testfile.txt`中读取源代码，将其字符转化为单词。
* 语法分析阶段：将分析得到的单词流根据文法解析成抽象语法树。

### 接口设计

| 名称                | 定义                                           |
| ------------------- | ---------------------------------------------- |
| sourceCode          | 以字符串形式存储的源代码                       |
| lexer               | 词法分析器                                     |
| parser              | 语法分析器                                     |
| readTestFIle()      | 从特定路径中读取源代码.txt文件 (`FileIO.java`) |
| analyzeCode()       | 词法分析阶段入口 (`Lexer.java`)                |
| analyzeTokens()     | 语法分析阶段入口(`Parser.java`)                |
| printLexerResult()  | 词法分析结果输出 (`FileIO.java`)               |
| printParserResult() | 语法分析结果输出(`FIleIO.java`)                |
| printError()        | 错误结果输出 (`FileIO.java`)                   |

### 文件组织

```c
SysYCompiler
|-- error.txt
|-- lexer.txt
|-- parser.txt
|-- src
|   |-- Compiler.java
|   |-- FileIO.java
|   |-- config.json
|   `-- frontend
|       |-- Error.java
|       |-- Lexer.java
|       |-- Parser.java
|       |-- Token.java
|       |-- TokenType.java
|       `-- ast
|           |-- AddExp.java
|           |-- AstNode.java
|           |-- Block.java
|           |-- Character.java
|           |-- CompUnit.java
|           |-- Cond.java
|           |-- ConstDecl.java
|           |-- ConstDef.java
|           |-- ConstExp.java
|           |-- ConstInitVal.java
|           |-- EqExp.java
|           |-- Exp.java
|           |-- ForStmt.java
|           |-- FuncDef.java
|           |-- FuncFParam.java
|           |-- FuncFParams.java
|           |-- FuncRParams.java
|           |-- FuncType.java
|           |-- InitVal.java
|           |-- LAndExp.java
|           |-- LOrExp.java
|           |-- LVal.java
|           |-- MainFuncDef.java
|           |-- MulExp.java
|           |-- Number.java
|           |-- PrimaryExp.java
|           |-- RelExp.java
|           |-- Stmt.java
|           |-- UnaryExp.java
|           |-- UnaryOp.java
|           |-- VarDecl.java
|           `-- VarDef.java
`-- testfile.txt
```

##  02词法分析设计

> 编码前设计

### 类别码定义(TokenType.java)

首先将统一的单词类别码以枚举类的形式存储在`TokenType.java`中。此外还自定义了一类类别码`ANNOTATION`，表示注释。

| 单词名称    | 类别码     | 单词名称 | 类别码    | 单词名称 | 类别码 | 单词名称   | 类别码     |
| ----------- | ---------- | -------- | --------- | -------- | ------ | ---------- | ---------- |
| Ident       | IDENFR     | else     | ELSETK    | void     | VOIDTK | ;          | SEMICN     |
| IntConst    | INTCON     | !        | NOT       | *        | MULT   | ,          | COMMA      |
| StringConst | STRCON     | &&       | AND       | /        | DIV    | (          | LPARENT    |
| CharConst   | CHRCON     | \|\|     | OR        | %        | MOD    | )          | RPARENT    |
| main        | MAINTK     | for      | FORTK     | <        | LSS    | [          | LBRACK     |
| const       | CONSTTK    | getint   | GETINTTK  | <=       | LEQ    | ]          | RBRACK     |
| int         | INTTK      | getchar  | GETCHARTK | >        | GRE    | {          | LBRACE     |
| char        | CHARTK     | printf   | PRINTFTK  | >=       | GEQ    | }          | RBRACE     |
| break       | BREAKTK    | return   | RETURNTK  | ==       | EQL    | annotation | ANNOTATION |
| continue    | CONTINUETK | +        | PLUS      | !=       | NEQ    |            |            |
| if          | IFTK       | -        | MINU      | =        | ASSIGN |            |            |

### 词法单元类设计(Token.java)

```java
public class Token {
    private final TokenType type;	// 类别码类型
    private final String content;	// 单词内容（以字符串形式存储）
    private final int lineno;		// 所在行号
    ......
}
```

### 词法分析器设计(Lexer.java)

```java
public class Lexer {
    private int tokenIndex;					// 遍历器当前字符下标
    private int lineno;						// 当前行号
    private final String sourceCode;		// 源代码
    private final ArrayList<Token> tokens;	// 词法解析后的token流
    private final ArrayList<Error> errors;	// 词法解析后的errors流
    ......
}
```

### 词法分析过程设计

词法分析使用的主要成员及方法：

* `sourceCode`编译的源代码
* `tokenIndex`当前源代码中字符下标，初始为0
* `lineno`当前字符下标所属行号，初始为1

* `analyzeCode()`词法分析器执行词法分析过程的入口
* `next()`获取下一个`Token`
* `getTokens()`获得词法解析的`Token`流
* `getErrors()`获得词法解析出现的错误集

词法解析过程中先将源代码以字符串形式存储，`analyzecode`方法循环调用`next`方法，不断移动字符下标直到字符串结尾，移动过程中跳过空白符（`’  ','\n','\t'`），遇到换行符则增加`lineno`。将每次`next`方法返回的词法单元保存至`tokens`中。

针对于标识符和保留字的区分，我的设计中先构建保留词表，然后将标识符和保留字看成同一类词法单元进行解析，得到相应的字符串后，去保留词表进行匹配，若能匹配上则证明该单词属于保留字，否则为标识符。

```java
private final HashMap<String, TokenType> keywords = new HashMap<String, TokenType>() {{
    put("main", TokenType.MAINTK);
    put("const", TokenType.CONSTTK);
    put("int", TokenType.INTTK);
    put("char", TokenType.CHARTK);
    put("break", TokenType.BREAKTK);
    put("continue", TokenType.CONTINUETK);
    put("if", TokenType.IFTK);
    put("else", TokenType.ELSETK);
    put("for", TokenType.FORTK);
    put("getint", TokenType.GETINTTK);
    put("getchar", TokenType.GETCHARTK);
    put("printf", TokenType.PRINTFTK);
    put("return", TokenType.RETURNTK);
    put("void", TokenType.VOIDTK);
}};
public Token next() {
    StringBuilder sb = new StringBuilder();
    ......
    // 标识符 or 保留字
    if (isIdentNonDigit()) {
        while (isIdentNonDigit() || isDigit()) {
            sb.append(sourceCode.charAt(tokenIndex));
            tokenIndex++
        }
        if (keywords.containsKey(sb.toString())) {
            return new Token(keywords.get(sb.toString()), sb.toString(), lineno);
        }
        return new Token(TokenType.IDENFR, sb.toString(), lineno);
    }
 	......
}
```

## 03语法分析设计

> 编码前设计

### 抽象语法树设计(ast软件包)

首先设计一个接口类`AstNode`，其包含的方法只有两个：

```java
public interface AstNode {
    String getSymbol();	// 返回非终结符名称
    void printToFile(BufferedWriter bw) throws IOException;	// 将语法内容输出到文件中
}
```

再将除了`<BlockItem>`，`<Decl>`，`<BType>`之外的非终结符构建相应名称的类并实现`AstNode`这一接口。构建的思路主要有以下4种：

* 一是非终结符推导的规则唯一且非递归规则时，如Exp → AddExp，直接使`Exp`的成员变量设置为一个`AddExp`类型的字段。

  ```java
  public class Exp implements AstNode {
      private final AddExp addExp;
      public Exp(AddExp addExp) { this.addExp = addExp; }
      @Override
      public String getSymbol() { return "<Exp>"; }
      @Override
      public void printToFile(BufferedWriter bw) throws IOException {
          addExp.printToFile(bw);
          bw.write(getSymbol() + "\n");
      }
  }
  ```

* 二是非终结符推导规则唯一但含有递归规则时，首先消除左递归，例如AddExp → MulExp | AddExp ('+' | '−') MulExp，消除左递归得到AddExp → MulExp { ('+' | '−') MulExp } ，因此可以将`AddExp`成员变量看作是一个装有`MulExp`类型的容器和一个装有符号`Token`的容器。**此时要注意输出时需要按原文法规则输出**。

  ```java
  public class AddExp implements AstNode {
      private final ArrayList<MulExp> mulExps;
      private final ArrayList<Token> ops;
      public AddExp(ArrayList<MulExp> mulExps, ArrayList<Token> ops) {
          this.mulExps = mulExps;
          this.ops = ops;
      }
      @Override
      public String getSymbol() { return "<AddExp>"; }
      @Override
      public void printToFile(BufferedWriter bw) throws IOException {
          for (int i = 0; i < mulExps.size(); i++) {
              mulExps.get(i).printToFile(bw);
              bw.write(getSymbol() + "\n");
              if (i < ops.size()) { bw.write(ops.get(i) + "\n"); }
          }
      }
  }
  ```

* 三是推导规则包含多种情况的，例如用`|`连接的：PrimaryExp → '(' Exp ')' | LVal | Number | Character。此时通常会选择用一个`AstNode`类来作为成员变量，输出时再根据其实际类型修改输出格式。

  ```java
  public class PrimaryExp implements AstNode {
      private final AstNode primaryExp;
      public PrimaryExp(Exp exp) { primaryExp = exp; }
      public PrimaryExp(LVal lVal) { primaryExp = lVal; }
      public PrimaryExp(Number number) { primaryExp = number; }
      public PrimaryExp(Character character) {
          primaryExp = character;
      }
      @Override
      public String getSymbol() { return "<PrimaryExp>"; }
      @Override
      public void printToFile(BufferedWriter bw) throws IOException {
          if (primaryExp instanceof Exp) {
              bw.write(TokenType.LPARENT + " (\n");
              primaryExp.printToFile(bw);
              bw.write(TokenType.RPARENT + " )\n");
          } else if (primaryExp instanceof LVal) {
              primaryExp.printToFile(bw);
          } else if (primaryExp instanceof Number) {
              primaryExp.printToFile(bw);
          } else if (primaryExp instanceof Character) {
              primaryExp.printToFile(bw);
          }
          bw.write(getSymbol() + "\n");
      }
  }
  ```

* 四是含有`[]`或`{}`的推导规则，如： LVal → Ident ['[' Exp ']']。我选择设计多个构造函数，来对应不同的情况，如果不含有对应成员则设为`null`。

  ```java
  public class LVal implements AstNode {
      private final Token ident;
      private final Exp exp;
      public LVal(Token ident, Exp exp) { // 对应LVal → Ident
          this.exp = exp;
          this.ident = ident;
      }
      public LVal(Token ident) {			// 对应 LVal → Ident '[' Exp ']'
          this.exp = null;
          this.ident = ident;
      }
      @Override
      public String getSymbol() { return "<LVal>"; }
      @Override
      public void printToFile(BufferedWriter bw) throws IOException {
          bw.write(ident + "\n");
          if (exp != null) {
              bw.write(TokenType.LBRACK + " [\n");
              exp.printToFile(bw);
              bw.write(TokenType.RBRACK + " ]\n");
          }
          bw.write(getSymbol() + "\n");
      }
  }
  ```

  又比如FuncRParams → Exp { ',' Exp }，则直接将其成员变量设置为一组容器。

  ```java
  public class FuncFParams implements AstNode {
      private final ArrayList<FuncFParam> funcFParams;
      public FuncFParams(ArrayList<FuncFParam> funcFParams) {
          this.funcFParams = funcFParams; 
      }
      @Override
      public String getSymbol() { return "<FuncFParams>"; }
      @Override
      public void printToFile(BufferedWriter bw) throws IOException {
          funcFParams.get(0).printToFile(bw);
          for (int i = 1; i < funcFParams.size(); i++) {
              bw.write(TokenType.COMMA + " ,\n");
              funcFParams.get(i).printToFile(bw);
          }
          bw.write(getSymbol() + "\n");
      }
  }
  ```

实际上这个语法树仍然不够简洁，例如`Exp`，`Cond`等完全可以省去，但考虑其在特定情境下的语义还是有区别，因此并没有做化简。等待尝试完语义分析后再考虑化简抽象语法树。

### 语法分析器设计(Parser.java)

```java
public class Parser {
    private final ArrayList<Token> lexerCode;	// 词法解析得到的单词流
    private final ArrayList<Error> errors;		// 词法分析和语法分析出现的错误
    private int lexerIndex;						// 单词流指针
    private CompUnit compUnit;					// 语法分析解析得到的抽象语法树顶
}
```

### 递归下降法分析语义

主要方法：

* `analyzeTokens()`语法分析入口
* `sym()`获取当前指针指向的单词
* `nextSym()`移动指针
* `getCompUnit()`获取语法分析得到的抽象语法树
* `parse*()`递归下降解析的方法，其中*表示各种非终结字符

特殊的分析方法如下：

1. 消除左递归：

   ```c
   乘除模表达式 MulExp → UnaryExp | MulExp ('*' | '/' | '%') UnaryExp 
   改写：MulExp → UnaryExp {('*' | '/' | '%') UnaryExp}
   加减表达式 AddExp → MulExp | AddExp ('+' | '−') MulExp
   改写：AddExp → MulExp {('+' | '−') MulExp}
   关系表达式 RelExp → AddExp | RelExp ('<' | '>' | '<=' | '>=') AddExp
   改写：RelExp → AddExp {('<' | '>' | '<=' | '>=') AddExp}
   相等性表达式 EqExp → RelExp | EqExp ('==' | '!=') RelExp
   改写：EqExp → RelExp {('==' | '!=') RelExp}
   逻辑与表达式 LAndExp → EqExp | LAndExp '&&' EqExp 
   改写：LAndExp → EqExp {'&&' EqExp} 
   逻辑或表达式 LOrExp → LAndExp | LOrExp '||' LAndExp
   改写：LOrExp → LAndExp {'||' LAndExp}
   ```

2. 超前看：

   可以采用超前看的策略的规则如下：

   CompUnit → {Decl} {FuncDef} MainFuncDef，可以抢先看`sym(2)`是否为`main`，来判断是否含`Decl`或`FuncDef`，再根据是否含`(`判断是否含`FuncDef`。

   UnaryExp → PrimaryExp | Ident '(' [FuncRParams] ')' | UnaryOp UnaryExp，通过超前看`sym(2)`是否为左括号`(`来判断是否为UnaryExp → Ident '(' [FuncRParams] ')' 。

3. 回溯：

   例如`Stmt`判断'return' [Exp] ';' 后是否包含`Exp`时，由于考虑`i`型错误的存在不能通过判断下一个字符是否为`;`来判断是否含`Exp`，因此可以采用先解析`Exp`，如果解析成功则继续解析，如果失败则回溯。

   又例如区分`Stmt`开头符号是`LVal = `还是`Exp`时也是采取了先解析`LVal`，如果解析完下一个单词不是`=`则回溯。

   自定义一个枚举字段`myError`，用于判断是否需要回溯。例如`LVal`解析时如果第一个单词不是`IDENFR`则抛出`Error`类型异常。

## 04错误处理设计

> 编码前设计

### 错误类型码定义(Error.java)

```java
public class Error {
    private final ErrorType type;	// 错误类型码
    private final int lineno;		// 错误行号

    public enum ErrorType {
        a, b, c, d, e, f, g, h, i, j, k, l, m, myError;
    }
}
```

### 错误发现过程

* 词法分析阶段：a类错误，出现单独的`&`和`|`两个符号。

* 语法分析阶段：i类错误，缺少分号；

  ​						   j类错误，缺少右小括号；

  ​						   k类错误，缺少右中括号。

## 05代码生成设计

## 06代码优化设计

