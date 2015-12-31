package eu.livotov.labs.android.sorm.core;

import android.database.Cursor;

import eu.livotov.labs.android.sorm.core.meta.EntityMetadata;
import eu.livotov.labs.android.sorm.core.meta.ViewMetadata;

/**
 * Created by IntelliJ IDEA.
 * User: dlivotov
 * Date: 11.12.10
 * Time: 20:12
 * To change this template use File | Settings | File Templates.
 */
public interface CustomInflatable
{
    boolean inflateEntity(Cursor sqliteCursor, EntityMetadata entityMetadata);

    boolean inflatePrimaryKeyOnly(long value);

    boolean inflateView(Cursor sqliteCursor, ViewMetadata viewMetadata);
}
