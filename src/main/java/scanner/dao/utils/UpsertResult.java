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
	public String toString() {
		return "Upserts results: "+inserts +" inserted, "+updates+" updated";
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
