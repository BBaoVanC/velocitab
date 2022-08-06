import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ServerConnection;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import net.luckperms.api.LuckPerms;
import net.luckperms.api.cacheddata.CachedMetaData;
import net.luckperms.api.model.user.UserManager;

import java.util.Optional;

public class NameFormatter {
    private final MiniMessage miniMessage;
    private final UserManager userManager;

    public NameFormatter(UserManager userManager) {
        this.userManager = userManager;
        this.miniMessage = MiniMessage.miniMessage();
    }

    public Component formatUsername(Player player, String serverName) {
        CachedMetaData metaData = userManager.getUser(player.getUniqueId()).getCachedData().getMetaData();
        return miniMessage.deserialize(
                Config.template,
                TagResolver.builder()
                        .resolver(Placeholder.unparsed("player", player.getUsername()))
                        .resolver(Placeholder.unparsed("server", serverName))
                        .resolver(Placeholder.parsed("luckperms_prefix",
                                Optional.ofNullable(metaData.getPrefix()).orElse("")))
                        .resolver(Placeholder.parsed("luckperms_suffix",
                                Optional.ofNullable(metaData.getSuffix()).orElse("")))
                        .build()
        );
    }
}
