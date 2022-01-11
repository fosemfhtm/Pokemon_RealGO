package com.skydoves.pokedexar.ui.scene

class FightResult(
    var ownerId: String,
    var name: String,
    var id: String,
    var result: String,
    var effect: String,
    var hp: Double,
    var attackOrder: Int,
    var maxHp: Double,
    var skillName: String
) {
}