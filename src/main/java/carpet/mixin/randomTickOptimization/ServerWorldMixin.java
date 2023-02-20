package carpet.mixin.randomTickOptimization;

import carpet.helpers.RandomTickOptimization;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.material.Material;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.profiler.Profiler;
import net.minecraft.world.World;
import net.minecraft.world.WorldSaveHandler;
import net.minecraft.world.dimension.Dimension;
import net.minecraft.world.level.LevelProperties;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerWorld.class)
public abstract class ServerWorldMixin extends World {
    protected ServerWorldMixin(WorldSaveHandler levelProperties, LevelProperties levelProperties2, Dimension dimension, Profiler profiler, boolean isClient) {
        super(levelProperties, levelProperties2, dimension, profiler, isClient);
    }

    // Prevent execution of the original return
    @Redirect(
            method = "createAndScheduleBlockTick(Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/block/Block;II)V",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/block/Block;doImmediateUpdates()Z"
            )
    )
    private boolean requiresUpdatesWorldGenFix(Block block) {
        return !RandomTickOptimization.needsWorldGenFix && block.doImmediateUpdates();
    }

    @Inject(
            method = "createAndScheduleBlockTick(Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/block/Block;II)V",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/block/Block;doImmediateUpdates()Z"
            ),
            cancellable = true
    )
    private void randomTickWorldGenFix(BlockPos pos, Block blockIn, int delay, int priority, CallbackInfo ci) {
        if (!RandomTickOptimization.needsWorldGenFix) return;
        if (this.isRegionLoaded(pos.add(-8, -8, -8), pos.add(8, 8, 8))) {
            BlockState state = this.getBlockState(pos);
            if (state.getMaterial() != Material.AIR && state.getBlock() == blockIn) {
                state.getBlock().onScheduledTick(this, pos, state, this.random);
            }
            ci.cancel(); // move return into the if for `needsWorldGenFix`
        }
    }
}
