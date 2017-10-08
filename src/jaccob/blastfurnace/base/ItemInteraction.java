package jaccob.blastfurnace.base;

import org.powerbot.script.rt4.ClientContext;
import org.powerbot.script.rt4.Item;

public class ItemInteraction implements Interaction {
	private Item item;
	private String option = null;

	public ItemInteraction(Item item) {
		this.item = item;
	}
	
	public ItemInteraction(ClientContext ctx, int itemId, boolean bank) {
		this.item = bank ? ctx.bank.select().id(itemId).peek() : ctx.inventory.select().id(itemId).peek();
	}
	
	public ItemInteraction(Item item, String option) {
		this.item = item;
		this.option = option;
	}
	
	@Override
	public boolean prepare() {
		return this.item.hover();
	}

	@Override
	public boolean execute() {
		return this.item.interact(option);
	}
}
