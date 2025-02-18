package carpet.mixin.unloadedEntityFix;

import carpet.CarpetSettings;

import net.minecraft.block.entity.MovingBlockEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.MoverType;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(MovingBlockEntity.class)
public class PistonBlockEntityMixin {
    @Redirect(
            method = {
                    "moveEntities",
                    "moveEntity"
            },
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/entity/Entity;move(Lnet/minecraft/entity/MoverType;DDD)V"
            )
    )
    private void moveAndUpdate(Entity entity, MoverType type, double x, double y, double z) {
        entity.move(type, x, y, z);
        if (CarpetSettings.unloadedEntityFix) {
            // Add entity to the correct chunk after moving
            entity.world.tickEntity(entity, false);
        }
    }
}
