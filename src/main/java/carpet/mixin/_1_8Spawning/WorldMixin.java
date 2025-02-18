package carpet.mixin._1_8Spawning;

import carpet.CarpetSettings;
import net.minecraft.entity.Entity;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(World.class)
public class WorldMixin {
    @Redirect(
            method = "canBuildIn(Lnet/minecraft/util/math/Box;Lnet/minecraft/entity/Entity;)Z",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/entity/Entity;hasSameVehicle(Lnet/minecraft/entity/Entity;)Z"
            )
    )
    private boolean noRidingCheck18(Entity entity, Entity other) {
        return CarpetSettings._1_8Spawning || entity.hasSameVehicle(other);
    }
}
