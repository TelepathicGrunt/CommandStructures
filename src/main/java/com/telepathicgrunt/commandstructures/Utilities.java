package com.telepathicgrunt.commandstructures;

import com.telepathicgrunt.commandstructures.mixin.ChunkMapAccessor;
import net.minecraft.network.protocol.game.ClientboundLevelChunkWithLightPacket;
import net.minecraft.server.level.ChunkHolder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.LevelChunk;

public final class Utilities {
    private Utilities() {}

    public static void refreshChunksOnClients(ServerLevel level) {
        int viewDistance = ((ChunkMapAccessor)level.getChunkSource().chunkMap).getViewDistance();
        level.players().forEach(player -> {
            for(int x = -viewDistance; x <= viewDistance; x++) {
                for(int z = -viewDistance; z <= viewDistance; z++) {
                    if(x + z < viewDistance) {
                        ChunkAccess chunkAccess = level.getChunk(new ChunkPos(player.chunkPosition().x + x, player.chunkPosition().z + z).getWorldPosition());
                        if(chunkAccess instanceof LevelChunk levelChunk) {
                            ClientboundLevelChunkWithLightPacket lightPacket = new ClientboundLevelChunkWithLightPacket(levelChunk, level.getLightEngine(), null, null, true);
                            player.untrackChunk(levelChunk.getPos());
                            player.trackChunk(levelChunk.getPos(), lightPacket);
                        }
                    }
                }
            }
        });
    }
}
