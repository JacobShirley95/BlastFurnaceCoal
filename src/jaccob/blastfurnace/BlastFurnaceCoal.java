package jaccob.blastfurnace;

import org.powerbot.script.Tile;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.util.Stack;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

import org.powerbot.bot.rt6.client.Skill;
import org.powerbot.script.Area;
import org.powerbot.script.Condition;
import org.powerbot.script.Filter;
import org.powerbot.script.Input;
import org.powerbot.script.MenuCommand;
import org.powerbot.script.PaintListener;
import org.powerbot.script.PollingScript;
import org.powerbot.script.Script.Manifest;
import org.powerbot.script.rt4.Bank.Amount;
import org.powerbot.script.rt4.Game.Tab;
import org.powerbot.script.rt4.ChatOption;
import org.powerbot.script.rt4.ClientContext;
import org.powerbot.script.rt4.Component;
import org.powerbot.script.rt4.Constants;
import org.powerbot.script.rt4.GameObject;
import org.powerbot.script.rt4.GeItem;
import org.powerbot.script.rt4.Item;
import org.powerbot.script.rt4.ItemQuery;
import org.powerbot.script.rt4.Npc;
import org.powerbot.script.rt4.TextQuery;
import org.powerbot.script.rt4.Widget;

import jaccob.blastfurnace.Defs.BarType;
import jaccob.blastfurnace.Defs.CarryMode;
import jaccob.blastfurnace.base.Callables;
import jaccob.blastfurnace.base.StateMachine;
import jaccob.blastfurnace.base.TileInteraction;
import jaccob.blastfurnace.base.State;
import jaccob.blastfurnace.states.BankWalk;
import jaccob.blastfurnace.states.Banking;
import jaccob.blastfurnace.states.OpenBank;

@Manifest(name="BlastFurnaceCoal", description="description", properties="")
public class BlastFurnaceCoal extends PollingScript<ClientContext> implements PaintListener{
	int barProfit = 0;
	
	StateMachine mch = new StateMachine();
	
	final boolean needForeman() {
		return ctx.skills.level(Constants.SKILLS_SMITHING) < 60;
	}
	
	final boolean run() {
		if (ctx.movement.energyLevel() > 15)
			return ctx.movement.running(true);
		
		return false;
	}
	
	@Override
	public void start() {
		ctx.camera.pitch(true);
		ctx.camera.angle(data.methods.getRandomAngle(Defs.BLAST_YAWS));
		
		data = new ScriptData(ctx);
		data.bar = Defs.BAR;
		data.carryMode = data.bar.coalRatio > 1 ? CarryMode.COAL : CarryMode.ORE;
		
		barProfit = getBarProfit(data.bar);
		ctx.input.speed(71);
		stateStack.push(start);
	}
	
	jaccob.blastfurnace.base.State<ScriptData> start = new Banking();
	Stack<jaccob.blastfurnace.base.State<ScriptData>> stateStack = new Stack<>();
	
	ScriptData data = new ScriptData(ctx);
	
	@Override
	public void poll() {
		while (!ctx.controller.isStopping()) {
			if (!ctx.game.loggedIn()) {
				ctx.controller.stop();
				return;
			}
			if (ctx.controller.isSuspended()) {
				Thread.yield();
				continue;
			}
			
			run();
			if (!stateStack.isEmpty()) {
				jaccob.blastfurnace.base.State<ScriptData> st = stateStack.peek();
				if (st.start()) {
					stateStack.clear();
					stateStack.push(st);
				}
				
				jaccob.blastfurnace.base.State<ScriptData> next = st.update(data);
				
				System.out.println(st);
				
				if (next != null)
					stateStack.push(next);
				else
					stateStack.pop();
			}
		}
		//else
			//stateStack.push(st);
		
		//System.out.println(stateStack.size());
	}
	
	public static String formatInterval(final long interval, boolean millisecs )
	{
	    final long hr = TimeUnit.MILLISECONDS.toHours(interval);
	    final long min = TimeUnit.MILLISECONDS.toMinutes(interval) %60;
	    final long sec = TimeUnit.MILLISECONDS.toSeconds(interval) %60;
	    final long ms = TimeUnit.MILLISECONDS.toMillis(interval) %1000;
	    if( millisecs ) {
	        return String.format("%02d:%02d:%02d.%03d", hr, min, sec, ms);
	    } else {
	        return String.format("%02d:%02d:%02d", hr, min, sec );
	    }
	}
	
	final static int getBarProfit(BarType type) {
		int coalPrice = new GeItem(Defs.COAL_ID).price;
		int orePrice = new GeItem(type.oreId).price;
		int barPrice = new GeItem(type.barId).price;

		return barPrice - ((coalPrice * type.coalRatio) + orePrice);
	}
	
	int startXP = 0;
	
	final int getXPPerHour() {
		long runTime = getRuntime();
		if (startXP == 0)
			startXP = ctx.skills.experience(Constants.SKILLS_SMITHING);
		
		return (int) (3600000d / (long) runTime * (double) (ctx.skills.experience(Constants.SKILLS_SMITHING) - startXP));
	}
	
	final int getBarsPerHour() {
		long runTime = getRuntime();

		return (int) (3600000d / (long) runTime * (double) (data.barsMade));
	}

	@Override
	public void repaint(Graphics g) {
		Graphics2D g2 = (Graphics2D) g;
		g2.setColor(Color.green);
		
		int barsPerHour = getBarsPerHour();
		int xp = ctx.skills.experience(Constants.SKILLS_SMITHING) - startXP;
		
		g2.drawString("Time running: " + formatInterval(getRuntime(), false), 10, 30);
		g2.drawString("Profit: " + (data.barsMade * barProfit) + "gp (" + ((int)Math.round((barsPerHour * barProfit) / 1000)) + "k / Hr)", 10, 60);
		g2.drawString("Bar profit: " + barProfit + "gp", 10, 90);
		g2.drawString("Bars made: " + data.barsMade + " (" + barsPerHour +" / Hr)", 10, 120);
		g2.drawString("XP: " + xp + " (" + getXPPerHour() + " / Hr)", 10, 150);
	}
}
