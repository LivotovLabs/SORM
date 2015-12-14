package eu.livotov.labs.android.sorm;

import android.annotation.TargetApi;
import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Build;
import android.text.TextUtils;
import android.util.Log;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import eu.livotov.labs.android.sorm.core.CustomDeflatable;
import eu.livotov.labs.android.sorm.core.CustomInflatable;
import eu.livotov.labs.android.sorm.core.SORMException;
import eu.livotov.labs.android.sorm.core.android.DexUtils;
import eu.livotov.labs.android.sorm.core.config.DatabaseLocation;
import eu.livotov.labs.android.sorm.core.config.EntityManagerConfiguration;
import eu.livotov.labs.android.sorm.core.log.LoggingUtils;
import eu.livotov.labs.android.sorm.core.meta.EntityColumnMetadata;
import eu.livotov.labs.android.sorm.core.meta.EntityMetadata;
import eu.livotov.labs.android.sorm.core.meta.ViewMetadata;
import eu.livotov.labs.android.sorm.core.query.Query;
import eu.livotov.labs.android.sorm.core.query.RawQuery;
import eu.livotov.labs.android.sorm.core.sqlite.SQLiteSchemaManager;
import eu.livotov.labs.android.sorm.core.sqlite.SQLiteUtils;

/**
 * Created by IntelliJ IDEA. User: dlivotov Date: 11.12.10 Time: 17:27 To change this template use File | Settings | File Templates.
 */
public class EntityManager
{

    //Redefine for possible  use crypted db
    public final static int CONFLICT_REPLACE = SQLiteDatabase.CONFLICT_REPLACE;
    public final static int CONFLICT_IGNORE = SQLiteDatabase.CONFLICT_IGNORE;

    private Context context;
    private SQLiteDatabase db;
    private File dbPath;
    private EntityManagerConfiguration configuration = new EntityManagerConfiguration();
    private Map<String, EntityMetadata> entities = new HashMap<String, EntityMetadata>();
    private Map<String, ViewMetadata> views = new HashMap<String, ViewMetadata>();
    private boolean schemaUpdated = false;

    public EntityManager(Context context)
    {
        this.context = context;
        configuration.append(context);
        dbPath = generateDatabasePath();

        open(context);

        if (configuration.isAutoclose())
        {
            close();
        }
    }

    private File generateDatabasePath()
    {
        String dbName = context.getPackageName() + ".db";

        if (!TextUtils.isEmpty(configuration.getDatabaseName()))
        {
            dbName = configuration.getDatabaseName();
        }

        File dbPath = null;

        File oldStyleInternalDbPath = new File(String.format("/data/data/%s/databases/%s", context.getPackageName(), dbName));

        switch (configuration.getDatabaseLocation())
        {
            case PHONE_MEMORY:
            case AUTOMATIC:
                dbPath = isOldStyleInterbalDbPresentAndAccessible(oldStyleInternalDbPath) ? oldStyleInternalDbPath : new File(context.getFilesDir(), dbName);
                break;

            case MEMORY_CARD:
                if (Build.VERSION.SDK_INT >= 8)
                {
                    dbPath = new File(context.getExternalFilesDir(null), dbName);
                }
                else
                {
                    dbPath = new File("/sdcard/" + (TextUtils.isEmpty(configuration.getMemoryCardLocationPrefix()) ? "data" : configuration.getMemoryCardLocationPrefix()) + "/" + context.getPackageName() + "/" + dbName);
                }
                break;

            case EXPLICIT:
                dbPath = new File(configuration.getExplicitDatabaseFileLocation() + File.separator + dbName);
                break;
        }

        return dbPath;
    }

    private synchronized void open(Context ctx)
    {
        PackageManager pm = ctx.getPackageManager();
        PackageInfo pinfo = null;
        ApplicationInfo ai = null;
        int newVersionCode = 0;
        int oldVersionCode = 0;

        try
        {
            ai = pm.getApplicationInfo(ctx.getPackageName(), 128);
            pinfo = pm.getPackageInfo(ai.packageName, 0);
            SharedPreferences prefs = EntityManager.getSharedPreferences(ctx);
            newVersionCode = pinfo.versionCode;
            oldVersionCode = prefs.getInt(EntityManagerConfiguration.LastUsedDatabaseVersionCodePreferenceKey, -1);
        }
        catch (Throwable err)
        {
            Log.e(EntityManager.class.getSimpleName(), "Failed to get application or package info from package manager: " + err.getMessage(), err);
        }

        boolean schemaScanRequired = true;
        final boolean debuggable = (ai == null) || (0 != (ai.flags &= ApplicationInfo.FLAG_DEBUGGABLE));

        if (debuggable)
        {
            schemaScanRequired = true;
            Log.i(EntityManager.class.getSimpleName(), "Application has debug mode set to ON (debuggable=true in Manifest), performing explicit schema rescan and upgrade");
        }
        else
        {
            if (oldVersionCode == newVersionCode)
            {
                Log.i(EntityManager.class.getSimpleName(), "Database and application version codes matched, no schema upgrade required");
                schemaScanRequired = false;
            }
            else
            {
                Log.i(EntityManager.class.getSimpleName(), String.format("Last recorderd schema and app version code: %s , new version code: %s - will perform schema check and upgdare", oldVersionCode, newVersionCode));
                schemaScanRequired = true;
            }
        }

        if (this.entities.isEmpty())
        {
            Collection<Class> entities = DexUtils.findEntityClasses(context, configuration);
            Collection<Class> views = DexUtils.findViewClasses(context, configuration);

            this.entities.clear();
            this.views.clear();

            for (Class clazz : entities)
            {
                this.entities.put(clazz.getCanonicalName(), new EntityMetadata(clazz));
            }

            for (Class clazz : views)
            {
                this.views.put(clazz.getCanonicalName(), new ViewMetadata(clazz));
            }

            try
            {
                dbPath.getParentFile().mkdirs();
            }
            catch (Throwable err)
            {
                Log.e(EntityManager.class.getSimpleName(), err.getMessage());
            }
        }


        try
        {
            Log.i(EntityManager.class.getSimpleName(), "Opening Database");
            db = SQLiteDatabase.openDatabase(dbPath.getAbsolutePath(), null, SQLiteDatabase.OPEN_READWRITE | SQLiteDatabase.CREATE_IF_NECESSARY | SQLiteDatabase.NO_LOCALIZED_COLLATORS | SQLiteDatabase.ENABLE_WRITE_AHEAD_LOGGING);

        }
        catch (Throwable err)
        {
            if (dbPath.exists())
            {
                Log.e(EntityManager.class.getSimpleName(), String.format("Unable to open database %s, probably corrupted file, " + "will rebuild. SQLite message: %s", dbPath.getAbsolutePath(), err.getMessage(), err));
                dbPath.delete();
                db = SQLiteDatabase.openDatabase(dbPath.getAbsolutePath(), null, SQLiteDatabase.OPEN_READWRITE | SQLiteDatabase.CREATE_IF_NECESSARY | SQLiteDatabase.NO_LOCALIZED_COLLATORS | SQLiteDatabase.ENABLE_WRITE_AHEAD_LOGGING);
            }
            else
            {
                Log.e(EntityManager.class.getSimpleName(), String.format("Unable to create database file %s, permission problems ?", dbPath.getAbsolutePath()));
                throw new SORMException(err.getMessage(), err);
            }
        }

        if (!schemaScanRequired)
        {
            return;
        }

        db.execSQL("PRAGMA read_uncommitted = true");
        db.execSQL("PRAGMA synchronous=OFF");

        if (Build.VERSION.SDK_INT >= 11)
        {
            try
            {
                db.enableWriteAheadLogging();
            }
            catch (Exception e)
            {
                Log.e("onOpen", e.getMessage());
            }
        }

        Collection<String> schema = SQLiteSchemaManager.generateSchema(db, this.entities.values(), this.views.values(), configuration.getSchemaEvolutionMode());

        if (schema != null)
        {
            try
            {
                SQLiteSchemaManager.applySchema(db, schema, this.entities.values());
            }
            catch (Throwable err)
            {
                Log.e(EntityManager.class.getSimpleName(), err.getMessage());
                throw new SORMException(err.getMessage(), err);
            }

            if (pinfo != null)
            {
                SharedPreferences prefs = EntityManager.getSharedPreferences(ctx);
                SharedPreferences.Editor editor = prefs.edit();
                editor.putInt(EntityManagerConfiguration.LastUsedDatabaseVersionCodePreferenceKey, pinfo.versionCode);
                editor.commit();
                Log.i(EntityManager.class.getSimpleName(), String.format("Saved new schema and app version code: %s", pinfo.versionCode));
            }
            else
            {
                Log.e(EntityManager.class.getSimpleName(), "PackageInfo is null for some reason, cannot update database version stamp.");
            }
        }
    }

    public void close()
    {
        if (db != null)
        {
            db.close();
            db = null;
        }
    }

    private boolean isOldStyleInterbalDbPresentAndAccessible(File path)
    {
        return path != null && path.exists() && path.canWrite() && path.canRead();
    }

    public static SharedPreferences getSharedPreferences(final Context ctx)
    {
        return ctx.getSharedPreferences(EntityManager.class.getCanonicalName(), Context.MODE_MULTI_PROCESS);
    }

    public EntityManager(Context context, EntityManagerConfiguration configuration)
    {
        this.context = context;
        this.configuration.append(context);
        this.configuration.append(configuration);
        this.dbPath = generateDatabasePath();

        open(context);

        if (configuration.isAutoclose())
        {
            close();
        }
    }

    public EntityManager(Context context, File databasePath, EntityManagerConfiguration configuration)
    {
        this.context = context;
        this.configuration.append(context);
        this.configuration.append(configuration);
        this.dbPath = databasePath;
        this.configuration.setDatabaseLocation(DatabaseLocation.EXPLICIT);

        open(context);

        if (configuration.isAutoclose())
        {
            close();
        }
    }

    public <T> T refresh(Class<T> entity)
    {
        return find(entity, getEntityPrimaryKey(entity), false);
    }

    public <T> T find(Class<T> entity, long id, boolean lazy)
    {
        StringBuffer buf = new StringBuffer();

        EntityMetadata meta = null;
        ViewMetadata view = null;

        if (isEntity(entity))
        {
            meta = entities.get(entity.getCanonicalName());
            buf.append("SELECT ").append(buildSQLSelectColumnsList(meta, lazy)).append(" FROM ").append(meta.getTableName());
        }
        else
        {
            if (isView(entity))
            {
                throwViewNotAllowedException(entity);
            }
            else
            {
                throwNotAnEntityException(entity);
            }
        }

        checkPrimaryKeyRequirement(meta);

        buf.append(" WHERE ").append(meta.getPrimaryKey().getColumnName()).append("=?");

        final List<?> data = executeQuery(meta, view, buf.toString(), "" + id);

        if (data.size() == 0)
        {
            return null;
        }
        else
        {
            T dataObject = (T) data.get(0);
            data.clear();
            return dataObject;
        }
    }

    private long getEntityPrimaryKey(Object entity)
    {
        if (isView(entity.getClass()))
        {
            throwViewNotAllowedException(entity.getClass());
        }

        EntityMetadata entityMetadata = entities.get(entity.getClass().getCanonicalName());

        if (entityMetadata == null)
        {
            throwNotAnEntityException(entity.getClass());
        }

        checkPrimaryKeyRequirement(entityMetadata);


        Long pk = null;

        if (entity instanceof CustomDeflatable)
        {
            pk = Long.valueOf(((CustomDeflatable) entity).deflatePrimaryKeyOnly());
        }

        if (pk != null)
        {
            return pk;
        }

        try
        {
            return (Long) entityMetadata.getPrimaryKey().getField().get(entity);
        }
        catch (Throwable err)
        {
            throw new SORMException(err);
        }
    }

    private boolean isEntity(Class clazz)
    {
        return entities.containsKey(clazz.getCanonicalName());
    }

    private String buildSQLSelectColumnsList(EntityMetadata entityMetadata, boolean lazy)
    {
        if (lazy)
        {
            StringBuffer columnsToLoad = new StringBuffer();
            for (EntityColumnMetadata cm : entityMetadata.getColumns().values())
            {
                if (cm.isLazy())
                {
                    continue;
                }

                if (columnsToLoad.length() > 0)
                {
                    columnsToLoad.append(", ");
                }

                columnsToLoad.append(cm.getColumnName());
            }

            return columnsToLoad.toString();
        }
        else
        {
            return "*";
        }
    }

    private boolean isView(Class clazz)
    {
        return views.containsKey(clazz.getCanonicalName());
    }

    private void throwViewNotAllowedException(final Class entity)
    {
        throw new SORMException(String.format("View %s cannot be used for this operation. Use views only for findAll() or createQuery() operations.", entity));
    }

    private void throwNotAnEntityException(final Class entity)
    {
        throw new SORMException(String.format("Not a view or entity: %s", entity));
    }

    private void checkPrimaryKeyRequirement(final EntityMetadata meta)
    {
        if (meta.getPrimaryKey() == null)
        {
            throw new SORMException(String.format("find/save/delete operations require the " +
                    "entity to have a primary key defined which is false " +
                    "for %s", meta.getEntityClass().getCanonicalName()));
        }
    }

    private List<?> executeQuery(final EntityMetadata entity, final ViewMetadata view, final String sql, final String... args)
    {
        checkEntityManager();

        if (configuration.isShowSql())
        {
            LoggingUtils.i(sql);

            if (args != null && args.length > 0)
            {
                StringBuilder argBuffer = new StringBuilder(args.length * 10 + 16);
                argBuffer.append("[ ");

                for (String argElement : args)
                {
                    argBuffer.append(argElement).append(" , ");
                }
                argBuffer.append(" ]");
                LoggingUtils.i(argBuffer.toString());
            }
        }


        Cursor cur = db.rawQuery(sql, args);
        int count = cur.getCount();
        ArrayList result = new ArrayList(count > 0 ? count : 16);
        while (cur.moveToNext())
        {
            result.add(inflateEntity(entity, view, cur));
        }
        cur.close();

        if (configuration.isAutoclose())
        {
            close();
        }

        return result;
    }

    private void checkEntityManager()
    {
        if (db == null || !db.isOpen())
        {
            open(context);
        }
    }

    protected Object inflateEntity(final EntityMetadata entity, final ViewMetadata view, final Cursor cursor)
    {
        Object object = null;

        if (view == null)
        {
            try
            {
                boolean entityWasInflatedByCustomAdapter = false;
                object = entity.getEntityClass().newInstance();

                if (object instanceof CustomInflatable)
                {
                    entityWasInflatedByCustomAdapter = ((CustomInflatable) object).inflateEntity(cursor, entity);
                }

                if (!entityWasInflatedByCustomAdapter)
                {
                    SQLiteUtils.loadEntityFieldsFromCursor(cursor, entity, null, object);
                }

                return object;
            }
            catch (Throwable err)
            {
                throw new SORMException(err.getMessage(), err);
            }
        }
        else
        {
            try
            {
                boolean viewWasInflatedByCustomAdapter = false;
                object = view.getViewClass().newInstance();

                if (object instanceof CustomInflatable)
                {
                    viewWasInflatedByCustomAdapter = ((CustomInflatable) object).inflateView(cursor, view);
                }

                if (!viewWasInflatedByCustomAdapter)
                {
                    SQLiteUtils.loadEntityFieldsFromCursor(cursor, entity, view, object);
                }

                return object;
            }
            catch (Throwable err)
            {
                throw new SORMException(err.getMessage(), err);
            }

        }
    }

    public <T> T find(Class<T> entity, long id)
    {
        return find(entity, id, false);
    }

    public <T> List<T> findAll(Class<T> entity)
    {
        return findAll(entity, false);
    }

    public <T> List<T> findAll(Class<T> entity, boolean lazy)
    {
        StringBuffer buf = new StringBuffer();

        EntityMetadata meta = null;
        ViewMetadata view = null;

        if (isEntity(entity))
        {
            meta = entities.get(entity.getCanonicalName());
            buf.append("SELECT ").append(buildSQLSelectColumnsList(meta, lazy)).append(" FROM ").append(meta.getTableName());
        }
        else if (isView(entity))
        {
            view = views.get(entity.getCanonicalName());
            buf.append("SELECT * FROM ").append(view.getViewName());
        }
        else
        {
            throwNotAnEntityException(entity);
        }

        return (List<T>) executeQuery(meta, view, buf.toString());
    }

    public <T> T create(T entity)
    {
        return create(entity, SQLiteDatabase.CONFLICT_NONE);
    }

    public <T> T create(T entity, int conflictAlgorithm)
    {

        if (isView(entity.getClass()))
        {
            throwViewNotAllowedException(entity.getClass());

        }
        checkEntityManagerForDataUpdate();

        EntityMetadata entityMetadata = entities.get(entity.getClass().getCanonicalName());

        if (entityMetadata == null)
        {
            throwNotAnEntityException(entity.getClass());
        }

        ContentValues valuesToInsert = buildContentValuesFromEntity(entity, entityMetadata, null);

        if (configuration.isShowSql())
        {
            LoggingUtils.i("INSERT INTO " + entityMetadata.getTableName() + " VALUES: " + valuesToInsert.toString());
        }

        final long id;

        if (conflictAlgorithm == SQLiteDatabase.CONFLICT_NONE)
        {
            id = db.insert(entityMetadata.getTableName(), null, valuesToInsert);
        }
        else
        {
            id = db.insertWithOnConflict(entityMetadata.getTableName(), null, valuesToInsert, conflictAlgorithm);
        }

        if (entityMetadata.getPrimaryKey() != null)
        {
            try
            {
                boolean customInflatedKey = false;

                if (entity instanceof CustomInflatable)
                {
                    customInflatedKey = ((CustomInflatable) entity).inflatePrimaryKeyOnly(id);
                }

                if (!customInflatedKey)
                {
                    entityMetadata.getPrimaryKey().getField().set(entity, Long.valueOf(id));
                }
            }
            catch (Throwable err)
            {
                throw new SORMException(err);
            }
        }
        if (configuration.isAutoclose())
        {
            close();
        }

        return entity;
    }

    public <T> T save(T entity)
    {
        if (isView(entity.getClass()))
        {
            throwViewNotAllowedException(entity.getClass());
        }

        checkEntityManagerForDataUpdate();

        EntityMetadata entityMetadata = entities.get(entity.getClass().getCanonicalName());

        if (entityMetadata == null)
        {
            throwNotAnEntityException(entity.getClass());
        }

        checkPrimaryKeyRequirement(entityMetadata);
        final String whereClause = entityMetadata.getPrimaryKey().getColumnName() + "=?";

        ContentValues valuesToUpdate = buildContentValuesFromEntity(entity, entityMetadata, null);
        long primaryKey = getEntityPrimaryKey(entity);

        if (primaryKey <= 0)
        {
            entity = create(entity);
            primaryKey = getEntityPrimaryKey(entity);
        }
        else
        {
            if (configuration.isShowSql())
            {
                LoggingUtils.i("UPDATE " + entityMetadata.getTableName() + " VALUES: " + valuesToUpdate.toString() + " WHERE " + whereClause + " [@ID: " + primaryKey + " ]");
            }

            db.update(entityMetadata.getTableName(), valuesToUpdate, whereClause, new String[]{"" + primaryKey});
        }

        if (configuration.isAutoclose())
        {
            close();
        }

        return entity;
    }

    public boolean delete(Class entity, long id)
    {
        if (isView(entity))
        {
            throwViewNotAllowedException(entity);
        }

        checkEntityManagerForDataUpdate();

        EntityMetadata entityMetadata = entities.get(entity.getCanonicalName());

        if (entityMetadata == null)
        {
            throwNotAnEntityException(entity);
        }

        checkPrimaryKeyRequirement(entityMetadata);

        final String whereClause = entityMetadata.getPrimaryKey().getColumnName() + "=?";
        int deletedRecs = db.delete(entityMetadata.getTableName(), whereClause, new String[]{"" + id});

        if (configuration.isShowSql())
        {
            LoggingUtils.i("DELETE FROM " + entityMetadata.getTableName() + " WHERE " + whereClause + " [@ID: " + id + " ]");
        }

        if (deletedRecs > 0)
        {
            if (configuration.isAutoclose())
            {
                close();
            }

            return true;
        }
        else
        {
            if (configuration.isAutoclose())
            {
                close();
            }

            return false;
        }
    }

    public boolean delete(Object entity)
    {
        checkEntityManagerForDataUpdate();
        return delete(entity.getClass(), getEntityPrimaryKey(entity));
    }

    public boolean deleteAll(Class entity)
    {
        if (isView(entity))
        {
            throwViewNotAllowedException(entity);
        }

        checkEntityManagerForDataUpdate();

        EntityMetadata entityMetadata = entities.get(entity.getCanonicalName());

        if (entityMetadata == null)
        {
            throwNotAnEntityException(entity);
        }

        if (configuration.isShowSql())
        {
            LoggingUtils.i("DELETE FROM " + entityMetadata.getTableName());
        }

        long deletedRecs = db.delete(entityMetadata.getTableName(), null, null);

        if (deletedRecs > 0)
        {
            if (configuration.isAutoclose())
            {
                close();
            }

            return true;
        }
        else
        {
            if (configuration.isAutoclose())
            {
                close();
            }

            return false;
        }
    }

    private void checkEntityManagerForDataUpdate()
    {
        checkEntityManager();

        if (db.isReadOnly())
        {
            throw new SORMException("Database is read-only state.");
        }
    }

    public <T> List<T> executeRawQuery(final Class<T> entity, final String sql, final String... params)
    {
        final String entityName = entity.getCanonicalName();

        EntityMetadata entityMetadata = entities.get(entityName);
        ViewMetadata viewMetadata = views.get(entityName);

        if (entityMetadata == null && viewMetadata == null)
        {
            throwNotAnEntityException(entity.getClass());
        }

        if (configuration.isShowSql())
        {
            LoggingUtils.i(sql);

            if (params != null && params.length > 0)
            {
                StringBuilder argBuffer = new StringBuilder(params.length * 16 + 4);
                argBuffer.append("[ ");
                for (String argElement : params)
                {
                    argBuffer.append(argElement).append(" , ");
                }
                argBuffer.append(" ]");
                LoggingUtils.i(argBuffer.toString());
            }
        }

        List<T> data = (List<T>) executeQuery(entityMetadata, viewMetadata, sql, params);

        if (configuration.isAutoclose())
        {
            close();
        }

        return data;
    }

    public <T> Query<T> createQuery(final Class<T> entity)
    {
        EntityMetadata entityMetadata = entities.get(entity.getCanonicalName());
        ViewMetadata viewMetadata = views.get(entity.getCanonicalName());

        if (entityMetadata == null && viewMetadata == null)
        {
            throwNotAnEntityException(entity.getClass());
        }

        return new Query(this, entityMetadata, viewMetadata);
    }

    public <T> RawQuery<T> createSQLQuery(final Class<T> entity, final String sql)
    {
        EntityMetadata entityMetadata = entities.get(entity.getCanonicalName());

        if (entityMetadata == null)
        {
            throwNotAnEntityException(entity.getClass());
        }

        return new RawQuery(this, entityMetadata, sql);
    }

    public void beginTransaction()
    {
        if (configuration.isShowSql())
        {
            LoggingUtils.i("BEGIN TRANSACTION");
        }

        if (Build.VERSION.SDK_INT > 10)
        {
            beginTransactionNonExclusive();
        }
        else
        {
            db.beginTransaction();
        }
    }

    @TargetApi(11)
    private void beginTransactionNonExclusive()
    {
        db.beginTransactionNonExclusive();
    }

    public void commit()
    {
        if (configuration.isShowSql())
        {
            LoggingUtils.i("COMMIT TRANSACTION");
        }

        db.setTransactionSuccessful();
        db.endTransaction();

        if (configuration.isAutoclose())
        {
            close();
        }
    }

    public void rollback()
    {
        if (configuration.isShowSql())
        {
            LoggingUtils.i("ROLLBACK TRANSACTION");
        }

        try
        {
            db.endTransaction();
        }
        catch (Throwable ignored)
        {
        }

        if (configuration.isAutoclose())
        {
            close();
        }
    }

    public SQLiteDatabase getDatabase()
    {
        checkEntityManager();
        return db;
    }

    public void backupDatabase(File backupTo)
    {
        close();

        copyFile(dbPath, backupTo);

        if (!configuration.isAutoclose())
        {
            open(context);
        }
    }

    public void copyFile(final File src, final File dst)
    {
        try
        {
            InputStream input = new FileInputStream(src);
            OutputStream output = new FileOutputStream(dst);

            byte[] buffer = new byte[2048];
            int length;

            while ((length = input.read(buffer)) > 0)
            {
                output.write(buffer, 0, length);
            }

            output.flush();
            output.close();
            input.close();
        }
        catch (Throwable err)
        {
            err.printStackTrace();
            throw new RuntimeException(err.getMessage(), err);
        }
    }

    public void restoreBackup(File restoreFrom)
    {
        close();

        copyFile(restoreFrom, dbPath);

        schemaUpdated = false;
        if (!configuration.isAutoclose())
        {
            open(context);
        }
    }

    public Map<String, EntityMetadata> getEntitiesMetadata()
    {
        return entities;
    }

    public Map<String, ViewMetadata> getViewsMetadata()
    {
        return views;
    }

    private ContentValues buildContentValuesFromEntity(Object entity, EntityMetadata entityMetadata, ViewMetadata viewMetadata)
    {
        ContentValues values = new ContentValues();
        boolean customDeflated = false;

        if (entity instanceof CustomDeflatable)
        {
            CustomDeflatable deflator = (CustomDeflatable) entity;

            if (viewMetadata == null)
            {
                customDeflated = deflator.deflateEntity(values, entityMetadata);
            }
            else
            {
                customDeflated = deflator.deflateView(values, viewMetadata);
            }
        }

        if (customDeflated)
        {
            return values;
        }

        if (entityMetadata != null)
        {
            for (EntityColumnMetadata column : entityMetadata.getColumns().values())
            {
                if (column.isPrimaryKey())
                {
                    continue;
                }

                try
                {
                    SQLiteUtils.putColumnDataToContentValues(values, column, column.getField().get(entity));
                }
                catch (Throwable err)
                {
                    throw new SORMException(err);
                }
            }
        }
        else
        {
            throwViewNotAllowedException(viewMetadata.getViewClass());
        }

        return values;
    }

}
