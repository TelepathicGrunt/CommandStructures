package com.telepathicgrunt.commandstructures;

import com.telepathicgrunt.commandstructures.mixin.ChunkMapAccessor;
import net.minecraft.network.protocol.game.ClientboundLevelChunkWithLightPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.LevelChunk;

import java.util.ServiceLoader;

public final class Utilities {
    private Utilities() {}

    public static <T> T loadService(Class<T> service) {
        return ServiceLoader.load(service, service.getClassLoader()).findFirst().orElseThrow(() -> new IllegalStateException("No platform implementation found for " + service.getName()));
    }

    public static void refreshChunksOnClients(ServerLevel level) {
        int viewDistance = ((ChunkMapAccessor)level.getChunkSource().chunkMap).getServerViewDistance();
        level.players().forEach(player -> {
            for(int x = -viewDistance; x <= viewDistance; x++) {
                for(int z = -viewDistance; z <= viewDistance; z++) {
                    if(x + z < viewDistance) {
                        ChunkAccess chunkAccess = level.getChunk(new ChunkPos(player.chunkPosition().x() + x, player.chunkPosition().z() + z).getWorldPosition());
                        if(chunkAccess instanceof LevelChunk levelChunk) {
                            new ClientboundLevelChunkWithLightPacket(levelChunk, level.getLightEngine(), null, null);
                        }
                    }
                }
            }
        });
    }
}
