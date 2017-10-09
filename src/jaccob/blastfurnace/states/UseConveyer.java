package jaccob.blastfurnace.states;

import java.awt.Point;
import java.util.concurrent.Callable;

import org.powerbot.script.Condition;
import org.powerbot.script.Tile;
import org.powerbot.script.rt4.ClientContext;
import org.powerbot.script.rt4.GameObject;
import org.powerbot.script.rt4.Widget;

import jaccob.blastfurnace.BlastFurnaceCoal;
import jaccob.blastfurnace.Defs;
import jaccob.blastfurnace.ScriptData;
import jaccob.blastfurnace.Defs.CarryMode;
import jaccob.blastfurnace.base.ObjectInteraction;
import jaccob.blastfurnace.base.RandomMouseInteraction;
import jaccob.blastfurnace.base.State;
import jaccob.blastfurnace.base.TileInteraction;

public class UseConveyer extends State<ScriptData>{
	
	final State<ScriptData> conveyAndCheck(ScriptData data, int id) {
		int res = data.methods.wait(data.callables.itemGoneCb(id), data.callables.widgetVisible(Defs.NEED_TO_SMELT_FIRST_ID));
		System.out.println("RESULT: " + res);
		
		if (res == 1) {
			String txt = data.getChatBoxText();
			System.out.println(txt);
			State<ScriptData> nextState = null;
			
			if (txt.contains("permission")) {
				data.foremanPaid = false;
				nextState = new HandleForeman();
				System.out.println("done foreman");
			} else if (txt.contains("collect")){
				nextState = new ClearDispenser();
			} else if (txt.contains("smelts")) {
				nextState = new ClearDispenser();
				data.carryMode = CarryMode.ORE;
			}
			return nextState;
			//return true;
		}
	
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
	public State<ScriptData> update(ScriptData data) {
		ClientContext ctx = data.ctx;
		GameObject conveyer = data.getConveyer();
		

		long timer = System.currentTimeMillis() + 9000;
		while (data.methods.distanceToDest() > 3 && System.currentTimeMillis() < timer) {
			if (conveyer.inViewport()) {
				Point p = conveyer.centerPoint();
				p.translate(0, 20);
				ctx.input.move(p);
				//ctx.input.click(true);
				
				break;
			}
		}
		
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
		
		for (int tries = 0; tries < 5; tries++) {
			if (conveyer.interact(true, "Put-ore-on")) {
				if (first) {
					Condition.wait(new Callable<Boolean>() {
						@Override
						public Boolean call() throws Exception {
							Tile dest = ctx.movement.destination();
							return dest.x() != -1;
						}
					}, 20, 10);
					
					if (!Condition.wait(new Callable<Boolean>() {
						@Override
						public Boolean call() throws Exception {
							Tile dest = ctx.movement.destination();
							
							if (dest.equals(new Tile(1943, 4967, 0))
							 || dest.equals(new Tile(1942, 4967, 0))) {
								return true;
							}
							
							System.out.println("fail");
							
							return false;
						}
					}, 20, 10))
						continue;
					
					ctx.inventory.select().id(Defs.COAL_BAG_ID).peek().hover();
				} else {
					if (data.carryMode == Defs.CarryMode.COAL) {
						new TileInteraction(Defs.BANK_AREA.getRandomTile(), ctx).prepare();
					} else {
						data.getDispenser(false).hover();
					}
					
				}
				
				State<ScriptData> s = conveyAndCheck(data, first ? targetId : Defs.COAL_ID);
				
				if (s != null)
					return s;
				
				if (first || ctx.inventory.select().count() > 1)
					return new UseConveyer();
				
				if (data.carryMode == Defs.CarryMode.COAL) {
					data.carryMode = Defs.CarryMode.ORE;
					return new Banking();
				} else {
					data.oreTrip++;
					
					if (data.bar.oreTrips > 0 && data.oreTrip == data.bar.oreTrips) {
						data.carryMode = CarryMode.COAL;
						data.oreTrip = 0;
					}
					
					return new HandleDispenser();
				}
			}
		}
		
		
		return null;
	}

}
