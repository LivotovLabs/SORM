package eu.livotov.labs.android.sorm.core.sqlite;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.text.TextUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import eu.livotov.labs.android.sorm.core.config.SchemaEvolutionMode;
import eu.livotov.labs.android.sorm.core.log.LoggingUtils;
import eu.livotov.labs.android.sorm.core.meta.EntityColumnMetadata;
import eu.livotov.labs.android.sorm.core.meta.EntityMetadata;
import eu.livotov.labs.android.sorm.core.meta.FieldIndexMetadata;
import eu.livotov.labs.android.sorm.core.meta.ViewMetadata;

/**
 * Created by IntelliJ IDEA.
 * User: dlivotov
 * Date: 11.12.10
 * Time: 21:54
 * To change this template use File | Settings | File Templates.
 */
public class SQLiteSchemaManager
{

    public static Collection<String> generateSchema(SQLiteDatabase db, Collection<EntityMetadata> metadata, Collection<ViewMetadata> views, SchemaEvolutionMode mode)
    {
        List<String> sql = new ArrayList<String>();

        for (EntityMetadata entity : metadata)
        {
            sql.addAll(generateSchemaForEntity(db, entity, mode));
        }

        for (ViewMetadata view : views)
        {
            sql.addAll(generateSchemaForView(db, view, mode));
        }

        return sql;
    }

    public static Collection<String> generateSchemaForEntity(final SQLiteDatabase db, final EntityMetadata entity, SchemaEvolutionMode mode)
    {
        List<String> sql = new ArrayList<String>();

        if (mode == SchemaEvolutionMode.CREATE)
        {
            sql.add(String.format("DROP TABLE IF EXISTS %s", entity.getTableName()));
        }

        if (mode != SchemaEvolutionMode.IGNORE)
        {
            if (mode != SchemaEvolutionMode.CREATE && checkIfTableExists(db, entity.getTableName()))
            {
                generateTableUpgradeStatements(sql, db, entity);
            }
            else
            {
                generateTableCreateStatements(sql, db, entity);
            }
            generateTableIndexesStatements(sql, db, entity);
        }

        sql.addAll(entity.getTableUpgradeStatements());

        return sql;
    }

    public static Collection<String> generateSchemaForView(final SQLiteDatabase db, final ViewMetadata view, SchemaEvolutionMode mode)
    {
        List<String> sql = new ArrayList<String>();

        sql.add(String.format("DROP VIEW IF EXISTS %s", view.getViewName()));
        generateViewCreateStatements(sql, db, view);

        return sql;
    }

    private static boolean checkIfTableExists(SQLiteDatabase db, String tableName)
    {
        try
        {
            Cursor c = db.rawQuery("SELECT name FROM sqlite_master WHERE name=?", new String[]{tableName});
            if (c != null && c.moveToFirst())
            {
                c.close();
                return true;
            }
            else
            {
                if (c != null)
                {
                    c.close();
                }

                return false;
            }
        }
        catch (Throwable err)
        {
            return false;
        }
    }

    private static void generateTableUpgradeStatements(final List<String> sql, final SQLiteDatabase db, final EntityMetadata entity)
    {
        Map<String, EntityColumnMetadata> currentColumns = getCurrentTableColumnsMap(db, entity);

        for (EntityColumnMetadata column : entity.getColumns().values())
        {
            if (!currentColumns.containsKey(column.getColumnName()))
            {
                sql.add(String.format("ALTER TABLE %s ADD COLUMN %s", entity.getTableName(), generateColumnDefinition(db, column)));

            }
        }
    }

    private static void generateTableCreateStatements(final List<String> sql, final SQLiteDatabase db, final EntityMetadata entity)
    {
        StringBuffer buf = new StringBuffer();

        buf.append(String.format("CREATE TABLE %s (", entity.getTableName()));

        int columnCounter = 0;

        for (EntityColumnMetadata column : entity.getColumns().values())
        {
            if (columnCounter > 0)
            {
                buf.append(", ");
            }

            buf.append(generateColumnDefinition(db, column));
            columnCounter++;
        }

        buf.append(");");
        sql.add(buf.toString());
    }

    private static void generateTableIndexesStatements(final List<String> sql, final SQLiteDatabase db, final EntityMetadata entity)
    {
        for (EntityColumnMetadata column : entity.getColumns().values())
        {
            for (FieldIndexMetadata index : column.getIndexes())
            {
                sql.add(String.format("CREATE %s INDEX IF NOT EXISTS %s ON %s (%s)", index.isUnique() ? "UNIQUE" : "", index.getIndexName(), entity.getTableName(), column.getColumnName()));
            }
        }
    }

    private static void generateViewCreateStatements(final List<String> sql, final SQLiteDatabase db, final ViewMetadata view)
    {
        StringBuffer buf = new StringBuffer();
        buf.append(String.format("CREATE VIEW IF NOT EXISTS %s AS %s", view.getViewName(), view.getViewQuery()));

        if (buf.charAt(buf.length() - 1) != ';')
        {
            buf.append(";");
        }

        sql.add(buf.toString());
    }

    private static Map<String, EntityColumnMetadata> getCurrentTableColumnsMap(SQLiteDatabase db, EntityMetadata entity)
    {
        Map<String, EntityColumnMetadata> fieldsMap = new HashMap<String, EntityColumnMetadata>();

        Cursor ti = db.rawQuery(String.format("PRAGMA table_info(%s)", entity.getTableName()), null);

        if (ti.moveToFirst())
        {
            do
            {
                final String tableColumnName = ti.getString(1);
                EntityColumnMetadata meta = entity.getColumn(tableColumnName);

                if (meta != null)
                {
                    fieldsMap.put(tableColumnName, meta);
                }
            } while (ti.moveToNext());
        }

        ti.close();
        return fieldsMap;
    }

    private static String generateColumnDefinition(final SQLiteDatabase db, final EntityColumnMetadata column)
    {
        StringBuffer buf = new StringBuffer();

        buf.append(String.format("%s %s", column.getColumnName(), column.getSqlType()));

        if (column.getSize() > 0)
        {
            buf.append(String.format("(%d)", column.getSize()));
        }

        if (column.isUnique() && !column.isPrimaryKey())
        {
            buf.append(" UNIQUE");
        }

        if (!column.isPrimaryKey() && !column.isNullable())
        {
            buf.append(" NOT NULL");
        }

        if (!column.isPrimaryKey() && !TextUtils.isEmpty(column.getDefaultValue()))
        {
            buf.append(String.format(" DEFAULT '%s'", column.getDefaultValue()));
        }

        if (column.isPrimaryKey())
        {
            buf.append(" PRIMARY KEY AUTOINCREMENT");
        }

        return buf.toString();
    }

    public static void applySchema(SQLiteDatabase db, Collection<String> schema, Collection<EntityMetadata> entities)
    {
        for (String ddl : schema)
        {
            LoggingUtils.i("Executing ORM: " + ddl);
            db.execSQL(ddl);
        }
    }

}
