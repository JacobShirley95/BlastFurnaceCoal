package jaccob.blastfurnace.base;

import org.powerbot.script.Tile;
import org.powerbot.script.rt4.ClientContext;
import org.powerbot.script.rt4.TileMatrix;

public class TileInteraction implements Interaction {

	private Tile tile;
	private ClientContext ctx;

	public TileInteraction(Tile tile, ClientContext ctx) {
		this.tile = tile;
		this.ctx = ctx;
	}
	
	@Override
	public boolean prepare() {
		TileMatrix mtx = tile.matrix(ctx);
		if (mtx.onMap()) {
			return ctx.input.move(mtx.mapPoint());
		}
		
		return false;
	}

	@Override
	public boolean execute() {
		return ctx.movement.step(tile);
	}

	@Override
	public boolean complete() {
		// TODO Auto-generated method stub
		return false;
	}

}
