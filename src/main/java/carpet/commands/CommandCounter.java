package carpet.commands;

import java.util.Collections;
import java.util.List;
import java.util.ArrayList;
import java.util.Locale;

import org.jetbrains.annotations.Nullable;

import carpet.CarpetSettings;
import carpet.helpers.HopperCounter;
import carpet.utils.Messenger;
import net.minecraft.command.CommandException;
import net.minecraft.command.CommandSource;
import net.minecraft.command.IncorrectUsageException;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.DyeColor;
import net.minecraft.util.math.BlockPos;

public class CommandCounter extends CommandCarpetBase
{
    /**
     * Gets the name of the command
     */
    @Override
    public String getUsageTranslationKey(CommandSource sender)
    {
        return "Usage: counter <color> <reset/realtime>";
    }

    @Override
    public String getCommandName()
    {
        return "counter";
    }

    /**
     * Callback for when the command is executed
     */
    @Override
    public void method_3279(MinecraftServer server, CommandSource sender, String[] args) throws CommandException
    {
        if (CarpetSettings.hopperCounters == CarpetSettings.HopperCounters.off && !CarpetSettings.cactusCounter){
            msg(sender, Messenger.m(null, "Need cactusCounter or hopperCounters to be enabled to use this command."));
            return;
        }
        if (args.length == 0) {
            msg(sender, HopperCounter.formatAll(server, false));
            return;
        }
        switch (args[0].toLowerCase(Locale.ROOT)) {
            case "realtime":
                msg(sender, HopperCounter.formatAll(server, true));
                return;
            case "reset":
                HopperCounter.resetAll(server);
                run(sender, this, "All counters restarted.");
                return;
        }
        HopperCounter counter = HopperCounter.getCounter(args[0]);
        if (counter == null) throw new IncorrectUsageException("Invalid color");
        if (args.length == 1) {
            msg(sender, counter.format(server,false, false));
            return;
        }
        switch (args[1].toLowerCase(Locale.ROOT)) {
            case "realtime":
                msg(sender, counter.format(server, true, false));
                return;
            case "reset":
                counter.reset(server);
                run(sender, this, String.format("%s counters restarted.", args[0]));
                return;
        }
        throw new IncorrectUsageException(getUsageTranslationKey(sender));

    }

    @Override
    public List<String> method_10738(MinecraftServer server, CommandSource sender, String[] args, @Nullable BlockPos pos)
    {
        if (CarpetSettings.hopperCounters == CarpetSettings.HopperCounters.off && !CarpetSettings.cactusCounter)
        {
            msg(sender, Messenger.m(null, "Need cactusCounter or hopperCounters to be enabled to use this command."));
            return Collections.<String>emptyList();
        }
        if (args.length == 1)
        {
            List<String> lst = new ArrayList<String>();
            lst.add("reset");
            for (DyeColor clr : DyeColor.values())
            {
                lst.add(clr.name().toLowerCase(Locale.ROOT));
            }
            lst.add("cactus");
            lst.add("all");
            lst.add("realtime");
            String[] stockArr = new String[lst.size()];
            stockArr = lst.toArray(stockArr);
            return method_2894(args, stockArr);
        }
        if (args.length == 2)
        {
            return method_2894(args, "reset", "realtime");
        }
        return Collections.<String>emptyList();
    }
}
