package carpet.mixin.accessors;

import net.minecraft.entity.ItemEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(ItemEntity.class)
public interface ItemEntityAccessor {
    @Accessor int getAge();
    @Accessor void setAge(int age);
    @Accessor("pickupCooldown") int getPickupDelay();
    @Accessor("pickupCooldown") void setPickupDelay(int pickupDelay);
}
