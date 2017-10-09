package jaccob.blastfurnace.states;

import java.util.concurrent.Callable;

import org.powerbot.script.Condition;
import org.powerbot.script.rt4.ClientContext;
import org.powerbot.script.rt4.GameObject;
import org.powerbot.script.rt4.Npc;

import jaccob.blastfurnace.Defs;
import jaccob.blastfurnace.ScriptData;
import jaccob.blastfurnace.base.State;

public class HandleForeman extends State<ScriptData> {
	
	@Override
	public State<ScriptData> update(ScriptData data) {
		ClientContext ctx = data.ctx;
		
		if (data.foremanPaid)
			return null;
		
		if (data.methods.invMoney() < 2500)
			return new WithdrawMoney(2500);
		
		ctx.bank.close();
		
		Npc foreman = ctx.npcs.select().id(Defs.FOREMAN_ID).peek();
		
		for (int tries = 0; tries < 5; tries++) {
			if (!foreman.inViewport())
				ctx.movement.step(Defs.FOREMAN_AREA.getRandomTile());
			
			int money = data.methods.invMoney();
			if (data.methods.waitTillReasonableStop(1, null) && foreman.interact("Pay")) {
				if (Condition.wait(new Callable<Boolean>() {
					@Override
					public Boolean call() throws Exception {
						return data.methods.invMoney() < money;
					}
				}, 50, 20)) {
					data.foremanPaid = true;
					break;
				}
				
				if (Condition.wait(new Callable<Boolean>() {
					@Override
					public Boolean call() throws Exception {
						return ctx.chat.select().text("Yes").peek().select();
					}
				}, 500, 10)) {
					data.foremanPaid = true;
					break;
				}
			}
		}
		
		return new DepositMoney();
	}

}
