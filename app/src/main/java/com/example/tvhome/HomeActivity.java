package com.example.tvhome;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Color;
import android.graphics.Outline;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.provider.Settings;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewOutlineProvider;
import android.widget.FrameLayout;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextClock;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Launcher estilo tvOS: dock con apps favoritas abajo, reloj arriba a la derecha
 * y fondo animado. Mantener OK sobre una app: la agrega o quita del dock.
 */
public class HomeActivity extends Activity {

    private static final int MATCH = ViewGroup.LayoutParams.MATCH_PARENT;
    private static final int WRAP = ViewGroup.LayoutParams.WRAP_CONTENT;
    private static final int GRID_COLUMNS = 5;

    private static final String PREFS = "tvhome";
    private static final String KEY_FAVS = "favs";
    private static final String[] DEFAULT_FAVS = {
            "com.netflix.ninja",
            "com.google.android.youtube.tv",
            "com.amazon.amazonvideo.livingroom",
            "com.disney.disneyplus",
            "com.apple.atve.androidtv.appletv",
            "com.spotify.tv.android",
            "com.android.vending"
    };

    private static class AppEntry {
        String pkg;
        String label;
        Drawable image;
        boolean banner;
    }

    private PackageManager pm;
    private float density;
    private FrameLayout root;
    private HorizontalScrollView dockScroll;
    private LinearLayout dock;
    private TextView focusLabel;
    private FrameLayout allAppsPanel;
    private LinearLayout allAppsList;

    private int dp(int v) {
        return Math.round(v * density);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        pm = getPackageManager();
        density = getResources().getDisplayMetrics().density;

        root = new FrameLayout(this);
        root.addView(new AnimatedBackground(this), new FrameLayout.LayoutParams(MATCH, MATCH));

        // Reloj arriba a la derecha
        TextClock clock = new TextClock(this);
        clock.setFormat24Hour("HH:mm");
        clock.setFormat12Hour("HH:mm");
        clock.setTextColor(Color.WHITE);
        clock.setTextSize(22);
        clock.setPadding(dp(22), dp(8), dp(22), dp(8));
        clock.setBackground(rounded(0x55FFFFFF, 40, 0));
        FrameLayout.LayoutParams clockLp = new FrameLayout.LayoutParams(WRAP, WRAP,
                Gravity.TOP | Gravity.END);
        clockLp.setMargins(0, dp(28), dp(48), 0);
        root.addView(clock, clockLp);

        // Dock abajo
        dock = new LinearLayout(this);
        dock.setOrientation(LinearLayout.HORIZONTAL);
        dock.setGravity(Gravity.CENTER_VERTICAL);
        dock.setClipChildren(false);

        dockScroll = new HorizontalScrollView(this);
        dockScroll.setHorizontalScrollBarEnabled(false);
        dockScroll.setClipToPadding(false);
        dockScroll.setClipChildren(false);
        dockScroll.setPadding(dp(18), dp(18), dp(18), dp(18));
        dockScroll.setBackground(rounded(0x663A6EA8, 32, 0));
        dockScroll.addView(dock);
        FrameLayout.LayoutParams dockLp = new FrameLayout.LayoutParams(WRAP, WRAP,
                Gravity.BOTTOM | Gravity.START);
        dockLp.setMargins(dp(48), 0, dp(48), dp(72));
        root.addView(dockScroll, dockLp);

        // Nombre de la app enfocada
        focusLabel = new TextView(this);
        focusLabel.setTextColor(Color.WHITE);
        focusLabel.setTextSize(20);
        FrameLayout.LayoutParams labelLp = new FrameLayout.LayoutParams(WRAP, WRAP,
                Gravity.BOTTOM | Gravity.START);
        labelLp.setMargins(dp(72), 0, 0, dp(30));
        root.addView(focusLabel, labelLp);

        // Panel "Todas las apps"
        allAppsList = new LinearLayout(this);
        allAppsList.setOrientation(LinearLayout.VERTICAL);
        allAppsList.setClipChildren(false);
        allAppsList.setPadding(dp(48), dp(40), dp(48), dp(40));

        ScrollView scroll = new ScrollView(this);
        scroll.setVerticalScrollBarEnabled(false);
        scroll.setClipChildren(false);
        scroll.setClipToPadding(false);
        scroll.addView(allAppsList);

        allAppsPanel = new FrameLayout(this);
        allAppsPanel.setBackgroundColor(0xF0081428);
        allAppsPanel.setVisibility(View.GONE);
        allAppsPanel.addView(scroll, new FrameLayout.LayoutParams(MATCH, MATCH));
        root.addView(allAppsPanel, new FrameLayout.LayoutParams(MATCH, MATCH));

        setContentView(root);
    }

    @Override
    protected void onResume() {
        super.onResume();
        buildDock();
        if (allAppsPanel.getVisibility() != View.VISIBLE) {
            focusDock();
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        // Boton Home: volver al dock
        closeAllApps();
        dockScroll.scrollTo(0, 0);
        focusDock();
    }

    @Override
    public void onBackPressed() {
        if (allAppsPanel.getVisibility() == View.VISIBLE) {
            closeAllApps();
        }
        // En la pantalla de inicio, Atras no hace nada.
    }

    // ------------------------------------------------------------------ dock

    private void buildDock() {
        dock.removeAllViews();
        for (String pkg : loadFavs()) {
            AppEntry e = loadEntry(pkg, true);
            if (e != null) {
                dock.addView(makeAppTile(e, true), tileParams());
            }
        }
        dock.addView(makeTextTile("Todas las apps", "Todas\nlas apps", this::openAllApps), tileParams());
        dock.addView(makeTextTile("Ajustes", "Ajustes",
                () -> startSafe(new Intent(Settings.ACTION_SETTINGS))), tileParams());
    }

    private LinearLayout.LayoutParams tileParams() {
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(dp(176), dp(99));
        lp.setMargins(dp(8), dp(8), dp(8), dp(8));
        return lp;
    }

    private void focusDock() {
        if (dock.getChildCount() > 0) {
            dock.getChildAt(0).requestFocus();
        }
    }

    // ------------------------------------------------------------ todas las apps

    private void openAllApps() {
        allAppsList.removeAllViews();

        TextView title = new TextView(this);
        title.setText("Todas las apps  -  mantene OK sobre una app para agregarla o quitarla del dock");
        title.setTextColor(0xCCFFFFFF);
        title.setTextSize(18);
        title.setPadding(0, 0, 0, dp(20));
        allAppsList.addView(title);

        LinearLayout row = null;
        int col = 0;
        for (AppEntry e : queryAllApps()) {
            if (col == 0) {
                row = new LinearLayout(this);
                row.setOrientation(LinearLayout.HORIZONTAL);
                row.setClipChildren(false);
                allAppsList.addView(row);
            }
            row.addView(makeGridCell(e));
            col = (col + 1) % GRID_COLUMNS;
        }

        dockScroll.setVisibility(View.INVISIBLE);
        focusLabel.setVisibility(View.INVISIBLE);
        allAppsPanel.setVisibility(View.VISIBLE);

        View first = firstFocusableTile();
        if (first != null) {
            first.requestFocus();
        }
    }

    private void closeAllApps() {
        if (allAppsPanel.getVisibility() != View.VISIBLE) {
            return;
        }
        allAppsPanel.setVisibility(View.GONE);
        allAppsList.removeAllViews(); // libera memoria
        dockScroll.setVisibility(View.VISIBLE);
        focusLabel.setVisibility(View.VISIBLE);
        focusDock();
    }

    private View firstFocusableTile() {
        for (int i = 0; i < allAppsList.getChildCount(); i++) {
            View child = allAppsList.getChildAt(i);
            if (child instanceof LinearLayout && ((LinearLayout) child).getChildCount() > 0) {
                View cell = ((LinearLayout) child).getChildAt(0);
                if (cell instanceof LinearLayout && ((LinearLayout) cell).getChildCount() > 0) {
                    return ((LinearLayout) cell).getChildAt(0);
                }
            }
        }
        return null;
    }

    private View makeGridCell(AppEntry e) {
        LinearLayout cell = new LinearLayout(this);
        cell.setOrientation(LinearLayout.VERTICAL);
        cell.setGravity(Gravity.CENTER_HORIZONTAL);
        cell.setClipChildren(false);
        cell.setPadding(dp(8), dp(10), dp(8), dp(10));
        cell.addView(makeAppTile(e, false), new LinearLayout.LayoutParams(dp(144), dp(81)));

        TextView name = new TextView(this);
        name.setText(e.label);
        name.setTextColor(0xCCFFFFFF);
        name.setTextSize(14);
        name.setSingleLine(true);
        name.setEllipsize(TextUtils.TruncateAt.END);
        name.setGravity(Gravity.CENTER);
        name.setPadding(0, dp(8), 0, 0);
        cell.addView(name, new LinearLayout.LayoutParams(dp(144), WRAP));

        cell.setLayoutParams(new LinearLayout.LayoutParams(dp(160), WRAP));
        return cell;
    }

    private List<AppEntry> queryAllApps() {
        Set<String> seen = new HashSet<>();
        List<AppEntry> out = new ArrayList<>();
        String[] categories = {Intent.CATEGORY_LEANBACK_LAUNCHER, Intent.CATEGORY_LAUNCHER};
        for (String category : categories) {
            Intent query = new Intent(Intent.ACTION_MAIN).addCategory(category);
            for (ResolveInfo ri : pm.queryIntentActivities(query, 0)) {
                String pkg = ri.activityInfo.packageName;
                if (pkg.equals(getPackageName()) || !seen.add(pkg)) {
                    continue;
                }
                AppEntry e = loadEntry(pkg, false);
                if (e != null) {
                    out.add(e);
                }
            }
        }
        Collections.sort(out, (a, b) -> a.label.compareToIgnoreCase(b.label));
        return out;
    }

    // ------------------------------------------------------------------ tiles

    private FrameLayout baseTile(final String label) {
        final FrameLayout tile = new FrameLayout(this);
        tile.setBackground(rounded(0xFF1C2A44, 14, 0));
        tile.setClipToOutline(true);
        tile.setOutlineProvider(new ViewOutlineProvider() {
            @Override
            public void getOutline(View view, Outline outline) {
                outline.setRoundRect(0, 0, view.getWidth(), view.getHeight(), dp(14));
            }
        });
        tile.setFocusable(true);
        tile.setClickable(true);
        tile.setLongClickable(true);
        tile.setOnFocusChangeListener((v, hasFocus) -> {
            float scale = hasFocus ? 1.14f : 1f;
            v.animate().scaleX(scale).scaleY(scale).setDuration(120).start();
            v.setTranslationZ(hasFocus ? dp(10) : 0);
            tile.setForeground(hasFocus ? rounded(Color.TRANSPARENT, 14, 0xFFFFFFFF) : null);
            if (hasFocus) {
                focusLabel.setText(label);
            }
        });
        return tile;
    }

    private View makeAppTile(final AppEntry e, final boolean inDock) {
        FrameLayout tile = baseTile(e.label);
        ImageView image = new ImageView(this);
        if (e.banner) {
            image.setScaleType(ImageView.ScaleType.CENTER_CROP);
        } else {
            image.setScaleType(ImageView.ScaleType.FIT_CENTER);
            image.setPadding(dp(20), dp(10), dp(20), dp(10));
        }
        image.setImageDrawable(e.image);
        tile.addView(image, new FrameLayout.LayoutParams(MATCH, MATCH));

        tile.setOnClickListener(v -> launch(e.pkg));
        tile.setOnLongClickListener(v -> {
            toggleFav(e);
            return true;
        });
        return tile;
    }

    private View makeTextTile(String label, String text, final Runnable action) {
        FrameLayout tile = baseTile(label);
        TextView tv = new TextView(this);
        tv.setText(text);
        tv.setTextColor(Color.WHITE);
        tv.setTextSize(18);
        tv.setGravity(Gravity.CENTER);
        tile.addView(tv, new FrameLayout.LayoutParams(MATCH, MATCH));
        tile.setOnClickListener(v -> action.run());
        return tile;
    }

    private GradientDrawable rounded(int color, int radiusDp, int strokeColor) {
        GradientDrawable g = new GradientDrawable();
        g.setColor(color);
        g.setCornerRadius(dp(radiusDp));
        if (strokeColor != 0) {
            g.setStroke(dp(3), strokeColor);
        }
        return g;
    }

    // --------------------------------------------------------------- datos

    private Intent launchIntent(String pkg) {
        Intent i = pm.getLeanbackLaunchIntentForPackage(pkg);
        if (i == null) {
            i = pm.getLaunchIntentForPackage(pkg);
        }
        return i;
    }

    private AppEntry loadEntry(String pkg, boolean preferBanner) {
        try {
            ApplicationInfo ai = pm.getApplicationInfo(pkg, 0);
            if (launchIntent(pkg) == null) {
                return null;
            }
            AppEntry e = new AppEntry();
            e.pkg = pkg;
            e.label = String.valueOf(ai.loadLabel(pm));
            Drawable banner = preferBanner ? ai.loadBanner(pm) : null;
            e.banner = banner != null;
            e.image = banner != null ? banner : ai.loadIcon(pm);
            return e;
        } catch (PackageManager.NameNotFoundException ex) {
            return null;
        }
    }

    private void launch(String pkg) {
        Intent i = launchIntent(pkg);
        if (i == null) {
            Toast.makeText(this, "No se puede abrir esa app", Toast.LENGTH_SHORT).show();
            return;
        }
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startSafe(i);
    }

    private void startSafe(Intent i) {
        try {
            startActivity(i);
        } catch (ActivityNotFoundException ex) {
            Toast.makeText(this, "No se pudo abrir", Toast.LENGTH_SHORT).show();
        }
    }

    private List<String> loadFavs() {
        SharedPreferences sp = getSharedPreferences(PREFS, MODE_PRIVATE);
        String s = sp.getString(KEY_FAVS, null);
        List<String> out = new ArrayList<>();
        if (s == null) {
            Collections.addAll(out, DEFAULT_FAVS);
        } else if (!s.isEmpty()) {
            Collections.addAll(out, s.split(","));
        }
        return out;
    }

    private void saveFavs(List<String> favs) {
        StringBuilder sb = new StringBuilder();
        for (String f : favs) {
            if (sb.length() > 0) {
                sb.append(',');
            }
            sb.append(f);
        }
        getSharedPreferences(PREFS, MODE_PRIVATE).edit().putString(KEY_FAVS, sb.toString()).apply();
    }

    private void toggleFav(AppEntry e) {
        List<String> favs = loadFavs();
        if (favs.contains(e.pkg)) {
            favs.remove(e.pkg);
            Toast.makeText(this, e.label + ": quitada del dock", Toast.LENGTH_SHORT).show();
        } else {
            favs.add(e.pkg);
            Toast.makeText(this, e.label + ": agregada al dock", Toast.LENGTH_SHORT).show();
        }
        saveFavs(favs);
        boolean gridOpen = allAppsPanel.getVisibility() == View.VISIBLE;
        buildDock();
        if (!gridOpen) {
            focusDock();
        }
    }
}
