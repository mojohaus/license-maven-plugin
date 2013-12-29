package org.codehaus.mojo.license.header.transformer;

import org.codehaus.plexus.component.annotations.Component;

/**
 * Implementation of {@link FileHeaderTransformer} for mysql format.
 * <p/>
 * <strong>Note:</strong> Using mysql files, you can not just use the {@code sql} file header transformer
 * (see http://jira.codehaus.org/browse/MLICENSE-56) for more informations about the problem.
 *
 * @author tchemit <chemit@codelutin.com>
 * @since 1.4
 */
@Component( role = FileHeaderTransformer.class, hint = "mysql" )
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

