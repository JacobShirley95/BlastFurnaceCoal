package jaccob.blastfurnace.states;

import java.util.concurrent.Callable;

import org.powerbot.script.Condition;
import org.powerbot.script.Tile;
import org.powerbot.script.rt4.ClientContext;
import org.powerbot.script.rt4.GameObject;
import org.powerbot.script.rt4.Widget;

import jaccob.blastfurnace.BlastFurnaceCoal;
import jaccob.blastfurnace.Defs;
import jaccob.blastfurnace.ScriptData;
import jaccob.blastfurnace.base.RandomMouseInteraction;
import jaccob.blastfurnace.base.Statee;
import jaccob.blastfurnace.base.TileInteraction;

public class UseConveyer extends Statee<ScriptData>{
	
	final Statee<ScriptData> conveyAndCheck(ScriptData data, int id) {
		int res = data.methods.wait(data.callables.itemGoneCb(id), data.callables.widgetVisible(Defs.NEED_TO_SMELT_FIRST_ID));
		if (res == 1) {
			String txt = data.getChatBoxText();
			System.out.println(txt);
			Statee<ScriptData> nextState = null;
			
			if (txt.contains("permission")) {
				data.foremanPaid = false;
				nextState = new HandleForeman();
				System.out.println("done foreman");
			} else {
				nextState = new ClearDispenser();
			}
			return nextState;
			//return true;
		}
		
		System.out.println("RESULT: " + res);
	
		return null;
	}
	
	public final boolean waitEmptyCoal(ClientContext ctx) {
		for (int tries = 0; tries < 5; tries++) {
			Widget wi = ctx.widgets.widget(Defs.NEED_TO_SMELT_FIRST_ID);
			if (Condition.wait(new Callable<Boolean>() {
				@Override
				public Boolean call() throws Exception {
					return ctx.inventory.select().id(Defs.COAL_ID).count() > 0 || wi.valid();
				}
			}, 50, 30)) {
				if (wi.valid())
					return false;
				
				return true;
			} else {
				ctx.inventory.select().id(Defs.COAL_BAG_ID).peek().interact("Empty");
			}
		}
		return false;
	}
	
	@Override
	public Statee<ScriptData> update(ScriptData data) {
		ClientContext ctx = data.ctx;
		GameObject conveyer = data.getConveyer();
		
		if (!conveyer.inViewport())
			return new ConveyerWalk();
		
		int targetId = data.carryMode == Defs.CarryMode.COAL ? Defs.COAL_ID : data.bar.oreId;
		boolean first = data.gotCoal && ctx.inventory.select().id(targetId).count() > 0;
		
		if (!first) {
			if (!data.gotCoal)
				return null;
			
			ctx.inventory.select().id(Defs.COAL_BAG_ID).peek().interact("Empty");
			conveyer.hover();
			
			if (!waitEmptyCoal(ctx))
				return new UseConveyer();
			else
				data.gotCoal = false;
		}
		
		if (conveyer.interact("Put-ore-on")) {
			if (first)
				ctx.inventory.select().id(Defs.COAL_BAG_ID).peek().hover();
			else {
				if (data.carryMode == Defs.CarryMode.COAL) {
					new TileInteraction(BlastFurnaceCoal.BANK_AREA.getRandomTile(), ctx).prepare();
				} else {
					data.getDispenser(false).hover();
				}
				
			}
			
			Statee<ScriptData> s = conveyAndCheck(data, first ? targetId : Defs.COAL_ID);
			
			if (s != null)
				return s;
			
			if (first || ctx.inventory.select().count() > 1)
				return new UseConveyer();
			
			if (data.carryMode == Defs.CarryMode.COAL) {
				return new Banking();
			} else {
				return new HandleDispenser();
			}
		}
		
		
		return null;
	}

}
