package jaccob.blastfurnace;

import org.powerbot.script.Tile;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

import org.powerbot.script.Area;
import org.powerbot.script.Condition;
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

@Manifest(name="BlastFurnaceCoal", description="description", properties="")
public class BlastFurnaceCoal extends PollingScript<ClientContext> implements PaintListener{
	final static int COAL_ID = 453;
	final static int COAL_BAG_ID = 12019;
	
	final static int GOLD_ID = 995;
	
	enum State {
		WALK_TO_BANK, BANKING, PAYMENT, WALK_TO_BLAST, USE_CONVEYER, DISPENSER
	}
	
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
		
		BarType(int barId, int oreId, boolean coal, int dispenserId, int coalRatio) {
			this.barId = barId;
			this.oreId = oreId;
			this.coal = coal;
			this.dispenserId = dispenserId;
			this.coalRatio = coalRatio;
		}
	}
	
	final static BarType BAR = BarType.MITHRIL;
	
	final static int[] COFFER_IDS = {29328, 29329};
	final static int[] COFFER_BOUNDS = {-24, 36, 10, 0, -32, 28};
	final static int MIN_COFFER_AMOUNT = 3000;
	
	final static Tile FURNACE_TILE = new Tile(1939, 4963);
	
	final static int BLAST_FURNACE_AMOUNT = 5000;
	
	final static Area BLAST_AREA = new Area(new Tile(1939, 4967), new Tile(1942, 4967));
	final static int[] BLAST_YAWS = {261, 241};
	final static int BLAST_CONVEYER_ID = 9100;
	final static int[] BLAST_CONVEYER_BOUNDS = {-36, 20, -240, -228, -72, 32};
			
	final static int[] DISPENSER_IDS = {9093, 9094, 9095, 9096};
	final static int[] DISPENSER_DONE_IDS = {9094, 9095, 9096};
	final static int[] DISPENSER_BOUNDS = {-120, -20, -104, -20, -64, 64};
	
	final static int[] STAMINA_POTS = {12631, 12629, 12625, 12627};
	
	//Components
	final static int DISPENSER_SEL_ID = 28;
	
	final static int CHAT_BOX_TEXT_ID = 229;
	final static int CHAT_BOX_TEXT_COMP_ID = 0;
	
	final static Area FOREMAN_AREA = new Area(new Tile(1944, 4958), new Tile(1946, 4960));
	final static int FOREMAN_ID = 2923;
	
	final static Area BANK_AREA = new Area(new Tile(1947, 4955), new Tile(1948, 4957));
	
	final static Point[] BLAST_MOUSE_MOVE_AREA = {new Point(7, 134), new Point(79, 218)};
	final static Point[] DISPENSER_MOUSE_MOVE_AREA = {new Point(194, 64), new Point(295, 174)};
	final static Point[] BANK_MOUSE_MOVE_AREA = {new Point(292, 29), new Point(301, 81)};
	
	State state = State.WALK_TO_BANK;
	CarryMode carryMode = CarryMode.COAL;
	
	int barProfit = getBarProfit(BAR);
	int barsMade = 0;
	
	final boolean needCoffer() {
		return ctx.skills.level(Constants.SKILLS_SMITHING) < 60;
	}
	
	final static int getBarProfit(BarType type) {
		int coalPrice = new GeItem(COAL_ID).price;
		int orePrice = new GeItem(type.oreId).price;
		int barPrice = new GeItem(type.barId).price;

		return barPrice - ((coalPrice * type.coalRatio) + orePrice);
	}
	
	final int getProfitPerHour() {
		long runTime = getRuntime();

		return (int) (3600000d / (long) runTime * (double) (barsMade * barProfit));
	}
	
	final int getBarsPerHour() {
		long runTime = getRuntime();

		return (int) (3600000d / (long) runTime * (double) (barsMade));
	}
	
	final boolean walkToBank() {
		Tile bankPos = BANK_AREA.getRandomTile();

		if (bankPos.distanceTo(ctx.players.local().tile()) > 5) {
			ctx.movement.step(bankPos);
			hoverBankArea();
		}

		return waitTillReasonableStop(5);
	}
	
	final boolean hoverBankArea() {
		return ctx.input.move(getRandom(BANK_MOUSE_MOVE_AREA));
	}
	
	final boolean withdrawAllSmartId(int id) {
		//return ctx.inventory.select().id(id).isEmpty() ? ctx.bank.withdraw(id, amount) : true;
		return ctx.inventory.select().count() != 28 ? ctx.bank.select().id(id).peek().interact("Withdraw-All") : true;
	}
	
	final boolean withdrawSmartId(int id, int amount) {
		//return ctx.inventory.select().id(id).isEmpty() ? ctx.bank.withdraw(id, amount) : true;
		return ctx.inventory.select().count() != 28 ? ctx.bank.withdraw(id, amount) : true;
	}
	
	final boolean bank() {
		barsMade += ctx.inventory.select().id(BAR.barId).count();
		
		if (ctx.bank.open()) {
			ctx.bank.depositAllExcept(COAL_BAG_ID);
			
			if (getCofferAmount() < MIN_COFFER_AMOUNT) {
				withdrawMoney(BLAST_FURNACE_AMOUNT);
				ctx.bank.close();
				return true;
			}
			
			if (ctx.inventory.select().id(COAL_BAG_ID).isEmpty())
				ctx.bank.withdraw(COAL_BAG_ID, 1);
			
			if (carryMode == CarryMode.COAL) {
				withdrawAllSmartId(COAL_ID);
				Condition.sleep(500);
				
				for (int tries = 0; tries < 5; tries++) {
					ctx.bank.close();
					ctx.game.tab(Tab.INVENTORY);
					ctx.inventory.select().id(COAL_BAG_ID).peek().interact("Fill");
					if (waitItemGone(COAL_ID)) {
						ctx.bank.open();
						break;
					}
					
					System.out.println(ctx.chat.select().peek().text());
					if (ctx.chat.canContinue()){
						break;
					}
				}
			}
			
			if (carryMode == CarryMode.COAL && ctx.inventory.select().id(COAL_ID).count() != 27)
				ctx.bank.deposit(COAL_ID, Amount.ALL);
			
			if (ctx.movement.energyLevel() < 20) {
				if (ctx.inventory.select().count() == 28)
					ctx.bank.deposit(BAR.oreId, 1);
				
				for (int staminaPot : STAMINA_POTS) {
					if (ctx.bank.withdraw(staminaPot, 1)) {
						ctx.bank.close();
						
						ctx.inventory.select().id(staminaPot).peek().interact("Drink");
						if (ctx.bank.open())
							ctx.bank.depositAllExcept(COAL_BAG_ID, GOLD_ID, COAL_ID, BAR.oreId);
						
						break;
					}
				}
			}
			
			if (carryMode == CarryMode.COAL)
				withdrawAllSmartId(COAL_ID);
			else
				withdrawAllSmartId(BAR.oreId);
			
			return true;
		}
		
		return false;
	}
	
	final GameObject getConveyer() {
		GameObject obj = ctx.objects.select().id(BLAST_CONVEYER_ID).peek();
		obj.bounds(BLAST_CONVEYER_BOUNDS);
		return obj;
	}
	
	final GameObject getCoffer() {
		GameObject obj = ctx.objects.select().id(COFFER_IDS).peek();
		obj.bounds(COFFER_BOUNDS);
		return obj;
	}
	
	final GameObject getDispenser(boolean done) {
		GameObject obj = null;
		if (done) {
			obj = ctx.objects.select().id(DISPENSER_DONE_IDS).peek();
		} else {
			obj = ctx.objects.select().id(DISPENSER_IDS).peek();
		}
		obj.bounds(DISPENSER_BOUNDS);
		return obj;
	}
		
	final String getChatBoxText() {
		return ctx.widgets.widget(CHAT_BOX_TEXT_ID).component(CHAT_BOX_TEXT_COMP_ID).text();
	}
	
	final boolean walkToBlastArea() {
		Tile random = new Tile(1939 + (int)Math.round(Math.random() * 3), 4967);
		System.out.println(random);
		ctx.movement.step(random);
		
		ctx.input.move(getRandom(BLAST_MOUSE_MOVE_AREA));
		
		ctx.camera.angle(getRandomAngle(BLAST_YAWS));
		
		return true;
	}
	
	final boolean waitTillReasonableStop(final int dist) {
		Condition.sleep(500);
		return Condition.wait(new Callable<Boolean>() {
			@Override
			public Boolean call() throws Exception {
				return distanceToDest() < dist;
			}
		}, 100, 60);
	}
	
	final int distanceToDest() {
		return ctx.movement.distance(ctx.movement.destination());
	}
	
	final boolean clickConveyer(GameObject conveyer) {
		if (conveyer.interact("Put-ore-on"))
			return true;
		
		return false;
	}
	
	final boolean waitItemGone(int id) {
		int c = ctx.inventory.select().id(id).count();
		if (c == 0)
			return true;
		
		if (Condition.wait(new Callable<Boolean>() {
			@Override
			public Boolean call() throws Exception {
				return ctx.inventory.select().id(id).count() < c;
			}
		}, 50, 60)) {
			return true;
		}
		
		return false;
	}
	
	final boolean useConveyer() {
		GameObject conveyer = getConveyer();
		
		long timer = System.currentTimeMillis() + 9000;
		while (distanceToDest() > 3 && System.currentTimeMillis() < timer) {
			if (conveyer.inViewport()) {
				Point p = conveyer.centerPoint();
				ctx.input.move(p);
				//ctx.input.click(true);
				
				break;
			}
		}
		
		for (int tries = 0; tries < 5; tries++) {
			if (clickConveyer(conveyer)) {
				if (carryMode == CarryMode.COAL) {
					ctx.inventory.select().id(COAL_BAG_ID).peek().hover();
					waitItemGone(COAL_ID);
					
					if (getChatBoxText().contains("You must ask the foreman's permission")) {
						handleForeman();
						state = State.BANKING;
						return false;
					}
					
					if (waitEmptyCoal() && clickConveyer(conveyer)) {
						Tile randomBankTile = BANK_AREA.getRandomTile();
						ctx.input.move(randomBankTile.matrix(ctx).mapPoint());
						return waitItemGone(COAL_ID);
					}
				} else {
					getDispenser(false).hover();
					if (waitItemGone(BAR.oreId))
						return true;
					
					if (getChatBoxText().contains("You must ask the foreman's permission")) {
						handleForeman();
						state = State.BANKING;
						return false;
					}
				}
			}
		}
		
		return false;
	}
	
	final boolean clickDispenser(boolean done) {
		GameObject dispenser = getDispenser(done);
		for (int tries = 0; tries < 5; tries++) {
			if (dispenser.interact(dispenser.actions()[0]))
				return true;
		}
		
		return false;
	}
	
	final boolean waitForDispenser() {
		return Condition.wait(new Callable<Boolean>() {
			@Override
			public Boolean call() throws Exception {
				return dispenserScreenVis();
			}
				
		}, 100, 30);
	}
	
	final boolean dispenserScreenVis() {
		return ctx.widgets.widget(DISPENSER_SEL_ID).valid();
	}
	
	final boolean selectAll() {
		Widget widg = ctx.widgets.widget(DISPENSER_SEL_ID);
		Component comp = widg.component(BAR.dispenserId);
		
		for (int tries = 0; tries < 5; tries++) {
			if (comp.interact("Take")) {
				Condition.sleep(200);
				return true;
			}
		}
		
		return false;
	}
	
	final boolean selectGloves(int id) {
		ItemQuery<Item> items = ctx.inventory.select().id(id);
		if (items.isEmpty())
			return true;
		
		if (items.peek().interact("Wear")) {
			Condition.sleep(200);
			return true;
		}
		
		return false;
	}
	
	final int getCofferAmount() {
		return ctx.varpbits.varpbit(795) / 2;
	}
	
	final boolean clickCoffer() {
		GameObject coffer = getCoffer();
		if (coffer.inViewport() && coffer.interact("Use")) {
			return Condition.wait(new Callable<Boolean>() {
				@Override
				public Boolean call() throws Exception {
					return !ctx.chat.select().text("Cancel").isEmpty();
				}
			});
		}
		return false;
	}
	
	final boolean insertIntoCoffer(int amount) {
		return ctx.input.sendln("" + amount);
	}
	
	final int invMoney() {
		return ctx.inventory.select().id(GOLD_ID).count(true);
	}
	
	final boolean withdrawMoney(int amount) {
		if (walkToBank() && ctx.bank.open()) {
			if (invMoney() == 0 && ctx.inventory.select().count() == 28)
				ctx.bank.deposit(BAR.oreId, 1);
			
			return ctx.bank.withdraw(GOLD_ID, amount);
		}
		return false;
	}
	
	final boolean handleForeman() {
		if (invMoney() < 2500) {
			withdrawMoney(2500);
			ctx.bank.close();
		}
		
		return payForeman();
	}
	
	final boolean handlePayment() {
		for (int tries = 0; tries < 3; tries++) {
			if (clickCoffer()) {
				TextQuery<ChatOption> q = ctx.chat.select().text("Deposit coins.");
				if (q.isEmpty())
					return false;
				
				q.poll().select();
				
				Condition.sleep(1000);
				if (ctx.chat.canContinue()) {
					if (handleForeman() && clickCoffer()) {
						ctx.chat.select().text("Deposit coins.").poll().select();
						
						Condition.sleep(1000);
						
						return insertIntoCoffer(BLAST_FURNACE_AMOUNT);
					}
				} else
					return insertIntoCoffer(BLAST_FURNACE_AMOUNT);
			}
		}
		
		return false;
	}
	
	final Point getRandom(Point[] area) {
		return new Point(area[0].x + (int)(Math.random() * (area[1].x - area[0].x)), area[0].y + (int)(Math.random() * (area[1].y - area[0].y)));
	}
	
	final boolean waitTillChatOptionText(String text) {
		return Condition.wait(new Callable<Boolean>() {
			@Override
			public Boolean call() throws Exception {
				return !ctx.chat.select().text(text).isEmpty();
			}
		}, 100, 30);
	}
	
	final boolean payForeman() {
		for (int tries = 0; tries < 3; tries++) {
			Npc foreman = ctx.npcs.select().id(FOREMAN_ID).peek();
			if (!foreman.inViewport())
				ctx.movement.step(FOREMAN_AREA.getRandomTile());
	
			if (waitTillReasonableStop(1) && foreman.interact("Pay") && waitTillChatOptionText("Yes")) {
				return ctx.chat.select().text("Yes").peek().select();
			}
		}
		
		return false;
	}
	
	final boolean waitTillDone() {
		return Condition.wait(new Callable<Boolean>() {
			@Override
			public Boolean call() throws Exception {
				return !ctx.objects.select().id(DISPENSER_DONE_IDS).isEmpty();
			}
		});
	}
	
	final boolean handleDispenser() {
		for (int tries = 0; tries < 5; tries++) {
			//if (waitTillDone()) {
				if (!dispenserScreenVis())
					if (!clickDispenser(false))
						continue;
				
				ctx.input.move(getRandom(DISPENSER_MOUSE_MOVE_AREA));
				
				if (waitForDispenser() && selectAll())
					return true;
			//}
			
			if (getChatBoxText().contains("Smithing"))
				ctx.chat.clickContinue();
		}
		return false;
	}
	
	final int randomRange(int[] minAndMax) {
		return (int)(minAndMax[0] + (Math.random() * (minAndMax[1] - minAndMax[0])));
	}
	
	final int getRandomAngle(int[] yaws) {
		int r = randomRange(yaws);
		if (r < 0)
			r += 360;
		return r;
	}
	
	final boolean run() {
		if (ctx.movement.energyLevel() > 15)
			return ctx.movement.running(true);
		
		return false;
	}
	
	final boolean waitEmptyCoal() {
		for (int tries = 0; tries < 5; tries++) {
			ctx.inventory.select().id(COAL_BAG_ID).peek().interact("Empty");
			if (Condition.wait(new Callable<Boolean>() {
				@Override
				public Boolean call() throws Exception {
					return ctx.inventory.select().id(COAL_ID).count() > 0;
				}
			}, 50, 30)) {
				return true;
			} else if (ctx.chat.clickContinue()) {
				return false;
			}
		}
		return false;
	}
	
	@Override
	public void start() {
		ctx.camera.pitch(true);
	}
	
	@Override
	public void poll() {
		run();
		
		switch (state) {
		case WALK_TO_BANK:
			if (walkToBank())
				state = State.BANKING;
			break;
		case BANKING:
			if (bank()) {
				if (getCofferAmount() < MIN_COFFER_AMOUNT) {
					ctx.bank.close();
					state = State.PAYMENT;
				} else {
					state = State.WALK_TO_BLAST;
				}
			}
			break;
		case PAYMENT:
			if (handlePayment()) {
				state = State.BANKING;
			} else
				state = State.BANKING;
			
			break;
		case WALK_TO_BLAST:
			if (walkToBlastArea()) {
				state = State.USE_CONVEYER;
			}
			break;
		case USE_CONVEYER:
			if (carryMode == CarryMode.COAL) {
				if (useConveyer()) {
					state = State.WALK_TO_BANK;
					carryMode = CarryMode.ORE;
				} else {
					state = State.WALK_TO_BANK;
					carryMode = CarryMode.ORE;
				}
			} else {
				if (useConveyer()) {
					Condition.sleep(300);
					state = State.DISPENSER;
					carryMode = CarryMode.COAL;
				} else {
					state = State.DISPENSER;
					carryMode = CarryMode.COAL;
				}
			}
			break;
		case DISPENSER:
			if (handleDispenser()) {
				state = State.WALK_TO_BANK;
			}
			break;
		}
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
		g2.drawString("Time running: " + formatInterval(getRuntime(), false), 10, 30);
		g2.drawString("Profit per hour: " + ((int)Math.round(getProfitPerHour() / 1000)), 10, 60);
		g2.drawString("Bars made: " + barsMade, 10, 90);
		g2.drawString("Bars per hour: " + getBarsPerHour(), 10, 120);
	}
}
