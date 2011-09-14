package org.codehaus.mojo.license.header.transformer;

/**
 * Implementation of {@link FileHeaderTransformer} for sql format.
 *
 * @author tchemit <chemit@codelutin.com>
 * @plexus.component role-hint="sql"
 * @since 1.0
 */
public class SqlFileHeaderTransformer
    extends AbstractFileHeaderTransformer
{
    public static final String NAME = "sql";

    public static final String DESCRIPTION = "header transformer with sql comment style";

    public static final String COMMENT_LINE_PREFIX = "-- ";

    public static final String COMMENT_START_TAG = "---";

    public static final String COMMENT_END_TAG = "---";

    public SqlFileHeaderTransformer()
    {
        super( NAME, DESCRIPTION, COMMENT_START_TAG, COMMENT_END_TAG, COMMENT_LINE_PREFIX );
    }

    public String[] getDefaultAcceptedExtensions()
    {
        return new String[]{ NAME };
    }
}
