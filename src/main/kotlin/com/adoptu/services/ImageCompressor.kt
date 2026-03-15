package com.adoptu.services

import java.awt.Image
import java.awt.image.BufferedImage
import java.io.ByteArrayOutputStream
import java.io.InputStream
import javax.imageio.ImageIO

object ImageCompressor {
    private const val MAX_WIDTH = 1200
    private const val MAX_HEIGHT = 1200
    private const val QUALITY = 0.8

    fun compress(inputStream: InputStream, format: String = "jpg"): ByteArrayOutputStream {
        val image = ImageIO.read(inputStream) ?: throw IllegalArgumentException("Invalid image data or unsupported format")
        val (newWidth, newHeight) = calculateDimensions(image.width, image.height)

        val resized = if (newWidth != image.width || newHeight != image.height) {
            val resizedImage = BufferedImage(newWidth, newHeight, BufferedImage.TYPE_INT_RGB)
            val graphics = resizedImage.createGraphics()
            graphics.drawImage(image.getScaledInstance(newWidth, newHeight, Image.SCALE_SMOOTH), 0, 0, null)
            graphics.dispose()
            resizedImage
        } else {
            if (image.type != BufferedImage.TYPE_INT_RGB) {
                val converted = BufferedImage(image.width, image.height, BufferedImage.TYPE_INT_RGB)
                converted.graphics.drawImage(image, 0, 0, null)
                converted
            } else {
                image
            }
        }

        val outputStream = ByteArrayOutputStream()
        val imageFormat = if (format.equals("png", ignoreCase = true)) "png" else "jpg"
        ImageIO.write(resized, imageFormat, outputStream)
        return outputStream
    }

    private fun calculateDimensions(width: Int, height: Int): Pair<Int, Int> {
        if (width <= MAX_WIDTH && height <= MAX_HEIGHT) {
            return width to height
        }

        val ratio = width.toFloat() / height.toFloat()
        return if (width > height) {
            MAX_WIDTH to (MAX_WIDTH / ratio).toInt()
        } else {
            (MAX_HEIGHT * ratio).toInt() to MAX_HEIGHT
        }
    }
}
