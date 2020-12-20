package com.pretzel

import com.pretzel.core.Report
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
                print(">>> ")
                input = scanner.nextLine()
                if (input.trim().startsWith("//") && "exit" in input)
                    exitProcess(0)
                l = Lexer(input, Lexer.SourceMode.DIRECT, true)
                ts = TokenStream.open(l)
                if (ts.length != 0) {
                    if (!l.hadError) {
                        p = Parser.fromLexer(l, true)
                        p.parse()
                    }
                }
            }
        }
    }
}
