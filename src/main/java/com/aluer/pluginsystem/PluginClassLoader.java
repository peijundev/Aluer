package com.aluer.pluginsystem;

import java.net.URL;
import java.net.URLClassLoader;

/**
 * 插件隔离类加载器。
 *
 * <p>每个插件 JAR 使用独立的 {@code PluginClassLoader} 加载，以实现类隔离。
 * 采用父优先（parent-first）策略：优先从父加载器（ServerGuard 引擎 + Spring + JDK）解析，
 * 保证插件能访问 {@code com.aluer.*} 引擎类与 Spring 依赖，同时又不把自身类泄露给其他插件。</p>
 *
 * <p>卸载插件时关闭本类加载器即可释放 JAR 文件句柄，使 JAR 可被热替换。</p>
 */
public class PluginClassLoader extends URLClassLoader {

    /**
     * @param urls   插件 JAR 的 URL 列表
     * @param parent 父类加载器（通常为 ServerGuard 引擎类加载器）
     */
    public PluginClassLoader(URL[] urls, ClassLoader parent) {
        super(urls, parent);
    }
}
