package app.batchimagedownloader;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by puttaraju on 16-05-2015.
 */

public class Group {

	public String string;
	public final List<String> children = new ArrayList<String>();

	public Group(String string) {
		this.string = string;
	}

}