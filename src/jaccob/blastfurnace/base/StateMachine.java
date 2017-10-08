package jaccob.blastfurnace.base;

import java.util.HashMap;
import java.util.Map;

public class StateMachine {
	public Statee current;

	public StateMachine() {
		
	}
	
	/*public void registerState(State state, int nextState) {
	}
	
	public void setState(State state) {
		current = state;
	}
	
	public void setState(int state) {
		current = states.get(state);
	}
	
	public void next(StateData entity) {
		current = states.get(nextStates.get(current.id()));
		current.run(entity);
	}*/
}
