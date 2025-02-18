package carpet.commands;

import carpet.CarpetSettings;
import carpet.mixin.accessors.ServerChunkProviderAccessor;
import it.unimi.dsi.fastutil.HashCommon;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import org.jetbrains.annotations.Nullable;

import net.minecraft.server.command.exception.CommandException;
import net.minecraft.server.command.source.CommandSource;
import net.minecraft.server.command.exception.IncorrectUsageException;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.world.chunk.ServerChunkCache;
import net.minecraft.text.LiteralText;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.World;
import net.minecraft.world.chunk.WorldChunk;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.lang.reflect.Field;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;

public class CommandLoadedChunks extends CommandCarpetBase {
	@Override
	public String getUsage(CommandSource sender) {
		return "Usage: loadedChunks <size | key | value>";
	}

	@Override
	public String getName() {
		return "loadedChunks";
	}


	@Override
	public void run(MinecraftServer server, CommandSource sender, String[] args) throws CommandException {
		if (!command_enabled("commandLoadedChunks", sender)) return;
		if (args.length == 0) throw new IncorrectUsageException(getUsage(sender));

		try {
			switch (args[0]) {
				case "size":
					size(sender);
					break;
				case "search":
					if (args.length != 3) throw new IncorrectUsageException(getUsage(sender));
					search(
							sender,
							parseChunkPosition(args[1], sender.getSourceBlockPos().getX()),
							parseChunkPosition(args[2], sender.getSourceBlockPos().getZ())
					);
					break;
				case "remove":
					if (args.length != 3) throw new IncorrectUsageException(getUsage(sender));
					remove(
							sender,
							parseChunkPosition(args[1], sender.getSourceBlockPos().getX()),
							parseChunkPosition(args[2], sender.getSourceBlockPos().getZ())
					);
					break;
				case "add":
					if (args.length != 3) throw new IncorrectUsageException(getUsage(sender));
					add(
							sender,
							parseChunkPosition(args[1], sender.getSourceBlockPos().getX()),
							parseChunkPosition(args[2], sender.getSourceBlockPos().getZ())
					);
					break;
				case "inspect":
					inspect(sender, args);
					break;
				case "dump":
					dump(sender);
					break;
				default:
					throw new IncorrectUsageException(getUsage(sender));
			}
		} catch (Exception exception) {
			exception.printStackTrace();
			throw new CommandException(exception.getMessage());
		}

	}

	private void dump(CommandSource sender) {
		Long2ObjectOpenHashMap<WorldChunk> loadedChunks = getLoadedChunks(sender);

		// TODO: arg with file name? and option for /tmp?
		String fileName = "loadedchunks-" + new SimpleDateFormat("yyyy-MM-dd-HH-mm-ss-SSSS").format(new Date()) + ".csv";
		try (PrintWriter pw = new PrintWriter(new BufferedWriter(new FileWriter(fileName)))) {
			pw.println("index,key,x,z,hash");
			long[] keys = getKeys(loadedChunks);
			Object[] values = getValues(loadedChunks);
			int n = getN(loadedChunks);
			for (int i = 0; i <= n; i++) {
				long key = keys[i];
				WorldChunk val = (WorldChunk) values[i];
				if (val == null) {
					pw.println(i + ",,,,");
				} else {
					pw.printf("%d,%d,%d,%d,%d\n", i, key, val.chunkX, val.chunkZ, HashCommon.mix(key) & (n - 1));
				}
			}
			pw.flush();
		} catch (Throwable t) {
			t.printStackTrace();
		}
		sendSuccess(sender, this, "Written to %s", fileName);
	}

	private static Long2ObjectOpenHashMap<WorldChunk> getLoadedChunks(CommandSource sender) {
		World world = sender.getSourceWorld();
		ServerChunkCache provider = (ServerChunkCache) world.getChunkSource();
		return (Long2ObjectOpenHashMap<WorldChunk>) ((ServerChunkProviderAccessor) provider).getLoadedChunksMap();
	}

	private static void size(CommandSource sender) throws NoSuchFieldException, IllegalAccessException {
		Long2ObjectOpenHashMap<WorldChunk> loadedChunks = getLoadedChunks(sender);
		sender.sendMessage(new LiteralText(String.format("Hashmap size is %d, %.2f", loadedChunks.size(), getFillLevel(loadedChunks))));
	}

	private void inspect(CommandSource sender, String[] args) throws CommandException, NoSuchFieldException, IllegalAccessException {
		World world = sender.getSourceWorld();
		Long2ObjectOpenHashMap<WorldChunk> loadedChunks = getLoadedChunks(sender);
		Object[] chunks = getValues(loadedChunks);
		int mask = getMask(loadedChunks);
		int start = 0;
		int end = chunks.length;
		Optional<Long> keyClass = Optional.empty();
		for (int i = 1; i < args.length; i++) {
			switch (args[i]) {
				case "from":
					start = Integer.parseInt(args[++i]);
					break;
				case "to":
					end = Integer.parseInt(args[++i]);
					break;
				case "class":
					keyClass = Optional.of(Long.valueOf(args[++i]));
					break;
				default:
					throw new IncorrectUsageException(getUsage(sender));
			}
		}
		ArrayList<String> inspections = new ArrayList<>();
		String last = "";
		int lastN = 0;
		for (int i = start; (i & mask) != (end & mask); i++) {
			WorldChunk chunk = (WorldChunk) chunks[i & mask];
			if (keyClass.isPresent()) {
				if (chunk == null) {
					if (!last.equals("null")) {
						if (lastN > 0) inspections.add(String.format("... %d %s", lastN, last));
						last = "null";
						lastN = 0;
					}
					lastN++;
					continue;
				}
				if (getKeyClass(chunk, mask) != keyClass.get()) {
					if (!last.equals("chunks")) {
						if (lastN > 0) inspections.add(String.format("... %d %s", lastN, last));
						last = "chunks";
						lastN = 0;
					}
					lastN++;
					continue;
				}
			}
			if (!last.isEmpty()) {
				if (lastN > 0) inspections.add(String.format("... %d %s", lastN, last));
				last = "";
				lastN = 0;
			}
			String formatted = formatChunk(world, chunk, i & mask, mask);
			inspections.add(formatted);

		}
		String result = inspections.stream().collect(Collectors.joining(", ", "[", "]"));
		sender.sendMessage(new LiteralText(result));
	}

	private static void search(CommandSource sender, int chunkX, int chunkZ) throws NoSuchFieldException, IllegalAccessException {
		World world = sender.getSourceWorld();
		Long2ObjectOpenHashMap<WorldChunk> loadedChunks = getLoadedChunks(sender);
		Object[] chunks = getValues(loadedChunks);
		int mask = getMask(loadedChunks);
		for (int i = 0; i < chunks.length; i++) {
			WorldChunk chunk = (WorldChunk) chunks[i];
			if (chunk == null) continue;
			if (chunk.chunkX != chunkX || chunk.chunkZ != chunkZ) continue;
			sender.sendMessage(new LiteralText(formatChunk(world, chunk, i, mask)));
			break;
		}
	}

	private static final HashMap<Long, WorldChunk> tempChunks = new HashMap<>();

	private static void add(CommandSource sender, int x, int z) {
		long hash = ChunkPos.toLong(x, z);
		if (!tempChunks.containsKey(hash)) {
			sender.sendMessage(new LiteralText(String.format("Chunk (%d, %d) couldn't been found", x, z)));
			return;
		}
		WorldChunk chunk = tempChunks.get(hash);
		Long2ObjectOpenHashMap<WorldChunk> loadedChunks = getLoadedChunks(sender);
		loadedChunks.put(hash, chunk);
		sender.sendMessage(new LiteralText(String.format("Chunk (%d, %d) has been added back", x, z)));
	}

	private static void remove(CommandSource sender, int x, int z) {
		long hash = ChunkPos.toLong(x, z);

		Long2ObjectOpenHashMap<WorldChunk> loadedChunks = getLoadedChunks(sender);
		if (!loadedChunks.containsKey(hash)) {
			sender.sendMessage(new LiteralText(String.format("Chunk (%d, %d) is not in loaded list", x, z)));
		}
		WorldChunk chunk = loadedChunks.remove(hash);
		tempChunks.put(hash, chunk);
		sender.sendMessage(new LiteralText(String.format("Chunk (%d, %d) has been removed", x, z)));
	}

	private static String formatChunk(World world, WorldChunk chunk, int pos, int mask) {
		if (chunk == null) {
			return String.format("%d: null", pos);
		}

		return String.format("%d: %s(%d, %d) %d", pos, getChunkDescriber(world, chunk), chunk.chunkX, chunk.chunkZ, getKeyClass(chunk, mask));
	}

	private static String getChunkDescriber(World world, WorldChunk chunk) {
		int x = chunk.chunkX, z = chunk.chunkZ;
		long hash = ChunkPos.toLong(x, z);
		String describer = "";
		if (world.isSpawnChunk(x, z)) {
			describer += "S ";
		}
		if (((hash ^ (hash >>> 16)) & 0xFFFF) == 0) {
			describer += "0 ";
		}
		return describer;
	}

	private static long getKeyClass(WorldChunk chunk, int mask) {
		return HashCommon.mix(ChunkPos.toLong(chunk.chunkX, chunk.chunkZ)) & mask;
	}

	private static int getMaxField(Long2ObjectOpenHashMap<WorldChunk> hashMap) throws NoSuchFieldException, IllegalAccessException {
		Field maxFill = Long2ObjectOpenHashMap.class.getDeclaredField("maxFill");
		maxFill.setAccessible(true);
		return (int) maxFill.get(hashMap);
	}

	protected static int getMask(Long2ObjectOpenHashMap<WorldChunk> hashMap) throws NoSuchFieldException, IllegalAccessException {
		Field mask = Long2ObjectOpenHashMap.class.getDeclaredField("mask");
		mask.setAccessible(true);
		return (int) mask.get(hashMap);
	}

	private static float getFillLevel(Long2ObjectOpenHashMap<WorldChunk> hashMap) throws NoSuchFieldException, IllegalAccessException {
		return (float) hashMap.size() / getMaxField(hashMap);
	}

	private static Object[] getValues(Long2ObjectOpenHashMap<WorldChunk> hashMap) throws NoSuchFieldException, IllegalAccessException {
		Field value = Long2ObjectOpenHashMap.class.getDeclaredField("value");
		value.setAccessible(true);
		return (Object[]) value.get(hashMap);
	}

	private static long[] getKeys(Long2ObjectOpenHashMap<WorldChunk> hashMap) throws NoSuchFieldException, IllegalAccessException {
		Field key = Long2ObjectOpenHashMap.class.getDeclaredField("key");
		key.setAccessible(true);
		return (long[]) key.get(hashMap);
	}
	private static int getN(Long2ObjectOpenHashMap<WorldChunk> hashMap) throws NoSuchFieldException, IllegalAccessException {
		Field n = Long2ObjectOpenHashMap.class.getDeclaredField("n");
		n.setAccessible(true);
		return (int) n.get(hashMap);
	}

	@Override
	public List<String> getSuggestions(MinecraftServer server, CommandSource sender, String[] args, @Nullable BlockPos targetPos) {
		if (!CarpetSettings.commandLoadedChunks) {
			return Collections.emptyList();
		}

		if (args.length == 1) {
			return suggestMatching(args, "size", "inspect", "search", "remove", "add", "dump");
		}

		switch (args[0]) {
			case "inspect":
				switch (args[args.length - 1]) {
					case "class":
					case "from":
					case "to":
						return Collections.emptyList();
				}
				return suggestMatching(args, "class", "from", "to");
			case "search":
			case "remove":
			case "add":
				if (args.length > 3) return Collections.emptyList();
				return getChunkCompletions(sender, args, 2);
		}

		return Collections.emptyList();
	}


	public static List<String> getChunkCompletions(CommandSource sender, String[] args, int index) {
		int chunkX = sender.getSourceBlockPos().getX() >> 4;
		int chunkZ = sender.getSourceBlockPos().getZ() >> 4;

		if (args.length == index) {
			return suggestMatching(args, Integer.toString(chunkX), "~");
		} else if (args.length == index + 1) {
			return suggestMatching(args, Integer.toString(chunkZ), "~");
		} else {
			return Collections.emptyList();
		}
	}
}