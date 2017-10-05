package jaccob.blastfurnace;

import org.powerbot.script.rt4.ClientContext;

import jaccob.blastfurnace.Defs.BarType;
import jaccob.blastfurnace.Defs.CarryMode;
import jaccob.blastfurnace.base.Callables;
import jaccob.blastfurnace.base.JaccobMethods;
import jaccob.blastfurnace.base.StateEntity;

public class ScriptData extends StateEntity{
	public JaccobMethods methods;
	public Callables callables;
	public Bank bank;
	public boolean gotCoal;
	public int barsMade;
	public CarryMode carryMode;
	public BarType bar;
	
	public ScriptData(ClientContext ctx) {
		super(ctx);
		
		methods = new JaccobMethods(ctx);
		callables = new Callables(ctx);
		bank = new Bank(ctx);
	}
}
