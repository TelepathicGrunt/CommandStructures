package com.telepathicgrunt.commandstructures.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.LongArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.tree.LiteralCommandNode;
import com.telepathicgrunt.commandstructures.CommandStructuresMain;
import com.telepathicgrunt.commandstructures.Utilities;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandRuntimeException;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.ResourceLocationArgument;
import net.minecraft.commands.arguments.coordinates.Coordinates;
import net.minecraft.commands.arguments.coordinates.Vec3Argument;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.ProcessorLists;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.LegacyRandomSource;
import net.minecraft.world.level.levelgen.RandomSupport;
import net.minecraft.world.level.levelgen.WorldgenRandom;
import net.minecraft.world.level.levelgen.XoroshiroRandomSource;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.PoolElementStructurePiece;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.pools.JigsawPlacement;
import net.minecraft.world.level.levelgen.structure.pools.SinglePoolElement;
import net.minecraft.world.level.levelgen.structure.pools.StructureTemplatePool;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureProcessorList;

import java.util.List;
import java.util.Optional;
import java.util.Set;

public class StructureSpawnCommand {
    public static void createCommand(CommandDispatcher<CommandSourceStack> dispatcher, CommandBuildContext buildContext) {
        String commandString = "spawnstructure";
        String locationArg = "location";
        String poolArg = "startpoolresourcelocation";
        String depthArg = "depth";
        String heightmapArg = "heightmapsnap";
        String legacyBoundsArg = "legacyboundingboxrule";
        String disableProcessors = "disableprocessors";
        String sendChunkLightingPacket = "sendchunklightingpacket";
        String randomSeed = "randomseed";

        LiteralCommandNode<CommandSourceStack> source = dispatcher.register(Commands.literal(commandString)
            .requires((permission) -> permission.hasPermission(2))
            .then(Commands.argument(locationArg, Vec3Argument.vec3())
            .then(Commands.argument(poolArg, ResourceLocationArgument.id())
            .suggests((ctx, sb) -> SharedSuggestionProvider.suggestResource(startPoolSuggestions(ctx), sb))
            .executes(cs -> {
                generateStructure(Vec3Argument.getCoordinates(cs, locationArg), cs.getArgument(poolArg, ResourceLocation.class), 10, false, false, false, false, null, cs);
                return 1;
            })
            .then(Commands.argument(depthArg, IntegerArgumentType.integer())
            .executes(cs -> {
                generateStructure(Vec3Argument.getCoordinates(cs, locationArg), cs.getArgument(poolArg, ResourceLocation.class), cs.getArgument(depthArg, Integer.class), false, false, false, false, null, cs);
                return 1;
            })
            .then(Commands.argument(heightmapArg, BoolArgumentType.bool())
            .executes(cs -> {
                generateStructure(Vec3Argument.getCoordinates(cs, locationArg), cs.getArgument(poolArg, ResourceLocation.class), cs.getArgument(depthArg, Integer.class), cs.getArgument(heightmapArg, Boolean.class), false, false, false, null, cs);
                return 1;
            })
            .then(Commands.argument(legacyBoundsArg, BoolArgumentType.bool())
            .executes(cs -> {
                generateStructure(Vec3Argument.getCoordinates(cs, locationArg), cs.getArgument(poolArg, ResourceLocation.class), cs.getArgument(depthArg, Integer.class), cs.getArgument(heightmapArg, Boolean.class), cs.getArgument(legacyBoundsArg, Boolean.class), false, false, null, cs);
                return 1;
            })
            .then(Commands.argument(disableProcessors, BoolArgumentType.bool())
            .executes(cs -> {
                generateStructure(Vec3Argument.getCoordinates(cs, locationArg), cs.getArgument(poolArg, ResourceLocation.class), cs.getArgument(depthArg, Integer.class), cs.getArgument(heightmapArg, Boolean.class), cs.getArgument(legacyBoundsArg, Boolean.class), cs.getArgument(disableProcessors, Boolean.class), false, null, cs);
                return 1;
            })
            .then(Commands.argument(sendChunkLightingPacket, BoolArgumentType.bool())
            .executes(cs -> {
                generateStructure(Vec3Argument.getCoordinates(cs, locationArg), cs.getArgument(poolArg, ResourceLocation.class), cs.getArgument(depthArg, Integer.class), cs.getArgument(heightmapArg, Boolean.class), cs.getArgument(legacyBoundsArg, Boolean.class), cs.getArgument(disableProcessors, Boolean.class), cs.getArgument(sendChunkLightingPacket, Boolean.class), null, cs);
                return 1;
            })
            .then(Commands.argument(randomSeed, LongArgumentType.longArg())
            .executes(cs -> {
                generateStructure(Vec3Argument.getCoordinates(cs, locationArg), cs.getArgument(poolArg, ResourceLocation.class), cs.getArgument(depthArg, Integer.class), cs.getArgument(heightmapArg, Boolean.class), cs.getArgument(legacyBoundsArg, Boolean.class), cs.getArgument(disableProcessors, Boolean.class), cs.getArgument(sendChunkLightingPacket, Boolean.class), cs.getArgument(randomSeed, Long.class), cs);
                return 1;
            })
        )))))))));

        dispatcher.register(Commands.literal(commandString).redirect(source));
    }

    private static Set<ResourceLocation> startPoolSuggestions(CommandContext<CommandSourceStack> cs) {
        return cs.getSource().getLevel().registryAccess().registryOrThrow(Registries.TEMPLATE_POOL).keySet();
    }

    private static void generateStructure(Coordinates coordinates, ResourceLocation structureStartPoolRL, int depth, boolean heightmapSnap, boolean legacyBoundingBoxRule, boolean disableProcessors, boolean sendChunkLightingPacket, Long randomSeed, CommandContext<CommandSourceStack> cs) {
        ServerLevel level = cs.getSource().getLevel();
        BlockPos centerPos = coordinates.getBlockPos(cs.getSource());
        if(heightmapSnap) centerPos = centerPos.below(centerPos.getY()); //not a typo. Needed so heightmap is not offset by player height.

        StructureTemplatePool templatePool = level.registryAccess().registryOrThrow(Registries.TEMPLATE_POOL).get(structureStartPoolRL);

        if(templatePool == null || templatePool.size() == 0) {
            String errorMsg = structureStartPoolRL + " template pool does not exist or is empty";
            CommandStructuresMain.LOGGER.error(errorMsg);
            throw new CommandRuntimeException(Component.translatable(errorMsg));
        }

        long finalSeed = randomSeed == null ? level.getSeed() : randomSeed;
        ChunkPos chunkPos = randomSeed == null ? new ChunkPos(centerPos) : new ChunkPos(0, 0);
        Structure.GenerationContext newGenerationContext = new Structure.GenerationContext(
                level.registryAccess(),
                level.getChunkSource().getGenerator(),
                level.getChunkSource().getGenerator().getBiomeSource(),
                level.getChunkSource().randomState(),
                level.getStructureManager(),
                finalSeed,
                chunkPos,
                level,
                (biomeHolder) -> true
        );

        Optional<Structure.GenerationStub> pieceGenerator = JigsawPlacement.addPieces(
                newGenerationContext,
                Holder.direct(templatePool),
                Optional.empty(),
                depth,
                centerPos,
                legacyBoundingBoxRule,
                heightmapSnap ? Optional.of(Heightmap.Types.WORLD_SURFACE_WG) : Optional.empty(),
                80);

        if(pieceGenerator.isPresent()) {
            WorldgenRandom worldgenrandom;
            if(randomSeed == null) {
                worldgenrandom = new WorldgenRandom(new XoroshiroRandomSource(RandomSupport.generateUniqueSeed()));
                long i = worldgenrandom.setDecorationSeed(finalSeed, centerPos.getX(), centerPos.getZ());
                worldgenrandom.setFeatureSeed(i, 0, 0);
            }
            else {
                worldgenrandom = new WorldgenRandom(new LegacyRandomSource(randomSeed));
            }

            BlockPos finalCenterPos = centerPos;
            List<StructurePiece> structurePieceList = pieceGenerator.get().getPiecesBuilder().build().pieces();


            structurePieceList.forEach(piece -> {
                if(disableProcessors) {
                    if(piece instanceof PoolElementStructurePiece poolElementStructurePiece) {
                        if(poolElementStructurePiece.getElement() instanceof SinglePoolElement singlePoolElement) {
                            Holder<StructureProcessorList> oldProcessorList = singlePoolElement.processors;
                            ResourceKey<StructureProcessorList> emptyKey = ResourceKey.create(Registries.PROCESSOR_LIST, new ResourceLocation("minecraft", "empty"));
                            Optional<Holder.Reference<StructureProcessorList>> emptyProcessorList = cs.getSource().getLevel().registryAccess().registryOrThrow(Registries.PROCESSOR_LIST).getHolder(emptyKey);
                            singlePoolElement.processors = emptyProcessorList.get();
                            generatePiece(level, level.getChunkSource().getGenerator(), chunkPos, worldgenrandom, finalCenterPos, piece);
                            singlePoolElement.processors = oldProcessorList; // Set the processors back or else our change is permanent.
                        }
                    }
                }
                else {
                    generatePiece(level, level.getLevel().getChunkSource().getGenerator(), chunkPos, worldgenrandom, finalCenterPos, piece);
                }
            });

            if(!structurePieceList.isEmpty()) {
                if(sendChunkLightingPacket) {
                    Utilities.refreshChunksOnClients(level);
                }
            }
            else {
                String errorMsg = structureStartPoolRL + " Template Pool spawned no pieces.";
                CommandStructuresMain.LOGGER.error(errorMsg);
                throw new CommandRuntimeException(Component.translatable(errorMsg));
            }
        }
        else {
            String errorMsg = structureStartPoolRL + " Template Pool spawned no pieces.";
            CommandStructuresMain.LOGGER.error(errorMsg);
            throw new CommandRuntimeException(Component.translatable(errorMsg));
        }
    }

    private static void generatePiece(ServerLevel level, ChunkGenerator chunkGenerator, ChunkPos chunkPos, WorldgenRandom worldgenrandom, BlockPos finalCenterPos, StructurePiece piece) {
        piece.postProcess(
                level,
                level.structureManager(),
                chunkGenerator,
                worldgenrandom,
                BoundingBox.infinite(),
                chunkPos,
                finalCenterPos
        );
    }
}
