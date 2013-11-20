package org.paylogic.jenkins.fogbugz.casecache;

import org.jenkinsci.plugins.database.jpa.GlobalTable;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import java.io.Serializable;


@GlobalTable
@Entity
public class CachedCase implements Serializable {

    public CachedCase() {
    }

    public CachedCase(int id) {
        this.id = id;
    }

    @Id @Column public int id;
    @Column public String title;
    @Column public int assignedTo;
    @Column public int openedBy;
    @Column public String tags;
    @Column public String featureBranch;
    @Column public String originalBranch;
    @Column public String targetBranch;
}
