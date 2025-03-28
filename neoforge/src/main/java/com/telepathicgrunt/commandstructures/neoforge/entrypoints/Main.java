package com.telepathicgrunt.commandstructures.neoforge.entrypoints;

import com.telepathicgrunt.commandstructures.CommandStructuresMain;
import com.telepathicgrunt.commandstructures.commands.FillStructureVoidCommand;
import com.telepathicgrunt.commandstructures.commands.SpawnMobsCommand;
import com.telepathicgrunt.commandstructures.commands.SpawnPiecesCommand;
import com.telepathicgrunt.commandstructures.commands.StructureSpawnCommand;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

@Mod(CommandStructuresMain.MODID)
public class Main {

    public Main() {
        CommandStructuresMain.CommandStructuresInit();

        IEventBus forgeBus = NeoForge.EVENT_BUS;
        forgeBus.addListener(this::registerCommand);
    }

    private void registerCommand(RegisterCommandsEvent event) {
        StructureSpawnCommand.createCommand(event.getDispatcher(), event.getBuildContext());
        SpawnPiecesCommand.createCommand(event.getDispatcher(), event.getBuildContext());
        SpawnMobsCommand.createCommand(event.getDispatcher(), event.getBuildContext());
        FillStructureVoidCommand.createCommand(event.getDispatcher(), event.getBuildContext());
    }
}
