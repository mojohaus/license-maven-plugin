package org.codehaus.mojo.license.header.transformer;

/**
 * Implementation of {@link FileHeaderTransformer} for mysql format.
 * <p/>
 * <strong>Note:</strong> Using mysql files, you can not just use the {@code sql} file header transformer
 * (see http://jira.codehaus.org/browse/MLICENSE-56) for more informations about the problem.
 *
 * @author tchemit <chemit@codelutin.com>
 * @plexus.component role-hint="mysql"
 * @since 1.4
 */
public class MySqlFileHeaderTransformer
    extends AbstractFileHeaderTransformer
{

    /**
     * Default constructor.
     */
    public MySqlFileHeaderTransformer()
    {
        super( "mysql", "header transformer with mysql comment style", "-- -", "-- -", "-- " );
    }

    /**
     * {@inheritDoc}
     */
    public String[] getDefaultAcceptedExtensions()
    {
        return new String[]{ "mysql" };
    }
}

