package ch.epfl.yinyang
package analysis

import ch.epfl.yinyang._
import scala.reflect.macros.blackbox.Context
import language.experimental.macros
import scala.collection.mutable
import mutable.{ ListBuffer, HashMap }

/**
 * Analyzed free variables in the block. Free vars are the identifiers that are not
 * defined in the block.
 */
trait FreeIdentAnalysis extends MacroModule with TransformationUtils {
  import c.universe._

  // Symbol tracking.
  // val symbolIds: mutable.HashMap[Int, Symbol] = new mutable.HashMap()
  // def symbolById(id: Int) = symbolIds(id)

  def freeVariables(tree: Tree): List[Tree] =
    new FreeVariableCollector().collect(tree)

  class FreeVariableCollector extends Traverser {

    private[this] val collected = ListBuffer[Tree]()
    private[this] var defined = List[Symbol]()

    private[this] final def isFree(id: Symbol) = !defined.contains(id)

    override def traverse(tree: Tree) = tree match {
      case i @ Ident(s) => {
        val sym = i.symbol
        //store info about idents
        // symbolIds.put(symbolId(sym), sym)
        if (sym.isTerm &&
          !(sym.isMethod || sym.isPackage || sym.isModule) &&
          isFree(sym)) collected append i
      }
      case _ => super.traverse(tree)
    }

    def collect(tree: Tree): List[Tree] = {
      collected.clear()
      defined = new LocalDefCollector().definedSymbols(tree)
      log(s"FreeIdentAnalysis: Defined (not-free variables): $defined", 2)
      traverse(tree)
      collected.map(x => (x.symbol, x)).toMap.values.toList
    }

  }

  class LocalDefCollector extends Traverser {

    private[this] val definedValues, definedMethods = ListBuffer[Symbol]()

    override def traverse(tree: Tree) = tree match {
      case vd @ ValDef(mods, name, tpt, rhs) =>
        definedValues += vd.symbol
        traverse(rhs)
      case dd @ DefDef(mods, name, tparams, vparamss, tpt, rhs) =>
        definedMethods += dd.symbol
        vparamss.flatten.foreach(traverse)
        traverse(rhs)
      case bind @ Bind(name, body) =>
        definedValues += bind.symbol
        traverse(body)
      case _ =>
        super.traverse(tree)
    }

    def definedSymbols(tree: Tree): List[Symbol] = {
      definedValues.clear()
      definedMethods.clear()
      traverse(tree)
      (definedValues ++ definedMethods).toList
    }

  }
}
