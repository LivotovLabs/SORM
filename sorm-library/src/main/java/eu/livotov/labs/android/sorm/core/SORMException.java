package eu.livotov.labs.android.sorm.core;

/**
 * Created by IntelliJ IDEA.
 * User: dlivotov
 * Date: 11.12.10
 * Time: 21:41
 * To change this template use File | Settings | File Templates.
 */
public class SORMException extends RuntimeException
{

    public SORMException(String message)
    {
        super(message);
    }

    public SORMException(String message, Throwable throwable)
    {
        super(message, throwable);
    }

    public SORMException(Throwable throwable)
    {
        super(throwable);
    }
}
