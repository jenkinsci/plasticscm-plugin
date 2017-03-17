package com.codicesoftware.plugins.hudson.model;

import com.codicesoftware.plugins.hudson.util.DateUtil;

import hudson.model.User;
import hudson.scm.ChangeLogSet;
import hudson.scm.EditType;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;

import org.kohsuke.stapler.export.Exported;
import org.kohsuke.stapler.export.ExportedBean;

@ExportedBean(defaultVisibility=999)
public class ChangeSet extends ChangeLogSet.Entry {
    private String version;
    private String repoName;
    private String repoServer;
    private String user;
    private String branch;
    private Date date;
    private String comment;
    private String guid;
    private List<Item> items;
    private String workspaceDir;

    public ChangeSet() {
        this("", "", "", null, "", "", "", "");
    }

    public ChangeSet(
            String version, String repoName, String repoServer, Date date,
            String branch, String user, String comment, String guid) {
        this.version = version;
        this.repoName = repoName;
        this.repoServer = repoServer;
        this.date = date;
        this.comment = comment;
        this.branch = branch;
        this.guid = guid;
        items = new ArrayList<Item>();
        setUser(user);
        this.workspaceDir = "/";
    }
    
    @Override
    public Collection<String> getAffectedPaths() {
        Collection<String> paths = new ArrayList<String>(items.size());
        for (Item item : items) {
            paths.add(item.getPath());
        }
        return paths;
    }

    @Override
    public ChangeLogSet getParent() {
        return (ChangeLogSet)super.getParent();
    }

    @Override
    public User getAuthor() {
        return User.get(user);
    }

    @Override
    public String getMsg() {
        return comment;
    }

    @Exported
    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    @Exported
    public String getRepoName() {
        return repoName;
    }

    public void setRepoName(String repoName) {
        this.repoName = repoName;
    }

    @Exported
    public String getRepoServer() {
        return repoServer;
    }

    public void setRepoServer(String repoServer) {
        this.repoServer = repoServer;
    }

    @Exported
    public String getRepository() {
        return repoName + "@" + repoServer;
    }

    @Exported
    public String getWorkspaceDir() {
        return workspaceDir;
    }

    public void setWorkspaceDir(String workspaceDir) {
        this.workspaceDir = workspaceDir;
    }

    @Exported
    public String getUser() {
        return user;
    }

    public void setUser(String user) {
        this.user = user;
    }

    @Exported
    public Date getDate() {
        return date;
    }

    public void setChangesetDateStr(String dateStr) throws ParseException {
        date = DateUtil.PLASTICSCM_DATETIME_FORMATTER.get().parse(dateStr);
    }

    public void setDateStr(String dateStr) throws ParseException {
        date = DateUtil.PLASTICSCM_DATETIME_FORMATTER.get().parse(dateStr);
    }

    @Exported
    public String getBranch() {
        return branch;
    }

    public void setBranch(String branch) {
        this.branch = branch;
    }

    @Exported
    public String getGuid() {
        return guid;
    }

    public void setGuid(String guid) {
        this.guid = guid;
    }

    @Exported
    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    @Exported
    public List<Item> getItems() {
        return items;
    }

    public void add(ChangeSet.Item item) {
        items.add(item);
        item.setParent(this);
    }

    @Override
    protected void setParent(hudson.scm.ChangeLogSet parent) {
        super.setParent(parent);
    }

    @ExportedBean(defaultVisibility=999)
    public static class Item {
        private String path;
        private ChangeSet parent;
        private String revno;
        private String parentRevno;

        public Item() {
            this("", "", "");
        }

        public Item(String path, String revno, String parentRevno) {
            this.path = path;
            this.revno = revno;
            this.parentRevno = parentRevno;
        }

        public ChangeSet getParent() {
            return parent;
        }

        void setParent(ChangeSet parent) {
            this.parent = parent;
        }

        @Exported
        public String getPath() {
            return path;
        }

        @Exported
        public String getPath(String base) {
            if (path.startsWith(base)) {
                return path.substring(base.length());
            }
            return path;
        }

        public void setPath(String path) {
            this.path = path;
        }

        @Exported
        public String getRevno() {
            return revno;
        }

        public void setRevno(String revno) {
            this.revno = revno;
        }

        @Exported
        public String getParentRevno() {
            return parentRevno;
        }

        public void setParentRevno(String parentRevno) {
            this.parentRevno = parentRevno;
        }

        @Exported
        public EditType getEditType() {
            if (revno.equals("0") && parentRevno.equals("-1")) {
                return EditType.ADD;
            }
            return EditType.EDIT;
        }
    }
}
