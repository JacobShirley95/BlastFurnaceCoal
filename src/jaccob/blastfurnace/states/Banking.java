package jaccob.blastfurnace.states;

import java.util.concurrent.Callable;

import org.powerbot.script.Condition;
import org.powerbot.script.Tile;
import org.powerbot.script.rt4.Bank.Amount;
import org.powerbot.script.rt4.Game.Tab;
import org.powerbot.script.rt4.ClientContext;
import org.powerbot.script.rt4.Component;
import org.powerbot.script.rt4.Constants;
import org.powerbot.script.rt4.Item;

import jaccob.blastfurnace.Defs;
import jaccob.blastfurnace.Defs.CarryMode;
import jaccob.blastfurnace.ScriptData;
import jaccob.blastfurnace.base.Interaction;
import jaccob.blastfurnace.base.ItemInteraction;
import jaccob.blastfurnace.base.Statee;
import jaccob.blastfurnace.base.TileInteraction;
import jaccob.blastfurnace.base.WidgetInteraction;

public class Banking extends Statee<ScriptData> {

	public Banking() {
		super();
	}
	
	final boolean finished(ScriptData data) {
		ClientContext ctx = data.ctx;
		return ctx.bank.select().id(data.bar.oreId).count(true) == 0 ||
			   ctx.bank.select().id(Defs.COAL_ID).count(true) == 0 ||
			   ctx.bank.select().id(Defs.GOLD_ID).count(true) == 0;
	}
	
	final Component bankCloseComponent(ClientContext ctx) {
		return ctx.widgets.widget(Constants.BANK_WIDGET).component(Constants.BANK_MASTER).component(Constants.BANK_CLOSE);
	}
	
	final int fillCoalBag(ScriptData data) {
		ClientContext ctx = data.ctx;
		ctx.game.tab(Tab.INVENTORY);
		for (int tries = 0; tries < 5; tries++) {
			if (ctx.inventory.select().id(Defs.COAL_BAG_ID).peek().interact("Fill")) {
				ctx.bank.nearest().tile().matrix(ctx).hover();
				
				return data.methods.wait(100, 12, data.callables.itemGoneCb(Defs.COAL_ID), 
										 data.callables.widgetVisible(Defs.COAL_BAG_FULL_ID));
			}
		}
		return -1;
	}
	
	final boolean coalFromBank(ScriptData data) {
		ClientContext ctx = data.ctx;
		
		for (int tries = 0; tries < 5; tries++) {
			if (data.bank.withdrawSmart(Defs.COAL_ID, Amount.ALL, new WidgetInteraction(bankCloseComponent(ctx)))) {
				break;
			}
		}
		
		for (int tries = 0; tries < 5; tries++) {
			ctx.bank.close();
			int result = fillCoalBag(data);
			
			if (result == 0) {
				data.gotCoal = true;
				ctx.bank.open();
				return true;
			} else if (result == 1) {
				data.gotCoal = true;
				return true;
			}
		}
		
		return false;
	}
	
	@Override
	public boolean start() {
		return true;
	}

	@Override
	public Statee<ScriptData> update(ScriptData data) {
		ClientContext ctx = data.ctx;
		
		if (!ctx.bank.opened())
			return new OpenBank(new ItemInteraction(ctx, data.bar.barId, false));
		
		WidgetInteraction hoverClose = new WidgetInteraction(bankCloseComponent(ctx));
		
		data.barsMade += ctx.inventory.select().id(data.bar.barId).count();
		if (data.bank.depositSmart(data.bar.barId, Amount.ALL.getValue(), new ItemInteraction(ctx, Defs.COAL_ID, true))) {
			data.bank.depositAllExcept(Defs.COAL_BAG_ID);
			
			if (finished(data)) {
				ctx.controller.stop();
				return null;
			}

			if (data.methods.getCofferAmount() < Defs.MIN_COFFER_AMOUNT) {
				return new PayCoffer();
			}
			
			if (ctx.inventory.select().id(Defs.COAL_BAG_ID).isEmpty())
				ctx.bank.withdraw(Defs.COAL_BAG_ID, 1);
			
			if (!data.gotCoal)
				coalFromBank(data);
			
			if (data.carryMode == CarryMode.COAL && ctx.inventory.select().id(Defs.COAL_ID).count() == 27)
				return new UseConveyer();
			else
				data.bank.cleverBankOpen(new ItemInteraction(ctx, Defs.COAL_ID, false));
			
			data.bank.depositSmart(Defs.COAL_ID, Amount.ALL.getValue(), new ItemInteraction(ctx, data.bar.oreId, true));

			if (ctx.movement.energyLevel() < 20) {
				if (ctx.inventory.select().count() == 28) {
					data.bank.depositSmart(data.bar.oreId, 1, hoverClose);
					data.bank.depositSmart(Defs.COAL_ID, 1, hoverClose);
				}
				
				for (int staminaPot : Defs.STAMINA_POTS) {
					if (data.bank.withdrawSmart(staminaPot, 1, hoverClose)) {
						ctx.bank.close();
						
						ctx.inventory.select().id(staminaPot).peek().interact("Drink");
						if (data.bank.cleverBankOpen(null))
							data.bank.depositAllExcept(Defs.COAL_BAG_ID, Defs.GOLD_ID, Defs.COAL_ID, data.bar.oreId);
						
						break;
					}
				}
			}

			if (data.bank.withdrawSmart(data.carryMode == CarryMode.COAL ? Defs.COAL_ID : data.bar.oreId, Amount.ALL, new TileInteraction(new Tile(1939 + (int)Math.round(Math.random() * 3), 4967), ctx))) {
				return new UseConveyer();
			}
		}

		return new Banking();
	}

}
