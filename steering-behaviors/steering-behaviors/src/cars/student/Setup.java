package cars.student;

import cars.engine.Car;

import java.util.List;

public class Setup {
    /**
     * Retorna todos os carros do exercício.
     * Azul: Wander/Arrive + desvio | Vermelho: Path Following + desvio | Verde x Laranja: Seek x Flee/Wander + desvio
     */
    public List<Car> createCars() {
        final var prey = new PreyCar();
        return List.of(
            new ExplorerCar(),
            new RacerCar(),
            new ChaserCar(prey),
            prey
        );
    }
}
