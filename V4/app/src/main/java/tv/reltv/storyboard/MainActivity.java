
package tv.reltv.storyboard;

import android.app.Activity;
import android.os.Bundle;
import android.content.Intent;
import android.net.Uri;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.webkit.ValueCallback;
import android.webkit.WebSettings;
import android.webkit.JavascriptInterface;
import android.widget.Toast;

import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

public class MainActivity extends Activity {

    private WebView webView;
    private ValueCallback<Uri[]> fileCallback;

    private static final int FILE_CHOOSER_REQUEST = 1001;
    private static final int SAVE_FILE_REQUEST = 1002;

    private String pendingContent;
    private String pendingMimeType;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        webView = new WebView(this);
        setContentView(webView);

        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setAllowFileAccess(true);
        settings.setAllowContentAccess(true);

        webView.addJavascriptInterface(new AndroidBridge(), "Android");
        webView.setWebViewClient(new WebViewClient());

        webView.setWebChromeClient(new WebChromeClient() {
            @Override
            public boolean onShowFileChooser(
                    WebView view,
                    ValueCallback<Uri[]> callback,
                    FileChooserParams params) {

                if (fileCallback != null) {
                    fileCallback.onReceiveValue(null);
                }

                fileCallback = callback;

                Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
                intent.addCategory(Intent.CATEGORY_OPENABLE);
                intent.setType("image/*");

                try {
                    startActivityForResult(
                        Intent.createChooser(intent, "Pilih Foto Karakter"),
                        FILE_CHOOSER_REQUEST
                    );
                } catch (Exception e) {
                    fileCallback = null;
                    return false;
                }

                return true;
            }
        });

        webView.loadUrl("file:///android_asset/index.html");
    }

    private class AndroidBridge {

        @JavascriptInterface
        public void saveFile(
                String filename,
                String mimeType,
                String content) {

            runOnUiThread(() -> {
                pendingContent = content;
                pendingMimeType = mimeType;

                Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
                intent.addCategory(Intent.CATEGORY_OPENABLE);
                intent.setType(mimeType);
                intent.putExtra(Intent.EXTRA_TITLE, filename);

                try {
                    startActivityForResult(intent, SAVE_FILE_REQUEST);
                } catch (Exception e) {
                    pendingContent = null;
                    pendingMimeType = null;
                    Toast.makeText(
                        MainActivity.this,
                        "Tidak dapat membuka penyimpanan",
                        Toast.LENGTH_LONG
                    ).show();
                }
            });
        }
    }

    @Override
    protected void onActivityResult(
            int requestCode, int resultCode, Intent data) {

        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == FILE_CHOOSER_REQUEST) {
            if (fileCallback != null) {
                Uri[] results = null;

                if (resultCode == RESULT_OK
                        && data != null
                        && data.getData() != null) {
                    results = new Uri[] { data.getData() };
                }

                fileCallback.onReceiveValue(results);
                fileCallback = null;
            }
            return;
        }

        if (requestCode == SAVE_FILE_REQUEST) {
            if (resultCode == RESULT_OK
                    && data != null
                    && data.getData() != null
                    && pendingContent != null) {

                try (OutputStream output =
                        getContentResolver().openOutputStream(data.getData())) {

                    if (output == null) {
                        throw new Exception("File tidak dapat dibuka");
                    }

                    output.write(
                        pendingContent.getBytes(StandardCharsets.UTF_8)
                    );
                    output.flush();

                    Toast.makeText(
                        this,
                        "File berhasil disimpan",
                        Toast.LENGTH_LONG
                    ).show();

                } catch (Exception e) {
                    Toast.makeText(
                        this,
                        "Gagal menyimpan file",
                        Toast.LENGTH_LONG
                    ).show();
                }
            }

            pendingContent = null;
            pendingMimeType = null;
        }
    }

    @Override
    public void onBackPressed() {
        if (webView != null && webView.canGoBack()) {
            webView.goBack();
        } else {
            super.onBackPressed();
        }
    }
}
