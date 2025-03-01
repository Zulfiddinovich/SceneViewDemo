package com.nouman.sceneview

import android.content.DialogInterface
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.MotionEvent
import android.view.View
import androidx.appcompat.app.AlertDialog
import com.google.ar.core.exceptions.CameraNotAvailableException
import com.google.ar.sceneform.HitTestResult
import com.google.ar.sceneform.assets.RenderableSource
import com.google.ar.sceneform.rendering.ModelRenderable
import com.google.ar.sceneform.ux.FootprintSelectionVisualizer
import com.google.ar.sceneform.ux.TransformationSystem
import com.nouman.sceneview.SceneViewActivity.Statics.EXTRA_MODEL_TYPE
import com.nouman.sceneview.nodes.DragTransformableNode
import com.nouman.sceneview.databinding.ActivitySceneViewBinding
import java.lang.Exception
import java.util.concurrent.CompletionException

class SceneViewActivity : AppCompatActivity() {
    private lateinit var binding: ActivitySceneViewBinding;

    var remoteModelUrl =
          "https://raw.githubusercontent.com/KhronosGroup/glTF-Sample-Assets/main/Models/CarConcept/glTF-WEBP/CarConcept.gltf"  // good

    var localModel = "model.sfb"
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySceneViewBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val remoteModelUrl = intent.getStringExtra(EXTRA_MODEL_TYPE)
        if (remoteModelUrl.equals("remote")) {
            // load remote model
            renderRemoteObject()
        } else {
            // load local model
            renderLocalObject()
        }
    }

    private fun renderRemoteObject() {

        binding.skuProgressBar.setVisibility(View.VISIBLE)
        ModelRenderable.builder()
            .setSource(
                this, RenderableSource.Builder().setSource(
                    this,
                    Uri.parse(remoteModelUrl),
                    RenderableSource.SourceType.GLTF2
                ).setScale(0.15f)
                    .setRecenterMode(RenderableSource.RecenterMode.CENTER)
                    .build()
            )
            .setRegistryId(remoteModelUrl)
            .build()
            .thenAccept { modelRenderable: ModelRenderable ->
                binding.skuProgressBar.setVisibility(View.GONE)
                addNodeToScene(modelRenderable)
            }
            .exceptionally { throwable: Throwable? ->
                var message: String?
                message = if (throwable is CompletionException) {
                    binding.skuProgressBar.setVisibility(View.GONE)
                    "Internet is not working"
                } else {
                    binding.skuProgressBar.setVisibility(View.GONE)
                    "Can't load Model"
                }
                val mainHandler = Handler(Looper.getMainLooper())
                val finalMessage: String = message
                val myRunnable = Runnable {
                    AlertDialog.Builder(this)
                        .setTitle("Error")
                        .setMessage(finalMessage + "")
                        .setPositiveButton("Retry") { dialogInterface: DialogInterface, _: Int ->
                            renderRemoteObject()
                            dialogInterface.dismiss()
                        }
                        .setNegativeButton("Cancel") { dialogInterface, _ -> dialogInterface.dismiss() }
                        .show()
                }
                mainHandler.post(myRunnable)
                null
            }
    }

    private fun renderLocalObject() {

        binding.skuProgressBar.setVisibility(View.VISIBLE)
        ModelRenderable.builder()
            .setSource(this, Uri.parse(localModel))
            .setRegistryId(localModel)
            .build()
            .thenAccept { modelRenderable: ModelRenderable ->
                binding.skuProgressBar.setVisibility(View.GONE)
                addNodeToScene(modelRenderable)
            }
            .exceptionally { throwable: Throwable? ->
                var message: String?
                message = if (throwable is CompletionException) {
                    binding.skuProgressBar.setVisibility(View.GONE)
                    "Internet is not working"
                } else {
                    binding.skuProgressBar.setVisibility(View.GONE)
                    "Can't load Model"
                }
                val mainHandler = Handler(Looper.getMainLooper())
                val finalMessage: String = message
                val myRunnable = Runnable {
                    AlertDialog.Builder(this)
                        .setTitle("Error")
                        .setMessage(finalMessage + "")
                        .setPositiveButton("Retry") { dialogInterface: DialogInterface, _: Int ->
                            renderLocalObject()
                            dialogInterface.dismiss()
                        }
                        .setNegativeButton("Cancel") { dialogInterface, _ -> dialogInterface.dismiss() }
                        .show()
                }
                mainHandler.post(myRunnable)
                null
            }
    }

    override fun onPause() {
        super.onPause()
        binding.sceneView.pause()
    }

    private fun addNodeToScene(model: ModelRenderable) {
        val transformationSystem = makeTransformationSystem()
        val dragTransformableNode = DragTransformableNode(1f, transformationSystem)
        dragTransformableNode.renderable = model
        binding.sceneView.getScene().addChild(dragTransformableNode)
        dragTransformableNode.select()
        binding.sceneView.getScene()
            .addOnPeekTouchListener { hitTestResult: HitTestResult?, motionEvent: MotionEvent? ->
                transformationSystem.onTouch(
                    hitTestResult,
                    motionEvent
                )
            }
    }

    private fun makeTransformationSystem(): TransformationSystem {
        val footprintSelectionVisualizer = FootprintSelectionVisualizer()
        return TransformationSystem(resources.displayMetrics, footprintSelectionVisualizer)
    }


    override fun onResume() {
        super.onResume()
        try {
            binding.sceneView.resume()
        } catch (e: CameraNotAvailableException) {
            e.printStackTrace()
        }
    }

    object Statics {
        var EXTRA_MODEL_TYPE = "modelType"
    }

    override fun onDestroy() {
        super.onDestroy()
        try {

            binding.sceneView.destroy()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
