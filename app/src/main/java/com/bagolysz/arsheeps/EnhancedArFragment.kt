package com.bagolysz.arsheeps

import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.util.Log
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import com.google.ar.core.*
import com.google.ar.sceneform.AnchorNode
import com.google.ar.sceneform.FrameTime
import com.google.ar.sceneform.HitTestResult
import com.google.ar.sceneform.Node
import com.google.ar.sceneform.math.Vector3
import com.google.ar.sceneform.rendering.ModelRenderable
import com.google.ar.sceneform.ux.ArFragment
import com.google.ar.sceneform.ux.TransformableNode
import java.lang.ref.WeakReference
import kotlin.math.pow
import kotlin.math.sqrt


class EnhancedArFragment : ArFragment() {

    private lateinit var modelLoader: ModelLoader
    // to keep tracking which trackable that we have created AnchorNode with it or not.
    private val trackableSet = mutableSetOf<String>()
    private var nodesPlaced = 0
    private var canPlaceSheeps = false

    private var dog: Node? = null
    private var farm: Node? = null

    // sheep object
    private var sheeps: MutableList<Sheep> = mutableListOf()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = super.onCreateView(inflater, container, savedInstanceState)
        modelLoader = ModelLoader(WeakReference(this))

        // set touch event listener
        arSceneView.scene.setOnTouchListener(::onTouchEvent)

        // add frame update listener
        arSceneView.scene.addOnUpdateListener(::onUpdateFrame)

        // add plane tap listener
        setOnTapArPlaneListener { hitResult, plane, motionEvent ->
            if (canPlaceSheeps && nodesPlaced < 3) {
                val anchor = hitResult.createAnchor()
                modelLoader.loadModel(anchor, Uri.parse(Utils.OBJ_SHEEP), Utils.OBJ_SHEEP, true)
                nodesPlaced++
            }
        }

        return view
    }

    override fun getSessionConfiguration(session: Session): Config {
        val config = super.getSessionConfiguration(session)
        // make camera auto focus
        config.focusMode = Config.FocusMode.AUTO

        // load pre-build image database
        config.augmentedImageDatabase = AugmentedImageDatabase.deserialize(
            session,
            context!!.resources.assets.open(Utils.MARKERS)
        )
        return config
    }

    fun onFabClick() {
        canPlaceSheeps = true
    }

    fun onFab2Click() {
        sheeps.forEach {
            it.changeDirection()
            it.mayMove = !it.mayMove
            it.finished = false
        }
    }

    private fun onUpdateFrame(frameTime: FrameTime?) {
        if (dog != null && farm != null && sheeps.isNotEmpty()) {
            val dogPos = dog!!.worldPosition
            val farmPos = farm!!.worldPosition

            sheeps.filter { inScreen(it.node.worldPosition) }.forEach {
                it.update()
                val sheepPos = it.node.worldPosition

                if (distance(dogPos, sheepPos) < DOG_DIST_TH) {
                    it.changeDirection()
                    it.mayChangeDirection = false
                    Handler().postDelayed(
                        { it.mayChangeDirection = true }, CHANGE_DELAY_MS
                    )
                }

                if (!it.finished && distance(farmPos, sheepPos) < FARM_DIST_TH) {
                    it.mayMove = false
                    it.finished = true
                    showMessage("A sheep has arrived")
                }
            }
        }
    }

    private fun inScreen(worldPos: Vector3): Boolean {
        val screenPoint = arSceneView.scene.camera.worldToScreenPoint(worldPos)
        return (screenPoint.x > 0 && screenPoint.x <= arSceneView.width) && (
                screenPoint.y > 0 && screenPoint.y <= arSceneView.height)
    }

    private fun onTouchEvent(hitTestResult: HitTestResult, motionEvent: MotionEvent): Boolean {
        val frame = arSceneView.arFrame
        // If there is no frame or ARCore is not tracking yet, just return.
        if (frame == null || frame.camera.trackingState != TrackingState.TRACKING) {
            return false
        }

        // get detected AugmentedImages
        // there are three types of trackables, they are AugmentedImage, Plane and Point.
        frame.getUpdatedTrackables(AugmentedImage::class.java).forEach { image ->
            when (image.trackingState) {
                TrackingState.TRACKING -> {
                    // if it is in tracking state and we didn't add AnchorNode, then add one
                    if (trackableSet.size < 2 && !trackableSet.contains(image.name)) {
                        showMessage("Adding object ${image.name}")

                        trackableSet.add(image.name)
                        val anchor = image.createAnchor(image.centerPose)
                        modelLoader.loadModel(
                            anchor,
                            Uri.parse(getImageModelName(image.name)),
                            getImageModelName(image.name),
                            false
                        )
                    }
                }
                TrackingState.STOPPED -> {
                    // remove it
                    trackableSet.remove(image.name)
                }
                else -> {
                }
            }
        }

        return true
    }

    private fun getImageModelName(name: String): String {
        return when (name) {
            "dog.png" -> Utils.OBJ_DOG
            "farm.png" -> Utils.OBJ_FARM
            else -> Utils.OBJ_SHEEP
        }
    }

    private fun showMessage(message: String?) {
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
    }

    private fun distance(v1: Vector3, v2: Vector3): Float {
        val diff = Vector3.subtract(v1, v2)
        return sqrt(diff.x.pow(2) + diff.z.pow(2))
    }

    // MODEL LOADER CALLBACKS
    fun addNodeToScene(
        anchor: Anchor,
        renderable: ModelRenderable,
        objName: String,
        transformable: Boolean
    ) {
        if (transformable) {
            val node = TransformableNode(transformationSystem)
            val anchorNode = AnchorNode(anchor)
            renderable.isShadowReceiver = false
            node.renderable = renderable
            node.scaleController.isEnabled = false
            node.rotationController.isEnabled = true
            node.translationController.isEnabled = true
            node.setParent(anchorNode)

            sheeps.add(Sheep(node))
            arSceneView.scene.addChild(anchorNode)
        } else {
            //we may not use TransformableNode to improve the stability of the marker model
            val anchorNode = AnchorNode(anchor)
            renderable.isShadowReceiver = false
            val node = Node()
            node.renderable = renderable
            node.setParent(anchorNode)

            if (objName == Utils.OBJ_FARM) {
                farm = node
            } else if (objName == Utils.OBJ_DOG) {
                dog = node
            }

            arSceneView.scene.addChild(anchorNode)
        }
    }

    fun onException(throwable: Throwable) {
        val builder = AlertDialog.Builder(context!!)
        builder.setMessage(throwable.message).setTitle("Oops, something went wrong!")
        val dialog = builder.create()
        dialog.show()
        return
    }

    private fun logMe(message: String) {
        Log.d("ARXX", message)
    }

    companion object {

        const val FARM_DIST_TH = 0.12f
        const val DOG_DIST_TH = 0.08f
        const val CHANGE_DELAY_MS = 1000L

    }
}