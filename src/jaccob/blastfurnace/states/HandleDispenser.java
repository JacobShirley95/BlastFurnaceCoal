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
import jaccob.blastfurnace.base.RandomMouseInteraction;
import jaccob.blastfurnace.base.Statee;
import jaccob.blastfurnace.base.TileInteraction;

public class HandleDispenser extends Statee<ScriptData>{
	
	final boolean dispenserScreenVis(ClientContext ctx) {
		return ctx.widgets.widget(Defs.DISPENSER_SEL_ID).valid();
	}
	
	final boolean selectAll(ScriptData data) {
		Widget widg = data.ctx.widgets.widget(Defs.DISPENSER_SEL_ID);
		Component comp = widg.component(data.bar.dispenserId);
		
		for (int tries = 0; tries < 5; tries++) {
			if (comp.interact("Take")) {
				Condition.sleep(200);
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
	
	@Override
	public Statee<ScriptData> update(ScriptData data) {
		ClientContext ctx = data.ctx;
		
		GameObject dispenser = data.getDispenser(false);
		Tile pos = dispenser.tile();
		
		if (!dispenser.inViewport()) {
			ctx.movement.step(pos);
		}
		
		if (dispenserScreenVis(ctx))
			ctx.widgets.close(ctx.widgets.widget(Defs.DISPENSER_SEL_ID));

		if (data.methods.waitTillReasonableStop(1, null)) {
			if (!dispenserScreenVis(ctx))
				if (!clickDispenser(dispenser))
					return new HandleDispenser();
			
			new RandomMouseInteraction(ctx, 
									   Defs.DISPENSER_MOUSE_MOVE_AREA[0], 
									   Defs.DISPENSER_MOUSE_MOVE_AREA[1]).prepare();
			if (waitForDispenser(ctx)) {
				if (!selectAll(data)) {
					return new HandleDispenser();
				} else {
					new TileInteraction(BlastFurnaceCoal.BANK_AREA.getRandomTile(), ctx).prepare();
					
					if (!ctx.inventory.select().id(data.bar.barId).isEmpty()) {
						return new Banking();
					}
				}
			}
		}
		
		if (data.getChatBoxText().contains("Smithing"))
			ctx.chat.clickContinue();
		
		return new HandleDispenser();
	}

}
