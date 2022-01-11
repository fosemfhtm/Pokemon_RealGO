/*
 * Designed and developed by 2020 skydoves (Jaewoong Eum)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.skydoves.pokedexar.ui.scene

import android.content.Context
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.viewModels
import androidx.databinding.ViewDataBinding
import com.google.ar.core.Pose
import com.google.ar.core.Session
import com.google.ar.core.TrackingState
import com.google.ar.sceneform.ux.ArFragment
import com.skydoves.bindables.BindingActivity
import com.skydoves.bundler.intentOf
import com.skydoves.pokedexar.R
import com.skydoves.pokedexar.databinding.ActivitySceneBinding
import com.skydoves.pokedexar.extensions.applyFullScreenWindow
import com.skydoves.pokedexar.extensions.findFragmentAs
import com.skydoves.pokedexar.ui.home.HomeViewModel
import com.skydoves.pokedexar.ui.room.SocketHandler
import com.skydoves.pokedexar_core.ModelRenderer
import com.skydoves.pokedexar_core.PokemonModels
import com.skydoves.whatif.whatIfNotNull
import dagger.hilt.android.AndroidEntryPoint
import io.socket.client.Socket
import io.socket.emitter.Emitter
import org.json.JSONObject
import com.amn.easysharedpreferences.EasySharedPreference
import com.google.ar.core.Anchor
import com.google.ar.sceneform.math.Vector3
import org.json.JSONArray
import kotlin.math.ceil

@AndroidEntryPoint
class SceneActivity : BindingActivity<ActivitySceneBinding>(R.layout.activity_scene) {

  private val viewModel by viewModels<HomeViewModel>()
  private lateinit var mSocket : Socket

  override fun onCreate(savedInstanceState: Bundle?) {
    applyFullScreenWindow()
    super.onCreate(savedInstanceState)
    binding.lifecycleOwner = this
    binding.vm = viewModel

    roomId = EasySharedPreference.Companion.getString("roomId", "Default")
    myId = EasySharedPreference.Companion.getString("myId", "Default")
    val startObj = JSONObject(EasySharedPreference.Companion.getString("startObject", "Default"))

    updatePokemon(startObj.getJSONArray("fights"))

    with(findFragmentAs<ArFragment>(R.id.arFragment)) {
      planeDiscoveryController.hide()
      planeDiscoveryController.setInstructionView(null)
      arSceneView.planeRenderer.isVisible = false
      arSceneView.scene.addOnUpdateListener {
        onUpdate(it)

        // checks the state of the AR frame is Tracking.
        val arFrame = arSceneView.arFrame ?: return@addOnUpdateListener
        if (arFrame.camera?.trackingState != TrackingState.TRACKING) {
          return@addOnUpdateListener
        }

        // init button interactions, hp, etc
        initializeUI()

        // initialize the global anchor with default rendering models.
        arSceneView.session.whatIfNotNull { session ->
          initializeModels(this, session)
        }
      }


    }
  }

  private fun initializeModels(arFragment: ArFragment, session: Session) {
    if (session.allAnchors.isEmpty() && !viewModel.isCaught) {
      var pose = Pose(floatArrayOf(0f, 0f, -1f), floatArrayOf(0f, 0f, 0f, 1f))
      myFighterAnchor = session.createAnchor(pose)
      myFighterAnchor.apply {
        val myPokemon = PokemonModels.getPokemonByName(myFighterName).
        copy(localPosition = PokemonModels.DEFAULT_POSITION_DETAILS_POKEMON1)
        ModelRenderer.renderObject(this@SceneActivity, myPokemon) { renderable ->
          ModelRenderer.addGardenOnScene(arFragment, this, renderable, myPokemon)
        }
      }

      pose = Pose(floatArrayOf(0f, 0f, -1f), floatArrayOf(0f, 0f, 0f, 1f))
      opFighterAnchor = session.createAnchor(pose)
      opFighterAnchor.apply {
        val opPokemon = PokemonModels.getPokemonByName(opFighterName).
        copy(localPosition = PokemonModels.DEFAULT_POSITION_DETAILS_POKEMON2).
        copy(direction = Vector3(0f, 0f, -1f))
        ModelRenderer.renderObject(this@SceneActivity, opPokemon) { renderable ->
          ModelRenderer.addGardenOnScene(arFragment, this, renderable, opPokemon)
        }
      }

      /*session.createAnchor(pose).apply {
        val pokemon1 = PokemonModels.getRandomPokemon().copy(localPosition = PokemonModels.DEFAULT_POSITION_DETAILS_POKEMON1)
        ModelRenderer.renderObject(this@SceneActivity, pokemon1) { renderable ->
          ModelRenderer.addPokemonOnScene(arFragment, this, renderable, pokemon1)
        }

        val pokemon2 = PokemonModels.getRandomPokemon().copy(localPosition = PokemonModels.DEFAULT_POSITION_DETAILS_POKEMON2)
        ModelRenderer.renderObject(this@SceneActivity, pokemon2) { renderable ->
          ModelRenderer.addPokemonOnScene(arFragment, this, renderable, pokemon2)
        }
      }*/
    }
  }

  // Update UI after receive response
  var onBattleResult = Emitter.Listener { args ->
    val obj = JSONObject(args[0].toString())

    val state = obj.getJSONObject("state")
    val stateKey = state.getString("key")
    val fightsObj = obj.getJSONArray("fights")
    if (stateKey == "default" || stateKey == "switch") {
      if (stateKey == "switch") {
        // 죽은 포켓몬과 다음 포켓몬 교체
        for (i in 0 until fightsObj.length()) {
          var fo = fightsObj.getJSONObject(i)
          if (fo.getString("result") == "die") {
            val deadPokemonId = fo.getString("id")
            // 죽은 포켓몬 AR 삭제
//            Log.d("ID", deadPokemonId + " " + myFighterId + " " + opFighterId)
            if (deadPokemonId == myFighterId) {
//              myFighterAnchor.detach() // detach -> remove?
            } else {
//              opFighterAnchor.detach()
            }

            fightsObj.put(i, state.getJSONObject("switch"))
          }
        }
      }
    } else if (stateKey == "end") {
      // End Battle
    } else {
      // ...
    }

    updatePokemon(fightsObj)
  }

  private fun initializeUI() {
    mSocket = SocketHandler.getSocket()
    mSocket.on("battle_result", onBattleResult)

    binding.battleBtnSkill1.setOnClickListener {
      val obj = JSONObject()
      obj.put("roomId", roomId)
      obj.put("skillIndex", 0)
      mSocket.emit("skill", obj)
    }
    binding.battleBtnSkill2.setOnClickListener {
      val obj = JSONObject()
      obj.put("roomId", roomId)
      obj.put("skillIndex", 1)
      mSocket.emit("skill", obj)
    }
    binding.battleBtnSkill3.setOnClickListener {
      val obj = JSONObject()
      obj.put("roomId", roomId)
      obj.put("skillIndex", 2)
      mSocket.emit("skill", obj)
    }
    binding.battleBtnSkill4.setOnClickListener {
      val obj = JSONObject()
      obj.put("roomId", roomId)
      obj.put("skillIndex", 3)
      mSocket.emit("skill", obj)
    }
  }

  companion object {
    fun startActivity(context: Context) {
      context.intentOf<SceneActivity> {
        startActivity(context)
      }
    }
  }

  lateinit var myId: String
  lateinit var roomId: String
  lateinit var myFighterName: String
  lateinit var opFighterName: String
  lateinit var myFighterId: String
  lateinit var opFighterId: String
  lateinit var myFighterAnchor: Anchor
  lateinit var opFighterAnchor: Anchor
  var myFighterHp: Double = 0.0
  var opFighterHp: Double = 0.0
  var myFighterMaxHp: Double = 0.0
  var opFighterMaxHp: Double = 0.0

  /*[{ownerId: p.id,
  id: p.fighter.id,
  hp: p.fighter.hp,
  name: p.fighter.name}, ...]*/
  fun updatePokemon(resultObj : JSONArray) {
    val fight1 = resultObj[0] as JSONObject
    val fight2 = resultObj[1] as JSONObject
    val id1 = fight1.get("ownerId")
    val id2 = fight2.get("ownerId")

    var toastText = ""
    // 내 포켓몬 찾기
    if (id1 == myId) {
      myFighterName = fight1.getString("name")
      opFighterName = fight2.getString("name")
      myFighterId = fight1.getString("id")
      opFighterId = fight2.getString("id")
      myFighterHp = fight1.getDouble("hp")
      myFighterMaxHp = fight1.getDouble("maxHp")
      opFighterHp = fight2.getDouble("hp")
      opFighterMaxHp = fight1.getDouble("maxHp")

      toastText = fight1.getString("effect")

    } else {
      myFighterName = fight2.getString("name")
      opFighterName = fight1.getString("name")
      myFighterId = fight2.getString("id")
      opFighterId = fight1.getString("id")
      myFighterHp = fight2.getDouble("hp")
      opFighterHp = fight1.getDouble("hp")

      var toastText = fight2.getString("effect")
    }

    Thread(Runnable {
//      if (toastText != "") {
//        Toast.makeText(this, toastText, Toast.LENGTH_SHORT).show()
//      }

    }).start()

    runOnUiThread {
//      if (toastText != "") {
//        Toast.makeText(this, toastText, Toast.LENGTH_SHORT).show() }
      binding.battleTextNameMe.setText(myFighterName)
      binding.battleTextNameOp.setText(opFighterName)
      binding.battleTextHpMe.setText(ceil(myFighterHp).toInt().toString())
      binding.battleTextHpOp.setText(ceil(opFighterHp).toInt().toString())
    }
  }
}
