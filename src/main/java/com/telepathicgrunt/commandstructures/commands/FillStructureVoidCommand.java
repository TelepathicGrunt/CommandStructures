package com.telepathicgrunt.commandstructures.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.tree.LiteralCommandNode;
import com.telepathicgrunt.commandstructures.UnsafeBulkSectionAccess;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.coordinates.Coordinates;
import net.minecraft.commands.arguments.coordinates.WorldCoordinate;
import net.minecraft.commands.arguments.coordinates.WorldCoordinates;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Blocks;

import java.util.LinkedList;
import java.util.Queue;

public class FillStructureVoidCommand {
    private static MinecraftServer currentMinecraftServer = null;

    public static void dataGenCommand(CommandDispatcher<CommandSourceStack> dispatcher, CommandBuildContext buildContext) {
        String commandString = "fillstructurevoid";
        String sizeArg = "radius";

        LiteralCommandNode<CommandSourceStack> source = dispatcher.register(Commands.literal(commandString)
                .requires((permission) -> permission.hasPermission(2))
                .then(Commands.argument(sizeArg, IntegerArgumentType.integer())
                .executes(cs -> {
                    WorldCoordinates worldCoordinates = new WorldCoordinates(
                            new WorldCoordinate(false, cs.getSource().getPosition().x()),
                            new WorldCoordinate(false, cs.getSource().getPosition().y()),
                            new WorldCoordinate(false, cs.getSource().getPosition().z())
                    );
                    fillStructureVoids(cs.getArgument(sizeArg, Integer.class), worldCoordinates, cs);
                    return 1;
                })
        ));

        dispatcher.register(Commands.literal(commandString).redirect(source));
    }

    public static void fillStructureVoids(int radius, Coordinates coordinates, CommandContext<CommandSourceStack> cs) {
        ServerLevel level = cs.getSource().getLevel();
        Player player = cs.getSource().getEntity() instanceof Player player1 ? player1 : null;
        BlockPos originPos = coordinates.getBlockPos(cs.getSource());

        player.displayClientMessage(Component.translatable("Working..."), true);

        UnsafeBulkSectionAccess sectionAccess = new UnsafeBulkSectionAccess(level);
        Queue<BlockPos> posQueue = new LinkedList<>();
        BlockPos.MutableBlockPos mutableBlockPos = new BlockPos.MutableBlockPos();
        posQueue.offer(originPos);

        while (!posQueue.isEmpty()) {
            BlockPos currentPos = posQueue.poll();
            mutableBlockPos.set(currentPos);

            if (sectionAccess.getBlockState(mutableBlockPos).isAir()) {
                level.setBlock(mutableBlockPos, Blocks.STRUCTURE_VOID.defaultBlockState(), 2);

                for (Direction direction : Direction.values()) {
                    mutableBlockPos.set(currentPos).move(direction);

                    if (mutableBlockPos.closerThan(originPos, radius + 1)) {
                        posQueue.offer(mutableBlockPos.immutable());
                    }
                }
            }
        }

        player.displayClientMessage(Component.translatable("Done!"), true);
    }
}
