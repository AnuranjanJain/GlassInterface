package com.glassinterface.core.camera

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import java.io.BufferedInputStream
import java.io.ByteArrayInputStream
import java.io.DataInputStream
import java.io.IOException
import java.io.InputStream
import java.util.Properties

/**
 * Parses a continuous MJPEG (Motion JPEG) HTTP stream into discrete Bitmap frames.
 * Ideal for streaming video from an ESP32-CAM over local Wi-Fi.
 */
class MjpegInputStream(inputStream: InputStream) : DataInputStream(BufferedInputStream(inputStream, HEADER_MAX_LENGTH)) {

    companion object {
        private val SOI_MARKER = byteArrayOf(0xFF.toByte(), 0xD8.toByte())
        private val EOF_MARKER = byteArrayOf(0xFF.toByte(), 0xD9.toByte())
        private const val CONTENT_LENGTH = "Content-Length"
        private const val HEADER_MAX_LENGTH = 100000
        private const val FRAME_MAX_LENGTH = 100000 + HEADER_MAX_LENGTH
    }

    private var contentLen = -1

    @Throws(IOException::class)
    fun readMjpegFrame(): Bitmap? {
        mark(FRAME_MAX_LENGTH)
        var headerLen = getStartOfSequence(this, SOI_MARKER)
        reset()

        val header = ByteArray(headerLen)
        readFully(header)

        try {
            contentLen = parseContentLength(header)
            if (contentLen <= 0) {
                contentLen = getEndOfSequence(this, EOF_MARKER)
            }
        } catch (nfe: NumberFormatException) {
            contentLen = getEndOfSequence(this, EOF_MARKER)
        }

        reset()

        val frameData = ByteArray(contentLen)
        skipBytes(headerLen)
        readFully(frameData)

        return BitmapFactory.decodeStream(ByteArrayInputStream(frameData))
    }

    @Throws(IOException::class)
    private fun getStartOfSequence(inputStream: DataInputStream, sequence: ByteArray): Int {
        val seqLen = sequence.size
        val c = ByteArray(seqLen)
        for (i in 0 until seqLen) {
            c[i] = inputStream.readByte()
        }
        var len = seqLen
        while (!c.contentEquals(sequence)) {
            for (i in 0 until seqLen - 1) {
                c[i] = c[i + 1]
            }
            c[seqLen - 1] = inputStream.readByte()
            len++
        }
        return len
    }

    @Throws(IOException::class)
    private fun getEndOfSequence(inputStream: DataInputStream, sequence: ByteArray): Int {
        val seqLen = sequence.size
        val c = ByteArray(seqLen)
        for (i in 0 until seqLen) {
            c[i] = inputStream.readByte()
        }
        var len = seqLen
        while (!c.contentEquals(sequence)) {
            for (i in 0 until seqLen - 1) {
                c[i] = c[i + 1]
            }
            c[seqLen - 1] = inputStream.readByte()
            len++
            if (len >= FRAME_MAX_LENGTH) return -1
        }
        return len
    }

    @Throws(IOException::class, NumberFormatException::class)
    private fun parseContentLength(headerBytes: ByteArray): Int {
        val headerString = ByteArrayInputStream(headerBytes).bufferedReader().readText()
        val lines = headerString.split("\n", "\r\n")
        for (line in lines) {
            if (line.startsWith(CONTENT_LENGTH, ignoreCase = true)) {
                val parts = line.split(":")
                if (parts.size == 2) {
                    return parts[1].trim().toInt()
                }
            }
        }
        return 0
    }
}
