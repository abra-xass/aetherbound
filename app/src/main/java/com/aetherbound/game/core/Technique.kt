package com.aetherbound.game.core

enum class TechniqueCategory { STRIKE, PULSE, GUARD, FIELD, BIND, RECOVERY }

data class Technique(
    val id: String,
    val name: String,
    val aspect: Aspect,
    val category: TechniqueCategory,
    val power: Int,
    val accuracy: Int,
    val priority: Int = 0,
    val critRate: Int = 1,
)
