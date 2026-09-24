package com.akito.million_egg

import androidx.datastore.core.CorruptionException
import androidx.datastore.core.Serializer
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import java.io.InputStream
import java.io.OutputStream

object PlayerStateSerializer : Serializer<PlayerState> {
    override val defaultValue: PlayerState = PlayerState()

    private val json = Json { ignoreUnknownKeys = true }

    override suspend fun readFrom(input: InputStream): PlayerState {
        try {
            return json.decodeFromString(
                PlayerState.serializer(),
                input.readBytes().decodeToString()
            )
        } catch (serialization: SerializationException) {
            throw CorruptionException("Unable to read PlayerState", serialization)
        }
    }

    override suspend fun writeTo(t: PlayerState, output: OutputStream) {
        output.write(
            json.encodeToString(PlayerState.serializer(), t).encodeToByteArray()
        )
    }
}

object GameProgressSerializer : Serializer<GameProgress> {
    override val defaultValue: GameProgress = GameProgress()

    private val json = Json { ignoreUnknownKeys = true }

    override suspend fun readFrom(input: InputStream): GameProgress {
        try {
            return json.decodeFromString(
                GameProgress.serializer(),
                input.readBytes().decodeToString()
            )
        } catch (serialization: SerializationException) {
            throw CorruptionException("Unable to read GameProgress", serialization)
        }
    }

    override suspend fun writeTo(t: GameProgress, output: OutputStream) {
        output.write(
            json.encodeToString(GameProgress.serializer(), t).encodeToByteArray()
        )
    }
}
