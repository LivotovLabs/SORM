package eu.livotov.labs.android.sorm.core.query;

import android.database.sqlite.SQLiteStatement;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import eu.livotov.labs.android.sorm.EntityManager;
import eu.livotov.labs.android.sorm.core.SORMException;
import eu.livotov.labs.android.sorm.core.meta.EntityMetadata;
import eu.livotov.labs.android.sorm.core.meta.ViewMetadata;

/**
 * Created by IntelliJ IDEA. User: dlivotov Date: 12.12.10 Time: 22:11 To change this template use File | Settings | File Templates.
 */
public final class RawQuery<T>
{

    private EntityManager mEntityManager;
    private EntityMetadata entity;
    private ViewMetadata view;
    private StringBuilder sql;
    private Map<String, Object> parameters = new HashMap<String, Object>();

    public RawQuery(EntityManager em, EntityMetadata metadata, final String sql)
    {
        this.mEntityManager = em;
        this.entity = metadata;
        this.view = null;
        this.sql = new StringBuilder(sql);
    }

    public RawQuery(EntityManager em, ViewMetadata metadata, final String sql)
    {
        this.mEntityManager = em;
        this.view = metadata;
        this.entity = null;
        this.sql = new StringBuilder(sql);
    }

    public RawQuery<T> set(final String key, Object value)
    {
        parameters.put(key, value);
        return this;
    }

    public RawQuery<T> setSql(final String key, String value)
    {
        parameters.put(key, new SqlString(value));
        return this;
    }

    public List<T> execute()
    {
        return mEntityManager.executeRawQuery(entity != null ? entity.getEntityClass() : view.getViewClass(), toSQL());
    }

    public long count()
    {
        SQLiteStatement sql = mEntityManager.getDatabase().compileStatement(toSQL());

        try
        {
            return sql.simpleQueryForLong();
        }
        catch (Throwable err)
        {
            throw new SORMException(err);
        }
        finally
        {
            if (sql != null)
            {
                sql.close();
            }
        }
    }

    public String toSQL()
    {
        return applyParams();
    }

    private String applyParams()
    {
        String q = sql.toString();

        for (String key : parameters.keySet())
        {
            Object value = parameters.get(key);

            if (value == null)
            {
                q = q.replace(keyToken(key), "");
                continue;
            }

            if (value instanceof Boolean)
            {
                q = q.replace(keyToken(key), "1");
                continue;
            }

            if (value instanceof RawQuery.SqlString)
            {
                q = q.replace(keyToken(key), ((SqlString) value).getSql() == null ? "" : ((SqlString) value).getSql());
                continue;
            }

            if (value instanceof RawQuery)
            {
                q = q.replace(keyToken(key), ((RawQuery) value).toSQL());
                continue;
            }

            if (value instanceof Enum)
            {
                q = q.replace(keyToken(key), "'" + ((Enum) value).name() + "'");
                continue;
            }

            q = q.replace(keyToken(key), escapeValue(value));
        }

        return q;
    }

    private String escapeValue(final Object value)
    {
        if (value instanceof String)
        {
            return "'" + ((String) value).replaceAll("'", "\\'") + "'";
        }
        else
        {
            return value.toString();
        }
    }

    private String keyToken(final String key)
    {
        return ":" + key;
    }

    public class SqlString
    {

        private String sql;

        public SqlString(String sql)
        {
            this.sql = sql;
        }

        public String getSql()
        {
            return sql;
        }
    }
}