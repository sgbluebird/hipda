package net.jejer.hipda.utils;

import android.Manifest;
import android.app.Activity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.Color;
import android.graphics.Rect;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.FileProvider;
import android.support.v7.app.AlertDialog;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.inputmethod.InputMethodManager;
import android.widget.TextView;
import android.widget.Toast;

import net.jejer.hipda.BuildConfig;
import net.jejer.hipda.R;
import net.jejer.hipda.async.FileDownTask;
import net.jejer.hipda.bean.HiSettingsHelper;
import net.jejer.hipda.cache.ImageContainer;
import net.jejer.hipda.cache.ImageInfo;
import net.jejer.hipda.ui.HiApplication;
import net.jejer.hipda.ui.MainFrameActivity;
import net.jejer.hipda.ui.PostActivity;

import java.io.File;

/**
 * Created by GreenSkinMonster on 2016-04-05.
 */
public class UIUtils {

    public static void infoSnack(View view, CharSequence message) {
        if (view != null) {
            makeSnack(view, message, null, Snackbar.LENGTH_SHORT, Color.WHITE)
                    .show();
        }
    }

    public static void errorSnack(View view, CharSequence message, CharSequence detail) {
        if (view != null) {
            makeSnack(view, message, detail, Snackbar.LENGTH_LONG,
                    ContextCompat.getColor(HiApplication.getAppContext(),
                            R.color.md_yellow_500))
                    .show();
        }
    }

    private static Snackbar makeSnack(final View view, final CharSequence message, final CharSequence detail, int length, int textColor) {
        final Snackbar snackbar = Snackbar.make(view, message, length);
        setSnackbarMessageTextColor(snackbar, textColor);

        if (!TextUtils.isEmpty(detail)) {
            snackbar.setAction("详情", new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    UIUtils.showMessageDialog(view.getContext(),
                            "详细信息",
                            message + "\n" + detail, true);
                    snackbar.dismiss();

                }
            });
        }
        return snackbar;
    }

    public static void setSnackbarMessageTextColor(Snackbar snackbar, int color) {
        View view = snackbar.getView();
        ((TextView) view.findViewById(R.id.snackbar_text)).setTextColor(color);
    }

    public static AlertDialog.Builder getMessageDialogBuilder(final Context context, String message, final String detail) {
        final LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        final View viewlayout = inflater.inflate(R.layout.item_simple_dialog, null);

        AlertDialog.Builder builder = new AlertDialog.Builder(context);

        final TextView tvTitle = (TextView) viewlayout.findViewById(R.id.tv_dialog_title);
        tvTitle.setText(message);

        final TextView textView = (TextView) viewlayout.findViewById(R.id.tv_dialog_content);
        textView.setText(detail);
        UIUtils.setLineSpacing(textView);

        builder.setView(viewlayout);
        return builder;
    }

    public static void showMessageDialog(final Context context, String message, final String detail, boolean copyable) {
        AlertDialog.Builder builder = getMessageDialogBuilder(context, message, detail);
        builder.setPositiveButton(context.getResources().getString(R.string.action_close), null);
        if (copyable) {
            builder.setNeutralButton(context.getResources().getString(R.string.action_copy),
                    new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            ClipboardManager clipboard = (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
                            ClipData clip = ClipData.newPlainText("COPY FROM HiPDA", detail);
                            clipboard.setPrimaryClip(clip);
                        }
                    });
        }
        AlertDialog dialog = builder.create();
        dialog.show();
    }

    public static void showReleaseNotesDialog(final Activity activity) {
        String releaseNotes;
        try {
            releaseNotes = Utils.readFromAssets(activity, "release-notes.txt");
        } catch (Exception e) {
            releaseNotes = e.getMessage();
        }

        String info = "*** 问题反馈请到 “设置”-“客户端发布帖”，不要另开新帖或短消息。\n*** 反馈前请阅读1楼红色字体须知，提供必需的信息和详细错误描述。\n\n";

        showMessageDialog(activity, "更新记录", info + releaseNotes, false);
    }

    public static boolean askForStoragePermission(Context ctx) {
        if (ContextCompat.checkSelfPermission(ctx, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
            UIUtils.toast("需要授予 \"存储空间\" 权限");
            if (ctx instanceof Activity)
                ActivityCompat.requestPermissions((Activity) ctx,
                        new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                        MainFrameActivity.PERMISSIONS_REQUEST_CODE_STORAGE);
            return true;
        }
        return false;
    }

    public static boolean askForBothPermissions(Activity activity) {
        boolean askCamera = !HiSettingsHelper.getInstance().isCameraPermAsked()
                && ContextCompat.checkSelfPermission(activity, Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED;
        boolean askStorage = ContextCompat.checkSelfPermission(activity, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED;
        String[] perms = null;
        if (askCamera && askStorage) {
            HiSettingsHelper.getInstance().setCameraPermAsked(true);
            perms = new String[]{Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE};
        } else if (askCamera) {
            HiSettingsHelper.getInstance().setCameraPermAsked(true);
            perms = new String[]{Manifest.permission.CAMERA};
        } else if (askStorage) {
            perms = new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE};
            UIUtils.toast("需要授予 \"存储空间\" 权限");
        }
        if (perms != null) {
            ActivityCompat.requestPermissions(activity, perms,
                    PostActivity.PERMISSIONS_REQUEST_CODE_BOTH);
            return true;
        }
        return false;
    }

    public static void setLineSpacing(TextView textView) {
        float lineSpacingExtra = 2;
        float lineSpacingMultiplier = 1.1f;
        if (HiSettingsHelper.getInstance().getPostLineSpacing() == 1) {
            lineSpacingExtra = 4;
            lineSpacingMultiplier = 1.2f;
        } else if (HiSettingsHelper.getInstance().getPostLineSpacing() == 2) {
            lineSpacingExtra = 6;
            lineSpacingMultiplier = 1.3f;
        } else if (HiSettingsHelper.getInstance().getPostLineSpacing() == 3) {
            lineSpacingExtra = 8;
            lineSpacingMultiplier = 1.4f;
        }
        textView.setLineSpacing(lineSpacingExtra, lineSpacingMultiplier);
    }

    public static void hideSoftKeyboard(Activity activity) {
        if (activity != null) {
            InputMethodManager imm = (InputMethodManager) activity.getSystemService(Context.INPUT_METHOD_SERVICE);
            if (imm != null) {
                imm.hideSoftInputFromWindow(activity.getWindow().getDecorView().getWindowToken(), InputMethodManager.HIDE_NOT_ALWAYS);
            }
        }
    }

    public static void shareImage(final Activity activity, final View view, String url) {
        if (activity == null || view == null)
            return;
        if (askForStoragePermission(activity))
            return;

        final ImageInfo imageInfo = ImageContainer.getImageInfo(url);
        if (!imageInfo.isReady()) {
            FileDownTask fileDownTask = new FileDownTask(activity) {
                @Override
                protected void onPostExecute(Void aVoid) {
                    super.onPostExecute(aVoid);
                    if (imageInfo.isReady())
                        shareImage(activity, view, imageInfo);
                    else
                        errorSnack(view, "分享时发生错误", mException != null ? mException.getMessage() : "");
                }
            };
            fileDownTask.execute(url);
        } else {
            shareImage(activity, view, imageInfo);
        }
    }

    private static void shareImage(Activity activity, final View view, ImageInfo imageInfo) {
        try {
            String filename = Utils.getImageFileName(Constants.FILE_SHARE_PREFIX, imageInfo.getMime());
            File cacheDirectory = HiApplication.getAppContext().getExternalCacheDir();
            File destFile = new File(cacheDirectory, filename);
            Utils.copy(new File(imageInfo.getPath()), destFile);

            Intent shareIntent = new Intent(Intent.ACTION_SEND);
            shareIntent.setType(imageInfo.getMime());
            Uri uri = Uri.fromFile(destFile);
            shareIntent.putExtra(Intent.EXTRA_STREAM, uri);
            activity.startActivity(Intent.createChooser(shareIntent, "分享图片"));
        } catch (Exception e) {
            Logger.e(e);
            errorSnack(view, "分享时发生错误", e.getMessage());
        }
    }

    public static void saveImage(final Activity activity, final View view, String url) {
        if (activity == null || view == null)
            return;
        if (askForStoragePermission(activity))
            return;

        final ImageInfo imageInfo = ImageContainer.getImageInfo(url);
        if (!imageInfo.isReady()) {
            FileDownTask fileDownTask = new FileDownTask(activity) {
                @Override
                protected void onPostExecute(Void aVoid) {
                    super.onPostExecute(aVoid);
                    if (imageInfo.isReady()) {
                        saveImage(activity, view, imageInfo);
                    } else {
                        errorSnack(view, "分享时发生错误", mException != null ? mException.getMessage() : "");
                    }
                }
            };
            fileDownTask.execute(url);
        } else {
            saveImage(activity, view, imageInfo);
        }
    }

    private static void saveImage(final Activity activity, final View view, final ImageInfo imageInfo) {
        try {
            String filename = Utils.getImageFileName("Hi_IMG", imageInfo.getMime());
            final File destFile = new File(getSaveFolder(), filename);
            Utils.copy(new File(imageInfo.getPath()), destFile);
            MediaScannerConnection.scanFile(activity, new String[]{destFile.getPath()}, null, null);

            Snackbar snackbar = Snackbar.make(view, "文件已保存", Snackbar.LENGTH_LONG);
            View snackbarView = snackbar.getView();
            ((TextView) snackbarView.findViewById(R.id.snackbar_text)).setTextColor(Color.WHITE);

            snackbar.setAction("查看", new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    try {
                        Intent intent = new Intent();
                        intent.setAction(Intent.ACTION_VIEW);
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                            intent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                            Uri contentUri = FileProvider.getUriForFile(activity, BuildConfig.APPLICATION_ID + ".provider", destFile);
                            intent.setDataAndType(contentUri, imageInfo.getMime());
                        } else {
                            intent.setDataAndType(Uri.fromFile(destFile), imageInfo.getMime());
                        }
                        activity.startActivity(intent);
                    } catch (Exception e) {
                        errorSnack(view, "打开文件发生错误", "请尝试将保存路径设置到内置存储\n" + e.getMessage());
                    }
                }
            });
            snackbar.show();
        } catch (Exception e) {
            Logger.e(e);
            errorSnack(view, "保存图片文件时发生错误", e.getMessage());
        }
    }

    public static File getSaveFolder() {
        String saveFolder = HiSettingsHelper.getInstance().getStringValue(HiSettingsHelper.PERF_SAVE_FOLDER, "");
        File dir = new File(saveFolder);
        if (saveFolder.startsWith("/") && dir.exists() && dir.isDirectory() && dir.canWrite()) {
            return dir;
        }
        dir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
        HiSettingsHelper.getInstance().setStringValue(HiSettingsHelper.PERF_SAVE_FOLDER, dir.getAbsolutePath());
        return dir;
    }

    public static Boolean isTablet(Context context) {
        return (context.getResources().getConfiguration().screenLayout &
                Configuration.SCREENLAYOUT_SIZE_MASK) >= Configuration.SCREENLAYOUT_SIZE_LARGE;
    }

    public static View getSnackView(Activity activity) {
        if (activity != null)
            return activity.getWindow().getDecorView().getRootView().findViewById(R.id.main_frame_container);
        return null;
    }

    public static int getWindowWidth(Window window) {
        DisplayMetrics metrics = new DisplayMetrics();
        window.getWindowManager().getDefaultDisplay().getMetrics(metrics);

        return metrics.widthPixels;
    }

    public static void toast(String text) {
        Toast.makeText(HiApplication.getAppContext(), text, Toast.LENGTH_SHORT).show();
    }

    public static int getRelativeTop(View myView, ViewGroup parentView) {
        Rect offsetViewBounds = new Rect();
        myView.getDrawingRect(offsetViewBounds);
        parentView.offsetDescendantRectToMyCoords(myView, offsetViewBounds);
        return offsetViewBounds.top;
    }

    public static void copyToClipboard(String text) {
        ClipboardManager clipboard = (ClipboardManager) HiApplication.getAppContext().getSystemService(Context.CLIPBOARD_SERVICE);
        ClipData clip = ClipData.newPlainText("COPY FROM HiPDA", text);
        clipboard.setPrimaryClip(clip);
    }

}