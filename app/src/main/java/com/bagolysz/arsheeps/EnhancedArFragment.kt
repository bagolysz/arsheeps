package com.bagolysz.arsheeps

import android.net.Uri
import android.os.Bundle
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
import com.google.ar.sceneform.rendering.ModelRenderable
import com.google.ar.sceneform.ux.ArFragment
import java.lang.ref.WeakReference

class EnhancedArFragment : ArFragment() {

    // to keep tracking which trackable that we have created AnchorNode with it or not.
    private val trackableSet = mutableSetOf<String>()
    private lateinit var modelLoader: ModelLoader

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = super.onCreateView(inflater, container, savedInstanceState)
        modelLoader = ModelLoader(WeakReference(this))

        // Turn off the plane discovery since we're only looking for ArImages
        //planeDiscoveryController.hide()
        //planeDiscoveryController.setInstructionView(null)
        //arSceneView.planeRenderer.isEnabled = false

        // set touch event listener
        arSceneView.scene.setOnTouchListener(::onTouchEvent)

        // add frame update listener
        arSceneView.scene.addOnUpdateListener(::onUpdateFrame)

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

    private fun onUpdateFrame(frameTime: FrameTime?) {
        // we will add anchor here later
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
                        trackableSet.add(image.name)
                        val anchor = image.createAnchor(image.centerPose)
                        modelLoader.loadModel(anchor, Uri.parse(getImageModelName(image.name)))
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
        showMessage("creating object for: $name")
        return when (name) {
            "dog.png" -> Utils.OBJ_DOG
            "farm.png" -> Utils.OBJ_FARM
            else -> Utils.OBJ_SHEEP
        }
    }

    private fun showMessage(message: String) {
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
    }

    // MODEL LOADER CALLBACKS
    fun addNodeToScene(anchor: Anchor, renderable: ModelRenderable) {
        //we don't use TransformableNode to improve the stability of the marker model
        //val node = TransformableNode(transformationSystem)
        val anchorNode = AnchorNode(anchor)
        renderable.isShadowReceiver = false
        anchorNode.renderable = renderable
//        val node = Node()
//        node.renderable = renderable
//        node.setParent(anchorNode)
        arSceneView.scene.addChild(anchorNode)
    }

    fun onException(throwable: Throwable) {
        val builder = AlertDialog.Builder(context!!)
        builder.setMessage(throwable.message).setTitle("Oops, something went wrong!")
        val dialog = builder.create()
        dialog.show()
        return
    }
}