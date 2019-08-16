package com.arubanetworks.meridiansamples;

import com.arubanetworks.meridian.Meridian;
import com.arubanetworks.meridian.editor.EditorKey;

import org.acra.ACRA;
import org.acra.ReportField;
import org.acra.ReportingInteractionMode;
import org.acra.annotation.ReportsCrashes;

@ReportsCrashes(
        mailTo = "developers@meridianapps.com",
        customReportContent = { ReportField.APP_VERSION_CODE, ReportField.APP_VERSION_NAME, ReportField.ANDROID_VERSION, ReportField.PHONE_MODEL, ReportField.CUSTOM_DATA, ReportField.STACK_TRACE, ReportField.LOGCAT },
        mode = ReportingInteractionMode.TOAST,
        resToastText = R.string.issue_report
)
public class Application extends android.app.Application {
    //NOTE: To build the Kotlin Samples App, change the build variant.
    // To build the default Sample SDK App, use:
    public static final EditorKey APP_KEY = EditorKey.forApp("4788662907437056");
    public static final EditorKey MAP_KEY = EditorKey.forMap("5283478200123392", APP_KEY);

    // To build your own customized SDK based App, replace APP_KEY and MAP_KEY with your location's App and Map ID values:
    // public static final EditorKey APP_KEY = EditorKey.forApp("APP_KEY");
    // public static final EditorKey MAP_KEY = EditorKey.forMap("MAP_KEY", APP_KEY);

    // To build the default Sample SDK App for EU Servers, use the following:
    // NOTE: Even if you're geographically located in the EU, you probably won't need to do this.
    // public static final EditorKey APP_KEY = EditorKey.forApp("4856321132199936");
    // public static final EditorKey MAP_KEY = EditorKey.forMap("5752754626625536", APP_KEY);

    public static final String PLACEMARK_UID = "CASIO_UID"; // replace this with a unique id for one of your placemarks.
    public static final String CAMPAIGN_ID = ""; // unique id for one of your campaigns here.
    public static final String TAG_MAC = ""; // mac address of one of your tags here
    public static final String EDITOR_TOKEN = ""; // your editor token here

    @Override
    public void onCreate() {
        // Configure Meridian
        Meridian.configure(this);

        // Example of setting the Sample App for the EU server
        // Meridian.getShared().setDomainRegion(Meridian.DomainRegion.DomainRegionEU);

        Meridian.getShared().setEditorToken(EDITOR_TOKEN);

        // Example of overriding cache headers
        //Meridian.getShared().setOverrideCacheHeaders(true);
        //Meridian.getShared().setCacheOverrideTimeout(1000*60*60); // 1 hour

        // Example of preventing this app from reporting Analytics if it is in debug mode.
        //Meridian.getShared().setUseAnalytics(!BuildConfig.DEBUG);

        // Example of setting the default picker style.
        //Meridian.getShared().setPickerStyle(LevelPickerControl.PickerStyle.PICKER_SEARCH);

        // ACRA is for bug reporting in the samples app and is not necessary for the Meridian SDK
        ACRA.init(this);
        ACRA.getErrorReporter().putCustomData("MERIDIAN_SDK", Meridian.getShared().getSDKVersion());
        ACRA.getErrorReporter().putCustomData("MERIDIAN_API", Meridian.getShared().getAPIVersion());
        ACRA.getErrorReporter().putCustomData("MERIDIAN_URL", Meridian.getShared().getAPIBaseUri().toString());

        // Create notification channel for Oreo
        CampaignReceiver.createNotificationChannel(this);
        super.onCreate();
    }
}
