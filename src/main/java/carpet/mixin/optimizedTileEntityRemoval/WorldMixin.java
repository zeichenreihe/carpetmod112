package carpet.mixin.optimizedTileEntityRemoval;

import carpet.CarpetSettings;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Set;

@Mixin(World.class)
public class WorldMixin {
    @Shadow @Final private List<BlockEntity> removedBlockEntities;
    @Shadow @Final public List<BlockEntity> tickingBlockEntities;
    @Shadow @Final public List<BlockEntity> blockEntities;

    @Inject(
            method = "tickEntities",
            at = @At(
                    value = "FIELD",
                    target = "Lnet/minecraft/world/World;removedBlockEntities:Ljava/util/List;",
                    ordinal = 0
            )
    )
    private void optimizedTileEntityRemoval(CallbackInfo ci) {
        if (!CarpetSettings.optimizedTileEntityRemoval || this.removedBlockEntities.isEmpty()) return;

        Set<BlockEntity> remove = Collections.newSetFromMap(new IdentityHashMap<>());
        remove.addAll(this.removedBlockEntities);
        this.tickingBlockEntities.removeAll(remove);
        this.blockEntities.removeAll(remove);
        this.removedBlockEntities.clear();
    }
}
