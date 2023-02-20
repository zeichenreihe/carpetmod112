package carpet.helpers;

import java.util.List;

import carpet.CarpetSettings;
import net.minecraft.Bootstrap;
import net.minecraft.block.*;
import net.minecraft.block.material.Material;
import net.minecraft.block.dispenser.ItemDispenserBehavior;
import net.minecraft.block.entity.DispenserBlockEntity;
import net.minecraft.client.sound.SoundCategory;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.TntEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.vehicle.AbstractMinecartEntity;
import net.minecraft.entity.vehicle.MinecartEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.potion.PotionUtil;
import net.minecraft.potion.Potions;
import net.minecraft.sound.Sounds;
import net.minecraft.util.math.BlockPointer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;

/*
 * Dispenser addons to improve dispensers dispense more stuff.
 * add this to end of dispenser section of bootstrap and fix the TNT part by removing it.
 * 
 *      // Carpet Dispenser addons XCOM-CARPET
        BetterDispenser.dispenserAddons();
 */

public class BetterDispenser {
    
    public static void dispenserAddons(){
        // Block rotation stuffs CARPET-XCOM
        DispenserBlock.SPECIAL_ITEMS.put(Item.fromBlock(Blocks.CACTUS), new Bootstrap.class_3115()
        {
            private final ItemDispenserBehavior dispenseBehavior = new ItemDispenserBehavior();
            
            protected ItemStack dispenseSilently(BlockPointer source, ItemStack stack)
            {
                if(!CarpetSettings.rotatorBlock){
                    return this.dispenseBehavior.dispense(source, stack);
                }
                Direction sourceFace = (Direction)source.getBlockState().get(DispenserBlock.FACING);
                World world = source.getWorld();
                BlockPos blockpos = source.getBlockPos().offset(sourceFace);
                BlockState iblockstate = world.getBlockState(blockpos);
                Block block = iblockstate.getBlock();
                
                // Block rotation for blocks that can be placed in all 6 rotations.
                if(block instanceof FacingBlock || block instanceof DispenserBlock){ 
                    Direction face = (Direction)iblockstate.get(FacingBlock.FACING);
                    face = rotateAround(face, sourceFace.getAxis());
                    if(sourceFace.getId() % 2 == 0){ // Rotate twice more to make blocks always rotate clockwise relative to the dispenser
                                                        // when index is equal to zero. when index is equal to zero the dispenser is in the opposite direction.
                        face = rotateAround(face, sourceFace.getAxis());
                        face = rotateAround(face, sourceFace.getAxis());
                    }
                    world.setBlockState(blockpos, iblockstate.with(FacingBlock.FACING, face), 3);
                
                // Block rotation for blocks that can be placed in only 4 horizontal rotations.
                }else if(block instanceof HorizotalFacingBlock){
                    Direction face = (Direction)iblockstate.get(HorizotalFacingBlock.DIRECTION);
                    face = rotateAround(face, sourceFace.getAxis());
                    if(sourceFace.getId() % 2 == 0){ // same as above.
                        face = rotateAround(face, sourceFace.getAxis());
                        face = rotateAround(face, sourceFace.getAxis());
                    }
                    if(sourceFace.getId() <= 1){ // Make sure to suppress rotation when index is lower then 2 as that will result in a faulty rotation for 
                                                    // blocks that only can be placed horizontaly.
                        world.setBlockState(blockpos, iblockstate.with(HorizotalFacingBlock.DIRECTION, face), 3);
                    }
                }
                // Send block update to the block that just have been rotated.
                world.updateNeighbor(blockpos, block, source.getBlockPos());
                
                return stack;
            }
        });
        
        // Block fill bottle of water. XCOM-CARPET
        DispenserBlock.SPECIAL_ITEMS.put(Items.GLASS_BOTTLE, new ItemDispenserBehavior()
        {
            private final ItemDispenserBehavior dispenseBehavior = new ItemDispenserBehavior();
            public ItemStack dispenseSilently(BlockPointer source, ItemStack stack)
            {
                if(!CarpetSettings.dispenserWaterBottle){
                    return this.dispenseBehavior.dispense(source, stack);
                }
                
                World world = source.getWorld();
                BlockPos blockpos = source.getBlockPos().offset(source.getBlockState().get(DispenserBlock.FACING));
                BlockState iblockstate = world.getBlockState(blockpos);
                Block block = iblockstate.getBlock();
                Material material = iblockstate.getMaterial();
                ItemStack itemstack;

                if (Material.WATER.equals(material) && block instanceof AbstractFluidBlock && iblockstate.get(AbstractFluidBlock.LEVEL) == 0)
                {
                    itemstack = PotionUtil.setPotion(new ItemStack(Items.POTION), Potions.WATER);
                }
                else
                {
                    itemstack = new ItemStack(Items.GLASS_BOTTLE);
                }

                stack.decrement(1);

                if (stack.isEmpty())
                {
                    return itemstack;
                }
                else
                {
                    if (((DispenserBlockEntity)source.getBlockEntity()).addToFirstFreeSlot(itemstack) < 0)
                    {
                        this.dispenseBehavior.dispense(source, itemstack);
                    }

                    return stack;
                }
            }
        });
        
        // Chest/hopper/tnt/furnnace Minecart thingy XCOM-CARPET
        DispenserBlock.SPECIAL_ITEMS.put(Item.fromBlock(Blocks.CHEST), new BehaviorDispenseMinecart(AbstractMinecartEntity.Type.CHEST));
        DispenserBlock.SPECIAL_ITEMS.put(Item.fromBlock(Blocks.HOPPER), new BehaviorDispenseMinecart(AbstractMinecartEntity.Type.HOPPER));
        DispenserBlock.SPECIAL_ITEMS.put(Item.fromBlock(Blocks.FURNACE), new BehaviorDispenseMinecart(AbstractMinecartEntity.Type.FURNACE));
        DispenserBlock.SPECIAL_ITEMS.put(Item.fromBlock(Blocks.TNT), new BehaviorDispenseMinecart(AbstractMinecartEntity.Type.TNT));
        /*
         * for tnt use this in the already existing tnt code if the removal isnt used.
         *      Bootstrap.BehaviorDispenseMinecart tntDispense = new Bootstrap.BehaviorDispenseMinecart(EntityMinecart.Type.TNT);
         *      return tntDispense.dispense(source, stack);
         */

    }

    public static Direction rotateAround(Direction facing, Direction.Axis axis) {
        switch (axis) {
            case X: return facing != Direction.WEST && facing != Direction.EAST ? rotateX(facing) : facing;
            case Y: return facing != Direction.UP && facing != Direction.DOWN ? facing.rotateYClockwise() : facing;
            case Z: return facing != Direction.NORTH && facing != Direction.SOUTH ? rotateZ(facing) : facing;
            default: throw new IllegalStateException("Unable to get CW facing for axis " + axis);
        }
    }

    public static Direction rotateX(Direction facing) {
        switch (facing) {
            case NORTH: return Direction.DOWN;
            case SOUTH: return Direction.UP;
            case UP: return Direction.NORTH;
            case DOWN: return Direction.SOUTH;
            default: throw new IllegalStateException("Unable to get X-rotated facing of " + facing);
        }
    }

    public static Direction rotateZ(Direction facing) {
        switch (facing) {
            case EAST: return Direction.DOWN;
            case WEST: return Direction.UP;
            case UP: return Direction.EAST;
            case DOWN: return Direction.WEST;
            default: throw new IllegalStateException("Unable to get Z-rotated facing of " + facing);
        }
    }

    public static class BehaviorDispenseMinecart extends ItemDispenserBehavior
    {
        private final ItemDispenserBehavior dispenseBehavior = new ItemDispenserBehavior();
        private final AbstractMinecartEntity.Type minecartType;

        public BehaviorDispenseMinecart(AbstractMinecartEntity.Type type)
        {
            this.minecartType = type;
        }

        public ItemStack dispenseSilently(BlockPointer source, ItemStack stack)
        {
            if(!CarpetSettings.dispenserMinecartFiller){
                return defaultBehavior(source, stack);
            }
            
            BlockPos blockpos = source.getBlockPos().offset(source.getBlockState().get(DispenserBlock.FACING));
            List<MinecartEntity> list = source.getWorld().getEntitiesInBox(MinecartEntity.class, new Box(blockpos));
    
            if (list.isEmpty())
            {
                return defaultBehavior(source, stack);
            }
            else
            {
                MinecartEntity minecart = list.get(0);
                minecart.remove();
                AbstractMinecartEntity entityminecart = AbstractMinecartEntity.createMinecart(minecart.world, minecart.x, minecart.y, minecart.z, this.minecartType);
                entityminecart.velocityX = minecart.velocityX;
                entityminecart.velocityY = minecart.velocityY;
                entityminecart.velocityZ = minecart.velocityZ;
                entityminecart.pitch = minecart.pitch;
                entityminecart.yaw = minecart.yaw;
                
                minecart.world.spawnEntity(entityminecart);
                stack.decrement(1);
                return stack;
            }
        }
        
        private ItemStack defaultBehavior(BlockPointer source, ItemStack stack){
            if(this.minecartType == AbstractMinecartEntity.Type.TNT){
                World world = source.getWorld();
                BlockPos blockpos = source.getBlockPos().offset(source.getBlockState().get(DispenserBlock.FACING));
                TntEntity entitytntprimed = new TntEntity(world, (double)blockpos.getX() + 0.5D, (double)blockpos.getY(), (double)blockpos.getZ() + 0.5D, (LivingEntity)null);
                world.spawnEntity(entitytntprimed);
                world.playSound((PlayerEntity)null, entitytntprimed.x, entitytntprimed.y, entitytntprimed.z, Sounds.ENTITY_TNT_PRIMED, SoundCategory.BLOCKS,
                        1.0F, 1.0F);
                stack.decrement(1);
                return stack;
            }else{
                return this.dispenseBehavior.dispense(source, stack);
            }
        }

        protected void playSound(BlockPointer source)
        {
            source.getWorld().syncGlobalEvent(1000, source.getBlockPos(), 0);
        }
    }
}
