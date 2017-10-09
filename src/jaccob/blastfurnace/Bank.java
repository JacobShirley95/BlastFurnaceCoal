package jaccob.blastfurnace;

import java.util.concurrent.Callable;

import org.powerbot.script.Condition;
import org.powerbot.script.Filter;
import org.powerbot.script.MenuCommand;
import org.powerbot.script.rt4.ClientAccessor;
import org.powerbot.script.rt4.ClientContext;
import org.powerbot.script.rt4.Constants;
import org.powerbot.script.rt4.Item;

import jaccob.blastfurnace.base.Interaction;

import org.powerbot.script.rt4.Bank.Amount;

public class Bank extends ClientAccessor{

	public Bank(ClientContext clx) {
		super(clx);
	}
	
	private boolean check(final Item item, final int amt) {
		item.hover();
		Condition.wait(new Condition.Check() {
			@Override
			public boolean poll() {
				return ctx.menu.indexOf(new Filter<MenuCommand>() {
					@Override
					public boolean accept(final MenuCommand command) {
						return command.action.startsWith("Withdraw") || command.action.startsWith("Deposit");
					}
				}) != -1;
			}
		}, 20, 10);
		final String s = "-".concat(Integer.toString(amt)) + " ";
		for (final String a : ctx.menu.items()) {
			if (a.contains(s)) {
				return true;
			}
		}
		return false;
	}
	
	public boolean depositAllExcept(final int... ids) {
		return depositAllExcept(new Filter<Item>() {
			@Override
			public boolean accept(final Item item) {
				final int id = item.id();
				for (final int i : ids) {
					if (id == i) {
						return true;
					}
				}
				return false;
			}
		});
	}
	
	public boolean depositAllExcept(final Filter<Item> filter) {
		if (ctx.inventory.select().select(filter).count() == 0) {
			return ctx.bank.depositInventory();
		}
		for (final Item i : ctx.inventory.select().shuffle()) {
			if (filter.accept(i)) {
				continue;
			}
			depositSmart(i.id(), Amount.ALL.getValue(), null);
		}

		return ctx.inventory.select().count() == ctx.inventory.select(filter).count();
	}
	
	public final boolean cleverBankOpen(Interaction interaction) {
		for (int tries = 0; tries < 5; tries++) {
			boolean opened = ctx.bank.opened();
			if (!opened) {
				if (ctx.bank.nearest().tile().matrix(ctx).interact("Use")) {
					try {
						if (interaction != null)
							interaction.prepare();
						
						if (Condition.wait(new Callable<Boolean>() {
							@Override
							public Boolean call() throws Exception {
								return ctx.bank.opened();
							}
						}, 50, 30))
							continue;
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
			}
			
			if (ctx.bank.opened()) {
				
				return true;
			}	
		}
		
		return false;
	}
	
	public boolean bankSelect(final Item item, final Amount amount) {
		return bankSelectWithdraw(item, amount.getValue());
	}
	
	public boolean bankSelectWithdraw(final Item item, final int amount) {
		if (!ctx.bank.opened() || !item.valid() || amount < -1) {
			return false;
		}

		if (!ctx.widgets.scroll(
				ctx.widgets.widget(Constants.BANK_WIDGET).component(Constants.BANK_ITEMS),
				item.component(),
				ctx.widgets.widget(Constants.BANK_WIDGET).component(Constants.BANK_SCROLLBAR)
		)) {
			return false;
		}
		final int count = ctx.bank.select().id(item.id()).count(true);
		final String action;
		if (count == 1 || amount == 1) {
			action = "Withdraw-1";
		} else if (amount == 0 || count <= amount) {
			action = "Withdraw-All";
		} else if (amount == 5 || amount == 10) {
			action = "Withdraw-" + amount;
		} else if (amount == -1) {
			action = "Withdraw-All-but-1";
		} else if (check(item, amount)) {
			action = "Withdraw-" + amount;
		} else {
			action = "Withdraw-X";
		}
		if (item.contains(ctx.input.getLocation())) {
			if (!(ctx.menu.click(new Filter<MenuCommand>() {
				@Override
				public boolean accept(final MenuCommand command) {
					return command.action.equalsIgnoreCase(action);
				}
			}) || item.interact(action))) {
				return false;
			}
		} else if (!item.interact(action)) {
			return false;
		}
		if (action.endsWith("X")) {
			if (!Condition.wait(new Condition.Check() {
				@Override
				public boolean poll() {
					return ctx.widgets.widget(162).component(33).visible();
				}
			})) {
				return false;
			}
			Condition.sleep();
			ctx.input.sendln(amount + "");
		}
		return true;
	}
	
	public final boolean waitInvChanged(int start) {
		return Condition.wait(new Callable<Boolean>() {
			@Override
			public Boolean call() {
				return start != ctx.inventory.select().count(true);
			}
		}, 50, 20);
	}
	
	public final boolean bankSelectSmart(int id, int amount) {
		if (ctx.inventory.select().count() != 28) {
			return bankSelectWithdraw(ctx.bank.select().id(id).poll(), amount);
		}
		return false;
	}
	
	public final boolean withdrawSmart(int id, Amount amount, Interaction interaction) {
		return withdrawSmart(id, amount.getValue(), interaction);
	}
	
	public final boolean withdrawSmart(int id, int amount, Interaction interaction) {
		final int cache = ctx.inventory.select().count(true);
		
		if (ctx.inventory.count() == 28)
			return true;
		
		if (bankSelectWithdraw(ctx.bank.select().id(id).poll(), amount)) {
			try {
				interaction.prepare();
				if (waitInvChanged(cache))
					return true;
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		return false;
	}
	
	public final boolean waitBankOpen() {
		return Condition.wait(new Callable<Boolean>() {
			@Override
			public Boolean call() throws Exception {
				return ctx.bank.opened();
			}
		}, 100, 20);
	}
	
	public final boolean depositSmart(int id, int amount, Interaction interaction) {
		final int cache = ctx.inventory.select().id(id).count(true);
		if (cache == 0)
			return true;
		
		if (bankSelectDeposit(id, amount)) {
			if (interaction != null)
				interaction.prepare();
			if (waitInvChanged(cache))
				return true;
		}
		return false;
	}
	
	public boolean bankSelectDeposit(final int id, final Amount amount) {
		return bankSelectDeposit(id, amount.getValue());
	}

	/**
	 * Deposits an item with the provided id and amount.
	 *
	 * @param id     the id of the item
	 * @param amount the amount to deposit
	 * @return <tt>true</tt> if the item was deposited, does not determine if amount was matched; otherwise <tt>false</tt>
	 */
	public boolean bankSelectDeposit(final int id, final int amount) {
		if (!ctx.bank.opened() || amount < 0) {
			return false;
		}
		final Item item = ctx.inventory.select().id(id).poll();
		if (!item.valid()) {
			return false;
		}
		final int count = ctx.inventory.select().id(id).count(true);
		final String action;
		if (count == 1 || amount == 1) {
			action = "Deposit";
		} else if (amount == 0 || count <= amount) {
			action = "Deposit-All";
		} else if (amount == 5 || amount == 10) {
			action = "Deposit-" + amount;
		} else if (check(item, amount)) {
			action = "Deposit-" + amount;
		} else {
			action = "Deposit-X";
		}
		if (item.contains(ctx.input.getLocation())) {
			if (!(ctx.menu.click(new Filter<MenuCommand>() {
				@Override
				public boolean accept(final MenuCommand command) {
					return command.action.equalsIgnoreCase(action);
				}
			}) || item.interact(action))) {
				return false;
			}
		} else if (!item.interact(action)) {
			return false;
		}
		if (action.endsWith("X")) {
			if (!Condition.wait(new Condition.Check() {
				@Override
				public boolean poll() {
					return ctx.widgets.widget(162).component(33).visible();
				}
			})) {
				return false;
			}
			Condition.sleep(500);
			ctx.input.sendln(amount + "");
		}
		return true;
	}

}
