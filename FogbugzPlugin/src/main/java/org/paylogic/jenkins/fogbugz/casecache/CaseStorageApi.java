package org.paylogic.jenkins.fogbugz.casecache;

import com.google.inject.Inject;
import hudson.Extension;
import hudson.model.RootAction;
import hudson.model.UnprotectedRootAction;
import jenkins.model.Jenkins;
import lombok.Setter;
import lombok.extern.java.Log;
import org.jenkinsci.plugins.database.jpa.PersistenceService;
import org.kohsuke.stapler.QueryParameter;
import org.paylogic.fogbugz.FogbugzCase;
import org.paylogic.jenkins.fogbugz.FogbugzNotifier;

import javax.persistence.EntityManager;
import javax.persistence.Query;
import javax.persistence.TypedQuery;
import java.io.IOException;
import java.sql.SQLException;
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
            // TODO: make split with regex and nicer and fail-proof
            cc.featureBranch = fbCase.getFeatureBranch().split("#")[1];
            cc.featureBranchPath = fbCase.getFeatureBranch();
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

    public CachedCase getCaseById(int caseid) throws IOException, SQLException {
        EntityManager em = ps.getGlobalEntityManagerFactory().createEntityManager();
        TypedQuery<CachedCase> query = em.createNamedQuery("CachedCase.findAllCachedCasesById", CachedCase.class);
        query.setParameter("id", caseid);
        List<CachedCase> cc = query.getResultList();
        if (cc.size() > 0) {
            return cc.get(0);
        }

        return null;
    }

    public List<CachedCase> getCasesByFeatureBranch(String featureBranch) throws IOException, SQLException {
        EntityManager em = ps.getGlobalEntityManagerFactory().createEntityManager();
        TypedQuery<CachedCase> query = em.createNamedQuery("CachedCase.findAllCachedCasesByFeatureBranch", CachedCase.class);
        query.setParameter("featureBranch", featureBranch);
        List<CachedCase> cc = query.getResultList();
        return cc;
    }

    public static CaseStorageApi get() {
        return Jenkins.getInstance().getExtensionList(RootAction.class).get(CaseStorageApi.class);
    }
}