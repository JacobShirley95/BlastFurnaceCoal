package jaccob.blastfurnace.states;

import org.powerbot.script.rt4.ClientContext;
import org.powerbot.script.rt4.Item;

import jaccob.blastfurnace.Defs;
import jaccob.blastfurnace.ScriptData;
import jaccob.blastfurnace.base.Interaction;
import jaccob.blastfurnace.base.Statee;

public class WithdrawMoney extends Statee<ScriptData>{

	private int amount;

	public WithdrawMoney(int amount) {
		this.amount = amount;
	}
	
	@Override
	public Statee<ScriptData> update(ScriptData data) {
		ClientContext ctx = data.ctx;
		Interaction interaction = new Interaction() {
			@Override
			public boolean prepare() {
				Item item = ctx.inventory.select().id(data.bar.oreId).peek();
				if (item != null && item.valid())
					return item.hover();
				else
					return ctx.inventory.select().id(Defs.COAL_ID).peek().hover();
			}

			@Override
			public boolean execute() {
				if (!ctx.bank.deposit(data.bar.oreId, 1))
					return ctx.bank.deposit(Defs.COAL_ID, 1);
				
				return true;
			}
		};
		
		if (data.bank.cleverBankOpen(interaction)) {
			if (data.methods.invMoney() == 0 && ctx.inventory.select().count() == 28) {
				interaction.execute();
			}
			
			ctx.bank.withdraw(Defs.GOLD_ID, amount);
		} else
			return new BankWalk();
		
		return null;
	}

}
