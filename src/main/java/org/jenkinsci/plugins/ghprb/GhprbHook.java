
package org.jenkinsci.plugins.ghprb;

import hudson.Extension;
import hudson.model.ProminentProjectAction;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;

/**
 * @author janinko
 */
public class GhprbHook implements ProminentProjectAction{

	public String getIconFileName() {
		return "icon.png";
	}

	public String getDisplayName() {
		return "AAAAAAAAAAAAa";
	}

	public String getUrlName() {
		return "ghprbhook";
	}

	public void doIndex(StaplerRequest request, StaplerResponse response){
		System.out.println("ASfaasafss");
		try {
			//gh.parseEventPayload(request.getReader(), GHEventPayload.PullRequest.class);
			response.sendError(406);
		} catch (IOException ex) {
			Logger.getLogger(GhprbHook.class.getName()).log(Level.SEVERE, null, ex);
		}
	}

	public void doBaf(StaplerRequest request, StaplerResponse response){
		System.out.println("ASfaasafss");
		try {
			//gh.parseEventPayload(request.getReader(), GHEventPayload.PullRequest.class);
			response.sendError(406);
		} catch (IOException ex) {
			Logger.getLogger(GhprbHook.class.getName()).log(Level.SEVERE, null, ex);
		}
	}
}
