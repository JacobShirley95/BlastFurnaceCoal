package jaccob.blastfurnace.states;

import org.powerbot.script.Tile;
import org.powerbot.script.rt4.ClientContext;

import jaccob.blastfurnace.BlastFurnaceCoal;
import jaccob.blastfurnace.Defs;
import jaccob.blastfurnace.ScriptData;
import jaccob.blastfurnace.base.State;
import jaccob.blastfurnace.base.StateEntity;
import jaccob.blastfurnace.base.TileInteraction;

public class BankWalk extends State<ScriptData> {

	public BankWalk(int id) {
		super(id);
	}

	@Override
	public boolean run(ScriptData data) {
		Tile bankPos = BlastFurnaceCoal.BANK_AREA.getRandomTile();

		TileInteraction interaction = new TileInteraction(bankPos, data.ctx);
		
		if (bankPos.distanceTo(data.ctx.players.local().tile()) > 5) {
			data.ctx.movement.step(bankPos);
			//hoverBankArea();
		}

		return data.methods.waitTillReasonableStop(5);
	}
}
