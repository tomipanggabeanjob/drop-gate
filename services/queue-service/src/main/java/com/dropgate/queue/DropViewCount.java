package com.dropgate.queue;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "drop_view_counts")
public class DropViewCount {

    @Id
    private String dropId;
    private long viewCount;

    protected DropViewCount() {
    }

    public DropViewCount(String dropId, long viewCount) {
        this.dropId = dropId;
        this.viewCount = viewCount;
    }

    public String getDropId() {
        return dropId;
    }

    public long getViewCount() {
        return viewCount;
    }

    public void setViewCount(long viewCount) {
        this.viewCount = viewCount;
    }
}
