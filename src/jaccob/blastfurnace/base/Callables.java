package jaccob.blastfurnace.base;

import java.util.concurrent.Callable;

import org.powerbot.script.rt4.ClientContext;

public class Callables {
	private ClientContext ctx;

	public Callables(ClientContext ctx) {
		this.ctx = ctx;
	}
	
	public final Callable<Boolean> itemGoneCb(int id) {
		return new Callable<Boolean>() {
			@Override
			public Boolean call() throws Exception {
				return ctx.inventory.select().id(id).count() == 0;
			}
		};
	}
	
	public final Callable<Boolean> widgetVisible(int id) {
		return new Callable<Boolean>() {
			@Override
			public Boolean call() throws Exception {
				return ctx.widgets.widget(id).valid();
			}
		};
	}
}
