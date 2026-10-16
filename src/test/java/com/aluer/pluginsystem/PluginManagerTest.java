package com.aluer.pluginsystem;

import com.aluer.model.AlertEvent;
import com.aluer.model.AlertType;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

class PluginManagerTest {

    private PluginManager newManager() {
        return new PluginManager(Path.of("target", "test-plugins"));
    }

    private PluginDescriptor descriptor(String name, String version, List<String> depends) {
        return PluginDescriptor.builder(name, version)
                .mainClass("test." + name + ".Main")
                .dependencies(depends)
                .build();
    }

    @Test
    void registerPluginTriggersOnLoadAndBecomesLoaded() {
        PluginManager pm = newManager();
        TestPlugin plugin = new TestPlugin("test", "1.0.0", List.of());

        PluginLoadResult result = pm.registerPlugin(plugin, descriptor("test", "1.0.0", List.of()));

        assertTrue(result.isSuccess());
        assertEquals("test", result.getDescriptor().getName());
        assertTrue(pm.isLoaded("test"));
        assertEquals(1, pm.getPluginCount());
        assertTrue(plugin.events.contains("load"));
    }

    @Test
    void rejectDuplicatePluginName() {
        PluginManager pm = newManager();
        pm.registerPlugin(new TestPlugin("dup", "1.0.0", List.of()), descriptor("dup", "1.0.0", List.of()));

        PluginLoadResult second = pm.registerPlugin(
                new TestPlugin("dup", "2.0.0", List.of()), descriptor("dup", "2.0.0", List.of()));

        assertFalse(second.isSuccess());
        assertEquals(1, pm.getPluginCount());
    }

    @Test
    void rejectNullInputs() {
        PluginManager pm = newManager();
        assertFalse(pm.registerPlugin(null, descriptor("x", "1.0.0", List.of())).isSuccess());
        assertFalse(pm.registerPlugin(new TestPlugin("x", "1.0.0", List.of()), null).isSuccess());
    }

    @Test
    void enableAndDisableLifecycle() {
        PluginManager pm = newManager();
        TestPlugin plugin = new TestPlugin("life", "1.0.0", List.of());
        pm.registerPlugin(plugin, descriptor("life", "1.0.0", List.of()));

        assertTrue(pm.enablePlugin("life"));
        assertTrue(plugin.events.contains("enable"));
        assertEquals(1, pm.getEnabledPlugins().size());

        assertTrue(pm.disablePlugin("life"));
        assertTrue(plugin.events.contains("disable"));
        assertEquals(0, pm.getEnabledPlugins().size());
    }

    @Test
    void unloadRemovesPlugin() {
        PluginManager pm = newManager();
        pm.registerPlugin(new TestPlugin("gone", "1.0.0", List.of()), descriptor("gone", "1.0.0", List.of()));

        assertTrue(pm.isLoaded("gone"));
        assertTrue(pm.unloadPlugin("gone"));
        assertFalse(pm.isLoaded("gone"));
        assertEquals(0, pm.getPluginCount());
    }

    @Test
    void commandRegistrationAndExecution() {
        PluginManager pm = newManager();
        // TestPlugin 在 onLoad 中注册名为 <name>-cmd 的命令
        pm.registerPlugin(new TestPlugin("cmd", "1.0.0", List.of()), descriptor("cmd", "1.0.0", List.of()));

        assertTrue(pm.getCommandNames().contains("cmd-cmd"));
        assertEquals("ok:cmd", pm.executeCommand("cmd-cmd", new String[]{}));
        assertNull(pm.executeCommand("nonexistent", new String[]{}));
    }

    @Test
    void disableRemovesPluginCommands() {
        PluginManager pm = newManager();
        pm.registerPlugin(new TestPlugin("cmddel", "1.0.0", List.of()), descriptor("cmddel", "1.0.0", List.of()));
        assertTrue(pm.getCommandNames().contains("cmddel-cmd"));

        pm.disablePlugin("cmddel");
        assertFalse(pm.getCommandNames().contains("cmddel-cmd"));
    }

    @Test
    void enableAllRespectsDependencyOrder() {
        PluginManager pm = newManager();
        // 共享启用顺序时间线，用于断言依赖顺序
        List<String> enableOrder = new ArrayList<>();
        TestPlugin a = new TestPlugin("a", "1.0.0", List.of("b"), enableOrder);
        TestPlugin b = new TestPlugin("b", "1.0.0", List.of(), enableOrder);
        // 故意先注册依赖方 a，再注册被依赖方 b，验证 enableAll 能按依赖顺序启用
        pm.registerPlugin(a, descriptor("a", "1.0.0", List.of("b")));
        pm.registerPlugin(b, descriptor("b", "1.0.0", List.of()));

        pm.enableAll();

        assertEquals(2, pm.getEnabledPlugins().size());
        // b 必须先于 a 启用
        assertEquals(List.of("b", "a"), enableOrder);
    }

    @Test
    void enableAllSkipsUnsatisfiedDependency() {
        PluginManager pm = newManager();
        TestPlugin orphan = new TestPlugin("orphan", "1.0.0", List.of("missing-dep"));
        pm.registerPlugin(orphan, descriptor("orphan", "1.0.0", List.of("missing-dep")));

        pm.enableAll();

        assertFalse(orphan.events.contains("enable"));
        assertEquals(0, pm.getEnabledPlugins().size());
    }

    @Test
    void publishAlertInvokesPublisherWhenConfigured() {
        PluginManager pm = newManager();
        AtomicInteger count = new AtomicInteger();
        pm.setAlertPublisher(event -> count.incrementAndGet());

        AlertEvent event = new AlertEvent();
        event.setType(AlertType.SECURITY_OTHER);
        event.setMessage("测试告警");
        pm.publishAlert(event);

        assertEquals(1, count.get());
    }

    @Test
    void publishAlertIsSafeWithoutPublisher() {
        PluginManager pm = newManager();
        AlertEvent event = new AlertEvent();
        event.setMessage("无发布器告警");
        // 不应抛异常
        pm.publishAlert(event);
    }

    @Test
    void descriptorFromAnnotation() {
        @Plugin(name = "anno", version = "2.0.0", author = "tester",
                description = "desc", depends = {"x", "y"})
        class AnnotatedPlugin implements ServerGuardPlugin {
            @Override public void onLoad(PluginContext context) {}
            @Override public void onEnable() {}
            @Override public void onDisable() {}
        }

        PluginDescriptor d = PluginDescriptor.fromAnnotation(
                AnnotatedPlugin.class.getAnnotation(Plugin.class),
                "com.example.AnnotatedPlugin", "classpath");

        assertEquals("anno", d.getName());
        assertEquals("2.0.0", d.getVersion());
        assertEquals("tester", d.getAuthor());
        assertEquals(List.of("x", "y"), d.getDependencies());
    }

    // ─── 测试插件实现 ─────────────────────────────────

    static class TestPlugin implements ServerGuardPlugin {
        final String name;
        final String version;
        final List<String> depends;
        final List<String> events = new ArrayList<>();
        /** 可选的跨插件共享启用顺序时间线（用于依赖顺序断言） */
        final List<String> enableOrder;
        PluginContext context;

        TestPlugin(String name, String version, List<String> depends) {
            this(name, version, depends, null);
        }

        TestPlugin(String name, String version, List<String> depends, List<String> enableOrder) {
            this.name = name;
            this.version = version;
            this.depends = depends;
            this.enableOrder = enableOrder;
        }

        @Override
        public void onLoad(PluginContext context) {
            this.context = context;
            events.add("load");
            context.registerCommand(name + "-cmd", "test command", args -> "ok:" + name);
        }

        @Override
        public void onEnable() {
            events.add("enable");
            if (enableOrder != null) {
                enableOrder.add(name);
            }
        }

        @Override
        public void onDisable() {
            events.add("disable");
        }
    }
}
