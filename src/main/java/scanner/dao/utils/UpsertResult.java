package scanner.dao.utils;

public class UpsertResult {
	private int updates=0;
	private int inserts=0;

	public UpsertResult() {
		// TODO Auto-generated constructor stub
	}
	
	public void updated() {
		this.updates++;
	}
	public void inserted() {
		this.inserts++;
	}
	public void addResult(int res) {
		switch (res) {
		case 0: 
			this.updated();
			break;
		case 1:
			this.inserted();
			break;
		default:
			//shouldn't occur
			break;
		}
		
	}
	public String toString(boolean updated) {
		String insrt = inserts +" inserted";
		String updt = updated?updates+" updated":updates + " already published and potentially conflicting records kept unchanged";
		String mesg = "Upserts results: "+insrt+", "+updt;
		return mesg;
	}

	/**
	 * @return the updates
	 */
	public int getUpdates() {
		return updates;
	}

	/**
	 * @return the inserts
	 */
	public int getInserts() {
		return inserts;
	}

}
