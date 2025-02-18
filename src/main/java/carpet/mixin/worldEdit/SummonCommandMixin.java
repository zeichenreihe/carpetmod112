package carpet.mixin.worldEdit;

import carpet.worldedit.WorldEditBridge;
import net.minecraft.server.command.source.CommandSource;
import net.minecraft.entity.Entity;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.command.SummonCommand;
import net.minecraft.server.entity.living.player.ServerPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(SummonCommand.class)
public class SummonCommandMixin {
    @Redirect(
            method = "run",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/entity/Entity;refreshPositionAndAngles(DDDFF)V"
            )
    )
    private void recordEntityCreation(Entity entity, double x, double y, double z, float yaw, float pitch, MinecraftServer server, CommandSource sender) {
        entity.refreshPositionAndAngles(x, y, z, yaw, pitch);
        ServerPlayerEntity worldEditPlayer = sender instanceof ServerPlayerEntity ? (ServerPlayerEntity) sender : null;
        WorldEditBridge.recordEntityCreation(worldEditPlayer, entity.world, entity);
    }
}
