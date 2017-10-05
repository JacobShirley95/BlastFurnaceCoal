package jaccob.blastfurnace.base;

import java.util.concurrent.Callable;

import org.powerbot.script.Condition;
import org.powerbot.script.rt4.ClientContext;


public class JaccobMethods {
	private ClientContext ctx;
	
	public JaccobMethods(ClientContext ctx) {
		this.ctx = ctx;
	}
	
	public final int wait(int duration, int freq, Callable<Boolean>... callables) {
		MultiCallable mC = new MultiCallable(callables);
		
		if (Condition.wait(mC, duration, freq)) {
			return mC.result;
		}
		
		return -1;
	}
	
	public final int wait(Callable<Boolean>... callables) {
		return wait(50, 100, callables);
	}
	
	public final boolean waitTillReasonableStop(final int dist) {
		Condition.sleep(500);
		return Condition.wait(new Callable<Boolean>() {
			@Override
			public Boolean call() throws Exception {
				return distanceToDest() < dist;
			}
		}, 100, 60);
	}
	
	public final int distanceToDest() {
		return ctx.movement.distance(ctx.movement.destination());
	}
	
	public final int tryUntil(int tries, Callable<Boolean>... callables) {
		try {
			for (int t = 0; t < tries; t++) {
				int i = 0;
				for (Callable<Boolean> cb : callables) {
					if (cb.call())
						return i;
					i++;
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		return -1;
	}
}
