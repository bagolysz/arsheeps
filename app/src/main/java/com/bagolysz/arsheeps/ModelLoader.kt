package com.bagolysz.arsheeps

import android.net.Uri
import android.util.Log
import com.google.ar.core.Anchor
import com.google.ar.sceneform.rendering.ModelRenderable
import java.lang.ref.WeakReference

internal class ModelLoader(private val owner: WeakReference<EnhancedArFragment>) {

    fun loadModel(anchor: Anchor, uri: Uri, transformable : Boolean = false) {
        val context = owner.get()?.context
        if (context == null) {
            Log.d(TAG, "Parent fragment is null. Cannot load model.")
            return
        }
        ModelRenderable.builder()
            .setSource(context, uri)
            .build()
            .handle<Any> { renderable, throwable ->
                val parent = owner.get()
                when {
                    throwable != null -> parent?.onException(throwable)
                    else -> parent?.addNodeToScene(anchor, renderable, transformable)
                }
            }
    }

    companion object {
        private const val TAG = "ModelLoader"
    }
}