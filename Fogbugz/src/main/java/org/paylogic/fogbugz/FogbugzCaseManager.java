package org.paylogic.fogbugz;

import lombok.extern.java.Log;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;

/**
 * Manager for FogbugzCase objects. Use this to retrieve, save and create cases.
 */
@Log
public class FogbugzCaseManager {

    private String url;
    private String token;
    private String featureBranchFieldname;
    private String originalBranchFieldname;
    private String targetBranchFieldname;
    private int mergekeeperUserId;

    /**
     * Constructor of FogbugzCaseManager.
     */
    public FogbugzCaseManager(String url, String token, String featureBranchFieldname,
                              String originalBranchFieldname, String targetBranchFieldname,
                              int mergekeeperUserId) {

        this.url = url;
        this.token = token;
        this.featureBranchFieldname = featureBranchFieldname;
        this.originalBranchFieldname = originalBranchFieldname;
        this.targetBranchFieldname = targetBranchFieldname;
        this.mergekeeperUserId = mergekeeperUserId;
    }

    /**
     * Helper method to create basic URL with authentication token in it.
     * @return String with basic URL
     */
    private String getFogbugzUrl() {
        return this.url + "api.asp?token=" + this.token;
    }

    /**
     * Helper method to create API url from Map, with proper encoding.
     * @param params Map with parameters to encode.
     * @return String which represents API URL.
     */
    private String mapToFogbugzUrl(Map<String, String> params) {
        String output = this.getFogbugzUrl();
        for (String key: params.keySet()) {
            try {
                String value = params.get(key);
                if (!value.isEmpty()) {
                    output += "&" + key + "=" + URLEncoder.encode(value, "UTF-8");
                }
            } catch (UnsupportedEncodingException e) {
                FogbugzCaseManager.log.info("Unsupported Encoding Exception, why?");
            }
        }
        FogbugzCaseManager.log.info("Generated URL to send to Fogbugz: " + output);
        return output;
    }

    /**
     * Retrieves a case using the Fogbugz API by caseId.
     * @param id the id of the case to fetch.
     * @return FogbugzCase if all is well, else null.
     */
    public FogbugzCase getCaseById(int id) {
        try {
            HashMap params = new HashMap();  // Hashmap defaults to <String, String>
            params.put("cmd", "search");
            params.put("q", Integer.toString(id));
            params.put("cols", "ixBug,tags,fOpen,sTitle,ixPersonOpenedBy,ixPersonAssignedTo," +
                                      this.getCustomFieldsCSV());  // TODO: milestones.

            URL uri = new URL(this.mapToFogbugzUrl(params));
            HttpURLConnection con = (HttpURLConnection) uri.openConnection();
            DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
            Document doc = dBuilder.parse(con.getInputStream());

            List<String> tags = new ArrayList();
            NodeList tagNodeList = doc.getElementsByTagName("tag");
            if (tagNodeList != null && tagNodeList.getLength() != 0) {
                for (int i = 0; i< tagNodeList.getLength(); i++) {
                    tags.add(tagNodeList.item(i).getTextContent());
                }
            }

            // Construct case object from retrieved data.
            return new FogbugzCase(
                    id,
                    doc.getElementsByTagName("sTitle").item(0).getTextContent(),
                    Integer.parseInt(doc.getElementsByTagName("ixPersonOpenedBy").item(0).getTextContent()),
                    Integer.parseInt(doc.getElementsByTagName("ixPersonAssignedTo").item(0).getTextContent()),
                    tags,
                    Boolean.valueOf(doc.getElementsByTagName("fOpen").item(0).getTextContent()),
                    doc.getElementsByTagName(this.featureBranchFieldname).item(0).getTextContent(),
                    doc.getElementsByTagName(this.originalBranchFieldname).item(0).getTextContent(),
                    doc.getElementsByTagName(this.targetBranchFieldname).item(0).getTextContent()
            );

        } catch (Exception e) {
            FogbugzCaseManager.log.log(Level.SEVERE, "Exception while fetching case " + Integer.toString(id), e);
        }
        return null;
    }

    /**
     * Saves a case to fogbugz using its API.
     * Supports creating new cases, by giving case 0 as caseId.
     * @param fbCase The case to save.
     * @param comment A message to pass for this edit.
     * @return boolean, true if all is well, else false.
     */
    public boolean saveCase(FogbugzCase fbCase, String comment) {
        try {
            HashMap params = new HashMap();
            // If id = 0, create new case.
            if (fbCase.getId() == 0) {
                params.put("cmd", "new");
                params.put("sTitle", fbCase.getTitle());
            } else {
                params.put("cmd", "edit");
                params.put("ixBug", Integer.toString(fbCase.getId()));
            }
            params.put("ixPersonAssignedTo", Integer.toString(fbCase.getAssignedTo()));
            params.put("ixPersonOpenedBy", Integer.toString(fbCase.getOpenedBy()));
            params.put("sTags", fbCase.tagsToCSV());
            params.put(this.featureBranchFieldname, fbCase.getFeatureBranch());
            params.put(this.originalBranchFieldname, fbCase.getOriginalBranch());
            params.put(this.targetBranchFieldname, fbCase.getTargetBranch());
            params.put("sEvent", comment);

            URL uri = new URL(this.mapToFogbugzUrl(params));
            HttpURLConnection con = (HttpURLConnection) uri.openConnection();
            String result = con.getInputStream().toString();
            FogbugzCaseManager.log.info("Fogbugz response got when saving case: " + result);
            // If we got this far, all is probably well.
            // TODO: parse XML that gets returned to check status furreal.
            return true;

        } catch (Exception e) {
            FogbugzCaseManager.log.log(Level.SEVERE, "Exception while creating/saving case " + Integer.toString(fbCase.getId()), e);
        }
        return false;
    }

    /**
     * Additional save method that does not propagate a comment.
     * @param fbCase The case to save.
     * @return boolean, true if all is well, else false.
     */
    public boolean saveCase(FogbugzCase fbCase) {
        return this.saveCase(fbCase, "");
    }

    /**
     * Assign case to mergekeepers user id. Note: does not save case.
     * @param fbCase the case to set assignedTo on.
     * @return modified case.
     */
    public FogbugzCase assignToMergekeepers(FogbugzCase fbCase) {
        fbCase.setAssignedTo(this.mergekeeperUserId);
        return fbCase;
    }

    private String getCustomFieldsCSV() {
        return this.featureBranchFieldname + "," + this.originalBranchFieldname + "," + this.targetBranchFieldname;
    }
}
