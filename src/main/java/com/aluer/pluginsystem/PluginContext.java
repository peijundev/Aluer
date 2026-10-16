package com.aluer.pluginsystem;

import com.aluer.model.AlertEvent;
import org.slf4j.Logger;

/**
 * 插件上下文 —— 插件与 ServerGuard 引擎交互的唯一入口。
 *
 * <p>在 {@link ServerGuardPlugin#onLoad(PluginContext)} 阶段由 {@link PluginManager}
 * 注入，提供以下能力：</p>
 * <ul>
 *   <li>获取插件自身日志器（按插件名隔离）</li>
 *   <li>读取插件描述符</li>
 *   <li>发布安全告警到引擎告警管道</li>
 *   <li>注册插件自定义命令</li>
 *   <li>从 Spring 容器查找引擎服务 Bean</li>
 * </ul>
 */
public interface PluginContext {

    /** 获取专属于本插件的日志器（logger name = 插件名） */
    Logger getLogger();

    /** 获取本插件的描述符 */
    PluginDescriptor getDescriptor();

    /** 获取插件管理器 */
    PluginManager getPluginManager();

    /**
     * 发布一条安全告警到引擎告警管道。
     *
     * <p>告警最终会被路由到引擎的告警处理器（若已配置），
     * 插件无需关心下游具体是 WebSocket 转发还是本地存储。</p>
     */
    void publishAlert(AlertEvent event);

    /**
     * 注册一条插件命令。
     *
     * @param name        命令名（不含斜杠，例如 "myplugin-scan"）
     * @param description 命令描述
     * @param handler     命令处理器
     */
    void registerCommand(String name, String description, PluginCommandHandler handler);

    /**
     * 从 Spring 容器查找引擎服务 Bean。
     *
     * @param type 目标类型
     * @return 对应 Bean，容器未启动或无该 Bean 时返回 null
     */
    <T> T getService(Class<T> type);
}
