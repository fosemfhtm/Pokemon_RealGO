
package com.skydoves.pokedexar.ui.shop

import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.*
import androidx.activity.viewModels
import com.amn.easysharedpreferences.EasySharedPreference
import com.google.android.filament.Box
import com.skydoves.bindables.BindingActivity
import com.skydoves.bundler.intentOf
import com.skydoves.pokedexar.R
import com.skydoves.pokedexar.database.BoxData
import com.skydoves.pokedexar.database.BoxListService
import com.skydoves.pokedexar.database.DataIO
import com.skydoves.pokedexar.database.GachaService
import com.skydoves.pokedexar.databinding.ActivitySceneBinding
import com.skydoves.pokedexar.extensions.applyFullScreenWindow
import com.skydoves.pokedexar.ui.home.HomeViewModel
import dagger.hilt.android.AndroidEntryPoint
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

@AndroidEntryPoint
class ShopActivity : BindingActivity<ActivitySceneBinding>(R.layout.activity_shop) {

  private val viewModel by viewModels<ShopViewModel>()

  override fun onCreate(savedInstanceState: Bundle?) {
    applyFullScreenWindow()
    super.onCreate(savedInstanceState)

    val draw_btn = findViewById<ImageButton>(R.id.gacha1)

    val shineEffect = findViewById<ImageView>(R.id.shine_effect)
    ObjectAnimator.ofFloat(shineEffect, "rotation", 180f).apply{
      duration = 3000
      repeatCount = ValueAnimator.INFINITE
      start()
    }

    draw_btn.setOnClickListener {
      DataIO.updateUserAndDo(10000){
        println(it)
        println(it.message)
        val msg = it.message
        if(msg == "success"){
          DataIO.gachaAndDo {
            showDetailDialog(it)
            Toast.makeText(this@ShopActivity ,it.pokemon.name +"을(를) 뽑았다!", Toast.LENGTH_SHORT).show()
          }
        } else {
          Toast.makeText(this@ShopActivity ,"돈이 부족합니다", Toast.LENGTH_SHORT).show()
        }
      }

    }

  }

  companion object {
    fun startActivity(context: Context) {
      context.intentOf<ShopActivity> {
        startActivity(context)
      }
    }
  }

  fun showDetailDialog(box: BoxData){
    val dialog = Dialog(this)
    dialog.setContentView(R.layout.dialog_detail_example)

    val resourceId =
      resources.getIdentifier("pokemon${box.pokemon.id}", "drawable", packageName)
    dialog.findViewById<ImageView>(R.id.detail_img).setImageResource(resourceId)
    dialog.findViewById<TextView>(R.id.detail_name).text = box.pokemon.name

    dialog.findViewById<TextView>(R.id.detail_type1).text = box.pokemon.type1.name
    dialog.findViewById<TextView>(R.id.detail_type1)
      .setBackgroundColor(getTypeColor(box.pokemon.type1.name))
    Log.d("t1", "${getTypeColor(box.pokemon.type1.name)}")
    if (box.pokemon.type2.name == "None") {
      dialog.findViewById<TextView>(R.id.detail_type2).setVisibility(View.GONE)
    }
    dialog.findViewById<TextView>(R.id.detail_type2).text = box.pokemon.type2.name
    dialog.findViewById<TextView>(R.id.detail_type2)
      .setBackgroundColor(getTypeColor(box.pokemon.type2.name))

    dialog.findViewById<TextView>(R.id.detail_atk).text = box.pokemon.atk.toString()
    dialog.findViewById<TextView>(R.id.detail_def).text = box.pokemon.dfs.toString()
    dialog.findViewById<TextView>(R.id.detail_stk).text = box.pokemon.stk.toString()
    dialog.findViewById<TextView>(R.id.detail_sef).text = box.pokemon.sef.toString()
    dialog.findViewById<TextView>(R.id.detail_spd).text = box.pokemon.spd.toString()
    dialog.findViewById<TextView>(R.id.detail_hp).text = box.pokemon.hp.toString()

    dialog.findViewById<ProgressBar>(R.id.progress_hp).progress = box.pokemon.hp
    dialog.findViewById<ProgressBar>(R.id.progress_atk).progress = box.pokemon.atk
    dialog.findViewById<ProgressBar>(R.id.progress_def).progress = box.pokemon.dfs
    dialog.findViewById<ProgressBar>(R.id.progress_stk).progress = box.pokemon.stk
    dialog.findViewById<ProgressBar>(R.id.progress_sef).progress = box.pokemon.sef
    dialog.findViewById<ProgressBar>(R.id.progress_spd).progress = box.pokemon.spd

    dialog.findViewById<TextView>(R.id.detail_skill1).text = box.skill1.name
    dialog.findViewById<TextView>(R.id.detail_skill2).text = box.skill2.name
    dialog.findViewById<TextView>(R.id.detail_skill3).text = box.skill3.name
    dialog.findViewById<TextView>(R.id.detail_skill4).text = box.skill4.name

    dialog.findViewById<TextView>(R.id.detail_skill1)
      .setBackgroundColor(getTypeColor(box.skill1.type.name))
    dialog.findViewById<TextView>(R.id.detail_skill2)
      .setBackgroundColor(getTypeColor(box.skill2.type.name))
    dialog.findViewById<TextView>(R.id.detail_skill3)
      .setBackgroundColor(getTypeColor(box.skill3.type.name))
    dialog.findViewById<TextView>(R.id.detail_skill4)
      .setBackgroundColor(getTypeColor(box.skill4.type.name))

    dialog.findViewById<Button>(R.id.release_button).visibility = View.GONE


    dialog.findViewById<Button>(R.id.close_button).setOnClickListener {
      dialog.dismiss()
    }

    dialog.show()
  }

  fun getTypeColor(type: String): Int {
    return when (type) {
      "격투" -> R.color.fighting
      "비행" -> R.color.flying
      "독" -> 2137270149
      "땅" -> R.color.ground
      "바위" -> R.color.rock
      "벌레" -> R.color.bug
      "고스트" -> R.color.ghost
      "강철" -> R.color.steel
      "불" -> 2142380840
      "물" -> 2133215452
      "풀" -> 2130738242
      "전기" -> 2145445451
      "에스퍼" -> 2141989227
      "얼음" -> R.color.ice
      "드래곤" -> 2134346388
      "페어리" -> 2141067844
      "악" -> R.color.dark
      "노말" -> 2142348709
      else -> 2133535019
    }
  }
}
