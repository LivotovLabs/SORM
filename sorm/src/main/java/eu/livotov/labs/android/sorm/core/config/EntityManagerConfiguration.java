package eu.livotov.labs.android.sorm.core.config;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;

import java.lang.reflect.Field;

import eu.livotov.labs.android.sorm.core.SORMException;

/**
 * Created by IntelliJ IDEA.
 * User: dlivotov
 * Date: 11.12.10
 * Time: 20:39
 * To change this template use File | Settings | File Templates.
 */
public class EntityManagerConfiguration
{
    public static final String LastUsedDatabaseVersionCodePreferenceKey = "SORM_last_used_database_version";

    private SchemaEvolutionMode schemaEvolutionMode = SchemaEvolutionMode.UPDATE;

    private DatabaseLocation databaseLocation = DatabaseLocation.PHONE_MEMORY;

    private String memoryCardLocationPrefix = "data";

    private String databaseName;

    private String entitiesPackage;

    private String explicitDatabaseFileLocation;

    private boolean autoclose = false;

    private boolean showSql = false;

    public void append(Context ctx)
    {
        try
        {
            PackageManager pm = ctx.getPackageManager();
            ApplicationInfo ai = pm.getApplicationInfo(ctx.getPackageName(), 128);
            Field[] fields = EntityManagerConfiguration.class.getDeclaredFields();

            for (Field f : fields)
            {
                f.setAccessible(true);
                final String key = "SORM_" + f.getName();

                if (ai != null && ai.metaData != null && ai.metaData.containsKey(key))
                {
                    f.set(this, ai.metaData.get(key));
                }
            }
        }
        catch (Throwable err)
        {
            throw new SORMException(err.getMessage(), err);
        }
    }

    public void append(EntityManagerConfiguration cfg)
    {
        if (cfg == null)
        {
            return;
        }

        Field[] fields = EntityManagerConfiguration.class.getDeclaredFields();

        for (Field f : fields)
        {
            try
            {
                Object fieldValue = f.get(cfg);

                if (fieldValue != null)
                {
                    f.set(this, fieldValue);
                }
            }
            catch (IllegalAccessException accessError)
            {
                accessError.printStackTrace();
            }
        }
    }

    public SchemaEvolutionMode getSchemaEvolutionMode()
    {
        return schemaEvolutionMode;
    }

    public void setSchemaEvolutionMode(final SchemaEvolutionMode schemaEvolutionMode)
    {
        this.schemaEvolutionMode = schemaEvolutionMode;
    }

    public DatabaseLocation getDatabaseLocation()
    {
        return databaseLocation;
    }

    public void setDatabaseLocation(final DatabaseLocation databaseLocation)
    {
        this.databaseLocation = databaseLocation;
    }

    public String getMemoryCardLocationPrefix()
    {
        return memoryCardLocationPrefix;
    }

    public void setMemoryCardLocationPrefix(final String memoryCardLocationPrefix)
    {
        this.memoryCardLocationPrefix = memoryCardLocationPrefix;
    }

    public String getDatabaseName()
    {
        return databaseName;
    }

    public void setDatabaseName(final String databaseName)
    {
        this.databaseName = databaseName;
    }

    public String getExplicitDatabaseFileLocation()
    {
        return explicitDatabaseFileLocation;
    }

    public void setExplicitDatabaseFileLocation(final String explicitDatabaseFileLocation)
    {
        this.explicitDatabaseFileLocation = explicitDatabaseFileLocation;
    }

    public boolean isAutoclose()
    {
        return autoclose;
    }

    public void setAutoclose(final boolean autoclose)
    {
        this.autoclose = autoclose;
    }

    public boolean isShowSql()
    {
        return showSql;
    }

    public void setShowSql(final boolean showSql)
    {
        this.showSql = showSql;
    }

    public String getEntitiesPackage()
    {
        return entitiesPackage;
    }

    public void setEntitiesPackage(final String entitiesPackage)
    {
        this.entitiesPackage = entitiesPackage;
    }
}
