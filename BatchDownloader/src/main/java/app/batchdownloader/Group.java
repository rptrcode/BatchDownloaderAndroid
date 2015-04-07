package app.batchdownloader;

import java.util.ArrayList;
import java.util.List;

public class Group {

	public final List<String> children = new ArrayList<String>();
	public String string;

	public Group(String string) {
		this.string = string;
	}

	public void clear() {
		children.clear();
	}

	public int size() {
		return children.size();
	}
}