package jaccob.blastfurnace.base;

import org.powerbot.script.rt4.ClientAccessor;
import org.powerbot.script.rt4.ClientContext;

public abstract class State<T extends StateEntity> {
	private int id;

	public State(int id) {
		this.id = id;
	}
	
	public int id() {
		return this.id;
	}
	
	public abstract boolean run(T entity);
}
