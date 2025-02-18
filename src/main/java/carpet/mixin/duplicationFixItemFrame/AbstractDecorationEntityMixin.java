package carpet.mixin.duplicationFixItemFrame;

import net.minecraft.entity.Entity;
import net.minecraft.entity.MoverType;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.decoration.DecorationEntity;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(DecorationEntity.class)
public abstract class AbstractDecorationEntityMixin extends Entity {
    public AbstractDecorationEntityMixin(World worldIn) {
        super(worldIn);
    }

    @Redirect(
            method = {
                "tick",
                "damage",
                "move",
                "addVelocity"
            },
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/entity/decoration/DecorationEntity;remove()V"
            )
    )
    private void moveSetDead1(DecorationEntity entity) {}

    @Inject(
            method = "tick",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/entity/decoration/DecorationEntity;onAttack(Lnet/minecraft/entity/Entity;)V",
                    shift = At.Shift.AFTER
            )
    )
    private void moveSetDead2(CallbackInfo ci) {
        remove();
    }

    @Inject(
            method = "damage",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/entity/decoration/DecorationEntity;onAttack(Lnet/minecraft/entity/Entity;)V",
                    shift = At.Shift.AFTER
            )
    )
    private void moveSetDead2(DamageSource source, float amount, CallbackInfoReturnable<Boolean> cir) {
        remove();
    }

    @Inject(
            method = "move",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/entity/decoration/DecorationEntity;onAttack(Lnet/minecraft/entity/Entity;)V",
                    shift = At.Shift.AFTER
            )
    )
    private void moveSetDead2(MoverType type, double d, double e, double f, CallbackInfo ci) {
        remove();
    }

    @Inject(
            method = "addVelocity",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/entity/decoration/DecorationEntity;onAttack(Lnet/minecraft/entity/Entity;)V",
                    shift = At.Shift.AFTER
            )
    )
    private void moveSetDead2(double x, double y, double z, CallbackInfo ci) {
        remove();
    }
}
