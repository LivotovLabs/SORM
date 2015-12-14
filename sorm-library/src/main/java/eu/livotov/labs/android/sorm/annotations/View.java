package eu.livotov.labs.android.sorm.annotations;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Created by IntelliJ IDEA.
 * User: dlivotov
 * Date: 11.12.10
 * Time: 17:03
 * To change this template use File | Settings | File Templates.
 */
@Retention(RetentionPolicy.RUNTIME)
public @interface View
{

    public abstract String name();

    public abstract String query();

    public abstract ViewColumnMap[] columnMap() default {};
}
