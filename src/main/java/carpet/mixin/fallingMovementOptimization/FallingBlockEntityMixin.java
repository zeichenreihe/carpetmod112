package carpet.mixin.fallingMovementOptimization;

import carpet.CarpetSettings;
import net.minecraft.entity.Entity;
import net.minecraft.entity.FallingBlockEntity;
import net.minecraft.entity.MoverType;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(FallingBlockEntity.class)
public abstract class FallingBlockEntityMixin extends Entity {
    // Optimization methods CARPET-XCOM
    private static double[] cache = new double[12];
    private static boolean[] cacheBool = new boolean[2];
    private static long cacheTime = 0;

    public FallingBlockEntityMixin(World worldIn) {
        super(worldIn);
    }

    @Unique private boolean cacheMatching() {
        return cache[0] == x && cache[1] == y && cache[2] == z && cache[3] == velocityX && cache[4] == velocityY && cache[5] == velocityZ && cacheTime == getServer().getTicks();
    }

    @Redirect(
            method = "tick",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/entity/FallingBlockEntity;move(Lnet/minecraft/entity/MoverType;DDD)V"
            )
    )
    private void movementOptimization(FallingBlockEntity fallingBlock, MoverType type, double x, double y, double z) {
        if (!CarpetSettings.fallingMovementOptimization) {
            fallingBlock.move(type, x, y, z);
            return;
        }
        if (cacheMatching()) {
            this.setPosition(cache[6], cache[7], cache[8]);
            velocityX = cache[9];
            velocityY = cache[10];
            velocityZ = cache[11];
            inCobweb = cacheBool[0];
            onGround = cacheBool[1];
        } else {
            cache[0] = x;
            cache[1] = y;
            cache[2] = z;
            cache[3] = velocityX;
            cache[4] = velocityY;
            cache[5] = velocityZ;
            cacheTime = getServer().getTicks();
            this.move(MoverType.SELF, this.velocityX, this.velocityY, this.velocityZ);
            if (removed) {
                cache[0] = Integer.MAX_VALUE;
            } else {
                cache[6] = x;
                cache[7] = y;
                cache[8] = z;
                cache[9] = velocityX;
                cache[10] = velocityY;
                cache[11] = velocityZ;
                cacheBool[0] = inCobweb;
                cacheBool[1] = onGround;
            }
        }
    }
}
