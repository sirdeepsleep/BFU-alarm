package installation.fire.wall;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ComponentName;
import android.content.pm.PackageInstaller;
import android.graphics.Color;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.view.DisplayCutout;
import android.view.Gravity;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.io.OutputStream;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends Activity {

    private final Handler handler = new Handler(Looper.getMainLooper());

    private static final String ACTION_INSTALL_COMPLETE = "com.fire.wall.INSTALL_COMPLETE";

    private EditText etStub;
    private Button btnGenerate;
    
    private EditText etPermPackage;
    private Button btnGeneratePerm;

    private TextView tvStatus;
    
    private Button btnOpenPlayStore;
    private Button btnOpenPlayServices;
    private Button btnOpenGsf;
    private Button btnOpenDeviceAdmin;
    private TextView tvLinkKeepAndroidOpen;

    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    private boolean waitForInstall = false;
    private int pendingInstallStatus = -1;
    private String pendingInstallMessage = null;

    private final BroadcastReceiver packageReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (Intent.ACTION_PACKAGE_ADDED.equals(action) || Intent.ACTION_PACKAGE_REPLACED.equals(action)) {
                String pkgName = intent.getData() != null ? intent.getData().getSchemeSpecificPart() : getString(R.string.unknown_package);
                Toast.makeText(context, getString(R.string.install_success_pkg, pkgName), Toast.LENGTH_LONG).show();
            }
        }
    };

    private final BroadcastReceiver installStatusReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (ACTION_INSTALL_COMPLETE.equals(intent.getAction())) {
                handleInstallerCallback(intent);
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if (Build.VERSION.SDK_INT >= 34) {
            View rootView = findViewById(android.R.id.content);
            rootView.setOnApplyWindowInsetsListener((v, insets) -> {
                DisplayCutout cutout = insets.getDisplayCutout();
                if (cutout != null) {
                    v.setPadding(v.getPaddingLeft(), cutout.getSafeInsetTop(), v.getPaddingRight(), v.getPaddingBottom());
                } else {
                    v.setPadding(v.getPaddingLeft(), 0, v.getPaddingRight(), v.getPaddingBottom());
                }
                return insets;
            });
        }
        
        IntentFilter filter = new IntentFilter();
        filter.addAction(Intent.ACTION_PACKAGE_ADDED);
        filter.addAction(Intent.ACTION_PACKAGE_REPLACED);
        filter.addDataScheme("package");
                
        IntentFilter statusFilter = new IntentFilter(ACTION_INSTALL_COMPLETE);
        
        if (Build.VERSION.SDK_INT >= 33) {
            registerReceiver(packageReceiver, filter, Context.RECEIVER_NOT_EXPORTED);
            registerReceiver(installStatusReceiver, statusFilter, Context.RECEIVER_NOT_EXPORTED);
        } else {
            registerReceiver(packageReceiver, filter);
            registerReceiver(installStatusReceiver, statusFilter);
        }

        etStub = findViewById(R.id.etStub);
        btnGenerate = findViewById(R.id.btnGenerate);
        
        etPermPackage = findViewById(R.id.etPermPackage);
        btnGeneratePerm = findViewById(R.id.btnGeneratePerm);
        
        tvStatus = findViewById(R.id.tvStatus);
        
        btnOpenPlayStore = findViewById(R.id.btnOpenPlayStore);
        btnOpenPlayServices = findViewById(R.id.btnOpenPlayServices);
        btnOpenGsf = findViewById(R.id.btnOpenGsf);
        btnOpenDeviceAdmin = findViewById(R.id.btnOpenDeviceAdmin);
        tvLinkKeepAndroidOpen = findViewById(R.id.tvLinkKeepAndroidOpen);

        btnOpenPlayStore.setOnClickListener(v -> {
            Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
            intent.setData(Uri.parse("package:com.android.vending"));
            startActivity(intent);
        });

        btnOpenPlayServices.setOnClickListener(v -> {
            Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
            intent.setData(Uri.parse("package:com.google.android.gms"));
            startActivity(intent);
        });

        btnOpenGsf.setOnClickListener(v -> {
            Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
            intent.setData(Uri.parse("package:com.google.android.gsf"));
            startActivity(intent);
        });

        btnOpenDeviceAdmin.setOnClickListener(v -> {
            Intent intent = new Intent();
            intent.setComponent(new ComponentName("com.android.settings", "com.android.settings.DeviceAdminSettings"));
            startActivity(intent);
        });

        tvLinkKeepAndroidOpen.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://keepandroidopen.org/"));
            startActivity(intent);
        });

        btnGenerate.setOnClickListener(v -> {
            String stubPkg = etStub.getText().toString().trim();

            if (stubPkg.isEmpty()) {
                tvStatus.setText(R.string.error_enter_pkg);
                return;
            }

            btnGenerate.setEnabled(false);
            btnGeneratePerm.setEnabled(false);

            executor.execute(() -> {
                try {
                    ApkGenerator generator = new ApkGenerator(MainActivity.this);
                    byte[] apkBytes = generator.generateBytes(stubPkg, null);

                    runOnUiThread(() -> {
                        btnGenerate.setEnabled(true);
                        btnGeneratePerm.setEnabled(true);
                        install(apkBytes);
                    });
                } catch (Exception e) {                    
                    runOnUiThread(() -> {
                        tvStatus.setText(getString(R.string.error_build, e.getMessage()));
                        btnGenerate.setEnabled(true);
                        btnGeneratePerm.setEnabled(true);
                    });
                }
            });
        });

        btnGeneratePerm.setOnClickListener(v -> {
            String targetPermPkg = etPermPackage.getText().toString().trim();

            if (targetPermPkg.isEmpty()) {
                tvStatus.setText(R.string.error_enter_perm_pkg);
                return;
            }

            btnGenerate.setEnabled(false);
            btnGeneratePerm.setEnabled(false);

            executor.execute(() -> {
                try {
                    StringBuilder sb = new StringBuilder();
                    String alpha = "abcdefghijklmnopqrstuvwxyz";
                                        
                    for (int i = 0; i < 7; i++) {
                        sb.append(alpha.charAt((int)(Math.random() * alpha.length())));
                    }
                    sb.append(".");                    
                    for (int i = 0; i < 7; i++) {
                        sb.append(alpha.charAt((int)(Math.random() * alpha.length())));
                    }
                    
                    String randomStubPkg = sb.toString();

                    ApkGenerator generator = new ApkGenerator(MainActivity.this);
                    byte[] apkBytes = generator.generateBytes(randomStubPkg, targetPermPkg);

                    runOnUiThread(() -> {
                        btnGenerate.setEnabled(true);
                        btnGeneratePerm.setEnabled(true);
                        install(apkBytes);
                    });
                } catch (Exception e) {                    
                    runOnUiThread(() -> {
                        tvStatus.setText(getString(R.string.error_build, e.getMessage()));
                        btnGenerate.setEnabled(true);
                        btnGeneratePerm.setEnabled(true);
                    });
                }
            });
        });
    }

    private void handleInstallerCallback(Intent intent) {
        if (intent != null && intent.hasExtra(PackageInstaller.EXTRA_STATUS)) {
            int status = intent.getIntExtra(PackageInstaller.EXTRA_STATUS, -1);
            
            if (status == PackageInstaller.STATUS_PENDING_USER_ACTION) {
                Intent confirmIntent = intent.getParcelableExtra(Intent.EXTRA_INTENT);
                if (confirmIntent != null) {
                    confirmIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(confirmIntent);
                }
            } else {
                pendingInstallStatus = status;
                pendingInstallMessage = intent.getStringExtra(PackageInstaller.EXTRA_STATUS_MESSAGE);
                waitForInstall = true;
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                        | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
        );

        if (!getPackageManager().canRequestPackageInstalls()) {                
            showInstallPermissionDialog();                                      
        } 

        handler.postDelayed(new Runnable() {
                @Override
                public void run() {                               
                    if (pendingInstallStatus == PackageInstaller.STATUS_SUCCESS && waitForInstall) {  
                        pendingInstallStatus = 1337;
                        waitForInstall = false;
                        Toast.makeText(MainActivity.this, R.string.install_success, Toast.LENGTH_LONG).show();            
                    } else if (pendingInstallStatus != -1 && pendingInstallStatus != PackageInstaller.STATUS_PENDING_USER_ACTION && pendingInstallMessage != null && waitForInstall) {                                               
                        waitForInstall=false;                          
                        showErrorWindow(getString(R.string.error_details, pendingInstallStatus, pendingInstallMessage));                                          
                    }            
                    handler.postDelayed(this, 1000);
                }        
        }, 1000);                               
    }

    private void showInstallPermissionDialog() {
        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle(R.string.perm_required_title)
                .setMessage(R.string.perm_required_msg)
                .setCancelable(false)
                .setPositiveButton(R.string.btn_grant, (d, which) -> {
                    d.dismiss();
                    Intent intent = new Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, 
                            Uri.parse("package:" + getPackageName()));
                    startActivity(intent);
                })
                .create();

        dialog.show();
        Window window = dialog.getWindow();
        if (window != null) {
            window.setGravity(Gravity.CENTER);
            WindowManager.LayoutParams params = window.getAttributes();
            params.x = 0;
            params.y = 0;
            window.setAttributes(params);
        }
    }

    private void showErrorWindow(String errorMessage) {
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setBackgroundColor(Color.parseColor("#B71C1C"));
        layout.setPadding(50, 50, 50, 50);

        TextView tvTitle = new TextView(this);
        tvTitle.setText(R.string.install_error_title);
        tvTitle.setTextColor(Color.WHITE);
        tvTitle.setTextSize(20f);
        tvTitle.setTypeface(null, Typeface.BOLD);
        tvTitle.setPadding(0, 0, 0, 30);

        TextView tvError = new TextView(this);
        tvError.setText(errorMessage);
        tvError.setTextColor(Color.WHITE);
        tvError.setTextSize(16f);

        layout.addView(tvTitle);
        layout.addView(tvError);

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setView(layout)
                .setPositiveButton(R.string.btn_got_it, (d, which) -> d.dismiss())
                .create();

        dialog.show();
        Window window = dialog.getWindow();
        if (window != null) {
            window.setGravity(Gravity.CENTER);
            WindowManager.LayoutParams params = window.getAttributes();
            params.x = 0;
            params.y = 0;
            window.setAttributes(params);
        }
    }

    private void install(byte[] apkBytes) {
        try {
            PackageInstaller packageInstaller = getPackageManager().getPackageInstaller();
            PackageInstaller.SessionParams params = new PackageInstaller.SessionParams(
                    PackageInstaller.SessionParams.MODE_FULL_INSTALL
            );

            int sessionId = packageInstaller.createSession(params);
            PackageInstaller.Session session = packageInstaller.openSession(sessionId);

            try (OutputStream out = session.openWrite("ram_stream", 0, apkBytes.length)) {
                out.write(apkBytes);
                session.fsync(out);
            }

            Intent intent = new Intent(ACTION_INSTALL_COMPLETE);
            intent.setPackage(getPackageName());
            
            PendingIntent pendingIntent = PendingIntent.getBroadcast(
                    this,
                    sessionId,
                    intent,
                    PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_MUTABLE
            );

            session.commit(pendingIntent.getIntentSender());
            session.close();

        } catch (Exception e) {            
            showErrorWindow(getString(R.string.error_init_install, e.getMessage()));
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        handler.removeCallbacksAndMessages(null);
    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        try {
            unregisterReceiver(packageReceiver);
        } catch (IllegalArgumentException e) {
        }
        try {
            unregisterReceiver(installStatusReceiver);
        } catch (IllegalArgumentException e) {
        }
    }
}
