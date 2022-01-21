package com.telepathicgrunt.commandstructures.commands;

import com.mojang.brigadier.CommandDispatcher;
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
import net.minecraft.network.chat.TextComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;
import net.minecraft.world.level.levelgen.placement.PlacementModifierType;

import java.util.Set;
import java.util.stream.Collectors;

public class PlacedFeatureSpawnCommand {
    private static final ResourceLocation BIOME_PLACEMENT_RL = new ResourceLocation("minecraft", "biome");

    public static void dataGenCommand(CommandDispatcher<CommandSourceStack> dispatcher) {
        String commandString = "spawnplacedfeature";
        String locationArg = "location";
        String rlArg = "placedfeatureresourcelocation";

        LiteralCommandNode<CommandSourceStack> source = dispatcher.register(Commands.literal(commandString)
            .requires((permission) -> permission.hasPermission(2))
            .then(Commands.argument(locationArg, Vec3Argument.vec3())
            .then(Commands.argument(rlArg, ResourceLocationArgument.id())
            .suggests((ctx, sb) -> SharedSuggestionProvider.suggestResource(placedFeatureSuggestions(ctx), sb))
            .executes(cs -> {
                generateStructure(Vec3Argument.getCoordinates(cs, locationArg), cs.getArgument(rlArg, ResourceLocation.class), cs);
                return 1;
            }))));

        dispatcher.register(Commands.literal(commandString).redirect(source));
    }

    private static Set<ResourceLocation> placedFeatureSuggestions(CommandContext<CommandSourceStack> cs) {
        return cs.getSource().getLevel().registryAccess().registryOrThrow(Registry.PLACED_FEATURE_REGISTRY).keySet();
    }

    private static void generateStructure(Coordinates coordinates, ResourceLocation placedFeatureRL, CommandContext<CommandSourceStack> cs) {
        ServerLevel level = cs.getSource().getLevel();
        BlockPos centerPos = coordinates.getBlockPos(cs.getSource());
        PlacedFeature placedFeature = cs.getSource().registryAccess().registryOrThrow(Registry.PLACED_FEATURE_REGISTRY).get(placedFeatureRL);
        PlacementModifierType<?> biomePlacement = level.registryAccess().registryOrThrow(Registry.PLACEMENT_MODIFIER_REGISTRY).get(BIOME_PLACEMENT_RL);

        if(placedFeature == null) {
            String errorMsg = placedFeatureRL + " placedfeature does not exist in registry";
            CommandStructuresMain.LOGGER.error(errorMsg);
            throw new CommandRuntimeException(new TextComponent(errorMsg));
        }

        BlockPos worldBottomPos = new BlockPos(centerPos.getX(), level.dimensionType().minY(), centerPos.getZ());

        PlacedFeature noBiomeCheckPlacedFeature = new PlacedFeature(
                placedFeature.feature,
                placedFeature.getPlacement().stream()
                        .filter(placementModifier -> placementModifier.type() != biomePlacement)
                        .collect(Collectors.toList()));

        boolean success = noBiomeCheckPlacedFeature.place(level, level.getChunkSource().getGenerator(), level.getRandom(), worldBottomPos);

        if(!success) {
            String errorMsg = placedFeatureRL + " placedfeature failed to be spawned. (It may have internal checks for valid spots or is chance based)";
            CommandStructuresMain.LOGGER.error(errorMsg);
            throw new CommandRuntimeException(new TextComponent(errorMsg));
        }

        Utilities.refreshChunksOnClients(level);
    }
}
