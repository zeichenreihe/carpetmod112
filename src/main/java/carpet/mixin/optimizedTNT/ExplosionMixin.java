package carpet.mixin.optimizedTNT;

import carpet.CarpetSettings;
import carpet.helpers.OptimizedTNT;
import carpet.mixin.accessors.ExplosionAccessor;
import net.minecraft.world.explosion.Explosion;

import carpet.mixin_accessors.loggers.LogableExplosion;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Explosion.class)
public class ExplosionMixin {
    @Inject(
            method = "damageEntities",
            at = @At("HEAD"),
            cancellable = true
    )
    private void onExplosionA(CallbackInfo ci) {
        if (CarpetSettings.optimizedTNT) {
            OptimizedTNT.doExplosionA((ExplosionAccessor) this);
            ci.cancel();
        }
    }

    @Inject(
            method = "damageBlocks",
            at = @At("HEAD"),
            cancellable = true
    )
    private void onExplosionB(boolean spawnParticles, CallbackInfo ci) {
        if (CarpetSettings.optimizedTNT) {
            OptimizedTNT.doExplosionB((ExplosionAccessor) this, (LogableExplosion) this, spawnParticles);
            ci.cancel();
        }
    }
}
