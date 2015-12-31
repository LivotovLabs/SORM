package eu.livotov.labs.android.sorm.core.meta;

import android.text.TextUtils;

import eu.livotov.labs.android.sorm.annotations.Index;

/**
 * Created by IntelliJ IDEA.
 * User: dlivotov
 * Date: 11.12.10
 * Time: 18:00
 * To change this template use File | Settings | File Templates.
 */
public class FieldIndexMetadata
{

    private EntityColumnMetadata field;

    private String indexName;

    private boolean unique;

    private long size;


    public FieldIndexMetadata(EntityColumnMetadata field, final Index annotation)
    {
        this.field = field;

        if (TextUtils.isEmpty(annotation.name()))
        {
            indexName = field.getColumnName() + "_IDX";
        }
        else
        {
            indexName = annotation.name();
        }

        unique = annotation.unique();
        size = annotation.length();
    }

    public EntityColumnMetadata getField()
    {
        return field;
    }

    public String getIndexName()
    {
        return indexName;
    }

    public boolean isUnique()
    {
        return unique;
    }

    public long getSize()
    {
        return size;
    }
}
