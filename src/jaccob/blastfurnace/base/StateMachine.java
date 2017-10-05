package jaccob.blastfurnace.base;

import java.util.HashMap;
import java.util.Map;

public class StateMachine {
	public State current;
	public Map<Integer, State> states = new HashMap<>();
	public Map<Integer, Integer> nextStates = new HashMap<>();
	
	public StateMachine() {
		
	}
	
	public void registerState(State state, int nextState) {
		states.put(state.id(), state);
		nextStates.put((Integer)state.id(), nextState);
	}
	
	public void setState(State state) {
		current = state;
	}
	
	public void setState(int state) {
		current = states.get(state);
	}
	
	public void next(StateEntity entity) {
		current = states.get(nextStates.get(current.id()));
		current.run(entity);
	}
}
