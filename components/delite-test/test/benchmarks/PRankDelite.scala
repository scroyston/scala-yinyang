trait PRD extends lifted.OptiGraph {
  def mainDelite(): Any = ()
  override def main(): Unit = {
    val G = graph_load("/home/viq/delite/Delite/test.bin")

    val e = 0.001
    val d = 0.85
    val max_iter = 6
    val PR = NodeProperty[Double](G)

    val diff = Reduceable[Double](0.0)
    var cnt = 0
    val N = G.NumNodes.asInstanceOf[Rep[Double]]
    PR.setAll(1.0 / N)

    println("G.N " + N)

    // move to ds
    val deg = NewArray[Int](G.NumNodes)
    for (t ← G.Nodes) {
      deg(t.Id) = t.OutDegree
    }

    tic(G, PR, deg)
    //var num_abs = 0
    //var v = 0.0
    var cond = true
    //val n = G.Node(0)
    while (cond) {
      diff.setValue(0.0)
      for (t ← G.Nodes) {
        val Val: Rep[Double] = ((1.0 - d) / N) + unit(d) * Sum(t.InNbrs) {
          w ⇒ PR(w) / deg(w.Id) //w.OutDegree
        }
        //val Val = v
        PR <= (t, Val)

        diff += Math.abs(Val - PR(t))
        //num_abs += 1
        //v += 1.0
      }
      PR.assignAll()
      cnt += 1
      cond = (diff.value > e) && (cnt < max_iter)
    }
    println("count = " + cnt)
    //println("abs times = " + num_abs)
    toc()

    //    for(t <- G.Nodes) {
    //      println(" PR " + t.Id + " " + PR(t))
    //    }
  }
}