/*
 * Copyright 2020 Valio Valtokari
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     <http://www.apache.org/licenses/LICENSE-2.0>
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.pretzel

import com.pretzel.core.ErrorType
import com.pretzel.core.Report
import com.pretzel.core.ast.Node
import com.pretzel.core.lexer.Lexer
import com.pretzel.core.lexer.TokenStream
import com.pretzel.core.parser.Parser

import java.io.File
import java.util.Scanner

import kotlin.system.exitProcess

import org.apache.commons.cli.Option
import org.apache.commons.cli.CommandLine
import org.apache.commons.cli.Options
import org.apache.commons.cli.DefaultParser
import org.apache.commons.cli.HelpFormatter
import org.apache.commons.cli.ParseException


class Main {
    companion object {
        @JvmStatic
        fun main(vararg args: String) {
            runRepl(*args)
        }

        @JvmStatic
        fun runRepl(vararg args: String) {
            val debug = Option("d","debug", false, "Debug mode (show times and actions done)")
            val trace = Option("t", "trace", false, "Show stack trace on errors")
            debug.isRequired = false
            trace.isRequired = false
            val options = Options()
            options.addOption(trace)
            options.addOption(debug)
            val parser = DefaultParser()
            val formatter = HelpFormatter()
            val cmd: CommandLine

            try {
                cmd = parser.parse(options, args)
            } catch (e: ParseException) {
                println(e.message)
                val progname = File(
                    Main::class.java.protectionDomain
                        .codeSource
                        .location
                        .path
                ).name

                formatter.printHelp(progname, options)
                exitProcess(1)
            }

            val isDebug = cmd.hasOption("debug")
            Report.debug = cmd.hasOption("trace")

            var l: Lexer
            var ts: TokenStream
            var p: Parser
            var input: String
            val scanner = Scanner(System.`in`)
            println("Welcome to the Pretzel REPL v0.1. Type \"// exit\" to exit.")

            while (true) {
                try {
                    print(">>> ")
                    input = scanner.nextLine()
                    if (input.trim().startsWith("//") && input.trim().replace("//", "") == "exit")
                        exitProcess(0)
                    l = Lexer(input, Lexer.SourceMode.DIRECT)
                    l.cancellationHook = { s: String, t: Lexer.Token -> Report.error(ErrorType.LEXER, s, t.toContext()) }
                    var t = System.currentTimeMillis()
                    ts = TokenStream.open(l)
                    val lextime = System.currentTimeMillis() - t
                    var parsetime = -1L
                    if (ts.length != 0) {
                        if (!l.hadError) {
                            p = Parser.fromLexer(l)
                            p.cancellationHook = { s: String, token: Lexer.Token, list: List<Node> -> Report.error(ErrorType.PARSER, s, token.toContext()) }
                            t = System.currentTimeMillis()
                            val node = p.parse()
                            parsetime = System.currentTimeMillis() - t
                            t = System.currentTimeMillis()
                        }
                    }

                    if (isDebug) {
                        val total = lextime + parsetime
                        println("""
                            Lexing time(ms):          $lextime
                            Parsing time(ms):         ${if (parsetime != -1L) parsetime else "(no parsing happened)"}
                            """.trimIndent()
                        )
                    }

                } catch (e: Parser.ParsingException) {
                    println("parsing error on token '${e.last}'")
                } catch (e: Lexer.LexingException) {
                    println("lexical error on character '${e.last}'")
                } catch (t: Throwable) {
                    println("internal error: ${t.javaClass.name}: ${t.message}")
                    t.stackTrace.forEach { println("   $it") }
                }
            }
        }
    }
}