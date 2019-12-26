package com.bagolysz.arsheeps

data class Level(
    val name: String,
    val numberOfSheeps: Int,
    val speed: Float,
    val randomChangePeriod: Long = 2000L
)