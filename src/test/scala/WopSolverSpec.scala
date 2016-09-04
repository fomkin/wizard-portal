import org.scalatest._
import wop.game.TicTacToe
import wop.game.WopState._
import wop.game.ai.WopSolver.Heuristica._
import wop.game.ai.WopSolver._

class WopSolverSpec extends FlatSpec with Matchers {

  it should "Local field is evaluated eq 4" in {
    Heuristica.evalBoard(
      LocalField(
        "___",
        "_x_",
        "___"
      )) should be(4)
  }

  it should "Global field is evaluated eq 40" in {
    Heuristica.evalBoard(
      GlobalField(
        "___",
        "_x_",
        "___"
      )) should be(40)
  }

  it should "Local empty should be eq 0" in {
    Heuristica.evalBoard(Empty) should be(0)
  }

  it should "Global empty should be eq 0" in {
    Heuristica.evalBoard(
      GlobalField(
        "___",
        "___",
        "___"
      )) should be(0)
  }

  def LocalField(xs: String*): TicTacToe[XO] = {
    TicTacToe(size = 3, matrix = xs.mkString.toVector map {
      case 'x' => XO.X
      case 'o' => XO.O
      case _ => XO.Empty
    })
  }

  val XWin = LocalField(
    "xox",
    "_x_",
    "x__"
  )

  val OWin = LocalField(
    "xox",
    "_o_",
    "xo_"
  )

  val Empty = LocalField(
    "___",
    "___",
    "___"
  )

  def GlobalField(xs: String*): TicTacToe[SubBoard] = {
    TicTacToe(size = 3, matrix = xs.mkString.toVector map {
      case 'x' => XWin
      case 'o' => OWin
      case _ => Empty
    })
  }
}
