package eu.livotov.labs.android.sorm.core.android;

import android.content.Context;
import android.content.pm.PackageManager;
import android.text.TextUtils;
import android.util.Log;

import java.io.IOException;
import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;

import dalvik.system.DexFile;
import eu.livotov.labs.android.sorm.annotations.Entity;
import eu.livotov.labs.android.sorm.annotations.View;
import eu.livotov.labs.android.sorm.core.config.EntityManagerConfiguration;

/**
 * Created by IntelliJ IDEA.
 * User: dlivotov
 * Date: 11.12.10
 * Time: 22:00
 * To change this template use File | Settings | File Templates.
 */
public class DexUtils
{

    public static List<Class> findEntityClasses(Context ctx, EntityManagerConfiguration cfg)
    {
        return findClasses(ctx, Entity.class, cfg);
    }

    private static List<Class> findClasses(Context context, Class<? extends Annotation> annotatedBy, EntityManagerConfiguration cfg)
    {
        List<Class> entityClasses = new ArrayList<Class>();
        long t1 = System.currentTimeMillis();

        try
        {
            String path = context.getPackageManager().getApplicationInfo(context.getPackageName(), 0).sourceDir;
            String entitiesPackageName = cfg.getEntitiesPackage();
            final String packageName = context.getPackageName();

            if (!TextUtils.isEmpty(entitiesPackageName))
            {
                Log.i(DexUtils.class.getSimpleName(), "Detected specific package name for entities (specified in Manifest): " + entitiesPackageName);
                if (entitiesPackageName.startsWith("."))
                {
                    entitiesPackageName = packageName + entitiesPackageName;
                }
            }
            else
            {
                entitiesPackageName = packageName;
            }

            Log.i(DexUtils.class.getSimpleName(), "Searching for the entities in: " + entitiesPackageName);


            DexFile dexfile = new DexFile(path);
            Enumeration entries = dexfile.entries();

            while (entries.hasMoreElements())
            {
                String name = (String) entries.nextElement();
                Class discoveredClass = null;

                if (name == null || !name.startsWith(entitiesPackageName))
                {
                    continue;
                }

                try
                {
                    discoveredClass = Class.forName(name, true, context.getClass().getClassLoader());
                }
                catch (ClassNotFoundException e)
                {
                    Log.e(DexUtils.class.getSimpleName(), e.getMessage(), e);
                }

                if (discoveredClass == null)
                {
                    continue;
                }

                Annotation ann = discoveredClass.getAnnotation(annotatedBy);

                if (ann != null)
                {
                    entityClasses.add(discoveredClass);
                }
            }

        }
        catch (IOException e)
        {
            Log.e(DexUtils.class.getSimpleName(), e.getMessage(), e);
        }
        catch (PackageManager.NameNotFoundException e)
        {
            Log.e(DexUtils.class.getSimpleName(), e.getMessage(), e);
        }

        long t2 = System.currentTimeMillis();
        Log.i(DexUtils.class.getSimpleName(), String.format("Found %s entities in %s ms", entityClasses.size(), (t2 - t1)));
        return entityClasses;
    }

    public static List<Class> findViewClasses(Context ctx, EntityManagerConfiguration cfg)
    {
        return findClasses(ctx, View.class, cfg);
    }
}
