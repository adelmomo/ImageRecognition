package com.example.ImageRecognition

import android.content.Context
import android.content.res.AssetManager
import android.graphics.Bitmap
import java.io.BufferedReader
import java.io.InputStreamReader

class ImageRecognizer {
    companion object {
        val labels = mutableMapOf<Int, String>()

        init {
            System.loadLibrary("ImageRecognition")
        }

        external fun classifierInit(manager: AssetManager)
        external fun classify(image: Bitmap): FloatArray
        fun createLabels(context: Context){

            // Use AssetManager to open the file
            val assetManager = context.assets
            val inputStream = assetManager.open("labels.txt")
            val reader = BufferedReader(InputStreamReader(inputStream))
            var idx=0
            // Read the contents of the file
            reader.useLines { lines ->
                lines.forEach { line ->

                    val description = line

                    // Add to the map
                    labels[idx++] = description
                }
            }

        }
    }


}