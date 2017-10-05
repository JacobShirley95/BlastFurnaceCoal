package jaccob.blastfurnace;

public class Defs {
	public enum CarryMode {
		COAL, ORE
	}
	
	public enum BarType {
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
	
	public final static int COAL_ID = 453;
	public final static int COAL_BAG_ID = 12019;
	
	public final static int GOLD_ID = 995;
	
	public final static int[] STAMINA_POTS = {12631, 12629, 12625, 12627};
}
