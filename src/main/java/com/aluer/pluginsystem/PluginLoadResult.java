package com.aluer.pluginsystem;

/**
 * 插件加载结果。
 *
 * <p>加载成功时携带 {@link PluginDescriptor}，失败时携带错误信息。
 * 采用静态工厂方法构造，调用方通过 {@link #isSuccess()} 分支处理。</p>
 */
public final class PluginLoadResult {

    private final boolean success;
    private final PluginDescriptor descriptor;
    private final String error;

    private PluginLoadResult(boolean success, PluginDescriptor descriptor, String error) {
        this.success = success;
        this.descriptor = descriptor;
        this.error = error;
    }

    /** 加载成功 */
    public static PluginLoadResult success(PluginDescriptor descriptor) {
        return new PluginLoadResult(true, descriptor, null);
    }

    /** 加载失败 */
    public static PluginLoadResult failure(String error) {
        return new PluginLoadResult(false, null, error);
    }

    public boolean isSuccess() { return success; }
    public PluginDescriptor getDescriptor() { return descriptor; }
    public String getError() { return error; }
}
