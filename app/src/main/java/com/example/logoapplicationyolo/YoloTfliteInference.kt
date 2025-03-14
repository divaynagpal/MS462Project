package com.example.logoapplicationyolo

import android.content.res.AssetManager
import android.graphics.Bitmap
import android.util.Log
import org.tensorflow.lite.Interpreter
import org.tensorflow.lite.Tensor
import java.io.FileInputStream
import java.io.IOException
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel
import java.util.Arrays


class YoloTfliteInference(private val modelPath: String, assetManager: AssetManager) {

    private var interpreter: Interpreter? = null
    private val merchantLabels = listOf(
        "NIKE", "Starbucks"
                )


                init {
        try {
            interpreter = Interpreter(loadModelFile(modelPath, assetManager))
            val outputTensor: Tensor = interpreter!!.getOutputTensor(0)
            val shape = outputTensor.shape()
            Log.d("TFLite", "Output shape: " + Arrays.toString(shape));
        } catch (e: IOException) {
            Log.e("TFLite", "Error loading model: ${e.message}")
        }
    }

   /* private fun loadModelFile(modelPath: String): MappedByteBuffer {
        val fileDescriptor = FileInputStream(modelPath).channel
        return fileDescriptor.map(FileChannel.MapMode.READ_ONLY, 0, fileDescriptor.size())
    }*/

    private fun loadModelFile(modelPath: String, assetManager: AssetManager): MappedByteBuffer {
        val assetFileDescriptor = assetManager.openFd(modelPath)
        val inputStream = FileInputStream(assetFileDescriptor.fileDescriptor)
        val fileChannel = inputStream.channel
        return fileChannel.map(FileChannel.MapMode.READ_ONLY, assetFileDescriptor.startOffset, assetFileDescriptor.declaredLength)
    }


    fun runInference(bitmap: Bitmap): String {
        val inputBuffer = preprocessImage(bitmap)
        val outputBuffer = Array(1) { Array(6) { FloatArray(8400) } }

        interpreter?.run(inputBuffer, outputBuffer)

        return processOutput(outputBuffer)
    }

    private fun preprocessImage(bitmap: Bitmap): ByteBuffer {
        val resizedBitmap = Bitmap.createScaledBitmap(bitmap, 640, 640, true)
        val inputBuffer = ByteBuffer.allocateDirect(4 * 640 * 640 * 3).apply {
            order(ByteOrder.nativeOrder())
        }

        val intValues = IntArray(640 * 640)
        resizedBitmap.getPixels(intValues, 0, 640, 0, 0, 640, 640)

        for (pixelValue in intValues) {
            val r = (pixelValue shr 16 and 0xFF) / 255.0f
            val g = (pixelValue shr 8 and 0xFF) / 255.0f
            val b = (pixelValue and 0xFF) / 255.0f
            inputBuffer.putFloat(r)
            inputBuffer.putFloat(g)
            inputBuffer.putFloat(b)
        }

        return inputBuffer
    }

   /* private fun processOutput(outputBuffer: Array<FloatArray>): String {
        print(outputBuffer)
        val maxIndex = outputBuffer[0].indices.maxByOrNull { outputBuffer[0][it] } ?: -1
        return "Detected Merchant: Merchant_$maxIndex"
    }*/

    private fun processOutput(outputBuffer: Array<Array<FloatArray>>): String {
        // Extract confidence scores and bounding box information
        var bestIndex = -1
        var bestConfidence = 0f
        var detectedClass = -1
        var selectedClassProb: List<Float> = mutableListOf()
        val detectedClasses = mutableSetOf<Int>()
        println(outputBuffer.contentDeepToString())

        println(outputBuffer[0])
        //for (i in outputBuffer[0][4].indices) {
        for (i in outputBuffer[0][4].indices) {
            val confidence = outputBuffer[0][4][i] // Confidence score
            val classProbabilities = outputBuffer[0].sliceArray(5 until outputBuffer[0].size).map { it[i] } // Get class probabilities
            //val classIndex = outputBuffer[0][5][i].toInt() // Class index
            val classIndex = classProbabilities.indexOf(classProbabilities.maxOrNull()) // Find the most probable class

            //println("Class Probabilities: $classProbabilities")
            if (classIndex != -1) {
                detectedClasses.add(classIndex) // Store unique class index
            }
            //println("class index : $classIndex")
            //println("confidence : $confidence")
            if (confidence > bestConfidence) {
                bestConfidence = confidence
                bestIndex = i
                detectedClass = classIndex
                selectedClassProb = classProbabilities
            }
        }

        println(detectedClass)
        println(detectedClasses)
        println(selectedClassProb)
        return if (bestIndex != -1 && detectedClass in merchantLabels.indices) {
            if (bestConfidence > 0.5){
                println("Detected Merchant: ${merchantLabels[0]} with confidence: $bestConfidence")
                "Detected Merchant: ${merchantLabels[0]}"
            }
            else {
                println("Detected Merchant: ${merchantLabels[1]} with confidence: " + (1-bestConfidence))
                "Detected Merchant: ${merchantLabels[1]}"
            }
        } else {
            "No merchant detected."
        }
    }

    /*private fun processOutput(outputBuffer: Array<Array<FloatArray>>): String {
        var bestIndex = -1
        var bestConfidence = 0f
        var detectedClass = -1
        var bestClassProbability = 0f

        println(outputBuffer.contentDeepToString())

        for (i in outputBuffer[0][4].indices) {
            val confidence = outputBuffer[0][4][i] // Confidence score

            // Get the probability of Class 0
            val class0Prob = outputBuffer[0][5][i]
            // Compute the probability of Class 1
            val class1Prob = 1 - class0Prob

            // Determine the class with the highest probability
            val (classIndex, maxProbability) = if (class0Prob > class1Prob) {
                0 to class0Prob
            } else {
                1 to class1Prob
            }

            // Update best detected class based on confidence and class probability
            if (confidence * maxProbability > bestConfidence * bestClassProbability) {
                bestConfidence = confidence
                bestClassProbability = maxProbability
                bestIndex = i
                detectedClass = classIndex
            }
        }

        println("Detected Class: $detectedClass")
        println("Best Class Probability: $bestClassProbability")

        return if (bestIndex != -1 && detectedClass in merchantLabels.indices) {
            println("Detected Merchant: ${merchantLabels[detectedClass]} with confidence: $bestConfidence and probability: $bestClassProbability")
            "Detected Merchant: ${merchantLabels[detectedClass]}"
        } else {
            "No merchant detected."
        }
    }*/

}

