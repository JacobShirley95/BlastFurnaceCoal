package jaccob.blastfurnace;

import java.awt.Point;

import org.powerbot.script.Area;
import org.powerbot.script.Tile;

public class Defs {
	public enum CarryMode {
		COAL, ORE
	}
	
	public enum BarType {
		STEEL(2353, 440, true, 110, 1),
		MITHRIL(2359, 447, true, 111, 2),
		ADAMANTITE(2361, 449, true, 112, 3),
		RUNITE(2363, 451, true, 113, 4);
		
		public int barId;
		public int oreId;
		public boolean coal;
		public int dispenserId;
		public int coalRatio;
		public int oreTrips;
		public int coalTrips;
		
		BarType(int barId, int oreId, boolean coal, int dispenserId, int coalRatio) {
			this.barId = barId;
			this.oreId = oreId;
			this.coal = coal;
			this.dispenserId = dispenserId;
			this.coalRatio = coalRatio;
			
			if (coalRatio % 2 == 1) {
				coalRatio -= 2;
			}
			
			this.oreTrips = coalRatio;
		}
	}
	
	public final static BarType BAR = BarType.ADAMANTITE;
	
	public final static int COAL_ID = 453;
	public final static int COAL_BAG_ID = 12019;
	
	public final static int GOLD_ID = 995;
	
	public final static int[] STAMINA_POTS = {12631, 12629, 12625, 12627};
	
	public final static int COAL_BAG_FULL_ID = 193;
	public final static int NEED_TO_SMELT_FIRST_ID = 229;
	
	public final static int[] COFFER_IDS = {29328, 29329};
	public final static int[] COFFER_BOUNDS = {-24, 36, 10, 0, -32, 28};
	public final static int MIN_COFFER_AMOUNT = 3000;
	
	public final static Tile FURNACE_TILE = new Tile(1939, 4963);
	
	public final static int BLAST_FURNACE_AMOUNT = 60000;
	
	public final static Area BLAST_AREA = new Area(new Tile(1939, 4967), new Tile(1942, 4967));
	public final static int[] BLAST_YAWS = {261, 241};
	public final static int BLAST_CONVEYER_ID = 9100;
	public final static int[] BLAST_CONVEYER_BOUNDS = {-36, 20, -240, -228, -72, 32};
			
	public final static int[] DISPENSER_IDS = {9093, 9094, 9095, 9096};
	public final static int[] DISPENSER_DONE_IDS = {9094, 9095, 9096};
	public final static int[] DISPENSER_BOUNDS = {-108, -44, -96, -36, -32, 32};
	
	public final static int DISPENSER_SEL_ID = 28;
	
	public final static int CHAT_BOX_TEXT_ID = 229;
	public final static int CHAT_BOX_TEXT_COMP_ID = 0;
	
	public final static Area FOREMAN_AREA = new Area(new Tile(1944, 4958), new Tile(1946, 4960));
	public final static int FOREMAN_ID = 2923;
	
	public final static Point[] BLAST_MOUSE_MOVE_AREA = {new Point(7, 134), new Point(59, 218)};
	public final static Point[] DISPENSER_MOUSE_MOVE_AREA = {new Point(194, 64), new Point(395, 264)};
	public final static Point[] BANK_MOUSE_MOVE_AREA = {new Point(302, 29), new Point(331, 81)};
	
	public final static Area BANK_AREA = new Area(new Tile(1947, 4955), new Tile(1948, 4957));
}
