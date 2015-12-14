package eu.livotov.labs.android.sorm.annotations;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Created by IntelliJ IDEA.
 * User: dlivotov
 * Date: 11.12.10
 * Time: 17:04
 * To change this template use File | Settings | File Templates.
 */
@Retention(RetentionPolicy.RUNTIME)
public @interface Column
{

    public abstract String name() default "";

    public abstract boolean nullable() default true;

    public abstract boolean unique() default false;

    public abstract String defaultValue() default "";

    public abstract String type() default "";

    public abstract long size() default 0;

    public abstract boolean lazy() default false;
}
