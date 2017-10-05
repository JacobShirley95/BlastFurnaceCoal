package jaccob.blastfurnace.base;

public interface Interaction {
	public boolean prepare();
	public boolean execute();
	public boolean complete();
}
