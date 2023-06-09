//> Scanning token-type(39)
package com.craftinginterpreters.lox;

enum TokenType {
  // Single-character tokens(11)
  LEFT_PAREN, RIGHT_PAREN, LEFT_BRACE, RIGHT_BRACE, // ( ) { }
  COMMA, DOT, MINUS, PLUS, SEMICOLON, SLASH, STAR,  // , . - + ; / *

  // One or two character tokens(8)
  BANG, BANG_EQUAL,         // ! !=
  EQUAL, EQUAL_EQUAL,       // = ==
  GREATER, GREATER_EQUAL,   // > >=
  LESS, LESS_EQUAL,         // < <=

  // Literals(3)
  IDENTIFIER, STRING, NUMBER, // identifer string number

  // Keywords(16)
  AND, CLASS, ELSE, FALSE, FUN, FOR, IF, NIL, OR, // and class else false fun for if nil or
  PRINT, RETURN, SUPER, THIS, TRUE, VAR, WHILE,   // print return super this true var while

  EOF // eof
}
