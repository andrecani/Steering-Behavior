package cars.student;

import cars.engine.Car;
import cars.engine.Vector2;

import java.util.random.RandomGenerator;

import static cars.engine.Vector2.*;

/**
 * 4) WANDER: caminhada aleatória suave.
 * <p>
 * Imagine um círculo na frente do carro. Um ponto desse círculo é o alvo do Seek, e a cada quadro
 * esse ponto desliza um pouquinho sobre o círculo. Como o alvo muda aos poucos, a trajetória
 * é curva e contínua em vez de "tremida". Precisa de estado (o ângulo atual), por isso é uma classe.
 */
public final class Wander {
    private static final RandomGenerator RND = RandomGenerator.getDefault();

    private final double circleDistance; // distância do centro do círculo até o carro
    private final double circleRadius;   // raio do círculo (quanto maior, mais curvas fechadas)
    private final double jitter;         // rad/s: o quão rápido o alvo desliza no círculo
    private final double speedFactor;    // fração de maxSpeed usada ao vagar
    private double angle = RND.nextDouble(0, 2 * Math.PI);

    public Wander(double circleDistance, double circleRadius, double jitter, double speedFactor) {
        this.circleDistance = circleDistance;
        this.circleRadius = circleRadius;
        this.jitter = jitter;
        this.speedFactor = speedFactor;
    }

    public Vector2 steer(Car car, double dt) {
        // pequena variação aleatória do ângulo, proporcional ao tempo (independe do FPS)
        angle += (RND.nextDouble() * 2 - 1) * jitter * dt;

        final var dir = car.getDirection();
        final var heading = Math.atan2(dir.y, dir.x);
        final var center = add(car.getPosition(), multiply(dir, circleDistance));
        final var target = add(center, byAngleSize(heading + angle, circleRadius));
        return Behaviors.seek(car, target, car.getMaxSpeed() * speedFactor);
    }
}
