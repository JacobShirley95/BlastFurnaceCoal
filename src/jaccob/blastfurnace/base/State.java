package jaccob.blastfurnace.base;

import org.powerbot.script.rt4.ClientAccessor;
import org.powerbot.script.rt4.ClientContext;

public abstract class State<T extends StateData> {
	public State() {
	}
	
	public boolean start() {
		return false;
	}
	
	public abstract State<T> update(T data);
}
