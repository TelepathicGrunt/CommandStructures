package com.telepathicgrunt.commandstructures.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.tree.LiteralCommandNode;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.coordinates.Coordinates;
import net.minecraft.commands.arguments.coordinates.Vec3Argument;
import net.minecraft.commands.arguments.coordinates.WorldCoordinate;
import net.minecraft.commands.arguments.coordinates.WorldCoordinates;
import net.minecraft.commands.synchronization.SuggestionProviders;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
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
import net.minecraft.world.phys.Vec3;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

public class SpawnMobsCommand {
    private static MinecraftServer currentMinecraftServer = null;
    private static Set<String> cachedSuggestion = new HashSet<>();

    public static void createCommand(CommandDispatcher<CommandSourceStack> dispatcher, CommandBuildContext buildContext) {
        String commandString = "spawnmobs";
        String rlArg = "resourcelocationpath";
        String locationArg = "location";
        String rowlengthArg = "rowlength";
        String livingentitiesArg = "livingentities";

        LiteralCommandNode<CommandSourceStack> source = dispatcher.register(Commands.literal(commandString)
                .requires((permission) -> permission.hasPermission(2))
                .then(Commands.argument(rlArg, StringArgumentType.string())
                .suggests((ctx, sb) -> SharedSuggestionProvider.suggest(namespaceSuggestions(ctx), sb))
                .executes(cs -> {
                    WorldCoordinates worldCoordinates = new WorldCoordinates(
                            new WorldCoordinate(false, cs.getSource().getPosition().x()),
                            new WorldCoordinate(false, cs.getSource().getPosition().y()),
                            new WorldCoordinate(false, cs.getSource().getPosition().z())
                    );
                    spawnMobs(cs.getArgument(rlArg, String.class), worldCoordinates, 13, false, cs);
                    return 1;
                })
                .then(Commands.argument(locationArg, Vec3Argument.vec3())
                .executes(cs -> {
                    spawnMobs(cs.getArgument(rlArg, String.class), Vec3Argument.getCoordinates(cs, locationArg), 13, false, cs);
                    return 1;
                })
                .then(Commands.argument(rowlengthArg, IntegerArgumentType.integer())
                .executes(cs -> {
                    spawnMobs(cs.getArgument(rlArg, String.class), Vec3Argument.getCoordinates(cs, locationArg), cs.getArgument(rowlengthArg, Integer.class), false, cs);
                    return 1;
                })
                .then(Commands.argument(livingentitiesArg, BoolArgumentType.bool())
                .executes(cs -> {
                    spawnMobs(cs.getArgument(rlArg, String.class), Vec3Argument.getCoordinates(cs, locationArg), cs.getArgument(rowlengthArg, Integer.class), cs.getArgument(livingentitiesArg, Boolean.class), cs);
                    return 1;
                })
        )))));

        dispatcher.register(Commands.literal(commandString).redirect(source));
    }

    private static Set<String> namespaceSuggestions(CommandContext<CommandSourceStack> cs) {
        if(currentMinecraftServer == cs.getSource().getServer()) {
            return cachedSuggestion;
        }

        Set<String> modidStrings = new HashSet<>();
        BuiltInRegistries.ENTITY_TYPE.entrySet().forEach((entry) -> {
            if (entry.getValue().canSummon()) {
                modidStrings.add(entry.getKey().location().getNamespace());
            }
        });

        currentMinecraftServer = cs.getSource().getServer();
        cachedSuggestion = modidStrings;
        return modidStrings;
    }

    public static void spawnMobs(String namespace, Coordinates coordinates, int rowlength, boolean livingEntitiesOnly, CommandContext<CommandSourceStack> cs) {
        ServerLevel level = cs.getSource().getLevel();
        Player player = cs.getSource().getEntity() instanceof Player player1 ? player1 : null;
        BlockPos pos = coordinates.getBlockPos(cs.getSource());

        List<EntityType<?>> types = BuiltInRegistries.ENTITY_TYPE.entrySet().stream()
                .filter(e -> namespace.equals("all") || e.getKey().location().getNamespace().equals(namespace))
                .map(Map.Entry::getValue)
                .collect(Collectors.toList());

        List<Entity> entities = types.stream().map(e -> e.create(level)).collect(Collectors.toList());
        if (livingEntitiesOnly) {
            entities = entities.stream().filter(e -> e instanceof LivingEntity).collect(Collectors.toList());
        }

        // Size of area we will need
        int columnCount = rowlength;
        int rowCount = (int) Math.max(Math.ceil((float)types.size() / columnCount), 1);
        if(rowCount == 1) {
            columnCount = types.size();
        }

        int spacing = 12;
        BlockPos bounds = new BlockPos((spacing * rowCount) + 16, spacing, spacing * columnCount);

        // Fill/clear area with structure void
        clearAreaNew(level, pos, player, bounds, Blocks.AIR.defaultBlockState(), Blocks.BARRIER.defaultBlockState());
        spawnMobs(level, pos, player, entities, columnCount, spacing);
    }

    private static void clearAreaNew(ServerLevel world, BlockPos pos, Player player, BlockPos bounds, BlockState fillBlock, BlockState floorBlock) {
        BlockPos.MutableBlockPos mutableChunk = new BlockPos.MutableBlockPos().set(pos.getX() >> 4, pos.getY(), pos.getZ() >> 4);
        mutableChunk.move(1,0,0);
        int endChunkX = (pos.getX() + bounds.getX()) >> 4;
        int endChunkZ = (pos.getZ() + bounds.getZ()) >> 4;

        int maxChunks = (endChunkX - mutableChunk.getX()) * (endChunkZ - mutableChunk.getZ());
        int currentSection = 0;
        for(; mutableChunk.getX() <= endChunkX; mutableChunk.move(1,0,0)) {
            for (; mutableChunk.getZ() <= endChunkZ; mutableChunk.move(0, 0, 1)) {
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
                            world.getChunkSource().blockChanged(mutable);
                            world.getChunkSource().getLightEngine().checkBlock(mutable);
                        }
                        for(int y = pos.getY() + 1; y < pos.getY() + 64; y++) {
                            mutable.setY(y);
                            oldState = chunk.setBlockState(mutable, fillBlock, false);
                            if(oldState != null) {
                                world.getChunkSource().blockChanged(mutable);
                                world.getChunkSource().getLightEngine().checkBlock(mutable);
                            }
                        }
                    }
                }
                currentSection++;
                if(player != null) {
                    player.displayClientMessage(Component.translatable("Working: %" +  Math.round(((float)currentSection / maxChunks) * 100f)), true);
                }
            }
            mutableChunk.set(mutableChunk.getX(), mutableChunk.getY(), pos.getZ() >> 4); // Set back to start of row
        }
    }

    private static void spawnMobs(ServerLevel world, BlockPos pos, Player player, List<Entity> entities, int columnCount, int spacing) {
        BlockPos.MutableBlockPos mutable = new BlockPos.MutableBlockPos().set(((pos.getX() >> 4) + 1) << 4, pos.getY(), (pos.getZ() >> 4) << 4);
        mutable.move(7, 1, 7);

        for(int mobIndex = 1; mobIndex <= entities.size(); mobIndex++) {
            if(player != null) {
                player.displayClientMessage(Component.translatable(" Spawning mobs"), true);
            }

            Entity entity = entities.get(mobIndex - 1);
            if (entity != null) {
                entity.setPos(Vec3.atCenterOf(mutable.above()));
                entity.setNoGravity(true);
                entity.setInvulnerable(true);
                entity.noPhysics = true;
                if (entity instanceof Mob mob) {
                    mob.setPersistenceRequired();
                    mob.setNoAi(true);
                    mob.setSpeed(0);
                    mob.addEffect(new MobEffectInstance(
                        MobEffects.MOVEMENT_SLOWDOWN,
                        99999999,
                        255,
                        true,
                        false,
                        false
                    ));
                    mob.addEffect(new MobEffectInstance(
                        MobEffects.REGENERATION,
                        99999999,
                        255,
                        true,
                        false,
                        false
                    ));
                    mob.addEffect(new MobEffectInstance(
                        MobEffects.ABSORPTION,
                        99999999,
                        255,
                        true,
                        false,
                        false
                    ));
                    mob.addEffect(new MobEffectInstance(
                        MobEffects.FIRE_RESISTANCE,
                        99999999,
                        255,
                        true,
                        false,
                        false
                    ));
                }
                world.addFreshEntity(entity);
            }

            mutable.move(0,0, spacing);

            // Move back to start of row
            if(mobIndex % columnCount == 0) {
                mutable.move(spacing,0, (-spacing * columnCount));
            }
        }
    }
}
