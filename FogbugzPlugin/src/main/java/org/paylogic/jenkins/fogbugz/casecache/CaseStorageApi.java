package org.paylogic.jenkins.fogbugz.casecache;

import hudson.Extension;
import hudson.model.RootAction;
import hudson.model.UnprotectedRootAction;
import jenkins.model.Jenkins;
import lombok.extern.java.Log;
import org.kohsuke.stapler.QueryParameter;
import org.paylogic.fogbugz.FogbugzCase;
import org.paylogic.jenkins.fogbugz.FogbugzNotifier;
import org.paylogic.redis.RedisProvider;
import redis.clients.jedis.Jedis;

import java.util.logging.Level;


@Log
@Extension
public class CaseStorageApi implements UnprotectedRootAction {

    public String getIconFileName() {
        return null;
    }

    public String getDisplayName() {
        return "Case storage API. Not meant for end-users.";
    }

    public String getUrlName() {
        return "fbevent";
    }

    public void doIndex(@QueryParameter(required = true) int caseid) {
        log.info("YAAAAY WE GOT A REQUEST!!!! QWOOOOOO (btw caseid = " + Integer.toString(caseid) + ")");
        Jedis redis = new RedisProvider().getConnection();
        try {
            log.info("Fetching case");
            FogbugzCase fbCase = new FogbugzNotifier().getFogbugzCaseManager().getCaseById(caseid);
            log.info("Fetched case");
            // TODO: make split with regex and nicer and fail-proof
            String featureBranch = fbCase.getFeatureBranch().split("#")[1];
            redis.set("cc_" + featureBranch, Integer.toString(fbCase.getId()));
            log.info("Saved featureBranch and caseId to Redis");
        } catch (Exception e) {
            log.log(Level.SEVERE, "Exception during execution of case hook", e);
        } finally {
            redis.disconnect();
        }
    }

    public int getCaseIdForTargetBranch(String targetBranch) {
        Jedis redis = new RedisProvider().getConnection();
        String data = redis.get("cc_" + targetBranch);
        redis.disconnect();
        return Integer.parseInt(data);
    }

    public static CaseStorageApi get() {
        return Jenkins.getInstance().getExtensionList(RootAction.class).get(CaseStorageApi.class);
    }
}