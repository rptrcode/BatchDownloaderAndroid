package app.batchdownloader;

import android.app.Activity;
import android.util.SparseArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseExpandableListAdapter;
import android.widget.CheckedTextView;
import android.widget.TextView;

public class ExpandableListAdapter extends BaseExpandableListAdapter {
	private final SparseArray<Group> groups;
	private LayoutInflater inflater;
	private Activity activity;


	public ExpandableListAdapter(Activity act, SparseArray<Group> groups) {
		activity = act;
		this.groups = groups;
		inflater = act.getLayoutInflater();
	}

	@Override
	public Object getChild(int groupPosition, int childPosition) {
		return groups.get(groupPosition).children().get(childPosition);
	}

	@Override
	public long getChildId(int groupPosition, int childPosition) {
		return 0;
	}

	@Override
	public View getChildView(int groupPosition, final int childPosition,
	                         boolean isLastChild, View convertView, ViewGroup parent) {
		final FileInfo child = (FileInfo) getChild(groupPosition, childPosition);
		if (child == null)
			return null;
		if (convertView == null) {
			convertView = inflater.inflate(R.layout.row_layout, null);
		}
		((TextView) convertView.findViewById(R.id.detail1)).setText(child.getFilename());
		((TextView) convertView.findViewById(R.id.detail2)).setText(child.getError());
		convertView.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				new ImageViewer(activity, child.getFilepath(), child.getFilename()).show();
			}
		});
		return convertView;
	}


	@Override
	public int getChildrenCount(int groupPosition) {
		return groups.get(groupPosition).size();
	}

	@Override
	public Object getGroup(int groupPosition) {
		return groups.get(groupPosition);
	}

	@Override
	public int getGroupCount() {
		return groups.size();
	}

	@Override
	public void onGroupCollapsed(int groupPosition) {
		super.onGroupCollapsed(groupPosition);
	}

	@Override
	public void onGroupExpanded(int groupPosition) {
		super.onGroupExpanded(groupPosition);
	}

	@Override
	public long getGroupId(int groupPosition) {
		return 0;
	}

	@Override
	public View getGroupView(int groupPosition, boolean isExpanded,
	                         View convertView, ViewGroup parent) {
		if (convertView == null) {
			convertView = inflater.inflate(R.layout.listrow_group, null);
		}
		Group group = (Group) getGroup(groupPosition);
		((CheckedTextView) convertView).setText(group.getGroupName());
		((CheckedTextView) convertView).setChecked(isExpanded);//		
		return convertView;
	}

	@Override
	public boolean hasStableIds() {
		return false;
	}

	@Override
	public boolean isChildSelectable(int groupPosition, int childPosition) {
		return false;
	}

}