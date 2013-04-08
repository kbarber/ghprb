package org.jenkinsci.plugins.ghprb;

import hudson.model.queue.QueueTaskFuture;
import java.util.HashSet;
import java.util.Iterator;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author janinko
 */
public class GhprbBuilds {
	private final HashSet<GhprbBuild> builds = new HashSet<GhprbBuild>();
	private GhprbTrigger trigger;
	private GhprbRepo repo;

	public GhprbBuilds(GhprbTrigger trigger, GhprbRepo repo){
		this.trigger = trigger;
		this.repo = repo;
	}

	public void check(){
		Iterator<GhprbBuild> it = builds.iterator();
		while(it.hasNext()){
			GhprbBuild build = it.next();
			build.check();
			if(build.isFinished()){
				it.remove();
			}
		}
	}
	
	public boolean cancelBuild(int id) {
		Iterator<GhprbBuild> it = builds.iterator();
		while(it.hasNext()){
			GhprbBuild build  = it.next();
			if (build.getPullID() == id) {
				if (build.cancel()) {
					it.remove();
					return true;
				}
			}
		}
		return false;
	}

	public void startJob(int id, String commit, boolean merged) {
		QueueTaskFuture<?> build = trigger.startJob(new GhprbCause(commit, id, merged));
		if(build == null){
			Logger.getLogger(GhprbRepo.class.getName()).log(Level.SEVERE, "Job didn't started");
			return;
		}
		builds.add(new GhprbBuild(repo, id, build, true));
	}
}
