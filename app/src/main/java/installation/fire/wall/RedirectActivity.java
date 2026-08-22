package installation.fire.wall;

import android.app.Activity;
import android.content.Intent;

public class RedirectActivity extends Activity {

    @Override
    protected void onResume() {
        super.onResume();        
        startActivity(new Intent(this, MainActivity.class));
        finish();
    }
}
