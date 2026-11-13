package cars.student;

import cars.engine.Car;
import cars.engine.Vector2;
import cars.engine.World;

import java.awt.*;

import static cars.engine.Vector2.vec2;

/**
 * COMBINAÇÃO 1 (carro azul): Wander + Arrive + Obstacle Avoidance.
 * <ul>
 *   <li>Sem destino: vaga pelo mapa (Wander).</li>
 *   <li>Ao clicar no mapa: vai até o ponto e para suavemente (Arrive).</li>
 *   <li>Ao chegar: volta a vagar até o próximo clique.</li>
 *   <li>O tempo todo desvia dos obstáculos, com prioridade sobre o resto.</li>
 * </ul>
 */
public class ExplorerCar extends Car {
    // ---- pesos (ver README para a justificativa) ----
    private static final double W_AVOID = 1.5;
    private static final double W_ARRIVE = 1.0;
    private static final double W_WANDER = 1.0;

    // ---- parâmetros dos comportamentos ----
    private static final double SLOWING_RADIUS = 250;
    private static final double ARRIVED_DISTANCE = 8;
    private static final double ARRIVED_SPEED = 15;

    private final Wander wander = new Wander(80, 40, 6.0, 0.45);
    private Vector2 lastClick = null; // último clique já processado
    private Vector2 target = null;    // destino atual (null = vagando)

    public ExplorerCar() {
        super(s -> s
            .color(Color.BLUE)
            .position(-120, 70)
            .orientationDegrees(0)
            .maxSpeed(300)
            .maxForce(300));
    }

    @Override
    public Vector2 calculateSteering(World world) {
        // novo clique? vira o destino (afastado de obstáculos, se cair em cima de um)
        final var click = world.getClickPos();
        if (click != null && (lastClick == null || !click.equals(lastClick))) {
            lastClick = click;
            target = TestMap.pushOutOfObstacles(click);
        }

        // chegou? volta a vagar
        if (target != null
            && Vector2.distance(getPosition(), target) < ARRIVED_DISTANCE
            && getSpeed() < ARRIVED_SPEED) {
            target = null;
        }

        final var avoid = Behaviors.obstacleAvoidance(this, TestMap.OBSTACLES, 60, 160);
        final var goal = target != null
            ? Behaviors.arrive(this, target, SLOWING_RADIUS)
            : wander.steer(this, world.getSecs());
        final var goalWeight = target != null ? W_ARRIVE : W_WANDER;

        return Behaviors.blend(getMaxForce(),
            Vector2.multiply(avoid, W_AVOID),
            Vector2.multiply(goal, goalWeight));
    }
}
