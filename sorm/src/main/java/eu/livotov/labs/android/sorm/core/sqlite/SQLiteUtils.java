package eu.livotov.labs.android.sorm.core.sqlite;

import android.content.ContentValues;
import android.database.Cursor;

import java.lang.reflect.Field;

import eu.livotov.labs.android.sorm.core.SORMException;
import eu.livotov.labs.android.sorm.core.meta.EntityColumnMetadata;
import eu.livotov.labs.android.sorm.core.meta.EntityMetadata;
import eu.livotov.labs.android.sorm.core.meta.ViewMetadata;

/**
 * Created by IntelliJ IDEA.
 * User: dlivotov
 * Date: 11.12.10
 * Time: 18:23
 * To change this template use File | Settings | File Templates.
 */
public class SQLiteUtils
{

    public final static String SQL_TYPE_TEXT = "TEXT";
    public final static String SQL_TYPE_INTEGER = "INTEGER";
    public final static String SQL_TYPE_FLOAT = "FLOAT";

    public final static String getAutomaticSQLiteTypeForField(Field f)
    {
        if (typeIsSQLiteInteger(f.getType()))
        {
            return SQL_TYPE_INTEGER;
        }

        if (typeIsSQLiteFloat(f.getType()))
        {
            return SQL_TYPE_FLOAT;
        }

        return SQL_TYPE_TEXT;
    }

    private static boolean typeIsSQLiteInteger(Class<?> type)
    {
        return (type.equals(Boolean.class)) ||
                (type.equals(Boolean.TYPE)) ||
                (type.equals(java.util.Date.class)) ||
                (type.equals(java.sql.Date.class)) ||
                (type.equals(Integer.class)) ||
                (type.equals(Integer.TYPE)) ||
                (type.equals(Long.class)) ||
                (type.equals(Long.TYPE));
    }

    private static boolean typeIsSQLiteFloat(Class<?> type)
    {
        return (type.equals(Double.class)) || (type.equals(Double.TYPE)) || (type.equals(Float.class)) ||
                (type.equals(Float.TYPE));
    }

    public static void loadEntityFieldsFromCursor(final Cursor c, EntityMetadata entity, ViewMetadata view, Object entityData)
    {
        //List<EntityColumnMetadata> columns = new ArrayList<EntityColumnMetadata>();

        if (view == null)
        {
            for (EntityColumnMetadata column : entity.getColumns().values())
            {
                if (c.getColumnIndex(column.getColumnName()) < 0 && column.isLazy())
                {
                    continue;
                }

                loadSingleField(column.getField(), c, c.getColumnIndex(column.getColumnName()), entityData);
            }
        }
        else
        {
            for (EntityColumnMetadata viewColumn : view.getColumns().values())
            {
                loadSingleField(viewColumn.getField(), c, c.getColumnIndex(viewColumn.getColumnName()), entityData);
            }
        }
    }

    private static void loadSingleField(Field field, Cursor c, int columnIndex, Object entityData)
    {
        final Class fieldType = field.getType();

        try
        {
            if ((fieldType.equals(Boolean.class)) || (fieldType.equals(Boolean.TYPE)))
            {
                field.set(entityData, Boolean.valueOf(c.getInt(columnIndex) != 0));
            }
            else
            {
                if (fieldType.equals(java.util.Date.class))
                {
                    field.set(entityData, new java.util.Date(c.getLong(columnIndex)));
                }
                else
                {
                    if (fieldType.equals(java.sql.Date.class))
                    {
                        field.set(entityData, new java.sql.Date(c.getLong(columnIndex)));
                    }
                    else
                    {
                        if ((fieldType.equals(Double.class)) || (fieldType.equals(Double.TYPE)))
                        {
                            field.set(entityData, Double.valueOf(c.getDouble(columnIndex)));
                        }
                        else
                        {
                            if ((fieldType.equals(Float.class)) || (fieldType.equals(Float.TYPE)))
                            {
                                field.set(entityData, Float.valueOf(c.getFloat(columnIndex)));
                            }
                            else
                            {
                                if ((fieldType.equals(Integer.class)) || (fieldType.equals(Integer.TYPE)))
                                {
                                    field.set(entityData, Integer.valueOf(c.getInt(columnIndex)));
                                }
                                else
                                {
                                    if ((fieldType.equals(Long.class)) || (fieldType.equals(Long.TYPE)))
                                    {
                                        field.set(entityData, Long.valueOf(c.getLong(columnIndex)));
                                    }
                                    else
                                    {
                                        if (fieldType.equals(String.class))
                                        {
                                            field.set(entityData, c.getString(columnIndex));
                                        }
                                        else
                                        {
                                            if (fieldType.isEnum())
                                            {
                                                try
                                                {
                                                    field.set(entityData, Enum.valueOf(fieldType, c.getString(columnIndex)));
                                                }
                                                catch (Throwable err)
                                                {
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        catch (Throwable err)
        {
            throw new SORMException(err);
        }
    }

    public static Long getContentValue(java.util.Date value)
    {
        return (null != value) ? Long.valueOf(value.getTime()) : null;
    }

    public static ContentValues putColumnDataToContentValues(final ContentValues values, final EntityColumnMetadata column, final Object value)
    {
        final Class fieldType = column.getField().getType();
        final String fieldName = column.getColumnName();

        if (value == null)
        {
            return values;
        }

        try
        {
            if ((fieldType.equals(Boolean.class)) || (fieldType.equals(Boolean.TYPE)))
            {
                values.put(fieldName, (Boolean) value);
            }
            else
            {
                if (fieldType.equals(java.util.Date.class))
                {
                    values.put(fieldName, Long.valueOf(((java.util.Date) value).getTime()));
                }
                else
                {
                    if (fieldType.equals(java.sql.Date.class))
                    {
                        values.put(fieldName, Long.valueOf(((java.sql.Date) value).getTime()));
                    }
                    else
                    {
                        if ((fieldType.equals(Double.class)) || (fieldType.equals(Double.TYPE)))
                        {
                            values.put(fieldName, (Double) value);
                        }
                        else
                        {
                            if ((fieldType.equals(Float.class)) || (fieldType.equals(Float.TYPE)))
                            {
                                values.put(fieldName, (Float) value);
                            }
                            else
                            {
                                if ((fieldType.equals(Integer.class)) || (fieldType.equals(Integer.TYPE)))
                                {
                                    values.put(fieldName, (Integer) value);
                                }
                                else
                                {
                                    if ((fieldType.equals(Long.class)) || (fieldType.equals(Long.TYPE)))
                                    {
                                        values.put(fieldName, (Long) value);
                                    }
                                    else
                                    {
                                        if ((fieldType.equals(String.class)) || (fieldType.equals(Character.TYPE)))
                                        {
                                            values.put(fieldName, value.toString());
                                        }
                                        else
                                        {
                                            if (fieldType.isEnum())
                                            {
                                                values.put(fieldName, ((Enum) value).name());
                                            }
                                            else
                                            {
                                                values.put(fieldName, value.toString());
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        catch (Throwable err)
        {
            throw new SORMException(err);
        }

        return values;
    }

    private static boolean typeIsSQLiteString(Class<?> type)
    {
        return (type.equals(String.class)) || (type.equals(Character.TYPE));
    }


}
