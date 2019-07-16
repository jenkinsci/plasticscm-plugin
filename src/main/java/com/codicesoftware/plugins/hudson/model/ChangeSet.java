package com.codicesoftware.plugins.hudson.model;

import com.codicesoftware.plugins.hudson.util.DateUtil;
import com.codicesoftware.plugins.hudson.util.StringUtil;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import hudson.Util;
import hudson.model.User;
import hudson.scm.ChangeLogSet;
import hudson.scm.EditType;
import org.kohsuke.stapler.export.Exported;
import org.kohsuke.stapler.export.ExportedBean;

import java.io.Serializable;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

@ExportedBean(defaultVisibility=999)
public class ChangeSet extends ChangeLogSet.Entry implements Serializable {
    private static final long serialVersionUID = 1L;

    private String version;
    private String repoName;
    private String repoServer;
    private String user;
    private String branch;
    @SuppressFBWarnings("SE_TRANSIENT_FIELD_NOT_RESTORED")
    private transient ZonedDateTime dateTime;
    private String dateTimeStr;
    private String comment;
    private String guid;
    private ArrayList<Item> items;
    private String workspaceDir;

    @SuppressWarnings("unused")
    public ChangeSet() {
        this("", "", "", "", "", "", "", "");
    }

    public ChangeSet(
            String version, String repoName, String repoServer, String dateTimeStr,
            String branch, String user, String comment, String guid) {
        this.version = version;
        this.repoName = repoName;
        this.repoServer = repoServer;
        setXmlDate(dateTimeStr);
        this.comment = comment;
        this.branch = branch;
        this.guid = guid;
        this.items = new ArrayList<>();
        this.user = user;
        this.workspaceDir = "/";
    }

    /**
     * Copy constructor.
     */
    public ChangeSet(ChangeSet o) {
        this.version = o.version;
        this.repoName = o.repoName;
        this.repoServer = o.repoServer;
        this.dateTime = (dateTime != null) ? ZonedDateTime.from(dateTime) : null;
        this.dateTimeStr = o.dateTimeStr;
        this.comment = o.comment;
        this.branch = o.branch;
        this.guid = o.guid;
        items = new ArrayList<>();
        for (Item i : Util.fixNull(o.items)) {
            items.add(new Item(i));
        }
        this.user = o.user;
        this.workspaceDir = o.workspaceDir;
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
        if (dateTime != null) {
            return dateTime.toInstant().toEpochMilli();
        }
        return -1;
    }

    @Exported
    public String getVersion() {
        return version;
    }

    public int getId() {
        return StringUtil.tryParse(version, -1);
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
    public ZonedDateTime getDateTime() {
        if (dateTime != null) {
            return dateTime;
        }
        return null;
    }

    /**
     * Used for XML date parsing.
     */
    public String getXmlDate() {
        if (Util.fixEmpty(dateTimeStr) != null) {
            return dateTimeStr;
        }
        if (dateTime != null) {
            return DateTimeFormatter.ISO_OFFSET_DATE_TIME.format(dateTime);
        }
        return "";
    }

    /**
     * Used for XML date formatting.
     */
    public void setXmlDate(String dateTimeStr) {
        this.dateTimeStr = dateTimeStr;
        if (Util.fixEmpty(dateTimeStr) != null) {
            ZonedDateTime dateTime = ZonedDateTime.parse(dateTimeStr, DateTimeFormatter.ISO_OFFSET_DATE_TIME);
            this.dateTime = dateTime;
        }
    }

    @Exported
    public String getDateTimeUniversal() {
        if (dateTime != null) {
            return DateUtil.DATETIME_UNIVERSAL_FORMATTER.format(dateTime);
        }
        return "";
    }

    @Exported
    public String getDateTimeLocal() {
        if (dateTime != null) {
            return DateUtil.DATETIME_LOCAL_FORMATTER.format(dateTime);
        }
        return "";
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

    @Exported
    public String getCsetSpec() {
        return String.format("cs:%s@%s@%s", version, repoName, repoServer);
    }

    @ExportedBean(defaultVisibility=999)
    public static class Item implements ChangeLogSet.AffectedFile, Serializable {
        private static final long serialVersionUID = 1L;

        private String path;
        private String revId;
        private String parentRevId;
        private String status;

        @SuppressWarnings("unused")
        public Item() {
            this("", "", "", "");
        }

        public Item(String path, String revId, String parentRevId, String status) {
            setPath(path);
            this.revId = revId;
            this.parentRevId = parentRevId;
            this.status = status;
        }

        /**
         * Copy constructor.
         */
        public Item(Item o) {
            setPath(o.path);
            this.revId = o.revId;
            this.parentRevId = o.parentRevId;
            this.status = o.status;
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
