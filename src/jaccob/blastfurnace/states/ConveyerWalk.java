package jaccob.blastfurnace.states;

import org.powerbot.script.Tile;
import org.powerbot.script.rt4.ClientContext;
import org.powerbot.script.rt4.GameObject;

import jaccob.blastfurnace.Defs;
import jaccob.blastfurnace.ScriptData;
import jaccob.blastfurnace.base.ObjectInteraction;
import jaccob.blastfurnace.base.RandomMouseInteraction;
import jaccob.blastfurnace.base.State;

public class ConveyerWalk extends State<ScriptData>{
	@Override
	public State<ScriptData> update(ScriptData data) {
		ClientContext ctx = data.ctx;
		
		Tile random = new Tile(1939 + (int)Math.round(Math.random() * 3), 4967);
		ctx.movement.step(random);
		
		new RandomMouseInteraction(ctx, Defs.BLAST_MOUSE_MOVE_AREA[0], Defs.BLAST_MOUSE_MOVE_AREA[1]).prepare();
		
		//ctx.camera.angle(data.methods.getRandomAngle(Defs.BLAST_YAWS));
		
		return null;
	}

}
