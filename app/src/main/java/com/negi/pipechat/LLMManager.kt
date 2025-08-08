package com.negi.pipechat

import android.content.Context
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.FutureCallback
import com.google.common.util.concurrent.MoreExecutors
import com.google.mediapipe.tasks.genai.llminference.LlmInference
import com.google.mediapipe.tasks.genai.llminference.LlmInference.LlmInferenceOptions
import java.io.File
import java.io.FileOutputStream

class LLMManager(context: Context, assetFileName: String = "gemma3-1b-it-int4.task") {
    private var llm: LlmInference? = null

    init {
        val outFile = File(context.filesDir, assetFileName)
        if (!outFile.exists()) {
            try {
                context.assets.open(assetFileName).use { input ->
                    FileOutputStream(outFile).use { output -> input.copyTo(output) }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        val options = LlmInferenceOptions.builder()
            .setModelPath(outFile.absolutePath)
            .setMaxTokens(512)
            .build()
        llm = LlmInference.createFromOptions(context, options)
    }

    fun generateResponseAsync(
        prompt: String,
        onResponse: (String) -> Unit
    ) {
        val future = llm?.generateResponseAsync(prompt)
        if (future == null) {
            onResponse("Error: LLM not initialized")
            return
        }
        Futures.addCallback(
            future,
            object : FutureCallback<String> {
                override fun onSuccess(result: String?) { onResponse(result.orEmpty()) }
                override fun onFailure(t: Throwable) { onResponse("Error: ${t.localizedMessage}") }
            },
            MoreExecutors.directExecutor()
        )
    }

    fun close() { llm?.close(); llm = null }
}
