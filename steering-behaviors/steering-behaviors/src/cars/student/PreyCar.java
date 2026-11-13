package cars.student;

import cars.engine.Car;
import cars.engine.Vector2;
import cars.engine.World;

import java.awt.*;

/**
 * COMBINAÇÃO 3 (carro laranja, a presa): Flee + Wander + Obstacle Avoidance.
 * Foge do {@link ChaserCar} e do cursor do mouse quando eles chegam perto; longe de ameaças, vaga.
 * Os pesos de Flee e Wander são dinâmicos: quanto mais perto a ameaça, mais Flee e menos Wander.
 */
public class PreyCar extends Car {
    private static final double W_AVOID = 1.5;
    private static final double PANIC_RADIUS = 260; // ameaça dentro desse raio começa a assustar

    private final Wander wander = new Wander(80, 50, 8.0, 0.4);

    public PreyCar() {
        super(s -> s
            .color(Color.ORANGE)
            .position(280, 120)
            .orientationDegrees(200)
            .maxSpeed(320)
            .maxForce(400));
    }

    @Override
    public Vector2 calculateSteering(World world) {
        // ameaça mais próxima: o carro caçador ou o mouse
        Vector2 threat = null;
        double best = PANIC_RADIUS;
        for (var c : world.getNeighbors((int) PANIC_RADIUS)) {
            if (c instanceof ChaserCar) {
                final var d = Car.distance(this, c);
                if (d < best) {
                    best = d;
                    threat = c.getPosition();
                }
            }
        }
        final var mouse = world.getMousePos();
        if (mouse != null) {
            final var d = Vector2.distance(getPosition(), mouse);
            if (d < best) {
                best = d;
                threat = mouse;
            }
        }

        // 0 = ameaça longe (borda do raio), 1 = ameaça muito perto
        final var panic = threat == null ? 0.0 : Math.min(2.0 * (1.0 - best / PANIC_RADIUS), 1.0);
        final var wFlee = panic;
        final var wWander = 1.0 - panic;

        final var avoid = Behaviors.obstacleAvoidance(this, TestMap.OBSTACLES, 80, 200);
        final var flee = threat == null ? Vector2.vec2() : Behaviors.flee(this, threat);
        final var wan = wander.steer(this, world.getSecs());

        return Behaviors.blend(getMaxForce(),
            Vector2.multiply(avoid, W_AVOID),
            Vector2.multiply(flee, wFlee),
            Vector2.multiply(wan, wWander));
    }
}
