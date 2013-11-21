package org.paylogic.jenkins.fogbugz.casecache;

import com.google.inject.Inject;
import hudson.Extension;
import hudson.model.AbstractModelObject;
import hudson.model.RootAction;
import hudson.model.UnprotectedRootAction;
import jenkins.model.Jenkins;
import lombok.Setter;
import lombok.extern.java.Log;
import org.jenkinsci.plugins.database.jpa.PersistenceService;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.StaplerRequest;
import org.paylogic.fogbugz.FogbugzCase;
import org.paylogic.jenkins.fogbugz.FogbugzNotifier;

import javax.persistence.EntityManager;
import java.util.List;
import java.util.logging.Level;


@Log
@Extension
public class CaseStorageApi implements UnprotectedRootAction {

    @Inject @Setter private PersistenceService ps;

    public String getIconFileName() {
        return null;
    }

    public String getDisplayName() {
        return null;
    }

    public String getUrlName() {
        return "fbevent";
    }

    public void doIndex(@QueryParameter(required = true) int caseid) {
        log.info("YAAAAY WE GOT A REQUEST!!!! QWOOOOOO (btw caseid = " + Integer.toString(caseid) + ")");
        Jenkins.getInstance().getInjector().injectMembers(this);

        try {
            log.info("Got case id, and converted it successfully to integer");
            log.info("Fetching case");
            FogbugzCase fbCase = new FogbugzNotifier().getFogbugzCaseManager().getCaseById(caseid);
            log.info("Fetched case");

            EntityManager em = ps.getGlobalEntityManagerFactory().createEntityManager();
            log.info("Opened entitymanager");
            em.getTransaction().begin();
            log.info("Started transaction");

            CachedCase cc = new CachedCase();
            CachedCase cases = em.find(CachedCase.class, caseid);
            if (cases != null) {
                log.info("Existing case found.");
                cc = cases;
            }

            log.info("Importing case....");
            cc.id = fbCase.getId();
            cc.title = fbCase.getTitle();
            cc.assignedTo = fbCase.getAssignedTo();
            cc.openedBy = fbCase.getOpenedBy();
            cc.targetBranch = fbCase.getTargetBranch();
            cc.originalBranch = fbCase.getOriginalBranch();
            cc.featureBranch = fbCase.getFeatureBranch();
            cc.tags = fbCase.tagsToCSV();
            log.info("Done importing");

            if (cases != null) {
                log.info("Refreshing existing case");
                em.merge(cc);
                em.refresh(cc);
            } else {
                log.info("Creating new case in DB");
                em.persist(cc);
            }
            log.info("Saved row");
            em.getTransaction().commit();
            log.info("Ended transaction");
            em.close();
            log.info("Closed connection");

        } catch (Exception e) {
            log.log(Level.SEVERE, "Exception during execution of case hook", e);
        }
    }

    public static CaseStorageApi get() {
        return Jenkins.getInstance().getExtensionList(RootAction.class).get(CaseStorageApi.class);
    }
}