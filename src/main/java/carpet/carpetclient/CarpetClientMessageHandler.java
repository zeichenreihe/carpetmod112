package carpet.carpetclient;

import carpet.CarpetSettings;
import carpet.helpers.CustomCrafting;
import carpet.helpers.TickSpeed;
import carpetmod.Build;
import com.google.gson.JsonObject;
import io.netty.buffer.Unpooled;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtList;
import net.minecraft.server.entity.living.player.ServerPlayerEntity;
import net.minecraft.network.PacketByteBuf;
import org.apache.commons.lang3.tuple.Pair;

public class CarpetClientMessageHandler {
    // Main packet data names
    public static final int GUI_ALL_DATA = 0;
    public static final int RULE_REQUEST = 1;
    public static final int VILLAGE_MARKERS = 2;
    public static final int BOUNDINGBOX_MARKERS = 3;
    public static final int TICKRATE_CHANGES = 4;
    public static final int CHUNK_LOGGER = 5;
    public static final int PISTON_UPDATES = 6;
    public static final int RANDOMTICK_DISPLAY = 7;
    public static final int CUSTOM_RECIPES = 8;

    private static final int NET_VERSION = 1;

    public static void handler(ServerPlayerEntity sender, PacketByteBuf data) {
        int type = data.readInt();

        if (GUI_ALL_DATA == type) {
            sendAllGUIOptions(sender);
        } else if (RULE_REQUEST == type) {
            ruleRequest(sender, data);
        } else if (VILLAGE_MARKERS == type) {
            registerVillagerMarkers(sender, data);
        } else if (BOUNDINGBOX_MARKERS == type) {
            boundingboxRequest(sender, data);
        } else if (CHUNK_LOGGER == type) {
            CarpetClientChunkLogger.logger.registerPlayer(sender, data);
        } else if (RANDOMTICK_DISPLAY == type) {
            CarpetClientRandomtickingIndexing.register(sender, data);
        } else if (CUSTOM_RECIPES == type) {
            confirmationReceivedCustomRecipesSendUpdate(sender);
        }
    }

    private static void registerVillagerMarkers(ServerPlayerEntity sender, PacketByteBuf data) {
        CarpetClientMarkers.registerVillagerMarkers(sender, data);
    }

    private static void boundingboxRequest(ServerPlayerEntity sender, PacketByteBuf data) {
        CarpetClientMarkers.updateClientBoundingBoxMarkers(sender, data);
    }

    private static void ruleRequest(ServerPlayerEntity sender, PacketByteBuf data) {
        CarpetClientRuleChanger.ruleChanger(sender, data);
    }

    public static void sendAllGUIOptions(ServerPlayerEntity sender) {
        PacketByteBuf data = new PacketByteBuf(Unpooled.buffer());
        data.writeInt(GUI_ALL_DATA);

        String[] list = CarpetSettings.findAll(null);

        NbtCompound chunkData = new NbtCompound();

        chunkData.putString("carpetVersion", Build.VERSION);
        chunkData.putFloat("tickrate", TickSpeed.tickRate);
        chunkData.putInt("netVersion", NET_VERSION);
        NbtList listNBT = new NbtList();
        for (String rule : list) {
            String current = CarpetSettings.get(rule);
            String[] options = CarpetSettings.getOptions(rule);
            String def = CarpetSettings.getDefault(rule);
            boolean isfloat = CarpetSettings.isDouble(rule);

            NbtCompound ruleNBT = new NbtCompound();
            ruleNBT.putString("rule", rule);
            ruleNBT.putString("current", current);
            ruleNBT.putString("default", def);
            ruleNBT.putBoolean("isfloat", isfloat);
            listNBT.add(ruleNBT);
        }
        chunkData.put("ruleList", listNBT);

        try {
            data.writeNbtCompound(chunkData);
        } catch (Exception ignored) {
        }

        CarpetClientServer.sender(data, sender);
    }

    public static void sendNBTVillageData(ServerPlayerEntity sender, NbtCompound compound) {
        PacketByteBuf data = new PacketByteBuf(Unpooled.buffer());
        data.writeInt(CarpetClientMessageHandler.VILLAGE_MARKERS);

        data.writeNbtCompound(compound);

        CarpetClientServer.sender(data, sender);
    }

    public static void sendNBTBoundingboxData(ServerPlayerEntity sender, NbtCompound compound) {
        PacketByteBuf data = new PacketByteBuf(Unpooled.buffer());
        data.writeInt(CarpetClientMessageHandler.BOUNDINGBOX_MARKERS);

        data.writeNbtCompound(compound);

        CarpetClientServer.sender(data, sender);
    }

    public static void sendTickRateChanges() {
        PacketByteBuf data = new PacketByteBuf(Unpooled.buffer());
        data.writeInt(CarpetClientMessageHandler.TICKRATE_CHANGES);
        data.writeFloat(TickSpeed.tickRate);

        CarpetClientServer.sender(data);
    }

    public static void sendNBTChunkData(ServerPlayerEntity sender, int dataType, NbtCompound compound) {
        PacketByteBuf data = new PacketByteBuf(Unpooled.buffer());
        data.writeInt(CarpetClientMessageHandler.CHUNK_LOGGER);
        data.writeInt(dataType);
        try {
            data.writeNbtCompound(compound);
        } catch (Exception ignored) {
        }
        CarpetClientServer.sender(data, sender);
    }

    public static void sendPistonUpdate() {
        PacketByteBuf data = new PacketByteBuf(Unpooled.buffer());
        data.writeInt(CarpetClientMessageHandler.PISTON_UPDATES);

        CarpetClientServer.sender(data);
    }

    public static void sendNBTRandomTickData(ServerPlayerEntity sender, NbtCompound compound) {
        PacketByteBuf data = new PacketByteBuf(Unpooled.buffer());
        data.writeInt(CarpetClientMessageHandler.RANDOMTICK_DISPLAY);
        try {
            data.writeNbtCompound(compound);
        } catch (Exception ignored) {
        }
        CarpetClientServer.sender(data, sender);
    }

    public static void sendCustomRecipes(ServerPlayerEntity sender) {
        if (CustomCrafting.getRecipeList().isEmpty()) return;
        PacketByteBuf data = new PacketByteBuf(Unpooled.buffer());
        data.writeInt(CUSTOM_RECIPES);

        NbtCompound chunkData = new NbtCompound();

        NbtList listNBT = new NbtList();
        for (Pair<String, JsonObject> pair : CustomCrafting.getRecipeList()) {
            NbtCompound recipe = new NbtCompound();
            recipe.putString("name", pair.getKey());
            recipe.putString("recipe", pair.getValue().toString());
            listNBT.add(recipe);
        }
        chunkData.put("recipeList", listNBT);

        try {
            data.writeNbtCompound(chunkData);
        } catch (Exception ignored) {
        }

        CarpetClientServer.sender(data, sender);
    }

    public static void confirmationReceivedCustomRecipesSendUpdate(ServerPlayerEntity sender) {
        sender.getRecipeBook().sendInitRecipes(sender);
    }
}
