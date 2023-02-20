package carpet.commands;

import narcolepticfrog.rsmm.MeterCommand;
import net.minecraft.server.command.CommandRegistry;

public class CarpetCommands {
    public static void register(CommandRegistry handler) {
        // Sorted alphabetically to make merge conflicts less likely
        handler.registerCommand(new CommandAutosave());
        handler.registerCommand(new CommandBlockInfo());
        handler.registerCommand(new CommandCarpet());
        handler.registerCommand(new CommandCounter());
        handler.registerCommand(new CommandDebugCarpet());
        handler.registerCommand(new CommandDebuglogger());
        handler.registerCommand(new CommandDistance());
        handler.registerCommand(new CommandEntityInfo());
        handler.registerCommand(new CommandFillBiome());
        handler.registerCommand(new CommandGMC());
        handler.registerCommand(new CommandGMS());
        handler.registerCommand(new CommandGrow());
        handler.registerCommand(new CommandLagSpike());
        handler.registerCommand(new CommandLazyChunkBehavior());
        handler.registerCommand(new CommandLight());
        handler.registerCommand(new CommandLoadChunk());
        handler.registerCommand(new CommandLog());
        handler.registerCommand(new CommandPerimeter());
        handler.registerCommand(new CommandPing());
        handler.registerCommand(new CommandPlayer());
        handler.registerCommand(new CommandProfile());
        handler.registerCommand(new CommandRemoveEntity());
        handler.registerCommand(new CommandRepopulate());
        handler.registerCommand(new CommandRNG());
        handler.registerCommand(new CommandScoreboardPublic());
        handler.registerCommand(new CommandSpawn());
        handler.registerCommand(new CommandStructure());
        handler.registerCommand(new CommandSubscribe());
        handler.registerCommand(new CommandTick());
        handler.registerCommand(new CommandTickingArea());
        handler.registerCommand(new CommandTNT());
        handler.registerCommand(new CommandUnload());
        handler.registerCommand(new CommandUnload13());
        handler.registerCommand(new CommandWaypoint());

        // ----- RSMM Start ----- //
        handler.registerCommand(new MeterCommand());
        // ----- RSMM End ----- //
    }
}
