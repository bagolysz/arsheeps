package com.bagolysz.arsheeps

import android.net.Uri
import android.os.Bundle
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
import com.google.ar.sceneform.math.Quaternion
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

    // dog object
    private var dog: Node? = null

    // sheep object
    private var sheep: Node? = null
    private var sheepDirection = Direction.FORWARD
    private var mayMove = false

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
            if (canPlaceSheeps && nodesPlaced < 1) {
                val anchor = hitResult.createAnchor()
                modelLoader.loadModel(anchor, Uri.parse(Utils.OBJ_SHEEP), true)
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
        changeDirection()
        showMessage(sheepDirection.name)
    }

    fun onFab2Click() {
        mayMove = !mayMove
    }

    private fun onUpdateFrame(frameTime: FrameTime?) {
        if (mayMove) {

            sheep?.let {sheep ->
                sheep.worldRotation = getRotationMatrix()
                sheep.worldPosition = getUpdatedPosition(sheep.worldPosition)

                dog?.let {dog ->
                    val error = distance(sheep.worldPosition, dog.worldPosition)

                    if (error < DIST_TH) {
                        changeDirection()
                    }
                }
            }

        }
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
                    if (!trackableSet.contains(image.name)) {
                        showMessage("adding object for ${image.name}")

                        trackableSet.add(image.name)
                        val anchor = image.createAnchor(image.centerPose)
                        modelLoader.loadModel(anchor, Uri.parse(getImageModelName(image.name)))

                        if (trackableSet.size == 1) {
                            canPlaceSheeps = true
                        }
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

    private fun showMessage(message: String) {
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
    }

    private fun logMe(message: String) {
        Log.d("ARXX", message)
    }

    // MODEL LOADER CALLBACKS
    fun addNodeToScene(anchor: Anchor, renderable: ModelRenderable, transformable: Boolean) {
        if (transformable) {
            val node = TransformableNode(transformationSystem)
            val anchorNode = AnchorNode(anchor)
            renderable.isShadowReceiver = false
            node.renderable = renderable
            node.scaleController.isEnabled = false
            node.rotationController.isEnabled = true
            node.translationController.isEnabled = true
            node.setParent(anchorNode)
            this.sheep = node
            arSceneView.scene.addChild(anchorNode)
        } else {
            //we may not use TransformableNode to improve the stability of the marker model
//            anchorNode.renderable = renderable
            val anchorNode = AnchorNode(anchor)
            renderable.isShadowReceiver = false
            val node = Node()
            node.renderable = renderable
            node.setParent(anchorNode)
            this.dog = node
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

    private fun distance(v1: Vector3, v2: Vector3): Float {
        val diff = Vector3.subtract(v1, v2)
        return sqrt(diff.x.pow(2) + diff.y.pow(2) + diff.z.pow(2))
    }

    // sheep object
    private fun getUpdatedPosition(oldPosition: Vector3): Vector3 {
        val newPosition = Vector3(oldPosition.x, oldPosition.y, oldPosition.z)

        when (sheepDirection) {
            Direction.FORWARD -> {
                newPosition.z += SPEED
            }
            Direction.BACK -> {
                newPosition.z -= SPEED
            }
            Direction.RIGHT -> {
                newPosition.x += SPEED
            }
            Direction.LEFT -> {
                newPosition.x -= SPEED
            }
        }

        return newPosition
    }

    private fun getRotationMatrix(): Quaternion {
        return when (sheepDirection) {
            Direction.FORWARD -> Quaternion.axisAngle(Vector3(0f, 1f, 0f), 0f)
            Direction.BACK -> Quaternion.axisAngle(Vector3(0f, 1f, 0f), 180f)
            Direction.RIGHT -> Quaternion.axisAngle(Vector3(0f, 1f, 0f), 90f)
            Direction.LEFT -> Quaternion.axisAngle(Vector3(0f, 1f, 0f), 270f)
        }
    }

    private fun changeDirection() {
        sheepDirection = when (sheepDirection) {
            Direction.FORWARD -> Direction.RIGHT
            Direction.RIGHT -> Direction.BACK
            Direction.BACK -> Direction.LEFT
            Direction.LEFT -> Direction.FORWARD
        }
    }

    enum class Direction {
        FORWARD,
        BACK,
        RIGHT,
        LEFT
    }

    companion object {

        const val SPEED = 0.0005f

        const val DIST_TH = 0.03f

    }
}