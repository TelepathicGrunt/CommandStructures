package com.telepathicgrunt.commandstructures;

import com.telepathicgrunt.commandstructures.commands.SpawnPiecesCommand;
import com.telepathicgrunt.commandstructures.commands.StructureSpawnCommand;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class CommandStructuresMain implements ModInitializer {

    public static final Logger LOGGER = LogManager.getLogger();
    public static final String MODID = "command_structures";

    @Override
    public void onInitialize() {
        CommandRegistrationCallback.EVENT.register((dispatcher, buildContext, dedicated) -> StructureSpawnCommand.createCommand(dispatcher, buildContext));
        CommandRegistrationCallback.EVENT.register((dispatcher, buildContext, dedicated) -> SpawnPiecesCommand.dataGenCommand(dispatcher, buildContext));

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
