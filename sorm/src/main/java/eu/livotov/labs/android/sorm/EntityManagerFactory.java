package eu.livotov.labs.android.sorm;

import android.content.Context;

import java.util.HashMap;
import java.util.Map;

import eu.livotov.labs.android.sorm.core.config.EntityManagerConfiguration;

/**
 * Created by IntelliJ IDEA.
 * User: dlivotov
 * Date: 12.12.10
 * Time: 17:16
 * To change this template use File | Settings | File Templates.
 */
public class EntityManagerFactory
{
    private static Map<String, EntityManager> entityManagersCache = new HashMap<String, EntityManager>();

    public static synchronized EntityManager getEntityManager(Context ctx, EntityManagerConfiguration cfg)
    {
        final String emKey = ctx.getPackageName();

        if (!entityManagersCache.containsKey(emKey))
        {
            entityManagersCache.put(emKey, new EntityManager(ctx, cfg));
        }

        return entityManagersCache.get(emKey);
    }

    public static synchronized void closeEntityManager(Context ctx)
    {
        EntityManager manager = getEntityManager(ctx);
        manager.close();
        entityManagersCache.remove(ctx.getPackageName());
    }

    public static synchronized EntityManager getEntityManager(Context ctx)
    {
        final String emKey = ctx.getPackageName();

        if (!entityManagersCache.containsKey(emKey))
        {
            entityManagersCache.put(emKey, new EntityManager(ctx));
        }

        return entityManagersCache.get(emKey);
    }
}
