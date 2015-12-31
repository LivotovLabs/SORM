package eu.livotov.labs.android.sorm.core.meta;

import android.text.TextUtils;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;

import eu.livotov.labs.android.sorm.annotations.Column;
import eu.livotov.labs.android.sorm.annotations.View;
import eu.livotov.labs.android.sorm.annotations.ViewColumnMap;

/**
 * Created by IntelliJ IDEA.
 * User: dlivotov
 * Date: 11.12.10
 * Time: 17:35
 * To change this template use File | Settings | File Templates.
 */
public class ViewMetadata
{

    Map<String, EntityColumnMetadata> columns = new HashMap<String, EntityColumnMetadata>();
    private Class viewClass;
    private String viewName;
    private String viewQuery;


    public ViewMetadata(Class clazz)
    {
        this.viewClass = clazz;
        loadClassMetadata();
    }

    private void loadClassMetadata()
    {
        View annotation = (View) viewClass.getAnnotation(View.class);

        if (annotation == null)
        {
            throw new RuntimeException(String.format("View class %s is not mapped by @View annotation.", viewClass.getCanonicalName()));
        }

        viewName = annotation.name();
        viewQuery = annotation.query();

        if (TextUtils.isEmpty(viewName))
        {
            viewName = viewClass.getSimpleName().toUpperCase();
        }

        if (TextUtils.isEmpty(viewQuery))
        {
            throw new IllegalArgumentException("You must provide a select query for constructing a sql view using a viewQuery parameter of a @View annotation.");
        }

        loadClassColumns(annotation);
    }

    private void loadClassColumns(final View annotation)
    {
        columns.clear();
        Map<String, String> extraFieldToColumnMap = new HashMap<String, String>();

        if (annotation.columnMap() != null && annotation.columnMap().length > 0)
        {
            for (ViewColumnMap mapEntry : annotation.columnMap())
            {
                extraFieldToColumnMap.put(mapEntry.fieldName().toLowerCase(), mapEntry.columnName());
            }
        }

        loadClassColumns(viewClass, extraFieldToColumnMap);
    }

    private void loadClassColumns(final Class clazz, final Map<String, String> extraFieldToColumnMap)
    {
        loadClassColumnsFor(clazz, extraFieldToColumnMap);

        Class parentClass = clazz.getSuperclass();
        if (parentClass != null)
        {
            loadClassColumns(parentClass, extraFieldToColumnMap);
        }
    }

    private void loadClassColumnsFor(final Class cls, final Map<String, String> extraFieldToColumnMap)
    {
        Field[] classFields = cls.getDeclaredFields();

        for (Field field : classFields)
        {
            Column columnAnnotation = (Column) field.getAnnotation(Column.class);

            if (columnAnnotation != null)
            {
                EntityColumnMetadata meta = new EntityColumnMetadata(this, field);
                columns.put(field.getName(), meta);
            }

            if (extraFieldToColumnMap.containsKey(field.getName().toLowerCase()))
            {
                EntityColumnMetadata meta = new EntityColumnMetadata(this, field, extraFieldToColumnMap.get(field.getName().toLowerCase()));
                columns.put(field.getName(), meta);
            }
        }
    }

    public Class getViewClass()
    {
        return viewClass;
    }

    public Map<String, EntityColumnMetadata> getColumns()
    {
        return columns;
    }

    public EntityColumnMetadata getMappedColumn(Field viewField)
    {
        return getMappedColumn(viewField.getName());
    }

    public EntityColumnMetadata getMappedColumn(String viewFieldName)
    {
        return columns.get(viewFieldName);
    }

    public String getViewName()
    {
        return viewName;
    }

    public String getViewQuery()
    {
        return viewQuery;
    }
}
