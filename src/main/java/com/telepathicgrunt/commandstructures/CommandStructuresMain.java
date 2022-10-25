package com.telepathicgrunt.commandstructures;

import com.telepathicgrunt.commandstructures.commands.SpawnMobsCommand;
import com.telepathicgrunt.commandstructures.commands.SpawnPiecesCommand;
import com.telepathicgrunt.commandstructures.commands.StructureSpawnCommand;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.core.RegistryAccess;
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
        CommandBuildContext buildContext = new CommandBuildContext(RegistryAccess.BUILTIN.get());
        StructureSpawnCommand.createCommand(event.getDispatcher(), buildContext);
        SpawnPiecesCommand.createCommand(event.getDispatcher(), buildContext);
        SpawnMobsCommand.createCommand(event.getDispatcher(), buildContext);
    }
}
