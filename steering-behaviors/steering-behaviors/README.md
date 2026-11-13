# Trabalho II – Steering Behaviors (Prático)

Disciplina: Inteligência Artificial e Ilusão de Inteligência em Jogos – PUCPR – Prof.ª Marina de Lara
Equipe: _(preencher nomes)_

Agentes autônomos (carros de F1 vistos de cima) que se movem com **Steering Behaviors**, sobre a engine em Java fornecida pela professora (`cars.engine`).

## Como executar

Requer **JDK 21 ou superior** (a engine usa `Math.clamp`).

```bash
./run.sh        # Linux / macOS
run.bat         # Windows
```

Ou manualmente: `javac -d out $(find src -name '*.java')` e `java -cp out cars.engine.Window`.

**Controles**

| Ação | Efeito |
|---|---|
| Clique do mouse | Define o destino do carro azul (Arrive) |
| Mover o mouse | O cursor assusta o carro laranja (Flee) |
| Tecla `D` | Liga/desliga as setas de debug (azul = velocidade, laranja = força de direção) |

## O mapa de teste

Criado em `TestMap.java`: 7 obstáculos circulares e um circuito fechado de 8 waypoints (linha tracejada).
Dois detalhes foram feitos de propósito para testar o desvio: um obstáculo **encosta** no circuito (topo) e outro fica **em cima** dele (canto inferior esquerdo), obrigando o carro de corrida a desviar e voltar à pista.

## Como a engine funciona (resumo)

Cada carro implementa `calculateSteering(World)`, que devolve uma **força**. A engine:
1. limita a força a `maxForce`;
2. calcula `aceleração = força × dt / massa`;
3. soma à velocidade (limitada a `maxSpeed`) e move o carro.

Todos os comportamentos abaixo seguem a fórmula de Reynolds: **força = velocidade desejada − velocidade atual**.

## Comportamentos simples

Implementados em `Behaviors.java` (e `Wander.java`).

### 1. Seek – `Behaviors.seek`
Calcula a velocidade desejada apontando do carro para o alvo, na velocidade máxima. A força é `desejada − atual`. O carro acelera em direção ao alvo e, como não freia, passa direto e orbita ao redor dele. Usado pelo carro verde para perseguir o laranja.

### 2. Flee – `Behaviors.flee`
O inverso do Seek: a velocidade desejada aponta para longe da ameaça. Usado pelo carro laranja para fugir do carro verde e do cursor do mouse.

### 3. Arrive – `Behaviors.arrive`
Seek com frenagem. Dentro de um raio de frenagem (`SLOWING_RADIUS = 250 px`), a velocidade desejada cai linearmente com a distância (`maxSpeed × dist / raio`), chegando a zero no alvo. O carro para suavemente, sem passar do ponto. Usado pelo carro azul ao clicar no mapa.

### 4. Wander – `Wander.java`
Um círculo imaginário fica na frente do carro (`circleDistance`, `circleRadius`). O alvo do Seek é um ponto desse círculo, e a cada quadro o ângulo desse ponto sofre uma pequena variação aleatória (`jitter`, em rad/s, proporcional ao `dt` para não depender do FPS). Como o alvo muda pouco a cada quadro, a trajetória é curva e contínua. Vagar usa só uma fração de `maxSpeed` (`speedFactor`), para um movimento mais calmo.

## Comportamentos avançados

### 5. Obstacle Avoidance – `Behaviors.obstacleAvoidance`
1. Projeta um corredor à frente do carro. Seu comprimento cresce com a velocidade (de `minLook` até `maxLook`): quem anda rápido precisa enxergar mais longe.
2. Cada obstáculo é "engordado" pelo raio do carro (`CAR_RADIUS = 14`). Entram na análise só os que estão à frente e cuja distância lateral é menor que esse raio efetivo.
3. Escolhe o **mais próximo** (o mais urgente).
4. Gera uma **força lateral** para o lado oposto ao do obstáculo (`perp(direção)`), mais uma leve frenagem (30% de `maxForce`). A intensidade é proporcional à urgência: `1 − distância / alcance`, então a reação é suave de longe e forte de perto.

### 6. Path Following – `Behaviors.pathFollowing` e `Path.java`
1. Prevê onde o carro estará em `PREDICT_TIME` segundos (`posição + velocidade × t`).
2. Projeta essa posição prevista no circuito (`Path.project`, que devolve a distância percorrida ao longo do circuito).
3. Mira em um ponto `LOOK_AHEAD` pixels **à frente** nesse ponto (`Path.pointAt`) e faz Seek nele.

Mirar à frente evita a oscilação em cima da linha; prever a posição faz o carro começar a curvar antes dos cantos. O circuito é fechado, então o carro dá voltas infinitas.

## Combinações de comportamentos

### Como combinamos: soma ponderada com prioridade (`Behaviors.blend`)
Somar tudo com pesos fixos tem um problema: uma força fraca de vagar pode "diluir" uma emergência de desvio. Por isso usamos um **orçamento de força**:
- as forças entram **em ordem de prioridade** (desvio de obstáculo primeiro);
- cada uma consome parte do orçamento (`maxForce`);
- a seguinte só recebe o que sobrou, e entra truncada se não couber.

Assim, longe dos obstáculos o carro segue o objetivo normalmente; perto, o desvio consome o orçamento e vence. A transição é gradual, porque a força de desvio cresce aos poucos com a urgência. Cada força é multiplicada pelo seu peso **antes** de entrar no blend.

### Combinação 1 – Carro azul (`ExplorerCar`): Wander + Arrive + Obstacle Avoidance
- Sem destino, vaga pelo mapa. Ao clicar, vai até o ponto e para (Arrive). Ao chegar, volta a vagar.
- **Por quê:** Wander e Arrive são objetivos mutuamente exclusivos (o carro ou explora ou vai a um lugar), então alternamos entre eles. O desvio atua sempre, em prioridade máxima.
- Se o clique cair dentro (ou muito perto) de um obstáculo, o destino é empurrado para fora dele (`TestMap.pushOutOfObstacles`). Sem isso, o Arrive e o desvio brigariam para sempre.

### Combinação 2 – Carro vermelho (`RacerCar`): Path Following + Obstacle Avoidance
- Dá voltas no circuito; ao encontrar o obstáculo que está sobre a pista, desvia e depois é trazido de volta pelo Path Following.
- **Por quê:** é o caso clássico de objetivo de longo prazo (seguir a pista) + reação de curto prazo (não bater). O desvio tem prioridade, e o Path Following funciona como "ímã" que recompõe a trajetória.

### Combinação 3 – Carros verde e laranja (`ChaserCar` e `PreyCar`)
- **Verde (caçador):** Seek (no laranja) + Obstacle Avoidance. Mais lento (260 contra 320), então nunca alcança o laranja de forma definitiva.
- **Laranja (presa):** Flee + Wander + Obstacle Avoidance. Foge do verde e do cursor do mouse quando eles entram em `PANIC_RADIUS = 260 px`; longe deles, vaga.
- **Pesos dinâmicos:** `panic = min(2 × (1 − dist / raio), 1)`, com `peso_flee = panic` e `peso_wander = 1 − panic`. Na borda do raio de pânico a fuga é fraca e o carro continua vagando; quando a ameaça chega à metade do raio, o carro só foge. Isso evita o "liga/desliga" brusco de um `if` simples.

## Pesos e parâmetros ajustados

| Carro | Comportamento | Peso | Parâmetros principais |
|---|---|---|---|
| Azul | Obstacle Avoidance | 1,5 | corredor 60–160 px |
| Azul | Arrive | 1,0 | raio de frenagem 250 px |
| Azul | Wander | 1,0 | círculo dist. 80, raio 40, jitter 6 rad/s, 45% da velocidade |
| Vermelho | Obstacle Avoidance | 1,5 | corredor 60–170 px |
| Vermelho | Path Following | 1,0 | previsão 0,25 s, mira 60 px à frente |
| Verde | Obstacle Avoidance | 1,5 | corredor 80–200 px |
| Verde | Seek | 1,0 | `maxSpeed` 260, `maxForce` 350 |
| Laranja | Obstacle Avoidance | 1,5 | corredor 80–200 px |
| Laranja | Flee | dinâmico (0 a 1) | pânico a 260 px |
| Laranja | Wander | dinâmico (1 a 0) | círculo dist. 80, raio 50, jitter 8 rad/s, 40% da velocidade |

Por que o desvio tem peso 1,5: a força de desvio nunca passa de ~1,04 × `maxForce` (lateral + frenagem). Com peso 1,5 ela consegue consumir todo o orçamento quando o obstáculo está próximo, e o Seek/Arrive/Path deixam de "puxar" o carro para dentro do obstáculo.

### Como chegamos nesses valores (ajuste incremental)
- **Carro vermelho:** com velocidade 320 ele "abria" até 95 px do circuito nas curvas (overshoot). Reduzimos a velocidade para 230, a previsão para 0,25 s e a mira para 60 px, e o desvio máximo caiu para ~65 px, que ocorre justamente no obstáculo que está sobre a pista.
- **Verde e laranja:** com corredor de 60–150 px o carro verde raspava no obstáculo (a 1 px da borda). Aumentar o corredor para 80–200 px e o `maxForce` do verde para 350 resolveu.
- **Validação:** simulações sem janela de 120 s (várias execuções): nenhum carro entra em um obstáculo (distância mínima à borda ≈ raio do carro).

## Estrutura do código

```
src/cars/engine/   engine fornecida (única alteração: Window.draw chama TestMap.draw para desenhar o mapa)
src/cars/student/
  Behaviors.java     seek, flee, arrive, obstacleAvoidance, pathFollowing, blend
  Wander.java        wander (com estado)
  Path.java          circuito fechado (project / pointAt)
  Obstacle.java      obstáculo circular
  TestMap.java       obstáculos + circuito + desenho
  ExplorerCar.java   combinação 1 (azul)
  RacerCar.java      combinação 2 (vermelho)
  ChaserCar.java     combinação 3 (verde)
  PreyCar.java       combinação 3 (laranja)
  Setup.java         cria os 4 carros
```

## Limitações conhecidas
- A engine faz *wrap-around* nas bordas: a presa pode "teletransportar" de um lado ao outro, e o caçador (que mira na posição atual dela) muda bruscamente de direção.
- O desvio só considera obstáculos circulares e não leva em conta outros carros.
- O Seek do caçador mira na posição atual da presa, e não na posição futura (isso seria *Pursuit*).
