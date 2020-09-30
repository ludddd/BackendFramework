package com.ludd.test_util

import io.ktor.utils.io.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream

suspend fun com.google.protobuf.GeneratedMessageV3.toInputChannel(): ByteReadChannel {
    val read = ByteArrayOutputStream(1024)
    withContext(Dispatchers.IO) {
        writeDelimitedTo(read)
    }
    return ByteReadChannel(read.toByteArray())
}

suspend fun ByteChannel.toInputStream(): ByteArrayInputStream {
    flush()
    val content = ByteArray(availableForRead)
    readAvailable(content, 0, content.size)
    return content.inputStream()
}
