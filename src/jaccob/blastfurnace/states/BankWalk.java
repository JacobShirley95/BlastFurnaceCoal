package jaccob.blastfurnace.states;

import java.util.concurrent.Callable;

import org.powerbot.script.Condition;
import org.powerbot.script.Tile;
import org.powerbot.script.rt4.ClientContext;
import org.powerbot.script.rt4.TileMatrix;

import jaccob.blastfurnace.BlastFurnaceCoal;
import jaccob.blastfurnace.Defs;
import jaccob.blastfurnace.ScriptData;
import jaccob.blastfurnace.base.Interaction;
import jaccob.blastfurnace.base.RandomMouseInteraction;
import jaccob.blastfurnace.base.State;
import jaccob.blastfurnace.base.StateData;
import jaccob.blastfurnace.base.TileInteraction;

public class BankWalk extends State<ScriptData> {

	public BankWalk() {
		super();
	}

	@Override
	public State<ScriptData> update(ScriptData data) {
		Tile bankPos = Defs.BANK_AREA.getRandomTile();
		TileInteraction interaction = new TileInteraction(bankPos, data.ctx);
		interaction.execute();
		
		if (bankPos.distanceTo(data.ctx.players.local().tile()) > 5) {
			data.ctx.movement.step(bankPos);
			new RandomMouseInteraction(data.ctx, Defs.BANK_MOUSE_MOVE_AREA[0], Defs.BANK_MOUSE_MOVE_AREA[1]).prepare();
		}

		Tile bT = data.ctx.bank.nearest().tile();
		TileMatrix mT = bT.matrix(data.ctx);
		Condition.wait(new Callable<Boolean>() {

			@Override
			public Boolean call() throws Exception {
				return mT.inViewport();
			}
			
		}, 50, 100);
		//data.methods.waitTillReasonableStop(5, null);
		
		return null;
	}
}
