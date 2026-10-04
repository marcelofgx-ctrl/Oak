package com.oakandember.operator;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import com.google.androidbrowserhelper.trusted.LauncherActivity;

/** Open the verified Oak workspace; external apps cannot supply launch URLs or files. */
public class MainActivity extends LauncherActivity {
    private static Intent safeIntent(Intent incoming) {
        Intent safe = new Intent(incoming);
        safe.setData(null);
        safe.replaceExtras((Bundle) null);
        safe.setClipData(null);
        safe.setSelector(null);
        return safe;
    }

    @Override protected void onCreate(Bundle state) {
        setIntent(safeIntent(getIntent()));
        super.onCreate(state);
    }

    @Override protected void onNewIntent(Intent incoming) {
        super.onNewIntent(safeIntent(incoming));
    }

    @Override protected Uri getLaunchingUrl() {
        return Uri.parse("https://oak-79f.pages.dev/operator?source=android-app");
    }
}
