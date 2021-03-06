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
 * Kohsuke Kawaguchi, Nikita Levyankov, Anton Kozak
 *
 *******************************************************************************/
package hudson.scm;

import hudson.model.AbstractBuild;
import hudson.model.User;
import hudson.scm.CVSChangeLogSet.CVSChangeLog;
import hudson.util.IOException2;
import java.io.IOException;
import java.util.AbstractList;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.commons.digester3.Digester;
import org.apache.commons.lang.StringUtils;
import org.kohsuke.stapler.export.Exported;
import org.kohsuke.stapler.export.ExportedBean;
import org.xml.sax.SAXException;

/**
 * {@link hudson.scm.ChangeLogSet} for CVS.
 * @author Kohsuke Kawaguchi
 * @author Nikita Levyankov
 */
public final class CVSChangeLogSet extends ChangeLogSet<CVSChangeLog> {
    private static final Logger LOGGER = Logger.getLogger(CVSChangeLogSet.class.getName());

    private List<CVSChangeLog> logs;

    public CVSChangeLogSet(AbstractBuild<?,?> build, List<CVSChangeLog> logs) {
        super(build);
        this.logs = Collections.unmodifiableList(logs);
        for (CVSChangeLog log : logs)
            log.setParent(this);
    }

    /**
     * Returns the read-only list of changes.
     */
    public List<CVSChangeLog> getLogs() {
        return logs;
    }

    @Override
    public String getKind() {
        return "cvs";
    }

    public static CVSChangeLogSet parse( AbstractBuild build, java.io.File f ) throws IOException, SAXException {
        Digester digester = new Digester();
        ArrayList<CVSChangeLog> r = new ArrayList<CVSChangeLog>();
        digester.push(r);

        digester.addObjectCreate("*/entry",CVSChangeLog.class);
        digester.addBeanPropertySetter("*/entry/date");
        digester.addBeanPropertySetter("*/entry/time");
        digester.addBeanPropertySetter("*/entry/author","user");
        digester.addBeanPropertySetter("*/entry/msg");
        digester.addSetNext("*/entry","add");

        digester.addObjectCreate("*/entry/file",File.class);
        digester.addBeanPropertySetter("*/entry/file/name");
        digester.addBeanPropertySetter("*/entry/file/fullName");
        digester.addBeanPropertySetter("*/entry/file/revision");
        digester.addBeanPropertySetter("*/entry/file/prevrevision");
        digester.addCallMethod("*/entry/file/dead","setDead");
        digester.addSetNext("*/entry/file","addFile");

        try {
            digester.parse(f);
        } catch (IOException e) {
            throw new IOException2("Failed to parse "+f,e);
        } catch (SAXException e) {
            throw new IOException2("Failed to parse "+f,e);
        }

        // merge duplicate entries. Ant task somehow seems to report duplicate entries.
        for(int i=r.size()-1; i>=0; i--) {
            CVSChangeLog log = r.get(i);
            boolean merged = false;
            if(!log.isComplete()) {
                r.remove(log);
                continue;
            }
            for(int j=0;j<i;j++) {
                CVSChangeLog c = r.get(j);
                if(c.canBeMergedWith(log)) {
                    c.merge(log);
                    merged = true;
                    break;
                }
            }
            if(merged)
                r.remove(log);
        }

        return new CVSChangeLogSet(build,r);
    }

    /**
     * In-memory representation of CVS Changelog.
     */
    public static class CVSChangeLog extends ChangeLogSet.Entry {
        private String date;
        private String time;
        private User author;
        private String msg;
        private final List<File> files = new ArrayList<File>();

        /**
         * Returns true if all the fields that are supposed to be non-null is present.
         * This is used to make sure the XML file was correct.
         */
        public boolean isComplete() {
            return date!=null && time!=null && msg!=null;
        }

        /**
         * Checks if two {@link CVSChangeLog} entries can be merged.
         * This is to work around the duplicate entry problems.
         */
        public boolean canBeMergedWith(CVSChangeLog that) {
            if(!this.date.equals(that.date))
                return false;
            if(!this.time.equals(that.time))    // TODO: perhaps check this loosely?
                return false;
            if(this.author==null || that.author==null || !this.author.equals(that.author))
                return false;
            if(!this.msg.equals(that.msg))
                return false;
            return true;
        }

        // this is necessary since core and CVS belong to different classloaders.
        protected void setParent(ChangeLogSet parent) {
            LOGGER.log(Level.FINEST, "Set parent " + parent);
            super.setParent(parent);
        }

        public void merge(CVSChangeLog that) {
            this.files.addAll(that.files);
            for (File f : that.files)
                f.parent = this;
        }

        @Exported
        public String getDate() {
            return date;
        }

        public void setDate(String date) {
            this.date = date;
        }

        @Exported
        public String getTime() {
            return time;
        }

        public void setTime(String time) {
            this.time = time;
        }

        /**
         * This method returns Empty string as revision number.
         * CVS stores revision numbers for files in unique way, so it's better to check revision from
         * {@link CVSChangeLogSet.File#getRevision()}
         * @return Empty string
         */
        @Override
        public String getCurrentRevision() {
            return StringUtils.EMPTY;
        }

        @Exported
        public User getAuthor() {
            if(author==null)
                return User.getUnknown();
            return author;
        }

        public Collection<String> getAffectedPaths() {
            return new AbstractList<String>() {
                public String get(int index) {
                    return files.get(index).getName();
                }

                public int size() {
                    return files.size();
                }
            };
        }

        public void setUser(String author) {
            this.author = User.get(author);
        }

        @Exported
        public String getUser() {// digester wants read/write property, even though it never reads. Duh.
            return author.getDisplayName();
        }

        @Exported
        public String getMsg() {
            return msg;
        }

        public void setMsg(String msg) {
            this.msg = msg;
        }

        public void addFile( File f ) {
            f.parent = this;
            files.add(f);
        }

        @Exported
        public List<File> getFiles() {
            return files;
        }
        
        @Override
        public Collection<File> getAffectedFiles() {
            return files;
        }
    }

    @ExportedBean(defaultVisibility=999)
    public static class File implements AffectedFile {
        private String name;
        private String fullName;
        private String revision;
        private String prevrevision;
        private boolean dead;
        private CVSChangeLog parent;

        /**
         * Returns file path with current revision postfix.
         *
         * For ex. foo/bar/zot.c revision: 1.2.3.4
         */
        public String getPath() {
            return getName() + " revision: " + getRevision();
        }

        /**
         * Gets the path name in the CVS repository, like
         * "foo/bar/zot.c"
         *
         * <p>
         * The path is relative to the workspace root.
         */
        @Exported
        public String getName() {
            return name;
        }

        /**
         * Gets the full path name in the CVS repository,
         * like "/module/foo/bar/zot.c"
         *
         * <p>
         * Unlike {@link #getName()}, this method returns
         * a full name from the root of the CVS repository.
         */
        @Exported
        public String getFullName() {
            if(fullName==null) {
                // Hudson < 1.91 doesn't record full path name for CVS,
                // so try to infer that from the current CVS setting.
                // This is an approximation since the config could have changed
                // since this build has done.
                SCM scm = parent.getParent().build.getProject().getScm();
                if(scm instanceof CVSSCM) {
                    CVSSCM cvsscm = (CVSSCM) scm;
                    if(cvsscm.isFlatten()) {
                        fullName = '/'+ StringUtils.join(cvsscm.getAllModules(), " ")+'/'+name;
                    } else {
                        // multi-module set up.
                        fullName = '/'+name;
                    }
                } else {
                    // no way to infer.
                    fullName = '/'+name;
                }
            }
            return fullName;
        }

        public void setFullName(String fullName) {
            this.fullName = fullName;
        }

        /**
         * Gets just the last component of the path, like "zot.c"
         */
        public String getSimpleName() {
            int idx = name.lastIndexOf('/');
            if(idx>0)   return name.substring(idx+1);
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        @Exported
        public String getRevision() {
            return revision;
        }

        public void setRevision(String revision) {
            this.revision = revision;
        }

        @Exported
        public String getPrevrevision() {
            return prevrevision;
        }

        public void setPrevrevision(String prevrevision) {
            this.prevrevision = prevrevision;
        }

        @Exported
        public boolean isDead() {
            return dead;
        }

        public void setDead() {
            this.dead = true;
        }

        @Exported
        public EditType getEditType() {
            // see issue #73. Can't do much better right now
            if(dead)
                return EditType.DELETE;
            if(revision.equals("1.1"))
                return EditType.ADD;
            return EditType.EDIT;
        }

        public CVSChangeLog getParent() {
            return parent;
        }
    }

    /**
     * Represents CVS revision number like "1.5.3.2". Immutable.
     */
    public static class Revision {
        public final int[] numbers;

        public Revision(int[] numbers) {
            this.numbers = numbers;
            assert numbers.length%2==0;
        }

        public Revision(String s) {
            String[] tokens = s.split("\\.");
            numbers = new int[tokens.length];
            for( int i=0; i<tokens.length; i++ )
                numbers[i] = Integer.parseInt(tokens[i]);
            assert numbers.length%2==0;
        }

        /**
         * Returns a new {@link Revision} that represents the previous revision.
         *
         * For example, "1.5"->"1.4", "1.5.2.13"->"1.5.2.12", "1.5.2.1"->"1.5"
         *
         * @return
         *      null if there's no previous version, meaning this is "1.1"
         */
        public Revision getPrevious() {
            if(numbers[numbers.length-1]==1) {
                // x.y.z.1 => x.y
                int[] p = new int[numbers.length-2];
                System.arraycopy(numbers,0,p,0,p.length);
                if(p.length==0)     return null;
                return new Revision(p);
            }

            int[] p = numbers.clone();
            p[p.length-1]--;

            return new Revision(p);
        }

        @Override
        public String toString() {
            StringBuilder buf = new StringBuilder();
            for (int n : numbers) {
                if(buf.length()>0)  buf.append('.');
                buf.append(n);
            }
            return buf.toString();
        }
    }
}
