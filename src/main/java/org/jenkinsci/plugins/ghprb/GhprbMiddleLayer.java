package org.jenkinsci.plugins.ghprb;

import com.coravy.hudson.plugins.github.GithubProjectProperty;
import hudson.model.AbstractProject;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author janinko
 */
public class GhprbMiddleLayer {
	private static final Pattern githubUserRepoPattern = Pattern.compile("^(http[s]?://[^/]*)/([^/]*)/([^/]*).*");

	private HashSet<String> admins;
	private HashSet<String> whitelisted;
	private HashSet<String> organisations;
	private GhprbTrigger    trigger;
	private GhprbRepo       repository;
	private GhprbBuilds     builds;

	private GhprbMiddleLayer(){}
	
	public static Builder getBuilder(){
		return new Builder();
	}

	public void addWhitelist(String author){
		whitelisted.add(author);
		trigger.addWhitelist(author);
	}

	public HashSet<String> getAdmins() {
		return admins;
	}

	public HashSet<String> getWhitelisted() {
		return whitelisted;
	}

	public HashSet<String> getOrganisations() {
		return organisations;
	}

	public GhprbBuilds getBuilds() {
		return builds;
	}

	public GhprbTrigger.DescriptorImpl getDescriptor(){
		return trigger.getDescriptor();
	}

	void run() {
		repository.check();
		builds.check();
	}

	void stop() {
		repository = null;
		builds = null;
	}

	public static class Builder{
		private GhprbMiddleLayer gml = new GhprbMiddleLayer();
		private String githubServer;
		private String user;
		private String repo;
		private Map<Integer, GhprbPullRequest> pulls;

		public Builder setTrigger(GhprbTrigger trigger) {
			if(gml == null) return this;

			gml.trigger = trigger;
			gml.admins = new HashSet<String>(Arrays.asList(trigger.getAdminlist().split("\\s+")));
			gml.whitelisted = new HashSet<String>(Arrays.asList(trigger.getWhitelist().split("\\s+")));
			gml.organisations = new HashSet<String>(Arrays.asList(trigger.getOrgslist().split("\\s+")));

			return this;
		}

		public Builder setPulls(Map<Integer, GhprbPullRequest> pulls) {
			if(gml == null) return this;
			this.pulls = pulls;
			return this;
		}

		public Builder setProject(AbstractProject<?, ?> project) {
			if(gml == null) return this;
			
			GithubProjectProperty ghpp = project.getProperty(GithubProjectProperty.class);
			if(ghpp == null || ghpp.getProjectUrl() == null) {
				Logger.getLogger(GhprbTrigger.class.getName()).log(Level.WARNING, "A github project url is required.");
				gml = null;
				return this;
			}
			String baseUrl = ghpp.getProjectUrl().baseUrl();
			Matcher m = githubUserRepoPattern.matcher(baseUrl);
			if(!m.matches()) {
				Logger.getLogger(GhprbTrigger.class.getName()).log(Level.WARNING, "Invalid github project url: {0}", baseUrl);
				gml = null;
				return this;
			}
			githubServer = m.group(1);
			user = m.group(2);
			repo = m.group(3);
			return this;
		}

		public GhprbMiddleLayer build(){
			if(gml == null || pulls == null || gml.trigger == null || githubServer == null){
				throw new IllegalStateException();
			}
			GhprbGitHub gh = new GhprbGitHub(gml.trigger, githubServer);
			GhprbHelper helper = new GhprbHelper(gml, gh);
			gml.repository = new GhprbRepo(gh, user, repo, helper,pulls);
			gml.repository.init();
			gml.builds = new GhprbBuilds(gml.trigger,gml.repository);
			return gml;
		}
	}

}
