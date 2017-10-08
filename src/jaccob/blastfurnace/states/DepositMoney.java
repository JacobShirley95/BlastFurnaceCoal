package jaccob.blastfurnace.states;

import org.powerbot.script.rt4.Bank.Amount;
import org.powerbot.script.rt4.ClientContext;
import org.powerbot.script.rt4.Item;

import jaccob.blastfurnace.Defs;
import jaccob.blastfurnace.ScriptData;
import jaccob.blastfurnace.base.Interaction;
import jaccob.blastfurnace.base.ItemInteraction;
import jaccob.blastfurnace.base.Statee;

public class DepositMoney extends Statee<ScriptData>{
	public DepositMoney() {
	}
	
	@Override
	public Statee<ScriptData> update(ScriptData data) {
		ClientContext ctx = data.ctx; 
		Interaction withdrawI = new Interaction() {
			
			@Override
			public boolean prepare() {
				return true;
			}
			
			@Override
			public boolean execute() {
				return ctx.bank.withdraw(ctx.inventory.select().reverse().peek(), 1);
			}
		};
		
		if (!ctx.bank.opened())
			return new OpenBank(new ItemInteraction(ctx, Defs.GOLD_ID, false));
		
		if (data.bank.depositSmart(Defs.GOLD_ID, Amount.ALL.getValue(), withdrawI)) {
			//withdrawI.execute();
		}
		
		return null;
	}

}
