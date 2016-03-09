package scanner.dao.utils;

public class SQLColumn {
	private String name;
	private String type;
	private int typecode;
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	public String getType() {
		return type;
	}
	public void setType(String type) {
		this.type = type;
	}
	public int getTypeCode() {
		return typecode;
	}
	public void setTypeCode(int typecode) {
		this.typecode = typecode;
	}
	@Override
	public String toString() {
		return "SQLColumn [name=" + name + ", type=" + type + ", typecode=" + typecode + "]";
	}
	
}
