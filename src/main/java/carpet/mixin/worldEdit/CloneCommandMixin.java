package carpet.mixin.worldEdit;

import carpet.mixin.accessors.StaticCloneDataAccessor;
import carpet.worldedit.WorldEditBridge;
import com.google.common.base.Predicate;
import net.minecraft.block.Block;
import net.minecraft.block.state.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.server.command.source.CommandSource;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.command.CloneCommand;
import net.minecraft.server.entity.living.player.ServerPlayerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.gen.structure.StructureBox;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Coerce;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Surrogate;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

import java.util.Deque;
import java.util.Iterator;
import java.util.List;

@Mixin(CloneCommand.class)
public class CloneCommandMixin {
    @Inject(
            method = "run",
            at = @At(
                    value = "INVOKE_ASSIGN",
                    target = "Lnet/minecraft/world/World;getBlockEntity(Lnet/minecraft/util/math/BlockPos;)Lnet/minecraft/block/entity/BlockEntity;",
                    ordinal = 1
            ),
            locals = LocalCapture.CAPTURE_FAILHARD
    )
    private void recordRemove(MinecraftServer server, CommandSource sender, String[] args, CallbackInfo ci,
                              BlockPos pos1, BlockPos pos2, BlockPos pos3, StructureBox box1, StructureBox box2, boolean flag, Block block,
            Predicate<BlockState> predicate,
                              World world, boolean flag1, List<?> list, List<?> list1, List<?> list2, Deque<BlockPos> deque, BlockPos pos4, Iterator<BlockPos> it,
                              BlockPos currentPos, BlockEntity unused) {
        ServerPlayerEntity worldEditPlayer = sender instanceof ServerPlayerEntity ? (ServerPlayerEntity) sender : null;
        WorldEditBridge.recordBlockEdit(worldEditPlayer, world, currentPos, Blocks.AIR.defaultState(), null);
    }

    @Inject(
            method = "run",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/World;getBlockEntity(Lnet/minecraft/util/math/BlockPos;)Lnet/minecraft/block/entity/BlockEntity;",
                    ordinal = 3
            ),
            locals = LocalCapture.CAPTURE_FAILHARD
    )
    private void recordAdd(MinecraftServer server, CommandSource sender, String[] args, CallbackInfo ci,
                           BlockPos blockpos, BlockPos blockpos1, BlockPos blockpos2, StructureBox structureboundingbox, StructureBox structureboundingbox1, int i, boolean flag, Block block, Predicate<?> predicate,
                           World world, boolean flag1, List<?> list, List<?> list1, List<?> list2, Deque<?> deque, BlockPos blockpos3, List<?> list3, List<?> list4, Iterator<?> var22,
                           @Coerce StaticCloneDataAccessor data) {
        ServerPlayerEntity worldEditPlayer = sender instanceof ServerPlayerEntity ? (ServerPlayerEntity) sender : null;
        WorldEditBridge.recordBlockEdit(worldEditPlayer, world, data.getPos(), data.getBlockState(), data.getNbt());
    }
}
