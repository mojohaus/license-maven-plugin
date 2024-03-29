package org.codehaus.mojo.license.header;

/*
 * #%L
 * License Maven Plugin
 * %%
 * Copyright (C) 2008 - 2011 CodeLutin, Codehaus, Tony Chemit
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

import org.codehaus.mojo.license.header.transformer.FileHeaderTransformer;
import org.codehaus.mojo.license.model.Copyright;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Implementation of {@link FileHeaderFilter} to update an incoming header.
 *
 * @author tchemit dev@tchemit.fr
 * @since 1.0
 */
public class UpdateFileHeaderFilter extends FileHeaderFilter {
    private static final Logger LOG = LoggerFactory.getLogger(UpdateFileHeaderFilter.class);

    /**
     * Flag sets to {@code true} if description can be updated.
     */
    private boolean updateDescription;

    /**
     * Flag set to {@code true} if license can be updated.
     */
    private boolean updateLicense;

    /**
     * Flag sets to {@code true} if copyright can be updated.
     */
    private boolean updateCopyright;

    @Override
    protected FileHeader getNewHeader(FileHeader oldHeader) {

        FileHeader result = new FileHeader();

        FileHeader newHeader = getFileHeader();

        FileHeaderTransformer transformer = getTransformer();

        boolean modified = false;

        // by default, reuse the old header
        result.setDescription(oldHeader.getDescription());
        result.setCopyright(new Copyright(oldHeader.getCopyright()));
        result.setLicense(oldHeader.getLicense());

        if (isUpdateDescription() && !transformer.isDescriptionEquals(oldHeader, newHeader)) {
            // can update description and it has changed

            LOG.debug(
                    "description has changed from [{}] to [{}]",
                    oldHeader.getDescription(),
                    newHeader.getDescription());

            // description has changed, mark header to be updated
            modified = true;

            // use the new description
            result.setDescription(newHeader.getDescription());
        }

        if (isUpdateCopyright() && !transformer.isCopyrightEquals(oldHeader, newHeader)) {
            // can update copyright and it has changed

            LOG.debug("copyright has changed from [{}] to [{}]", oldHeader.getCopyright(), newHeader.getCopyright());

            // description has changed, mark header to be updated
            modified = true;

            // use the new copyright
            result.setCopyright(new Copyright(newHeader.getCopyright()));
        }

        if (isUpdateLicense() && !transformer.isLicenseEquals(oldHeader, newHeader)) {
            // can update license and it has changed

            LOG.debug("license has changed from [{}] to [{}]", oldHeader.getLicense(), newHeader.getLicense());

            // description has changed, mark header to be updated
            modified = true;

            // use the new license
            result.setLicense(newHeader.getLicense());
        }

        if (!modified) {

            // nothing has to be updated, so return a {@code null} result
            result = null;
        }

        return result;
    }

    private boolean isUpdateCopyright() {
        return updateCopyright;
    }

    public void setUpdateCopyright(boolean updateCopyright) {
        this.updateCopyright = updateCopyright;
    }

    private boolean isUpdateDescription() {
        return updateDescription;
    }

    public void setUpdateDescription(boolean updateDescription) {
        this.updateDescription = updateDescription;
    }

    private boolean isUpdateLicense() {
        return updateLicense;
    }

    public void setUpdateLicense(boolean updateLicense) {
        this.updateLicense = updateLicense;
    }
}
