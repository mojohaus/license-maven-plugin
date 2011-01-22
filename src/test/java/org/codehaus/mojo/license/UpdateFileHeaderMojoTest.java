/*
 * #%L
 * License Maven Plugin
 * 
 * $Id$
 * $HeadURL$
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

package org.codehaus.mojo.license;

import org.junit.Assert;
import org.junit.Test;
import org.nuiton.plugin.AbstractMojoTest;
import org.nuiton.plugin.PluginHelper;

import java.io.File;
import java.util.Set;

import static org.codehaus.mojo.license.UpdateFileHeaderMojo.FileState;

/**
 * Tests the {@link UpdateFileHeaderMojo}.
 *
 * @author tchemit <chemit@codelutin.com>
 * @since 2.1
 */
public class UpdateFileHeaderMojoTest
        extends AbstractMojoTest<UpdateFileHeaderMojo> {


    private Set<File> uptodates;

    private Set<File> updates;

    private Set<File> adds;

    private Set<File> fails;

    private Set<File> ignores;

    private Set<File> process;

    @Override
    protected String getGoalName(String methodName) {
        return "update-file-header";
    }

    @Override
    protected void clearMojo(UpdateFileHeaderMojo mojo) {
        mojo.clear();
    }

    @Override
    protected void setUpMojo(UpdateFileHeaderMojo mojo, File pomFile)
            throws Exception {
        super.setUpMojo(mojo, pomFile);
        mojo.getProject().setBasedir(new File(mojo.getProject().getBasedir(), getMethodName()));
    }

    @Test
    public void addLicense()
            throws Exception {

        File f = getMyBeanFile(getMethodName());

        UpdateFileHeaderMojo mojo = getMojo();
        String content;

//        content = PluginHelper.readAsString(f, mojo.getEncoding());
//
//        // check no header
//        checkPattern(f, content, "Copyright (C) 2010", false);
//        checkPattern(f, content, "Project name : maven-license-plugin-java", false);
//        checkPattern(f, content, "Organization is CodeLutin", false);

        execute(true, mojo);
        Assert.assertEquals(1, process.size());

        if (uptodates == null) {
            Assert.assertEquals(1, adds.size());
        } else {
            Assert.assertEquals(1, uptodates.size());
            Assert.assertNull(adds);
        }
        content = PluginHelper.readAsString(f, mojo.getEncoding());

        // check header
        checkPattern(f, content, "Copyright (C) 2010", true);
        checkPattern(f, content, "Project name : maven-license-plugin-java", true);
        checkPattern(f, content, "Organization is CodeLutin", true);

        // re execute mojo to make sure we are in uptodate
        execute(false, mojo);

        Assert.assertEquals(1, process.size());
        Assert.assertEquals(1, uptodates.size());

        int oldLength = content.length();
        content = PluginHelper.readAsString(f, mojo.getEncoding());
        int newLength = content.length();

        Assert.assertEquals(oldLength, newLength);
    }

    @Test
    public void all()
            throws Exception {

        UpdateFileHeaderMojo mojo = getMojo();

        mojo.setClearAfterOperation(false);
        execute(true, mojo);
        Assert.assertEquals(4, process.size());

        if (uptodates == null) {
            // first invocation
            Assert.assertEquals(1, updates.size());
            Assert.assertEquals(1, adds.size());
            Assert.assertNull(uptodates);
        } else {
            Assert.assertNotNull(uptodates);
            Assert.assertEquals(2, uptodates.size());
            Assert.assertNull(adds);
            Assert.assertNull(updates);
        }
        Assert.assertEquals(1, fails.size());
        Assert.assertEquals(1, ignores.size());

        execute(false, mojo);

        Assert.assertEquals(4, process.size());
        Assert.assertNotNull(uptodates);
        Assert.assertNull(updates);
//        Assert.assertEquals(2, uptodates.size());
        Assert.assertEquals(2, uptodates.size());
        Assert.assertNull(adds);
        Assert.assertNull(updates);
        Assert.assertEquals(1, fails.size());
        Assert.assertEquals(1, ignores.size());

        mojo.setClearAfterOperation(true);
        mojo.execute();
        Assert.assertEquals(0, process.size());
        for (FileState state : FileState.values()) {

            Assert.assertNull(mojo.getFiles(state));
        }
    }

    @Test
    public void updateLicense()
            throws Exception {

        File f = getMyBeanFile(getMethodName());

        UpdateFileHeaderMojo mojo = getMojo();

        String content;

        content = PluginHelper.readAsString(f, mojo.getEncoding());

        // check header
        checkPattern(f, content, "Copyright (C) 2000 Codelutin do NOT update!", true);
        checkPattern(f, content, "License Test :: do NOT update!", true);
//        checkPattern(f, content, "Fake to be removed!", true);

        execute(true, mojo);

        Assert.assertEquals(1, process.size());
        if (uptodates != null) {

            Assert.assertEquals(1, uptodates.size());
            Assert.assertNull(updates);
        } else {
            Assert.assertNull(uptodates);
            Assert.assertEquals(1, updates.size());
        }
        content = PluginHelper.readAsString(f, mojo.getEncoding());

        // check header (description + copyright) does not changed
        checkPattern(f, content, "Copyright (C) 2000 Codelutin do NOT update!", true);
        checkPattern(f, content, "License Test :: do NOT update!", true);

        // check license changed
        checkPattern(f, content, "Fake to be removed!", false);

        execute(false, mojo);

        Assert.assertEquals(1, process.size());
        Assert.assertNotNull(uptodates);
        Assert.assertEquals(1, uptodates.size());
        Assert.assertNull(updates);
//        Assert.assertEquals(1, uptodates.size());
//        Assert.assertNull(updates);

        int oldLength = content.length();
        content = PluginHelper.readAsString(f, mojo.getEncoding());
        int newLength = content.length();

        Assert.assertEquals(oldLength, newLength);
    }

    @Test
    public void failLicense()
            throws Exception {

        File f = getMyBeanFile(getMethodName());

        UpdateFileHeaderMojo mojo = getMojo();

        String content;

        content = PluginHelper.readAsString(f, mojo.getEncoding());

        // check header
        checkPattern(f, content, "Copyright (C) 2000 Codelutin do NOT update!", true);
        checkPattern(f, content, "License Test :: do NOT update!", true);
        checkPattern(f, content, "License content do NOT update!", true);
//        checkPattern(f, content, "Fake to be removed!", true);

        execute(true, mojo);
        Assert.assertEquals(1, process.size());
        Assert.assertEquals(1, fails.size());

        content = PluginHelper.readAsString(f, mojo.getEncoding());

        // check header does not changed
        checkPattern(f, content, "Copyright (C) 2000 Codelutin do NOT update!", true);
        checkPattern(f, content, "License Test :: do NOT update!", true);
        checkPattern(f, content, "License content do NOT update!", true);

        execute(false, mojo);
        Assert.assertEquals(1, process.size());
        Assert.assertEquals(1, fails.size());

        content = PluginHelper.readAsString(f, mojo.getEncoding());

        // check header does not changed
        checkPattern(f, content, "Copyright (C) 2000 Codelutin do NOT update!", true);
        checkPattern(f, content, "License Test :: do NOT update!", true);
        checkPattern(f, content, "License content do NOT update!", true);
    }

    @Test
    public void ignoreLicense()
            throws Exception {

        File f = getMyBeanFile(getMethodName());

        UpdateFileHeaderMojo mojo = getMojo();

        String content;

        content = PluginHelper.readAsString(f, mojo.getEncoding());

        // check header
        checkPattern(f, content, "Copyright (C) 2000 Codelutin Do not update!", true);
        checkPattern(f, content, " * %" + "%Ignore-License", true);
        checkPattern(f, content, "yet another license", true);
        checkPattern(f, content, "NEVER_FINd_ME!", false);

        execute(true, mojo);

        Assert.assertEquals(1, process.size());
        Assert.assertEquals(1, ignores.size());

        content = PluginHelper.readAsString(f, mojo.getEncoding());

        // check header (description + copyright) does not changed
        checkPattern(f, content, "Copyright (C) 2000 Codelutin Do not update!", true);
        checkPattern(f, content, " * %" + "%Ignore-License", true);
        checkPattern(f, content, "yet another license", true);
        checkPattern(f, content, "NEVER_FINd_ME!", false);

        execute(false, mojo);

        Assert.assertEquals(1, process.size());
        Assert.assertEquals(1, ignores.size());

        content = PluginHelper.readAsString(f, mojo.getEncoding());

        // check header (description + copyright) does not changed
        checkPattern(f, content, "Copyright (C) 2000 Codelutin Do not update!", true);
        checkPattern(f, content, " * %" + "%Ignore-License", true);
        checkPattern(f, content, "yet another license", true);
        checkPattern(f, content, "NEVER_FINd_ME!", false);
    }

    @Test
    public void skip()
            throws Exception {

        UpdateFileHeaderMojo mojo = getMojo();
        mojo.execute();
        Assert.assertTrue(mojo.isSkip());
    }

    protected void execute(boolean verbose, UpdateFileHeaderMojo mojo)
            throws Exception {
        mojo.setVerbose(verbose);
        mojo.execute();

        process = mojo.getProcessedFiles();
        uptodates = mojo.getFiles(FileState.uptodate);
        updates = mojo.getFiles(FileState.update);
        adds = mojo.getFiles(FileState.add);
        fails = mojo.getFiles(FileState.fail);
        ignores = mojo.getFiles(FileState.ignore);
    }

    protected File getMyBeanFile(String testName) {
        File f = PluginHelper.getFile(getBasedir(), "target", "test-classes", "org", "codehaus", "mojo", "license",
                                      "updateFileHeaderMojoTest", testName, "src", "MyBean.java");
        return f;
    }


}
