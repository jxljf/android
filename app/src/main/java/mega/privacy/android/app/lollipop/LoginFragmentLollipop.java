package mega.privacy.android.app.lollipop;

import android.app.Activity;
import android.app.Fragment;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.SwitchCompat;
import android.text.InputType;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.Display;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.ScrollView;
import android.widget.Switch;
import android.widget.TextView;

import java.util.Locale;

import mega.privacy.android.app.DatabaseHandler;
import mega.privacy.android.app.MegaApplication;
import mega.privacy.android.app.MegaAttributes;
import mega.privacy.android.app.MegaPreferences;
import mega.privacy.android.app.OldPreferences;
import mega.privacy.android.app.OldUserCredentials;
import mega.privacy.android.app.R;
import mega.privacy.android.app.UserCredentials;
import mega.privacy.android.app.providers.FileProviderActivity;
import mega.privacy.android.app.utils.Constants;
import mega.privacy.android.app.utils.Util;
import nz.mega.sdk.MegaApiAndroid;
import nz.mega.sdk.MegaApiJava;
import nz.mega.sdk.MegaError;
import nz.mega.sdk.MegaNode;
import nz.mega.sdk.MegaRequest;
import nz.mega.sdk.MegaRequestListenerInterface;


public class LoginFragmentLollipop extends Fragment implements View.OnClickListener, MegaRequestListenerInterface {

    Context context;
    private AlertDialog insertMailDialog;
    private AlertDialog insertMKDialog;

    LoginFragmentLollipop loginFragment = this;

    TextView loginTitle;
    TextView newToMega;
    EditText et_user;
    EditText et_password;
    TextView bRegister;
    TextView registerText;
    TextView bLogin;
    TextView bForgotPass;
    ImageView loginThreeDots;
    SwitchCompat loginSwitch;
    TextView loginABC;
    LinearLayout loginLogin;
    LinearLayout loginLoggingIn;
    LinearLayout loginCreateAccount;
    View loginDelimiter;
    ProgressBar loginProgressBar;
    ProgressBar loginFetchNodesProgressBar;
    TextView generatingKeysText;
    TextView queryingSignupLinkText;
    TextView confirmingAccountText;
    TextView loggingInText;
    TextView fetchingNodesText;
    TextView prepareNodesText;
    TextView serversBusyText;
    ScrollView scrollView;

    RelativeLayout forgotPassLayout;
    TextView forgotPassTitle;
    TextView forgotPassFirstP;
    TextView forgotPassSecondP;
    TextView forgotPassAction;
    Button yesMK;
    Button noMK;

    RelativeLayout parkAccountLayout;
    TextView parkAccountTitle;
    TextView parkAccountFirstP;
    TextView parkAccountSecondP;
    Button parkAccountButton;

    CountDownTimer timer;
    boolean firstRequestUpdate = true;

    float scaleH, scaleW;
    float density;
    DisplayMetrics outMetrics;
    Display display;

    DatabaseHandler dbH;
    Handler handler = new Handler();

    private String lastEmail;
    private String lastPassword;
    private String gPublicKey;
    private String gPrivateKey;
    private String gSession;

    private MegaApiAndroid megaApi;
    private String confirmLink;

    int numberOfClicks = 0;

    boolean firstTime = true;

    private boolean backWhileLogin;
    private boolean loginClicked = false;

    Intent intentReceived = null;
    Bundle extras = null;
    Uri uriData = null;
    String action = null;
    String url = null;
    private long parentHandle = -1;

    @Override
    public void onSaveInstanceState(Bundle outState) {
        log("onSaveInstanceState");
        super.onSaveInstanceState(outState);
    }

    /*
     * Task to process email and password
     */
    private class HashTask extends AsyncTask<String, Void, String[]> {

        @Override
        protected String[] doInBackground(String... args) {
            String privateKey = megaApi.getBase64PwKey(args[1]);
            String publicKey = megaApi.getStringHash(privateKey, args[0]);
            return new String[]{new String(privateKey), new String(publicKey)};
        }


        @Override
        protected void onPostExecute(String[] key) {
            onKeysGenerated(key[0], key[1]);
        }

    }

    @Override
    public void onCreate (Bundle savedInstanceState){
        log("onCreate");
        super.onCreate(savedInstanceState);

        if(context==null){
            log("context is null");
            return;
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        log("onCreateView");

        loginClicked = false;
        backWhileLogin = false;

        UserCredentials credentials = dbH.getCredentials();
        if (credentials != null) {
            log("Credentials NOT null");
            firstTime = false;
        }
        else{
            firstTime = true;
        }

        display = ((Activity)context).getWindowManager().getDefaultDisplay();
        outMetrics = new DisplayMetrics ();
        display.getMetrics(outMetrics);
        density  = getResources().getDisplayMetrics().density;

        scaleW = Util.getScaleW(outMetrics, density);
        scaleH = Util.getScaleH(outMetrics, density);
        float scaleText;
        if (scaleH < scaleW){
            scaleText = scaleH;
        }
        else{
            scaleText = scaleW;
        }

        View v = inflater.inflate(R.layout.fragment_login, container, false);

        scrollView = (ScrollView) v.findViewById(R.id.scroll_view_login);

        loginTitle = (TextView) v.findViewById(R.id.login_text_view);
        //Left margin
        LinearLayout.LayoutParams textParams = (LinearLayout.LayoutParams)loginTitle.getLayoutParams();
        textParams.setMargins(Util.scaleWidthPx(60, outMetrics), Util.scaleHeightPx(40, outMetrics), 0, Util.scaleHeightPx(20, outMetrics));
        loginTitle.setLayoutParams(textParams);

        loginTitle.setText(R.string.login_text);
        loginTitle.setTextSize(TypedValue.COMPLEX_UNIT_SP, (20*scaleText));
        loginTitle.setOnClickListener(this);

        et_user = (EditText) v.findViewById(R.id.login_email_text);
        android.view.ViewGroup.LayoutParams paramsb1 = et_user.getLayoutParams();
        paramsb1.width = Util.scaleWidthPx(280, outMetrics);
        et_user.setLayoutParams(paramsb1);
        //Left margin
        textParams = (LinearLayout.LayoutParams)et_user.getLayoutParams();
        textParams.setMargins(Util.scaleWidthPx(60, outMetrics), 0, 0, Util.scaleHeightPx(10, outMetrics));
        et_user.setLayoutParams(textParams);

        et_user.setCursorVisible(true);

        et_password = (EditText) v.findViewById(R.id.login_password_text);
        et_password.setLayoutParams(paramsb1);
        et_password.setLayoutParams(textParams);

        et_password.setCursorVisible(true);

        et_password.setOnEditorActionListener(new TextView.OnEditorActionListener() {

            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_DONE) {
                    submitForm();
                    return true;
                }
                return false;
            }
        });
        loginThreeDots = (ImageView) v.findViewById(R.id.login_three_dots);
        LinearLayout.LayoutParams textThreeDots = (LinearLayout.LayoutParams)loginThreeDots.getLayoutParams();
        textThreeDots.setMargins(Util.scaleWidthPx(0, outMetrics), 0, Util.scaleWidthPx(10, outMetrics), 0);
        loginThreeDots.setLayoutParams(textThreeDots);

        loginABC = (TextView) v.findViewById(R.id.ABC);

        loginSwitch = (SwitchCompat) v.findViewById(R.id.switch_login);
        LinearLayout.LayoutParams switchParams = (LinearLayout.LayoutParams)loginSwitch.getLayoutParams();
        switchParams.setMargins(0, 0, Util.scaleWidthPx(10, outMetrics), 0);
        loginSwitch.setLayoutParams(switchParams);
        loginSwitch.setChecked(false);

        loginSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {

            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if(!isChecked){
                    et_password.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
                    et_password.setSelection(et_password.getText().length());
                }else{
                    et_password.setInputType(InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
                    et_password.setSelection(et_password.getText().length());
                }
            }
        });

        bLogin = (TextView) v.findViewById(R.id.button_login_login);
        bLogin.setText(getString(R.string.login_text).toUpperCase(Locale.getDefault()));
        android.view.ViewGroup.LayoutParams paramsbLogin = bLogin.getLayoutParams();
        paramsbLogin.height = Util.scaleHeightPx(48, outMetrics);
		/*paramsbLogin.width = Util.scaleWidthPx(63, outMetrics);*/
        bLogin.setLayoutParams(paramsbLogin);
        //Margin
        LinearLayout.LayoutParams textParamsLogin = (LinearLayout.LayoutParams)bLogin.getLayoutParams();
        textParamsLogin.setMargins(Util.scaleWidthPx(65, outMetrics), Util.scaleHeightPx(40, outMetrics), 0, Util.scaleHeightPx(10, outMetrics));
        bLogin.setLayoutParams(textParamsLogin);

        bLogin.setOnClickListener(this);

        bForgotPass = (TextView) v.findViewById(R.id.button_forgot_pass);
        bForgotPass.setText(getString(R.string.forgot_pass).toUpperCase(Locale.getDefault()));
        android.view.ViewGroup.LayoutParams paramsbForgotPass = bForgotPass.getLayoutParams();
        paramsbForgotPass.height = Util.scaleHeightPx(48, outMetrics);
		/*paramsbLogin.width = Util.scaleWidthPx(63, outMetrics);*/
        bForgotPass.setLayoutParams(paramsbForgotPass);
        //Margin
        LinearLayout.LayoutParams textParamsForgotPass = (LinearLayout.LayoutParams)bForgotPass.getLayoutParams();
        textParamsForgotPass.setMargins(Util.scaleWidthPx(65, outMetrics), 0, 0, Util.scaleHeightPx(20, outMetrics));
        bForgotPass.setLayoutParams(textParamsForgotPass);

        bForgotPass.setOnClickListener(this);

        loginDelimiter = (View) v.findViewById(R.id.login_delimiter);
        loginCreateAccount = (LinearLayout) v.findViewById(R.id.login_create_account_layout);

        newToMega = (TextView) v.findViewById(R.id.text_newToMega);
        //Margins (left, top, right, bottom)
        LinearLayout.LayoutParams textnewToMega = (LinearLayout.LayoutParams)newToMega.getLayoutParams();
        textnewToMega.setMargins(Util.scaleWidthPx(65, outMetrics), Util.scaleHeightPx(20, outMetrics), 0, Util.scaleHeightPx(30, outMetrics));
        newToMega.setLayoutParams(textnewToMega);
        newToMega.setTextSize(TypedValue.COMPLEX_UNIT_SP, (20*scaleText));

        bRegister = (TextView) v.findViewById(R.id.button_create_account_login);

        bRegister.setText(getString(R.string.create_account).toUpperCase(Locale.getDefault()));
        android.view.ViewGroup.LayoutParams paramsb2 = bRegister.getLayoutParams();
        paramsb2.height = Util.scaleHeightPx(48, outMetrics);
        bRegister.setLayoutParams(paramsb2);
        //Margin
        LinearLayout.LayoutParams textParamsRegister = (LinearLayout.LayoutParams)bRegister.getLayoutParams();
        textParamsRegister.setMargins(Util.scaleWidthPx(65, outMetrics), 0, 0, 0);
        bRegister.setLayoutParams(textParamsRegister);

        bRegister.setOnClickListener(this);

        loginLogin = (LinearLayout) v.findViewById(R.id.login_login_layout);
        loginLoggingIn = (LinearLayout) v.findViewById(R.id.login_logging_in_layout);
        loginProgressBar = (ProgressBar) v.findViewById(R.id.login_progress_bar);
        loginFetchNodesProgressBar = (ProgressBar) v.findViewById(R.id.login_fetching_nodes_bar);
        generatingKeysText = (TextView) v.findViewById(R.id.login_generating_keys_text);
        queryingSignupLinkText = (TextView) v.findViewById(R.id.login_query_signup_link_text);
        confirmingAccountText = (TextView) v.findViewById(R.id.login_confirm_account_text);
        loggingInText = (TextView) v.findViewById(R.id.login_logging_in_text);
        fetchingNodesText = (TextView) v.findViewById(R.id.login_fetch_nodes_text);
        prepareNodesText = (TextView) v.findViewById(R.id.login_prepare_nodes_text);
        serversBusyText = (TextView) v.findViewById(R.id.login_servers_busy_text);

        loginLogin.setVisibility(View.VISIBLE);
        loginCreateAccount.setVisibility(View.VISIBLE);
        loginDelimiter.setVisibility(View.VISIBLE);
        loginLoggingIn.setVisibility(View.GONE);
        generatingKeysText.setVisibility(View.GONE);
        loggingInText.setVisibility(View.GONE);
        fetchingNodesText.setVisibility(View.GONE);
        prepareNodesText.setVisibility(View.GONE);
        loginProgressBar.setVisibility(View.GONE);
        queryingSignupLinkText.setVisibility(View.GONE);
        confirmingAccountText.setVisibility(View.GONE);
        serversBusyText.setVisibility(View.GONE);
//		loginLogin.setVisibility(View.GONE);
//		loginCreateAccount.setVisibility(View.GONE);
//		loginDelimiter.setVisibility(View.GONE);
//		loginLoggingIn.setVisibility(View.VISIBLE);
//		generatingKeysText.setVisibility(View.VISIBLE);
//		loggingInText.setVisibility(View.VISIBLE);
//		fetchingNodesText.setVisibility(View.VISIBLE);
//		prepareNodesText.setVisibility(View.VISIBLE);
//		loginProgressBar.setVisibility(View.VISIBLE);
//		queryingSignupLinkText.setVisibility(View.VISIBLE);
//		confirmingAccountText.setVisibility(View.VISIBLE);

        forgotPassLayout = (RelativeLayout) v.findViewById(R.id.forgot_pass_full_layout);
        forgotPassTitle = (TextView) v.findViewById(R.id.title_forgot_pass_layout);
        RelativeLayout.LayoutParams forgotPassTitleParams = (RelativeLayout.LayoutParams)forgotPassTitle.getLayoutParams();
        forgotPassTitleParams.setMargins(Util.scaleWidthPx(24, outMetrics), Util.scaleHeightPx(70, outMetrics), Util.scaleWidthPx(24, outMetrics), 0);
        forgotPassTitle.setLayoutParams(forgotPassTitleParams);

        forgotPassFirstP = (TextView) v.findViewById(R.id.first_par_forgot_pass_layout);
        RelativeLayout.LayoutParams firstParParams = (RelativeLayout.LayoutParams)forgotPassFirstP.getLayoutParams();
        firstParParams.setMargins(Util.scaleWidthPx(24, outMetrics), Util.scaleHeightPx(20, outMetrics), Util.scaleWidthPx(24, outMetrics), 0);
        forgotPassFirstP.setLayoutParams(firstParParams);

        forgotPassSecondP = (TextView) v.findViewById(R.id.second_par_forgot_pass_layout);
        RelativeLayout.LayoutParams secondParParams = (RelativeLayout.LayoutParams)forgotPassSecondP.getLayoutParams();
        secondParParams.setMargins(Util.scaleWidthPx(24, outMetrics), Util.scaleHeightPx(20, outMetrics), Util.scaleWidthPx(24, outMetrics), 0);
        forgotPassSecondP.setLayoutParams(secondParParams);

        forgotPassAction = (TextView) v.findViewById(R.id.action_forgot_pass_layout);
        RelativeLayout.LayoutParams actionParams = (RelativeLayout.LayoutParams)forgotPassAction.getLayoutParams();
        actionParams.setMargins(Util.scaleWidthPx(24, outMetrics), Util.scaleHeightPx(25, outMetrics), Util.scaleWidthPx(24, outMetrics), 0);
        forgotPassAction.setLayoutParams(actionParams);

        yesMK = (Button) v.findViewById(R.id.yes_MK_button);
        LinearLayout.LayoutParams yesMKParams = (LinearLayout.LayoutParams)yesMK.getLayoutParams();
        yesMKParams.setMargins(Util.scaleWidthPx(20, outMetrics), Util.scaleHeightPx(25, outMetrics), 0, 0);
        yesMK.setLayoutParams(yesMKParams);
        yesMK.setOnClickListener(this);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            yesMK.setBackground(ContextCompat.getDrawable(context, R.drawable.ripple_upgrade));
        }

        noMK = (Button) v.findViewById(R.id.no_MK_button);
        LinearLayout.LayoutParams noMKParams = (LinearLayout.LayoutParams)noMK.getLayoutParams();
        noMKParams.setMargins(Util.scaleWidthPx(16, outMetrics), Util.scaleHeightPx(25, outMetrics), 0, 0);
        noMK.setLayoutParams(noMKParams);
        noMK.setOnClickListener(this);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            noMK.setBackground(ContextCompat.getDrawable(context, R.drawable.ripple_upgrade));
        }

        parkAccountLayout = (RelativeLayout) v.findViewById(R.id.park_account_layout);
        parkAccountTitle = (TextView) v.findViewById(R.id.title_park_account_layout);
        RelativeLayout.LayoutParams parkAccountTitleParams = (RelativeLayout.LayoutParams)parkAccountTitle.getLayoutParams();
        parkAccountTitleParams.setMargins(Util.scaleWidthPx(24, outMetrics), Util.scaleHeightPx(70, outMetrics), Util.scaleWidthPx(24, outMetrics), 0);
        parkAccountTitle.setLayoutParams(parkAccountTitleParams);

        parkAccountFirstP = (TextView) v.findViewById(R.id.first_par_park_account_layout);
        RelativeLayout.LayoutParams parkAccountFParams = (RelativeLayout.LayoutParams)parkAccountFirstP.getLayoutParams();
        parkAccountFParams.setMargins(Util.scaleWidthPx(24, outMetrics), Util.scaleHeightPx(20, outMetrics), Util.scaleWidthPx(24, outMetrics), 0);
        parkAccountFirstP.setLayoutParams(parkAccountFParams);

        parkAccountSecondP = (TextView) v.findViewById(R.id.second_par_park_account_layout);
        RelativeLayout.LayoutParams parkAccountSParams = (RelativeLayout.LayoutParams)parkAccountSecondP.getLayoutParams();
        parkAccountSParams.setMargins(Util.scaleWidthPx(24, outMetrics), Util.scaleHeightPx(20, outMetrics), Util.scaleWidthPx(24, outMetrics), 0);
        parkAccountSecondP.setLayoutParams(parkAccountSParams);

        parkAccountButton = (Button) v.findViewById(R.id.park_account_button);
        RelativeLayout.LayoutParams parkButtonParams = (RelativeLayout.LayoutParams)parkAccountButton.getLayoutParams();
        parkButtonParams.setMargins(0, Util.scaleHeightPx(25, outMetrics),  Util.scaleWidthPx(24, outMetrics), 0);
        parkAccountButton.setLayoutParams(parkButtonParams);
        parkAccountButton.setOnClickListener(this);

        intentReceived = ((LoginActivityLollipop) context).getIntent();
        if(intentReceived!=null){
            action = intentReceived.getAction();
            if(action!=null) {
                log("action is: "+action);
                if (Constants.ACTION_CONFIRM.equals(action)) {
                    handleConfirmationIntent(intentReceived);
                    return v;
                } else if (action.equals(Constants.ACTION_RESET_PASS)) {
                    String link = intentReceived.getDataString();
                    if (link != null) {
                        log("link to resetPass: " + link);
                        showDialogInsertMKToChangePass(link);
                        return v;
                    }
                } else if (action.equals(Constants.ACTION_PASS_CHANGED)) {
                    int result = intentReceived.getIntExtra("RESULT", -20);
                    if (result == 0) {
                        log("Show success mesage");
                        Util.showAlert(context, getString(R.string.pass_changed_alert), null);
                        return v;
                    } else if (result == MegaError.API_EARGS) {
                        log("Incorrect arguments!");
                        Util.showAlert(context, getString(R.string.email_verification_text_error), getString(R.string.general_error_word));
                        return v;
                    } else if (result == MegaError.API_EKEY) {
                        log("Incorrect MK when changing pass");
                        Util.showAlert(context, getString(R.string.incorrect_MK), getString(R.string.incorrect_MK_title));
                        return v;
                    } else {
                        log("Error when changing pass - show error message");
                        Util.showAlert(context, getString(R.string.email_verification_text_error), getString(R.string.general_error_word));
                        return v;
                    }
                } else if (action.equals(Constants.ACTION_PARK_ACCOUNT)) {
                    String link = intentReceived.getDataString();
                    if (link != null) {
                        log("link to parkAccount: " + link);
                        showConfirmationParkAccount(link);
                        return v;
                    } else {
                        log("Error when parking account - show error message");
                        Util.showAlert(context, getString(R.string.email_verification_text_error), getString(R.string.general_error_word));
                        return v;
                    }
                }
            }
            else{
                log("ACTION NULL");
            }
        }
        else{
            log("No INTENT");
        }

        if (credentials != null){
            log("Credentials NOT null");
            if ((intentReceived != null) && (intentReceived.getAction() != null)){
                if (intentReceived.getAction().equals(Constants.ACTION_REFRESH)){
                    parentHandle = intentReceived.getLongExtra("PARENT_HANDLE", -1);
                    startLoginInProcess();
                    return v;
                }
                else{
                    if(intentReceived.getAction().equals(Constants.ACTION_OPEN_MEGA_FOLDER_LINK)){
                        action = Constants.ACTION_OPEN_MEGA_FOLDER_LINK;
                        url = intentReceived.getDataString();
                    }
                    else if(intentReceived.getAction().equals(Constants.ACTION_IMPORT_LINK_FETCH_NODES)){
                        action = Constants.ACTION_OPEN_MEGA_LINK;
                        url = intentReceived.getDataString();
                    }
                    else if (intentReceived.getAction().equals(Constants.ACTION_CANCEL_UPLOAD) || intentReceived.getAction().equals(Constants.ACTION_CANCEL_DOWNLOAD) || intentReceived.getAction().equals(Constants.ACTION_CANCEL_CAM_SYNC)){
                        action = intentReceived.getAction();
                    }
                    else if(intentReceived.getAction().equals(Constants.ACTION_CHANGE_MAIL)){
                        log("intent received ACTION_CHANGE_MAIL");
                        action = Constants.ACTION_CHANGE_MAIL;
                        url = intentReceived.getDataString();
                    }
                    else if(intentReceived.getAction().equals(Constants.ACTION_CANCEL_ACCOUNT)){
                        log("intent received ACTION_CANCEL_ACCOUNT");
                        action = Constants.ACTION_CANCEL_ACCOUNT;
                        url = intentReceived.getDataString();
                    }
//					else if (intentReceived.getAction().equals(ManagerActivityLollipop.ACTION_FILE_EXPLORER_UPLOAD)){
//						action = ManagerActivityLollipop.ACTION_FILE_EXPLORER_UPLOAD;
//						uriData = intentReceived.getData();
//						log("URI: "+uriData);
//						extras = intentReceived.getExtras();
//						url = null;
//						Snackbar.make(scrollView,getString(R.string.login_before_share),Snackbar.LENGTH_LONG).show();
//					}
                    else if (intentReceived.getAction().equals(Constants.ACTION_FILE_PROVIDER)){
                        action = Constants.ACTION_FILE_PROVIDER;
                        uriData = intentReceived.getData();
                        extras = intentReceived.getExtras();
                        url = null;
                    }
                    else if (intentReceived.getAction().equals(Constants.ACTION_EXPORT_MASTER_KEY)){
                        action = Constants.ACTION_EXPORT_MASTER_KEY;
                    }
                    else if (intentReceived.getAction().equals(Constants.ACTION_IPC)){
                        action = Constants.ACTION_IPC;
                    }

                    MegaNode rootNode = megaApi.getRootNode();
                    if (rootNode != null){
                        Intent intent = new Intent(context, ManagerActivityLollipop.class);
                        if (action != null){
//							if (action.equals(ManagerActivityLollipop.ACTION_FILE_EXPLORER_UPLOAD)){
//								intent = new Intent(this, FileExplorerActivityLollipop.class);
//								if(extras != null)
//								{
//									intent.putExtras(extras);
//								}
//								intent.setData(uriData);
//							}
                            if (action.equals(Constants.ACTION_FILE_PROVIDER)){
                                intent = new Intent(context, FileProviderActivity.class);
                                if(extras != null)
                                {
                                    intent.putExtras(extras);
                                }
                                intent.setData(uriData);
                            }
                            intent.setAction(action);
                            if (url != null){
                                intent.setData(Uri.parse(url));
                            }
                        }
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB){
                            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
                        }

                        ((LoginActivityLollipop) context).startCameraSyncService(false, 5 * 60 * 1000);

                        this.startActivity(intent);
                        ((LoginActivityLollipop) context).finish();
                    }
                    else{
                        startFastLogin();
                        return v;
                    }
                }
            }
            else{
                MegaNode rootNode = megaApi.getRootNode();
                if (rootNode != null){

                    log("rootNode != null");
                    Intent intent = new Intent(context, ManagerActivityLollipop.class);
                    if (action != null){
//						if (action.equals(ManagerActivityLollipop.ACTION_FILE_EXPLORER_UPLOAD)){
//							intent = new Intent(this, FileExplorerActivityLollipop.class);
//							if(extras != null)
//							{
//								intent.putExtras(extras);
//							}
//							intent.setData(uriData);
//						}
                        if (action.equals(Constants.ACTION_FILE_PROVIDER)){
                            intent = new Intent(context, FileProviderActivity.class);
                            if(extras != null)
                            {
                                intent.putExtras(extras);
                            }
                            intent.setData(uriData);
                        }
                        intent.setAction(action);
                        if (url != null){
                            intent.setData(Uri.parse(url));
                        }
                    }
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB){
                        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    }

                    MegaPreferences prefs = dbH.getPreferences();
                    prefs = dbH.getPreferences();
                    if(prefs!=null)
                    {
                        if (prefs.getCamSyncEnabled() != null){
                            if (Boolean.parseBoolean(prefs.getCamSyncEnabled())){
                                ((LoginActivityLollipop) context).startCameraSyncService(false, 30 * 1000);
                            }
                        }
                    }
                    this.startActivity(intent);
                    ((LoginActivityLollipop) context).finish();
                }
                else{
                    log("rootNode == null");
                    startFastLogin();
                    return v;
                }
            }
        }
        else {
            log("Credentials IS NULL");
            if ((intentReceived != null)) {
                log("INTENT NOT NULL");
                if (intentReceived.getAction() != null) {
                    log("ACTION NOT NULL");
                    Intent intent;
                    if (intentReceived.getAction().equals(Constants.ACTION_FILE_PROVIDER)) {
                        intent = new Intent(context, FileProviderActivity.class);
                        if (extras != null) {
                            intent.putExtras(extras);
                        }
                        intent.setData(uriData);

                        intent.setAction(action);

                        action = Constants.ACTION_FILE_PROVIDER;
                    } else if (intentReceived.getAction().equals(Constants.ACTION_FILE_EXPLORER_UPLOAD)) {
                        action = Constants.ACTION_FILE_EXPLORER_UPLOAD;
                        //					uriData = intentReceived.getData();
                        //					log("URI: "+uriData);
                        //					extras = intentReceived.getExtras();
                        //					url = null;
                        ((LoginActivityLollipop)context).showSnackbar(getString(R.string.login_before_share));
                    } else if (intentReceived.getAction().equals(Constants.ACTION_EXPORT_MASTER_KEY)) {
                        log("ManagerActivityLollipop.ACTION_EXPORT_MASTER_KEY");
                        action = Constants.ACTION_EXPORT_MASTER_KEY;
                    }
                }
            }
            if (OldPreferences.getOldCredentials(context) != null) {
                oldCredentialsLogin();
            }
        }
        log("END onCreateView");
        return v;
    }

    public void startLoginInProcess(){
        log("startLoginInProcess");

        UserCredentials credentials = dbH.getCredentials();
        lastEmail = credentials.getEmail();
		gSession = credentials.getSession();

        loginLogin.setVisibility(View.GONE);
        loginDelimiter.setVisibility(View.GONE);
        loginCreateAccount.setVisibility(View.GONE);
        queryingSignupLinkText.setVisibility(View.GONE);
        confirmingAccountText.setVisibility(View.GONE);
        loginLoggingIn.setVisibility(View.VISIBLE);
        scrollView.setBackgroundColor(getResources().getColor(R.color.white));
//					generatingKeysText.setVisibility(View.VISIBLE);
//					megaApi.fastLogin(gSession, this);

        loginProgressBar.setVisibility(View.VISIBLE);
        loginFetchNodesProgressBar.setVisibility(View.GONE);
        loggingInText.setVisibility(View.VISIBLE);
        fetchingNodesText.setVisibility(View.VISIBLE);
        prepareNodesText.setVisibility(View.GONE);
        serversBusyText.setVisibility(View.GONE);
        megaApi.fetchNodes(this);
    }

    public void oldCredentialsLogin(){
        log("oldCredentialsLogin");
        loginLogin.setVisibility(View.GONE);
        loginDelimiter.setVisibility(View.GONE);
        loginCreateAccount.setVisibility(View.GONE);
        queryingSignupLinkText.setVisibility(View.GONE);
        confirmingAccountText.setVisibility(View.GONE);
        loginLoggingIn.setVisibility(View.VISIBLE);
        scrollView.setBackgroundColor(getResources().getColor(R.color.white));
//				generatingKeysText.setVisibility(View.VISIBLE);
        loginProgressBar.setVisibility(View.VISIBLE);
        loginFetchNodesProgressBar.setVisibility(View.GONE);
        loggingInText.setVisibility(View.VISIBLE);
        fetchingNodesText.setVisibility(View.GONE);
        prepareNodesText.setVisibility(View.GONE);
        serversBusyText.setVisibility(View.GONE);

        OldUserCredentials oldCredentials = OldPreferences.getOldCredentials(context);
        lastEmail = oldCredentials.getEmail();
        OldPreferences.clearCredentials(context);
        onKeysGeneratedLogin(oldCredentials.getPrivateKey(), oldCredentials.getPublicKey());

    }

    public void startFastLogin(){
        log("startFastLogin");
        UserCredentials credentials = dbH.getCredentials();
        lastEmail = credentials.getEmail();
        gSession = credentials.getSession();

        loginLogin.setVisibility(View.GONE);
        scrollView.setBackgroundColor(getResources().getColor(R.color.white));
        loginDelimiter.setVisibility(View.GONE);
        loginCreateAccount.setVisibility(View.GONE);
        queryingSignupLinkText.setVisibility(View.GONE);
        confirmingAccountText.setVisibility(View.GONE);
        loginLoggingIn.setVisibility(View.VISIBLE);
//						generatingKeysText.setVisibility(View.VISIBLE);
        loginProgressBar.setVisibility(View.VISIBLE);
        loginFetchNodesProgressBar.setVisibility(View.GONE);
        loggingInText.setVisibility(View.VISIBLE);
        fetchingNodesText.setVisibility(View.GONE);
        prepareNodesText.setVisibility(View.GONE);
        serversBusyText.setVisibility(View.GONE);
        megaApi.fastLogin(gSession, this);
    }

    private void submitForm() {
        if (!validateForm()) {
            return;
        }

        InputMethodManager imm = (InputMethodManager) context.getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(et_user.getWindowToken(), 0);

        if(!Util.isOnline(context))
        {
            loginLoggingIn.setVisibility(View.GONE);
            loginLogin.setVisibility(View.VISIBLE);
            scrollView.setBackgroundColor(getResources().getColor(R.color.background_create_account));
            loginDelimiter.setVisibility(View.VISIBLE);
            loginCreateAccount.setVisibility(View.VISIBLE);
            queryingSignupLinkText.setVisibility(View.GONE);
            confirmingAccountText.setVisibility(View.GONE);
            generatingKeysText.setVisibility(View.GONE);
            loggingInText.setVisibility(View.GONE);
            fetchingNodesText.setVisibility(View.GONE);
            prepareNodesText.setVisibility(View.GONE);
            serversBusyText.setVisibility(View.GONE);

            ((LoginActivityLollipop)context).showSnackbar(getString(R.string.error_server_connection_problem));
            return;
        }

        loginLogin.setVisibility(View.GONE);
        scrollView.setBackgroundColor(getResources().getColor(R.color.white));
        loginDelimiter.setVisibility(View.GONE);
        loginCreateAccount.setVisibility(View.GONE);
        loginLoggingIn.setVisibility(View.VISIBLE);
        scrollView.setBackgroundColor(getResources().getColor(R.color.white));
        generatingKeysText.setVisibility(View.VISIBLE);
        loginProgressBar.setVisibility(View.VISIBLE);
        loginFetchNodesProgressBar.setVisibility(View.GONE);
        queryingSignupLinkText.setVisibility(View.GONE);
        confirmingAccountText.setVisibility(View.GONE);

        lastEmail = et_user.getText().toString().toLowerCase(Locale.ENGLISH).trim();
        lastPassword = et_password.getText().toString();

        log("generating keys");

        new HashTask().execute(lastEmail, lastPassword);
    }

    private void onKeysGenerated(String privateKey, String publicKey) {
        log("key generation finished");

        this.gPrivateKey = privateKey;
        this.gPublicKey = publicKey;

        if (confirmLink == null) {
            onKeysGeneratedLogin(privateKey, publicKey);
        }
        else{
            if(!Util.isOnline(context)){
                ((LoginActivityLollipop)context).showSnackbar(getString(R.string.error_server_connection_problem));
                return;
            }

            loginLogin.setVisibility(View.GONE);
            scrollView.setBackgroundColor(getResources().getColor(R.color.white));
            loginDelimiter.setVisibility(View.GONE);
            loginCreateAccount.setVisibility(View.GONE);
            loginLoggingIn.setVisibility(View.VISIBLE);
            scrollView.setBackgroundColor(getResources().getColor(R.color.white));
            generatingKeysText.setVisibility(View.VISIBLE);
            loginProgressBar.setVisibility(View.VISIBLE);
            loginFetchNodesProgressBar.setVisibility(View.GONE);
            queryingSignupLinkText.setVisibility(View.GONE);
            confirmingAccountText.setVisibility(View.VISIBLE);
            fetchingNodesText.setVisibility(View.GONE);
            prepareNodesText.setVisibility(View.GONE);
            serversBusyText.setVisibility(View.GONE);

            log("fastConfirm");
            megaApi.fastConfirmAccount(confirmLink, privateKey, this);
        }
    }

    private void onKeysGeneratedLogin(final String privateKey, final String publicKey) {

        if(!Util.isOnline(context)){
            loginLoggingIn.setVisibility(View.GONE);
            loginLogin.setVisibility(View.VISIBLE);
            scrollView.setBackgroundColor(getResources().getColor(R.color.background_create_account));
            loginDelimiter.setVisibility(View.VISIBLE);
            loginCreateAccount.setVisibility(View.VISIBLE);
            queryingSignupLinkText.setVisibility(View.GONE);
            confirmingAccountText.setVisibility(View.GONE);
            generatingKeysText.setVisibility(View.GONE);
            loggingInText.setVisibility(View.GONE);
            fetchingNodesText.setVisibility(View.GONE);
            prepareNodesText.setVisibility(View.GONE);
            serversBusyText.setVisibility(View.GONE);

            ((LoginActivityLollipop)context).showSnackbar(getString(R.string.error_server_connection_problem));
            return;
        }

        loggingInText.setVisibility(View.VISIBLE);
        fetchingNodesText.setVisibility(View.GONE);
        prepareNodesText.setVisibility(View.GONE);
        serversBusyText.setVisibility(View.GONE);

        log("fastLogin con publicKey y privateKey");
        megaApi.fastLogin(lastEmail, publicKey, privateKey, this);
    }

    /*
     * Validate email and password
     */
    private boolean validateForm() {
        String emailError = getEmailError();
        String passwordError = getPasswordError();

        et_user.setError(emailError);
        et_password.setError(passwordError);

        if (emailError != null) {
            et_user.requestFocus();
            return false;
        } else if (passwordError != null) {
            et_password.requestFocus();
            return false;
        }
        return true;
    }

    public void onLoginClick(View v){
        submitForm();
    }

    public void onRegisterClick(View v){
        //Change fragmentVisible in the activity
        ((LoginActivityLollipop)context).showFragment(Constants.CREATE_ACCOUNT_FRAGMENT);
    }

    /*
     * Validate email
     */
    private String getEmailError() {
        String value = et_user.getText().toString();
        if (value.length() == 0) {
            return getString(R.string.error_enter_email);
        }
        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(value).matches()) {
            return getString(R.string.error_invalid_email);
        }
        return null;
    }

    /*
     * Validate password
     */
    private String getPasswordError() {
        String value = et_password.getText().toString();
        if (value.length() == 0) {
            return getString(R.string.error_enter_password);
        }
        return null;
    }


    @Override
    public void onClick(View v) {

        switch(v.getId()) {
            case R.id.button_login_login: {
                log("click on button_login_login");
                loginClicked = true;
                onLoginClick(v);
                break;
            }
            case R.id.button_create_account_login:{
                log("click on button_create_account_login");
                onRegisterClick(v);
                break;
            }
            case R.id.park_account_button:{
                log("click to park account");
                showDialogInsertMail(false);
                break;
            }
            case R.id.button_forgot_pass:{
                log("click on button_forgot_pass");
                showForgotPassLayout();
                break;
            }
            case R.id.yes_MK_button:{
                log("click on yes_MK_button");
                showDialogInsertMail(true);
                break;
            }
            case R.id.no_MK_button:{
                log("click on no_MK_button");
                showParkAccountLayout();
                break;
            }
            case R.id.login_text_view:{
                numberOfClicks++;
                if (numberOfClicks == 5){
                    MegaAttributes attrs = dbH.getAttributes();
                    if (attrs.getFileLogger() != null){
                        try {
                            if (Boolean.parseBoolean(attrs.getFileLogger()) == false) {
                                dbH.setFileLogger(true);
                                Util.setFileLogger(true);
                                numberOfClicks = 0;
                                MegaApiAndroid.setLogLevel(MegaApiAndroid.LOG_LEVEL_MAX);
                                ((LoginActivityLollipop)context).showSnackbar(getString(R.string.settings_enable_logs));
                                log("App Version: " + Util.getVersion(context));
                            }
                            else{
                                dbH.setFileLogger(false);
                                Util.setFileLogger(false);
                                numberOfClicks = 0;
                                MegaApiAndroid.setLogLevel(MegaApiAndroid.LOG_LEVEL_FATAL);
                                ((LoginActivityLollipop)context).showSnackbar(getString(R.string.settings_disable_logs));
                            }
                        }
                        catch(Exception e){
                            dbH.setFileLogger(true);
                            Util.setFileLogger(true);
                            numberOfClicks = 0;
                            ((LoginActivityLollipop)context).showSnackbar(getString(R.string.settings_enable_logs));
                        }
                    }
                    else{
                        dbH.setFileLogger(true);
                        Util.setFileLogger(true);
                        numberOfClicks = 0;
                        ((LoginActivityLollipop)context).showSnackbar(getString(R.string.settings_enable_logs));
                    }
                }
            }
        }
    }


    public void showForgotPassLayout(){
        log("showForgotPassLayout");
        loginLoggingIn.setVisibility(View.GONE);
        loginLogin.setVisibility(View.GONE);
        parkAccountLayout.setVisibility(View.GONE);
        forgotPassLayout.setVisibility(View.VISIBLE);
        scrollView.setBackgroundColor(getResources().getColor(R.color.white));
    }

    public void hideForgotPassLayout(){
        log("hideForgotPassLayout");
        loginLoggingIn.setVisibility(View.GONE);
        forgotPassLayout.setVisibility(View.GONE);
        parkAccountLayout.setVisibility(View.GONE);
        loginLogin.setVisibility(View.VISIBLE);
        scrollView.setBackgroundColor(getResources().getColor(R.color.background_create_account));
    }

    public void showParkAccountLayout(){
        log("showParkAccountLayout");
        loginLoggingIn.setVisibility(View.GONE);
        loginLogin.setVisibility(View.GONE);
        forgotPassLayout.setVisibility(View.GONE);
        parkAccountLayout.setVisibility(View.VISIBLE);
        scrollView.setBackgroundColor(getResources().getColor(R.color.white));
    }

    public void hideParkAccountLayout(){
        log("hideParkAccountLayout");
        loginLoggingIn.setVisibility(View.GONE);
        forgotPassLayout.setVisibility(View.GONE);
        parkAccountLayout.setVisibility(View.GONE);
        loginLogin.setVisibility(View.VISIBLE);
        scrollView.setBackgroundColor(getResources().getColor(R.color.background_create_account));
    }

    /*
     * Get email address from confirmation code and set to emailView
     */
    private void updateConfirmEmail(String link) {
        if(!Util.isOnline(context)){
            ((LoginActivityLollipop)context).showSnackbar(getString(R.string.error_server_connection_problem));
            return;
        }

        loginLogin.setVisibility(View.GONE);
        scrollView.setBackgroundColor(getResources().getColor(R.color.white));
        loginDelimiter.setVisibility(View.GONE);
        loginCreateAccount.setVisibility(View.GONE);
        loginLoggingIn.setVisibility(View.VISIBLE);
        scrollView.setBackgroundColor(getResources().getColor(R.color.white));
        generatingKeysText.setVisibility(View.GONE);
        queryingSignupLinkText.setVisibility(View.VISIBLE);
        confirmingAccountText.setVisibility(View.GONE);
        fetchingNodesText.setVisibility(View.GONE);
        prepareNodesText.setVisibility(View.GONE);
        serversBusyText.setVisibility(View.GONE);
        loginProgressBar.setVisibility(View.VISIBLE);
        log("querySignupLink");
        megaApi.querySignupLink(link, this);
    }

    /*
 * Handle intent from confirmation email
 */
    public void handleConfirmationIntent(Intent intent) {
        confirmLink = intent.getStringExtra(Constants.EXTRA_CONFIRMATION);
        loginTitle.setText(R.string.login_confirm_account);
        bLogin.setText(getString(R.string.login_confirm_account).toUpperCase(Locale.getDefault()));
        updateConfirmEmail(confirmLink);
    }

    @Override
    public void onAttach(Context context) {
        log("onAttach");
        super.onAttach(context);
        this.context = context;

        dbH = DatabaseHandler.getDbHandler(context);

        if (megaApi == null){
            megaApi = ((MegaApplication) ((Activity)context).getApplication()).getMegaApi();
        }
    }

    @Override
    public void onAttach(Activity context) {
        log("onAttach Activity");
        super.onAttach(context);
        this.context = context;

        dbH = DatabaseHandler.getDbHandler(context);

        if (megaApi == null){
            megaApi = ((MegaApplication) ((Activity)context).getApplication()).getMegaApi();
        }
    }

    @Override
    public void onRequestUpdate(MegaApiJava api, MegaRequest request) {

        try{
            if(timer!=null){
                timer.cancel();
                serversBusyText.setVisibility(View.GONE);
            }
        }
        catch(Exception e){
            log("TIMER EXCEPTION");
            log(e.getMessage());
        }
//		log("onRequestUpdate: " + request.getRequestString());
        if (request.getType() == MegaRequest.TYPE_FETCH_NODES){
            if (firstRequestUpdate){
                loginProgressBar.setVisibility(View.GONE);
                firstRequestUpdate = false;
            }
            loginFetchNodesProgressBar.setVisibility(View.VISIBLE);
            loginFetchNodesProgressBar.getLayoutParams().width = Util.px2dp((250*scaleW), outMetrics);
            if (request.getTotalBytes() > 0){
                double progressValue = 100.0 * request.getTransferredBytes() / request.getTotalBytes();
                if ((progressValue > 99) || (progressValue < 0)){
                    progressValue = 100;
                    prepareNodesText.setVisibility(View.VISIBLE);
                    loginProgressBar.setVisibility(View.VISIBLE);
                }
//				log("progressValue = " + (int)progressValue);
                loginFetchNodesProgressBar.setProgress((int)progressValue);
            }
        }
    }

    @Override
    public void onRequestStart(MegaApiJava api, MegaRequest request)
    {
        log("onRequestStart: " + request.getRequestString());
        if (request.getType() == MegaRequest.TYPE_FETCH_NODES){
//			loginProgressBar.setVisibility(View.GONE);
            loginFetchNodesProgressBar.setVisibility(View.VISIBLE);
            loginFetchNodesProgressBar.getLayoutParams().width = Util.px2dp((250*scaleW), outMetrics);
            loginFetchNodesProgressBar.setProgress(0);
        }
    }

    @Override
    public void onRequestFinish(MegaApiJava api, MegaRequest request, MegaError error) {
        try{
            if(timer!=null){
                timer.cancel();
                serversBusyText.setVisibility(View.GONE);
            }
        }
        catch(Exception e){
            log("TIMER EXCEPTION");
            log(e.getMessage());
        }

        log("onRequestFinish: " + request.getRequestString());
        if (request.getType() == MegaRequest.TYPE_LOGIN){
            if (error.getErrorCode() != MegaError.API_OK) {
                String errorMessage;
                if (error.getErrorCode() == MegaError.API_ENOENT) {
                    errorMessage = getString(R.string.error_incorrect_email_or_password);
                }
                else if (error.getErrorCode() == MegaError.API_ESID){
                    errorMessage = getString(R.string.error_server_expired_session);
                }
                else if (error.getErrorCode() == MegaError.API_ETOOMANY){
                    errorMessage = getString(R.string.too_many_attempts_login);
                }
                else if (error.getErrorCode() == MegaError.API_EINCOMPLETE){
                    errorMessage = getString(R.string.account_not_validated_login);
                }
                else if (error.getErrorCode() == MegaError.API_EBLOCKED){
                    errorMessage = getString(R.string.error_account_suspended);
                }
                else{
                    errorMessage = error.getErrorString();
                }
                log("LOGIN_ERROR: "+error.getErrorCode()+ " "+error.getErrorString());
                loginLoggingIn.setVisibility(View.GONE);
                loginLogin.setVisibility(View.VISIBLE);
                scrollView.setBackgroundColor(getResources().getColor(R.color.background_create_account));
                loginDelimiter.setVisibility(View.VISIBLE);
                loginCreateAccount.setVisibility(View.VISIBLE);
                queryingSignupLinkText.setVisibility(View.GONE);
                confirmingAccountText.setVisibility(View.GONE);
                generatingKeysText.setVisibility(View.GONE);
                loggingInText.setVisibility(View.GONE);
                fetchingNodesText.setVisibility(View.GONE);
                prepareNodesText.setVisibility(View.GONE);
                serversBusyText.setVisibility(View.GONE);

                ((LoginActivityLollipop)context).showSnackbar(errorMessage);

//				DatabaseHandler dbH = new DatabaseHandler(this);
                DatabaseHandler dbH = DatabaseHandler.getDbHandler(context.getApplicationContext());
                dbH.clearCredentials();
                if (dbH.getPreferences() != null){
                    ((LoginActivityLollipop)context).stopCameraSyncService();
                }
            }
            else{

                loginProgressBar.setVisibility(View.VISIBLE);
                loginFetchNodesProgressBar.setVisibility(View.GONE);
                loggingInText.setVisibility(View.VISIBLE);
                fetchingNodesText.setVisibility(View.VISIBLE);
                prepareNodesText.setVisibility(View.GONE);
                serversBusyText.setVisibility(View.GONE);

                gSession = megaApi.dumpSession();
                UserCredentials credentials = new UserCredentials(lastEmail, gSession);

//				DatabaseHandler dbH = new DatabaseHandler(getApplicationContext());
                DatabaseHandler dbH = DatabaseHandler.getDbHandler(context.getApplicationContext());
                dbH.clearCredentials();

                log("Logged in: " + gSession);

//				String session = megaApi.dumpSession();
//				Toast.makeText(this, "Session = " + session, Toast.LENGTH_LONG).show();

                //TODO
                //addAccount (email, session)
//				String accountType = getIntent().getStringExtra(ARG_ACCOUNT_TYPE);
//				if (accountType != null){
//					authTokenType = getIntent().getStringExtra(ARG_AUTH_TYPE);
//					if (authTokenType == null){
//						authTokenType = .AUTH_TOKEN_TYPE_INSTANTIATE;
//					}
//					Account account = new Account(lastEmail, accountscroll_view_loginType);
//					accountManager.addAccountExplicitly(account, gSession, null);
//					log("AUTTHO: _" + authTokenType + "_");
//					accountManager.setAuthToken(account, authTokenType, gSession);
//				}

                megaApi.fetchNodes(this);
            }
        }
        else if(request.getType() == MegaRequest.TYPE_GET_RECOVERY_LINK){
            log("TYPE_GET_RECOVERY_LINK");
            if (error.getErrorCode() == MegaError.API_OK){
                log("The recovery link has been sent");
                Util.showAlert(context, getString(R.string.email_verification_text), getString(R.string.email_verification_title));
            }
            else if (error.getErrorCode() == MegaError.API_ENOENT){
                log("No account with this mail: "+error.getErrorString()+" "+error.getErrorCode());
                Util.showAlert(context, getString(R.string.invalid_email_text), getString(R.string.invalid_email_title));
            }
            else{
                log("Error when asking for recovery pass link");
                log(error.getErrorString() + "___" + error.getErrorCode());
                Util.showAlert(context,getString(R.string.email_verification_text_error), getString(R.string.general_error_word));
            }
        }
        else if (request.getType() == MegaRequest.TYPE_FETCH_NODES){
            if (error.getErrorCode() == MegaError.API_OK){
                log("ok fetch nodes");
                DatabaseHandler dbH = DatabaseHandler.getDbHandler(context.getApplicationContext());

                gSession = megaApi.dumpSession();
                lastEmail = megaApi.getMyUser().getEmail();
                UserCredentials credentials = new UserCredentials(lastEmail, gSession);

                dbH.saveCredentials(credentials);
            }
            if(confirmLink==null){
                log("confirmLink==null");
                if (error.getErrorCode() != MegaError.API_OK) {
                    log("Error fetch nodes");
                    String errorMessage;
                    if (error.getErrorCode() == MegaError.API_ESID){
                        errorMessage = getString(R.string.error_server_expired_session);
                    }
                    else if (error.getErrorCode() == MegaError.API_ETOOMANY){
                        errorMessage = getString(R.string.too_many_attempts_login);
                    }
                    else if (error.getErrorCode() == MegaError.API_EINCOMPLETE){
                        errorMessage = getString(R.string.account_not_validated_login);
                    }
                    else if (error.getErrorCode() == MegaError.API_EBLOCKED){
                        errorMessage = getString(R.string.error_account_suspended);
                    }
                    else{
                        errorMessage = error.getErrorString();
                    }
                    loginLoggingIn.setVisibility(View.GONE);
                    loginLogin.setVisibility(View.VISIBLE);
                    scrollView.setBackgroundColor(getResources().getColor(R.color.background_create_account));
                    loginDelimiter.setVisibility(View.VISIBLE);
                    loginCreateAccount.setVisibility(View.VISIBLE);
                    generatingKeysText.setVisibility(View.GONE);
                    loggingInText.setVisibility(View.GONE);
                    fetchingNodesText.setVisibility(View.GONE);
                    prepareNodesText.setVisibility(View.GONE);
                    serversBusyText.setVisibility(View.GONE);
                    queryingSignupLinkText.setVisibility(View.GONE);
                    confirmingAccountText.setVisibility(View.GONE);

                    ((LoginActivityLollipop)context).showSnackbar(errorMessage);
                }
                else{
                    log("OK fetch nodes");
                    if((action!=null)&&(url!=null)) {
                        if (action.equals(Constants.ACTION_CHANGE_MAIL)) {
                            log("Action change mail after fetch nodes");
                            Intent changeMailIntent = new Intent(context, ManagerActivityLollipop.class);
                            changeMailIntent.setAction(Constants.ACTION_CHANGE_MAIL);
                            changeMailIntent.setData(Uri.parse(url));
                            startActivity(changeMailIntent);
                            ((LoginActivityLollipop) context).finish();
                        }
                        else if(action.equals(Constants.ACTION_RESET_PASS)) {
                            log("Action reset pass after fetch nodes");
                            Intent resetPassIntent = new Intent(context, ManagerActivityLollipop.class);
                            resetPassIntent.setAction(Constants.ACTION_RESET_PASS);
                            resetPassIntent.setData(Uri.parse(url));
                            startActivity(resetPassIntent);
                            ((LoginActivityLollipop) context).finish();
                        }
                        else if(action.equals(Constants.ACTION_CANCEL_ACCOUNT)) {
                            log("Action cancel Account after fetch nodes");
                            Intent cancelAccountIntent = new Intent(context, ManagerActivityLollipop.class);
                            cancelAccountIntent.setAction(Constants.ACTION_CANCEL_ACCOUNT);
                            cancelAccountIntent.setData(Uri.parse(url));
                            startActivity(cancelAccountIntent);
                            ((LoginActivityLollipop) context).finish();
                        }
                    }

                    if (!backWhileLogin){
                        log("NOT backWhileLogin");
                        if (parentHandle != -1){
                            Intent intent = new Intent();
                            intent.putExtra("PARENT_HANDLE", parentHandle);
                            ((LoginActivityLollipop) context).setResult(Activity.RESULT_OK, intent);
                            ((LoginActivityLollipop) context).finish();
                        }
                        else{
                            Intent intent = null;
                            if (firstTime){
                                log("First time");
                                intent = new Intent(context,ManagerActivityLollipop.class);
                                intent.putExtra("firstTimeCam", true);
                                if (action != null){
                                    log("Action not NULL");
                                    if (action.equals(Constants.ACTION_EXPORT_MASTER_KEY)){
                                        log("ACTION_EXPORT_MK");
                                        intent.setAction(action);
                                    }
                                }
                            }
                            else{
                                boolean initialCam = false;
//								DatabaseHandler dbH = new DatabaseHandler(getApplicationContext());
                                DatabaseHandler dbH = DatabaseHandler.getDbHandler(context.getApplicationContext());
                                MegaPreferences prefs = dbH.getPreferences();
                                prefs = dbH.getPreferences();
                                if (prefs != null){
                                    if (prefs.getCamSyncEnabled() != null){
                                        if (Boolean.parseBoolean(prefs.getCamSyncEnabled())){
                                            ((LoginActivityLollipop) context).startCameraSyncService(false, 30 * 1000);
                                        }
                                    }
                                    else{
                                        ((LoginActivityLollipop) context).startCameraSyncService(true, 30 * 1000);
                                        initialCam = true;
                                    }
                                }
                                else{
                                    intent = new Intent(context,ManagerActivityLollipop.class);
                                    intent.putExtra("firstTimeCam", true);
                                    initialCam = true;
                                }

                                if (!initialCam){
                                    log("NOT initialCam");
                                    intent = new Intent(context,ManagerActivityLollipop.class);
                                    if (action != null){
                                        log("The action is: "+action);
//										if (action.equals(ManagerActivityLollipop.ACTION_FILE_EXPLORER_UPLOAD)){
//											intent = new Intent(this, FileExplorerActivityLollipop.class);
//											if(extras != null)
//											{
//												intent.putExtras(extras);
//											}
//											intent.setData(uriData);
//										}
                                        if (action.equals(Constants.ACTION_FILE_PROVIDER)){
                                            intent = new Intent(context, FileProviderActivity.class);
                                            if(extras != null)
                                            {
                                                intent.putExtras(extras);
                                            }
                                            if(uriData != null)
                                            {
                                                intent.setData(uriData);
                                            }
                                        }
                                        intent.setAction(action);
                                        if (url != null){
                                            intent.setData(Uri.parse(url));
                                        }
                                    }
                                }
                                else{
                                    log("initialCam YESSSS");
                                    intent = new Intent(context,ManagerActivityLollipop.class);
                                    if (action != null){
                                        log("The action is: "+action);
                                        intent.setAction(action);
                                    }
                                }
                                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                            }

                            startActivity(intent);
                            ((LoginActivityLollipop)context).finish();
                        }
                    }
                }
            }
            else{
                log("Go to ChooseAccountFragment");
                ((LoginActivityLollipop)context).showFragment(Constants.CHOOSE_ACCOUNT_FRAGMENT);
            }

        }
        else if (request.getType() == MegaRequest.TYPE_QUERY_SIGNUP_LINK){
            log("MegaRequest.TYPE_QUERY_SIGNUP_LINK");
            String s = "";
            loginLogin.setVisibility(View.VISIBLE);
            scrollView.setBackgroundColor(getResources().getColor(R.color.background_create_account));
            bForgotPass.setVisibility(View.INVISIBLE);
            loginDelimiter.setVisibility(View.VISIBLE);
            loginCreateAccount.setVisibility(View.VISIBLE);
            loginLoggingIn.setVisibility(View.GONE);
            generatingKeysText.setVisibility(View.GONE);
            queryingSignupLinkText.setVisibility(View.GONE);
            confirmingAccountText.setVisibility(View.GONE);
            fetchingNodesText.setVisibility(View.GONE);
            prepareNodesText.setVisibility(View.GONE);
            serversBusyText.setVisibility(View.GONE);

            if(error.getErrorCode() == MegaError.API_OK){
                s = request.getEmail();
                et_user.setText(s);
                et_password.requestFocus();
            }
            else{
                ((LoginActivityLollipop)context).showSnackbar(error.getErrorString());
                confirmLink = null;
            }
        }
        else if (request.getType() == MegaRequest.TYPE_CONFIRM_ACCOUNT){
            if (error.getErrorCode() == MegaError.API_OK){
                log("fastConfirm finished - OK");
                onKeysGeneratedLogin(gPrivateKey, gPublicKey);
            }
            else{
                loginLogin.setVisibility(View.VISIBLE);
                scrollView.setBackgroundColor(getResources().getColor(R.color.background_create_account));
                loginDelimiter.setVisibility(View.VISIBLE);
                loginCreateAccount.setVisibility(View.VISIBLE);
                loginLoggingIn.setVisibility(View.GONE);
                generatingKeysText.setVisibility(View.GONE);
                queryingSignupLinkText.setVisibility(View.GONE);
                confirmingAccountText.setVisibility(View.GONE);
                fetchingNodesText.setVisibility(View.GONE);
                prepareNodesText.setVisibility(View.GONE);
                serversBusyText.setVisibility(View.GONE);

                if (error.getErrorCode() == MegaError.API_ENOENT){
                    ((LoginActivityLollipop)context).showSnackbar(getString(R.string.error_incorrect_email_or_password));
                }
                else{
                    ((LoginActivityLollipop)context).showSnackbar(error.getErrorString());
                }
            }
        }
    }

    @Override
    public void onRequestTemporaryError(MegaApiJava api, MegaRequest request, MegaError e)
    {
        log("onRequestTemporaryError: " + request.getRequestString());

//		if (request.getType() == MegaRequest.TYPE_LOGIN){
//
//		}
//		else if (request.getType() == MegaRequest.TYPE_FETCH_NODES){
//
//		}
        try{
            timer = new CountDownTimer(10000, 2000) {

                public void onTick(long millisUntilFinished) {
                    log("TemporaryError one more");
                }

                public void onFinish() {
                    log("the timer finished, message shown");
                    serversBusyText.setVisibility(View.VISIBLE);
                }
            }.start();
        }catch (Exception exception){
            log(exception.getMessage());
            log("EXCEPTION when starting count");
        }
    }


    public void showDialogInsertMail(final boolean reset){
        log("showDialogInsertMail");

        LinearLayout layout = new LinearLayout(context);
        layout.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        params.setMargins(Util.scaleWidthPx(20, outMetrics), Util.scaleHeightPx(20, outMetrics), Util.scaleWidthPx(17, outMetrics), 0);

        final EditText input = new EditText(context);
        layout.addView(input, params);

//		input.setId(EDIT_TEXT_ID);
        input.setSingleLine();
        input.setHint(getString(R.string.edit_text_insert_mail));
        input.setTextColor(getResources().getColor(R.color.text_secondary));
        input.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
//		input.setSelectAllOnFocus(true);
        input.setImeOptions(EditorInfo.IME_ACTION_DONE);
        input.setInputType(InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS);
        input.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId,	KeyEvent event) {
                log("OK RESET PASSWORD");
                if (actionId == EditorInfo.IME_ACTION_DONE) {
                    String value = input.getText().toString().trim();
                    String emailError = Util.getEmailError(value, context);
                    if (emailError != null) {
                        log("mail incorrect");
                        input.setError(emailError);
                        input.requestFocus();
                    } else {
                        if(reset){
                            log("ask for link to reset pass");
                            megaApi.resetPassword(value, true, loginFragment);
                        }
                        else{
                            log("ask for link to park account");
                            megaApi.resetPassword(value, false, loginFragment);
                        }
                        insertMailDialog.dismiss();
                    }
                }
                else{
                    log("other IME" + actionId);
                }
                return false;
            }
        });
        input.setImeActionLabel(getString(R.string.general_add),EditorInfo.IME_ACTION_DONE);
        input.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if (hasFocus) {
                    showKeyboardDelayed(v);
                }
                else{
                    hideKeyboardDelayed(v);
                }
            }
        });
        String title;
        String text;
        String buttonText;
        AlertDialog.Builder builder = new AlertDialog.Builder(context, R.style.AppCompatAlertDialogStyle);
        if(reset){
            title= getString(R.string.title_alert_reset_with_MK);
            text = getString(R.string.text_alert_reset_with_MK);
            buttonText=getString(R.string.context_send);
        }
        else{
            title= getString(R.string.park_account_dialog_title);
            text = getString(R.string.dialog_park_account);
            buttonText=getString(R.string.park_account_button);
        }
        builder.setTitle(title);
        builder.setMessage(text);
        builder.setPositiveButton(buttonText,
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {

                    }
                });
        builder.setOnDismissListener(new DialogInterface.OnDismissListener() {
            @Override
            public void onDismiss(DialogInterface dialog) {
                View view = ((LoginActivityLollipop) context).getCurrentFocus();
                if (view != null) {
                    InputMethodManager inputManager = (InputMethodManager) ((LoginActivityLollipop) context).getSystemService(Context.INPUT_METHOD_SERVICE);
                    inputManager.hideSoftInputFromWindow(view.getWindowToken(), InputMethodManager.HIDE_NOT_ALWAYS);
                }
            }
        });
        builder.setNegativeButton(getString(android.R.string.cancel), null);
        builder.setView(layout);
        insertMailDialog = builder.create();
        insertMailDialog.show();
        insertMailDialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                log("OK BTTN PASSWORD");
                String value = input.getText().toString().trim();
                String emailError = Util.getEmailError(value, context);
                if (emailError != null) {
                    log("mail incorrect");
                    input.setError(emailError);
                } else {
                    if(reset){
                        log("ask for link to reset pass");
                        megaApi.resetPassword(value, true, loginFragment);
                    }
                    else{
                        log("ask for link to park account");
                        megaApi.resetPassword(value, false, loginFragment);
                    }

                    insertMailDialog.dismiss();
                }
            }
        });
    }

    /*
     * Display keyboard
     */
    private void showKeyboardDelayed(final View view) {
        log("showKeyboardDelayed");
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                InputMethodManager imm = (InputMethodManager) context.getSystemService(Context.INPUT_METHOD_SERVICE);
                imm.showSoftInput(view, InputMethodManager.SHOW_IMPLICIT);
            }
        }, 50);
    }

    private void hideKeyboardDelayed(final View view) {
        log("showKeyboardDelayed");
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                InputMethodManager imm = (InputMethodManager) context.getSystemService(Context.INPUT_METHOD_SERVICE);
                if (imm.isActive()) {
                    imm.toggleSoftInput(0, InputMethodManager.HIDE_NOT_ALWAYS);
                }
            }
        }, 50);
    }

    public void showConfirmationParkAccount(String link){
        log("showConfirmationParkAccount");

        final String linkUrl = link;

        DialogInterface.OnClickListener dialogClickListener = new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                switch (which){
                    case DialogInterface.BUTTON_POSITIVE:
                        log("Call to Change Password Activity: "+linkUrl);
                        Intent intent = new Intent(context, ChangePasswordActivityLollipop.class);
                        intent.setAction(Constants.ACTION_RESET_PASS_FROM_PARK_ACCOUNT);
                        intent.setData(Uri.parse(linkUrl));
                        startActivity(intent);
                        break;

                    case DialogInterface.BUTTON_NEGATIVE:
                        //No button clicked
                        break;
                }
            }
        };

        AlertDialog.Builder builder = new AlertDialog.Builder(context, R.style.AppCompatAlertDialogStyle);
        builder.setTitle(getResources().getString(R.string.park_account_dialog_title));
        String message= getResources().getString(R.string.park_account_text_last_step);
        builder.setMessage(message).setPositiveButton(R.string.pin_lock_enter, dialogClickListener)
                .setNegativeButton(R.string.general_cancel, dialogClickListener).show();
    }

    public void showDialogInsertMKToChangePass(String link){
        log("showDialogInsertMKToChangePass");

        final String linkUrl = link;

        LinearLayout layout = new LinearLayout(context);
        layout.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        params.setMargins(Util.scaleWidthPx(20, outMetrics), Util.scaleHeightPx(20, outMetrics), Util.scaleWidthPx(17, outMetrics), 0);

        final EditText input = new EditText(context);
        layout.addView(input, params);

//		input.setId(EDIT_TEXT_ID);
        input.setSingleLine();
        input.setHint(getString(R.string.edit_text_insert_mk));
        input.setTextColor(getResources().getColor(R.color.text_secondary));
        input.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
//		input.setSelectAllOnFocus(true);
        input.setImeOptions(EditorInfo.IME_ACTION_DONE);
        input.setInputType(InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS);
        input.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId,	KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_DONE) {
                    log("IME OK BTTN PASSWORD");
                    String value = input.getText().toString().trim();
                    if(value.equals("")||value.isEmpty()){
                        log("input is empty");
                        input.setError(getString(R.string.invalid_string));
                        input.requestFocus();
                    }
                    else {

                        log("positive button pressed - reset pass");
                        Intent intent = new Intent(context, ChangePasswordActivityLollipop.class);
                        intent.setAction(Constants.ACTION_RESET_PASS_FROM_LINK);
                        intent.setData(Uri.parse(linkUrl));
                        intent.putExtra("MK", value);
                        startActivity(intent);
                        insertMKDialog.dismiss();
                    }
                }
                else{
                    log("other IME" + actionId);
                }
                return false;
            }
        });
        input.setImeActionLabel(getString(R.string.general_add),EditorInfo.IME_ACTION_DONE);

        AlertDialog.Builder builder = new AlertDialog.Builder(context, R.style.AppCompatAlertDialogStyle);
        builder.setTitle(getString(R.string.title_dialog_insert_MK));
        builder.setMessage(getString(R.string.text_dialog_insert_MK));
        builder.setPositiveButton(getString(R.string.cam_sync_ok),
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {

                    }
                });
        builder.setOnDismissListener(new DialogInterface.OnDismissListener() {
            @Override
            public void onDismiss(DialogInterface dialog) {
                View view = ((LoginActivityLollipop) context).getCurrentFocus();
                if (view != null) {
                    InputMethodManager inputManager = (InputMethodManager) context.getSystemService(Context.INPUT_METHOD_SERVICE);
                    inputManager.hideSoftInputFromWindow(view.getWindowToken(), InputMethodManager.HIDE_NOT_ALWAYS);
                }
            }
        });
        builder.setNegativeButton(getString(android.R.string.cancel), null);
        builder.setView(layout);
        insertMKDialog = builder.create();
        insertMKDialog.show();
        insertMKDialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                log("OK BTTN PASSWORD");
                String value = input.getText().toString().trim();
                if(value.equals("")||value.isEmpty()){
                    log("input is empty");
                    input.setError(getString(R.string.invalid_string));
                    input.requestFocus();
                }
                else {
                    log("positive button pressed - reset pass");
                    Intent intent = new Intent(context, ChangePasswordActivityLollipop.class);
                    intent.setAction(Constants.ACTION_RESET_PASS_FROM_LINK);
                    intent.setData(Uri.parse(linkUrl));
                    intent.putExtra("MK", value);
                    startActivity(intent);
                    insertMKDialog.dismiss();
                }
            }
        });
    }

    @Override
    public void onDestroy(){
        if(megaApi != null)
        {
            megaApi.removeRequestListener(this);
        }

        super.onDestroy();
    }

    public int onBackPressed() {
        log("onBackPressed");

        backWhileLogin = true;

        if (loginClicked){
            return 0;
        }
        else{

            if(forgotPassLayout.getVisibility()==View.VISIBLE){
                log("Forgot Pass layout is VISIBLE");
                hideForgotPassLayout();
                return 1;
            }

            if(parkAccountLayout.getVisibility()==View.VISIBLE){
                log("Park account layout is VISIBLE");
                hideParkAccountLayout();
                return 1;
            }

            ((LoginActivityLollipop) context).showFragment(Constants.TOUR_FRAGMENT);
            return 1;
        }
    }

    private static void log(String log) {
        Util.log("LoginFragmentLollipop", log);
    }
}
