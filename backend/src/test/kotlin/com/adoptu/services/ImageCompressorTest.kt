package com.adoptu.services

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.awt.image.BufferedImage
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import javax.imageio.ImageIO
import kotlin.test.assertTrue

class ImageCompressorTest {

    @TempDir
    lateinit var tempDir: File

    private fun createTestImage(width: Int, height: Int): ByteArray {
        val image = BufferedImage(width, height, BufferedImage.TYPE_INT_RGB)
        for (x in 0 until width) {
            for (y in 0 until height) {
                image.setRGB(x, y, (x + y) % 256)
            }
        }
        val outputStream = ByteArrayOutputStream()
        ImageIO.write(image, "jpg", outputStream)
        return outputStream.toByteArray()
    }

    @Test
    fun `compress reduces large image dimensions`() {
        val largeImage = createTestImage(2000, 1500)
        val inputStream = ByteArrayInputStream(largeImage)

        val result = ImageCompressor.compress(inputStream, "jpg")

        val compressedImage = ImageIO.read(ByteArrayInputStream(result.toByteArray()))
        assertTrue(compressedImage.width <= 1200)
        assertTrue(compressedImage.height <= 1200)
    }

    @Test
    fun `compress keeps small image dimensions unchanged`() {
        val smallImage = createTestImage(800, 600)
        val inputStream = ByteArrayInputStream(smallImage)

        val result = ImageCompressor.compress(inputStream, "jpg")

        val compressedImage = ImageIO.read(ByteArrayInputStream(result.toByteArray()))
        assertTrue(compressedImage.width <= 800)
        assertTrue(compressedImage.height <= 600)
    }

    @Test
    fun `compress outputs smaller file size`() {
        val largeImage = createTestImage(2000, 1500)
        val inputStream = ByteArrayInputStream(largeImage)

        val result = ImageCompressor.compress(inputStream, "jpg")

        assertTrue(result.size() < largeImage.size)
    }

    @Test
    fun `compress handles png format`() {
        val image = createTestImage(1000, 800)
        val inputStream = ByteArrayInputStream(image)

        val result = ImageCompressor.compress(inputStream, "png")

        val compressedImage = ImageIO.read(ByteArrayInputStream(result.toByteArray()))
        assertTrue(compressedImage.width <= 1200)
    }

    @Test
    fun `compress handles portrait orientation`() {
        val portraitImage = createTestImage(800, 1200)
        val inputStream = ByteArrayInputStream(portraitImage)

        val result = ImageCompressor.compress(inputStream, "jpg")

        val compressedImage = ImageIO.read(ByteArrayInputStream(result.toByteArray()))
        assertTrue(compressedImage.width <= 1200)
        assertTrue(compressedImage.height <= 1200)
    }
}
