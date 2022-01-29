package com.telepathicgrunt.commandstructures.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.LongArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.tree.LiteralCommandNode;
import com.telepathicgrunt.commandstructures.CommandStructuresMain;
import com.telepathicgrunt.commandstructures.Utilities;
import net.minecraft.commands.CommandRuntimeException;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.ResourceLocationArgument;
import net.minecraft.commands.arguments.coordinates.Coordinates;
import net.minecraft.commands.arguments.coordinates.Vec3Argument;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.core.SectionPos;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.levelgen.LegacyRandomSource;
import net.minecraft.world.level.levelgen.RandomSupport;
import net.minecraft.world.level.levelgen.WorldgenRandom;
import net.minecraft.world.level.levelgen.XoroshiroRandomSource;
import net.minecraft.world.level.levelgen.feature.ConfiguredStructureFeature;
import net.minecraft.world.level.levelgen.feature.MineshaftFeature;
import net.minecraft.world.level.levelgen.feature.NetherFortressFeature;
import net.minecraft.world.level.levelgen.feature.OceanMonumentFeature;
import net.minecraft.world.level.levelgen.feature.StrongholdFeature;
import net.minecraft.world.level.levelgen.feature.StructureFeature;
import net.minecraft.world.level.levelgen.feature.configurations.JigsawConfiguration;
import net.minecraft.world.level.levelgen.feature.configurations.MineshaftConfiguration;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;
import net.minecraft.world.level.levelgen.feature.configurations.StructureFeatureConfiguration;
import net.minecraft.world.level.levelgen.feature.structures.JigsawPlacement;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.BuriedTreasurePieces;
import net.minecraft.world.level.levelgen.structure.NetherFossilPieces;
import net.minecraft.world.level.levelgen.structure.PoolElementStructurePiece;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.minecraft.world.level.levelgen.structure.pieces.PieceGenerator;
import net.minecraft.world.level.levelgen.structure.pieces.PieceGeneratorSupplier;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePiecesBuilder;

import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

public class RawStructureSpawnCommand {
    public static void dataGenCommand(CommandDispatcher<CommandSourceStack> dispatcher) {
        String commandString = "spawnrawstructure";
        String locationArg = "location";
        String cfRL = "configuredstructure";
        String saveStructureBounds = "savestructurebounds";
        String sendChunkLightingPacket = "sendchunklightingpacket";
        String randomSeed = "randomseed";

        LiteralCommandNode<CommandSourceStack> source = dispatcher.register(Commands.literal(commandString)
            .requires((permission) -> permission.hasPermission(2))
            .then(Commands.argument(locationArg, Vec3Argument.vec3())
            .then(Commands.argument(cfRL, ResourceLocationArgument.id())
            .suggests((ctx, sb) -> SharedSuggestionProvider.suggestResource(startPoolSuggestions(ctx), sb))
            .executes(cs -> {
                generateStructure(Vec3Argument.getCoordinates(cs, locationArg), cs.getArgument(cfRL, ResourceLocation.class), true, true, null, cs);
                return 1;
            })
            .then(Commands.argument(saveStructureBounds, BoolArgumentType.bool())
            .executes(cs -> {
                generateStructure(Vec3Argument.getCoordinates(cs, locationArg), cs.getArgument(cfRL, ResourceLocation.class), cs.getArgument(saveStructureBounds, Boolean.class), true, null, cs);
                return 1;
            })
            .then(Commands.argument(saveStructureBounds, BoolArgumentType.bool())
            .executes(cs -> {
                generateStructure(Vec3Argument.getCoordinates(cs, locationArg), cs.getArgument(cfRL, ResourceLocation.class), cs.getArgument(saveStructureBounds, Boolean.class), cs.getArgument(sendChunkLightingPacket, Boolean.class), null, cs);
                return 1;
            })
            .then(Commands.argument(randomSeed, LongArgumentType.longArg())
            .executes(cs -> {
                generateStructure(Vec3Argument.getCoordinates(cs, locationArg), cs.getArgument(cfRL, ResourceLocation.class), cs.getArgument(saveStructureBounds, Boolean.class), cs.getArgument(sendChunkLightingPacket, Boolean.class), cs.getArgument(randomSeed, Long.class), cs);
                return 1;
            })
        ))))));

        dispatcher.register(Commands.literal(commandString).redirect(source));
    }

    private static Set<ResourceLocation> startPoolSuggestions(CommandContext<CommandSourceStack> cs) {
        return cs.getSource().getLevel().registryAccess().registryOrThrow(Registry.CONFIGURED_STRUCTURE_FEATURE_REGISTRY).keySet();
    }

    private static void generateStructure(Coordinates coordinates, ResourceLocation structureRL, boolean saveStructureBounds, boolean sendChunkLightingPacket, Long randomSeed, CommandContext<CommandSourceStack> cs) {
        ServerLevel level = cs.getSource().getLevel();
        BlockPos centerPos = coordinates.getBlockPos(cs.getSource());
        ChunkPos chunkPos = new ChunkPos(centerPos);
        ChunkAccess chunkAccess = level.getChunk(chunkPos.x, chunkPos.z);
        SectionPos sectionpos = SectionPos.bottomOf(chunkAccess);

        WorldgenRandom worldgenrandom;
        if (randomSeed == null) {
            worldgenrandom = new WorldgenRandom(new XoroshiroRandomSource(RandomSupport.seedUniquifier()));
            long i = worldgenrandom.setDecorationSeed(level.getSeed(), centerPos.getX(), centerPos.getZ());
            worldgenrandom.setFeatureSeed(i, 0, 0);
        } else {
            worldgenrandom = new WorldgenRandom(new LegacyRandomSource(randomSeed));
        }

        ConfiguredStructureFeature<?, ?> configuredStructureFeature = level.registryAccess().ownedRegistryOrThrow(Registry.CONFIGURED_STRUCTURE_FEATURE_REGISTRY).get(structureRL);

        if (configuredStructureFeature == null) {
            String errorMsg = structureRL + " ConfiguredStructureFeature does not exist in registry";
            CommandStructuresMain.LOGGER.error(errorMsg);
            throw new CommandRuntimeException(new TextComponent(errorMsg));
        }

        StructureStart<?> structureStart;

        if (configuredStructureFeature.feature == StructureFeature.MINESHAFT) {
            StructurePiecesBuilder structurePiecesBuilder = new StructurePiecesBuilder();
            MineshaftFeature.generatePieces(
                    structurePiecesBuilder,
                    new PieceGenerator.Context<>(
                            (MineshaftConfiguration) configuredStructureFeature.config,
                            level.getChunkSource().getGenerator(),
                            level.getStructureManager(),
                            chunkPos,
                            level,
                            worldgenrandom,
                            0)
            );
            structureStart = new StructureStart<>(configuredStructureFeature.feature, chunkPos, 0, structurePiecesBuilder.build());
        }
        else if (configuredStructureFeature.feature == StructureFeature.OCEAN_MONUMENT) {
            StructurePiecesBuilder structurePiecesBuilder = new StructurePiecesBuilder();
            OceanMonumentFeature.generatePieces(
                    structurePiecesBuilder,
                    new PieceGenerator.Context<>(
                            (NoneFeatureConfiguration) configuredStructureFeature.config,
                            level.getChunkSource().getGenerator(),
                            level.getStructureManager(),
                            chunkPos,
                            level,
                            worldgenrandom,
                            0)
            );
            structureStart = new StructureStart<>(configuredStructureFeature.feature, chunkPos, 0, structurePiecesBuilder.build());
        }
        else if (configuredStructureFeature.feature == StructureFeature.PILLAGER_OUTPOST) {
            StructurePiecesBuilder structurePiecesBuilder = new StructurePiecesBuilder();
            PieceGeneratorSupplier.Context<JigsawConfiguration> newContext = new PieceGeneratorSupplier.Context<>(
                    level.getChunkSource().getGenerator(),
                    level.getChunkSource().getGenerator().getBiomeSource(),
                    level.getSeed(),
                    randomSeed == null ? new ChunkPos(centerPos) : new ChunkPos(0, 0),
                    (JigsawConfiguration) configuredStructureFeature.config,
                    level,
                    (b) -> true,
                    level.getStructureManager(),
                    level.registryAccess()
            );
            Optional<PieceGenerator<JigsawConfiguration>> pieceGenerator = JigsawPlacement.addPieces(
                    newContext,
                    PoolElementStructurePiece::new,
                    centerPos.below(centerPos.getY()),
                    true,
                    true);
            pieceGenerator.ifPresent(jigsawConfigurationPieceGenerator -> jigsawConfigurationPieceGenerator.generatePieces(
                    structurePiecesBuilder,
                    new PieceGenerator.Context<>(
                            (JigsawConfiguration) configuredStructureFeature.config,
                            level.getChunkSource().getGenerator(),
                            level.getStructureManager(),
                            chunkPos,
                            level,
                            worldgenrandom,
                            0)
            ));
            structureStart = new StructureStart<>(configuredStructureFeature.feature, chunkPos, 0, structurePiecesBuilder.build());
        }
        else if(configuredStructureFeature.feature == StructureFeature.NETHER_BRIDGE) {
            StructurePiecesBuilder structurePiecesBuilder = new StructurePiecesBuilder();
            NetherFortressFeature.generatePieces(
                    structurePiecesBuilder,
                    new PieceGenerator.Context<>(
                            (NoneFeatureConfiguration) configuredStructureFeature.config,
                            level.getChunkSource().getGenerator(),
                            level.getStructureManager(),
                            chunkPos,
                            level,
                            worldgenrandom,
                            0)
            );
            structureStart = new StructureStart<>(configuredStructureFeature.feature, chunkPos, 0, structurePiecesBuilder.build());
        }
        else if(configuredStructureFeature.feature == StructureFeature.NETHER_FOSSIL) {
            StructurePiecesBuilder structurePiecesBuilder = new StructurePiecesBuilder();
            NetherFossilPieces.addPieces(level.getStructureManager(), structurePiecesBuilder, worldgenrandom, centerPos);
            structureStart = new StructureStart<>(configuredStructureFeature.feature, chunkPos, 0, structurePiecesBuilder.build());
        }
        else if(configuredStructureFeature.feature == StructureFeature.BURIED_TREASURE) {
            StructurePiecesBuilder structurePiecesBuilder = new StructurePiecesBuilder();
            structurePiecesBuilder.addPiece(new BuriedTreasurePieces.BuriedTreasurePiece(centerPos));
            structureStart = new StructureStart<>(configuredStructureFeature.feature, chunkPos, 0, structurePiecesBuilder.build());
        }
        else if(configuredStructureFeature.feature == StructureFeature.STRONGHOLD) {
            StructurePiecesBuilder structurePiecesBuilder = new StructurePiecesBuilder();
            StrongholdFeature.generatePieces(
                    structurePiecesBuilder,
                    new PieceGenerator.Context<>(
                            (NoneFeatureConfiguration) configuredStructureFeature.config,
                            level.getChunkSource().getGenerator(),
                            level.getStructureManager(),
                            chunkPos,
                            level,
                            worldgenrandom,
                            0)
            );
            structureStart = new StructureStart<>(configuredStructureFeature.feature, chunkPos, 0, structurePiecesBuilder.build());
        }
        else {
            structureStart = configuredStructureFeature.generate(
                    level.registryAccess(),
                    level.getChunkSource().getGenerator(),
                    level.getChunkSource().getGenerator().getBiomeSource(),
                    level.getStructureManager(),
                    randomSeed == null ? RandomSupport.seedUniquifier() : randomSeed,
                    chunkPos,
                    0,
                    new StructureFeatureConfiguration(1, 0, 0),
                    level,
                    (biome) -> true
            );
        }

        structureStart.getPieces().forEach(piece -> generatePiece(level, worldgenrandom, centerPos, piece));
        level.structureFeatureManager().setStartForFeature(sectionpos, configuredStructureFeature.feature, structureStart, chunkAccess);

        if(saveStructureBounds) {
            Set<ChunkPos> chunkPosSet = structureStart.getPieces().stream().map(piece -> new ChunkPos(piece.getBoundingBox().getCenter())).collect(Collectors.toSet());
            for(ChunkPos chunkPos1 : chunkPosSet) {
                ChunkAccess chunkAccess1 = level.getChunk(chunkPos1.x, chunkPos1.z);
                level.getChunkSource().getGenerator().createReferences(level, level.structureFeatureManager(), chunkAccess1);
            }
        }

        if(!structureStart.getPieces().isEmpty()) {
            if(sendChunkLightingPacket) {
                Utilities.refreshChunksOnClients(level);
            }
        }
        else {
            String errorMsg = structureRL + " ConfiguredStructure failed to be spawned. (It may have internal checks for valid spots)";
            CommandStructuresMain.LOGGER.error(errorMsg);
            throw new CommandRuntimeException(new TextComponent(errorMsg));
        }
    }

    private static void generatePiece(ServerLevel level, WorldgenRandom worldgenrandom, BlockPos finalCenterPos, StructurePiece piece) {
        piece.postProcess(
                level,
                level.structureFeatureManager(),
                level.getChunkSource().getGenerator(),
                worldgenrandom,
                BoundingBox.infinite(),
                new ChunkPos(finalCenterPos),
                finalCenterPos
        );
    }
}
