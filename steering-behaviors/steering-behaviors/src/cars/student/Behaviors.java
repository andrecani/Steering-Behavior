package cars.student;

import cars.engine.Car;
import cars.engine.Vector2;

import java.util.List;

import static cars.engine.Vector2.*;

/**
 * Biblioteca de steering behaviors (sem estado).
 * <p>
 * Todos retornam uma força de direção (steering) no formato "velocidade desejada - velocidade atual".
 * A engine depois limita essa força a maxForce, divide pela massa e integra a velocidade.
 * O comportamento com estado (Wander) está na classe {@link Wander}.
 */
public final class Behaviors {
    private Behaviors() {
    }

    // ------------------------------------------------------------------
    // 1) SEEK: vai em direção a um alvo
    // ------------------------------------------------------------------
    public static Vector2 seek(Car car, Vector2 target) {
        return seek(car, target, car.getMaxSpeed());
    }

    /** Seek com velocidade desejada customizada (permite andar mais devagar que maxSpeed). */
    public static Vector2 seek(Car car, Vector2 target, double speed) {
        final var toTarget = subtract(target, car.getPosition());
        if (toTarget.isZero()) return vec2();
        final var desired = multiply(normalize(toTarget), speed);
        return subtract(desired, car.getVelocity());
    }

    // ------------------------------------------------------------------
    // 2) FLEE: foge de uma ameaça (o inverso do Seek)
    // ------------------------------------------------------------------
    public static Vector2 flee(Car car, Vector2 threat) {
        var away = subtract(car.getPosition(), threat);
        if (away.isZero()) away = car.getDirection(); // exatamente em cima da ameaça: segue em frente
        final var desired = multiply(normalize(away), car.getMaxSpeed());
        return subtract(desired, car.getVelocity());
    }

    // ------------------------------------------------------------------
    // 3) ARRIVE: seek que desacelera suavemente ao chegar
    // ------------------------------------------------------------------
    public static Vector2 arrive(Car car, Vector2 target, double slowingRadius) {
        final var toTarget = subtract(target, car.getPosition());
        final var dist = toTarget.size();
        if (dist < 1e-6) return negate(car.getVelocity());
        // dentro do raio de frenagem, a velocidade desejada cai linearmente até 0
        final var speed = car.getMaxSpeed() * Math.min(dist / slowingRadius, 1.0);
        final var desired = multiply(divide(toTarget, dist), speed);
        return subtract(desired, car.getVelocity());
    }

    // ------------------------------------------------------------------
    // 5) OBSTACLE AVOIDANCE
    // ------------------------------------------------------------------

    /**
     * Projeta um "bigode" à frente do carro (maior quanto mais rápido ele anda) e procura o obstáculo
     * mais próximo que cruza esse corredor. Se achar, empurra o carro para o lado oposto ao obstáculo
     * (força lateral) e aplica uma leve frenagem. Quanto mais perto o obstáculo, mais forte a reação.
     *
     * @param minLook comprimento mínimo do corredor (carro parado)
     * @param maxLook comprimento do corredor na velocidade máxima
     */
    public static Vector2 obstacleAvoidance(Car car, List<Obstacle> obstacles, double minLook, double maxLook) {
        final var pos = car.getPosition();
        final var dir = car.getDirection();
        final var look = minLook + (maxLook - minLook) * (car.getSpeed() / car.getMaxSpeed());

        Obstacle threat = null;
        double threatT = Double.MAX_VALUE;   // distância à frente até o obstáculo
        double threatSide = 0;               // lado do obstáculo (produto vetorial)
        double threatReach = 0;

        for (var o : obstacles) {
            final var rel = subtract(o.center(), pos);
            final var reach = o.radius() + TestMap.CAR_RADIUS;       // obstáculo "engordado" pelo carro
            final var ahead = rel.dot(dir);                          // projeção na direção do movimento
            final var side = dir.x * rel.y - dir.y * rel.x;          // distância lateral com sinal

            final var inside = rel.size() < reach;
            if (!inside && (ahead <= 0 || ahead > look + reach)) continue; // atrás ou longe demais
            if (!inside && Math.abs(side) >= reach) continue;              // fora do corredor

            final var t = Math.max(ahead, 0);
            if (t < threatT) {
                threat = o;
                threatT = t;
                threatSide = side;
                threatReach = reach;
            }
        }

        if (threat == null) return vec2();

        // urgência: 1 = colado no obstáculo, 0 = no limite do alcance do corredor
        final var urgency = 1.0 - Math.min(threatT / (look + threatReach), 1.0);
        // obstáculo à "esquerda" (side>0) => vira para o lado oposto, e vice-versa
        final var sign = threatSide > 0 ? -1.0 : 1.0;
        final var lateral = multiply(perp(dir), sign * car.getMaxForce() * urgency);
        final var brake = multiply(dir, -0.3 * car.getMaxForce() * urgency);
        return add(lateral, brake);
    }

    // ------------------------------------------------------------------
    // 6) PATH FOLLOWING
    // ------------------------------------------------------------------

    /**
     * Prevê onde o carro estará em predictTime segundos, projeta essa posição no caminho e
     * faz Seek para um ponto lookAhead pixels à frente nesse ponto projetado.
     * Mirar "à frente" evita que o carro fique oscilando em cima da linha.
     */
    public static Vector2 pathFollowing(Car car, Path path, double predictTime, double lookAhead, double speed) {
        final var future = add(car.getPosition(), multiply(car.getVelocity(), predictTime));
        final var s = path.project(future);
        final var target = path.pointAt(s + lookAhead);
        return seek(car, target, speed);
    }

    // ------------------------------------------------------------------
    // COMBINAÇÃO: soma ponderada com prioridade e orçamento de força
    // ------------------------------------------------------------------

    /**
     * Combina forças (já multiplicadas pelos seus pesos) em ordem de prioridade.
     * Mantém um "orçamento" de maxForce: a primeira força consome o que precisar; as seguintes
     * só recebem o que sobrar. Se uma não cabe inteira, entra truncada. Assim, uma
     * emergência (desviar de obstáculo) nunca é diluída pelas forças menos importantes.
     */
    public static Vector2 blend(double maxForce, Vector2... forcesByPriority) {
        final var total = vec2();
        for (var f : forcesByPriority) {
            if (Vector2.isZero(f)) continue;
            final var remaining = maxForce - total.size();
            if (remaining <= 0) break;
            total.add(truncate(f, remaining));
        }
        return total;
    }
}
