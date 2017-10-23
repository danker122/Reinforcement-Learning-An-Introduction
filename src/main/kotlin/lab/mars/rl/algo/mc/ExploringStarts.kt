package lab.mars.rl.algo.mc

import lab.mars.rl.algo.V_from_Q_ND
import lab.mars.rl.algo.mc.MonteCarlo.Companion.log
import lab.mars.rl.model.OptimalSolution
import lab.mars.rl.model.State
import lab.mars.rl.util.argmax
import lab.mars.rl.util.buf.newBuf
import lab.mars.rl.util.debug

fun MonteCarlo.`Optimal Exploring Starts`(): OptimalSolution {
    if (policy.isEmpty()) {
        policy = mdp.QFunc { 0.0 }
        for (s in started)
            policy[s, s.actions.first()] = 1.0
    }
    val Q = mdp.QFunc { 0.0 }
    val tmpQ = mdp.QFunc { Double.NaN }
    val count = mdp.QFunc { 0 }
    val tmpS = newBuf<State>(states.size)

    for (episode in 1..episodes) {
        log.debug { "$episode/$episodes" }
        var s = started.rand()
        var a = s.actions.rand()//Exploring Starts

        var accumulate = 0.0
        do {
            val possible = a.sample()
            if (tmpQ[s, a].isNaN())
                tmpQ[s, a] = accumulate
            accumulate += possible.reward
            s = possible.next
        } while (s.isNotTerminal().apply { if (this) a = s.actions.rand(policy(s)) })

        tmpS.clear()
        for (s in states) {
            if (s.isTerminal()) continue
            for (a in s.actions) {
                val value = tmpQ[s, a]
                if (!value.isNaN()) {
                    Q[s, a] += accumulate - value
                    count[s, a] += 1
                    tmpS.append(s)
                    tmpQ[s, a] = Double.NaN
                }
            }
        }
        for (s in tmpS) {
            val a_greedy = argmax(s.actions) {
                val n = count[s, it]
                if (n > 0)
                    Q[s, it] / n
                else
                    Q[s, it]
            }
            for (a in s.actions)
                policy[s, a] = if (a === a_greedy) 1.0 else 0.0
        }
    }

    Q.set { idx, value ->
        val n = count[idx]
        if (n > 0)
            value / n
        else
            value
    }
    val V = mdp.VFunc { 0.0 }
    val result = Triple(policy, V, Q)
    V_from_Q_ND(states, result)
    return result
}