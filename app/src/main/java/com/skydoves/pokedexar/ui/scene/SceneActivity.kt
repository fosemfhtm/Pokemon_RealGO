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

import android.app.Dialog
import android.content.Context
import android.media.MediaPlayer
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.TextView
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
import com.google.gson.Gson
import org.json.JSONArray
import kotlin.math.ceil

@AndroidEntryPoint
class SceneActivity : BindingActivity<ActivitySceneBinding>(R.layout.activity_scene) {

  private val viewModel by viewModels<HomeViewModel>()
  private lateinit var mSocket : Socket
  var mediaPlayer : MediaPlayer? = null

  override fun onCreate(savedInstanceState: Bundle?) {
    applyFullScreenWindow()
    super.onCreate(savedInstanceState)
    binding.lifecycleOwner = this
    binding.vm = viewModel

    mediaPlayer = MediaPlayer.create(this, R.raw.battle_bgm)
    mediaPlayer?.start()

    roomId = EasySharedPreference.Companion.getString("roomId", "Default")
    myId = EasySharedPreference.Companion.getString("myId", "Default")
    val startObj = JSONObject(EasySharedPreference.Companion.getString("startObject", "Default"))

    updatePokemon(startObj.getJSONArray("fights"))

    // init button interactions, hp, etc
    initializeUI()

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

        // initialize the global anchor with default rendering models.
        arSceneView.session.whatIfNotNull { session ->
          initializeModels(this, session)
        }
      }
    }
  }

  override fun onDestroy() {
    super.onDestroy()
    mediaPlayer?.release()
    mediaPlayer = null
  }

  private fun initializeModels(arFragment: ArFragment, session: Session) {
//    Log.d(null, session.allAnchors.size.toString())

    // allAnchor.isEmpty() 일 때마다 불러오는듯....??!!!!
//    if (session.allAnchors.isEmpty()) {
    if (session.allAnchors.size < 2) {
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
    } /*else if (session.allAnchors.size == 1) {
      var pose = Pose(floatArrayOf(0f, 0f, -1f), floatArrayOf(0f, 0f, 0f, 1f))
      var swapFighterAnchor = session.createAnchor(pose)
      swapFighterAnchor.apply {
        val swapPokemon = PokemonModels.getPokemonByName(swapPokemonName).
        copy(localPosition = swapPokemonPos).
        copy(direction = swapPokemonDir)
        ModelRenderer.renderObject(this@SceneActivity, swapPokemon) { renderable ->
          ModelRenderer.addGardenOnScene(arFragment, this, renderable, swapPokemon)
        }
      }
      }*/
  }

  // Update UI after receive response
  var onBattleResult = Emitter.Listener { args ->
    val obj = JSONObject(args[0].toString())

    val state = obj.getJSONObject("state")
    val stateKey = state.getString("key")
    val fightsObj = obj.getJSONArray("fights")

    showAnimation(obj)

   //swapLogic(obj)

    //updatePokemon(fightsObj)


  }

  lateinit var endDialog : Dialog

  var onBattleEnd = Emitter.Listener { args ->
    val obj = JSONObject(args[0].toString())
    val stateObj = obj.getJSONObject("state")
    val resultObj = obj.getJSONObject("result")

    val winnerId = stateObj.getString("winner")


    runOnUiThread {
      endDialog = Dialog(this@SceneActivity)
      endDialog.setContentView(R.layout.dialog_battle_end)
      endDialog.setCancelable(false)
      endDialog.show()

      endDialog.findViewById<TextView>(R.id.battle_end_winner).setText(winnerId)
    }
  }


  private fun initializeUI() {
    mSocket = SocketHandler.getSocket()
    mSocket.on("battle_result", onBattleResult)
    mSocket.on("battle_end", onBattleEnd)

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


  fun swapLogic(obj: JSONObject){
    val state = obj.getJSONObject("state")
    val stateKey = state.getString("key")
    val fightsObj = obj.getJSONArray("fights")

    if (stateKey == "default" || stateKey == "switch") {
      if (stateKey == "switch") {
        val swapPokemonObj = state.getJSONObject("switch")

        // 죽은 포켓몬과 다음 포켓몬 교체
        for (i in 0 until fightsObj.length()) {
          var fo = fightsObj.getJSONObject(i)
          if (fo.getString("result") == "die") {
            val deadPokemonId = fo.getString("id")
            // 죽은 포켓몬 AR 삭제
//            Log.d("ID", deadPokemonId + " " + myFighterId + " " + opFighterId)

            if (deadPokemonId == myFighterId) {
              myFighterName = swapPokemonObj.getString("name")
              myFighterAnchor.detach() // detach -> remove?
              opFighterAnchor.detach()

            } else if (deadPokemonId == opFighterId){
              opFighterName = swapPokemonObj.getString("name")
              myFighterAnchor.detach()
              opFighterAnchor.detach()
            } else {
              // ...
            }

            fightsObj.put(i, state.getJSONObject("switch"))
            updateInfo( fightsObj )

            runOnUiThread {
              val myhptext = "${ceil(myFighterHp).toInt()} / ${myFighterMaxHp.toInt()}"
              val ophptext = "${ceil(opFighterHp).toInt()} / ${opFighterMaxHp.toInt()}"
              binding.battleTextNameMe.setText(myFighterName) // 교체
              binding.battleTextNameOp.setText(opFighterName) // 교체
              binding.battleTextHpMe.setText(myhptext) // all
              binding.battleTextHpOp.setText(ophptext) // all
              binding.battleBarHpMe.max = myFighterMaxHp.toInt() // 교체
              binding.battleBarHpOp.max = opFighterMaxHp.toInt() // 교체 */
              binding.battleBarHpMe.setProgress(ceil(myFighterHp).toInt()) // all
              binding.battleBarHpOp.setProgress(ceil(opFighterHp).toInt()) // all

            }



          }
        }
      }
    } else if (stateKey == "end") {
      // End Battle
    } else {
      // ...
    }
  }

  fun updateInfo(fightsObj: JSONArray){
    val resultObj = fightsObj

    val fight1 = resultObj[0] as JSONObject
    val fight2 = resultObj[1] as JSONObject
    val id1 = fight1.get("ownerId")
    val id2 = fight2.get("ownerId")

    var res1 = Gson().fromJson(fight1.toString(), FightResult::class.java)
    var res2 = Gson().fromJson(fight2.toString(), FightResult::class.java)

    if(res1.attackOrder == 2) {
      res1 = Gson().fromJson(fight2.toString(), FightResult::class.java)
      res2 = Gson().fromJson(fight1.toString(), FightResult::class.java)
    }

    var toastText = ""
    // 내 포켓몬 찾기
    if (id2 == myId) {
      myFighterName = fight1.getString("name")
      opFighterName = fight2.getString("name")
      myFighterId = fight1.getString("id")
      opFighterId = fight2.getString("id")
      myFighterHp = fight1.getDouble("hp")
      opFighterHp = fight2.getDouble("hp")
      myFighterMaxHp = fight1.getDouble("maxHp")
      opFighterMaxHp = fight2.getDouble("maxHp")

      toastText = fight1.getString("effect")

    } else {
      myFighterName = fight2.getString("name")
      opFighterName = fight1.getString("name")
      myFighterId = fight2.getString("id")
      opFighterId = fight1.getString("id")
      myFighterHp = fight2.getDouble("hp")
      opFighterHp = fight1.getDouble("hp")
      myFighterMaxHp = fight2.getDouble("maxHp")
      opFighterMaxHp = fight1.getDouble("maxHp")

      var toastText = fight2.getString("effect")
    }

    val myhptext = "${ceil(myFighterHp).toInt()} / ${myFighterMaxHp.toInt()}"
    val ophptext = "${ceil(opFighterHp).toInt()} / ${opFighterMaxHp.toInt()}"
    val myhppercent = myFighterHp/myFighterMaxHp
    val ophppercent = opFighterHp/opFighterMaxHp
  }

  fun showAnimation(obj: JSONObject){

    val state = obj.getJSONObject("state")
    val stateKey = state.getString("key")
    val fightsObj = obj.getJSONArray("fights")

    val resultObj = fightsObj

    val fight1 = resultObj[0] as JSONObject
    val fight2 = resultObj[1] as JSONObject
    val id1 = fight1.get("ownerId")
    val id2 = fight2.get("ownerId")

    var res1 = Gson().fromJson(fight1.toString(), FightResult::class.java)
    var res2 = Gson().fromJson(fight2.toString(), FightResult::class.java)

    if(res1.attackOrder == 2) {
      res1 = Gson().fromJson(fight2.toString(), FightResult::class.java)
      res2 = Gson().fromJson(fight1.toString(), FightResult::class.java)
    }

    var toastText = ""
    // 내 포켓몬 찾기
    if (id2 == myId) {
      myFighterName = fight1.getString("name")
      opFighterName = fight2.getString("name")
      myFighterId = fight1.getString("id")
      opFighterId = fight2.getString("id")
      myFighterHp = fight1.getDouble("hp")
      opFighterHp = fight2.getDouble("hp")
      myFighterMaxHp = fight1.getDouble("maxHp")
      opFighterMaxHp = fight2.getDouble("maxHp")

      toastText = fight1.getString("effect")

    } else {
      myFighterName = fight2.getString("name")
      opFighterName = fight1.getString("name")
      myFighterId = fight2.getString("id")
      opFighterId = fight1.getString("id")
      myFighterHp = fight2.getDouble("hp")
      opFighterHp = fight1.getDouble("hp")
      myFighterMaxHp = fight2.getDouble("maxHp")
      opFighterMaxHp = fight1.getDouble("maxHp")

      var toastText = fight2.getString("effect")
    }

    val myhptext = "${ceil(myFighterHp).toInt()} / ${myFighterMaxHp.toInt()}"
    val ophptext = "${ceil(opFighterHp).toInt()} / ${opFighterMaxHp.toInt()}"
    val myhppercent = myFighterHp/myFighterMaxHp
    val ophppercent = opFighterHp/opFighterMaxHp

    runOnUiThread {
      /* binding.battleTextNameMe.setText(myFighterName) // 교체
      binding.battleTextNameOp.setText(opFighterName) // 교체
      binding.battleTextHpMe.setText(myhptext) // all
      binding.battleTextHpOp.setText(ophptext) // all
      binding.battleBarHpMe.setProgress(ceil(myFighterHp).toInt()) // all
      binding.battleBarHpOp.setProgress(ceil(opFighterHp).toInt()) // all
      binding.battleBarHpMe.max = myFighterMaxHp.toInt() // 교체
      binding.battleBarHpOp.max = opFighterMaxHp.toInt() // 교체 */

      binding.effectText.setText("${res1.name}의 ${res1.skillName}!")
      // 움찔

      Handler().postDelayed(
        {
          // 선공 스킬 이펙트
          binding.effectText.setText(res1.effect)

          if(res2.ownerId == myId){
            binding.battleTextHpOp.setText(ophptext) // all
            binding.battleBarHpOp.setProgress(ceil(opFighterHp).toInt()) // all

            if(opFighterHp == 0.0) {
              swapLogic(obj)
              return@postDelayed
            }

            // 만약 애니메이션 넣으면 여기에
          } else {
            binding.battleTextHpMe.setText(myhptext) // all
            binding.battleBarHpMe.setProgress(ceil(myFighterHp).toInt()) // all

            if(myFighterHp == 0.0) {
              swapLogic(obj)
              return@postDelayed
            }
          }

          Handler().postDelayed({
            var str3 = "${res2.name}의 ${res2.skillName}!" // or 교체
            binding.effectText.setText(str3)


            Handler().postDelayed({
              binding.effectText.setText(res2.effect)

              if(res1.ownerId == myId){
                binding.battleTextHpOp.setText(ophptext) // all
                binding.battleBarHpOp.setProgress(ceil(opFighterHp).toInt()) // all
                // binding.battleTextNameOp.setText(opFighterName) //
                // binding.battleBarHpOp.max = opFighterMaxHp.toInt() //

                if(opFighterHp == 0.0) {
                  swapLogic(obj)
                  return@postDelayed
                }

                // 만약 애니메이션 넣으면 여기에
              } else {
                binding.battleTextHpMe.setText(myhptext) // all
                binding.battleBarHpMe.setProgress(ceil(myFighterHp).toInt()) // all
                //binding.battleTextNameMe.setText(myFighterName)
                //binding.battleBarHpMe.max = myFighterMaxHp.toInt()

                if(myFighterHp == 0.0) {
                  swapLogic(obj)
                  return@postDelayed
                }
              }

            }, 1000)
          }, 1000)
        }, 1000)

      println(res1.effect)

      println("${res2.name}의 ${res2.skillName}!")
      println(res2.effect)

    }



  }


  fun updatePokemon(resultObj : JSONArray) {
    val fight1 = resultObj[0] as JSONObject
    val fight2 = resultObj[1] as JSONObject
    val id1 = fight1.get("ownerId")
    val id2 = fight2.get("ownerId")

    var res1 = Gson().fromJson(fight1.toString(), FightResult::class.java)
    var res2 = Gson().fromJson(fight2.toString(), FightResult::class.java)

    if(res1.attackOrder == 2) {
      res1 = Gson().fromJson(fight2.toString(), FightResult::class.java)
      res2 = Gson().fromJson(fight1.toString(), FightResult::class.java)
    }

    var toastText = ""
    // 내 포켓몬 찾기
    if (id2 == myId) {
      myFighterName = fight1.getString("name")
      opFighterName = fight2.getString("name")
      myFighterId = fight1.getString("id")
      opFighterId = fight2.getString("id")
      myFighterHp = fight1.getDouble("hp")
      opFighterHp = fight2.getDouble("hp")
      myFighterMaxHp = fight1.getDouble("maxHp")
      opFighterMaxHp = fight2.getDouble("maxHp")

      toastText = fight1.getString("effect")

    } else {
      myFighterName = fight2.getString("name")
      opFighterName = fight1.getString("name")
      myFighterId = fight2.getString("id")
      opFighterId = fight1.getString("id")
      myFighterHp = fight2.getDouble("hp")
      opFighterHp = fight1.getDouble("hp")
      myFighterMaxHp = fight2.getDouble("maxHp")
      opFighterMaxHp = fight1.getDouble("maxHp")

      var toastText = fight2.getString("effect")
    }

//      if (toastText != "") {
//        Toast.makeText(this, toastText, Toast.LENGTH_SHORT).show() }

      val yellowBarId = this.resources.getIdentifier("def_progressbar", "drawable", this.packageName)
      val redBarId = this.resources.getIdentifier("hp_progressbar", "drawable", this.packageName)
      val greenBarId = this.resources.getIdentifier("sef_progressbar","drawable", this.packageName)

      val yellowbar = this.resources.getDrawable(yellowBarId)
      val redbar = this.resources.getDrawable(redBarId)
      val greenbar = this.resources.getDrawable(greenBarId)

      val myhptext = "${ceil(myFighterHp).toInt()} / ${myFighterMaxHp.toInt()}"
      val ophptext = "${ceil(opFighterHp).toInt()} / ${opFighterMaxHp.toInt()}"
      val myhppercent = myFighterHp/myFighterMaxHp
      val ophppercent = opFighterHp/opFighterMaxHp

      runOnUiThread {
        binding.battleTextNameMe.setText(myFighterName)
        binding.battleTextNameOp.setText(opFighterName)
        binding.battleTextHpMe.setText(myhptext)
        binding.battleTextHpOp.setText(ophptext)
        binding.battleBarHpMe.setProgress(ceil(myFighterHp).toInt())
        binding.battleBarHpOp.setProgress(ceil(opFighterHp).toInt())
        binding.battleBarHpMe.max = myFighterMaxHp.toInt()
        binding.battleBarHpOp.max = opFighterMaxHp.toInt()

        binding.effectText.setText("${res1.name}의 ${res1.skillName}!")


        Handler().postDelayed(
          {
            binding.effectText.setText(res1.effect)

            Handler().postDelayed({
              var str3 = "${res2.name}의 ${res2.skillName}!"
               binding.effectText.setText(str3)


              Handler().postDelayed({
                binding.effectText.setText(res2.effect)



              }, 2000)
            }, 2000)
          }, 2000)

        println(res1.effect)

        println("${res2.name}의 ${res2.skillName}!")
        println(res2.effect)

      }

//      if (myhppercent < 0.2) {
//        binding.battleBarHpMe.progressDrawable = redbar
//      }
//      else if (myhppercent < 0.5) {
//        binding.battleBarHpMe.progressDrawable = yellowbar
//      }
//      else {
//        binding.battleBarHpMe.progressDrawable = greenbar
//      }
//
//      if (ophppercent < 0.2) {
//        binding.battleBarHpOp.progressDrawable = redbar
//      }
//      else if (myhppercent < 0.5) {
//        binding.battleBarHpOp.progressDrawable = yellowbar
//      }
//      else {
//        binding.battleBarHpOp.progressDrawable = greenbar
//      }


    }
  }
