package carpet.mixin.betterStatistics;

import carpet.helpers.StatHelper;
import net.minecraft.item.FoodItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.stat.Stat;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(FoodItem.class)
public class FoodItemMixin {
    @Redirect(
            method = "method_3367",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/stat/Stats;used(Lnet/minecraft/item/Item;)Lnet/minecraft/stat/Stat;"
            )
    )
    private Stat addUseMeta(Item item, ItemStack stack) {
        return StatHelper.getObjectUseStats(item, stack.getMeta());
    }
}
