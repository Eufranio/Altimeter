package tv.voidstar.altimeter;

import com.google.common.reflect.TypeToken;
import com.google.inject.Inject;
import org.slf4j.Logger;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.config.ConfigDir;
import org.spongepowered.api.data.key.Key;
import org.spongepowered.api.data.key.KeyFactory;
import org.spongepowered.api.data.value.mutable.Value;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.event.filter.cause.Root;
import org.spongepowered.api.event.game.GameReloadEvent;
import org.spongepowered.api.event.game.state.GameInitializationEvent;
import org.spongepowered.api.event.game.state.GameLoadCompleteEvent;
import org.spongepowered.api.event.game.state.GameStartedServerEvent;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.game.state.GameStoppingServerEvent;
import org.spongepowered.api.event.network.ClientConnectionEvent;
import org.spongepowered.api.plugin.Plugin;
import org.spongepowered.api.plugin.PluginContainer;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.format.TextStyles;
import org.spongepowered.api.util.TypeTokens;

import java.io.File;
import java.io.IOException;
import java.time.Instant;

@Plugin(
        id = "altimeter",
        name = "Altimeter",
        description = "Limit number of accounts joining per IP address",
        authors = {
                "ZeHeisenberg"
        }
)
public class Altimeter {

    private static Altimeter plugin;

    private PluginContainer container;

    @Inject
    @ConfigDir(sharedRoot = true)
    private File defaultConfigDir;

    @Inject
    private Logger logger;

    @Listener
    public void onInit(GameInitializationEvent event) throws IOException {
        plugin = this;

        File rootDir = new File(defaultConfigDir, "altimeter");
        if (!rootDir.exists()) {
            if (!rootDir.mkdirs()) {
                Altimeter.getLogger().error("Unable to create root config dir");
            }
        }

        AltimeterConfig.init(rootDir);
        AltimeterData.init(rootDir);
    }

    @Listener
    public void onGameLoaded(GameLoadCompleteEvent event) {
        AltimeterConfig.load();
        AltimeterData.load();
    }

    @Listener
    public void onReload(GameReloadEvent event) {
        AltimeterConfig.load();
        AltimeterData.reload();
        getLogger().info("Altimeter reloaded");
    }

    @Listener
    public void onClientConnectionEvent(ClientConnectionEvent.Login event, @Root Player player) {
        if(!AltimeterData.canLogIn(player)) {
            event.setMessage(
                    Text.of("Too many accounts have logged in from this address."),
                    Text.of("Contact a server admin.")
            );
            event.setCancelled(true);
        }
    }

    @Listener
    public void onServerStart(GameStartedServerEvent event) {
        Sponge.getPluginManager().fromInstance(Altimeter.getInstance())
                .ifPresent(pluginContainer -> container = pluginContainer);
        registerCommands();

        Sponge.getScheduler().createTaskBuilder()
                .execute(() -> AltimeterData.checkAndClearAccounts(Instant.now()))
                .interval(AltimeterConfig.getCheckIntervalValue(), AltimeterConfig.getCheckIntervalUnit())
                .submit(this.container);
    }

    @Listener
    public void onStop(GameStoppingServerEvent event) {
        AltimeterData.save();
        AltimeterConfig.save();
    }

    private void registerCommands() {
        // clear queue for IP
        // set limit for specific IP?
    }

    private static Altimeter getInstance() {
        return plugin;
    }

    public static Logger getLogger() {
        return plugin.logger;
    }
}