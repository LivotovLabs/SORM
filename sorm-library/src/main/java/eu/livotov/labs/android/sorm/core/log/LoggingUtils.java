package eu.livotov.labs.android.sorm.core.log;

import android.util.Log;

/**
 * Created by IntelliJ IDEA.
 * User: dlivotov
 * Date: 11.12.10
 * Time: 22:21
 * To change this template use File | Settings | File Templates.
 */
public class LoggingUtils
{
    public final static String TAG = "SORM";

    public static void e(String msg)
    {
        Log.e(TAG, msg);
    }

    public static void e(String msg, Throwable err)
    {
        Log.e(TAG, msg, err);
    }

    public static void e(Throwable err)
    {
        Log.e(TAG, err.getMessage(), err);
    }

    public static void i(String msg, String... params)
    {
        if (params == null || params.length == 0)
        {
            Log.i(TAG, msg);
        }
        else
        {
            Log.i(TAG, String.format(msg, params));
        }
    }

    public static void d(String msg)
    {
        if (Log.isLoggable(TAG, Log.DEBUG))
        {
            Log.d(TAG, msg);
        }
    }

    public static void d(String msg, Throwable err)
    {
        if (Log.isLoggable(TAG, Log.DEBUG))
        {
            Log.d(TAG, msg, err);
        }
    }
}
