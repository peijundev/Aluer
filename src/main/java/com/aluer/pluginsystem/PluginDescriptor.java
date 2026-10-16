package com.aluer.pluginsystem;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * 插件描述符 —— 描述一个 ServerGuard 插件的不可变元数据。
 *
 * <p>元数据来源有二：JAR 内 {@code META-INF/serverguard-plugin.properties}，或插件主类上的
 * {@link Plugin} 注解。本类由 {@link PluginManager} 在加载时组装，加载后用于依赖解析、
 * 状态查询与展示。</p>
 */
public final class PluginDescriptor {

    /** 插件唯一名称 */
    private final String name;

    /** 插件版本 */
    private final String version;

    /** 作者 */
    private final String author;

    /** 描述 */
    private final String description;

    /** 声明的插件 API 版本 */
    private final String apiVersion;

    /** 主类全限定名 */
    private final String mainClass;

    /** 依赖的其他插件名称 */
    private final List<String> dependencies;

    /** 来源（JAR 路径或 "classpath"） */
    private final String source;

    public PluginDescriptor(String name, String version, String author, String description,
                            String apiVersion, String mainClass, List<String> dependencies, String source) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("插件名称不能为空");
        }
        if (version == null || version.isBlank()) {
            throw new IllegalArgumentException("插件版本不能为空: " + name);
        }
        this.name = name;
        this.version = version;
        this.author = author == null ? "" : author;
        this.description = description == null ? "" : description;
        this.apiVersion = apiVersion == null ? "1.0" : apiVersion;
        this.mainClass = mainClass;
        this.dependencies = dependencies == null ? List.of() : List.copyOf(dependencies);
        this.source = source == null ? "unknown" : source;
    }

    public String getName() { return name; }
    public String getVersion() { return version; }
    public String getAuthor() { return author; }
    public String getDescription() { return description; }
    public String getApiVersion() { return apiVersion; }
    public String getMainClass() { return mainClass; }
    public List<String> getDependencies() { return dependencies; }
    public String getSource() { return source; }

    /**
     * 从 {@link Plugin} 注解构造描述符（注解字段优先于 properties 提供的默认值）。
     *
     * @param annotation 插件主类上的注解
     * @param mainClass  主类全限定名
     * @param source     来源
     */
    public static PluginDescriptor fromAnnotation(Plugin annotation, String mainClass, String source) {
        return new PluginDescriptor(
                annotation.name(),
                annotation.version(),
                annotation.author(),
                annotation.description(),
                annotation.apiVersion(),
                mainClass,
                List.of(annotation.depends()),
                source);
    }

    /** 构建一个可变的 Builder 副本，便于从 properties 逐步填充 */
    public static Builder builder(String name, String version) {
        return new Builder(name, version);
    }

    /** 描述符构建器 */
    public static final class Builder {
        private final String name;
        private final String version;
        private String author = "";
        private String description = "";
        private String apiVersion = "1.0";
        private String mainClass;
        private List<String> dependencies = Collections.emptyList();
        private String source = "unknown";

        private Builder(String name, String version) {
            this.name = name;
            this.version = version;
        }

        public Builder author(String v) { this.author = v; return this; }
        public Builder description(String v) { this.description = v; return this; }
        public Builder apiVersion(String v) { this.apiVersion = v; return this; }
        public Builder mainClass(String v) { this.mainClass = v; return this; }
        public Builder dependencies(List<String> v) { this.dependencies = v; return this; }
        public Builder source(String v) { this.source = v; return this; }

        public PluginDescriptor build() {
            return new PluginDescriptor(name, version, author, description, apiVersion,
                    mainClass, dependencies, source);
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof PluginDescriptor that)) return false;
        return name.equals(that.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name);
    }

    @Override
    public String toString() {
        return "PluginDescriptor{name='" + name + "', version='" + version + "', main='" + mainClass + "'}";
    }
}
