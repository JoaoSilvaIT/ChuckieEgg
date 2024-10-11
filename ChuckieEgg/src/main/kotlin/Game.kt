import com.sun.org.apache.xpath.internal.operations.Bool

/**
 * Represents the game action.
 */
enum class Action { WALK_LEFT, WALK_RIGHT, UP_STAIRS, DOWN_STAIRS, JUMP }

enum class State { PLAYING, WINNER, TIMEOUT}

/**
 * Represents all game information.
 * @property man information about man
 * @property floor positions of floor cells
 * @property stairs positions of stairs cells
 */
data class Game(
    val man: Man,
    val floor: List<Cell>,
    val stairs: List<Cell>,
    val eggs: List<Cell>,
    val food: List<Cell>,
    val score: Int,
    val time: Int,
    val state: State,
)

/**
 * Loads a game from a file.
 * @param fileName the name of the file with the game information.
 * @return the game loaded.
 */
fun loadGame(fileName: String) :Game {
    val cells: List<CellContent> = loadLevel(fileName)
    return Game(
        man = createMan( cells.first { it.type==CellType.MAN }.cell ),
        floor = cells.ofType(CellType.FLOOR),
        stairs = cells.ofType(CellType.STAIR),
        eggs = cells.ofType(CellType.EGG),
        food = cells.ofType(CellType.FOOD),
        score = 0,
        time = 0,
        state = State.PLAYING,
    )
}

fun Game.update(man: Man, eggs: List<Cell>, food: List<Cell>, score: Int, time: Int, state: State) :Game {
    return Game(
        man = man,
        floor = this.floor,
        stairs = this.stairs,
        eggs = eggs,
        food = food,
        score = score,
        time = time,
        state = state
    )
}

fun Game.onGround(pos: Point) :Boolean {
    var floors = this.floor
    this.stairs.forEach { cell ->
        if (this.floor.contains(cell + Direction.LEFT) || this.floor.contains(cell + Direction.RIGHT)) {
            floors = floors + cell
        }
    }
    val bounds = listOf(
        pos + Point(1, CELL_HEIGHT + 1),
        pos + Point(CELL_WIDTH - 1, CELL_HEIGHT + 1)
    )

    return bounds.any { it.toCell() in floors }
}

fun Game.tryMove(speed: Speed): Point {
    val predicted = this.man.pos.applySpeed(speed, this.floor)
    return predicted.limitToArea(MAX_X, MAX_Y)
}

fun Game.onStairs(): Boolean {
    return this.stairs.any { it == this.man.pos.toCell() }
}

/**
 * Performs an action to the game.
 * If the action is null, returns current game.
 * @param action the action to perform.
 * @receiver the current game.
 * @return the game after the action performed.
 */
fun Game.doAction(action: Action?) :Game {
    val faced = this.man.faced
    val pos = this.man.pos

    if (action == Action.WALK_LEFT && !this.man.jumping) {
        return this.update(Man(
            pos,
            Direction.LEFT,
            Speed(-MOVE_SPEED, 0),
            false,
            false,
            man.spriteState
        ), eggs, food, score, time, state)
    }
    else if (action == Action.WALK_RIGHT && !this.man.jumping) {
        return this.update(Man(
            pos,
            Direction.RIGHT,
            Speed(MOVE_SPEED, 0),
            false,
            false,
            man.spriteState
        ), eggs, food, score, time, state)
    }
    else if (action == Action.JUMP && !this.man.jumping && !onStairs()) {
        return this.update(Man(
            pos,
            faced,
            Speed(faced.dCol * MOVE_SPEED, -CELL_HEIGHT/2),
            true,
            false,
            man.spriteState
        ), eggs, food, score, time, state)
    }
    else if (action == Action.UP_STAIRS && !this.man.jumping && this.onStairs()) {
        return this.update(
            Man(
                pos,
                Direction.UP,
                Speed(0, -CLIMBING_SPEED),
                false,
                true,
                man.spriteState
            ), eggs, food, score, time, state
        )
    }
    else if (action == Action.DOWN_STAIRS && !this.man.jumping && this.onStairs()) {
        return this.update(
            Man(
                pos,
                Direction.DOWN,
                Speed(0, CLIMBING_SPEED),
                false,
                true,
                man.spriteState
            ), eggs, food, score, time, state
        )
    }
    return this
}

/**
 * Computes the next game state.
 * If the man is stopped, returns current game.
 * @receiver the current game.
 * @return the game after the next frame.
 */
fun Game.stepFrame(): Game {
    var faced = this.man.faced
    var pos = this.man.pos
    var speed = this.man.speed
    var jumping = this.man.jumping
    var climbing = this.man.climbing
    var score = this.score
    var state = this.state
    var eggs = this.eggs
    var foods = this.food
    var time = this.time
    var spriteState = man.spriteState

    if (state == State.PLAYING) {

        //println(this.onGround(pos))

        println(pos)

        if (jumping || (!this.onGround(pos) && !this.onStairs())) {
            speed += Speed(0, CELL_HEIGHT / SPRITE_HEIGHT)
            speed = speed.clamp(-CELL_HEIGHT / 2, CELL_HEIGHT / 2)

            pos = this.tryMove(speed)

            if (this.onGround(pos)) {
                val cellPoint = pos.toCell().toPoint()
                jumping = false
                speed = Speed(speed.dx, 0)
                pos = Point(pos.x, cellPoint.y)
            }
        } else {
            pos = this.tryMove(speed)
            speed = speed.stopIfInCell(pos)
        }

        climbing = climbing && this.onStairs()

        val bounds = pos.getBounds(CELL_WIDTH, CELL_HEIGHT * 2)

        val egg = bounds.firstOrNull { it.toCell() in this.eggs }
        if (egg != null) {
            eggs = eggs.filter { it != egg.toCell() }
            score += 100
        }

        val food = bounds.firstOrNull { it.toCell() in this.food }
        if (food != null) {
            foods = foods.filter { it != food.toCell() }
            score += 50
        }

        time += 1

        if (time >= 2666) {
            state = State.TIMEOUT

        } else if (eggs.isEmpty()) {
            state = State.WINNER
            score += 2666 - time
        }

        if ((man.speed.dx != 0 || man.speed.dy != 0) && !jumping)
            spriteState += 1
        else
            spriteState = 0
    }

    return this.update(Man(pos, this.man.faced, speed, jumping, climbing, spriteState), eggs, foods, score, time, state)
}
