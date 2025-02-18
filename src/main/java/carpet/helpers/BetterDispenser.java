package carpet.helpers;

import java.util.List;

import carpet.CarpetSettings;

import net.minecraft.Bootstrap;
import net.minecraft.block.*;
import net.minecraft.block.dispenser.DispenseItemBehavior;
import net.minecraft.block.state.BlockState;
import net.minecraft.block.material.Material;
import net.minecraft.block.entity.DispenserBlockEntity;
import net.minecraft.entity.living.LivingEntity;
import net.minecraft.entity.PrimedTntEntity;
import net.minecraft.entity.living.player.PlayerEntity;
import net.minecraft.entity.vehicle.MinecartEntity;
import net.minecraft.entity.vehicle.RideableMinecartEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.potion.PotionUtils;
import net.minecraft.potion.Potions;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.world.IBlockSource;
import net.minecraft.world.World;

/*
 * Dispenser addons to improve dispensers dispense more stuff.
 * add this to end of dispenser section of bootstrap and fix the TNT part by removing it.
 * 
 *      // Carpet Dispenser addons XCOM-CARPET
        BetterDispenser.dispenserAddons();
 */

public class BetterDispenser {

	public static void dispenserAddons() {
		// Block rotation stuffs CARPET-XCOM
		DispenserBlock.BEHAVIORS.put(Item.byBlock(Blocks.CACTUS), new Bootstrap.OptionalDispenseBehavior() {
			private final DispenseItemBehavior dispenseBehavior = new DispenseItemBehavior();

			private ItemStack dispenseSilently(IBlockSource source, ItemStack stack) {
				if (!CarpetSettings.rotatorBlock) {
					return this.dispenseBehavior.dispense(source, stack);
				}
				Direction sourceFace = source.getBlockState().get(DispenserBlock.FACING);
				World world = source.getWorld();
				BlockPos blockpos = source.getPos().offset(sourceFace);
				BlockState iblockstate = world.getBlockState(blockpos);
				Block block = iblockstate.getBlock();

				// Block rotation for blocks that can be placed in all 6 rotations.
				if (block instanceof FacingBlock || block instanceof DispenserBlock) {
					Direction face = (Direction) iblockstate.get(FacingBlock.FACING);
					face = rotateAround(face, sourceFace.getAxis());
					if (sourceFace.getId() % 2 == 0) { // Rotate twice more to make blocks always rotate clockwise relative to the dispenser
						// when index is equal to zero. when index is equal to zero the dispenser is in the opposite direction.
						face = rotateAround(face, sourceFace.getAxis());
						face = rotateAround(face, sourceFace.getAxis());
					}
					world.setBlockState(blockpos, iblockstate.set(FacingBlock.FACING, face), 3);

					// Block rotation for blocks that can be placed in only 4 horizontal rotations.
				} else if (block instanceof HorizontalFacingBlock) {
					Direction face = (Direction) iblockstate.get(HorizontalFacingBlock.FACING);
					face = rotateAround(face, sourceFace.getAxis());
					if (sourceFace.getId() % 2 == 0) { // same as above.
						face = rotateAround(face, sourceFace.getAxis());
						face = rotateAround(face, sourceFace.getAxis());
					}
					if (sourceFace.getId() <= 1) { // Make sure to suppress rotation when index is lower then 2 as that will result in a faulty rotation for
						// blocks that only can be placed horizontaly.
						world.setBlockState(blockpos, iblockstate.set(HorizontalFacingBlock.FACING, face), 3);
					}
				}
				// Send block update to the block that just have been rotated.
				world.neighborChanged(blockpos, block, source.getPos());

				return stack;
			}
		});

		// Block fill bottle of water. XCOM-CARPET
		DispenserBlock.BEHAVIORS.put(Items.GLASS_BOTTLE, new DispenseItemBehavior() {
			private final DispenseItemBehavior dispenseBehavior = new DispenseItemBehavior();

			public ItemStack dispenseSilently(IBlockSource source, ItemStack stack) {
				if (!CarpetSettings.dispenserWaterBottle) {
					return this.dispenseBehavior.dispense(source, stack);
				}

				World world = source.getWorld();
				BlockPos blockpos = source.getPos().offset(source.getBlockState().get(DispenserBlock.FACING));
				BlockState iblockstate = world.getBlockState(blockpos);
				Block block = iblockstate.getBlock();
				Material material = iblockstate.getMaterial();
				ItemStack itemstack;

				if (Material.WATER.equals(material) && block instanceof LiquidBlock && iblockstate.get(LiquidBlock.LEVEL) == 0) {
					itemstack = PotionUtils.setPotion(new ItemStack(Items.POTION), Potions.WATER);
				} else {
					itemstack = new ItemStack(Items.GLASS_BOTTLE);
				}

				stack.decrease(1);

				if (stack.isEmpty()) {
					return itemstack;
				} else {
					if (((DispenserBlockEntity) source.getBlockEntity()).insertStack(itemstack) < 0) {
						this.dispenseBehavior.dispense(source, itemstack);
					}

					return stack;
				}
			}
		});

		// Chest/hopper/tnt/furnnace Minecart thingy XCOM-CARPET
		DispenserBlock.BEHAVIORS.put(Item.byBlock(Blocks.CHEST), new BehaviorDispenseMinecart(MinecartEntity.Type.CHEST));
		DispenserBlock.BEHAVIORS.put(Item.byBlock(Blocks.HOPPER), new BehaviorDispenseMinecart(MinecartEntity.Type.HOPPER));
		DispenserBlock.BEHAVIORS.put(Item.byBlock(Blocks.FURNACE), new BehaviorDispenseMinecart(MinecartEntity.Type.FURNACE));
		DispenserBlock.BEHAVIORS.put(Item.byBlock(Blocks.TNT), new BehaviorDispenseMinecart(MinecartEntity.Type.TNT));
		/*
		 * for tnt use this in the already existing tnt code if the removal isnt used.
		 *      Bootstrap.BehaviorDispenseMinecart tntDispense = new Bootstrap.BehaviorDispenseMinecart(EntityMinecart.Type.TNT);
		 *      return tntDispense.dispense(source, stack);
		 */

	}

	public static Direction rotateAround(Direction facing, Direction.Axis axis) {
		switch (axis) {
			case X:
				return facing != Direction.WEST && facing != Direction.EAST ? rotateX(facing) : facing;
			case Y:
				return facing != Direction.UP && facing != Direction.DOWN ? facing.clockwiseY() : facing;
			case Z:
				return facing != Direction.NORTH && facing != Direction.SOUTH ? rotateZ(facing) : facing;
			default:
				throw new IllegalStateException("Unable to get CW facing for axis " + axis);
		}
	}

	public static Direction rotateX(Direction facing) {
		switch (facing) {
			case NORTH:
				return Direction.DOWN;
			case SOUTH:
				return Direction.UP;
			case UP:
				return Direction.NORTH;
			case DOWN:
				return Direction.SOUTH;
			default:
				throw new IllegalStateException("Unable to get X-rotated facing of " + facing);
		}
	}

	public static Direction rotateZ(Direction facing) {
		switch (facing) {
			case EAST:
				return Direction.DOWN;
			case WEST:
				return Direction.UP;
			case UP:
				return Direction.EAST;
			case DOWN:
				return Direction.WEST;
			default:
				throw new IllegalStateException("Unable to get Z-rotated facing of " + facing);
		}
	}

	public static class BehaviorDispenseMinecart extends DispenseItemBehavior {
		private final DispenseItemBehavior dispenseBehavior = new DispenseItemBehavior();
		private final MinecartEntity.Type minecartType;

		public BehaviorDispenseMinecart(MinecartEntity.Type type) {
			this.minecartType = type;
		}

		public ItemStack dispenseSilently(IBlockSource source, ItemStack stack) {
			if (!CarpetSettings.dispenserMinecartFiller) {
				return defaultBehavior(source, stack);
			}

			BlockPos blockpos = source.getPos().offset(source.getBlockState().get(DispenserBlock.FACING));
			List<MinecartEntity> list = source.getWorld().getEntities(MinecartEntity.class, new Box(blockpos));

			if (list.isEmpty()) {
				return defaultBehavior(source, stack);
			} else {
				MinecartEntity minecart = list.get(0);
				minecart.remove();
				MinecartEntity entityminecart = MinecartEntity.create(minecart.world, minecart.x, minecart.y, minecart.z, this.minecartType);
				entityminecart.velocityX = minecart.velocityX;
				entityminecart.velocityY = minecart.velocityY;
				entityminecart.velocityZ = minecart.velocityZ;
				entityminecart.pitch = minecart.pitch;
				entityminecart.yaw = minecart.yaw;

				minecart.world.addEntity(entityminecart);
				stack.decrease(1);
				return stack;
			}
		}

		private ItemStack defaultBehavior(IBlockSource source, ItemStack stack) {
			if (this.minecartType == MinecartEntity.Type.TNT) {
				World world = source.getWorld();
				BlockPos blockpos = source.getPos().offset(source.getBlockState().get(DispenserBlock.FACING));
				PrimedTntEntity entitytntprimed = new PrimedTntEntity(world,
						(double) blockpos.getX() + 0.5D,
						(double) blockpos.getY(),
						(double) blockpos.getZ() + 0.5D,
						(LivingEntity) null
				);
				world.addEntity(entitytntprimed);
				world.playSound((PlayerEntity) null,
						entitytntprimed.x,
						entitytntprimed.y,
						entitytntprimed.z,
						SoundEvents.ENTITY_TNT_PRIMED,
						SoundCategory.BLOCKS,
						1.0F,
						1.0F
				);
				stack.decrease(1);
				return stack;
			} else {
				return this.dispenseBehavior.dispense(source, stack);
			}
		}

		protected void playSound(IBlockSource source) {
			source.getWorld().doEvent(1000, source.getPos(), 0);
		}
	}
}
