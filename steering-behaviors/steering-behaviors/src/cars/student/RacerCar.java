package cars.student;

import cars.engine.Car;
import cars.engine.Vector2;
import cars.engine.World;

import java.awt.*;

/**
 * COMBINAÇÃO 2 (carro vermelho): Path Following + Obstacle Avoidance.
 * Dá voltas no circuito sozinho. Quando um obstáculo aparece no caminho, desvia e,
 * depois de passar, o Path Following o devolve ao circuito.
 */
public class RacerCar extends Car {
    private static final double W_AVOID = 1.5;
    private static final double W_PATH = 1.0;

    private static final double PREDICT_TIME = 0.25; // s: quão à frente prevê a posição
    private static final double LOOK_AHEAD = 60;    // px: quão à frente no caminho ele mira

    public RacerCar() {
        super(s -> s
            .color(Color.RED)
            .position(TestMap.PATH.pointAt(0))
            .orientation(headingAlongPath())
            .maxSpeed(230)
            .maxForce(400));
    }

    private static double headingAlongPath() {
        final var d = Vector2.subtract(TestMap.PATH.pointAt(30), TestMap.PATH.pointAt(0));
        return Math.atan2(d.y, d.x);
    }

    @Override
    public Vector2 calculateSteering(World world) {
        final var avoid = Behaviors.obstacleAvoidance(this, TestMap.OBSTACLES, 60, 170);
        final var path = Behaviors.pathFollowing(this, TestMap.PATH, PREDICT_TIME, LOOK_AHEAD, getMaxSpeed());

        return Behaviors.blend(getMaxForce(),
            Vector2.multiply(avoid, W_AVOID),
            Vector2.multiply(path, W_PATH));
    }
}
