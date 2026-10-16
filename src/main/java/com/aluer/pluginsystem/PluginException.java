package com.aluer.pluginsystem;

/**
 * 插件系统运行时异常。
 *
 * <p>用于在插件加载/启用/停用/卸载失败时抛出的统一异常类型，
 * 调用方据此区分插件系统错误与一般运行时错误。</p>
 */
public class PluginException extends RuntimeException {

    public PluginException(String message) {
        super(message);
    }

    public PluginException(String message, Throwable cause) {
        super(message, cause);
    }
}
