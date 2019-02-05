package org.codehaus.mojo.license.download;

/*
 * #%L
 * License Maven Plugin
 * %%
 * Copyright (C) 2018 Codehaus
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Lesser Public License for more details.
 *
 * You should have received a copy of the GNU General Lesser Public
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/lgpl-3.0.html>.
 * #L%
 */

import java.io.File;

/**
 * A triple consiting of a {@link File}, its SHA-1 checksum and a boolean whether {@link #file}'s name comes from
 * {@link PreferredFileNames} and is thus preferred.
 *
 * @author <a href="https://github.com/ppalaga">Peter Palaga</a>
 * @since 1.18
 */
public class FileNameEntry
{
    public FileNameEntry( File file, boolean preferred, String sha1 )
    {
        super();
        this.file = file;
        this.preferred = preferred;
        this.sha1 = sha1;
    }

    private final File file;

    private final boolean preferred;

    private final String sha1;

    public File getFile()
    {
        return file;
    }

    public boolean isPreferred()
    {
        return preferred;
    }

    public String getSha1()
    {
        return sha1;
    }
}