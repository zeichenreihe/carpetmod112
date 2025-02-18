package carpet.mixin.duplicationFixRidingEntitys;

import carpet.CarpetSettings;
import carpet.patches.FakeServerPlayerEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.living.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;

@Mixin(Entity.class)
public class EntityMixin {
    @Shadow @Final private List<Entity> passengers;

    @Inject(
            method = "writeNbt",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/nbt/NbtCompound;putString(Ljava/lang/String;Ljava/lang/String;)V"
            ),
            cancellable = true
    )
    private void duplicationFixRidingEntitys(NbtCompound compound, CallbackInfoReturnable<Boolean> cir) {
        // Fix for fixing duplication caused by riding entitys into unloaded chunks CARPET-XCOM
        if(CarpetSettings.duplicationFixRidingEntitys && hasPlayerPassenger()) {
            cir.setReturnValue(false);
        }
    }

    // Method for fixing duplication caused by riding entitys into unloaded chunks CARPET-XCOM
    private boolean hasPlayerPassenger() {
        for (Entity passenger : passengers) {
            if (passenger instanceof PlayerEntity && !(passenger instanceof FakeServerPlayerEntity)) {
                return true;
            }
            if (((EntityMixin) (Object) passenger).hasPlayerPassenger()) {
                return true;
            }
        }
        return false;
    }
}
