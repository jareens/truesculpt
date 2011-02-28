package truesculpt.ui.adapters;

import java.util.ArrayList;
import truesculpt.main.R;
import truesculpt.ui.adapters.HistoryAdapter.ViewHolder;
import truesculpt.ui.panels.OpenFilePanel.FileElem;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.TextView;

public class OpenFileAdapter extends BaseAdapter 
{
    private ArrayList<FileElem> mFileList=null;
    
	class ViewHolder
	{
		TextView title;
		ImageView image;
		Bitmap bmp;
	}
	
    public OpenFileAdapter(Context context, ArrayList<FileElem> fileList) 
    {
    	mFileList = fileList;
    	inflater = LayoutInflater.from(context);
    }

    public int getCount()
    {
        return mFileList.size();
    }

    public Object getItem(int position)
    {
        return mFileList.get(position);
    }

    public long getItemId(int position)
    {
        return position;
    }

    LayoutInflater inflater;
    
    // create a new ImageView for each item referenced by the Adapter
    public View getView(int position, View convertView, ViewGroup parent)
    {
		ViewHolder holder;
		
		FileElem elem=mFileList.get(position);
		
		if (convertView == null)
		{	
			holder = new ViewHolder();
			
			convertView = inflater.inflate(R.layout.openfileitem, null);
			convertView.setLayoutParams(new GridView.LayoutParams(200,200));
			convertView.setPadding(8, 8, 8, 8);
			holder.title = (TextView) convertView.findViewById(R.id.fileopentitle);
			holder.image = (ImageView) convertView.findViewById(R.id.fileopenimage);
			holder.image.setScaleType(ImageView.ScaleType.FIT_CENTER);	
			holder.bmp = BitmapFactory.decodeFile(elem.imagefilename);
			convertView.setTag(holder);
		} 
		else
		{
			holder = (ViewHolder) convertView.getTag();
		}         

		holder.title.setText(elem.name);			
		holder.image.setImageBitmap(holder.bmp);			
		holder.title.setEnabled(true);
		holder.title.setVisibility(View.VISIBLE);
		
        return convertView;
    } 
}