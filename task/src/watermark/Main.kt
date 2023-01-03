package watermark

import java.awt.Color
import java.awt.Transparency
import java.awt.image.BufferedImage
import java.io.File
import java.lang.IllegalArgumentException
import javax.imageio.ImageIO
import kotlin.system.exitProcess

fun main() {
    WaterMark.console()
}

object WaterMark {
    private var isImageExists = false
    private var isWatermarkExists = false
    private val approvedExtensions = listOf("jpg", "png")
    var outputFileName: String = "output.png"
        set(value) {
            val fileExtension = value.split(".").last().lowercase()
            if (fileExtension !in approvedExtensions) {
                println("The output file extension isn't \"jpg\" or \"png\".")
                exitProcess(0)
            }
            field = value
        }
    var transparencyLevel = 0
        set(value) = when (value) {
            in 0..100 -> field = value
            else -> {
                println("The transparency percentage is out of range.")
                exitProcess(0)
            }
        }
    var transparentColor = Color(0, 0, 0)
    private var image: BufferedImage = BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB)
        set(value) {
            if (value.colorModel.numColorComponents != 3) {
                println("The number of image color components isn't 3.")
                exitProcess(0)
            }
            if (value.colorModel.pixelSize !in intArrayOf(24, 32)) {
                println("The image isn't 24 or 32-bit.")
                exitProcess(0)
            }
            field = value
        }
    private var watermark: BufferedImage = BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB)
        set(value) {
            if (value.colorModel.numColorComponents != 3) {
                println("The number of watermark color components isn't 3.")
                exitProcess(0)
            }
            if (value.colorModel.pixelSize !in intArrayOf(24, 32)) {
                println("The watermark isn't 24 or 32-bit.")
                exitProcess(0)
            }
            imageDifferWatermark.x = dimensionDifference(image, value)[0]
            imageDifferWatermark.y = dimensionDifference(image, value)[1]
            if (!imageDifferWatermark.isPositive()) {
                println("The watermark's dimensions are larger.")
                exitProcess(0)
            }
            field = value
        }
    var imageFileName: String = ""
        set(value) {
            field = value
            val file = File(value)
            isImageExists = file.exists()
            if (!isImageExists) {
                println("The file $imageFileName doesn't exist.")
                exitProcess(0)
            }
            image = ImageIO.read(file)
        }
    var watermarkFileName: String = ""
        set(value) {
            field = value
            val file = File(value)
            isWatermarkExists = file.exists()
            if (!isWatermarkExists) {
                println("The file $watermarkFileName doesn't exist.")
                exitProcess(0)
            }
            watermark = ImageIO.read(file)
        }
    private var useAlpha: Boolean = false
    private var useCustomAlpha = false

    class Position {
        var x: Int = 0
        var y: Int = 0
        fun isPositive(): Boolean {
            return x > 0 && y > 0
        }
    }

    private var watermarkPosition: Position = Position()
    private var imageDifferWatermark: Position = Position()

    enum class PositionMethod {
        SINGLE, GRID, NULL
    }

    var positionMethod = PositionMethod.NULL

    private fun dimensionDifference(
        image1: BufferedImage, image2: BufferedImage
    ) = listOf(image1.width - image2.width, image1.height - image2.height)

    fun process() {
        val output = BufferedImage(image.width, image.height, BufferedImage.TYPE_INT_RGB)
        for (x in 0 until output.width)
            for (y in 0 until output.height) {

                val i = Color(image.getRGB(x, y), useAlpha)

                val isWatermarkInArea =
                    x in watermarkPosition.x until watermarkPosition.x + watermark.width &&
                            y in watermarkPosition.y until watermarkPosition.y + watermark.height ||
                            positionMethod == PositionMethod.GRID

                if (isWatermarkInArea) {
                    val w = if (positionMethod == PositionMethod.GRID) {
                        Color(watermark.getRGB(x % watermark.width, y % watermark.height), useAlpha)
                    } else Color(watermark.getRGB(x - watermarkPosition.x, y - watermarkPosition.y), useAlpha)

                    val color = if (
                        (!useCustomAlpha && w.alpha == 255) ||
                        (useCustomAlpha && Color(w.red, w.green, w.blue) != transparentColor)
                    ) {
                        Color(
                            (transparencyLevel * w.red + (100 - transparencyLevel) * i.red) / 100,
                            (transparencyLevel * w.green + (100 - transparencyLevel) * i.green) / 100,
                            (transparencyLevel * w.blue + (100 - transparencyLevel) * i.blue) / 100
                        )
                    } else {
                        i
                    }
                    output.setRGB(x, y, color.rgb)
                } else {
                    output.setRGB(x, y, i.rgb)
                }
            }

        ImageIO.write(output, outputFileName.split(".").last(), File(outputFileName))
        println("The watermarked image $outputFileName has been created.")
    }

    fun console() {
        println("Input the image filename:")
        imageFileName = readln()
        println("Input the watermark image filename:")
        watermarkFileName = readln()
        if (watermark.transparency == Transparency.TRANSLUCENT) {
            println("Do you want to use the watermark's Alpha channel?")
            when (readln()) {
                "yes" -> useAlpha = true
            }
        } else {
            println("Do you want to set a transparency color?")
            when (readln()) {
                "yes" -> {
                    println("Input a transparency color ([Red] [Green] [Blue]):")
                    try {
                        val rgb = readln().split(" ").map { it.toInt() }
                        if (rgb.size != 3) throw IllegalArgumentException()
                        transparentColor = Color(rgb[0], rgb[1], rgb[2])
                    } catch (e: Exception) {
                        // NumberFormatException IllegalArgumentException IndexOutOfBoundsException
                        println("The transparency color input is invalid.")
                        exitProcess(0)
                    }

                    useCustomAlpha = true
                }
            }
        }
        println("Input the watermark transparency percentage (Integer 0-100):")
        try {
            transparencyLevel = readln().toInt()
        } catch (e: NumberFormatException) {
            println("The transparency percentage isn't an integer number.")
            exitProcess(0)
        }
        println("Choose the position method (single, grid):")
        when (readln()) {
            "single" -> {
                positionMethod = PositionMethod.SINGLE
                println("Input the watermark position ([x 0-${imageDifferWatermark.x}] [y 0-${imageDifferWatermark.y}]):")
                try {
                    val (diffX, diffY) = readln().split(" ").map { it.toInt() }
                    if (
                        diffX !in 0..imageDifferWatermark.x ||
                        diffY !in 0..imageDifferWatermark.y
                    ) {
                        println("The position input is out of range.")
                        exitProcess(0)
                    }
                    watermarkPosition.x = diffX
                    watermarkPosition.y = diffY
                } catch (e: Exception) {
                    println("The position input is invalid.")
                    exitProcess(0)
                }
            }

            "grid" -> {
                positionMethod = PositionMethod.GRID
            }

            else -> {
                println("The position method input is invalid.")
                exitProcess(0)
            }
        }
        println("Input the output image filename (jpg or png extension):")
        outputFileName = readln()
        process()
    }
}