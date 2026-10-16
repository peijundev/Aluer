package com.aluer.pluginsystem;

import com.aluer.config.ServerGuardConfig;
import com.aluer.service.ServerGuardService;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;

import java.nio.file.Path;

/**
 * 插件系统 Spring 集成引导。
 *
 * <p>在引擎启动时：</p>
 * <ol>
 *   <li>构造 {@link PluginManager}（插件目录取自 {@code serverguard.plugin.directory}）。</li>
 *   <li>将插件告警管道对接至 {@link ServerGuardService#publishAlert}（统一告警管道）。</li>
 *   <li>若 {@code serverguard.plugin.enabled=true}，扫描插件目录并启用所有插件。</li>
 * </ol>
 *
 * <p>引擎关闭时统一停用所有插件，保证插件资源得到释放。</p>
 */
@Component
public class PluginSystemService {

    private static final Logger logger = LoggerFactory.getLogger(PluginSystemService.class);

    private final PluginManager pluginManager;
    private final ServerGuardConfig config;
    private final ObjectProvider<ServerGuardService> serverGuardServiceProvider;

    public PluginSystemService(ServerGuardConfig config,
                               ObjectProvider<ServerGuardService> serverGuardServiceProvider) {
        this.config = config;
        this.serverGuardServiceProvider = serverGuardServiceProvider;
        this.pluginManager = new PluginManager(resolvePluginsDirectory(config));
    }

    @PostConstruct
    public void start() {
        // 对接统一告警管道（延迟获取，避免与 ServerGuardService 的初始化顺序耦合）
        ServerGuardService serverGuardService = serverGuardServiceProvider.getIfAvailable();
        if (serverGuardService != null) {
            pluginManager.setAlertPublisher(serverGuardService::publishAlert);
        } else {
            logger.warn("未能获取 ServerGuardService，插件告警将仅记录日志");
        }

        if (!config.getPlugin().isEnabled()) {
            logger.info("插件系统已禁用（serverguard.plugin.enabled=false）");
            return;
        }

        int loaded = pluginManager.loadPluginsFromDirectory();
        logger.info("从 {} 加载了 {} 个插件", pluginManager.getPluginsDirectory(), loaded);

        pluginManager.enableAll();
        logger.info("插件系统就绪：已启用 {}/{} 个插件",
                pluginManager.getEnabledPlugins().size(), pluginManager.getPluginCount());
    }

    @PreDestroy
    public void shutdown() {
        pluginManager.disableAll();
        logger.info("插件系统已关闭");
    }

    public PluginManager getPluginManager() {
        return pluginManager;
    }

    private static Path resolvePluginsDirectory(ServerGuardConfig config) {
        String dir = config != null && config.getPlugin() != null
                ? config.getPlugin().getDirectory()
                : null;
        if (dir == null || dir.isBlank()) {
            return Path.of("serverguard", "plugins");
        }
        return Path.of(dir);
    }
}
