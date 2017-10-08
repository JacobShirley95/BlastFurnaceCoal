package jaccob.blastfurnace.states;

import org.powerbot.script.Tile;
import org.powerbot.script.rt4.ClientContext;

import jaccob.blastfurnace.BlastFurnaceCoal;
import jaccob.blastfurnace.Defs;
import jaccob.blastfurnace.ScriptData;
import jaccob.blastfurnace.base.Interaction;
import jaccob.blastfurnace.base.Statee;
import jaccob.blastfurnace.base.StateData;
import jaccob.blastfurnace.base.TileInteraction;

public class OpenBank extends Statee<ScriptData> {

	private Interaction interaction;

	public OpenBank(Interaction interaction) {
		super();
		this.interaction = interaction;
	}

	@Override
	public Statee<ScriptData> update(ScriptData data) {
		if (!data.bank.cleverBankOpen(interaction)) {
			return new BankWalk();
		} 
		
		return null;
	}
}
