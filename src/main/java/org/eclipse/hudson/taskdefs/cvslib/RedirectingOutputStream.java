
/*
 * Copyright  2002,2004 The Apache Software Foundation
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */
//TODO replace it with Apache Ant 1.8.2 library
package org.eclipse.hudson.taskdefs.cvslib;

import org.apache.tools.ant.taskdefs.LogOutputStream;

/**
 * A dummy stream that just passes stuff to the parser.
 *
 * @version $Revision: 969 $ $Date: 2006-11-05 13:16:01 -0800 (Sun, 05 Nov 2006) $
 */
class RedirectingOutputStream
     extends LogOutputStream {
    private final ChangeLogParser parser;


    /**
     * Creates a new instance of this class.
     *
     * @param parser the parser to which output is sent.
     */
    public RedirectingOutputStream(final ChangeLogParser parser) {
        super(null, 0);
        this.parser = parser;
    }


    /**
     * Logs a line to the log system of ant.
     *
     * @param line the line to log.
     */
    protected void processLine(final String line) {
        parser.stdout(line);
    }
}

