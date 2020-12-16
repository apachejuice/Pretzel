package com.pretzel

import java.util.Scanner


class Main {
    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            val l = Lexer(Scanner(System.`in`).nextLine(), Lexer.SourceMode.DIRECT)
            val ts = TokenStream.open(l)
            ts.accept(Lexer.TokenType.IN)
            println(ts)
        }
    }
}
