package jaccob.blastfurnace.base;

import org.powerbot.script.rt4.GameObject;

public class ObjectInteraction implements Interaction {

	private GameObject gO;
	private String option = null;

	public ObjectInteraction(GameObject gO) {
		this.gO = gO;
	}
	
	public ObjectInteraction(GameObject gO, String option) {
		this.gO = gO;
		this.option = option;
	}
	
	@Override
	public boolean prepare() {
		return this.gO.hover();
	}

	@Override
	public boolean execute() {
		return this.gO.interact(option);
	}
}
