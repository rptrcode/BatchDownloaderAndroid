package app.batchdownloader;

import java.util.ArrayList;
import java.util.List;

public class Group {

	public final List<MainActivity.FileInfo> children = new ArrayList<>();
	private String groupName;

	public Group(String string) {
		this.groupName = string;
	}

	public String getGroupName() {
		return groupName;
	}

	public void clear() {
		children.clear();
	}

	public int size() {
		return children.size();
	}
}