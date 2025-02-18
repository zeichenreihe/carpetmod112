package carpet.commands;

import carpet.CarpetSettings;
import carpet.helpers.OptimizedTNT;

import net.minecraft.server.command.exception.CommandException;
import net.minecraft.server.command.exception.IncorrectUsageException;
import net.minecraft.server.command.source.CommandSource;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.math.BlockPos;

import java.util.Collections;
import java.util.List;
import java.util.Random;

public class CommandTNT extends CommandCarpetBase {
	public static Random rand = new Random();
	private static final String USAGE = "/tnt [x y z]/clear";

	@Override
	public String getName() {
		return "tnt";
	}

	@Override
	public String getUsage(CommandSource sender) {
		return USAGE;
	}

	@Override
	public void run(MinecraftServer server, CommandSource sender, String[] args) throws CommandException {
		if (args[0].equals("setSeed")) {
			try {
				rand.setSeed(Long.parseLong(args[1]) ^ 0x5DEECE66DL);
				sendSuccess(sender,
						this,
						"RNG TNT angle seed set to " + args[1] +
								(CarpetSettings.TNTAdjustableRandomAngle ? "" : " Enable TNTAdjustableRandomAngle" + " rule or seed wont work.")
				);
			} catch (Exception ignored) {
			}
		} else if (args[0].equals("clear")) {
			sendSuccess(sender, this, "TNT scanning block cleared.");
		} else if (args.length == 3) {
			int x = (int) Math.round(parseTeleportCoordinate(sender.getSourceBlockPos().getX(), args[0], false).getCoordinate());
			int y = (int) Math.round(parseTeleportCoordinate(sender.getSourceBlockPos().getY(), args[1], false).getCoordinate());
			int z = (int) Math.round(parseTeleportCoordinate(sender.getSourceBlockPos().getZ(), args[2], false).getCoordinate());
			OptimizedTNT.setBlastChanceLocation(new BlockPos(x, y, z));
			sendSuccess(sender, this, String.format("TNT scanning block at: %d %d %d", x, y, z));
		} else {
			throw new IncorrectUsageException(getUsage(sender));
		}
	}

	@Override
	public List<String> getSuggestions(MinecraftServer server, CommandSource sender, String[] args, BlockPos targetPos) {
		if (args.length == 0) {
			return Collections.emptyList();
		} else if (args.length == 1) {
			return suggestMatching(args, String.valueOf(targetPos.getX()), "clear");
		} else if (args.length == 2) {
			return suggestMatching(args, String.valueOf(targetPos.getY()));
		} else if (args.length == 3) {
			return suggestMatching(args, String.valueOf(targetPos.getZ()));
		} else {
			return Collections.emptyList();
		}
	}
}
