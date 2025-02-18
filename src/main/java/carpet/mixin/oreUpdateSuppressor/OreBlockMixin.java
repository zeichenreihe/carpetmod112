package carpet.mixin.oreUpdateSuppressor;

import carpet.CarpetSettings;
import net.minecraft.block.Block;
import net.minecraft.block.state.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.material.Material;
import net.minecraft.block.material.MaterialColor;
import net.minecraft.block.OreBlock;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(OreBlock.class)
public class OreBlockMixin extends Block {
    public OreBlockMixin(Material blockMaterialIn, MaterialColor blockMapColorIn) {
        super(blockMaterialIn, blockMapColorIn);
    }

    @Override
    public void neighborChanged(BlockState state, World worldIn, BlockPos pos, Block blockIn, BlockPos fromPos) {
        if (CarpetSettings.oreUpdateSuppressor && this == Blocks.EMERALD_ORE){
            throw new StackOverflowError("Carpet-triggered update suppression");
        }
        super.neighborChanged(state, worldIn, pos, blockIn, fromPos);
    }
}
