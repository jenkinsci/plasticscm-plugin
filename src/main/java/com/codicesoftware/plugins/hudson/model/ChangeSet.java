package com.codicesoftware.plugins.hudson.model;

import com.codicesoftware.plugins.hudson.util.DateUtil;
import hudson.model.User;
import hudson.scm.ChangeLogSet;
import hudson.scm.EditType;
import org.kohsuke.stapler.export.Exported;
import org.kohsuke.stapler.export.ExportedBean;

import java.io.Serializable;
import java.text.ParseException;
import java.util.*;

@ExportedBean(defaultVisibility=999)
public class ChangeSet extends ChangeLogSet.Entry implements Serializable {
    private static final long serialVersionUID = 2536283825419567018L;

    private String version;
    private String repoName;
    private String repoServer;
    private String user;
    private String branch;
    private Date date;
    private String comment;
    private String guid;
    private ArrayList<Item> items;
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
        this.date = (date != null) ? new Date(date.getTime()) : null;
        this.comment = comment;
        this.branch = branch;
        this.guid = guid;
        items = new ArrayList<>();
        setUser(user);
        this.workspaceDir = "/";
    }
    
    @Override
    public Collection<String> getAffectedPaths() {
        Collection<String> paths = new ArrayList<>(items.size());
        for (Item item : items) {
            paths.add(item.getPath());
        }
        return paths;
    }

    @Override
    public Collection<? extends ChangeLogSet.AffectedFile> getAffectedFiles() {
        return Collections.unmodifiableCollection(items);
    }

    @Override
    public ChangeLogSet getParent() {
        return super.getParent();
    }

    @Override
    public User getAuthor() {
        return User.get(user);
    }

    @Override
    public String getMsg() {
        return comment;
    }

    @Override
    public String getCommitId() {
        return version;
    }

    @Override
    public long getTimestamp() {
        return date.getTime();
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

    /**
     * @deprecated Returned value does not follow RepSpec format
     */
    @Exported
    @Deprecated
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
        if (date != null) {
            return new Date(date.getTime());
        }
        return null;
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

    public void addItem(ChangeSet.Item item) {
        items.add(item);
    }

    @Override
    protected void setParent(ChangeLogSet parent) {
        super.setParent(parent);
    }

    @ExportedBean(defaultVisibility=999)
    public static class Item implements ChangeLogSet.AffectedFile, Serializable {
        private static final long serialVersionUID = -197448462344216883L;

        private String path;
        private String revId;
        private String parentRevId;
        private String status;

        public Item() {
            this("", "", "", "");
        }

        public Item(String path, String revId, String parentRevId, String status) {
            setPath(path);
            this.revId = revId;
            this.parentRevId = parentRevId;
            this.status = status;
        }

        @Exported
        @Override
        public String getPath() {
            return path;
        }

        @Exported
        public String getPath(String base) {
            if (path.startsWith(base))
                return formatPath(path.substring(base.length()));

            return path;
        }

        public void setPath(String path) {
            this.path = formatPath(path);
        }

        static String formatPath(String path) {
            path = path.replace('\\', '/');

            return path.startsWith("/")
                ? path.replaceFirst("^/*", "")
                : path;
        }

        @Exported
        public String getRevId() {
            return revId;
        }

        public void setRevId(String revId) {
            this.revId = revId;
        }

        @Exported
        public String getParentRevId() {
            return parentRevId;
        }

        public void setParentRevId(String parentRevId) {
            this.parentRevId = parentRevId;
        }

        @Exported
        public String getStatus() {
            return status;
        }

        public void setStatus(String status) {
            this.status = status;
        }

        @Exported
        @Override
        public EditType getEditType() {
            String type = (status != null) ? status.toLowerCase() : "";
            switch (type) {
                case "added":
                    return EditType.ADD;
                case "deleted":
                    return EditType.DELETE;
                default:
                    return EditType.EDIT;
            }
        }
    }
}
