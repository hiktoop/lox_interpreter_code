/*
 * Function Callable:
 * - arity(): the number of
 * - call(interpreter itpt, list<object> args): interpreter it with args
 */
package com.craftinginterpreters.lox;

import java.util.List;

interface LoxCallable {
  int arity();
  Object call(Interpreter interpreter, List<Object> arguments);
}
