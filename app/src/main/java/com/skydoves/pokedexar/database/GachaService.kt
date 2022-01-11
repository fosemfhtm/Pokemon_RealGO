package com.skydoves.pokedexar.database

import com.google.android.filament.Box
import retrofit2.Call
import retrofit2.http.*

interface GachaService{

    @FormUrlEncoded
    @POST("/pokemon/gacha/")
    fun requestBoxList(
        @Header("Authorization") token: String,
        @Field("gacha") gacha: Int,
    ) : Call<BoxData>
}