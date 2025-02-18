package carpet.mixin.spongeRandom;

import carpet.CarpetSettings;
import net.minecraft.block.Block;
import net.minecraft.block.state.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.material.Material;
import net.minecraft.block.SpongeBlock;
import net.minecraft.block.state.property.BooleanProperty;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import java.util.Random;

@Mixin(SpongeBlock.class)
public class SpongeBlockMixin extends Block {
    @Shadow @Final public static BooleanProperty WET;

    protected SpongeBlockMixin(Material materialIn) {
        super(materialIn);
    }

    @Override
    public void randomTick(World world, BlockPos pos, BlockState state, Random random) {
        super.randomTick(world, pos, state, random);
        if (!CarpetSettings.spongeRandom) {
            return;
        }
        boolean touchesWater = false;
        boolean touchesWet = false;
        for (Direction direction : Direction.values()) {
            BlockPos neighbor = pos.offset(direction);//TODO: optimise
            if (world.getBlockState(neighbor).getMaterial() == Material.WATER) {
                touchesWater = true;
            }
            if (world.getBlockState(neighbor).getBlock() == Blocks.SPONGE && world.getBlockState(neighbor).get(WET)) {
                touchesWet = true;
            }
        }
        if (state.get(WET) && !touchesWater && world.hasSkyAccess(pos.up()) && world.isSunny() && !world.isRaining(pos.up())) {
            world.setBlockState(pos, state.set(WET, Boolean.FALSE), 2);
        } else if (!state.get(WET) && (touchesWet || touchesWater || world.isRaining(pos.up())) && random.nextInt(3) == 0) {
            world.setBlockState(pos, state.set(WET, Boolean.TRUE), 2);
        }
    }
}
