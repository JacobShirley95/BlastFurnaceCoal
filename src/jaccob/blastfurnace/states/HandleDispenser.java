package jaccob.blastfurnace.states;

import java.util.concurrent.Callable;

import org.powerbot.script.Condition;
import org.powerbot.script.Tile;
import org.powerbot.script.rt4.ClientContext;
import org.powerbot.script.rt4.Component;
import org.powerbot.script.rt4.GameObject;
import org.powerbot.script.rt4.Widget;

import jaccob.blastfurnace.BlastFurnaceCoal;
import jaccob.blastfurnace.Defs;
import jaccob.blastfurnace.ScriptData;
import jaccob.blastfurnace.base.ObjectInteraction;
import jaccob.blastfurnace.base.RandomMouseInteraction;
import jaccob.blastfurnace.base.State;
import jaccob.blastfurnace.base.TileInteraction;

public class HandleDispenser extends State<ScriptData>{
	
	final boolean dispenserScreenVis(ClientContext ctx) {
		return ctx.widgets.widget(Defs.DISPENSER_SEL_ID).valid();
	}
	
	final boolean selectAll(ScriptData data) {
		Widget widg = data.ctx.widgets.widget(Defs.DISPENSER_SEL_ID);
		Component comp = widg.component(data.bar.dispenserId);
		
		for (int tries = 0; tries < 5; tries++) {
			if (comp.click()) {
				return true;
			}
		}
		
		return false;
	}
	
	final boolean waitForDispenser(ClientContext ctx) {
		return Condition.wait(new Callable<Boolean>() {
			@Override
			public Boolean call() throws Exception {
				return dispenserScreenVis(ctx);
			}
		}, 100, 40);
	}
	
	final boolean clickDispenser(GameObject dispenser) {
		for (int tries = 0; tries < 5; tries++) {
			if (dispenser.interact(dispenser.actions()[0]))
				return true;
		}
		
		return false;
	}
	
	final boolean waitMoving(ClientContext ctx) {
		return Condition.wait(new Callable<Boolean>() {

			@Override
			public Boolean call() throws Exception {
				return ctx.players.local().inMotion() || dispenserScreenVis(ctx);
			}
			
		}, 50, 15);
	}
	
	@Override
	public State<ScriptData> update(ScriptData data) {
		ClientContext ctx = data.ctx;
		
		GameObject dispenser = data.getDispenser(false);
		Tile pos = dispenser.tile();
		
		if (!dispenser.inViewport()) {
			ctx.movement.step(pos);
			data.methods.waitTillReasonableStop(1, new ObjectInteraction(dispenser));
		}
		
		if (dispenserScreenVis(ctx))
			ctx.widgets.close(ctx.widgets.widget(Defs.DISPENSER_SEL_ID));
		
		for (int tries = 0; tries < 5; tries++) {
			if (tries == 3 && dispenserScreenVis(ctx)) {
				ctx.widgets.close(ctx.widgets.widget(Defs.DISPENSER_SEL_ID));
				Condition.wait(new Callable<Boolean>() {

					@Override
					public Boolean call() throws Exception {
						return data.getDispenser(true).valid();
					}
					
				}, 200, 30);
			}
			
			if (!dispenserScreenVis(ctx))
				if (!clickDispenser(dispenser))
					return new HandleDispenser();
			
			if (waitMoving(ctx)) {
				new RandomMouseInteraction(ctx, 
										   Defs.DISPENSER_MOUSE_MOVE_AREA[0], 
										   Defs.DISPENSER_MOUSE_MOVE_AREA[1]).prepare();
				if (waitForDispenser(ctx)) {
					if (selectAll(data))
						new TileInteraction(Defs.BANK_AREA.getRandomTile(), ctx).prepare();
					
					if (ctx.inventory.select().id(data.bar.barId).count() == 27)
						return new Banking();
				}
			}
		}
		
		return new Banking();
	}

}
