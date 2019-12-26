package com.bagolysz.arsheeps

import com.google.ar.sceneform.Node
import com.google.ar.sceneform.math.Quaternion
import com.google.ar.sceneform.math.Vector3
import kotlin.random.Random

class Sheep(val node: Node) {

    var degrees: Double = 0.0
    var mayMove = false
    var finished = false
    var mayChangeDirection = true

    fun init() {
        changeDirection()
    }

    fun update() {
        if (mayMove && !finished) {
            node.worldPosition = getUpdatedPosition(node.worldPosition)
        }
    }

    fun changeDirection(rotation: Quaternion? = null) {
        if (rotation != null) {
            node.worldRotation = rotation
            degrees = toDegrees(rotation)
        } else {
            val rand = Random.nextInt(0, 4)
            val generatedDir = Direction.values()[rand]
            val generatedRot = getRotationMatrix(generatedDir)
            node.worldRotation = generatedRot
            degrees = toDegrees(generatedRot)
        }
    }

    private fun getUpdatedPosition(oldPosition: Vector3): Vector3 {
        val newPosition = Vector3(oldPosition.x, oldPosition.y, oldPosition.z)

        when (degrees) {
            in 0.0..90.0 -> {
                val m = map(degrees, 0.0, 90.0).toFloat()
                newPosition.z += (1 - m) * SPEED
                newPosition.x += m * SPEED
            }
            in 90.0..180.0 -> {
                val m = map(degrees, 90.0, 180.0).toFloat()
                newPosition.x += (1 - m) * SPEED
                newPosition.z -= m * SPEED
            }
            in 180.0..270.0 -> {
                val m = map(degrees, 180.0, 270.0).toFloat()
                newPosition.z -= (1 - m) * SPEED
                newPosition.x -= m * SPEED
            }
            in 270.0..360.0 -> {
                val m = map(degrees, 270.0, 360.0).toFloat()
                newPosition.x -= (1 - m) * SPEED
                newPosition.z += m * SPEED
            }
        }

        return newPosition
    }

    private fun toDegrees(rotation: Quaternion): Double {
        var w = rotation.w.toDouble()
        if (rotation.y < 0.0) {
            w = -w
        }

        val var3 = Math.acos(w) * 2.0
        val var1 = Math.toDegrees(var3)
        return var1
    }

    private fun map(
        x: Double,
        inMin: Double,
        inMax: Double,
        outMin: Double = 0.0,
        outMax: Double = 1.0
    ): Double {
        return (x - inMin) * (outMax - outMin) / (inMax - inMin) + outMin
    }

    private fun getRotationMatrix(direction: Direction): Quaternion {
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
        private const val SPEED = 0.0004f
    }
}