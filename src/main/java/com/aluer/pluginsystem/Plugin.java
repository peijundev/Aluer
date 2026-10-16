package com.aluer.pluginsystem;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 标记一个类为 ServerGuard 插件的入口类。
 *
 * <p>插件主类实现 {@link ServerGuardPlugin} 接口，并可选地使用本注解声明元数据。
 * 元数据也可通过 JAR 内 {@code META-INF/serverguard-plugin.properties} 提供，
 * 两者同时存在时注解字段会覆盖 properties 中的同名字段（注解视为作者意图的权威来源）。</p>
 *
 * <p>加载插件时 {@link PluginManager} 通过反射读取本注解构造 {@link PluginDescriptor}。</p>
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface Plugin {

    /** 插件唯一名称（用于依赖解析与命令前缀） */
    String name();

    /** 插件版本（语义化版本，例如 "1.2.0"） */
    String version();

    /** 作者 */
    String author() default "";

    /** 一句话描述 */
    String description() default "";

    /** 插件声明的 ServerGuard 插件 API 版本 */
    String apiVersion() default "1.0";

    /** 依赖的其他插件名称列表（启用前必须已加载） */
    String[] depends() default {};
}
