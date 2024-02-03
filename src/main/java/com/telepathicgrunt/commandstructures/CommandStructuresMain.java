package com.telepathicgrunt.commandstructures;

import com.telepathicgrunt.commandstructures.commands.FillStructureVoidCommand;
import com.telepathicgrunt.commandstructures.commands.SpawnMobsCommand;
import com.telepathicgrunt.commandstructures.commands.SpawnPiecesCommand;
import com.telepathicgrunt.commandstructures.commands.StructureSpawnCommand;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@Mod(CommandStructuresMain.MODID)
public class CommandStructuresMain {

    public static final Logger LOGGER = LogManager.getLogger();
    public static final String MODID = "command_structures";

    public CommandStructuresMain() {
        IEventBus forgeBus = NeoForge.EVENT_BUS;
        forgeBus.addListener(this::registerCommand);

        // Silences logspam due to some mc implementations with spawning structures rawly like Mineshafts
        Logger rootLogger = LogManager.getRootLogger();
        if (rootLogger instanceof org.apache.logging.log4j.core.Logger) {
            ((org.apache.logging.log4j.core.Logger) rootLogger).addFilter(new LogSpamFiltering());
        }
        else {
            LOGGER.error("Registration failed with unexpected class: {}", rootLogger.getClass());
        }
    }

    private void registerCommand(RegisterCommandsEvent event) {
        StructureSpawnCommand.createCommand(event.getDispatcher(), event.getBuildContext());
        SpawnPiecesCommand.createCommand(event.getDispatcher(), event.getBuildContext());
        SpawnMobsCommand.createCommand(event.getDispatcher(), event.getBuildContext());
        FillStructureVoidCommand.createCommand(event.getDispatcher(), event.getBuildContext());
    }
}
