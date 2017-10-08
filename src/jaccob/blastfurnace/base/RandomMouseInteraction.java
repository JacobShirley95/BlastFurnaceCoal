package jaccob.blastfurnace.base;

import java.awt.Point;
import java.awt.geom.Area;

import org.powerbot.script.rt4.ClientAccessor;
import org.powerbot.script.rt4.ClientContext;

public class RandomMouseInteraction extends ClientAccessor implements Interaction {
	private Area area;
	private Point p1;
	private Point p2;

	public RandomMouseInteraction(ClientContext ctx, Point p1, Point p2) {
		super(ctx);
		
		this.p1 = p1;
		this.p2 = p2;
	}
	
	@Override
	public boolean prepare() {
		return ctx.input.move(p1.x + (int)(Math.random()*(p2.x - p1.x)), p1.y + (int)(Math.random()*(p2.y - p1.y))); 
	}

	@Override
	public boolean execute() {
		return false;
	}

}
