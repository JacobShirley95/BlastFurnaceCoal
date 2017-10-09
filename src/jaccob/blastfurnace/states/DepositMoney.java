package jaccob.blastfurnace.states;

import org.powerbot.script.rt4.Bank.Amount;
import org.powerbot.script.rt4.ClientContext;
import org.powerbot.script.rt4.Item;

import jaccob.blastfurnace.Defs;
import jaccob.blastfurnace.ScriptData;
import jaccob.blastfurnace.base.Interaction;
import jaccob.blastfurnace.base.ItemInteraction;
import jaccob.blastfurnace.base.State;

public class DepositMoney extends State<ScriptData>{
	public DepositMoney() {
	}
	
	@Override
	public State<ScriptData> update(ScriptData data) {
		ClientContext ctx = data.ctx; 
		Interaction withdrawI = new Interaction() {
			
			@Override
			public boolean prepare() {
				return true;
			}
			
			@Override
			public boolean execute() {
				int targetId = ctx.inventory.select().id(Defs.COAL_ID).isEmpty() ? data.bar.oreId : Defs.COAL_ID;
				return ctx.bank.withdraw(targetId, 1);
			}
		};
		
		if (!ctx.bank.opened())
			return new OpenBank(new ItemInteraction(ctx, Defs.GOLD_ID, false));
		
		if (data.bank.depositSmart(Defs.GOLD_ID, Amount.ALL.getValue(), withdrawI)) {
			withdrawI.execute();
		}
		
		return null;
	}

}
