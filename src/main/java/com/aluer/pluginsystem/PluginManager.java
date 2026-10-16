package com.aluer.pluginsystem;

import com.aluer.model.AlertEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Constructor;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.stream.Stream;

/**
 * ServerGuard 插件管理器 —— 插件系统的核心，负责插件的发现、加载、启用、停用与卸载。
 *
 * <p>职责边界：</p>
 * <ul>
 *   <li><b>发现</b>：扫描插件目录下的 {@code *.jar}。</li>
 *   <li><b>加载</b>：读取 {@code META-INF/serverguard-plugin.properties} 或 {@link Plugin} 注解，
 *       用隔离的 {@link PluginClassLoader} 实例化主类并回调 {@code onLoad}。</li>
 *   <li><b>启用</b>：按依赖顺序回调 {@code onEnable}。</li>
 *   <li><b>停用/卸载</b>：回调 {@code onDisable}、移除命令、关闭类加载器。</li>
 * </ul>
 *
 * <p>本类不依赖 Spring 容器，可独立实例化（单元测试友好），
 * 由 {@code PluginSystemService} 在引擎启动时装配并与告警管道对接。</p>
 */
public class PluginManager {

    private static final Logger logger = LoggerFactory.getLogger(PluginManager.class);

    /** 插件描述文件在 JAR 内的固定路径 */
    private static final String PLUGIN_PROPERTIES = "META-INF/serverguard-plugin.properties";

    /** 默认插件目录 */
    private static final Path DEFAULT_PLUGINS_DIR = Path.of("serverguard", "plugins");

    /** 已加载插件（name → 装载体） */
    private final Map<String, LoadedPlugin> plugins = new ConcurrentHashMap<>();

    /** 插件注册的命令（命令名 → 命令定义） */
    private final Map<String, RegisteredCommand> commands = new ConcurrentHashMap<>();

    /** 插件目录 */
    private final Path pluginsDirectory;

    /** 告警发布器（由 Spring 集成层注入，可选） */
    private volatile Consumer<AlertEvent> alertPublisher;

    public PluginManager() {
        this(DEFAULT_PLUGINS_DIR);
    }

    public PluginManager(Path pluginsDirectory) {
        this.pluginsDirectory = pluginsDirectory != null ? pluginsDirectory : DEFAULT_PLUGINS_DIR;
    }

    public Path getPluginsDirectory() {
        return pluginsDirectory;
    }

    // ─── 目录发现 ──────────────────────────────────────

    /**
     * 扫描插件目录并加载所有 {@code *.jar}。
     *
     * @return 成功加载的插件数量
     */
    public int loadPluginsFromDirectory() {
        if (!Files.isDirectory(pluginsDirectory)) {
            logger.info("插件目录不存在，跳过扫描: {}", pluginsDirectory);
            return 0;
        }
        int loaded = 0;
        try (Stream<Path> stream = Files.list(pluginsDirectory)) {
            List<Path> jars = stream
                    .filter(p -> p.getFileName().toString().endsWith(".jar"))
                    .sorted()
                    .toList();
            for (Path jar : jars) {
                PluginLoadResult result = loadPluginJar(jar);
                if (result.isSuccess()) {
                    loaded++;
                } else {
                    logger.warn("加载插件失败 {}: {}", jar.getFileName(), result.getError());
                }
            }
        } catch (IOException e) {
            logger.error("扫描插件目录失败 {}: {}", pluginsDirectory, e.getMessage());
        }
        return loaded;
    }

    // ─── JAR 加载 ──────────────────────────────────────

    /**
     * 从 JAR 文件加载插件。
     *
     * <p>要求 JAR 内包含 {@code META-INF/serverguard-plugin.properties}，
     * 其中 {@code main} 键声明插件主类；其余元数据可由 properties 或主类上的
     * {@link Plugin} 注解提供。</p>
     */
    public PluginLoadResult loadPluginJar(Path jarPath) {
        if (jarPath == null || !Files.isRegularFile(jarPath)) {
            return PluginLoadResult.failure("JAR 不存在: " + jarPath);
        }
        try {
            Properties props = readPluginProperties(jarPath);
            String mainClass = props.getProperty("main");
            if (mainClass == null || mainClass.isBlank()) {
                return PluginLoadResult.failure(
                        "缺少 main 主类声明（" + PLUGIN_PROPERTIES + " 的 main 键）");
            }

            URL[] urls = new URL[]{jarPath.toUri().toURL()};
            PluginClassLoader loader = new PluginClassLoader(urls, ServerGuardPlugin.class.getClassLoader());

            Class<?> clazz = Class.forName(mainClass, true, loader);
            if (!ServerGuardPlugin.class.isAssignableFrom(clazz)) {
                return PluginLoadResult.failure("主类未实现 ServerGuardPlugin: " + mainClass);
            }

            ServerGuardPlugin instance = (ServerGuardPlugin) instantiate(clazz);
            PluginDescriptor descriptor = buildDescriptor(clazz, mainClass, props, jarPath.toString());
            return register(instance, descriptor, loader);
        } catch (Throwable e) {
            logger.debug("加载插件 JAR 失败", e);
            return PluginLoadResult.failure("加载失败: " + e.getMessage());
        }
    }

    // ─── 程序化注册（测试 / 内嵌插件友好） ─────────────

    /**
     * 直接注册一个已实例化的插件（用于测试或内嵌插件，不走 JAR 加载）。
     *
     * <p>注册即触发 {@code onLoad}，成功后将插件置于 {@code LOADED} 状态。</p>
     */
    public PluginLoadResult registerPlugin(ServerGuardPlugin plugin, PluginDescriptor descriptor) {
        return register(plugin, descriptor, null);
    }

    private PluginLoadResult register(ServerGuardPlugin plugin, PluginDescriptor descriptor,
                                      PluginClassLoader loader) {
        if (plugin == null) {
            return PluginLoadResult.failure("插件实例为空");
        }
        if (descriptor == null) {
            return PluginLoadResult.failure("插件描述符为空");
        }
        if (plugins.containsKey(descriptor.getName())) {
            return PluginLoadResult.failure("插件已存在: " + descriptor.getName());
        }

        LoadedPlugin loaded = new LoadedPlugin(descriptor, plugin, loader);
        plugins.put(descriptor.getName(), loaded);

        try {
            PluginContext context = new DefaultPluginContext(descriptor, this);
            plugin.onLoad(context);
            loaded.context = context;
            loaded.state = PluginState.LOADED;
            logger.info("插件已加载: {} v{}", descriptor.getName(), descriptor.getVersion());
            return PluginLoadResult.success(descriptor);
        } catch (Throwable e) {
            plugins.remove(descriptor.getName());
            logger.error("插件 {} onLoad 失败", descriptor.getName(), e);
            return PluginLoadResult.failure("onLoad 失败: " + e.getMessage());
        }
    }

    // ─── 生命周期 ──────────────────────────────────────

    /**
     * 按依赖顺序启用所有已加载插件。
     *
     * <p>采用多轮迭代：每轮尝试启用依赖已满足的插件，直至无新插件被启用。
     * 依赖缺失或启用失败的插件保持停用状态并记录警告。</p>
     */
    public void enableAll() {
        Set<String> enabled = new HashSet<>();
        boolean progress = true;
        while (progress) {
            progress = false;
            for (LoadedPlugin lp : plugins.values()) {
                if (lp.state == PluginState.ENABLED || enabled.contains(lp.descriptor.getName())) {
                    continue;
                }
                if (dependenciesSatisfied(lp.descriptor, enabled) && enable(lp)) {
                    enabled.add(lp.descriptor.getName());
                    progress = true;
                }
            }
        }
        for (LoadedPlugin lp : plugins.values()) {
            if (lp.state != PluginState.ENABLED) {
                logger.warn("插件 {} 未启用（依赖未满足或启用失败）", lp.descriptor.getName());
            }
        }
    }

    /** 启用指定插件 */
    public boolean enablePlugin(String name) {
        LoadedPlugin lp = plugins.get(name);
        if (lp == null) {
            logger.warn("启用失败：插件不存在 {}", name);
            return false;
        }
        return enable(lp);
    }

    private boolean enable(LoadedPlugin lp) {
        if (lp.state == PluginState.ENABLED) {
            return true;
        }
        try {
            lp.plugin.onEnable();
            lp.state = PluginState.ENABLED;
            logger.info("插件已启用: {}", lp.descriptor.getName());
            return true;
        } catch (Throwable e) {
            lp.state = PluginState.LOADED;
            logger.error("插件 {} onEnable 失败", lp.descriptor.getName(), e);
            return false;
        }
    }

    /** 停用指定插件（并移除其注册的命令） */
    public boolean disablePlugin(String name) {
        LoadedPlugin lp = plugins.get(name);
        if (lp == null) {
            return false;
        }
        if (lp.state == PluginState.ENABLED) {
            try {
                lp.plugin.onDisable();
            } catch (Throwable e) {
                logger.error("插件 {} onDisable 失败", lp.descriptor.getName(), e);
            }
            lp.state = PluginState.DISABLED;
            logger.info("插件已停用: {}", name);
        }
        commands.values().removeIf(c -> c.pluginName.equals(name));
        return true;
    }

    /** 停用所有插件 */
    public void disableAll() {
        for (String name : new ArrayList<>(plugins.keySet())) {
            disablePlugin(name);
        }
    }

    /** 卸载指定插件（停用 + 移除 + 关闭类加载器） */
    public boolean unloadPlugin(String name) {
        LoadedPlugin lp = plugins.get(name);
        if (lp == null) {
            return false;
        }
        disablePlugin(name);
        plugins.remove(name);
        if (lp.classLoader != null) {
            try {
                lp.classLoader.close();
            } catch (IOException e) {
                logger.debug("关闭插件类加载器失败 {}: {}", name, e.getMessage());
            }
        }
        logger.info("插件已卸载: {}", name);
        return true;
    }

    // ─── 查询 ──────────────────────────────────────────

    public PluginDescriptor getPlugin(String name) {
        LoadedPlugin lp = plugins.get(name);
        return lp != null ? lp.descriptor : null;
    }

    public Collection<PluginDescriptor> getPlugins() {
        return plugins.values().stream().map(lp -> lp.descriptor).toList();
    }

    /** 当前处于 ENABLED 状态的插件描述符 */
    public Collection<PluginDescriptor> getEnabledPlugins() {
        return plugins.values().stream()
                .filter(lp -> lp.state == PluginState.ENABLED)
                .map(lp -> lp.descriptor)
                .toList();
    }

    public boolean isLoaded(String name) {
        return plugins.containsKey(name);
    }

    public int getPluginCount() {
        return plugins.size();
    }

    // ─── 命令 ──────────────────────────────────────────

    /** 注册插件命令（由 {@link PluginContext#registerCommand} 委托） */
    public void registerCommand(String pluginName, String name, String description,
                                PluginCommandHandler handler) {
        if (name == null || name.isBlank() || handler == null) {
            logger.warn("忽略非法命令注册: plugin={}, name={}", pluginName, name);
            return;
        }
        commands.put(name, new RegisteredCommand(pluginName, name, description, handler));
    }

    /** 执行插件注册的命令，返回命令输出；命令不存在返回 null */
    public String executeCommand(String name, String[] args) {
        RegisteredCommand cmd = commands.get(name);
        if (cmd == null) {
            return null;
        }
        try {
            return cmd.handler.execute(args == null ? new String[0] : args);
        } catch (Throwable e) {
            logger.error("插件命令 {} 执行失败", name, e);
            return "命令执行失败: " + e.getMessage();
        }
    }

    /** 已注册的命令名集合 */
    public Collection<String> getCommandNames() {
        return new ArrayList<>(commands.keySet());
    }

    // ─── 告警发布 ──────────────────────────────────────

    /** 注入告警发布器（由 Spring 集成层对接引擎告警管道） */
    public void setAlertPublisher(Consumer<AlertEvent> publisher) {
        this.alertPublisher = publisher;
    }

    /** 发布告警到引擎管道（未配置发布器时静默丢弃并记录调试日志） */
    public void publishAlert(AlertEvent event) {
        Consumer<AlertEvent> publisher = alertPublisher;
        if (publisher != null) {
            publisher.accept(event);
        } else {
            logger.debug("未配置告警发布器，告警被丢弃: {}", event != null ? event.getMessage() : "null");
        }
    }

    // ─── 内部辅助 ──────────────────────────────────────

    private boolean dependenciesSatisfied(PluginDescriptor descriptor, Set<String> enabled) {
        for (String dep : descriptor.getDependencies()) {
            if (!enabled.contains(dep)) {
                return false;
            }
        }
        return true;
    }

    private Properties readPluginProperties(Path jarPath) throws IOException {
        Properties props = new Properties();
        try (JarFile jar = new JarFile(jarPath.toFile())) {
            JarEntry entry = jar.getJarEntry(PLUGIN_PROPERTIES);
            if (entry != null) {
                try (InputStream in = jar.getInputStream(entry)) {
                    props.load(in);
                }
            }
        }
        return props;
    }

    private PluginDescriptor buildDescriptor(Class<?> clazz, String mainClass,
                                             Properties props, String source) {
        Plugin annotation = clazz.getAnnotation(Plugin.class);
        if (annotation != null) {
            return PluginDescriptor.fromAnnotation(annotation, mainClass, source);
        }
        String name = props.getProperty("name",
                mainClass.substring(mainClass.lastIndexOf('.') + 1));
        String version = props.getProperty("version", "1.0.0");
        return PluginDescriptor.builder(name, version)
                .author(props.getProperty("author", ""))
                .description(props.getProperty("description", ""))
                .apiVersion(props.getProperty("api-version", "1.0"))
                .mainClass(mainClass)
                .dependencies(parseList(props.getProperty("depends", "")))
                .source(source)
                .build();
    }

    private List<String> parseList(String csv) {
        if (csv == null || csv.isBlank()) {
            return List.of();
        }
        List<String> result = new ArrayList<>();
        for (String item : csv.split(",")) {
            String trimmed = item.trim();
            if (!trimmed.isEmpty()) {
                result.add(trimmed);
            }
        }
        return result;
    }

    private Object instantiate(Class<?> clazz) throws Exception {
        Constructor<?> constructor = clazz.getDeclaredConstructor();
        constructor.setAccessible(true);
        return constructor.newInstance();
    }

    // ─── 内部类型 ──────────────────────────────────────

    /** 插件状态机 */
    private enum PluginState {
        /** 已加载（onLoad 完成，尚未启用） */
        LOADED,
        /** 已启用（onEnable 完成） */
        ENABLED,
        /** 已停用（onDisable 完成） */
        DISABLED
    }

    /** 已加载插件的运行时载体 */
    private static final class LoadedPlugin {
        final PluginDescriptor descriptor;
        final ServerGuardPlugin plugin;
        final PluginClassLoader classLoader;
        volatile PluginContext context;
        volatile PluginState state = PluginState.LOADED;

        LoadedPlugin(PluginDescriptor descriptor, ServerGuardPlugin plugin, PluginClassLoader classLoader) {
            this.descriptor = descriptor;
            this.plugin = plugin;
            this.classLoader = classLoader;
        }
    }

    /** 插件注册的命令定义 */
    private static final class RegisteredCommand {
        final String pluginName;
        final String name;
        final String description;
        final PluginCommandHandler handler;

        RegisteredCommand(String pluginName, String name, String description, PluginCommandHandler handler) {
            this.pluginName = pluginName;
            this.name = name;
            this.description = description;
            this.handler = handler;
        }
    }

    /** {@link PluginContext} 默认实现 */
    private static final class DefaultPluginContext implements PluginContext {
        private final PluginDescriptor descriptor;
        private final PluginManager manager;
        private final Logger logger;

        DefaultPluginContext(PluginDescriptor descriptor, PluginManager manager) {
            this.descriptor = descriptor;
            this.manager = manager;
            this.logger = LoggerFactory.getLogger("com.aluer.pluginsystem.plugin." + descriptor.getName());
        }

        @Override
        public Logger getLogger() {
            return logger;
        }

        @Override
        public PluginDescriptor getDescriptor() {
            return descriptor;
        }

        @Override
        public PluginManager getPluginManager() {
            return manager;
        }

        @Override
        public void publishAlert(AlertEvent event) {
            manager.publishAlert(event);
        }

        @Override
        public void registerCommand(String name, String description, PluginCommandHandler handler) {
            manager.registerCommand(descriptor.getName(), name, description, handler);
        }

        @Override
        @SuppressWarnings("unchecked")
        public <T> T getService(Class<T> type) {
            try {
                var context = com.aluer.ServerGuardApplication.getContext();
                if (context != null) {
                    return context.getBean(type);
                }
            } catch (Throwable ignored) {
                // 容器未启动或无该 Bean
            }
            return null;
        }
    }
}
