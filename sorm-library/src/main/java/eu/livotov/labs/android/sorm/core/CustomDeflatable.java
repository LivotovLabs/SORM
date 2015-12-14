package eu.livotov.labs.android.sorm.core;

import android.content.ContentValues;

import eu.livotov.labs.android.sorm.core.meta.EntityMetadata;
import eu.livotov.labs.android.sorm.core.meta.ViewMetadata;

/**
 * Created by IntelliJ IDEA.
 * User: dlivotov
 * Date: 11.12.10
 * Time: 20:12
 * To change this template use File | Settings | File Templates.
 */
public interface CustomDeflatable
{
    boolean deflateEntity(ContentValues values, EntityMetadata entityMetadata);

    long deflatePrimaryKeyOnly();

    boolean deflateView(ContentValues values, ViewMetadata viewMetadata);
}
