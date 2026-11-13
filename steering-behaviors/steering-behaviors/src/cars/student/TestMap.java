package cars.student;

import cars.engine.Vector2;

import java.awt.*;
import java.util.List;

import static cars.engine.Vector2.vec2;

/**
 * Mapa de testes: obstáculos circulares e um circuito para o Path Following.
 * Coordenadas: origem no centro da janela (1024x768), y para baixo.
 */
public final class TestMap {
    private TestMap() {
    }

    /** Raio aproximado do carro, usado para "engordar" os obstáculos nos cálculos. */
    public static final double CAR_RADIUS = 14;

    public static final List<Obstacle> OBSTACLES = List.of(
        new Obstacle(vec2(-250, -120), 45),
        new Obstacle(vec2(-60, 150), 50),
        new Obstacle(vec2(200, -60), 55),
        new Obstacle(vec2(-150, 10), 30),
        new Obstacle(vec2(60, -235), 40),   // encosta no circuito: força um desvio
        new Obstacle(vec2(-340, 170), 32),  // fica em cima do circuito: obriga a desviar e voltar
        new Obstacle(vec2(330, 190), 40)
    );

    /** Circuito (sentido horário na tela). */
    public static final Path PATH = new Path(List.of(
        vec2(-400, -260),
        vec2(-100, -300),
        vec2(300, -290),
        vec2(420, -100),
        vec2(380, 200),
        vec2(100, 300),
        vec2(-250, 280),
        vec2(-420, 60)
    ));

    /**
     * Se o ponto estiver dentro de um obstáculo (ou colado nele), empurra-o para fora, para a borda
     * mais uma folga. Evita que um clique "impossível" faça o Arrive brigar para sempre com o desvio.
     */
    public static Vector2 pushOutOfObstacles(Vector2 p) {
        final var result = p.clone();
        for (var o : OBSTACLES) {
            final var minDist = o.radius() + CAR_RADIUS + 25;
            final var away = Vector2.subtract(result, o.center());
            if (away.size() < minDist) {
                final var dir = away.isZero() ? vec2(1, 0) : Vector2.normalize(away);
                result.set(o.center().x + dir.x * minDist, o.center().y + dir.y * minDist);
            }
        }
        return result;
    }

    /** Desenha o mapa (chamado pela Window antes de desenhar os carros). */
    public static void draw(Graphics2D g) {
        final var oldStroke = g.getStroke();
        final var oldColor = g.getColor();

        // circuito tracejado
        final var pts = PATH.getPoints();
        g.setColor(new Color(120, 120, 140));
        g.setStroke(new BasicStroke(2f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_ROUND, 1f, new float[]{8f, 8f}, 0f));
        for (int i = 0; i < pts.size(); i++) {
            final var a = pts.get(i);
            final var b = pts.get((i + 1) % pts.size());
            g.drawLine((int) a.x, (int) a.y, (int) b.x, (int) b.y);
        }
        g.setStroke(oldStroke);
        for (var p : pts) {
            g.fillOval((int) p.x - 3, (int) p.y - 3, 6, 6);
        }

        // obstáculos
        for (var o : OBSTACLES) {
            final var r = (int) o.radius();
            final var x = (int) o.center().x - r;
            final var y = (int) o.center().y - r;
            g.setColor(new Color(150, 150, 150));
            g.fillOval(x, y, 2 * r, 2 * r);
            g.setColor(new Color(90, 90, 90));
            g.setStroke(new BasicStroke(2f));
            g.drawOval(x, y, 2 * r, 2 * r);
        }

        g.setStroke(oldStroke);
        g.setColor(oldColor);
    }
}
