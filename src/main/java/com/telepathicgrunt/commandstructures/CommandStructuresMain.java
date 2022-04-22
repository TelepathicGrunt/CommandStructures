package com.telepathicgrunt.commandstructures;

import com.telepathicgrunt.commandstructures.commands.ConfiguredFeatureSpawnCommand;
import com.telepathicgrunt.commandstructures.commands.PlacedFeatureSpawnCommand;
import com.telepathicgrunt.commandstructures.commands.RawStructureSpawnCommand;
import com.telepathicgrunt.commandstructures.commands.SpawnPiecesCommand;
import com.telepathicgrunt.commandstructures.commands.StructureSpawnCommand;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.quiltmc.loader.api.ModContainer;
import org.quiltmc.qsl.base.api.entrypoint.ModInitializer;
import org.quiltmc.qsl.command.api.CommandRegistrationCallback;

public class CommandStructuresMain implements ModInitializer {

    public static final Logger LOGGER = LogManager.getLogger();
    public static final String MODID = "command_structures";

    @Override
    public void onInitialize(ModContainer mod) {
        CommandRegistrationCallback.EVENT.register((dispatcher, integrated, dedicated) -> StructureSpawnCommand.dataGenCommand(dispatcher));
        CommandRegistrationCallback.EVENT.register((dispatcher, integrated, dedicated) -> SpawnPiecesCommand.dataGenCommand(dispatcher));
        CommandRegistrationCallback.EVENT.register((dispatcher, integrated, dedicated) -> ConfiguredFeatureSpawnCommand.dataGenCommand(dispatcher));
        CommandRegistrationCallback.EVENT.register((dispatcher, integrated, dedicated) -> PlacedFeatureSpawnCommand.dataGenCommand(dispatcher));
        CommandRegistrationCallback.EVENT.register((dispatcher, integrated, dedicated) -> RawStructureSpawnCommand.dataGenCommand(dispatcher));

        // Silences logspam due to some mc implementations with spawning structures rawly like Mineshafts
        Logger rootLogger = LogManager.getRootLogger();
        if (rootLogger instanceof org.apache.logging.log4j.core.Logger) {
            ((org.apache.logging.log4j.core.Logger) rootLogger).addFilter(new LogSpamFiltering());
        }
        else {
            LOGGER.error("Registration failed with unexpected class: {}", rootLogger.getClass());
        }
    }
}
