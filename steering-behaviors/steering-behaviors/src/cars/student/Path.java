package cars.student;

import cars.engine.Vector2;

import java.util.List;

import static cars.engine.Vector2.*;

/**
 * Caminho fechado (circuito) formado por segmentos de reta entre waypoints.
 * <p>
 * A posição ao longo do caminho é representada por um único número: a distância
 * percorrida desde o primeiro waypoint ("comprimento de arco", s). Isso torna fácil
 * "andar para frente" no caminho: basta somar um valor a s.
 */
public final class Path {
    private final List<Vector2> points;
    private final double[] start;   // start[i] = comprimento de arco no início do segmento i
    private final double length;    // comprimento total do circuito

    public Path(List<Vector2> points) {
        if (points.size() < 3) throw new IllegalArgumentException("Path precisa de ao menos 3 pontos");
        this.points = List.copyOf(points);
        this.start = new double[points.size()];
        double acc = 0;
        for (int i = 0; i < points.size(); i++) {
            start[i] = acc;
            acc += segmentLength(i);
        }
        this.length = acc;
    }

    public List<Vector2> getPoints() {
        return points;
    }

    public double getLength() {
        return length;
    }

    private Vector2 a(int i) {
        return points.get(i);
    }

    private Vector2 b(int i) {
        return points.get((i + 1) % points.size());
    }

    private double segmentLength(int i) {
        return Vector2.distance(a(i), b(i));
    }

    /**
     * Projeta um ponto qualquer no caminho: devolve o s do ponto do caminho mais próximo dele.
     */
    public double project(Vector2 p) {
        double bestS = 0;
        double bestDist = Double.MAX_VALUE;
        for (int i = 0; i < points.size(); i++) {
            final var ab = subtract(b(i), a(i));
            final var ap = subtract(p, a(i));
            final var t = Math.clamp(ap.dot(ab) / ab.sizeSqr(), 0.0, 1.0);
            final var closest = add(a(i), multiply(ab, t));
            final var d = Vector2.distanceSqr(p, closest);
            if (d < bestDist) {
                bestDist = d;
                bestS = start[i] + t * segmentLength(i);
            }
        }
        return bestS;
    }

    /**
     * Ponto do caminho na posição s (com volta ao início: o caminho é fechado).
     */
    public Vector2 pointAt(double s) {
        s = ((s % length) + length) % length;
        for (int i = points.size() - 1; i >= 0; i--) {
            if (s >= start[i]) {
                final var t = (s - start[i]) / segmentLength(i);
                return add(a(i), multiply(subtract(b(i), a(i)), t));
            }
        }
        return a(0).clone();
    }

    /**
     * Distância de um ponto até o caminho.
     */
    public double distanceTo(Vector2 p) {
        return Vector2.distance(p, pointAt(project(p)));
    }
}
