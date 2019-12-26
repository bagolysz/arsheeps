package com.bagolysz.arsheeps

import com.google.ar.sceneform.Node
import com.google.ar.sceneform.math.Quaternion
import com.google.ar.sceneform.math.Vector3
import kotlin.random.Random

class Sheep(val node: Node) {

    var direction: Direction = Direction.FORWARD
    var mayMove = false
    var finished = false
    var mayChangeDirection = true

    fun update() {
        if (mayMove && !finished) {
            node.worldRotation = getRotationMatrix()
            node.worldPosition = getUpdatedPosition(node.worldPosition)
        }
    }

    fun changeDirection() {
        if (mayChangeDirection) {
            var still = true
            while (still) {
                val rand = Random.nextInt(0, 4)
                val newDirection = Direction.values()[rand]
                if (newDirection != direction) {
                    direction = newDirection
                    still = false
                }
            }
        }

//        direction = when (direction) {
//            Direction.FORWARD -> Direction.RIGHT
//            Direction.RIGHT -> Direction.BACK
//            Direction.BACK -> Direction.LEFT
//            Direction.LEFT -> Direction.FORWARD
//        }
    }

    private fun getUpdatedPosition(oldPosition: Vector3): Vector3 {
        val newPosition = Vector3(oldPosition.x, oldPosition.y, oldPosition.z)

        when (direction) {
            Direction.FORWARD -> {
                newPosition.z += SPEED
            }
            Direction.BACK -> {
                newPosition.z -= SPEED
            }
            Direction.RIGHT -> {
                newPosition.x += SPEED
            }
            Direction.LEFT -> {
                newPosition.x -= SPEED
            }
        }

        return newPosition
    }

    private fun getRotationMatrix(): Quaternion {
        return when (direction) {
            Direction.FORWARD -> Quaternion.axisAngle(Vector3(0f, 1f, 0f), 0f)
            Direction.BACK -> Quaternion.axisAngle(Vector3(0f, 1f, 0f), 180f)
            Direction.RIGHT -> Quaternion.axisAngle(Vector3(0f, 1f, 0f), 90f)
            Direction.LEFT -> Quaternion.axisAngle(Vector3(0f, 1f, 0f), 270f)
        }
    }


    enum class Direction {
        FORWARD,
        BACK,
        RIGHT,
        LEFT
    }

    companion object {
        private const val SPEED = 0.0007f
    }
}