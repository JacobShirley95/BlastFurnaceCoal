package jaccob.blastfurnace.states;

import java.util.concurrent.Callable;

import org.powerbot.script.Condition;
import org.powerbot.script.rt4.ChatOption;
import org.powerbot.script.rt4.ClientContext;
import org.powerbot.script.rt4.GameObject;
import org.powerbot.script.rt4.TextQuery;

import jaccob.blastfurnace.Defs;
import jaccob.blastfurnace.ScriptData;
import jaccob.blastfurnace.base.Statee;

public class PayCoffer extends Statee<ScriptData> {

	final GameObject getCoffer(ClientContext ctx) {
		GameObject obj = ctx.objects.select().id(Defs.COFFER_IDS).peek();
		obj.bounds(Defs.COFFER_BOUNDS);
		return obj;
	}
	
	final boolean clickCoffer(ClientContext ctx) {
		GameObject coffer = getCoffer(ctx);
		if (coffer.inViewport() && coffer.interact("Use")) {
			return Condition.wait(new Callable<Boolean>() {
				@Override
				public Boolean call() throws Exception {
					return !ctx.chat.select().text("Cancel").isEmpty();
				}
			});
		}
		return false;
	}
	
	@Override
	public Statee<ScriptData> update(ScriptData data) {
		ClientContext ctx = data.ctx;
		
		if (data.methods.invMoney() == 0)
			return new WithdrawMoney(Defs.BLAST_FURNACE_AMOUNT);
		
		ctx.bank.close();
		
		if (clickCoffer(ctx)) {
			TextQuery<ChatOption> q = ctx.chat.select().text("Deposit coins.");
			if (q.isEmpty())
				return null;
			
			q.poll().select();
			
			Condition.sleep(1000);
			
			if (ctx.chat.canContinue())
				return new HandleForeman();
					
			ctx.input.sendln("" + Defs.BLAST_FURNACE_AMOUNT);
		}
		
		return null;
	}

}
