package jaccob.blastfurnace.states;

import java.util.concurrent.Callable;

import org.powerbot.script.Condition;
import org.powerbot.script.rt4.ClientContext;
import org.powerbot.script.rt4.GameObject;
import org.powerbot.script.rt4.Npc;

import jaccob.blastfurnace.Defs;
import jaccob.blastfurnace.ScriptData;
import jaccob.blastfurnace.base.Statee;

public class ClearDispenser extends Statee<ScriptData> {
	
	@Override
	public Statee<ScriptData> update(ScriptData data) {
		ClientContext ctx = data.ctx;
		
		if (!ctx.bank.opened())
			return new OpenBank(null);
		
		if (ctx.bank.depositAllExcept(Defs.COAL_BAG_ID)) {
			return new HandleDispenser();
		}
		
		return null;
	}

}
