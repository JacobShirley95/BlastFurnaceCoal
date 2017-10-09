package jaccob.blastfurnace.base;

import org.powerbot.script.rt4.ClientAccessor;
import org.powerbot.script.rt4.ClientContext;

public abstract class Statee<T extends StateData> {
	public Statee() {
	}
	
	public boolean start() {
		return false;
	}
	
	public abstract Statee<T> update(T data);
}
