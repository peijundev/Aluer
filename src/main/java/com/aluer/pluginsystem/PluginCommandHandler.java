package com.aluer.pluginsystem;

/**
 * 插件命令处理器函数式接口。
 *
 * <p>插件通过 {@link PluginContext#registerCommand} 注册自定义命令，
 * 命令被触发时回调本接口。</p>
 */
@FunctionalInterface
public interface PluginCommandHandler {

    /**
     * 执行命令。
     *
     * @param args 命令参数（不含命令名本身）
     * @return 命令输出文本（展示给调用方，可能为 null）
     */
    String execute(String[] args);
}
