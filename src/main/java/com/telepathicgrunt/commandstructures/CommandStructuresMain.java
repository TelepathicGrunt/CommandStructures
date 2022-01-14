package com.telepathicgrunt.commandstructures;

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
    }

    private void registerCommand(RegisterCommandsEvent event) {
        StructureSpawnCommand.dataGenCommand(event.getDispatcher());
        SpawnPiecesCommand.dataGenCommand(event.getDispatcher());
        ConfiguredFeatureSpawnCommand.dataGenCommand(event.getDispatcher());
        PlacedFeatureSpawnCommand.dataGenCommand(event.getDispatcher());
    }
}
