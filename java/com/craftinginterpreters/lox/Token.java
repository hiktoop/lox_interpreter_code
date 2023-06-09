/*
 * Token:
 * - type: the type of token
 * - lexeme: the origonal string in source code
 * - literal: the real thing of this token
 * - line: the line in source code of this token
 */
package com.craftinginterpreters.lox;

class Token {
  final TokenType type;
  final String lexeme;
  final Object literal;
  final int line; // [location]

  Token(TokenType type, String lexeme, Object literal, int line) {
    this.type = type;
    this.lexeme = lexeme;
    this.literal = literal;
    this.line = line;
  }

  public String toString() {
    return type + " " + lexeme + " " + literal;
  }
}
