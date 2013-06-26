/**
 * 
 */
package com.jeffmcknight.sfbikeshuttle;

//import java.util.zip.Inflater;

//import android.app.Activity;
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
//import android.opengl.Visibility;
import android.os.Bundle;
import android.view.LayoutInflater;
//import android.view.View;
import android.widget.NumberPicker;

//******************** Class - TweetRiderCountDialogFragment ********************
/**
 * @author jeffmcknight
 *
 */
public class TweetRiderCountDialogFragment extends DialogFragment
{
	int intCurrentRiderCount;
	
	// ******************** newInstance() ********************    
	public static TweetRiderCountDialogFragment newInstance(int title, int intRiderCount) 
	{
	    TweetRiderCountDialogFragment frag = new TweetRiderCountDialogFragment();
	    Bundle args = new Bundle();
	    args.putInt("title", title);
	    args.putInt("ridercount", intRiderCount);
	    frag.setArguments(args);
	    return frag;
	}

	NumberPicker pickerRiderQueue;
	
	// ******************** getPickerRiderQueue() ********************    
	 public NumberPicker getPickerRiderQueue()
	{
		return pickerRiderQueue;
	}

		// ******************** getIntCurrentRiderCount() ********************    
	/**
	 * @return the intCurrentRiderCount
	 */
	protected int getIntCurrentRiderCount()
	{
		return intCurrentRiderCount;
	}

	// ******************** setIntCurrentRiderCount() ********************    
	/**
	 * @param intCurrentRiderCount the intCurrentRiderCount to set
	 */
	protected void setIntCurrentRiderCount(int intCurrentRiderCount)
	{
		this.intCurrentRiderCount = intCurrentRiderCount;
	}

	// ******************** onCreateDialog() ********************    
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) 
    {
    	int title = getArguments().getInt("title");
    	intCurrentRiderCount = getArguments().getInt("title");
    	Builder builderTemp;
    	LayoutInflater inflater;
//    	View viewDialog;

        // Get the layout inflater
        inflater = getActivity().getLayoutInflater();
        
        // Inflate the XML to the pickerRiderQueue object
        // Pass null as the parent view because its going in the dialog layout
        pickerRiderQueue = (NumberPicker) inflater.inflate(R.layout.dialog_signin, null);
        
        pickerRiderQueue.setMinValue(0);
        pickerRiderQueue.setMaxValue(20);
        pickerRiderQueue.setValue(intCurrentRiderCount);
        pickerRiderQueue.setWrapSelectorWheel(false);
        pickerRiderQueue.setDescendantFocusability(NumberPicker.FOCUS_BLOCK_DESCENDANTS); // blocks soft keyboard from displaying in AlertDialog

        // Get the builder
        builderTemp = new AlertDialog.Builder(getActivity());

        // Set the NumberPicker layout for the dialog
        builderTemp.setView(pickerRiderQueue);
    	
    	// Set the title and positive/negative buttons
    	builderTemp.setTitle(title);
    	builderTemp.setPositiveButton(R.string.alert_dialog_ok,
    		new DialogInterface.OnClickListener() 
    		{
    			public void onClick(DialogInterface dialog, int whichButton) 
    			{
    				((ShuttleMapActivity)getActivity()).setIntRiderCount(pickerRiderQueue.getValue());
    				((ShuttleMapActivity)getActivity()).doRiderCountPositiveClick();
    			}
    		} 
    	);
    	builderTemp.setNegativeButton(R.string.alert_dialog_cancel,
    		new DialogInterface.OnClickListener() 
    		{
    			public void onClick(DialogInterface dialog, int whichButton) 
    			{
    				((ShuttleMapActivity)getActivity()).doRiderCountNegativeClick();
    			}
    		}
    	);
//    	pickerRiderQueue = new NumberPicker(getActivity());
//		builderTemp.setView(pickerRiderQueue);

    	return builderTemp.create();
    	
/*    	return new AlertDialog.Builder(getActivity())
    	//                .setIcon(R.drawable.alert_dialog_icon)
    	.setTitle(title)
    	.setPositiveButton
    		(R.string.alert_dialog_ok,
    			new DialogInterface.OnClickListener() 
    			{
    				public void onClick(DialogInterface dialog, int whichButton) 
    				{
    					((ShuttleMapActivity)getActivity()).doPositiveClick();
    				}
    			} 
    		)
    	.setNegativeButton
    		(R.string.alert_dialog_cancel,
    			new DialogInterface.OnClickListener() 
    			{
    				public void onClick(DialogInterface dialog, int whichButton) 
    				{
    					((ShuttleMapActivity)getActivity()).doNegativeClick();
    				}
    			}
    		)
    		.create();
*/    	
    } // END ******************** onCreateDialog() ********************
} // END ******************** Class - ShuttleMapActivity ********************
