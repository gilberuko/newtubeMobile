package com.ottmty.tube.panel;

/** Datos fijos de la app. Cada proyecto (mobile / tv) trae su propia copia. */
public final class PanelConfig {
    /** Panel ibo al que está ligada la cuenta. */
    public static final String PANEL_BASE = "https://ottmty.xyz/ott17";
    public static final String APP_NAME = "Tube OTTMTY";
    public static final boolean IS_TV = false;
    /** Pantalla de entrada original de la app: el login pasa a ella al iniciar sesión. */
    public static final String SPLASH_CLASS = "com.liskovsoft.smartyoutubetv2.tv.ui.main.SplashActivity";

    private PanelConfig() {
    }
}
