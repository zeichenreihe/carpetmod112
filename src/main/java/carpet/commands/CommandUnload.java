package carpet.commands;

import java.util.Collections;
import java.util.List;

import org.jetbrains.annotations.Nullable;

import carpet.CarpetSettings;
import carpet.utils.ChunkLoading;

import net.minecraft.server.command.exception.CommandException;
import net.minecraft.server.command.exception.IncorrectUsageException;
import net.minecraft.server.command.source.CommandSource;
import net.minecraft.server.command.exception.InvalidNumberException;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;

public class CommandUnload extends CommandCarpetBase {
	@Override
	public String getUsage(CommandSource sender) {
		return "Usage: unload <brief|verbose|order> <X1> <Y1> <Z1> [<x2> <y2> <z2>]";
	}

	@Override
	public String getName() {
		return "unload";
	}


	public void print_multi_message(List<String> messages, CommandSource sender) {
		for (String line : messages) {
			sendSuccess(sender, this, line);
		}
	}

	@Override
	public void run(MinecraftServer server, CommandSource sender, String[] args) throws CommandException {
		if (!command_enabled("commandUnload", sender)) return;
		if (args.length != 0 && args.length != 1 && args.length != 2 && args.length != 4 && args.length != 7) {
			throw new IncorrectUsageException(getUsage(sender));
		}
		BlockPos pos = sender.getSourceBlockPos();
		BlockPos pos2 = null;
		boolean verbose = false;
		boolean order = false;
		boolean custom_dim = false;
		int custom_dim_id = 0;
		if (args.length > 0) {
			verbose = "verbose".equalsIgnoreCase(args[0]);
		}
		if (args.length > 0) {
			order = "order".equalsIgnoreCase(args[0]);
		}

		if (args.length >= 4) {
			pos = parseBlockPos(sender, args, 1, false);
		}
		if (args.length >= 7) {
			pos2 = parseBlockPos(sender, args, 4, false);
		}
		if (args.length > 0) {
			if ("overworld".equalsIgnoreCase(args[0])) {
				custom_dim = true;
				custom_dim_id = 0;
			}
			if ("nether".equalsIgnoreCase(args[0])) {
				custom_dim = true;
				custom_dim_id = -1;
			}
			if ("end".equalsIgnoreCase(args[0])) {
				custom_dim = true;
				custom_dim_id = 1;
			}
			if (custom_dim && args.length > 1) {
				if ("verbose".equalsIgnoreCase(args[1])) {
					verbose = true;
				}
			}
		}

		if (order) {
			List<String> orders = ChunkLoading.check_unload_order((ServerWorld) sender.getSourceWorld(), pos, pos2);
			print_multi_message(orders, sender);
			return;
		}
		ServerWorld world = (ServerWorld) (custom_dim ? server.getWorld(custom_dim_id) : sender.getSourceWorld());
		sendSuccess(sender, this, "Chunk unloading report for " + world.dimension.getType());
		List<String> report = ChunkLoading.test_save_chunks(world, pos, verbose);
		print_multi_message(report, sender);
	}

	@Override
	public List<String> getSuggestions(MinecraftServer server, CommandSource sender, String[] args, @Nullable BlockPos pos) {
		if (!CarpetSettings.commandUnload) {
			return Collections.emptyList();
		}
		if (args.length == 1) {
			return suggestMatching(args, "verbose", "brief", "order", "nether", "overworld", "end");
		}
		if (args.length == 2 && ("nether".equalsIgnoreCase(args[0]) || "overworld".equalsIgnoreCase(args[0]) || "end".equalsIgnoreCase(args[0]))) {
			return suggestMatching(args, "verbose");
		}
		if (args.length > 1 && args.length <= 4) {
			return suggestCoordinate(args, 1, pos);
		}
		if (args.length > 4 && args.length <= 7) {
			return suggestCoordinate(args, 4, pos);
		}
		return Collections.emptyList();
	}
}
