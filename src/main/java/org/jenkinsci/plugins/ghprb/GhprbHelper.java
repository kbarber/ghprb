
package org.jenkinsci.plugins.ghprb;

import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;
import org.kohsuke.github.GHCommitState;

/**
 * @author janinko
 */
public class GhprbHelper {
	private final Pattern retestPhrasePattern;
	private final Pattern whitelistPhrasePattern;
	private final Pattern oktotestPhrasePattern;
	private final GhprbMiddleLayer gml;
	private final GhprbGitHub gh;

	public GhprbHelper(GhprbMiddleLayer gml, GhprbGitHub gh){
		retestPhrasePattern = Pattern.compile(gml.getDescriptor().getRetestPhrase());
		whitelistPhrasePattern = Pattern.compile(gml.getDescriptor().getWhitelistPhrase());
		oktotestPhrasePattern = Pattern.compile(gml.getDescriptor().getOkToTestPhrase());
		this.gh = gh;
		this.gml = gml;
	}

	public boolean isRetestPhrase(String comment){
		return retestPhrasePattern.matcher(comment).matches();
	}

	public boolean isWhitelistPhrase(String comment){
		return whitelistPhrasePattern.matcher(comment).matches();
	}

	public boolean isOktotestPhrase(String comment){
		return oktotestPhrasePattern.matcher(comment).matches();
	}

	public boolean isWhitelisted(String username){
		return gml.getWhitelisted().contains(username)
		    || gml.getAdmins().contains(username)
		    || isInWhitelistedOrganisation(username);
	}

	public boolean isAdmin(String username){
		return gml.getAdmins().contains(username);
	}

	public boolean isAutoCloseFailedPullRequests() {
		return gml.getDescriptor().getAutoCloseFailedPullRequests();
	}
	
	public void addWhitelist(String author) {
		Logger.getLogger(GhprbRepo.class.getName()).log(Level.INFO, "Adding {0} to whitelist", author);
		gml.addWhitelist(author);
	}

	private boolean isInWhitelistedOrganisation(String username) {
		for(String organisation : gml.getOrganisations()){
			if(gh.isUserMemberOfOrganization(organisation,username)){
				return true;
			}
		}
		return false;
	}

	public boolean isMe(String username){
		return gml.getDescriptor().getUsername().equals(username);
	}

	public String getDefaultComment() {
		return gml.getDescriptor().getRequestForTestingPhrase();
	}

	public String build(GhprbPullRequest pr) {
		StringBuilder sb = new StringBuilder();
		if(gml.getBuilds().cancelBuild(pr.getId())){
			sb.append("Previous build stopped.");
		}

		if(pr.isMergeable()){
			sb.append(" Merged build triggered.");
		}else{
			sb.append(" Build triggered.");
		}

		gml.getBuilds().startJob(pr.getId(),pr.getHead(), pr.isMergeable());
		return sb.toString();
	}
}
