package carpet.mixin.worldEdit;

import carpet.helpers.CapturedDrops;
import net.minecraft.block.Block;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(Block.class)
public class BlockMixin {
    @Redirect(
            method = "dropItems(Lnet/minecraft/world/World;Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/item/ItemStack;)V",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/World;addEntity(Lnet/minecraft/entity/Entity;)Z"
            )
    )
    private static boolean captureDrops(World world, Entity entity) {
        if (world.addEntity(entity)) {
            if (CapturedDrops.isCapturingDrops()) CapturedDrops.captureDrop((ItemEntity) entity);
            return true;
        }
        return false;
    }
}
