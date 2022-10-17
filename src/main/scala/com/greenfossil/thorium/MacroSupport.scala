package com.greenfossil.thorium

import scala.quoted.Quotes

trait MacroSupport(globalDebug: Boolean):

  def findEnclosingTerm(using quotes: Quotes)(sym: quotes.reflect.Symbol): quotes.reflect.Symbol =
    import quotes.reflect.*
    sym match
      case sym if sym.flags is Flags.Macro => findEnclosingTerm(sym.owner)
      case sym if !sym.isTerm              => findEnclosingTerm(sym.owner)
      case _                               => sym

  def showStructure(using quotes:Quotes)(msg: String, x: quotes.reflect.Tree | List[quotes.reflect.Tree], debug: Boolean = globalDebug): Unit =
    import quotes.reflect.*
    if debug
    then
      x match
        case xs: List[Tree @unchecked]  =>
          println(s"$msg: ${xs.map(_.show(using Printer.TreeStructure))}")

        case term: Tree @unchecked =>
          println(s"$msg: ${term.show(using Printer.TreeStructure)}")

  def showCode(using quotes:Quotes)(msg: String, x: quotes.reflect.Tree | List[quotes.reflect.Tree], debug: Boolean = globalDebug): Unit =
    import quotes.reflect.*
    if debug
    then
      x match
        case xs: List[Tree @unchecked]  =>
          println(s"$msg: ${xs.map(_.show(using quotes.reflect.Printer.TreeAnsiCode))}")

        case term: Tree @unchecked =>
          println(s"$msg: ${term.show(using quotes.reflect.Printer.TreeAnsiCode)}")

  def show(using quotes:Quotes)(msg: String, x: quotes.reflect.Tree | List[quotes.reflect.Tree], debug: Boolean = globalDebug): Unit =
    import quotes.reflect.*
    if debug
    then
      x match
        case xs: List[Tree @unchecked]  =>
          println(s"===> [List] ${msg}")
          println(s"Code - Size:${xs.size}")
          println("  " + xs.map(_.show(using quotes.reflect.Printer.TreeAnsiCode)))

          println(s"Structure - Size:${xs.size}")
          println("  " + xs.map(_.show(using Printer.TreeStructure)))

        case term: Tree @unchecked =>
          println(s"===> [Tree] ${msg}")
          println(s"Symbol: ${term.symbol.flags.show}")
          println(s"Code: ${term.show(using quotes.reflect.Printer.TreeAnsiCode)}")
          println(s"Struct: ${term.show(using Printer.TreeStructure)}")
