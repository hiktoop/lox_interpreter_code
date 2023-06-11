# java版lox解释器介绍

## 总览

java版本的lox解释器的实现依据是在抽象语法树上直接解析执行，是一种效率很低的实现，但是对于第一步来说，已经是很不错的了。模块划分很简单，就是一般的编译器的结构的简化，即**词法分析、语法分析、语义分析和最终的代码执行**，对应于代码就是`Scanner、Parser、Resolver、Interpreter`类。

当然此外还有很多的细节，首先是lox语言的语法和要实现的功能，这可以直接看作者本人的描述[Appendix I &middot; Crafting Interpreters](https://craftinginterpreters.com/appendix-i.html)

## 词法分析

简单来讲就是扫描源代码文本或者输入的代码，将每个单词的组合解析成语言的基本单位，并称之为Token。所有的token都解析完毕后传入语法分析器

```java
enum TokenType {
  // Single-character tokens.
  // (          )            {           }
  LEFT_PAREN, RIGHT_PAREN, LEFT_BRACE, RIGHT_BRACE,
  // ,    .    -      +       ;         /     *
  COMMA, DOT, MINUS, PLUS, SEMICOLON, SLASH, STAR,

  // One or two character tokens.
  BANG, BANG_EQUAL,        // ! !=
  EQUAL, EQUAL_EQUAL,      // = ==
  GREATER, GREATER_EQUAL,  // > >=
  LESS, LESS_EQUAL,        // < <=

  // Literals.
  IDENTIFIER, STRING, NUMBER,

  // Keywords.
  AND, CLASS, ELSE, FALSE, FUN, FOR, IF, NIL, OR,
  PRINT, RETURN, SUPER, THIS, TRUE, VAR, WHILE,

  EOF
}

class Token {
  final TokenType type; // 类型
  final String lexeme;  // 文本
  final Object literal; // 值
  final int line;       // 行号

  Token(TokenType type, String lexeme, Object literal, int line) {
    this.type = type;
    this.lexeme = lexeme;
    this.literal = literal;
    this.line = line;
  }
}
```

词法分析通过scanner类来完成，所做的工作相当简单：扫描每个字母并根据情况对其后若干位的字母做出判断来生成相应的token。

## 语法分析

语法分析需要定义表达式和语法的结构：

```java
 * Expr:
 * - assign: name(token), value(expr)
 * - binary: left(expr), operator(token), right(expr)
 * - call: callee(expr), paren(token), arguments(list<expr>)
 * - get: object(expr), name(token)
 * - grouping: expression(expr)
 * - literal: value(object)
 * - logical: left(expr), operator(token), right(expr)
 * - set: object(expr), name(token), value(expr)
 * - super: keyword(token), method(token)
 * - this: keyword(token)
 * - unary; operator(token), right(expr)
 * - variable: name(token)
 
 * Stmt:
 * - Block: statement(list<stmt>)
 * - Class: name(token), superclass(variable), methods(lsit<function>)
 * - Expression: expression(expr) [what meanning of this]
 * - Function: name(token), params(list<token>), body(list<stmt>)
 * - If: condition(expr), thenbranch(stmt), elsebranch(stmt)
 * - Print: expression(expr)
 * - Return: keyword(token), value(expr)
 * - Variable: name(token), initializer(expr)
 * - While: condition(expr), body(stmt)
```

采用自顶向下的递归下降的分析方法，也就是文法遵循LL(1)文法：从左到右依次遍历，并从左边开始推导，具体判别参考编译原理教材。

将语句和表达式分为几个层级：

```java
1.class/function/var/statement
  
2.forStatement/ifStatement/printStatement/returnStatement/whileStatement/Bolck/expressionStatement
  
3.or->and->equality->conpration->term->factor->unary->call->primary
```

第一层的语句是顶层解析，二层也是一些语句，第三层是最细分的表达式。解析表达式时从or开始进入函数，但是又先递归进入下层直到最后的primary,解析完primary后再回溯到上层进行解析，进而实现解析的优先级顺序。顶层和二层语句的选择与词法分析类似，通过首位的token确定。

## 语义分析

语义分析有两项任务，一是分析代码的语义是否正确，比如赋值、除零、引用错误等问题。二是将一些信息存储到之后解释代码要用到的符号表中，此处解析各变量所在的局部环境并将表达式与相应的定义层数存储到解释器的本地变量中。

scope是一个环境栈每有一个block、函数、for语句、if语句或者while语句时就将新的环境入栈，退出时及时出栈

定义一些变量以维持一个状态机：

```java
private enum FunctionType {
  NONE,        // none
  FUNCTION,    // finction
  INITIALIZER, // initializer
  METHOD       // method
}

private enum ClassType {
  NONE,    // none
  CLASS,   // class
  SUBCLASS // subclass
}
```

### 访问者模式

受访者定义相应的访问接口和接受函数，在接受函数中调用与自己对应的访问函数接口，具体的访问者类定义访问接口的实现，并在访问者类某处调用相应受访者的accept函数，传入访问者的类(this)。这样，当访问者需要增加时，只需要实现该访问者的功能并为对应受访者添加接口即可，同理，受访者增加时也只需要加入访问者实现的接口即可。

![](/home/whisper/.config/marktext/images/2023-06-11-14-24-47-image.png)

图片来源：[设计模式-访问者](https://refactoringguru.cn/design-patterns/visitor)

### 实现

因后面的解释仍然需要访问这些语句，所以使用访问者模式，在语句和表达式中定义各自的accept接口，以调用传入参数的访问操作，在Resolver和Interpreter中各自实现每个语句和表达式的访问函数。

各语句的访问的时候将各种状态进行改变，发现状态不满足条件的时候即可报错。另一个要点是变量的先声明后定义，即在一个语句的访问中，声明变量后总是先要将赋值后面的表达式分析完毕再对该变量进行定义，例如：

```java
  @Override
  public Void visitVarStmt(Stmt.Var stmt) {
    declare(stmt.name);
    if (stmt.initializer != null) {
      resolve(stmt.initializer);
    }
    // define is after initializer's resolve
    define(stmt.name);
    return null;
  }
```

值得注意的而是在这个过程中很多语句的解析是不需要处理的，比如print语句，只需要解析后面的表达式，而不用真的输出表达式。

## 代码解释

### 环境

最后一步的解释与上一步的解析在结构上大同小异，唯一的不同是变量的存储，在上一步中变量只是按照表达式记录了其所在的层级，但是此处要真正的记录变量的值，因此不能简单的使用栈，因此使用链表结构，每个基本单元具有指向外部环境的指针，再加上一个全局变量，构成程序的整个环境。

Environment类的接口：

```java
 * Environment:
 * 
 * attributes:
 * - enclosing(Environment): outside of this scope
 * - values(map<string, object>)
 * 
 * methods:
 * - init()/init(enclosing): init with/without enclosing
 * - get(token name): return name's value in this or enclosing
 * - assign(token name, object value): assign name as value in this or enclosing
 * - define(string name, object value): define map in this 
 * - ancestor(int distance): return environment with its distance
 * - getAt(int distance, string key): get the value at distance with key
 * - assignAt(int distance, string key): assign the value at distance with key
 * - toString()
```

### 类、函数与实例

```java
 * LoxCallable(interface):
 * - arity(): the number of
 * - call(interpreter itpt, list<object> args): interpreter it with args
 
 * LoxClass: LoxCallable
 * - name(string)
 * - superclass(loxclass)
 * - methods(map<string, loxfinction)
 *
 * - findMethod(string name)
 * - toString(): return name
 

 * LoxFunction: LoxCallable
 * - decclartion(function)
 * - closure(environment)
 * - isInitializer(bool)
 * 
 * - bind(LoxInstance instance): bind this function with the instance
 * - arity(): declaration's arguments' size
 
 * LoxInstance:
 * 
 * - klass
 * - fiolds(map<string, object>)
 * 
 * - LoxInstance(LoxClass klass): init this with klass
 * - get(Token name): get this klass' instance(function or something), or bind one at the first time
 * - set(Token name, Object value): set the name's value
```

LoxCallable是一个接口，定义了两个接口函数，返回参数个数和调用。LoxClass和LoxFunction实现了这个接口，LoxInstance是绑定了类的函数实例。

### 解释

解释的过程就很简单了，依次执行每条语句并存储相应的变量就行了。
