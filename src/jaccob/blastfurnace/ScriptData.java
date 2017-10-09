package jaccob.blastfurnace;

import org.powerbot.script.rt4.ClientContext;
import org.powerbot.script.rt4.GameObject;

import jaccob.blastfurnace.Defs.BarType;
import jaccob.blastfurnace.Defs.CarryMode;
import jaccob.blastfurnace.base.Callables;
import jaccob.blastfurnace.base.JaccobMethods;
import jaccob.blastfurnace.base.StateData;

public class ScriptData extends StateData{
	public JaccobMethods methods;
	public Callables callables;
	public Bank bank;
	public boolean gotCoal;
	public int barsMade;
	public int oreTrip = 0;
	public CarryMode carryMode = CarryMode.COAL;
	public BarType bar;
	public boolean foremanPaid;
	
	public ScriptData(ClientContext ctx) {
		super(ctx);
		
		methods = new JaccobMethods(ctx);
		callables = new Callables(ctx);
		bank = new Bank(ctx);
	}
	
	public final GameObject getConveyer() {
		GameObject obj = ctx.objects.select().id(Defs.BLAST_CONVEYER_ID).peek();
		obj.bounds(Defs.BLAST_CONVEYER_BOUNDS);
		return obj;
	}
	
	public final String getChatBoxText() {
		return ctx.widgets.widget(Defs.CHAT_BOX_TEXT_ID).component(Defs.CHAT_BOX_TEXT_COMP_ID).text();
	}
	
	public final GameObject getDispenser(boolean done) {
		GameObject obj = null;
		if (done) {
			obj = ctx.objects.select().id(Defs.DISPENSER_DONE_IDS).peek();
		} else {
			obj = ctx.objects.select().id(Defs.DISPENSER_IDS).peek();
		}
		obj.bounds(Defs.DISPENSER_BOUNDS);
		return obj;
	}
}
