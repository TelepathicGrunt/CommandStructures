package com.telepathicgrunt.commandstructures;

import net.minecraft.network.protocol.game.ClientboundLevelChunkWithLightPacket;
import net.minecraft.server.level.ChunkHolder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.chunk.LevelChunk;

public final class Utilities {
    private Utilities() {}

    public static void refreshChunksOnClients(ServerLevel level) {
        for(ChunkHolder chunkholder : level.getChunkSource().chunkMap.getChunks()) {
            LevelChunk levelChunk = chunkholder.getTickingChunk();
            if(levelChunk != null) {
                int viewDistance = level.getChunkSource().chunkMap.viewDistance;
                ClientboundLevelChunkWithLightPacket lightPacket = new ClientboundLevelChunkWithLightPacket(levelChunk, level.getLightEngine(), null, null, true);
                level.players().forEach(player -> {
                    int distance = player.chunkPosition().getChessboardDistance(levelChunk.getPos());
                    if(distance < viewDistance) {
                        player.trackChunk(levelChunk.getPos(), lightPacket);
                    }
                });
            }
        }
    }
}
