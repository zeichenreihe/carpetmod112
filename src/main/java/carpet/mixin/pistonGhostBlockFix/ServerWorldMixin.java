package carpet.mixin.pistonGhostBlockFix;

import carpet.utils.extensions.ExtendedServerWorldPistonGhostBlockFix;
import net.minecraft.server.world.ServerWorld;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerWorld.class)
public class ServerWorldMixin implements ExtendedServerWorldPistonGhostBlockFix {
    private boolean blockActionsProcessed;

    @Inject(
            method = "tick",
            at = @At("HEAD")
    )
    private void onTickStart(CallbackInfo ci) {
        blockActionsProcessed = false;
    }

    @Inject(
            method = "doBlockEvents",
            at = @At("RETURN")
    )
    private void onBlockEventsEnd(CallbackInfo ci) {
        blockActionsProcessed = true;
    }

    @Override
    public boolean haveBlockActionsProcessed() {
        return blockActionsProcessed;
    }
}
