package carpet.mixin.chunkLogger;

import carpet.carpetclient.CarpetClientChunkLogger;
import net.minecraft.entity.Entity;
import net.minecraft.entity.PortalTeleporter;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Entity.class)
public abstract class EntityMixin {
    @Shadow public abstract String getTranslationKey();

    @Inject(
            method = "updateWaterState",
            at = @At("HEAD")
    )
    private void onWaterMovementStart(CallbackInfoReturnable<Boolean> cir) {
        CarpetClientChunkLogger.setReason(() -> "Entity checking if pushed by water: " + getTranslationKey());
    }

    @Inject(
            method = "updateWaterState",
            at = @At("RETURN")
    )
    private void onWaterMovementEnd(CallbackInfoReturnable<Boolean> cir) {
        CarpetClientChunkLogger.resetReason();
    }

    @Redirect(
            method = "changeDimension",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/entity/PortalTeleporter;method_8584(Lnet/minecraft/entity/Entity;F)Z"
            )
    )
    private boolean placeInExistingPortal(PortalTeleporter teleporter, Entity entityIn, float rotationYaw) {
        try {
            CarpetClientChunkLogger.setReason(() -> "Entity going through nether portal: " + getTranslationKey());
            return teleporter.method_8584(entityIn, rotationYaw);
        } finally {
            CarpetClientChunkLogger.resetReason();
        }
    }
}
