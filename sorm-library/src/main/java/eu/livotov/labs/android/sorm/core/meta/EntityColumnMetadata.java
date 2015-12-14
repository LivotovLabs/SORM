package eu.livotov.labs.android.sorm.core.meta;

import android.text.TextUtils;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

import eu.livotov.labs.android.sorm.annotations.Column;
import eu.livotov.labs.android.sorm.annotations.Id;
import eu.livotov.labs.android.sorm.annotations.Index;
import eu.livotov.labs.android.sorm.annotations.Lob;
import eu.livotov.labs.android.sorm.core.sqlite.SQLiteUtils;

/**
 * Created by IntelliJ IDEA.
 * User: dlivotov
 * Date: 11.12.10
 * Time: 17:37
 * To change this template use File | Settings | File Templates.
 */
public class EntityColumnMetadata
{

    List<FieldIndexMetadata> indexes = new ArrayList<FieldIndexMetadata>();
    private ViewMetadata view;
    private EntityMetadata entity;
    private Field field;
    private String columnName;
    private boolean nullable;
    private String defaultValue;
    private boolean unique;
    private String sqlType;
    private long size = 0;
    private boolean lazy = false;
    private boolean lob;
    private boolean primaryKey;
    private String fulltextIndexFieldName;

    public EntityColumnMetadata(EntityMetadata entity, final Field field)
    {
        this.entity = entity;
        this.view = null;
        this.field = field;
        this.field.setAccessible(true);
        loadEntityColumns();
    }

    private void loadEntityColumns()
    {
        Column columnAnnotation = field.getAnnotation(Column.class);
        Annotation[] otherAnnotations = field.getAnnotations();

        if (columnAnnotation == null)
        {
            throw new RuntimeException(String.format("Field %s of entity %s has no @Column annotation"));
        }

        if (TextUtils.isEmpty(columnAnnotation.name()))
        {
            columnName = field.getName().toUpperCase();
        }
        else
        {
            columnName = columnAnnotation.name();
        }

        nullable = columnAnnotation.nullable();
        unique = columnAnnotation.unique();
        defaultValue = columnAnnotation.defaultValue();
        lazy = columnAnnotation.lazy();

        if (TextUtils.isEmpty(columnAnnotation.type()))
        {
            sqlType = SQLiteUtils.getAutomaticSQLiteTypeForField(field);
        }
        else
        {
            sqlType = columnAnnotation.type();
        }

        size = columnAnnotation.size();

        indexes.clear();

        for (Annotation annotation : otherAnnotations)
        {
            if (annotation.annotationType().equals(Index.class))
            {
                indexes.add(new FieldIndexMetadata(this, (Index) annotation));
                continue;
            }

            if (annotation.annotationType().equals(Lob.class))
            {
                sqlType = SQLiteUtils.SQL_TYPE_TEXT;
                lob = true;
                continue;
            }

            if (annotation.annotationType().equals(Id.class))
            {
                sqlType = SQLiteUtils.SQL_TYPE_INTEGER;
                unique = true;
                primaryKey = true;
                lazy = false;
                fulltextIndexFieldName = null;

                if (!field.getType().equals(Long.TYPE) && !field.getType().equals(Integer.TYPE) && !field.getType().equals(Long.class) && !field.getType().equals(Integer.class))
                {
                    throw new RuntimeException(String.format("Primary key field %s must be either of type Long or " + "Integer.", field.getName()));
                }

                if (lob)
                {
                    throw new RuntimeException(String.format("Primary key field %s cannot be a lob type at the same " + "time.", field.getName()));
                }
                continue;
            }
        }
    }

    public EntityColumnMetadata(ViewMetadata view, final Field field)
    {
        this.entity = null;
        this.view = view;
        this.field = field;
        this.field.setAccessible(true);
        loadEntityColumns();
    }

    public EntityColumnMetadata(final ViewMetadata viewMetadata, final Field field, final String mappedColumnName)
    {
        this.entity = null;
        this.view = view;
        this.field = field;
        this.field.setAccessible(true);
        loadEntityColumns(mappedColumnName);
    }

    private void loadEntityColumns(final String columnName)
    {
        this.columnName = columnName;
        nullable = true;
        unique = false;
        defaultValue = null;
        lazy = false;
        sqlType = SQLiteUtils.getAutomaticSQLiteTypeForField(field);
        size = 0;
        indexes.clear();
        primaryKey = false;
    }

    public EntityMetadata getEntity()
    {
        return entity;
    }

    public ViewMetadata getView()
    {
        return view;
    }

    public Field getField()
    {
        return field;
    }

    public String getColumnName()
    {
        return columnName;
    }

    public boolean isNullable()
    {
        return nullable;
    }

    public boolean isUnique()
    {
        return unique;
    }

    public String getSqlType()
    {
        return sqlType;
    }

    public long getSize()
    {
        return size;
    }

    public boolean isLob()
    {
        return lob;
    }

    public List<FieldIndexMetadata> getIndexes()
    {
        return indexes;
    }

    public boolean isPrimaryKey()
    {
        return primaryKey;
    }

    public String getDefaultValue()
    {
        return defaultValue;
    }

    public String getFulltextIndexFieldName()
    {
        return fulltextIndexFieldName;
    }

    public boolean isLazy()
    {
        return lazy;
    }

    public void setLazy(boolean lazy)
    {
        this.lazy = lazy;
    }
}
