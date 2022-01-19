package com.telepathicgrunt.commandstructures;

import com.telepathicgrunt.commandstructures.commands.ConfiguredFeatureSpawnCommand;
import com.telepathicgrunt.commandstructures.commands.PlacedFeatureSpawnCommand;
import com.telepathicgrunt.commandstructures.commands.RawStructureSpawnCommand;
import com.telepathicgrunt.commandstructures.commands.SpawnPiecesCommand;
import com.telepathicgrunt.commandstructures.commands.StructureSpawnCommand;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@Mod(CommandStructuresMain.MODID)
public class CommandStructuresMain {

    public static final Logger LOGGER = LogManager.getLogger();
    public static final String MODID = "command_structures";

    public CommandStructuresMain() {
        IEventBus forgeBus = MinecraftForge.EVENT_BUS;
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
        StructureSpawnCommand.dataGenCommand(event.getDispatcher());
        SpawnPiecesCommand.dataGenCommand(event.getDispatcher());
        ConfiguredFeatureSpawnCommand.dataGenCommand(event.getDispatcher());
        PlacedFeatureSpawnCommand.dataGenCommand(event.getDispatcher());
        RawStructureSpawnCommand.dataGenCommand(event.getDispatcher());
    }
}
