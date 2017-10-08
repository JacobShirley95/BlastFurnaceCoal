package jaccob.blastfurnace.base;

import org.powerbot.script.rt4.Component;

public class WidgetInteraction implements Interaction {

	private Component comp;

	public WidgetInteraction(Component comp) {
		this.comp = comp;
	}
	
	@Override
	public boolean prepare() {
		return comp.hover();
	}

	@Override
	public boolean execute() {
		// TODO Auto-generated method stub
		return false;
	}

}
