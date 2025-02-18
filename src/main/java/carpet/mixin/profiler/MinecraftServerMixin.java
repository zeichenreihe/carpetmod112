package carpet.mixin.profiler;

import carpet.helpers.LagSpikeHelper;
import carpet.utils.CarpetProfiler;
import net.minecraft.server.MinecraftServer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import static carpet.helpers.LagSpikeHelper.PrePostSubPhase.POST;
import static carpet.helpers.LagSpikeHelper.PrePostSubPhase.PRE;
import static carpet.helpers.LagSpikeHelper.TickPhase.*;

@Mixin(MinecraftServer.class)
public class MinecraftServerMixin {
    @Inject(
            method = "tick()V",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/server/PlayerManager;saveAll()V"
            )
    )
    private void onAutosaveStart(CallbackInfo ci) {
        LagSpikeHelper.processLagSpikes(null, AUTOSAVE, PRE);
        CarpetProfiler.start_section(null, "Autosave");
    }

    @Inject(
            method = "tick()V",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/server/MinecraftServer;saveWorlds(Z)V",
                    shift = At.Shift.AFTER
            )
    )
    private void onAutosaveEnd(CallbackInfo ci) {
        CarpetProfiler.end_current_section();
        LagSpikeHelper.processLagSpikes(null, AUTOSAVE, POST);
    }

    @Inject(
            method = "tickWorlds",
            at = @At(
                    value = "CONSTANT",
                    args = "stringValue=connection"
            )
    )
    private void onNetworkStart(CallbackInfo ci) {
        CarpetProfiler.start_section(null, "Network");
    }

    @Inject(
            method = "tickWorlds",
            at = @At(
                    value = "CONSTANT",
                    args = "stringValue=commandFunctions"
            )
    )
    private void onNetworkEnd(CallbackInfo ci) {
        CarpetProfiler.end_current_section();
    }

    @Inject(
            method = "tickWorlds",
            at = @At("HEAD")
    )
    private void preTick(CallbackInfo ci) {
        LagSpikeHelper.processLagSpikes(null, TICK, PRE);
    }

    @Inject(
            method = "tickWorlds",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/util/profiler/Profiler;push(Ljava/lang/String;)V",
                    ordinal = 0,
                    shift = At.Shift.AFTER
            )
    )
    private void prePlayer(CallbackInfo ci) {
        LagSpikeHelper.processLagSpikes(null, PLAYER, PRE);
    }

    @Inject(
            method = "tickWorlds",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/util/profiler/Profiler;swap(Ljava/lang/String;)V",
                    ordinal = 0
            )
    )
    private void postPlayer(CallbackInfo ci) {
        LagSpikeHelper.processLagSpikes(null, PLAYER, POST);
    }

    @Inject(
            method = "tickWorlds",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/util/profiler/Profiler;push(Ljava/util/function/Supplier;)V",
                    shift = At.Shift.AFTER
            )
    )
    private void preDimension(CallbackInfo ci) {
        LagSpikeHelper.processLagSpikes(null, DIMENSION, PRE);
    }

    @Inject(
            method = "tickWorlds",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/server/entity/EntityTracker;tick()V",
                    shift = At.Shift.AFTER
            )
    )
    private void postDimension(CallbackInfo ci) {
        LagSpikeHelper.processLagSpikes(null, DIMENSION, POST);
    }
}
