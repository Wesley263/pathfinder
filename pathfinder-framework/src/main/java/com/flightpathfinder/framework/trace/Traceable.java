package com.flightpathfinder.framework.trace;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 声明可追踪节点的标记注解。
 *
 * <p>可标注在类型或方法上，供追踪拦截逻辑识别并写入节点信息。
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface Traceable {

    /**
     * 自定义追踪节点名。
     *
     * @return 节点名；为空时由调用侧推导
     */
    String value() default "";
}

