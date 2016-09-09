package unipd.elia.delta.androidsharedlib;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Handler;
import android.os.Looper;
import android.view.WindowManager;

import java.util.concurrent.Semaphore;

/**
 * Created by Elia on 25/06/2015.
 */
public class DialogHelper {
    int pressedButtonID;
    private final Semaphore dialogSemaphore = new Semaphore(0, true);
    private Runnable mMyDialog;

    private Context context;
    private String message;

    public DialogHelper(final Context context, final String message, final String positiveButtonText, final String negativeButtonText, final String neutralButtonText){
        this.context = context;
        this.message = message;

        mMyDialog = new Runnable()
        {
            public void run()
            {
                AlertDialog errorDialog = new AlertDialog.Builder(context).create();
                errorDialog.getWindow().setType(WindowManager.LayoutParams.TYPE_SYSTEM_ALERT);
                errorDialog.setMessage(message);

                if(positiveButtonText != null) {
                    errorDialog.setButton(DialogInterface.BUTTON_POSITIVE, positiveButtonText, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            pressedButtonID = DialogInterface.BUTTON_POSITIVE;
                            dialogSemaphore.release();
                        }
                    });
                }

                if(negativeButtonText != null) {
                    errorDialog.setButton(DialogInterface.BUTTON_NEGATIVE, negativeButtonText, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            pressedButtonID = DialogInterface.BUTTON_NEGATIVE;
                            dialogSemaphore.release();
                        }
                    });
                }

                if(neutralButtonText != null) {
                    errorDialog.setButton(DialogInterface.BUTTON_NEUTRAL, neutralButtonText, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            pressedButtonID = DialogInterface.BUTTON_NEUTRAL;
                            dialogSemaphore.release();
                        }
                    });
                }

                errorDialog.setCancelable(false);
                errorDialog.show();
            }
        };
    }



    public int ShowMyModalDialog()  //should be called from non-UI thread
    {
        new Handler(Looper.getMainLooper()).post(mMyDialog);

        try
        {
            dialogSemaphore.acquire();
        }
        catch (InterruptedException e)
        {
        }
        return pressedButtonID;
    }
}
