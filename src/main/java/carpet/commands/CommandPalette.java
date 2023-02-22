package carpet.commands;

import carpet.mixin.accessors.HashMapPaletteAccessor;
import carpet.mixin.accessors.LinearPaletteAccessor;
import carpet.mixin.accessors.PaletteDataAccessor;
import carpet.mixin.accessors.class_2743Accessor;
import org.jetbrains.annotations.Nullable;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.WoolBlock;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.class_2743;
import net.minecraft.command.AbstractCommand;
import net.minecraft.command.CommandException;
import net.minecraft.command.CommandSource;
import net.minecraft.command.IncorrectUsageException;
import net.minecraft.server.MinecraftServer;
import net.minecraft.text.LiteralText;
import net.minecraft.util.DyeColor;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.chunk.*;
import net.minecraft.world.chunk.palette.*;

import java.util.*;

public class CommandPalette extends CommandCarpetBase {
	/**
	 * Gets the name of the command
	 */

	public String getUsageTranslationKey(CommandSource sender) {
		return "Usage: palette <info | fill | size | posInfo> <X> <Y> <Z> <full | normal> <4 to 13>";
	}

	public String getCommandName() {
		return "palette";
	}

	/**
	 * Callback for when the command is executed
	 */
	public void method_3279(MinecraftServer server, CommandSource sender, String[] args) throws CommandException {
		if (!command_enabled("commandChunk", sender)) return;

		try {
			BlockPos pos = new BlockPos(sender.getBlockPos().getX(), sender.getBlockPos().getY(), sender.getBlockPos().getZ());
			if (args.length < 4 && args[0].equals("posInfo")) {
				throw new IncorrectUsageException(getUsageTranslationKey(sender));
			} else if (args.length >= 4) {
				pos = getBlockPos(sender, args, 1, false);
			}
			World world = sender.getWorld();
			Chunk chunk = world.getChunk(pos);
			ChunkSection[] list = chunk.getBlockStorage();
			int h = pos.getY() >> 4;
			if (h < 0) h = 0;
			if (h > 15) h = 15;
			ChunkSection ebs = list[h];
			class_2743 bsc = ebs.method_11774();
			int bits = ((class_2743Accessor) bsc).getBitsPerBlock();

			switch (args[0]) {
				case "bits":
					sender.sendMessage(new LiteralText("Palette bit size: " + bits));
					return;
				case "size":
					getSize(sender, bsc);
					return;
				case "posInfo":
					boolean isFull = false;
					if (args.length >= 5) isFull = args[4].equals("full");
					Block block = null;
					if (args.length >= 6) block = AbstractCommand.getBlock(sender, args[5]);
					BlockState iblockstate = null;
					if (args.length >= 7 && block != null) {
						iblockstate = method_13901(block, args[6]);
					} else if (block != null) {
						iblockstate = block.getDefaultState();
					}
					infoPalette(sender, bsc, pos, isFull, iblockstate);
					return;
				case "fill":
					int bitSize = -1;
					int type = 1;
					if (args.length >= 5) type = args[4].equals("full") ? 2 : 1;
					if (args.length >= 6) bitSize = parseInt(args[5]);
					fill(sender, bsc, pos, type, bitSize);
					return;
				default:
					throw new IncorrectUsageException(getUsageTranslationKey(sender));

			}
		} catch (Exception e) {
			e.printStackTrace();
			throw new IncorrectUsageException(getUsageTranslationKey(sender));
		}
	}

	private static BlockState[] backup = null;
	private static HashMap<BlockPos, BlockEntity> tileEntityList = new HashMap<>();

	private static void fill(CommandSource sender, class_2743 bsc, BlockPos pos, int type, int bitSize) {
		if (type != 3 && backup != null) type = 3;

		if (bitSize < 1 || bitSize > 64) bitSize = ((PaletteDataAccessor) ((class_2743Accessor) bsc).getPaletteData()).getBitsPerBlock();

		BlockPos basePos = new BlockPos(pos.getX() >>> 4 << 4, pos.getY() >>> 4 << 4, pos.getZ() >>> 4 << 4);
		int color = -1;
		int storeJ = -1;
		if (type != 3) {
			backup = new BlockState[4096];
		}
		HashSet<BlockPos> backupSet = new HashSet<>();
		for (int i = 0; i < 4096; i++) {
			BlockPos set = getBlockIndex(i, basePos);
			if (type == 1) {
				int j = i * bitSize / 64;
				int k = ((i + 1) * bitSize - 1) / 64;

				if (j != k) {
					backup[i] = sender.getWorld().getBlockState(set);
					BlockEntity te = sender.getWorld().getBlockEntity(set);
					if (te != null) {
						tileEntityList.put(set, te);
						sender.getWorld().removeBlockEntity(set);
					}
					sender.getWorld().setBlockState(set, Blocks.GLASS.getDefaultState(), 128);
				}
			} else if (type == 2) {
				backup[i] = sender.getWorld().getBlockState(set);
				BlockEntity te = sender.getWorld().getBlockEntity(set);
				if (te != null) {
					tileEntityList.put(set, te);
					sender.getWorld().removeBlockEntity(set);
				}
				set = getBlockIndex(i, basePos);
				int j = i * bitSize / 64;
				int k = ((i + 1) * bitSize - 1) / 64;

				if (j != storeJ) {
					storeJ = j;
					color = (color + 1) & 15;
				}

				if (j != k) {
					sender.getWorld().setBlockState(set, Blocks.GLASS.getDefaultState(), 128);
				} else {
					sender.getWorld().setBlockState(set, Blocks.STAINED_GLASS.getDefaultState().with(WoolBlock.COLOR, DyeColor.byId(color)),
							128);
				}
			} else if (type == 3) {
				if (backup[i] != null && !backupSet.contains(set)) {
					backupSet.add(set);
					sender.getWorld().setBlockState(set, backup[i], 128);
					BlockEntity te = tileEntityList.get(set);
					if (te != null) {
						sender.getWorld().removeBlockEntity(set);
						te.cancelRemoval();
						sender.getWorld().setBlockEntity(set, te);
					}
				}
			}
		}
		if (type == 3) {
			backup = null;
			tileEntityList.clear();
		}
	}

	private void infoPalette(CommandSource sender, class_2743 bsc, BlockPos pos, boolean full, BlockState blockState) {
		PaletteData bArray = ((class_2743Accessor) bsc).getPaletteData();
		int bits = ((PaletteDataAccessor) bArray).getBitsPerBlock();
		int index = getIndex(pos);
		int i = index * bits;
		int j = i / 64;
		int k = ((index + 1) * bits - 1) / 64;
		int l = i % 64;
		long[] longArray = bArray.getBlockStateIds();

		if (j == k) {
			displayJKBits(sender, longArray[j], l, l + bits - 1, "");
			if (full) {
				for (BlockPos bp : getArrayFromJK(j, k, bits, pos)) {
					sender.sendMessage(new LiteralText(bp.toString()));
				}
			}
		} else {
			displayJKBits(sender, longArray[j], l, 64, "1");
			displayJKBits(sender, longArray[k], 0, (l + bits - 1) % 64, "2");
			if (full) {
				for (BlockPos bp : getArrayFromJK(j, k, bits, pos)) {
					sender.sendMessage(new LiteralText(bp.toString()));
				}
			}
		}
		if (blockState != null && ((class_2743Accessor) bsc).getPalette() instanceof RegistryPalette && j != k) {
			int blockStateBits = Block.BLOCK_STATES.getId(blockState);
			int leftBits = 64 - l;
			int rightBits = bits - leftBits;
			int leftMask = (1 << leftBits) - 1;
			int rightMask = ((1 << rightBits) - 1) << leftBits;
			int blockStateMaskL = blockStateBits & leftMask;
			int blockStateMaskR = blockStateBits & rightMask;
			sender.sendMessage(new LiteralText("Left bit match:"));
			for(int itr = 0; itr < Block.BLOCK_STATES.size(); itr++){
				BlockState ibs = Block.BLOCK_STATES.fromId(itr);
				if(ibs != null) {
					int left = itr & leftMask;
					if(left == blockStateMaskL){
						String s = String.format("%"+bits+"s", Integer.toBinaryString(itr)).replace(' ', '0') + " " + ibs.toString().replace("minecraft:", "");
						sender.sendMessage(new LiteralText(s));
					}
				}
			}
			sender.sendMessage(new LiteralText("Right bit match:"));
			for(int itr = 0; itr < Block.BLOCK_STATES.size(); itr++){
				BlockState ibs = Block.BLOCK_STATES.fromId(itr);
				if(ibs != null) {
					int right = itr & rightMask;
					if(right == blockStateMaskR){
						String s = String.format("%"+bits+"s", Integer.toBinaryString(itr)).replace(' ', '0') + " " + ibs.toString().replace("minecraft:", "");
						sender.sendMessage(new LiteralText(s));
					}
				}
			}
		}
	}

	private static void displayJKBits(CommandSource sender, long longString, long l1, long l2, String append) {
		StringBuilder sb = new StringBuilder();

		String add = "§f";
		for (int bitNum = 0; bitNum < 64; bitNum++) {
			char s = (longString & 1) == 1 ? '1' : '0';
			longString = longString >> 1;
			if (bitNum == l1) add = "§c";
			sb.append(add + s);
			if (bitNum == l2) add = "§f";
		}
		sender.sendMessage(new LiteralText("§8L" + append + ":" + sb));
	}

	private static BlockPos[] getArrayFromJK(int j, int k, int bits, BlockPos pos) {
		BlockPos basePos = new BlockPos(pos.getX() >>> 4 << 4, pos.getY() >>> 4 << 4, pos.getZ() >>> 4 << 4);
		ArrayList<BlockPos> list = new ArrayList<>();
		for(int index = 0; index < 4096; index++){
			int i = index * bits;
			int jj = i / 64;
			int kk = ((index + 1) * bits - 1) / 64;
			if (jj == j || kk == k || jj == k || kk == j) {
				list.add(getBlockIndex(index, basePos));
			}
		}
		return list.toArray(new BlockPos[0]);
	}

	private static int getIndex(BlockPos pos) {
		int x = pos.getX() & 15;
		int y = pos.getY() & 15;
		int z = pos.getZ() & 15;

		return y << 8 | z << 4 | x;
	}

	private static BlockPos getBlockIndex(int index, BlockPos pos) {
		int x = (pos.getX() & ~0xF) | (index & 0xF);
		int y = (pos.getY() & ~0xF) | ((index >>> 8) & 0xF);
		int z = (pos.getZ() & ~0xF) | ((index >>> 4) & 0xF);

		return new BlockPos(x, y, z);
	}

	private void getSize(CommandSource sender, class_2743 bsc) {
		Palette ibsp = ((class_2743Accessor) bsc).getPalette();
		if (ibsp instanceof LinearPalette) {
			sender.sendMessage(new LiteralText("Palette size: " + ((LinearPaletteAccessor) ((LinearPalette) ibsp)).getBitsPerBlock()));
		} else if (ibsp instanceof HashMapPalette) {
			sender.sendMessage(new LiteralText("Palette size: " + ((HashMapPaletteAccessor) ((HashMapPalette) ibsp)).getBitsPerBlock()));
		} else if (ibsp instanceof RegistryPalette) {
			sender.sendMessage(new LiteralText("Palette size MAX aka 4096"));
		}
	}

	public List<String> method_10738(MinecraftServer server, CommandSource sender, String[] args, @Nullable BlockPos targetPos) {
		if (args.length == 1) {
			return method_2894(args, "bits", "size", "posInfo", "fill");
		} else if (args.length >= 2 && args.length <= 4) {
			return method_10707(args, 1, targetPos);
		} else if (args.length == 5 && (args[0].equals("posInfo") || args[0].equals("fill"))) {
			return method_2894(args, "full", "normal");
		} else if (args.length == 6 && args[0].equals("fill")) {
			return method_2894(args, "4", "5", "13");
		} else if (args.length == 6 && args[0].equals("posInfo")) {
			return method_10708(args, Block.REGISTRY.getKeySet());
		} else {
			return Collections.emptyList();
		}
	}
}