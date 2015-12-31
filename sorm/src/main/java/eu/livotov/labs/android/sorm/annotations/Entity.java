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
public @interface Entity
{

    public abstract String table() default "";

    public abstract boolean inheritance() default false;

    public abstract String[] upgradeStatements() default {};
}
