/*******************************************************************************
 *
 * Copyright (c) 2004-2011 Oracle Corporation.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *
 * Anton Kozak
 *
 *******************************************************************************/
package hudson.scm;

import hudson.scm.ModuleLocation;
import hudson.scm.ParametrizedModuleLocationImpl;
import hudson.scm.ModuleLocationImpl;
import java.util.HashMap;
import java.util.Map;
import org.junit.Test;

import static junit.framework.Assert.assertEquals;

/**
 * Tests for {@link ParametrizedModuleLocationImpl}
 */
public class ParametrizedModuleLocationImplTest {
    @Test
    public void testGet() {
        String cvsRoot = ":pserver:${user}:${password}@${host}:/var/cvsroot";
        String modules = "${modules}";
        String branch = "${branch}";
        String dir = "${dir}";

        Map<String, String> params = new HashMap<String, String>();
        params.put("user", "anonymous");
        params.put("password", "password");
        params.put("host", "localhost");
        params.put("dir", "local");
        params.put("branch", "test");
        params.put("modules", "doc src");
        ModuleLocation parametrizedLocation = new ParametrizedModuleLocationImpl(
            new ModuleLocationImpl(cvsRoot, modules, branch, false, dir), params);
        assertEquals(parametrizedLocation.getCvsroot(), ":pserver:anonymous:password@localhost:/var/cvsroot");
        assertEquals(parametrizedLocation.getModule(), "doc src");
        assertEquals(parametrizedLocation.getBranch(), "test");
        assertEquals(parametrizedLocation.getLocalDir(), "local");

    }
}
