package valkyrie.driver.api;

import static valkyrie.utils.string.StrStaticImports.streq;

/**
 * 可封存完整性码的抽象基类。
 * <p>
 * 该类用于支持对象完整性校验机制：子类需实现 {@link #computeIntegrityCode()} 以计算当前状态的完整性码，
 * 通过调用 {@link #finalIntegrityCode()} 可将计算出的码封存（冻结），之后可通过 {@link #isIntegrityValid()}
 * 验证对象自封存后是否被篡改。
 * <p>
 * <b>典型使用流程：</b>
 * <ol>
 *   <li>创建子类实例并完成状态初始化</li>
 *   <li>调用 {@link #finalIntegrityCode()} 封存当前状态的完整性码</li>
 *   <li>后续在需要校验完整性时调用 {@link #isIntegrityValid()}</li>
 * </ol>
 * <p>
 * <b>设计说明：</b>
 * <ul>
 *   <li>一旦调用 {@code finalIntegrityCode()}，再次调用将抛出 {@link IllegalCallerException}</li>
 *   <li>在调用 {@code finalIntegrityCode()} 之前，{@code isIntegrityValid()} 的行为未定义（通常返回 {@code false}）</li>
 *   <li>子类应确保 {@code computeIntegrityCode()} 是确定性的，且基于对象所有相关的状态字段计算</li>
 * </ul>
 *
 * @author Luo Tiansheng
 * @since 2026/4/11
 */
public abstract class Sealable {

        /**
         * 标记完整性码是否已被封存。
         * <p>
         * 初始值为 {@code false}，调用 {@link #finalIntegrityCode()} 后变为 {@code true}，
         * 并阻止后续再次封存。
         */
        private boolean isFinalIntegrityCode = false;

        /**
         * 已封存的完整性码。
         * <p>
         * 仅在 {@link #finalIntegrityCode()} 成功调用后被赋值，否则为 {@code null}。
         */
        private String integrityCode;

        /**
         * 计算当前对象的完整性码。
         * <p>
         * 子类必须实现该方法，基于对象的关键状态（如所有字段值）生成一个唯一标识当前状态的字符串。
         * 该方法的实现应满足：
         * <ul>
         *   <li>幂等性：同一状态下多次调用应返回相同结果</li>
         *   <li>敏感性：状态改变应导致不同的完整性码</li>
         *   <li>效率：应避免重计算开销过大的操作</li>
         * </ul>
         *
         * @return 当前状态的完整性码（不能为 {@code null}）
         */
        public abstract String computeIntegrityCode();

        /**
         * 封存当前对象的完整性码。
         * <p>
         * 调用该方法将计算当前完整性码并冻结，后续任何对 {@code finalIntegrityCode()} 的再次调用
         * 都会抛出异常。封存后，可通过 {@link #isIntegrityValid()} 验证对象完整性。
         *
         * @throws IllegalCallerException 如果该方法已被调用过一次（即完整性码已封存）
         */
        public void finalIntegrityCode() {
                if (isFinalIntegrityCode)
                        throw new IllegalCallerException("Illegal call finalIntegrityCode()");

                integrityCode = computeIntegrityCode();
                isFinalIntegrityCode = true;
        }

        /**
         * 校验对象的完整性是否有效。
         * <p>
         * 该方法重新计算当前状态的完整性码，并与封存时的码进行比较。
         * <p>
         * <b>注意：</b>
         * <ul>
         *   <li>如果在调用 {@link #finalIntegrityCode()} 之前调用该方法，由于封存码尚未设置，
         *       返回结果通常为 {@code false}（因为 {@code integrityCode} 为 {@code null}，
         *       与重新计算得到的非空码不相等）</li>
         *   <li>该方法不会修改封存状态，可多次调用</li>
         * </ul>
         *
         * @return {@code true} 如果当前状态的完整性码与封存时的码相同（即对象未被篡改）；
         *         {@code false} 否则（包括从未封存、封存后状态改变、或计算出的码与封存码不一致）
         */
        public boolean isIntegrityValid() {
                return streq(computeIntegrityCode(), integrityCode);
        }
}
