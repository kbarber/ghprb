package org.jenkinsci.plugins.ghprb;

import hudson.model.AbstractBuild;
import java.io.IOException;
import java.net.URL;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import jenkins.model.Jenkins;
import org.kohsuke.github.GHCommitState;
import org.kohsuke.github.GHEvent;
import org.kohsuke.github.GHIssueState;
import org.kohsuke.github.GHPullRequest;
import org.kohsuke.github.GHRepository;

/**
 * @author Honza Brázdil <jbrazdil@redhat.com>
 */
public class GhprbRepo {
	private final String reponame;

	private Map<Integer,GhprbPullRequest> pulls;

	private GhprbGitHub gh;
	private GHRepository repo;
	private GhprbHelper helper;

	public GhprbRepo(GhprbGitHub gh,
	                 String user,
	                 String repository,
	                 GhprbHelper helper,
	                 Map<Integer,GhprbPullRequest> pulls){
		this.gh = gh;
		reponame = user + "/" + repository;
		this.helper = helper;
		this.pulls = pulls;
	}

	public void init(){
		for(GhprbPullRequest pull : pulls.values()){
			pull.init(helper,this);
		}
	}

	private boolean checkState(){
		if(repo == null){
			try {
				repo = gh.get().getRepository(reponame);
			} catch (IOException ex) {
				Logger.getLogger(GhprbRepo.class.getName()).log(Level.SEVERE, "Could not retrieve repo named " + reponame + " (Do you have properly set 'GitHub project' field in job configuration?)", ex);
				return false;
			}
		}
		return true;
	}

	public void check(){
		if(!checkState()) return;

		List<GHPullRequest> prs;
		try {
			prs = repo.getPullRequests(GHIssueState.OPEN);
		} catch (IOException ex) {
			Logger.getLogger(GhprbRepo.class.getName()).log(Level.SEVERE, "Could not retrieve pull requests.", ex);
			return;
		}
		Set<Integer> closedPulls = new HashSet<Integer>(pulls.keySet());

		for(GHPullRequest pr : prs){
			check(pr);
			closedPulls.remove(pr.getNumber());
		}

		removeClosed(closedPulls, pulls);
	}

	private void check(GHPullRequest pr){
			Integer id = pr.getNumber();
			GhprbPullRequest pull;
			if(pulls.containsKey(id)){
				pull = pulls.get(id);
			}else{
				pull = new GhprbPullRequest(pr);
				pull.init(helper, this);
				pulls.put(id, pull);
			}
			pull.check(pr);
	}

	private void removeClosed(Set<Integer> closedPulls, Map<Integer,GhprbPullRequest> pulls) {
		if(closedPulls.isEmpty()) return;

		for(Integer id : closedPulls){
			pulls.remove(id);
		}
	}

	public void createCommitStatus(AbstractBuild<?,?> build, GHCommitState state, String message, int id){
		String sha1 = build.getCause(GhprbCause.class).getCommit();
		createCommitStatus(sha1, state, Jenkins.getInstance().getRootUrl() + build.getUrl(), message, id);
	}

	public void createCommitStatus(String sha1, GHCommitState state, String url, String message, int id) {
		Logger.getLogger(GhprbRepo.class.getName()).log(Level.INFO, "Setting status of {0} to {1} with url {2} and message: {3}", new Object[]{sha1, state, url, message});
		try {
			repo.createCommitStatus(sha1, state, url, message);
		} catch (IOException ex) {
			if(GhprbTrigger.DESCRIPTOR.getUseComments()){
				Logger.getLogger(GhprbRepo.class.getName()).log(Level.INFO, "Could not update commit status of the Pull Request on Github. Trying to send comment.", ex);
				addComment(id, message);
			}else{
				Logger.getLogger(GhprbRepo.class.getName()).log(Level.SEVERE, "Could not update commit status of the Pull Request on Github.", ex);
			}
		}
	}

	public String getName() {
		return reponame;
	}

	public GhprbHelper getHelper() {
		return helper;
	}

	public void addComment(int id, String comment) {
		try {
			repo.getPullRequest(id).comment(comment);
		} catch (IOException ex) {
			Logger.getLogger(GhprbRepo.class.getName()).log(Level.SEVERE, "Couldn't add comment to pullrequest #" + id + ": '" + comment + "'", ex);
		}
	}

	public void closePullRequest(int id) {
		try {
			repo.getPullRequest(id).close();
		} catch (IOException ex) {
			Logger.getLogger(GhprbRepo.class.getName()).log(Level.SEVERE, "Couldn't close the pullrequest #" + id + ": '", ex);
		}
	}

	public String getRepoUrl(){
		return gh.getGitHubServer()+"/"+reponame;
	}

	public void createHook() throws IOException{
		repo.createWebHook(new URL("url"), EnumSet.of(GHEvent.ISSUE_COMMENT, GHEvent.PULL_REQUEST));
	}
}
