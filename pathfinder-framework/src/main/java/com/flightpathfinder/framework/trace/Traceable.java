package com.flightpathfinder.framework.trace;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 声明可追踪节点的标记注解。
 *
 * 用于定义当前类型或方法在模块内的职责边界。
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



