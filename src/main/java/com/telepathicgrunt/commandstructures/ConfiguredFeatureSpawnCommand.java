package com.telepathicgrunt.commandstructures;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.LongArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.tree.LiteralCommandNode;
import net.minecraft.commands.CommandRuntimeException;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.ResourceLocationArgument;
import net.minecraft.commands.arguments.coordinates.Coordinates;
import net.minecraft.commands.arguments.coordinates.Vec3Argument;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.data.worldgen.ProcessorLists;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.network.protocol.game.ClientboundLevelChunkWithLightPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ChunkHolder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.levelgen.LegacyRandomSource;
import net.minecraft.world.level.levelgen.RandomSupport;
import net.minecraft.world.level.levelgen.WorldgenRandom;
import net.minecraft.world.level.levelgen.XoroshiroRandomSource;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import net.minecraft.world.level.levelgen.feature.configurations.JigsawConfiguration;
import net.minecraft.world.level.levelgen.feature.structures.JigsawPlacement;
import net.minecraft.world.level.levelgen.feature.structures.SinglePoolElement;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.PoolElementStructurePiece;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.pieces.PieceGenerator;
import net.minecraft.world.level.levelgen.structure.pieces.PieceGeneratorSupplier;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePiecesBuilder;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureProcessorList;

import java.util.BitSet;
import java.util.Optional;
import java.util.Set;
import java.util.function.Supplier;

public class ConfiguredFeatureSpawnCommand {
    public static void dataGenCommand(CommandDispatcher<CommandSourceStack> dispatcher) {
        String commandString = "spawnfeature";
        String locationArg = "location";
        String rlArg = "configuredfeatureresourcelocation";

        LiteralCommandNode<CommandSourceStack> source = dispatcher.register(Commands.literal(commandString)
            .requires((permission) -> permission.hasPermission(2))
            .then(Commands.argument(locationArg, Vec3Argument.vec3())
            .then(Commands.argument(rlArg, ResourceLocationArgument.id())
            .suggests((ctx, sb) -> SharedSuggestionProvider.suggestResource(configuredFeatureSuggestions(ctx), sb))
            .executes(cs -> {
                generateStructure(Vec3Argument.getCoordinates(cs, locationArg), cs.getArgument(rlArg, ResourceLocation.class), cs);
                return 1;
            }))));

        dispatcher.register(Commands.literal(commandString).redirect(source));
    }

    private static Set<ResourceLocation> configuredFeatureSuggestions(CommandContext<CommandSourceStack> cs) {
        return cs.getSource().getLevel().registryAccess().registryOrThrow(Registry.CONFIGURED_FEATURE_REGISTRY).keySet();
    }

    private static void generateStructure(Coordinates coordinates, ResourceLocation configuredFeatureRL, CommandContext<CommandSourceStack> cs) {
        ServerLevel level = cs.getSource().getLevel();
        BlockPos centerPos = coordinates.getBlockPos(cs.getSource());
        ConfiguredFeature<?, ?> cf = cs.getSource().registryAccess().registryOrThrow(Registry.CONFIGURED_FEATURE_REGISTRY).get(configuredFeatureRL);

        if(cf == null) {
            String errorMsg = configuredFeatureRL + " configuredfeature does not exist in registry";
            CommandStructuresMain.LOGGER.error(errorMsg);
            throw new CommandRuntimeException(new TextComponent(errorMsg));
        }

        cf.place(level, level.getChunkSource().getGenerator(), level.getRandom(), centerPos);

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
