package carpet.carpetclient;

import com.google.common.collect.Lists;
import io.netty.buffer.Unpooled;
import carpet.CarpetSettings;
import carpet.utils.Messenger;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import net.minecraft.server.entity.living.player.ServerPlayerEntity;
import net.minecraft.network.PacketByteBuf;

public class CarpetClientRuleChanger {
    // Minor rule changing packet names
    private static final int CHANGE_RULE = 0;
    private static final int CHANGE_TEXT_RULE = 1;
    private static final int RESET_RULE = 2;
    private static final int REQUEST_RULE_TIP = 3;

    private static final Map<String, Integer> valueIndex = new HashMap<>();

    static void ruleChanger(ServerPlayerEntity sender, PacketByteBuf data) {
        int type = data.readInt();
        String rule = data.readString(100);

        if (CHANGE_RULE == type) {
            if (sender.canUseCommand(2, "carpet")) {
                String[] options = CarpetSettings.getOptions(rule);
                int index = valueIndex.getOrDefault(rule.toLowerCase(Locale.ENGLISH), -1);
                if (index == -1) {
                    String value = CarpetSettings.get(rule);
                    index = Lists.newArrayList(options).indexOf(value);
                }
                index = (index + 1) % options.length;
                valueIndex.put(rule.toLowerCase(Locale.ENGLISH), index);
                String value = options[index];
                ruleChangeLogic(sender, rule, value);
            } else {
                Messenger.m(sender, "r You do not have permissions to change the rules.");
            }
        } else if (CHANGE_TEXT_RULE == type) {
            if (sender.canUseCommand(2, "carpet")) {
                String value = data.readString(100);
                ruleChangeLogic(sender, rule, value);
            } else {
                Messenger.m(sender, "r You do not have permissions to change the rules.");
            }
        } else if (RESET_RULE == type) {
            if (sender.canUseCommand(2, "carpet")) {
                String value = CarpetSettings.getDefault(rule);
                ruleChangeLogic(sender, rule, value);
                valueIndex.put(rule.toLowerCase(Locale.ENGLISH), 0);
            } else {
                Messenger.m(sender, "r You do not have permissions to change the rules.");
            }
        } else if (REQUEST_RULE_TIP == type) {
            CarpetClientRuleTips.getInfoRuleTip(sender, rule);
        }
    }

    private static void ruleChangeLogic(ServerPlayerEntity sender, String rule, String value) {
        CarpetSettings.set(rule, value);
        String s = CarpetSettings.getDescription(rule) + " is set to: " + CarpetSettings.get(rule);
        Messenger.print_server_message(sender.getSourceWorld().getServer(), s);
    }

    public static void updateCarpetClientsRule(String rule, String value) {
        PacketByteBuf data = new PacketByteBuf(Unpooled.buffer());

        data.writeInt(CarpetClientMessageHandler.RULE_REQUEST);
        data.writeString(rule);
        data.writeInt(CHANGE_RULE);
        data.writeString(value);

        CarpetClientServer.sender(data);
    }

    static void updatePlayerRuleInfo(ServerPlayerEntity sender, String rule, String value) {
        PacketByteBuf data = new PacketByteBuf(Unpooled.buffer());

        data.writeInt(CarpetClientMessageHandler.RULE_REQUEST);
        data.writeString(rule);
        data.writeInt(REQUEST_RULE_TIP);
        data.writeString(value);

        CarpetClientServer.sender(data, sender);
    }
}
