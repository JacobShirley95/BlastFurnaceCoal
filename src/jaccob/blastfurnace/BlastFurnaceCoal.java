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
	
	State state = State.WALK_TO_BANK;
	CarryMode carryMode = BAR.coalRatio > 1 ? CarryMode.COAL : CarryMode.ORE;
	int oreTrip = 0;
	
	int barProfit = getBarProfit(BAR);
	int barsMade = 0;
	
	Bank bank;
	Callables callables;
	
	StateMachine mch = new StateMachine();
	
	final boolean needForeman() {
		return ctx.skills.level(Constants.SKILLS_SMITHING) < 60;
	}
	
	final boolean finished() {
		return ctx.bank.select().id(BAR.oreId).count(true) == 0 ||
			   ctx.bank.select().id(COAL_ID).count(true) == 0 ||
			   ctx.bank.select().id(GOLD_ID).count(true) == 0;
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

		return (int) (3600000d / (long) runTime * (double) (barsMade));
	}
	
	final boolean walkToBank() {
		Tile bankPos = BANK_AREA.getRandomTile();

		TileInteraction interaction = new TileInteraction(bankPos, ctx);
		
		if (bankPos.distanceTo(ctx.players.local().tile()) > 5) {
			ctx.movement.step(bankPos);
			hoverBankArea();
		}

		return waitTillReasonableStop(5);
	}
	
	final boolean hoverBlastArea() {
		return ctx.input.move(getRandom(BANK_MOUSE_MOVE_AREA));
	}
	
	final boolean hoverBankArea() {
		return ctx.input.move(getRandom(BANK_MOUSE_MOVE_AREA));
	}
	
	final boolean clickCoalBag() {
		ctx.game.tab(Tab.INVENTORY);
		return ctx.inventory.select().id(COAL_BAG_ID).peek().interact("Fill");
	}
	
	final Component bankCloseComponent() {
		return ctx.widgets.widget(Constants.BANK_WIDGET).component(Constants.BANK_MASTER).component(Constants.BANK_CLOSE);
	}
	
	final int fillCoalBag() {
		for (int tries = 0; tries < 5; tries++) {
			if (clickCoalBag()) {
				ctx.bank.nearest().tile().matrix(ctx).hover();
				
				return waitMultiple(callables.itemGoneCb(COAL_ID), callables.widgetVisible(COAL_BAG_FULL_ID));
			}
		}
		return -1;
	}
	
	final boolean coalFromBank() {
		for (int tries = 0; tries < 5; tries++) {
			if (bank.withdrawSmart(COAL_ID, Amount.ALL, new Callable<Boolean>() {
				@Override
				public Boolean call() throws Exception {
					return bankCloseComponent().hover();
				}
			})) {
				break;
			}
		}
		
		for (int tries = 0; tries < 5; tries++) {
			if (ctx.bank.close()) {
				int result = fillCoalBag();
				
				if (result == 0) {
					ctx.bank.open();
					return true;
				} else if (result == 1) {
					return true;
				}
			}
		}
		
		return false;
	}
	
	final boolean cleverBankOpen() {
		for (int tries = 0; tries < 5; tries++) {
			boolean opened = ctx.bank.opened();
			if (!opened) {
				if (ctx.bank.nearest().tile().matrix(ctx).interact("Use")) {
					ctx.inventory.select().id(BAR.barId).peek().hover();
					if (Condition.wait(new Callable<Boolean>() {
						@Override
						public Boolean call() throws Exception {
							return ctx.bank.opened();
						}
					}, 50, 30))
						continue;
				}
			}
			
			if (ctx.bank.opened()) {
				
				return true;
			}	
		}
		
		return false;
	}
	
	final boolean bank() {
		barsMade += ctx.inventory.select().id(BAR.barId).count();
		
		for (int tries2 = 0; tries2 < 5; tries2++) {
			if (cleverBankOpen()) {
				bank.depositSmart(BAR.barId, Amount.ALL.getValue(), new Callable<Boolean>() {
					@Override
					public Boolean call() throws Exception {
						return ctx.bank.select().id(COAL_ID).peek().hover();
					}
				});
				
				ctx.bank.depositAllExcept(COAL_BAG_ID);
				
				if (finished()) {
					ctx.controller.stop();
					return true;
				}
				
				if (getCofferAmount() < MIN_COFFER_AMOUNT) {
					withdrawMoney(BLAST_FURNACE_AMOUNT);
					ctx.bank.close();
					return true;
				}
				
				if (ctx.inventory.select().id(COAL_BAG_ID).isEmpty())
					ctx.bank.withdraw(COAL_BAG_ID, 1);
				
				if (coalFromBank()) {
					System.out.println("SDFSDF");
				}
				
				if (carryMode == CarryMode.COAL && ctx.inventory.select().id(COAL_ID).count() == 27)
					return true;
				else
					ctx.bank.open();
				
				ctx.bank.deposit(COAL_ID, Amount.ALL);

				if (ctx.movement.energyLevel() < 20) {
					if (ctx.inventory.select().count() == 28) {
						ctx.bank.deposit(BAR.oreId, 1);
						ctx.bank.deposit(COAL_ID, 1);
					}
					
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
	
				if (bank.withdrawSmart(carryMode == CarryMode.COAL ? COAL_ID : BAR.oreId, Amount.ALL, new Callable<Boolean>() {
					@Override
					public Boolean call() throws Exception {
						return ctx.input.move(new Tile(1939 + (int)Math.round(Math.random() * 3), 4967).matrix(ctx).mapPoint());
					}
				})) {
					return true;
				}
				
			} else
				Condition.sleep(500);
		}
		
		return false;
	}
	
	static class MultiCallable implements Callable<Boolean> {
		public int result = -1;
		private Callable<Boolean>[] callables;

		public MultiCallable(Callable<Boolean>... callables) {
			this.callables = callables;
		}
		
		@Override
		public Boolean call() throws Exception {
			for (int i = 0; i < callables.length; i++) {
				if (callables[i].call()) {
					result = i;
					return true;
				}
			}
			
			return false;
		}
	}
	
	final int waitMultiple(int duration, int freq, Callable<Boolean>... callables) {
		MultiCallable mC = new MultiCallable(callables);
		
		if (Condition.wait(mC, duration, freq)) {
			return mC.result;
		}
		
		return -1;
	}
	
	final int waitMultiple(Callable<Boolean>... callables) {
		return waitMultiple(50, 100, callables);
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
	
	final boolean walkToDispenser() {
		Tile pos = getDispenser(false).tile();

		if (pos.distanceTo(ctx.players.local().tile()) > 5) {
			ctx.movement.step(pos);
			hoverBankArea();
		}

		return waitTillReasonableStop(1);
	}
	
	final boolean clearDispenser() {
		for (int tries = 0; tries < 5; tries++) {
			if (walkToBank() && cleverBankOpen()) {
				if (ctx.bank.depositAllExcept(COAL_BAG_ID)) {
					if (walkToDispenser()) {
						if (handleDispenser())
							return true;
					}
				}
			}
		}
		
		return false;
	}
	
	final boolean conveyAndCheck(int id) {
		int res = waitMultiple(callables.itemGoneCb(id), callables.widgetVisible(NEED_TO_SMELT_FIRST_ID));
		if (res == 1) {
			String txt = getChatBoxText();
			System.out.println(txt);
			if (txt.contains("permission")) {
				handleForeman();
				System.out.println("done foreman");
			} else {
				clearDispenser();
			}
			state = State.WALK_TO_BANK;
			return false;
			//return true;
		}
		
		System.out.println("RESULT: " + res);
		
		if (res == -1)
			return false;
		
		return true;
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
				int targetId = carryMode == CarryMode.COAL ? COAL_ID : BAR.oreId;
				
				ctx.inventory.select().id(COAL_BAG_ID).peek().hover();
				if (!conveyAndCheck(targetId))
					return false;
				
				ctx.inventory.select().id(COAL_BAG_ID).peek().interact("Empty");
				conveyer.hover();
				
				if (waitEmptyCoal() && clickConveyer(conveyer)) {
					if (!conveyAndCheck(COAL_ID))
						return false;
					
					if (carryMode == CarryMode.COAL) {
						Tile randomBankTile = BANK_AREA.getRandomTile();
						ctx.input.move(randomBankTile.matrix(ctx).mapPoint());
					} else {
						getDispenser(false).hover();
					}
					return waitMultiple(callables.itemGoneCb(COAL_ID)) == 0;
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
		if (walkToBank()) {
			for (int tries = 0; tries < 5; tries++) {
				if (cleverBankOpen()) {
					if (invMoney() == 0 && ctx.inventory.select().count() == 28) {
						if (carryMode == CarryMode.COAL) {
							ctx.bank.deposit(COAL_ID, 1);
						} else {
							ctx.bank.deposit(BAR.oreId, 1);
						}
					}
					
					return ctx.bank.withdraw(GOLD_ID, amount);
				}
			}
			
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
			
			int money = invMoney();
			if (waitTillReasonableStop(1) && foreman.interact("Pay")) {
				if (Condition.wait(new Callable<Boolean>() {

					@Override
					public Boolean call() throws Exception {
						return invMoney() < money;
					}
					
				}, 50, 20))
					return true;
				
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
				
				if (waitForDispenser()) {
					if (!selectAll()) {
						ctx.widgets.close(ctx.widgets.widget(DISPENSER_SEL_ID));
						continue;
					} else {
						ctx.input.move(BANK_AREA.getRandomTile().matrix(ctx).mapPoint());
						
						if (!ctx.inventory.select().id(BAR.barId).isEmpty()) {
							return true;
						}
					}
				}
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
			Widget wi = ctx.widgets.widget(NEED_TO_SMELT_FIRST_ID);
			if (Condition.wait(new Callable<Boolean>() {
				@Override
				public Boolean call() throws Exception {
					return ctx.inventory.select().id(COAL_ID).count() > 0 || wi.valid();
				}
			}, 50, 30)) {
				if (wi.valid())
					return false;
				
				return true;
			} else {
				ctx.inventory.select().id(COAL_BAG_ID).peek().interact("Empty");
			}
		}
		return false;
	}
	
	@Override
	public void start() {
		bank = new Bank(ctx);
		callables = new Callables(ctx);
		
		ctx.camera.pitch(true);
	}
	
	@Override
	public void poll() {
		//System.out.println(ctx.varpbits.varpbit(545)+", "+ctx.varpbits.varpbit(543));
		
		while (!ctx.controller.isStopping()) {
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
				if (walkToBlastArea())
					state = State.USE_CONVEYER;
				break;

			case USE_CONVEYER:
				if (carryMode == CarryMode.COAL) {
					useConveyer();
					state = State.WALK_TO_BANK;
					carryMode = CarryMode.ORE;
				} else {
					if (useConveyer()) {
						Condition.sleep(300);
						state = State.DISPENSER;
						oreTrip++;
						
						if (BAR.oreTrips > 1 && oreTrip == BAR.oreTrips) {
							carryMode = CarryMode.COAL;
							oreTrip = 0;
						}
					}
				}
				break;
			case DISPENSER:
				handleDispenser();
				state = State.WALK_TO_BANK;
				
				break;
			}
		}//*/
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
		g2.drawString("Bars made: " + barsMade, 10, 90);
		g2.drawString("Bars p/h: " + barsPerHour, 10, 120);
		g2.drawString("XP p/h: " + getXPPerHour(), 10, 150);
	}
}
