package com.telepathicgrunt.commandstructures;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.tree.LiteralCommandNode;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.ResourceLocationArgument;
import net.minecraft.commands.arguments.blocks.BlockStateArgument;
import net.minecraft.commands.arguments.coordinates.Coordinates;
import net.minecraft.commands.arguments.coordinates.Vec3Argument;
import net.minecraft.commands.arguments.coordinates.WorldCoordinate;
import net.minecraft.commands.arguments.coordinates.WorldCoordinates;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerChunkCache;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.StructureBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.StructureBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.StructureMode;
import net.minecraft.world.level.chunk.LevelChunk;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class SpawnPiecesCommand {
    public static void dataGenCommand(CommandDispatcher<CommandSourceStack> dispatcher) {
        String commandString = "spawnpieces";
        String rlArg = "resourcelocationpath";
        String locationArg = "location";
        String fillerblockArg = "fillerblock";
        String floorblockArg = "floorblock";
        String rowlengthArg = "rowlength";

        LiteralCommandNode<CommandSourceStack> source = dispatcher.register(Commands.literal(commandString)
                .requires((permission) -> permission.hasPermission(2))
                .then(Commands.argument(rlArg, ResourceLocationArgument.id())
                        .suggests((ctx, sb) -> SharedSuggestionProvider.suggestResource(templatePathsSuggestions(ctx), sb))
                        .executes(cs -> {
                            WorldCoordinates worldCoordinates = new WorldCoordinates(
                                    new WorldCoordinate(false, cs.getSource().getPosition().x()),
                                    new WorldCoordinate(false, cs.getSource().getPosition().y()),
                                    new WorldCoordinate(false, cs.getSource().getPosition().z())
                            );
                            spawnPieces(cs.getArgument(rlArg, ResourceLocation.class), worldCoordinates, Blocks.STRUCTURE_VOID.defaultBlockState(), Blocks.BARRIER.defaultBlockState(), 13, cs);
                            return 1;
                        })
                        .then(Commands.argument(locationArg, Vec3Argument.vec3())
                                .executes(cs -> {
                                    spawnPieces(cs.getArgument(rlArg, ResourceLocation.class), Vec3Argument.getCoordinates(cs, locationArg), Blocks.STRUCTURE_VOID.defaultBlockState(), Blocks.BARRIER.defaultBlockState(), 13, cs);
                                    return 1;
                                })
                                .then(Commands.argument(fillerblockArg, BlockStateArgument.block())
                                        .executes(cs -> {
                                            spawnPieces(cs.getArgument(rlArg, ResourceLocation.class), Vec3Argument.getCoordinates(cs, locationArg), BlockStateArgument.getBlock(cs, fillerblockArg).getState(), Blocks.BARRIER.defaultBlockState(), 13, cs);
                                            return 1;
                                        })
                                        .then(Commands.argument(floorblockArg, BlockStateArgument.block())
                                                .executes(cs -> {
                                                    spawnPieces(cs.getArgument(rlArg, ResourceLocation.class), Vec3Argument.getCoordinates(cs, locationArg), BlockStateArgument.getBlock(cs, fillerblockArg).getState(), BlockStateArgument.getBlock(cs, floorblockArg).getState(), 13, cs);
                                                    return 1;
                                                })
                                                .then(Commands.argument(rowlengthArg, IntegerArgumentType.integer())
                                                        .executes(cs -> {
                                                            spawnPieces(cs.getArgument(rlArg, ResourceLocation.class), Vec3Argument.getCoordinates(cs, locationArg), BlockStateArgument.getBlock(cs, fillerblockArg).getState(), BlockStateArgument.getBlock(cs, floorblockArg).getState(), cs.getArgument(rowlengthArg, Integer.class), cs);
                                                            return 1;
                                                        })
                                                ))))));

        dispatcher.register(Commands.literal(commandString).redirect(source));
    }

    private static Set<ResourceLocation> templatePathsSuggestions(CommandContext<CommandSourceStack> cs) {
        ResourceManager resourceManager = cs.getSource().getLevel().getServer().getResourceManager();
        Set<ResourceLocation> rlSet = resourceManager.listResources("structures", (filename) -> filename.endsWith(".nbt"))
                .stream()
                .map(resourceLocation -> {
                    String namespace = resourceLocation.getNamespace();
                    String path = resourceLocation.getPath()
                            .replaceAll("structures/", "")
                            .replaceAll(".nbt", "");

                    // We want to suggest folders instead of individual nbts
                    int i = path.lastIndexOf('/');
                    path = path.substring(0, i) + "/";

                    return new ResourceLocation(namespace, path);
                })
                .collect(Collectors.toSet());

        // add suggestion for entire mods/vanilla too
        rlSet.addAll(rlSet.stream()
                .map(resourceLocation -> new ResourceLocation(resourceLocation.getNamespace(), ""))
                .collect(Collectors.toSet()));

        return rlSet;
    }

    public static void spawnPieces(ResourceLocation path, Coordinates coordinates, BlockState fillBlockState, BlockState floorBlockState, int rowlength, CommandContext<CommandSourceStack> cs) {
        ServerLevel level = cs.getSource().getLevel();
        Player player = cs.getSource().getEntity() instanceof Player player1 ? player1 : null;
        BlockPos pos = coordinates.getBlockPos(cs.getSource());

        List<ResourceLocation> identifiers = getResourceLocations(player, level, path.getNamespace(), path.getPath());

        // Size of area we will need
        int columnCount = rowlength;
        int rowCount = (int) Math.max(Math.ceil((float)identifiers.size() / columnCount), 1);
        if(rowCount == 1) {
            columnCount = identifiers.size();
        }

        int spacing = 48;
        BlockPos bounds = new BlockPos((spacing * rowCount) + 16, spacing, spacing * columnCount);

        // Fill/clear area with structure void
        BlockPos.MutableBlockPos mutableChunk = clearAreaNew(level, pos, player, bounds, fillBlockState, floorBlockState);

        generateStructurePieces(level, pos, player, identifiers, columnCount, spacing, mutableChunk);
    }

    private static BlockPos.MutableBlockPos clearAreaNew(Level world, BlockPos pos, Player player, BlockPos bounds, BlockState fillBlock, BlockState floorBlock) {
        BlockPos.MutableBlockPos mutableChunk = new BlockPos.MutableBlockPos().set(pos.getX() >> 4, pos.getY(), pos.getZ() >> 4);
        mutableChunk.move(1,0,0);
        int endChunkX = (pos.getX() + bounds.getX()) >> 4;
        int endChunkZ = (pos.getZ() + bounds.getZ()) >> 4;

        int maxChunks = (endChunkX - mutableChunk.getX()) * (endChunkZ - mutableChunk.getZ());
        int currentSection = 0;
        for(; mutableChunk.getX() < endChunkX; mutableChunk.move(1,0,0)) {
            for (; mutableChunk.getZ() < endChunkZ; mutableChunk.move(0, 0, 1)) {
                LevelChunk chunk = world.getChunk(mutableChunk.getX(), mutableChunk.getZ());
                BlockPos.MutableBlockPos mutable = new BlockPos(mutableChunk.getX() << 4, pos.getY(), mutableChunk.getZ() << 4).mutable();
                mutable.move(-1, 0, 0);
                for(int x = 0; x < 16; x++) {
                    mutable.setZ(mutableChunk.getZ() << 4);
                    mutable.move(1, 0, -1);
                    for(int z = 0; z < 16; z++) {
                        mutable.move(0, 0, 1);
                        mutable.setY(pos.getY());
                        BlockState oldState = chunk.setBlockState(mutable, floorBlock, false);
                        if(oldState != null) {
                            ((ServerChunkCache)world.getChunkSource()).blockChanged(mutable);
                            world.getChunkSource().getLightEngine().checkBlock(mutable);
                        }
                        for(int y = pos.getY() + 1; y < pos.getY() + 64; y++) {
                            mutable.setY(y);
                            oldState = chunk.setBlockState(mutable, fillBlock, false);
                            if(oldState != null) {
                                ((ServerChunkCache)world.getChunkSource()).blockChanged(mutable);
                                world.getChunkSource().getLightEngine().checkBlock(mutable);
                            }
                        }
                    }
                }
                currentSection++;
                if(player != null) {
                    player.displayClientMessage(new TranslatableComponent("Working: %" +  Math.round(((float)currentSection / maxChunks) * 100f)), true);
                }
            }
            mutableChunk.set(mutableChunk.getX(), mutableChunk.getY(), pos.getZ() >> 4); // Set back to start of row
        }
        return mutableChunk;
    }



    private static List<ResourceLocation> getResourceLocations(Player player, ServerLevel world, String modId, String filter) {
        if(player != null) {
            player.displayClientMessage(new TranslatableComponent(" Working.... "), true);
        }
        ResourceManager resourceManager = world.getServer().getResourceManager();
        return resourceManager.listResources("structures", (filename) -> filename.endsWith(".nbt"))
                .stream()
                .filter(resourceLocation -> resourceLocation.getNamespace().equals(modId))
                .filter(resourceLocation -> resourceLocation.getPath().startsWith("structures/" + filter))
                .map(resourceLocation -> new ResourceLocation(resourceLocation.getNamespace(), resourceLocation.getPath().replaceAll("^structures/", "").replaceAll(".nbt$", "")))
                .toList();
    }


    private static void generateStructurePieces(Level world, BlockPos pos, Player player, List<ResourceLocation> identifiers, int columnCount, int spacing, BlockPos.MutableBlockPos mutableChunk) {
        mutableChunk.set(((pos.getX() >> 4) + 1) << 4, pos.getY(), (pos.getZ() >> 4) << 4);

        for(int pieceIndex = 1; pieceIndex <= identifiers.size(); pieceIndex++) {
            if(player != null) {
                player.displayClientMessage(new TranslatableComponent(" Working making structure: " + identifiers.get(pieceIndex - 1)), true);
            }

            world.setBlock(mutableChunk, Blocks.STRUCTURE_BLOCK.defaultBlockState().setValue(StructureBlock.MODE, StructureMode.LOAD), 3);
            BlockEntity be = world.getBlockEntity(mutableChunk);
            if(be instanceof StructureBlockEntity structureBlockTileEntity) {
                structureBlockTileEntity.setStructureName(identifiers.get(pieceIndex-1)); // set identifier

                structureBlockTileEntity.setMode(StructureMode.LOAD);
                structureBlockTileEntity.setIgnoreEntities(false);
                structureBlockTileEntity.loadStructure((ServerLevel) world,false); // load structure

                structureBlockTileEntity.setMode(StructureMode.SAVE);
                //structureBlockTileEntity.saveStructure(true); //save structure
                //structureBlockTileEntity.setShowAir(true);
                structureBlockTileEntity.setIgnoreEntities(false);
            }

            mutableChunk.move(0,0, spacing);


            // Move back to start of row
            if(pieceIndex % columnCount == 0) {
                mutableChunk.move(spacing,0, (-spacing * columnCount));
            }
        }
    }
}
