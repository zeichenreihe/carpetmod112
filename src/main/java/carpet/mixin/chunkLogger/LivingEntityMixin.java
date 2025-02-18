package carpet.mixin.chunkLogger;

import carpet.carpetclient.CarpetClientChunkLogger;
import net.minecraft.entity.Entity;
import net.minecraft.entity.living.LivingEntity;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin extends Entity {
    public LivingEntityMixin(World worldIn) {
        super(worldIn);
    }

    @Inject(
            method = "moveEntityWithVelocity",
            at = @At("HEAD")
    )
    private void onTravelStart(float strafe, float vertical, float forward, CallbackInfo ci) {
        CarpetClientChunkLogger.setReason(() -> "Entity walking around: " + getName());
    }

    @Inject(
            method = "moveEntityWithVelocity",
            at = @At("RETURN")
    )
    private void onTravelEnd(float strafe, float vertical, float forward, CallbackInfo ci) {
        CarpetClientChunkLogger.resetReason();
    }
}
