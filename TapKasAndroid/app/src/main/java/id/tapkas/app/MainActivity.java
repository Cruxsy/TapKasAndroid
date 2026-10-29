package id.tapkas.app;

import android.app.Activity;
import android.nfc.NdefMessage;
import android.nfc.NdefRecord;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.nfc.tech.Ndef;
import android.os.Bundle;
import android.webkit.JavascriptInterface;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.Locale;

public class MainActivity extends Activity implements NfcAdapter.ReaderCallback {
    private WebView webView;
    private NfcAdapter nfcAdapter;
    private volatile boolean scanMode = false;
    private volatile String writeCode = null;

    @Override public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        nfcAdapter = NfcAdapter.getDefaultAdapter(this);
        webView = new WebView(this);
        webView.setWebChromeClient(new WebChromeClient());
        webView.setWebViewClient(new WebViewClient());
        webView.getSettings().setJavaScriptEnabled(true);
        webView.getSettings().setDomStorageEnabled(true);
        webView.getSettings().setAllowFileAccess(true);
        webView.addJavascriptInterface(new NfcBridge(), "AndroidNFC");
        setContentView(webView);
        webView.loadUrl("file:///android_asset/index.html");
    }

    private void enableReader() {
        if (nfcAdapter == null) { jsError("HP ini tidak memiliki NFC."); return; }
        if (!nfcAdapter.isEnabled()) { jsError("Aktifkan NFC di pengaturan HP."); return; }
        Bundle options = new Bundle();
        options.putInt(NfcAdapter.EXTRA_READER_PRESENCE_CHECK_DELAY, 250);
        nfcAdapter.enableReaderMode(this, this,
                NfcAdapter.FLAG_READER_NFC_A | NfcAdapter.FLAG_READER_NFC_B |
                NfcAdapter.FLAG_READER_NFC_F | NfcAdapter.FLAG_READER_NFC_V |
                NfcAdapter.FLAG_READER_NO_PLATFORM_SOUNDS, options);
    }

    @Override public void onTagDiscovered(Tag tag) {
        String codeToWrite = writeCode;
        if (codeToWrite != null) {
            try {
                Ndef ndef = Ndef.get(tag);
                if (ndef == null) throw new IOException("Tag tidak mendukung NDEF.");
                ndef.connect();
                NdefRecord record = NdefRecord.createTextRecord("id", codeToWrite);
                ndef.writeNdefMessage(new NdefMessage(new NdefRecord[]{record}));
                ndef.close();
                writeCode = null; scanMode = false;
                disableReader();
                js("toast('Kode " + jsEscape(codeToWrite) + " berhasil ditulis ke tag.');render();");
            } catch (Exception ex) {
                writeCode = null; scanMode = false; disableReader();
                jsError("Gagal menulis tag: " + ex.getMessage());
            }
            return;
        }
        if (!scanMode) return;
        try {
            Ndef ndef = Ndef.get(tag);
            if (ndef == null) throw new IOException("Tag tidak berisi data NDEF teks.");
            ndef.connect();
            NdefMessage message = ndef.getNdefMessage();
            ndef.close();
            if (message == null || message.getRecords().length == 0) throw new IOException("Tag kosong.");
            String value = readText(message.getRecords()[0]);
            scanMode = false; disableReader();
            js("nativeNfcRead('" + jsEscape(value) + "');");
        } catch (Exception ex) {
            scanMode = false; disableReader();
            jsError("Tag tidak terbaca: " + ex.getMessage());
        }
    }

    private String readText(NdefRecord record) throws IOException {
        if (record.getTnf() != NdefRecord.TNF_WELL_KNOWN || !java.util.Arrays.equals(record.getType(), NdefRecord.RTD_TEXT))
            throw new IOException("Format tag bukan teks NDEF.");
        byte[] payload = record.getPayload();
        if (payload.length == 0) return "";
        int languageLength = payload[0] & 0x3F;
        if (payload.length < 1 + languageLength) throw new IOException("Data tag tidak valid.");
        return new String(payload, 1 + languageLength, payload.length - 1 - languageLength, Charset.forName("UTF-8"));
    }

    private void disableReader() { if (nfcAdapter != null) runOnUiThread(() -> nfcAdapter.disableReaderMode(this)); }
    private void js(String code) { runOnUiThread(() -> webView.evaluateJavascript(code, null)); }
    private void jsError(String message) { js("nativeNfcError('" + jsEscape(message) + "');"); }
    private String jsEscape(String s) { return s == null ? "" : s.replace("\\", "\\\\").replace("'", "\\'").replace("\n", " ").replace("\r", " "); }

    private class NfcBridge {
        @JavascriptInterface public void startScan() {
            runOnUiThread(() -> { writeCode = null; scanMode = true; enableReader(); });
        }
        @JavascriptInterface public void stopScan() {
            runOnUiThread(() -> { scanMode = false; writeCode = null; disableReader(); });
        }
        @JavascriptInterface public void writeTag(String code) {
            runOnUiThread(() -> {
                if (nfcAdapter == null) { jsError("HP ini tidak memiliki NFC."); return; }
                writeCode = code; scanMode = false;
                js("toast('Tempelkan tag NFC kosong/yang dapat ditulis ke belakang HP...');");
                enableReader();
            });
        }
    }

    @Override protected void onPause() { super.onPause(); if (nfcAdapter != null) nfcAdapter.disableReaderMode(this); }
    @Override protected void onResume() { super.onResume(); }
}
