package dsl.print

import ch.epfl.lamp.mpde.api._
import base._
import scala.collection._

/** The int printing DSL */
trait PrintDSL
  extends ScalaCompile with CodeGenerator with base.LiftBase with MiniIntDSL
  with base.Interpret with BaseYinYang {

  var sb: StringBuffer = new StringBuffer()

  val recompileHoles = mutable.Set[scala.Int]()

  // hey, we can refine println -- but we don't need it right now
  def println(x: Int) = sb.append(s"scala.Predef.println(${x.toString});\n")

  def break(x: Int) = {
    sb.append("scala.Predef.println(\"break called with " + x.toString + "\");\n")
    x match {
      case Hole(tpe, id) ⇒
        recompileHoles += id
      case _ ⇒
    }
  }

  def reset() = {
    sb = new StringBuffer()
    recompileHoles.clear
    holes.clear
  }

  def stagingAnalyze(): List[scala.Int] = {
    reset()
    main()

    recompileHoles.toList
  }

  def generateCode(className: String): String = {
    reset()
    val res = main()
    val distinctHoles = holes.distinct
    s"""
      class $className extends Function${distinctHoles.size}[${"Int, " * distinctHoles.size} Int] {
        def apply(${distinctHoles.map(y ⇒ y.toString + ": " + y.tpe.toString).mkString("", ",", "")}) = {
          ${sb.toString} 
          ${res.toString}
        }
      }
    """
  }

  // TODO make a super trait
  override def interpret[T: Manifest](params: Any*): T = {
    if (compiledCode == null) {
      compiledCode = compile[T, () ⇒ T]
    }
    compiledCode.apply().asInstanceOf[T]
  }

  var compiledCode: () ⇒ Any = _
}

trait MiniIntDSL extends BaseYinYang with base.LiftBase { self: CodeGenerator ⇒

  type Int = IntOps

  val holes: mutable.ArrayBuffer[Hole] = new mutable.ArrayBuffer()

  trait IntOps {
    def +(that: Int): Int = IntPlus(IntOps.this, that)
    def value: scala.Int
  }

  // actual classes that provide lifting
  case class IntConst(i: scala.Int) extends IntOps {
    override def toString = s"$i"
    def value = i
  }

  case class IntPlus(l: IntOps, r: IntOps) extends IntOps {
    override def toString = s"($l + $r)"
    def value = 5
  }

  implicit object LiftInt extends LiftEvidence[scala.Int, Int] {
    def lift(v: scala.Int): Int = IntConst(v)
  }

  case class Hole(tpe: Manifest[Any], symbolId: scala.Int) extends IntOps {
    override def toString = "x" + symbolId
    def value = -1
  }

  implicit object HoleInt extends HoleEvidence[scala.Int, this.Int] {
    def emit(tpe: Manifest[Any], symbolId: scala.Int): Int =
      {
        val h = Hole(tpe, symbolId)
        holes += h
        h
      }
  }

}

trait MiniUnitDSL extends base.LiftBase {
  implicit object LiftUnit extends LiftEvidence[scala.Unit, Unit] {
    def lift(v: Unit): Unit = ()
  }
}
