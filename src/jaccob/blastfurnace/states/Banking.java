package jaccob.blastfurnace.states;

import java.util.concurrent.Callable;

import org.powerbot.script.Condition;
import org.powerbot.script.Tile;
import org.powerbot.script.rt4.Bank.Amount;
import org.powerbot.script.rt4.ClientContext;

import jaccob.blastfurnace.Defs;
import jaccob.blastfurnace.Defs.CarryMode;
import jaccob.blastfurnace.ScriptData;
import jaccob.blastfurnace.base.State;

public class Banking extends State<ScriptData> {

	public Banking(int id) {
		super(id);
	}
	
	final boolean finished(ScriptData data) {
		ClientContext ctx = data.ctx;
		return ctx.bank.select().id(data.bar.oreId).count(true) == 0 ||
			   ctx.bank.select().id(Defs.COAL_ID).count(true) == 0 ||
			   ctx.bank.select().id(Defs.GOLD_ID).count(true) == 0;
	}

	@Override
	public boolean run(ScriptData data) {
		ClientContext ctx = data.ctx;
		data.barsMade += ctx.inventory.select().id(data.bar.barId).count();
		
		for (int tries = 0; tries < 5; tries++) {
			if (data.bank.cleverBankOpen(new Callable<Boolean>() {
				@Override
				public Boolean call() throws Exception {
					return ctx.inventory.select().id(data.bar.barId).peek().hover();
				}
			})) {
				if (data.bank.depositSmart(data.bar.barId, Amount.ALL.getValue(), new Callable<Boolean>() {
					@Override
					public Boolean call() throws Exception {
						return ctx.bank.select().id(Defs.COAL_ID).peek().hover();
					}
				})) {
					ctx.bank.depositAllExcept(Defs.COAL_BAG_ID);
					
					if (finished(data)) {
						ctx.controller.stop();
						return true;
					}
					
					if (getCofferAmount() < MIN_COFFER_AMOUNT) {
						withdrawMoney(BLAST_FURNACE_AMOUNT);
						ctx.bank.close();
						return true;
					}
					
					if (ctx.inventory.select().id(Defs.COAL_BAG_ID).isEmpty())
						ctx.bank.withdraw(Defs.COAL_BAG_ID, 1);
					
					if (coalFromBank()) {
						System.out.println("SDFSDF");
					}
					
					if (data.carryMode == CarryMode.COAL && ctx.inventory.select().id(Defs.COAL_ID).count() == 27)
						return true;
					else
						ctx.bank.open();
					
					ctx.bank.deposit(Defs.COAL_ID, Amount.ALL);
	
					if (ctx.movement.energyLevel() < 20) {
						if (ctx.inventory.select().count() == 28) {
							ctx.bank.deposit(data.bar.oreId, 1);
							ctx.bank.deposit(Defs.COAL_ID, 1);
						}
						
						for (int staminaPot : Defs.STAMINA_POTS) {
							if (ctx.bank.withdraw(staminaPot, 1)) {
								ctx.bank.close();
								
								ctx.inventory.select().id(staminaPot).peek().interact("Drink");
								if (ctx.bank.open())
									ctx.bank.depositAllExcept(Defs.COAL_BAG_ID, Defs.GOLD_ID, Defs.COAL_ID, data.bar.oreId);
								
								break;
							}
						}
					}
		
					if (data.bank.withdrawSmart(data.carryMode == CarryMode.COAL ? Defs.COAL_ID : data.bar.oreId, Amount.ALL, new Callable<Boolean>() {
						@Override
						public Boolean call() throws Exception {
							return ctx.input.move(new Tile(1939 + (int)Math.round(Math.random() * 3), 4967).matrix(ctx).mapPoint());
						}
					})) {
						return true;
					}
				}
			} else
				Condition.sleep(500);
		}
		return false;
	}

}
