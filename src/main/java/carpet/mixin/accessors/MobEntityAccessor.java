package carpet.mixin.accessors;

import net.minecraft.entity.ai.goal.GoalSelector;
import net.minecraft.entity.mob.MobEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(MobEntity.class)
public interface MobEntityAccessor {
    // TODO: unsure, could also be attackGoals
    @Accessor("goals") GoalSelector getGoalSelector();
    @Invoker boolean invokeCanImmediatelyDespawn();
}
