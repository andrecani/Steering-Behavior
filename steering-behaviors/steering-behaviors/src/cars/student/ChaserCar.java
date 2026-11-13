package cars.student;

import cars.engine.Car;
import cars.engine.Vector2;
import cars.engine.World;

import java.awt.*;

/**
 * COMBINAÇÃO 3 (carro verde, o caçador): Seek + Obstacle Avoidance.
 * Persegue o {@link PreyCar} (laranja). É propositalmente mais lento que a presa.
 */
public class ChaserCar extends Car {
    private static final double W_AVOID = 1.5;
    private static final double W_SEEK = 1.0;

    private final Car prey;

    public ChaserCar(Car prey) {
        super(s -> s
            .color(new Color(30, 160, 60))
            .position(-380, -60)
            .orientationDegrees(90)
            .maxSpeed(260)
            .maxForce(350));
        this.prey = prey;
    }

    @Override
    public Vector2 calculateSteering(World world) {
        final var avoid = Behaviors.obstacleAvoidance(this, TestMap.OBSTACLES, 80, 200);
        final var seek = Behaviors.seek(this, prey.getPosition());

        return Behaviors.blend(getMaxForce(),
            Vector2.multiply(avoid, W_AVOID),
            Vector2.multiply(seek, W_SEEK));
    }
}
