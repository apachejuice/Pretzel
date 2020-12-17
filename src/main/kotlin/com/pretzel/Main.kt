package com.pretzel

import java.util.Scanner


class Main {
    companion object {
        @JvmStatic
        fun main(vararg args: String) {
            var l: Lexer
            var ts: TokenStream
            var input: String
            val scanner = Scanner(System.`in`)

            while (true) {
                print(">>> ")
                input = scanner.nextLine()
                l = Lexer(input, Lexer.SourceMode.DIRECT, true)
                ts = TokenStream.open(l)
                if (ts.length != 0) println(ts)
            }
        }
    }
}
