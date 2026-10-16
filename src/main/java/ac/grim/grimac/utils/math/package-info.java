/**
 * 数学基础库 —— 移植自 GrimAC 的移动模拟引擎。
 *
 * <p>本包代码原样复制自开源反作弊 <a href="https://github.com/GrimAnticheat/Grim">GrimAC</a>
 * （GNU GPL-3.0），属于其「移动模拟引擎（prediction engine）」的数学基础设施，提供：</p>
 * <ul>
 *   <li>{@link ac.grim.grimac.utils.math.Vector3dm} / {@link ac.grim.grimac.utils.math.Vec2}：高精度向量类型。</li>
 *   <li>{@link ac.grim.grimac.utils.math.GrimMath}：物理计算用数学工具（clamp / gcd / sqrt / lerp 等）。</li>
 *   <li>{@link ac.grim.grimac.utils.math.VanillaMath} 及 Optifine/Legacy/Modern 变体：三角函数实现，用于精确复现客户端移动。</li>
 * </ul>
 *
 * <p>保留 {@code ac.grim.grimac} 原始包名以便后续同步上游更新；Aluer 自有检测服务
 * （{@code com.aluer.anticheat}）通过适配器调用本引擎。依赖 PacketEvents（provided）与 Lombok。</p>
 *
 * <p>注意：本包为移动模拟引擎的<b>第一阶段</b>移植，仅包含不依赖 {@code GrimPlayer} /
 * {@code platform API} / 碰撞箱的基础数学类型；依赖上述组件的 {@code VectorUtils} /
 * {@code Location} / {@code TrigHandler} 待后续阶段移植。</p>
 */
package ac.grim.grimac.utils.math;
