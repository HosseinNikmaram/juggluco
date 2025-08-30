package tk.glucodata;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

/**
 * Test activity for demonstrating Firebase Crashlytics functionality
 * This activity should only be used for testing purposes
 */
public class CrashTestActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // Create a simple layout with test buttons
        Button testErrorButton = new Button(this);
        testErrorButton.setText("Test Error Logging");
        testErrorButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Test error logging
                Applic.logError("Test error message from CrashTestActivity");
                Toast.makeText(CrashTestActivity.this, "Error logged to Firebase", Toast.LENGTH_SHORT).show();
            }
        });
        
        Button testCrashButton = new Button(this);
        testCrashButton.setText("Test Crash (WARNING: Will crash app)");
        testCrashButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Test crash reporting - this will crash the app
                Toast.makeText(CrashTestActivity.this, "App will crash in 2 seconds...", Toast.LENGTH_LONG).show();
                
                // Delay the crash to allow the toast to be seen
                new android.os.Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        throw new RuntimeException("Test crash for Firebase Crashlytics - triggered from CrashTestActivity");
                    }
                }, 2000);
            }
        });
        
        Button testExceptionButton = new Button(this);
        testExceptionButton.setText("Test Exception Logging");
        testExceptionButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    // Simulate an error condition
                    throw new IllegalStateException("Simulated error for testing");
                } catch (Exception e) {
                    // Log the exception to Firebase
                    Applic.logCrash(e, "Simulated error from CrashTestActivity");
                    Toast.makeText(CrashTestActivity.this, "Exception logged to Firebase", Toast.LENGTH_SHORT).show();
                }
            }
        });
        
        Button closeButton = new Button(this);
        closeButton.setText("Close");
        closeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
        
        // Create a simple vertical layout
        android.widget.LinearLayout layout = new android.widget.LinearLayout(this);
        layout.setOrientation(android.widget.LinearLayout.VERTICAL);
        layout.setPadding(50, 50, 50, 50);
        
        layout.addView(testErrorButton);
        layout.addView(testCrashButton);
        layout.addView(testExceptionButton);
        layout.addView(closeButton);
        
        setContentView(layout);
    }
}