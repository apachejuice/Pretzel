package com.pretzel

import com.pretzel.core.ErrorType
import com.pretzel.core.Report
import com.pretzel.core.ast.Node
import com.pretzel.core.lexer.Lexer
import com.pretzel.core.lexer.TokenStream
import com.pretzel.core.parser.Parser
import java.util.Scanner
import kotlin.system.exitProcess


class Main {
    companion object {
        @JvmStatic
        fun main(vararg args: String) {
            var l: Lexer
            var ts: TokenStream
            var p: Parser
            var input: String
            val scanner = Scanner(System.`in`)
            println("Welcome to the Pretzel REPL v0.1. Type \"// exit\" to exit.")
            Report.debug = "--debug" in args

            while (true) {
                try {
                    print(">>> ")
                    input = scanner.nextLine()
                    if (input.trim().startsWith("//") && "exit" in input)
                        exitProcess(0)
                    l = Lexer(input, Lexer.SourceMode.DIRECT)
                    l.cancellationHook = { s: String, t: Lexer.Token -> Report.error(ErrorType.LEXER, s, t) }
                    ts = TokenStream.open(l)
                    if (ts.length != 0) {
                        if (!l.hadError) {
                            p = Parser.fromLexer(l)
                            p.cancellationHook = { s: String, token: Lexer.Token, list: List<Node> -> Report.error(ErrorType.PARSER, s, token) }
                            p.parse()
                        }
                    }

                } catch (e: Parser.ParsingException) {
                    println("parsing error on token '${e.last}'")
                } catch (e: Lexer.LexingException) {
                    println("lexical error on character '${e.last}'")
                }
            }
        }
    }
}
