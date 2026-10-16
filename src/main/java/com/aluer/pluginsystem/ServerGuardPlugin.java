package com.aluer.pluginsystem;

/**
 * ServerGuard 插件生命周期接口。
 *
 * <p>第三方开发者实现本接口以扩展 ServerGuard 引擎能力（自定义检测器、命令、告警处理等）。
 * 生命周期由 {@link PluginManager} 驱动：</p>
 *
 * <ol>
 *   <li>{@link #onLoad(PluginContext)} —— 插件被加载，可读取配置、注册命令、保存上下文引用。</li>
 *   <li>{@link #onEnable()} —— 插件被启用，启动后台任务、注册监听器等。</li>
 *   <li>{@link #onDisable()} —— 插件被停用，释放资源、停止线程。</li>
 * </ol>
 *
 * <p>实现类必须提供公开无参构造方法，供 {@code PluginClassLoader} 反射实例化。</p>
 */
public interface ServerGuardPlugin {

    /**
     * 插件加载回调（实例化后立即调用）。
     *
     * @param context 插件上下文，提供日志、服务查找、命令注册、告警发布等能力
     */
    void onLoad(PluginContext context);

    /** 插件启用回调（依赖满足且调用 enable 时触发） */
    void onEnable();

    /** 插件停用回调（卸载前调用，用于释放资源） */
    void onDisable();
}
