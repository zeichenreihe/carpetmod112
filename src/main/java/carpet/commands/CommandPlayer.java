package carpet.commands;

import carpet.CarpetSettings;
import carpet.helpers.EntityPlayerActionPack;
import carpet.patches.FakeServerPlayerEntity;
import carpet.utils.extensions.ActionPackOwner;
import net.minecraft.command.CommandException;
import net.minecraft.command.CommandSource;
import net.minecraft.command.IncorrectUsageException;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.MinecraftServer;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.util.math.BlockPos;

import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

public class CommandPlayer extends CommandCarpetBase
{
    @Override
    public String getCommandName()
    {
        return "player";
    }

    @Override
    public String getUsageTranslationKey(CommandSource sender)
    {
        return "player <spawn|kill|stop|drop|swapHands|mount|dismount> <player_name>  OR /player <use|attack|jump> <player_name> <once|continuous|interval.. ticks>";
    }

    @Override
    public void method_3279(MinecraftServer server, CommandSource sender, String[] args) throws CommandException
    {
        if (!command_enabled("commandPlayer", sender)) return;
        if (args.length < 2)
        {
            throw new IncorrectUsageException("player <x> action");
        }
        String playerName = args[0];
        String action = args[1];
        ServerPlayerEntity player = server.getPlayerManager().getPlayer(playerName);
        if (sender instanceof PlayerEntity)
        {
            PlayerEntity sendingPlayer = getAsPlayer(sender);
            if (!(server.getPlayerManager().isOperator(sendingPlayer.getGameProfile())))
            {
                if (!(sendingPlayer == player || player == null || player instanceof FakeServerPlayerEntity))
                {
                    throw new IncorrectUsageException("Non OP players can't control other players");
                }
            }
        }
        if (player == null && !action.equalsIgnoreCase("spawn") && !action.equalsIgnoreCase("respawn"))
        {
            throw new IncorrectUsageException("player doesn't exist");
        }
        EntityPlayerActionPack actionPack = player == null ? null : ((ActionPackOwner) player).getActionPack();
        if ("use".equalsIgnoreCase(action) || "attack".equalsIgnoreCase(action) || "jump".equalsIgnoreCase(action))
        {
            String option = "once";
            int interval = 0;
            if (args.length > 2)
            {
                option = args[2];
                if (args.length > 3 && option.equalsIgnoreCase("interval"))
                {
                    interval = parseClampedInt(args[3],2,72000);
                }
            }
            if (action.equalsIgnoreCase("use"))
            {
                if (option.equalsIgnoreCase("once"))
                    actionPack.useOnce();
                if (option.equalsIgnoreCase("continuous"))
                    actionPack.setUseForever();
                if (option.equalsIgnoreCase("interval") && interval > 1)
                    actionPack.setUse(interval, 0);
            }
            if (action.equalsIgnoreCase("attack"))
            {
                if (option.equalsIgnoreCase("once"))
                    actionPack.attackOnce();
                if (option.equalsIgnoreCase("continuous"))
                    actionPack.setAttackForever();
                if (option.equalsIgnoreCase("interval") && interval > 1)
                    actionPack.setAttack(interval, 0);
            }
            if (action.equalsIgnoreCase("jump"))
            {
                if (option.equalsIgnoreCase("once"))
                    actionPack.jumpOnce();
                if (option.equalsIgnoreCase("continuous"))
                    actionPack.setJumpForever();
                if (option.equalsIgnoreCase("interval") && interval > 1)
                    actionPack.setJump(interval, 0);
            }
            return;
        }
        if ("stop".equalsIgnoreCase(action))
        {
            actionPack.stop();
            return;
        }
        if ("drop".equalsIgnoreCase(action))
        {
            actionPack.dropItem();
            return;
        }
        if ("swapHands".equalsIgnoreCase(action))
        {
            actionPack.swapHands();
            return;
        }
        if ("spawn".equalsIgnoreCase(action))
        {
            if (player != null)
            {
                throw new IncorrectUsageException("player "+playerName+" already exists");
            }
            if (playerName.length() < 3 || playerName.length() > 16)
            {
                throw new IncorrectUsageException("player names can only be 3 to 16 chars long");
            }
            if (isWhitelistedPlayer(server, playerName) && !sender.canUseCommand(2, "gamemode")) {
                throw new CommandException("You are not allowed to spawn a whitelisted player");
            }
            Vec3d vec3d = sender.getPos();
            double d0 = vec3d.x;
            double d1 = vec3d.y;
            double d2 = vec3d.z;
            double yaw = 0.0D;
            double pitch = 0.0D;
            World world = sender.getWorld();
            int dimension = world.dimension.getDimensionType().getId();
            int gamemode = server.method_3026().getGameModeId();

            if (sender instanceof ServerPlayerEntity)
            {
                ServerPlayerEntity entity = getAsPlayer(sender);
                yaw = (double)entity.yaw;
                pitch = (double)entity.pitch;
                gamemode = entity.interactionManager.getGameMode().getGameModeId();
            }
            if (args.length >= 5)
            {
                d0 = getCoordinate(d0, args[2], true).getAmount();
                d1 = getCoordinate(d1, args[3], -4096, 4096, false).getAmount();
                d2 = getCoordinate(d2, args[4], true).getAmount();
                yaw = getCoordinate(yaw, args.length > 5 ? args[5] : "~", false).getAmount();
                pitch = getCoordinate(pitch, args.length > 6 ? args[6] : "~", false).getAmount();
            }
            if (args.length >= 8)
            {
                String dimension_string = args[7];
                dimension = 0;
                if ("nether".equalsIgnoreCase(dimension_string))
                {
                    dimension = -1;
                }
                if ("end".equalsIgnoreCase(dimension_string))
                {
                    dimension = 1;
                }
            }
            if (args.length >= 9)
            {
                gamemode = parseClampedInt(args[8],0,3);
                if (gamemode == 1 && !sender.canUseCommand(2, "gamemode")) {
                    throw new CommandException("You are not allowed to spawn a creative player");
                }
            }
            FakeServerPlayerEntity.createFake(playerName, server, d0, d1, d2, yaw, pitch, dimension, gamemode );
            return;
        }
        if ("kill".equalsIgnoreCase(action))
        {
            if (!(player instanceof FakeServerPlayerEntity))
            {
                throw new IncorrectUsageException("use /kill or /kick on regular players");
            }
            player.kill();
            return;
        }
        if ("shadow".equalsIgnoreCase(action))
        {
            if (player instanceof FakeServerPlayerEntity)
            {
                throw new IncorrectUsageException("cannot shadow server side players");
            }
            FakeServerPlayerEntity.createShadow(server, player);
            return;
        }
        if ("mount".equalsIgnoreCase(action))
        {
            actionPack.mount();
            return;
        }
        if ("dismount".equalsIgnoreCase(action))
        {
            actionPack.dismount();
            return;
        }
        //FP only
        if (action.matches("^(?:move|sneak|sprint|look)$"))
        {
            if (player != null && !(player instanceof FakeServerPlayerEntity))
                throw new IncorrectUsageException(action+" action could only be run on existing fake players");

            if ("move".equalsIgnoreCase(action))
            {
                if (args.length < 3)
                    throw new IncorrectUsageException("/player "+playerName+" go <forward|backward|left|right>");
                String where = args[2];
                if ("forward".equalsIgnoreCase(where))
                {
                    actionPack.setForward(1.0F);
                    return;
                }
                if ("backward".equalsIgnoreCase(where))
                {
                    actionPack.setForward(-1.0F);
                    return;
                }
                if ("left".equalsIgnoreCase(where))
                {
                    actionPack.setStrafing(-1.0F);
                    return;
                }
                if ("right".equalsIgnoreCase(where))
                {
                    actionPack.setStrafing(1.0F);
                    return;
                }
                throw new IncorrectUsageException("/player "+playerName+" go <forward|backward|left|right>");
            }
            if ("sneak".equalsIgnoreCase(action))
            {
                actionPack.setSneaking(true);
                return;
            }
            if ("sprint".equalsIgnoreCase(action))
            {
                actionPack.setSprinting(true);
                return;
            }
            if ("look".equalsIgnoreCase(action))
            {
                if (args.length < 3)
                    throw new IncorrectUsageException("/player "+playerName+" look <left|right|north|south|east|west|up|down| yaw .. pitch>");
                if (args[2].charAt(0)>='A' && args[2].charAt(0)<='z')
                {
                    if(!actionPack.look(args[2].toLowerCase()))
                    {
                        throw new IncorrectUsageException("look direction is north, south, east, west, up or down");
                    }
                }
                else if (args.length > 3)
                {
                    float yaw = (float) getCoordinate(player.yaw, args[2], false).getAmount();
                    float pitch = (float) getCoordinate(player.pitch, args[3], false).getAmount();
                    actionPack.look(yaw,pitch);
                }
                else
                {
                    throw new IncorrectUsageException("/player "+playerName+" look <north|south|east|west|up|down| yaw .. pitch>");
                }
                return;
            }
        }
        if ("despawn".equalsIgnoreCase(action))
        {
            if (!(player instanceof FakeServerPlayerEntity))
            {
                throw new IncorrectUsageException("this is a bot only command");
            }
            ((FakeServerPlayerEntity)player).despawn();
            return;
        }
        if ("respawn".equalsIgnoreCase(action))
        {
            if (player != null)
            {
                throw new IncorrectUsageException("player "+playerName+" already exists");
            }
            if (playerName.length() < 3 || playerName.length() > 16)
            {
                throw new IncorrectUsageException("player names can only be 3 to 16 chars long");
            }
            if (isWhitelistedPlayer(server, playerName) && !sender.canUseCommand(2, "gamemode")) {
                throw new CommandException("You are not allowed to spawn a whitelisted player");
            }
            FakeServerPlayerEntity.create(playerName, server);
            return;
        }
        throw new IncorrectUsageException("unknown action: "+action);
    }

    private boolean isWhitelistedPlayer(MinecraftServer server, String playerName) {
        for(String s : server.getPlayerManager().getWhitelistedNames()){
            if(s.toLowerCase().equals(playerName.toLowerCase())) return true;
        }
        return false;
    }

    @Override
    public List<String> method_10738(MinecraftServer server, CommandSource sender, String[] args, @Nullable BlockPos targetPos)
    {
        if (!CarpetSettings.commandPlayer)
        {
            return Collections.emptyList();
        }
        if (args.length == 1)
        {
            Set<String> players = new HashSet<>(Arrays.asList(server.getPlayerNames()));
            players.add("Steve");
            players.add("Alex");
            return method_2894(args, players.toArray(new String[0]));
        }
        if (args.length == 2)
        {
            //currently for all, needs to be restricted for Fake plaeyrs
            return method_2894(args,
                    "spawn","kill","attack","use","jump","stop","shadow",
                    "swapHands","drop","mount","dismount",
                    "move","sneak","sprint","look", "despawn", "respawn");
        }
        if (args.length == 3 && (args[1].matches("^(?:use|attack|jump)$")))
        {
            //currently for all, needs to be restricted for Fake plaeyrs
            return method_2894(args, "once","continuous","interval");
        }
        if (args.length == 4 && (args[1].equalsIgnoreCase("interval")))
        {
            //currently for all, needs to be restricted for Fake plaeyrs
            return method_2894(args, "20");
        }
        if (args.length == 3 && (args[1].equalsIgnoreCase("move")))
        {
            return method_2894(args, "left", "right","forward","backward");
        }
        if (args.length == 3 && (args[1].equalsIgnoreCase("look")))
        {
            return method_2894(args, "left", "right","north","south","east","west","up","down");
        }
        if (args.length > 2 && (args[1].equalsIgnoreCase("spawn") ))
        {
            if (args.length <= 5)
            {
                return method_10707(args, 2, targetPos);
            }
            else if (args.length <= 7)
            {
                return method_2894(args, "0.0");
            }
            else if (args.length == 8)
            {
                return method_2894(args, "overworld", "end", "nether");
            }
            else if (args.length == 9)
            {
                return method_2894(args, "0", "1", "2", "3");
            }
        }
        return Collections.emptyList();
    }
}
