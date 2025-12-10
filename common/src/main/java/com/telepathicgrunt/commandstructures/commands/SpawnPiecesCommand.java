package com.telepathicgrunt.commandstructures.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import com.mojang.brigadier.tree.LiteralCommandNode;
import com.telepathicgrunt.commandstructures.CommandStructuresMain;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.IdentifierArgument;
import net.minecraft.commands.arguments.blocks.BlockStateArgument;
import net.minecraft.commands.arguments.coordinates.Coordinates;
import net.minecraft.commands.arguments.coordinates.Vec3Argument;
import net.minecraft.commands.arguments.coordinates.WorldCoordinate;
import net.minecraft.commands.arguments.coordinates.WorldCoordinates;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerChunkCache;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.StructureBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.StructureBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.StructureMode;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplateManager;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

public class SpawnPiecesCommand {
    private static MinecraftServer currentMinecraftServer = null;
    private static Set<Identifier> cachedSuggestion = new HashSet<>();

    public static void createCommand(CommandDispatcher<CommandSourceStack> dispatcher, CommandBuildContext buildContext) {
        String commandString = "spawnpieces";
        String rlArg = "resourcelocationpath";
        String locationArg = "location";
        String savepieceArg = "savepieces";
        String floorblockArg = "floorblock";
        String fillerblockArg = "fillerblock";
        String rowlengthArg = "rowlength";
        String spacingArg = "spacing";

        LiteralCommandNode<CommandSourceStack> source = dispatcher.register(Commands.literal(commandString)
                .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                .then(Commands.argument(rlArg, IdentifierArgument.id())
                .suggests((ctx, sb) -> SharedSuggestionProvider.suggestResource(templatePathsSuggestions(ctx), sb))
                .executes(cs -> {
                    WorldCoordinates worldCoordinates = new WorldCoordinates(
                            new WorldCoordinate(false, cs.getSource().getPosition().x()),
                            new WorldCoordinate(false, cs.getSource().getPosition().y()),
                            new WorldCoordinate(false, cs.getSource().getPosition().z())
                    );
                    spawnPieces(cs.getArgument(rlArg, Identifier.class), worldCoordinates, false, Blocks.BARRIER.defaultBlockState(), Blocks.AIR.defaultBlockState(), 13, -1, cs);
                    return 1;
                })
                .then(Commands.argument(locationArg, Vec3Argument.vec3())
                .executes(cs -> {
                    spawnPieces(cs.getArgument(rlArg, Identifier.class), Vec3Argument.getCoordinates(cs, locationArg), false, Blocks.BARRIER.defaultBlockState(), Blocks.AIR.defaultBlockState(), 13, -1, cs);
                    return 1;
                })
                .then(Commands.argument(savepieceArg, BoolArgumentType.bool())
                .executes(cs -> {
                    spawnPieces(cs.getArgument(rlArg, Identifier.class), Vec3Argument.getCoordinates(cs, locationArg), cs.getArgument(savepieceArg, boolean.class), Blocks.BARRIER.defaultBlockState(), Blocks.AIR.defaultBlockState(), 13, -1, cs);
                    return 1;
                })
                .then(Commands.argument(floorblockArg, BlockStateArgument.block(buildContext))
                .executes(cs -> {
                    spawnPieces(cs.getArgument(rlArg, Identifier.class), Vec3Argument.getCoordinates(cs, locationArg), cs.getArgument(savepieceArg, boolean.class), BlockStateArgument.getBlock(cs, floorblockArg).getState(), Blocks.AIR.defaultBlockState(), 13, -1, cs);
                    return 1;
                })
                .then(Commands.argument(fillerblockArg, BlockStateArgument.block(buildContext))
                .executes(cs -> {
                    spawnPieces(cs.getArgument(rlArg, Identifier.class), Vec3Argument.getCoordinates(cs, locationArg), cs.getArgument(savepieceArg, boolean.class), BlockStateArgument.getBlock(cs, floorblockArg).getState(), BlockStateArgument.getBlock(cs, fillerblockArg).getState(), 13, -1, cs);
                    return 1;
                })
                .then(Commands.argument(rowlengthArg, IntegerArgumentType.integer())
                .executes(cs -> {
                    spawnPieces(cs.getArgument(rlArg, Identifier.class), Vec3Argument.getCoordinates(cs, locationArg), cs.getArgument(savepieceArg, boolean.class), BlockStateArgument.getBlock(cs, floorblockArg).getState(), BlockStateArgument.getBlock(cs, fillerblockArg).getState(), cs.getArgument(rowlengthArg, Integer.class), -1, cs);
                    return 1;
                })
                .then(Commands.argument(spacingArg, IntegerArgumentType.integer())
                .executes(cs -> {
                    spawnPieces(cs.getArgument(rlArg, Identifier.class), Vec3Argument.getCoordinates(cs, locationArg), cs.getArgument(savepieceArg, boolean.class), BlockStateArgument.getBlock(cs, floorblockArg).getState(), BlockStateArgument.getBlock(cs, fillerblockArg).getState(), cs.getArgument(rowlengthArg, Integer.class), cs.getArgument(spacingArg, Integer.class), cs);
                    return 1;
                })
        ))))))));

        dispatcher.register(Commands.literal(commandString).redirect(source));
    }

    private static Set<Identifier> templatePathsSuggestions(CommandContext<CommandSourceStack> cs) {
        if(currentMinecraftServer == cs.getSource().getServer()) {
            return cachedSuggestion;
        }

        ResourceManager resourceManager = cs.getSource().getLevel().getServer().getResourceManager();
        Set<String> modidStrings = new HashSet<>();
        Set<Identifier> rlSet = resourceManager.listResources("structure", (filename) -> filename.toString().endsWith(".nbt"))
                .keySet()
                .stream()
                .map(resourceLocation -> {
                    String namespace = resourceLocation.getNamespace();
                    modidStrings.add(namespace);

                    String path = resourceLocation.getPath()
                            .replaceAll("structure/", "")
                            .replaceAll(".nbt", "");

                    // We want to suggest folders instead of individual nbts
                    int i = path.lastIndexOf('/');
                    if(i > 0) {
                        path = path.substring(0, i) + "/";
                    }

                    return Identifier.fromNamespaceAndPath(namespace, path);
                })
                .collect(Collectors.toSet());

        // add suggestion for entire mods/vanilla too
        rlSet.addAll(modidStrings.stream()
                .map(modid -> Identifier.fromNamespaceAndPath(modid, ""))
                .collect(Collectors.toSet()));

        currentMinecraftServer = cs.getSource().getServer();
        cachedSuggestion = rlSet;
        return rlSet;
    }

    public static void spawnPieces(Identifier path, Coordinates coordinates, boolean savePieces, BlockState floorBlockState, BlockState fillBlockState, int rowlength, int spacing, CommandContext<CommandSourceStack> cs) throws CommandSyntaxException {
        ServerLevel level = cs.getSource().getLevel();
        Player player = cs.getSource().getEntity() instanceof Player player1 ? player1 : null;
        BlockPos pos = coordinates.getBlockPos(cs.getSource());

        List<Identifier> nbtRLs = getIdentifiers(level, path.getNamespace(), path.getPath());

        if(nbtRLs.isEmpty()) {
            String errorMsg = path + " path has no nbt pieces in it. No pieces will be placed.";
            CommandStructuresMain.LOGGER.error(errorMsg);
            throw new SimpleCommandExceptionType(Component.translatable(errorMsg)).create();
        }

        if (spacing <= -1) {
            for (Identifier nbtRL : nbtRLs) {
                Optional<StructureTemplate> optionalStructureTemplate = level.getServer().getStructureManager().get(nbtRL);
                if (optionalStructureTemplate.isPresent()) {
                    StructureTemplate structureTemplate = optionalStructureTemplate.get();
                    spacing = Math.max(Math.max(spacing, structureTemplate.getSize().getX()), structureTemplate.getSize().getZ());
                }
            }
            spacing += 1;
        }

        // Size of area we will need
        int columnCount = rowlength;
        int rowCount = (int) Math.max(Math.ceil((float)nbtRLs.size() / columnCount), 1);
        if(rowCount == 1) {
            columnCount = nbtRLs.size();
        }

        BlockPos bounds = new BlockPos((spacing * rowCount) + 16, spacing, spacing * columnCount);

        // Fill/clear area with structure void
        clearAreaNew(level, pos, player, bounds, fillBlockState, floorBlockState, spacing);
        generateStructurePieces(level, pos, player, nbtRLs, columnCount, spacing, savePieces);
    }

    private static void clearAreaNew(ServerLevel world, BlockPos pos, Player player, BlockPos bounds, BlockState fillBlock, BlockState floorBlock, int spacing) {
        BlockPos.MutableBlockPos mutableChunk = new BlockPos.MutableBlockPos().set(pos.getX() >> 4, pos.getY(), pos.getZ() >> 4);
        BlockPos.MutableBlockPos mutablePos = new BlockPos.MutableBlockPos();

        mutableChunk.move(1,0,0);
        int endChunkX = (pos.getX() + bounds.getX()) >> 4;
        int endChunkZ = (pos.getZ() + bounds.getZ()) >> 4;

        int maxChunks = (endChunkX - mutableChunk.getX()) * (endChunkZ - mutableChunk.getZ());
        int currentSection = 0;

        int minY = pos.getY() + 1;
        int maxY = pos.getY() + spacing + 1;

        ServerChunkCache chunkSource = world.getChunkSource();

        while (mutableChunk.getX() < endChunkX) {
            while (mutableChunk.getZ() < endChunkZ) {
                mutablePos.set(mutableChunk.getX() << 4, pos.getY(), mutableChunk.getZ() << 4);
                mutablePos.move(-1, 0, 0);

                ChunkAccess chunkAccess = world.getChunk(mutableChunk.getX(), mutableChunk.getZ());
                for (int x = 0; x < 16; x++) {
                    mutablePos.setZ(mutableChunk.getZ() << 4);
                    mutablePos.move(1, 0, -1);

                    for (int z = 0; z < 16; z++) {
                        mutablePos.move(0, 0, 1);
                        mutablePos.setY(pos.getY());
                        BlockState oldState = chunkAccess.setBlockState(mutablePos, floorBlock, Block.UPDATE_CLIENTS);

                        if (oldState != null) {
                            chunkSource.blockChanged(mutablePos);
                        }

                        for (int y = minY; y < maxY; y++) {
                            mutablePos.setY(y);
                            oldState = chunkAccess.setBlockState(mutablePos, fillBlock, Block.UPDATE_CLIENTS);

                            if (oldState != null) {
                                chunkSource.blockChanged(mutablePos);
                            }
                        }
                    }
                }

                currentSection++;
                if(player != null) {
                    player.displayClientMessage(Component.translatable("Working: %" +  Math.round(((float)currentSection / maxChunks) * 100f)), true);
                }

                mutableChunk.move(0, 0, 1);
            }
            mutableChunk.set(mutableChunk.getX(), mutableChunk.getY(), pos.getZ() >> 4); // Set back to start of row
            mutableChunk.move(1,0,0);
        }
    }



    private static List<Identifier> getIdentifiers(ServerLevel world, String modId, String filter) {
        ResourceManager resourceManager = world.getServer().getResourceManager();
        return resourceManager.listResources("structure", (filename) -> filename.toString().endsWith(".nbt"))
                .keySet()
                .stream()
                .filter(resourceLocation -> resourceLocation.getNamespace().equals(modId))
                .filter(resourceLocation -> resourceLocation.getPath().startsWith("structure/" + filter))
                .map(resourceLocation -> Identifier.fromNamespaceAndPath(resourceLocation.getNamespace(), resourceLocation.getPath().replaceAll("^structure/", "").replaceAll(".nbt$", "")))
                .toList();
    }


    private static void generateStructurePieces(ServerLevel world, BlockPos pos, Player player, List<Identifier> nbtRLs, int columnCount, int spacing, boolean savePieces) {
        BlockPos.MutableBlockPos mutable = new BlockPos.MutableBlockPos().set(((pos.getX() >> 4) + 1) << 4, pos.getY(), (pos.getZ() >> 4) << 4);

        for(int pieceIndex = 1; pieceIndex <= nbtRLs.size(); pieceIndex++) {
            if(player != null) {
                player.displayClientMessage(Component.literal("Working making structure: " + nbtRLs.get(pieceIndex - 1)), true);
            }

            world.setBlock(mutable, Blocks.STRUCTURE_BLOCK.defaultBlockState().setValue(StructureBlock.MODE, StructureMode.LOAD), 3);
            BlockEntity be = world.getBlockEntity(mutable);
            if(be instanceof StructureBlockEntity structureBlockTileEntity) {
                structureBlockTileEntity.setStructureName(nbtRLs.get(pieceIndex-1)); // set identifier

                structureBlockTileEntity.setMode(StructureMode.LOAD);
                structureBlockTileEntity.setIgnoreEntities(false);

                fillStructureVoidSpace(world, nbtRLs.get(pieceIndex-1), mutable);
                structureBlockTileEntity.placeStructure(world); // load structure

                structureBlockTileEntity.setMode(StructureMode.SAVE);
                if(savePieces) {
                    structureBlockTileEntity.saveStructure(true);
                }
                //structureBlockTileEntity.setShowAir(true);
                structureBlockTileEntity.setIgnoreEntities(false);
            }

            mutable.move(0,0, spacing);

            // Move back to start of row
            if(pieceIndex % columnCount == 0) {
                mutable.move(spacing,0, (-spacing * columnCount));
            }
        }
    }

    // Needed so that structure void is preserved in structure pieces.
    private static void fillStructureVoidSpace(ServerLevel world, Identifier resourceLocation, BlockPos startSpot) {
        StructureTemplateManager structuremanager = world.getStructureManager();
        Optional<StructureTemplate> optional = structuremanager.get(resourceLocation);
        optional.ifPresent(template -> {
            BlockPos.MutableBlockPos mutable = startSpot.mutable();
            ChunkAccess chunk = world.getChunk(mutable);
            for(int x = 0; x < template.getSize().getX(); x++) {
                for (int z = 0; z < template.getSize().getZ(); z++) {
                    for(int y = 0; y < template.getSize().getY(); y++) {
                        mutable.set(startSpot).move(x, y + 1, z);
                        if(chunk.getPos().x != mutable.getX() >> 4 || chunk.getPos().z != mutable.getZ() >> 4) {
                            chunk = world.getChunk(mutable);
                        }

                        BlockState oldState = chunk.setBlockState(mutable, Blocks.STRUCTURE_VOID.defaultBlockState(), Block.UPDATE_CLIENTS);
                        if(oldState != null) {
                            world.getChunkSource().blockChanged(mutable);
                        }
                    }
                }
            }
        });
    }
}
