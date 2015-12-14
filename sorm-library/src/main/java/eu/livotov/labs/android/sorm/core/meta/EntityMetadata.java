package eu.livotov.labs.android.sorm.core.meta;

import android.database.Cursor;
import android.text.TextUtils;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import eu.livotov.labs.android.sorm.annotations.Column;
import eu.livotov.labs.android.sorm.annotations.Entity;
import eu.livotov.labs.android.sorm.core.SORMException;

/**
 * Created by IntelliJ IDEA.
 * User: dlivotov
 * Date: 11.12.10
 * Time: 17:35
 * To change this template use File | Settings | File Templates.
 */
public class EntityMetadata
{

    private Class entityClass;
    private Map<String, Class> matchingSubclasses = new HashMap<String, Class>();
    private String tableName;
    private boolean inheritance;
    private EntityColumnMetadata primaryKey;
    private Map<String, EntityColumnMetadata> columns = new HashMap<String, EntityColumnMetadata>();
    private Map<String, EntityColumnMetadata> columnsIndexedBySqlColumnName = new HashMap<String, EntityColumnMetadata>();
    private List<String> tableUpgradeStatements = new ArrayList<String>();

    public EntityMetadata(Class clazz)
    {
        this.entityClass = clazz;
        loadClassMetadata();
    }

    private void loadClassMetadata()
    {
        Entity annotation = (Entity) entityClass.getAnnotation(Entity.class);

        if (annotation == null)
        {
            throw new RuntimeException(String.format("Enity %s is not mapped by @Entity annotation.", entityClass.getCanonicalName()));
        }

        if (TextUtils.isEmpty(annotation.table()))
        {
            tableName = entityClass.getSimpleName();
        }
        else
        {
            tableName = annotation.table();
        }

        if (annotation.upgradeStatements() != null && annotation.upgradeStatements().length > 0)
        {
            tableUpgradeStatements.addAll(Arrays.asList(annotation.upgradeStatements()));
        }

        inheritance = annotation.inheritance();

        columns.clear();

        loadClassColumns(entityClass);

        if (columns.isEmpty())
        {
            throw new RuntimeException(String.format("Entity %s has no declated persistent fields.", this.entityClass.getSimpleName()));
        }
    }

    private void loadClassColumns(final Class clazz)
    {
        loadClassColumnsFor(clazz);

        if (!this.entityClass.equals(clazz))
        {
            matchingSubclasses.put(clazz.getCanonicalName(), clazz);
        }

        if (inheritance)
        {
            Class parentClass = clazz.getSuperclass();
            if (parentClass != null)
            {
                loadClassColumns(parentClass);
            }
        }
    }

    private void loadClassColumnsFor(final Class cls)
    {
        Field[] classFields = cls.getDeclaredFields();

        for (Field field : classFields)
        {
            Column columnAnnotation = (Column) field.getAnnotation(Column.class);

            if (columnAnnotation != null && !columns.containsKey(field.getName()))
            {
                EntityColumnMetadata meta = new EntityColumnMetadata(this, field);
                columns.put(field.getName(), meta);
                columnsIndexedBySqlColumnName.put(meta.getColumnName(), meta);

                if (meta.isPrimaryKey())
                {
                    if (primaryKey != null)
                    {
                        throw new SORMException(String.format("Entity %s may contain only one primary key !", entityClass.getCanonicalName()));
                    }

                    primaryKey = meta;
                }
            }
        }
    }

    public Class getEntityClass()
    {
        return entityClass;
    }

    public Class getEntityClassFor(Class cls)
    {
        if (entityClass.equals(cls))
        {
            return entityClass;
        }
        else if (inheritance)
        {
            return matchingSubclasses.get(cls.getCanonicalName());
        }
        else
        {
            return null;
        }
    }

    public String getTableName()
    {
        return tableName;
    }

    public Map<String, EntityColumnMetadata> getColumns()
    {
        return columns;
    }

    public EntityColumnMetadata getColumn(Field field)
    {
        return getColumn(field.getName());
    }

    public EntityColumnMetadata getColumn(String fieldName)
    {
        EntityColumnMetadata meta = columns.get(fieldName);

        if (meta == null)
        {
            meta = columnsIndexedBySqlColumnName.get(fieldName);
        }

        return meta;
    }

    public int getColumnIndex(Cursor cursor, Field field)
    {
        return getColumnIndex(cursor, field.getName());
    }

    public int getColumnIndex(Cursor cursor, String fieldName)
    {
        final EntityColumnMetadata column = getColumn(fieldName);

        if (column != null)
        {
            return cursor.getColumnIndex(column.getColumnName());
        }

        throw new RuntimeException(String.format("Invalid (not mapped or not exists) field name %s for entity %s", fieldName, entityClass.getCanonicalName()));
    }

    public EntityColumnMetadata getPrimaryKey()
    {
        return primaryKey;
    }

    public Map<String, EntityColumnMetadata> getColumnsIndexedBySqlColumnName()
    {
        return columnsIndexedBySqlColumnName;
    }

    public List<String> getTableUpgradeStatements()
    {
        return tableUpgradeStatements;
    }
}
