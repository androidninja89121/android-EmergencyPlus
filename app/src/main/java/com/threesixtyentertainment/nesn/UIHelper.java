package com.threesixtyentertainment.nesn;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.widget.Toast;

public class UIHelper {

    public static void showAlert(final Activity context, int msgStringId, final String actionTarget) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setMessage(context.getResources().getText(msgStringId))
            .setCancelable(false)
            .setPositiveButton("Settings", new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int id) {
                    context.startActivity(new Intent(actionTarget));
                    dialog.dismiss();
                }
            })
            .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int id) {
                    dialog.cancel();
                }
            });

        builder.create().show();
    }

    public static void show(String msg) {
        Toast.makeText(MyApp.getContext(), msg, Toast.LENGTH_SHORT).show();
    }

}
