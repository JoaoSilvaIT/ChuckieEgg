import pt.isel.canvas.*
import kotlin.math.abs

// Dimensions of the sprites in the images files
// Floor, Egg, Food and Stair are 1x1 ; Man is 1x2 ; Hen is 1x3 or 2x2
const val SPRITE_WIDTH = 24  // [pixels in image file]
const val SPRITE_HEIGHT = 16 // [pixels in image file]

// Dimensions of the Arena grid
const val GRID_WIDTH = 20
const val GRID_HEIGHT = 24

// Dimensions of each cell of the Arena grid
const val VIEW_FACTOR = 2 // each cell is VIEW_FACTOR x sprite
const val CELL_WIDTH = VIEW_FACTOR * SPRITE_WIDTH   // [pixels]
const val CELL_HEIGHT = VIEW_FACTOR * SPRITE_HEIGHT  // [pixels]

/**
 * Creates a canvas with the dimensions of the arena.
 */
fun createCanvas() = Canvas(GRID_WIDTH * CELL_WIDTH, GRID_HEIGHT * CELL_HEIGHT, BLACK)

/**
 * Draw horizontal and vertical lines of the grid in arena.
 */
fun Canvas.drawGridLines() {
    (0 ..< width step CELL_WIDTH).forEach { x -> drawLine(x, 0, x, height, WHITE, 1) }
    (0 ..< height step CELL_HEIGHT).forEach { y -> drawLine(0, y, width, y, WHITE, 1) }
}

/**
 * Represents a sprite in the image.
 * Example: Sprite(2,3,1,1) is the man facing left.
 * @property row the row of the sprite in the image. (in sprites)
 * @property col the column of the sprite in the image. (in sprites)
 * @property height the height of the sprite in the image. (in sprites)
 * @property width the width of the sprite in the image. (in sprites)
 */
data class Sprite(val row: Int, val col: Int, val height: Int = 1, val width: Int = 1)

/**
 * Draw a sprite in a position of the canvas.
 * @param pos the position in the canvas (top-left of base cell).
 * @param spriteRow the row of the sprite in the image.
 * @param spriteCol the column of the sprite in the image.
 * @param spriteHeight the height of the sprite in the image.
 * @param spriteWidth the width of the sprite in the image.
 */
fun Canvas.drawSprite(pos: Point, s: Sprite) {
    val x = s.col * SPRITE_WIDTH + s.col + 1  // in pixels
    val y = s.row * SPRITE_HEIGHT + s.row + s.height
    val h = s.height * SPRITE_HEIGHT
    val w = s.width * SPRITE_WIDTH
    drawImage(
        fileName = "chuckieEgg|$x,$y,$w,$h",
        xLeft = pos.x,
        yTop = pos.y - (s.height-1) * CELL_HEIGHT,
        width = CELL_WIDTH * s.width,
        height = CELL_HEIGHT * s.height
    )
}

/**
 * Draw all the elements of the game.
 */
fun Canvas.drawGame(game: Game) {
    erase()
    drawGridLines()
    game.floor.forEach { drawSprite(it.toPoint(), Sprite(0,0)) }
    game.stairs.forEach { drawSprite(it.toPoint(), Sprite(0,1)) }
    game.eggs.forEach { drawSprite(it.toPoint(), Sprite(1,1)) }
    game.food.forEach { drawSprite(it.toPoint(), Sprite(1,0)) }
    drawMan(game.man)

    drawText(40, 50, "Score: " + game.score, 255255255, 35)
    drawText(600, 50, "Time: " + game.time, 255255255, 35)
    if (game.state == State.WINNER)
        drawText(200, 400, "YOU WON!", 255255255, 100)
    else if (game.state == State.TIMEOUT)
        drawText(200, 400, "YOU LOSE" , 255255255, 100)
}

/**
 * Draws the man in canvas according to the direction he is facing.
 */
fun Canvas.drawMan(m: Man) {

    val sprite = when(m.faced) {
        Direction.LEFT -> Sprite(2,3 + m.spriteState % 2,2)
        Direction.RIGHT -> Sprite(0,3 + m.spriteState % 2,2)
        Direction.UP -> Sprite(4,0 + m.spriteState % 5,2)
        Direction.DOWN -> Sprite(4,0 + abs(6 - m.spriteState) % 5,2)
    }
    drawSprite(m.pos, sprite)
}
