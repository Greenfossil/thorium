package com.greenfossil.webserver

import scala.quoted.Quotes

trait MacroSupport(debug: Boolean) {

  def findEnclosingTerm(using quotes: Quotes)(sym: quotes.reflect.Symbol): quotes.reflect.Symbol =
    import quotes.reflect.*
    sym match
      case sym if sym.flags is Flags.Macro => findEnclosingTerm(sym.owner)
      case sym if !sym.isTerm              => findEnclosingTerm(sym.owner)
      case _                               => sym

  def showStructure(using quotes:Quotes)(msg: String, x: quotes.reflect.Tree | List[quotes.reflect.Tree]): Unit =
    import quotes.reflect.*
    if debug
    then
      x match
        case xs: List[Tree]  =>
          println(s"$msg: ${xs.map(_.show(using Printer.TreeStructure))}")

        case term: Tree =>
          println(s"$msg: ${term.show(using Printer.TreeStructure)}")

    else ()

  def showCode(using quotes:Quotes)(msg: String, x: quotes.reflect.Tree | List[quotes.reflect.Tree] ): Unit =
    import quotes.reflect.*
    if debug
    then
      x match
        case xs: List[Tree]  =>
          println(s"$msg: ${xs.map(_.show(using quotes.reflect.Printer.TreeAnsiCode))}")

        case term: Tree =>
          println(s"$msg: ${term.show(using quotes.reflect.Printer.TreeAnsiCode)}")

    else ()

  def show(using quotes:Quotes)(msg: String, x: quotes.reflect.Tree | List[quotes.reflect.Tree]): Unit =
    import quotes.reflect.*
    if debug
    then
      x match
        case xs: List[Tree]  =>
          println(s"===> [List] ${msg}")
          println(s"Code - Size:${xs.size}")
          println("  " + xs.map(_.show(using quotes.reflect.Printer.TreeAnsiCode)))

          println(s"Structure - Size:${xs.size}")
          println("  " + xs.map(_.show(using Printer.TreeStructure)))

        case term: Tree =>
          println(s"===> [Tree] ${msg}")
          println(s"Symbol: ${term.symbol.flags.show}")
          println(s"Code: ${term.show(using quotes.reflect.Printer.TreeAnsiCode)}")
          println(s"Struct: ${term.show(using Printer.TreeStructure)}")

    else ()

}
