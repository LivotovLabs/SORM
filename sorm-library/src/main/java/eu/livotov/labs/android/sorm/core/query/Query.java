package eu.livotov.labs.android.sorm.core.query;

import android.content.ContentValues;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteDoneException;
import android.database.sqlite.SQLiteStatement;
import android.text.TextUtils;

import java.util.List;

import eu.livotov.labs.android.sorm.EntityManager;
import eu.livotov.labs.android.sorm.core.SORMException;
import eu.livotov.labs.android.sorm.core.meta.EntityColumnMetadata;
import eu.livotov.labs.android.sorm.core.meta.EntityMetadata;
import eu.livotov.labs.android.sorm.core.meta.ViewMetadata;
import eu.livotov.labs.android.sorm.core.sqlite.SQLiteUtils;

/**
 * Created by IntelliJ IDEA. User: dlivotov Date: 12.12.10 Time: 22:11 To change this template use File | Settings | File Templates.
 */
public final class Query<T>
{

    private EntityManager mEntityManager;
    private ViewMetadata view;
    private EntityMetadata entity;
    private String mSelect = "";
    private Integer mLimit = null;
    private Integer mLimitOffet = null;
    private String mFrom = "";
    private String mJoin = null;
    private StringBuilder mWhere = null;
    private StringBuilder mOrderBy = null;
    private StringBuilder mGroupBy = null;


    //private final static String empty = "";

    public Query(EntityManager em, EntityMetadata metadata)
    {
        this.mEntityManager = em;
        this.entity = metadata;
        this.mSelect = "SELECT * ";
        this.mFrom = String.format("FROM %s ", entity.getTableName());
    }

    public Query(EntityManager em, EntityMetadata entity, ViewMetadata view)
    {
        this.mEntityManager = em;
        this.entity = entity;
        this.view = view;

        if (view != null)
        {
            this.mSelect = "SELECT * ";
            this.mFrom = String.format("FROM %s ", view.getViewName());
        }
        else
        {
            this.mSelect = "SELECT * ";
            this.mFrom = String.format("FROM %s ", entity.getTableName());
        }
    }

    public Query<T> distinct()
    {
        if (!this.mSelect.contains("distinct"))
        {
            this.mSelect = this.mSelect.replace("select ", "select distinct ");
        }
        return this;
    }

    //todo: support joins
    //    public Query innerJoin(Class<? extends Entity<?>> entityType)
    //    {
    //        String joinEntityName = EntityUtil.getTableName(entityType);
    //
    //        return innerJoin(entityType,
    //                         new StringBuilder().append(this.mEntityName).append(".").append(joinEntityName).append("Id").toString(),
    //                         new StringBuilder().append(joinEntityName).append(".Id").toString());
    //    }
    //
    //    public Query outerJoin(Class<? extends Entity<?>> entityType)
    //    {
    //        String joinEntityName = EntityUtil.getTableName(entityType);
    //
    //        return outerJoin(entityType,
    //                         new StringBuilder().append(this.mEntityName).append(".").append(joinEntityName).append("Id").toString(),
    //                         new StringBuilder().append(joinEntityName).append(".Id").toString());
    //    }
    //
    //    public Query innerJoin(Class<? extends Entity<?>> entityType, String column1, String column2)
    //    {
    //        return innerJoin(entityType, column1, column2, Comparison.IsEqualTo);
    //    }
    //
    //    public Query innerJoin(Class<? extends Entity<?>> entityType, String column1, String column2, String comparison)
    //    {
    //        return join(entityType, "INNER", column1, column2, comparison);
    //    }
    //
    //    public Query outerJoin(Class<? extends Entity<?>> entityType, String column1, String column2)
    //    {
    //        return outerJoin(entityType, column1, column2, Comparison.IsEqualTo);
    //    }
    //
    //    public Query outerJoin(Class<? extends Entity<?>> entityType, String column1, String column2, String comparison)
    //    {
    //        return join(entityType, "OUTER", column1, column2, comparison);
    //    }
    //
    //    private Query join(Class<? extends Entity<?>> entityType, String joinType, String column1, String column2, String comparison)
    //    {
    //        String joinEntityName = EntityUtil.getTableName(entityType);
    //
    //        String join = String.format("%s JOIN %s ON %s %s %s ",
    //                                    new Object[]{joinType, joinEntityName, column1, comparison, column2});
    //
    //        this.mJoin = new StringBuilder().append(this.mJoin).append(join).toString();
    //
    //        return this;
    //    }

    public Query<T> between(Object val1, Object val2)
    {
        return addComparison(Comparison.Between, val1, val2);
    }

    private Query<T> addComparison(String comparison, Object val1, Object val2)
    {
        StringBuilder where = prepareWhere();

        where.append(' ');
        where.append(comparison);
        where.append(' ');
        addValue(val1, where);
        where.append(' ');

        if (val2 instanceof Enum) //???
        {
        }
        else
        {
            where.append("AND ");
        }

        addValue(val2, where);
        where.append(' ');

        return this;
    }

    private StringBuilder prepareWhere()
    {
        if (null == mWhere)
        {
            mWhere = new StringBuilder();
        }
        return mWhere;
    }

    protected StringBuilder addValue(Object val, StringBuilder where)
    {
        if ((val instanceof String))
        {
            where.append('\'').append(((String) val).replace("'", "''")).append('\'');
        }
        else
        {
            if ((val instanceof Boolean))
            {
                if ((Boolean) val)
                {
                    where.append('1');
                }
                else
                {
                    where.append('0');
                }
            }
            else
            {
                if (val instanceof Enum)
                {
                    where.append('\'').append(((Enum) val).name()).append('\'');

                }
                else
                {
                    if (((val instanceof java.util.Date)) || ((val instanceof java.sql.Date)))
                    {
                        where.append(Long.valueOf(((java.util.Date) val).getTime()));

                    }
                    else
                    {
                        where.append(val);
                    }
                }
            }
        }
        return where;
    }

    public Query<T> in(Object[] vals)
    {
        return addComparison(Comparison.In, vals);
    }

    private Query<T> addComparison(String comparison, Object[] vals)
    {
        StringBuilder where = prepareWhere();
        where.append(' ').append(comparison).append(' ').append('(');


        for (Object val : vals)
        {
            addValue(val, where);
            where.append(',').append(' ');
        }

        where.setLength(where.length() - 2);
        where.append(')').append(' ');
        return this;
    }

    public Query<T> notInRaw(String sql)
    {
        StringBuilder where = prepareWhere();
        where.append("NOT IN (");
        where.append(sql);
        where.append(' ').append(')').append(' ');
        return this;
    }

    public Query<T> isEqualTo(Object val)
    {
        return addComparison(Comparison.IsEqualTo, val);
    }

    private Query<T> addComparison(String comparison, Object val)
    {
        StringBuilder where = prepareWhere();

        where.append(' ').append(comparison).append(' ');
        addValue(val, where);
        where.append(' ');
        return this;
    }

    public Query<T> appendWhere(String where)
    {
        prepareWhere().append(where);
        return this;
    }

    public Query<T> isNull()
    {
        prepareWhere().append(' ').append(Comparison.IsNull).append(' ');
        return this;

    }

    public Query<T> isNotNull()
    {
        prepareWhere().append(' ').append(Comparison.IsNotNull).append(' ');
        return this;
    }

    public Query<T> isGreaterThan(Object val)
    {
        return addComparison(Comparison.IsGreaterThan, val);
    }

    public Query<T> isGreaterThanOrEqualTo(Object val)
    {
        return addComparison(Comparison.IsGreaterThanOrEqualTo, val);
    }

    public Query<T> isLessThan(Object val)
    {
        return addComparison(Comparison.IsLessThan, val);
    }

    public Query<T> isLessThanOrEqualTo(Object val)
    {
        return addComparison(Comparison.IsLessThanOrEqualTo, val);
    }

    public Query<T> isNotEqualTo(Object val)
    {
        return addComparison(Comparison.IsNotEqualTo, val);
    }

    public Query<T> like(Object val)
    {
        return addComparison(Comparison.Like, val.toString());
    }

    public Query<T> notBetween(Object val1, Object val2)
    {
        return addComparison(Comparison.NotBetween, val1, val2);
    }

    public Query<T> notIn(Object... vals)
    {
        return addComparison(Comparison.NotIn, vals);
    }

    public Query<T> notIn(List<Object> vals)
    {
        return addComparison(Comparison.NotIn, vals.toArray());
    }

    public Query<T> notLike(Object val)
    {
        return addComparison(Comparison.NotLike, val);
    }

    public Query<T> resetWhere()
    {
        if (null != mWhere)
        {
            mWhere.setLength(0);
        }
        return this;
    }

    public Query<T> where(String column)
    {
        return where(column, null);
    }

    public Query<T> where(String column, String function)
    {
        StringBuilder where = prepareWhere();
        if (where.length() > 0)
        {
            where.append("AND");
        }
        where.append(' ');
        if (null != function)
        {
            where.append(function).append('(');
        }
        where.append(entity.getColumn(column).getColumnName());
        if (null != function)
        {
            where.append(')');
        }
        where.append(' ');
        return this;
    }

    public Query<T> and(String column)
    {
        return where(column, null);
    }

    public Query<T> andWithOpenBracket(String column)
    {
        return whereWithAndPlusOpenBracket(column, null);
    }

    protected Query<T> whereWithAndPlusOpenBracket(String column, String function)
    {
        StringBuilder where = prepareWhere();
        if (where.length() > 0)
        {
            where.append("AND (");
        }
        where.append(' ');
        if (null != function)
        {
            where.append(function).append('(');
        }
        where.append(entity.getColumn(column).getColumnName());
        if (null != function)
        {
            where.append(')');
        }
        where.append(' ');
        return this;
    }

    public Query<T> and(String column, String function)
    {
        return where(column, function);
    }

    public Query<T> or(String column)
    {
        StringBuilder where = prepareWhere();
        if (where.length() > 0)
        {
            where.append("OR");
        }
        where.append(' ').append(entity.getColumn(column).getColumnName()).append(' ');
        return this;
    }

    public Query<T> closeBracket()
    {
        StringBuilder where = prepareWhere();
        if (where.length() > 0)
        {
            where.append(")");
        }
        return this;
    }

    public Query<T> and()
    {
        StringBuilder where = prepareWhere();
        if (where.length() > 0)
        {
            where.append(" AND ");
        }
        return this;
    }

    public Query<T> or()
    {
        StringBuilder where = prepareWhere();
        if (where.length() > 0)
        {
            where.append(" OR ");
        }
        return this;
    }

    public Query<T> leftBracket()
    {
        StringBuilder where = prepareWhere();
        if (where.length() > 0)
        {
            where.append(" ( ");
        }
        return this;
    }

    public Query<T> rightBracket()
    {
        StringBuilder where = prepareWhere();
        if (where.length() > 0)
        {
            where.append(" ) ");
        }
        return this;
    }

    public Query<T> column(String column)
    {
        StringBuilder where = prepareWhere();
        where.append(' ').append(entity.getColumn(column).getColumnName()).append(' ');
        return this;
    }

    public Query<T> groupBy(String field)
    {
        if (field == null)
        {
            field = "";
        }

        if (null == mGroupBy)
        {
            mGroupBy = new StringBuilder();
        }
        else
        {
            mGroupBy.append(", ");
        }
        mGroupBy.append(entity.getColumn(field).getColumnName());
        return this;
    }

    public Query<T> orderBy(String field, String direction)
    {
        if (field == null)
        {
            field = "";
        }

        if (null == mOrderBy)
        {
            mOrderBy = new StringBuilder();
        }
        else
        {
            mOrderBy.append(',');
        }

        mOrderBy.append(entity.getColumn(field).getColumnName()).append(' ').append(direction).append(' ');
        return this;
    }

    public Query<T> limit(int count, int offset)
    {
        this.mLimit = Integer.valueOf(count);
        this.mLimitOffet = Integer.valueOf(offset);
        return this;
    }

    public long loadSum(String field)
    {
        if (TextUtils.isEmpty(field))
        {
            throw new IllegalArgumentException();
        }

        String sumField = entity.getColumn(field).getColumnName();
        if (TextUtils.isEmpty(field))
        {
            throw new IllegalArgumentException();
        }

        SQLiteDatabase db = this.mEntityManager.getDatabase();
        String oldSelect = this.mSelect;
        this.mSelect = "SELECT SUM(" + sumField + ") ";
        SQLiteStatement sql = db.compileStatement(getQueryString(false));
        this.mSelect = oldSelect;
        long count = sql.simpleQueryForLong();
        sql.close();

        return count;
    }

    private String getQueryString(boolean lazy)
    {
        String oldSelect = mSelect;

        StringBuilder sb = new StringBuilder(32);

        if (lazy && view == null)
        {
            StringBuilder columnsToLoad = sb;
            sb.append("SELECT ");
            for (EntityColumnMetadata cm : entity.getColumns().values())
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
            sb.append(' ');
            mSelect = sb.toString();
        }

        if (0 == sb.length())
        {
            sb.append(mSelect);
        }

        sb.append(mFrom);
        if (null != mJoin)
        {
            sb.append(mJoin);
        }

        if (null != mWhere)
        {
            sb.append("WHERE");
            sb.append(mWhere);
        }
        if (null != mGroupBy)
        {
            sb.append("GROUP BY ");
            sb.append(mGroupBy);
        }
        if (null != mOrderBy)
        {
            sb.append(" ORDER BY ").append(mOrderBy);
        }
        if (null != mLimit)
        {
            sb.append("LIMIT ").append(mLimit);
            if (null != mLimitOffet)
            {
                sb.append(" OFFSET ").append(mLimitOffet);
            }
        }

        String sql = sb.toString();
        mSelect = oldSelect;
        return sql;
    }

    public long loadMax(String field, long defaultWhenNoRows)
    {
        if (TextUtils.isEmpty(field))
        {
            throw new IllegalArgumentException();
        }

        String sumField = entity.getColumn(field).getColumnName();
        if (TextUtils.isEmpty(field))
        {
            throw new IllegalArgumentException();
        }

        SQLiteDatabase db = this.mEntityManager.getDatabase();
        String oldSelect = this.mSelect;

        this.mSelect = "SELECT MAX(" + sumField + ") ";
        SQLiteStatement sql = db.compileStatement(getQueryString(false));
        this.mSelect = oldSelect;
        try
        {
            long max = sql.simpleQueryForLong();
            return max;
        }
        catch (SQLiteDoneException ex) //no data
        {
            return defaultWhenNoRows;
        }
        finally
        {
            sql.close();
        }
    }

    public long loadCount()
    {
        SQLiteDatabase db = this.mEntityManager.getDatabase();
        String oldSelect = this.mSelect;
        this.mSelect = "SELECT COUNT(*) ";
        SQLiteStatement sql = db.compileStatement(getQueryString(false));
        this.mSelect = oldSelect;
        long count = sql.simpleQueryForLong();
        sql.close();

        return count;
    }

    public T loadSingle()
    {
        Integer oldLimit = this.mLimit;
        Integer oldLimitOffset = this.mLimitOffet;
        limit(1);

        List entities = load();

        this.mLimit = oldLimit;
        this.mLimitOffet = oldLimitOffset;


        if (entities.size() > 0)
        {
            return (T) entities.get(0);
        }

        return null;
    }

    public Query<T> limit(int count)
    {
        this.mLimit = Integer.valueOf(count);
        return this;
    }

    public List<T> load()
    {
        return load(false);
    }

    public List<T> load(boolean lazy)
    {
        return mEntityManager.executeRawQuery(view == null ? entity.getEntityClass() : view.getViewClass(), getQueryString(lazy));
    }

    public int delete()
    {
        int deletedCount = 0;
        SQLiteDatabase db = this.mEntityManager.getDatabase();

        try
        {
            db.beginTransaction();
            deletedCount = db.delete(entity.getTableName(), (null != mWhere) ? mWhere.toString() : null, null);
            db.setTransactionSuccessful();
        }
        finally
        {
            db.endTransaction();
        }

        return deletedCount;
    }

    public int update(Object... filedNamesAndValues)
    {
        int updatedCount = 0;

        if (entity == null)
        {
            throw new SORMException("Query.update() is not supported for views. Please use for entities only.");
        }

        SQLiteDatabase db = this.mEntityManager.getDatabase();
        ContentValues content = getContentValues(filedNamesAndValues);

        try
        {
            db.beginTransaction();
            updatedCount = db.update(entity.getTableName(), content, (null != mWhere) ? mWhere.toString() : null, null);
            db.setTransactionSuccessful();
        }
        finally
        {
            db.endTransaction();
        }

        return updatedCount;
    }

    public ContentValues getContentValues(Object... filedNamesAndValues)
    {
        ContentValues content = new ContentValues();

        for (int i = 0; i < filedNamesAndValues.length; i += 2)
        {
            final String column = filedNamesAndValues[i].toString();
            final Object value = filedNamesAndValues[i + 1];
            final EntityColumnMetadata columnModel = entity.getColumn(column);
            SQLiteUtils.putColumnDataToContentValues(content, columnModel, value);
        }
        return content;
    }

    public int update(ContentValues content, String[] whereArgs)
    {
        int updatedCount = 0;

        if (entity == null)
        {
            throw new SORMException("Query.update() is not supported for views. Please use for entities only.");
        }

        SQLiteDatabase db = this.mEntityManager.getDatabase();

        try
        {
            db.beginTransaction();
            updatedCount = db.update(entity.getTableName(), content, (null != mWhere) ? mWhere.toString() : null, whereArgs);
            db.setTransactionSuccessful();
        }
        finally
        {
            db.endTransaction();
        }

        return updatedCount;
    }

    public EntityMetadata getEntity()
    {
        return entity;
    }

}