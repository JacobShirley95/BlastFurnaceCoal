package jaccob.blastfurnace;

import org.powerbot.script.Tile;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.util.Stack;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

import org.powerbot.script.Area;
import org.powerbot.script.Condition;
import org.powerbot.script.Filter;
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

import jaccob.blastfurnace.BlastFurnaceCoal.CarryMode;
import jaccob.blastfurnace.base.Callables;
import jaccob.blastfurnace.base.StateMachine;
import jaccob.blastfurnace.base.TileInteraction;
import jaccob.blastfurnace.base.Statee;
import jaccob.blastfurnace.states.BankWalk;
import jaccob.blastfurnace.states.Banking;
import jaccob.blastfurnace.states.OpenBank;

@Manifest(name="BlastFurnaceCoal", description="description", properties="")
public class BlastFurnaceCoal extends PollingScript<ClientContext> implements PaintListener{
	final static int COAL_ID = 453;
	final static int COAL_BAG_ID = 12019;
	
	final static int GOLD_ID = 995;
	
	enum CarryMode {
		COAL, ORE
	}

	enum BarType {
		STEEL(2353, 440, true, 110, 1),
		MITHRIL(2359, 447, true, 111, 2);
		
		public int barId;
		public int oreId;
		public boolean coal;
		public int dispenserId;
		public int coalRatio;
		public int oreTrips;
		
		BarType(int barId, int oreId, boolean coal, int dispenserId, int coalRatio) {
			this.barId = barId;
			this.oreId = oreId;
			this.coal = coal;
			this.dispenserId = dispenserId;
			this.coalRatio = coalRatio;
			this.oreTrips = coalRatio;
		}
	}
	
	final static BarType BAR = BarType.STEEL;
	
	final static int COAL_BAG_FULL_ID = 193;
	final static int NEED_TO_SMELT_FIRST_ID = 229;
	
	final static int[] COFFER_IDS = {29328, 29329};
	final static int[] COFFER_BOUNDS = {-24, 36, 10, 0, -32, 28};
	final static int MIN_COFFER_AMOUNT = 3000;
	
	final static Tile FURNACE_TILE = new Tile(1939, 4963);
	
	final static int BLAST_FURNACE_AMOUNT = 10000;
	
	final static Area BLAST_AREA = new Area(new Tile(1939, 4967), new Tile(1942, 4967));
	final static int[] BLAST_YAWS = {261, 241};
	final static int BLAST_CONVEYER_ID = 9100;
	final static int[] BLAST_CONVEYER_BOUNDS = {-36, 20, -240, -228, -72, 32};
			
	final static int[] DISPENSER_IDS = {9093, 9094, 9095, 9096};
	final static int[] DISPENSER_DONE_IDS = {9094, 9095, 9096};
	final static int[] DISPENSER_BOUNDS = {-108, -44, -96, -36, -32, 32};
	
	final static int[] STAMINA_POTS = {12631, 12629, 12625, 12627};
	
	//Components
	final static int DISPENSER_SEL_ID = 28;
	
	final static int CHAT_BOX_TEXT_ID = 229;
	final static int CHAT_BOX_TEXT_COMP_ID = 0;
	
	final static Area FOREMAN_AREA = new Area(new Tile(1944, 4958), new Tile(1946, 4960));
	final static int FOREMAN_ID = 2923;
	
	public final static Area BANK_AREA = new Area(new Tile(1947, 4955), new Tile(1948, 4957));
	
	public final static Point[] BLAST_MOUSE_MOVE_AREA = {new Point(7, 134), new Point(79, 218)};
	public final static Point[] DISPENSER_MOUSE_MOVE_AREA = {new Point(194, 64), new Point(395, 264)};
	public final static Point[] BANK_MOUSE_MOVE_AREA = {new Point(292, 29), new Point(301, 81)};
	
	CarryMode carryMode = BAR.coalRatio > 1 ? CarryMode.COAL : CarryMode.ORE;
	
	int barProfit = getBarProfit(BAR);
	
	Bank bank;
	Callables callables;
	
	StateMachine mch = new StateMachine();
	
	final boolean needForeman() {
		return ctx.skills.level(Constants.SKILLS_SMITHING) < 60;
	}
	
	final static int getBarProfit(BarType type) {
		int coalPrice = new GeItem(COAL_ID).price;
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
	
	
	final boolean run() {
		if (ctx.movement.energyLevel() > 15)
			return ctx.movement.running(true);
		
		return false;
	}
	
	@Override
	public void start() {
		bank = new Bank(ctx);
		callables = new Callables(ctx);
		
		ctx.camera.pitch(true);
		data = new ScriptData(ctx);
		data.bar = Defs.BAR;
		stateStack.push(start);
	}
	
	Statee<ScriptData> start = new Banking();
	Stack<Statee<ScriptData>> stateStack = new Stack<>();
	
	ScriptData data = new ScriptData(ctx);
	
	@Override
	public void poll() {
		while (!ctx.controller.isStopping()) {
			run();
			if (!stateStack.isEmpty()) {
				Statee<ScriptData> st = stateStack.peek();
				Statee<ScriptData> next = st.update(data);
				
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

	@Override
	public void repaint(Graphics g) {
		Graphics2D g2 = (Graphics2D) g;
		g2.setColor(Color.green);
		
		int barsPerHour = getBarsPerHour();
		
		g2.drawString("Time running: " + formatInterval(getRuntime(), false), 10, 30);
		g2.drawString("Profit p/h: " + ((int)Math.round((barsPerHour * barProfit) / 1000)), 10, 60);
		g2.drawString("Bars made: " + data.barsMade, 10, 90);
		g2.drawString("Bars p/h: " + barsPerHour, 10, 120);
		g2.drawString("XP p/h: " + getXPPerHour(), 10, 150);
	}
}
