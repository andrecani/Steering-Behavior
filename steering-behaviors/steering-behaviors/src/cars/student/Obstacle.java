package cars.student;

import cars.engine.Vector2;

/**
 * Obstáculo circular do mapa de testes.
 * Coordenadas no mesmo sistema da engine: origem no centro da janela, y para baixo.
 */
public record Obstacle(Vector2 center, double radius) {
}
