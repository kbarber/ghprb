package org.jenkinsci.plugins.ghprb;

import java.io.IOException;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.kohsuke.github.GHOrganization;
import org.kohsuke.github.GHUser;
import org.kohsuke.github.GitHub;

/**
 * @author janinko
 */
public class GhprbGitHub {
	private GitHub gh;
	private GhprbTrigger trigger;
	private String gitHubServer;

	public GhprbGitHub(GhprbTrigger trigger, String gitHubServer){
		this.trigger = trigger;
		this.gitHubServer = gitHubServer;
	}

	private void connect() throws IOException{
		String accessToken = trigger.getDescriptor().getAccessToken();
		String serverAPIUrl = trigger.getDescriptor().getServerAPIUrl();
		if(accessToken != null && !accessToken.isEmpty()) {
			try {
				gh = GitHub.connectUsingOAuth(serverAPIUrl, accessToken);
			} catch(IOException e) {
				Logger.getLogger(GhprbRepo.class.getName()).log(Level.SEVERE, "Can''t connect to {0} using oauth", serverAPIUrl);
				throw e;
			}
		} else {
			gh = GitHub.connect(trigger.getDescriptor().getUsername(), null, trigger.getDescriptor().getPassword());
		}
	}

	public GitHub get() throws IOException{
		if(gh == null){
			connect();
		}
		return gh;
	}

	public String getGitHubServer(){
		return gitHubServer;
	}

	public boolean isUserMemberOfOrganization(String organisation, String member){
		try {
			GHOrganization org = get().getOrganization(organisation);
			List<GHUser> members = org.getMembers();
			for(GHUser user : members){
				if(user.getLogin().equals(member)){
					return true;
				}
			}
		} catch (IOException ex) {
			Logger.getLogger(GhprbRepo.class.getName()).log(Level.SEVERE, null, ex);
			return false;
		}
		return false;
	}
}
